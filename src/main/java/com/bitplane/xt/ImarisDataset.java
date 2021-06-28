package com.bitplane.xt;

import Imaris.Error;
import Imaris.IDataSetPrx;
import Imaris.cColorTable;
import Imaris.tType;
import bdv.util.AxisOrder;
import bdv.util.volatiles.SharedQueue;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import java.util.ArrayList;
import java.util.List;
import mpicbg.spim.data.sequence.FinalVoxelDimensions;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.axis.CalibratedAxis;
import net.imagej.axis.DefaultLinearAxis;
import net.imglib2.Volatile;
import net.imglib2.converter.Converter;
import net.imglib2.display.ColorTable8;
import net.imglib2.display.RealARGBColorConverter;
import net.imglib2.img.Img;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileByteArray;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileFloatArray;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileShortArray;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;

/**
 * Wraps Imaris {@code IDataSetPrx} into {@code CachedCellImg}s that are lazy-loaded.
 * <p>
 * The data is provided as
 * <ul>
 *     <li>an {@code Img} ({@link #getImage}),</li>
 *     <li>an {@code ImgPlus} with the correct metadata ({@link #getImgPlus}), and</li>
 *     <li>a list of {@code SourceAndConverter}, one for each channel, for display in BDV ({@link #getSources}).</li>
 * </ul>
 * All these are views on the same data, backed by a common cache.
 * The BDV sources are multi-resolution and have volatile versions.
 *
 * @param <T>
 * 		imglib2 pixel type
 */
public class ImarisDataset< T extends NativeType< T > & RealType< T > >
{
	private final IDataSetPrx dataset;

	private final tType datasetType;

	/**
	 * Maps Imaris dimension indices to imglib2 dimension indices.
	 * If {@code i} is dimension index from Imaris (0..4 means X,Y,Z,C,T)
	 * then {@code mapDimensions[i]} is the corresponding dimension in {@link #getImage}.
	 * For {@link #getImage} dimensions with size=1 are skipped.
	 * E.g., for a X,Y,C image {@code mapDimensions = {0,1,-1,2,-1}}.
	 */
	private final int[] mapDimensions;

	/**
	 * physical calibration: size of voxel in X,Y,Z
	 */
	private final VoxelDimensions voxelDimensions;

	/**
	 * Non-volatile and volatile images for each resolution, backed by a joint cache which loads blocks from Imaris.
	 */
	private final CachedImagePyramid< T, ?, ? > imagePyramid;

	/**
	 * ImgPlus wrapping full resolution image.
	 * Metadata and color tables are set up according to Imaris (at the time of construction of this {@code ImarisDataset}).
	 */
	private final ImgPlus< T > imp;

	/**
	 * List of sources, one for each channel of the dataset.
	 * The sources provide nested volatile versions.
	 */
	private final List< SourceAndConverter< T > > sources;

	public < V extends Volatile< T > & NativeType< V > & RealType< V >, A >
	ImarisDataset( final IDataSetPrx dataset ) throws Error
	{
		this.dataset = dataset;

		datasetType = dataset.GetType();
		final T type;
		switch ( datasetType )
		{
		case eTypeUInt8:
			type = ( T ) new UnsignedByteType();
			break;
		case eTypeUInt16:
			type = ( T ) new UnsignedShortType();
			break;
		case eTypeFloat:
			type = ( T ) new FloatType();
			break;
		default:
			throw new IllegalArgumentException();
		}


		// --------------------------------------------------------------------
		// Analyze sizes and extends to find axis order, dimension mapping, and
		// calibration.

		final ArrayList< CalibratedAxis > axes = new ArrayList<>();
		mapDimensions = new int[] { 0, 1, -1, -1, -1 };

		final int sx = dataset.GetSizeX();
		final int sy = dataset.GetSizeY();
		final int sz = dataset.GetSizeZ();
		final int sc = dataset.GetSizeC();
		final int st = dataset.GetSizeT();

		final double maxX = dataset.GetExtendMaxX();
		final double minX = dataset.GetExtendMinX();
		final double maxY = dataset.GetExtendMaxY();
		final double minY = dataset.GetExtendMinY();
		final double maxZ = dataset.GetExtendMaxZ();
		final double minZ = dataset.GetExtendMinZ();

		final double[] calib = new double[] {
				( maxX - minX ) / sx,
				( maxY - minY ) / sy,
				( maxZ - minZ ) / sz
		};
		final String unit = dataset.GetUnit();
		voxelDimensions = new FinalVoxelDimensions( unit, calib );

		axes.add( new DefaultLinearAxis( Axes.X, unit, calib[ 0 ] ) );
		axes.add( new DefaultLinearAxis( Axes.Y, unit, calib[ 1 ] ) );
		final StringBuffer sbAxisOrder = new StringBuffer( "XY" );

		int d = 2;
		if ( sz > 1 )
		{
			axes.add( new DefaultLinearAxis( Axes.Z, unit, calib[ 2 ] ) );
			sbAxisOrder.append( "Z" );
			mapDimensions[ 2 ] = d++;
		}
		if ( sc > 1 )
		{
			axes.add( new DefaultLinearAxis( Axes.CHANNEL ) );
			sbAxisOrder.append( "C" );
			mapDimensions[ 3 ] = d++;
		}
		if ( st > 1 )
		{
			axes.add( new DefaultLinearAxis( Axes.TIME ) );
			sbAxisOrder.append( "T" );
			mapDimensions[ 4 ] = d++;
		}
		final int numDimensions = d;
		final AxisOrder axisOrder = AxisOrder.valueOf( sbAxisOrder.toString() );


		// --------------------------------------------------------------------
		// Analyze pyramid sizes and derive imglib2 dimensions.

		final int[][] pyramidSizes = dataset.GetPyramidSizes();
		final int[][] pyramidBlockSizes = dataset.GetPyramidBlockSizes();
		final int numResolutions = pyramidSizes.length;

		final long[][] dimensions = new long[ numResolutions ][ numDimensions ];
		final int[][] cellDimensions = new int[ numResolutions ][ numDimensions ];
		for ( int l = 0; l < numResolutions; ++l )
		{
			for ( int i = 0; i < 3; ++i )
			{
				if ( mapDimensions[ i ] >= 0 )
				{
					dimensions[ l ][ mapDimensions[ i ] ] = pyramidSizes[ l ][ i ];
					cellDimensions[ l ][ mapDimensions[ i ] ] = pyramidBlockSizes[ l ][ i ];
				}
			}
			if ( sc > 1 )
			{
				dimensions[ l ][ mapDimensions[ 3 ] ] = sc;
				cellDimensions[ l ][ mapDimensions[ 3 ] ] = 1;
			}
			if ( st > 1 )
			{
				dimensions[ l ][ mapDimensions[ 4 ] ] = st;
				cellDimensions[ l ][ mapDimensions[ 4 ] ] = 1;
			}
		}


		// --------------------------------------------------------------------
		// Create cached images.

		final CachedImagePyramid< T, V, A > imagePyramid = new CachedImagePyramid<>( type, axisOrder, dimensions, cellDimensions, volatileArraySource() );
		this.imagePyramid = imagePyramid;


		// --------------------------------------------------------------------
		// Create ImgPlus with metadata and color tables.

		final Img< T > img = getImage();
		imp = new ImgPlus<>( img );
		for ( int i = 0; i < axes.size(); ++i )
			imp.setAxis( axes.get( i ), i );
		imp.initializeColorTables( sc * sz );
		for ( int c = 0; c < sc; ++c )
		{
			final ColorTable8 cT = createColorTable( c );
			for ( int z = 0; z < sz; ++z )
				imp.setColorTable( cT, c * sz + z );
		}
		imp.setName( getName() );



		// --------------------------------------------------------------------
		// Instantiate multi-resolution sources.

		sources = new ArrayList<>();

		final double[][] mipmapScales = new double[ numResolutions ][ 3 ];
		mipmapScales[ 0 ][ 0 ] = 1;
		mipmapScales[ 0 ][ 1 ] = 1;
		mipmapScales[ 0 ][ 2 ] = 1;
		for ( int level = 1; level < numResolutions; ++level )
		{
			for ( d = 0; d < 3; ++d )
			{
				final boolean half = pyramidSizes[ level - 1 ][ d ] / 2 == pyramidSizes[ level ][ d ];
				final double s = half ? 2 : 1;
				mipmapScales[ level ][ d ] = s * mipmapScales[ level - 1 ][ d ];
			}
		}

		final List< ImagePyramid< T, V > > channelPyramids = imagePyramid.splitIntoSourceStacks();
		final V volatileType = imagePyramid.getVolatileType();
		final boolean hasTimepoints = st > 1;
		for ( int c = 0; c < sc; ++c )
		{
			final String name = String.format( "%s - %s", getName(), dataset.GetChannelName( c ) );
			final ImagePyramid< T, V > channelPyramid = channelPyramids.get( c );
			final Source< T > source = hasTimepoints
					? new ImarisSource4D<>( voxelDimensions, minX, minY, minZ, type, channelPyramid.getImgs(), mipmapScales, name )
					: new ImarisSource3D<>( voxelDimensions, minX, minY, minZ, type, channelPyramid.getImgs(), mipmapScales, name );
			final Source< V > volatileSource = hasTimepoints
					? new ImarisSource4D<>( voxelDimensions, minX, minY, minZ, volatileType, channelPyramid.getVolatileImgs(), mipmapScales, name )
					: new ImarisSource3D<>( voxelDimensions, minX, minY, minZ, volatileType, channelPyramid.getVolatileImgs(), mipmapScales, name );
			final SourceAndConverter< V > vsoc = new SourceAndConverter<>( volatileSource, createConverterToARGB( volatileType, c ) );
			final SourceAndConverter< T > soc = new SourceAndConverter<>( source, createConverterToARGB( type, c ), vsoc );
			sources.add( soc );
		}
	}

	/**
	 * Get the full resolution image.
	 * The image is a {@code CachedCellImg} which loads blocks from Imaris.
	 */
	public < A > Img< T > getImage()
	{
		return imagePyramid.getImg( 0 );
	}

	/**
	 * Get {@code ImgPlus} wrapping full resolution image (see {@link #getImage}).
	 * Metadata and color tables are set up according to Imaris
	 * (at the time of construction of this {@code ImarisDataset}).
	 */
	public ImgPlus< T > getImgPlus()
	{
		return imp;
	}

	/**
	 * Get the list of sources, one for each channel of the dataset.
	 * The sources provide nested volatile versions.
	 * The sources are multi-resolution, reflecting the resolution pyramid of the Imaris dataset.
	 */
	public List< SourceAndConverter< T > > getSources()
	{
		return sources;
	}

	/**
	 * Get the {@code SharedQueue} used for asynchronous loading of blocks from Imaris.
	 */
	public SharedQueue getSharedQueue()
	{
		return imagePyramid.getSharedQueue();
	}

	/**
	 * Get the number of levels in the resolution pyramid.
	 */
	public int numResolutions()
	{
		return imagePyramid.numResolutions();
	}

	/**
	 * Get the number channels.
	 */
	public int numChannels()
	{
		return imagePyramid.numChannels();
	}

	/*
	 * Get the number timepoints.
	 */
	public int numTimepoints()
	{
		return imagePyramid.numTimepoints();
	}

	/**
	 * Get an instance of the pixel type.
	 */
	public T getType()
	{
		return imagePyramid.getType();
	}

	/**
	 * Get the axis order of this dataset.
	 *
	 * Note that Dimensions of size 1 are stripped from the dataset.
	 * So a single-channel, single-timepoint image might have axis order {@code XYZ}.
	 */
	public AxisOrder axisOrder()
	{
		return imagePyramid.axisOrder();
	}

	/**
	 * Get the physical calibration: size of voxel in X,Y,Z
	 */
	public VoxelDimensions getVoxelDimensions()
	{
		return voxelDimensions;
	}

	/**
	 * Get the base color of a channel.
	 *
	 * @param channel index of the channel
	 * @return channel color
	 */
	public ARGBType getChannelColor( final int channel ) throws Error
	{
		final int rgba = dataset.GetChannelColorRGBA( channel );
		final int r = rgba & 0xff;
		final int g = ( rgba >> 8 ) & 0xff;
		final int b = ( rgba >> 16 ) & 0xff;
		final int a = ( rgba >> 24 ) & 0xff;
		return new ARGBType( ARGBType.rgba( r, g, b, 255 - a ) );
	}

	/**
	 * Get the "Image > Filename" parameter of the dataset.
	 */
	public String getFilename() throws Error
	{
		return dataset.GetParameter( "Image", "Filename" );
	}

	/**
	 * Get the "Image > Name" parameter of the dataset.
	 */
	public String getName() throws Error
	{
		return dataset.GetParameter("Image", "Name");
	}

	/**
	 * TODO: is this required?
	 */
	public IDataSetPrx getDataset()
	{
		return dataset;
	}

	@FunctionalInterface
	private interface GetDataSubVolume
	{
		/**
		 * Get sub-volume as flattened primitive array.
		 *
		 * @param ox offset in X
		 * @param oy offset in Y
		 * @param oz offset in Z
		 * @param oc channel index
		 * @param ot timepoint index
		 * @param r resolution level (0 is full resolution)
		 * @param sx size in X
		 * @param sy size in Y
		 * @param sz size in Z
		 * @return {@code byte[]}, {@code short[]}, {@code float[]}, depending on dataset type.
		 */
		Object get( final int ox, final int oy, final int oz, final int oc, final int ot, final int r, final int sx, final int sy, final int sz ) throws Error;
	}

	/**
	 * Get the appropriate {@code GetDataSubVolume} for {@link #datasetType}.
	 */
	private GetDataSubVolume dataSource()
	{
		switch ( datasetType )
		{
		case eTypeUInt8:
			return dataset::GetPyramidDataBytes;
		case eTypeUInt16:
			return dataset::GetPyramidDataShorts;
		case eTypeFloat:
			return dataset::GetPyramidDataFloats;
		default:
			throw new IllegalArgumentException();
		}
	}

	@FunctionalInterface
	interface PixelSource< A >
	{
		/**
		 * Get sub-volume as flattened primitive array.
		 *
		 * @param level
		 * 		resolution level (0 is full resolution).
		 * @param min
		 * 		minimum of interval in {@link #getImage image} space.
		 * 		Will be augmented to 5D if necessary (See {@link #mapDimensions}).
		 * @param size
		 * 		size of interval in {@link #getImage image} space.
		 * 		Will be augmented to 5D if necessary (See {@link #mapDimensions}).
		 *
		 * @return {@code byte[]}, {@code short[]}, {@code float[]}, depending on dataset type.
		 */
		A get( final int level, final long[] min, final int[] size ) throws Error;
	}

	private interface MapIntervalDimension
	{
		int min( final long[] min );

		int size( final int[] size );
	}

	private static MapIntervalDimension mapIntervalDimension( final int d )
	{
		if ( d < 0 )
			return constantMapIntervalDimension;

		return new MapIntervalDimension()
		{
			@Override
			public int min( final long[] min )
			{
				return ( int ) min[ d ];
			}

			@Override
			public int size( final int[] size )
			{
				return size[ d ];
			}
		};
	}

	private static final MapIntervalDimension constantMapIntervalDimension = new MapIntervalDimension()
	{
		@Override
		public int min( final long[] min )
		{
			return 0;
		}

		@Override
		public int size( final int[] size )
		{
			return 1;
		}
	};

	/**
	 * Apply {@link #mapDimensions} to {@link #dataSource}.
	 */
	private < A > PixelSource< A > volatileArraySource()
	{
		final GetDataSubVolume getDataSubVolume = dataSource();

		// Apply mapDimensions to getDataSubVolume
		final MapIntervalDimension x = mapIntervalDimension( mapDimensions[ 0 ] );
		final MapIntervalDimension y = mapIntervalDimension( mapDimensions[ 1 ] );
		final MapIntervalDimension z = mapIntervalDimension( mapDimensions[ 2 ] );
		final MapIntervalDimension c = mapIntervalDimension( mapDimensions[ 3 ] );
		final MapIntervalDimension t = mapIntervalDimension( mapDimensions[ 4 ] );
		final PixelSource< ? > pixels = ( r, min, size ) -> getDataSubVolume.get(
				x.min( min ), y.min( min ), z.min( min ), c.min( min ), t.min( min ), r,
				x.size( size ), y.size( size ), z.size( size ) );

		switch ( datasetType )
		{
		case eTypeUInt8:
			return ( r, min, size ) -> ( A ) new VolatileByteArray( ( byte[] ) ( pixels.get( r, min, size ) ), true );
		case eTypeUInt16:
			return ( r, min, size ) -> ( A ) new VolatileShortArray( ( short[] ) ( pixels.get( r, min, size ) ), true );
		case eTypeFloat:
			return ( r, min, size ) -> ( A ) new VolatileFloatArray( ( float[] ) ( pixels.get( r, min, size ) ), true );
		default:
			throw new IllegalArgumentException();
		}
	}

	private ColorTable8 createColorTable( final int channel ) throws Error
	{
		final cColorTable vColorTable = dataset.GetChannelColorTable( channel );
		if ( vColorTable != null && vColorTable.mColorRGB.length > 0 )
			return createColorTableFrom( vColorTable );
		else
		{
			final int vColor = dataset.GetChannelColorRGBA( channel );
			return createColorTableFrom( vColor );
		}
	}

	private static ColorTable8 createColorTableFrom( final int aRGBA )
	{
		final int vSize = 256;

		final byte[] rLut = new byte[ vSize ];
		final byte[] gLut = new byte[ vSize ];
		final byte[] bLut = new byte[ vSize ];
		final byte[] aLut = new byte[ vSize ];

		final int[] vRGBA = new int[ 4 ];
		components( aRGBA, vRGBA );
		final byte alpha = UnsignedByteType.getCodedSignedByte( 255 - vRGBA[ 3 ] );
		for ( int i = 0; i < vSize; ++i )
		{
			rLut[ i ] = ( byte ) ( i * vRGBA[ 0 ] / 255 );
			gLut[ i ] = ( byte ) ( i * vRGBA[ 1 ] / 255 );
			bLut[ i ] = ( byte ) ( i * vRGBA[ 2 ] / 255 );
			aLut[ i ] = alpha;
		}
		return new ColorTable8( rLut, gLut, bLut, aLut );
	}

	private static ColorTable8 createColorTableFrom( final cColorTable aColor )
	{
		final int[] vRGB = aColor.mColorRGB;
		final byte alpha = UnsignedByteType.getCodedSignedByte(
			255 - UnsignedByteType.getUnsignedByte(aColor.mAlpha));
		final int vSize = 256;
		final int vSourceSize = vRGB.length;

		final byte[] rLut = new byte[ vSize ];
		final byte[] gLut = new byte[ vSize ];
		final byte[] bLut = new byte[ vSize ];
		final byte[] aLut = new byte[ vSize ];

		final int[] vRGBA = new int[ 4 ];
		for ( int i = 0; i < vSize; ++i )
		{
			final int vIndex = ( i * vSourceSize ) / vSize;
			components( vRGB[ vIndex ], vRGBA );
			rLut[ i ] = ( byte ) ( vRGBA[ 0 ] );
			gLut[ i ] = ( byte ) ( vRGBA[ 1 ] );
			bLut[ i ] = ( byte ) ( vRGBA[ 2 ] );
			aLut[ i ] = alpha;
		}
		return new ColorTable8( rLut, gLut, bLut, aLut );
	}

	/**
	 * Split 8-bit rgba packed into an {@code int} value into components R, G, B, A.
	 * Store in the provided {@code components} array.
	 */
	private static void components( final int rgba, final int[] components )
	{
		components[ 0 ] = rgba & 0xff;
		components[ 1 ] = ( rgba >> 8 ) & 0xff;
		components[ 2 ] = ( rgba >> 16 ) & 0xff;
		components[ 3 ] = ( rgba >> 24 ) & 0xff;
	}

	/**
	 * Construct a converters for the specified {@code channel} with display range and color set up according to Imaris.
	 */
	private < T extends NumericType< T > & RealType< T > > Converter< T, ARGBType > createConverterToARGB( final T type, final int channel ) throws Error
	{
		final double typeMin = dataset.GetChannelRangeMin( channel );
		final double typeMax = dataset.GetChannelRangeMax( channel );
		final RealARGBColorConverter< T > converter = RealARGBColorConverter.create( type, typeMin, typeMax );
		converter.setColor( getChannelColor( channel ) );
		return converter;
	}
}
