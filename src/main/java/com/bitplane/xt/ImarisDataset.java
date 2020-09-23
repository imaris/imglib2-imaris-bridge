package com.bitplane.xt;

import Imaris.Error;
import Imaris.IDataSetPrx;
import Imaris.cColorTable;
import Imaris.tType;
import java.util.ArrayList;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.axis.CalibratedAxis;
import net.imagej.axis.DefaultLinearAxis;
import net.imglib2.cache.CacheLoader;
import net.imglib2.cache.img.CachedCellImg;
import net.imglib2.cache.img.ReadOnlyCachedCellImgFactory;
import net.imglib2.display.ColorTable8;
import net.imglib2.img.Img;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileByteArray;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileFloatArray;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileShortArray;
import net.imglib2.img.cell.Cell;
import net.imglib2.img.cell.CellGrid;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;

import static net.imglib2.cache.img.ReadOnlyCachedCellImgOptions.options;

class ImarisDataset< T extends NativeType< T > & RealType< T > >
{
	private static final int xyzCellSize = 32; // TODO make configurable

	private final IDataSetPrx dataset;

	private final tType datasetType;

	private final T type;

	private final ArrayList< CalibratedAxis > axes;

	private final long[] dimensions;

	/**
	 * cellDimensions are set to (if present)
	 * X,Y,Z: xyzCellSize
	 * C,T: 1
	 */
	private final int[] cellDimensions;

	/**
	 * Maps Imaris dimension indices to imglib2 dimension indices.
	 * If {@code i} is dimension index from Imaris (0..4 means X,Y,Z,C,T)
	 * then {@code mapDimensions[i]} is the corresponding dimension in {@link #getImage}.
	 * For {@link #getImage} dimensions with size=1 are skipped present.
	 * E.g., for a X,Y,C image {@code mapDimensions = {0,1,-1,2,-1}}.
	 */
	private final int[] mapDimensions;

	/**
	 * TODO
	 * physical calibration: size of voxel in X,Y,Z
	 */
	private final double[] calib;

	public ImarisDataset( final IDataSetPrx dataset ) throws Error
	{
		this.dataset = dataset;

		datasetType = dataset.GetType();
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

		axes = new ArrayList<>();
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

		calib = new double[] {
				( maxX - minX ) / sx,
				( maxY - minY ) / sy,
				( maxZ - minZ ) / sz
		};

		final String unit = dataset.GetUnit();

		final ArrayList< Long > dimensionsList = new ArrayList<>();
		final ArrayList< Integer > cellDimensionsList = new ArrayList<>();

		axes.add( new DefaultLinearAxis( Axes.X, unit, calib[ 0 ] ) );
		dimensionsList.add( ( long ) sx );
		cellDimensionsList.add( xyzCellSize );

		axes.add( new DefaultLinearAxis( Axes.Y, unit, calib[ 1 ] ) );
		dimensionsList.add( ( long ) sy );
		cellDimensionsList.add( xyzCellSize );

		int d = 2;
		if ( sz > 1 )
		{
			axes.add( new DefaultLinearAxis( Axes.Z, unit, calib[ 2 ] ) );
			dimensionsList.add( ( long ) sz );
			cellDimensionsList.add( xyzCellSize );
			mapDimensions[ 2 ] = d++;
		}

		if ( sc > 1 )
		{
			axes.add( new DefaultLinearAxis( Axes.CHANNEL ) );
			dimensionsList.add( ( long ) sc );
			cellDimensionsList.add( 1 );
			mapDimensions[ 3 ] = d++;
		}

		if ( st > 1 )
		{
			axes.add( new DefaultLinearAxis( Axes.TIME ) );
			dimensionsList.add( ( long ) st );
			cellDimensionsList.add( 1 );
			mapDimensions[ 4 ] = d;
		}

		dimensions = dimensionsList.stream().mapToLong( Long::longValue ).toArray();
		cellDimensions = cellDimensionsList.stream().mapToInt( Integer::intValue ).toArray();
	}

	public < A > Img< T > getImage()
	{
		final ReadOnlyCachedCellImgFactory factory = new ReadOnlyCachedCellImgFactory();
		final CellGrid grid = new CellGrid( dimensions, cellDimensions );
		final PixelSource< A > s = volatileArraySource();
		final CacheLoader< Long, Cell< A > > loader = key -> {
			final int n = grid.numDimensions();
			final long[] cellMin = new long[ n ];
			final int[] cellDims = new int[ n ];
			grid.getCellDimensions( key, cellMin, cellDims );
			return new Cell<>(
					cellDims,
					cellMin,
					s.get( cellMin, cellDims ) );
		};
		final CachedCellImg< T, A > img = factory.createWithCacheLoader(
				dimensions,
				type,
				loader,
				options().cellDimensions( cellDimensions ) );
		return img;
	}

	public ImgPlus< T > getImgPlus() throws Error
	{
		final Img< T > img = getImage();
		final ImgPlus< T > imp = new ImgPlus<>( img );

		for ( int i = 0; i < axes.size(); ++i )
			imp.setAxis( axes.get( i ), i );

		final int sc = dataset.GetSizeC();
		final int sz = dataset.GetSizeZ();
		imp.initializeColorTables( sc * sz );
		for ( int c = 0; c < sc; ++c )
		{
			final ColorTable8 cT = createColorTable( c );
			for ( int z = 0; z < sz; ++z )
			{
				imp.setColorTable( cT, c * sz + z );
			}
		}

		return imp;
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
		 * @param sx size in X
		 * @param sy size in Y
		 * @param sz size in Z
		 * @return {@code byte[]}, {@code short[]}, {@code float[]}, depending on dataset type.
		 */
		Object get( final int ox, final int oy, final int oz, final int oc, final int ot, final int sx, final int sy, final int sz ) throws Error;
	}

	/**
	 * Get the appropriate {@code GetDataSubVolume} for {@link #datasetType}.
	 */
	private GetDataSubVolume dataSource()
	{
		switch ( datasetType )
		{
		case eTypeUInt8:
			return dataset::GetDataSubVolumeAs1DArrayBytes;
		case eTypeUInt16:
			return dataset::GetDataSubVolumeAs1DArrayShorts;
		case eTypeFloat:
			return dataset::GetDataSubVolumeAs1DArrayFloats;
		default:
			throw new IllegalArgumentException();
		}
	}

	@FunctionalInterface
	private interface PixelSource< A >
	{
		/**
		 * Get sub-volume as flattened primitive array.
		 *
		 * @param min
		 * 		minimum of interval in {@link #getImage image} space.
		 * 		Will be augmented to 5D if necessary (See {@link #mapDimensions}).
		 * @param size
		 * 		size of interval in {@link #getImage image} space.
		 * 		Will be augmented to 5D if necessary (See {@link #mapDimensions}).
		 *
		 * @return {@code byte[]}, {@code short[]}, {@code float[]}, depending on dataset type.
		 */
		A get( final long[] min, final int[] size ) throws Error;
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
		final PixelSource< ? > pixels = ( min, size ) -> getDataSubVolume.get(
				x.min( min ), y.min( min ), z.min( min ), c.min( min ), t.min( min ),
				x.size( size ), y.size( size ), z.size( size ) );

		switch ( datasetType )
		{
		case eTypeUInt8:
			return ( min, size ) -> ( A ) new VolatileByteArray( ( byte[] ) ( pixels.get( min, size ) ), true );
		case eTypeUInt16:
			return ( min, size ) -> ( A ) new VolatileShortArray( ( short[] ) ( pixels.get( min, size ) ), true );
		case eTypeFloat:
			return ( min, size ) -> ( A ) new VolatileFloatArray( ( float[] ) ( pixels.get( min, size ) ), true );
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
		for ( int i = 0; i < vSize; ++i )
		{
			rLut[ i ] = ( byte ) ( i * vRGBA[ 0 ] / 255 );
			gLut[ i ] = ( byte ) ( i * vRGBA[ 1 ] / 255 );
			bLut[ i ] = ( byte ) ( i * vRGBA[ 2 ] / 255 );
			aLut[ i ] = ( byte ) ( i * vRGBA[ 3 ] / 255 );
		}
		return new ColorTable8( rLut, gLut, bLut, aLut );
	}

	private static ColorTable8 createColorTableFrom( final cColorTable aColor )
	{
		final int[] vRGB = aColor.mColorRGB;
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
			aLut[ i ] = aColor.mAlpha;
		}
		return new ColorTable8( rLut, gLut, bLut, aLut );
	}

	private static void components( final int rgba, final int[] components )
	{
		components[ 0 ] = rgba & 0xff;
		components[ 1 ] = ( rgba >> 8 ) & 0xff;
		components[ 2 ] = ( rgba >> 16 ) & 0xff;
		components[ 3 ] = ( rgba >> 24 ) & 0xff;
	}

	// TODO: for BDV?
	double[] getCalib()
	{
		return calib;
	}

	// TODO: remove?, for BDV???
	ARGBType getChannelColor( final int channel ) throws Error
	{
		final int rgba = dataset.GetChannelColorRGBA( channel );
		final int r = rgba & 0xff;
		final int g = ( rgba >> 8 ) & 0xff;
		final int b = ( rgba >> 16 ) & 0xff;
		final int a = ( rgba >> 24 ) & 0xff;
		System.out.println( "rgba = " + rgba );
		return new ARGBType( ARGBType.rgba( r, g, b, a ) );
	}
}
