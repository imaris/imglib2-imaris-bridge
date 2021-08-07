package com.bitplane.xt;

import Imaris.Error;
import Imaris.IDataSetPrx;
import bdv.util.AxisOrder;
import bdv.util.volatiles.SharedQueue;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import com.bitplane.xt.util.ColorTableUtils;
import com.bitplane.xt.util.DatasetCalibration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.axis.CalibratedAxis;
import net.imagej.axis.DefaultLinearAxis;
import net.imglib2.Volatile;
import net.imglib2.display.ColorTable8;
import net.imglib2.img.Img;
import net.imglib2.img.basictypeaccess.volatiles.VolatileArrayDataAccess;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;

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

	/**
	 * Dimensions of the Imaris dataset, and how they map to ImgLib2 dimensions.
	 */
	private final DatasetDimensions datasetDimensions;

	/**
	 * Physical calibration: size of voxel and min coordinate in X,Y,Z
	 * <p>
	 * Note, that the min coordinate is in ImgLib2 convention:
	 * it refers to the voxel center. This is in contrast to Imaris
	 * conventions, where it would indicate the min corner of the min
	 * voxel.
	 */
	private final DatasetCalibration calib;

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

	// open existing
	public < V extends Volatile< T > & NativeType< V > & RealType< V >, A extends VolatileArrayDataAccess< A > >
	ImarisDataset( final IDataSetPrx dataset ) throws Error
	{
		this( dataset, ImarisDatasetOptions.options() );
	}

	// open existing
	public < V extends Volatile< T > & NativeType< V > & RealType< V >, A extends VolatileArrayDataAccess< A > >
	ImarisDataset( final IDataSetPrx dataset, final ImarisDatasetOptions options ) throws Error
	{
		this( dataset, new DatasetDimensions( dataset, options.values.includeAxes() ), false, false, options );
	}

	public < V extends Volatile< T > & NativeType< V > & RealType< V >, A extends VolatileArrayDataAccess< A > >
	ImarisDataset(
			final IDataSetPrx dataset,
			final DatasetDimensions datasetDimensions,
			final boolean writable,
			final boolean isEmptyDataset,
			final ImarisDatasetOptions options ) throws Error
	{

		this.dataset = dataset;
		this.calib = new DatasetCalibration( dataset );
		this.datasetDimensions = datasetDimensions;

		// --------------------------------------------------------------------
		// Determine imglib2 dimensions/cellDimensions for all pyramid levels.

		final int[][] pyramidSizes = dataset.GetPyramidSizes();
		final int[][] pyramidBlockSizes = dataset.GetPyramidBlockSizes();
		final int numResolutions = pyramidSizes.length;
		final AxisOrder axisOrder = datasetDimensions.getAxisOrder();
		final int numDimensions = axisOrder.numDimensions();
		final int[] mapDimensions = datasetDimensions.getMapDimensions();
		final int[] imarisDimensions = datasetDimensions.getImarisDimensions();

		final long[][] dimensions = new long[ numResolutions ][ numDimensions ];
		final int[][] cellDimensions = new int[ numResolutions ][ numDimensions ];
		for ( int l = 0; l < numResolutions; ++l )
		{
			for ( int i = 0; i < 5; ++i )
			{
				final int d = mapDimensions[ i ];
				if ( d >= 0 )
				{
					if ( i < 3 )
					{
						dimensions[ l ][ d ] = pyramidSizes[ l ][ i ];
						cellDimensions[ l ][ d ] = pyramidBlockSizes[ l ][ i ];
					}
					else
					{
						dimensions[ l ][ d ] = imarisDimensions[ i ];
						cellDimensions[ l ][ d ] = 1;
					}
				}
			}
		}

		// handle optional cellDimensions override (for full resolution only)
		if ( options.values.cellDimensions() != null )
		{
			final int[] optionalCellDimensions = options.values.cellDimensions();
			final int max = optionalCellDimensions.length - 1;
			for ( int i = 0; i < 5; ++i )
			{
				final int d = mapDimensions[ i ];
				if ( d >= 0 )
					cellDimensions[ 0 ][ d ] =  optionalCellDimensions[ Math.min( d, max ) ];
			}
		}


		// --------------------------------------------------------------------
		// Create cached images.

		final T type = ImarisUtils.imglibTypeFor( dataset.GetType() );
		final SharedQueue queue = new SharedQueue( 16, numResolutions );
		final CachedImagePyramid< T, V, A > imagePyramid = new CachedImagePyramid<>(
				type, axisOrder, dataset,
				dimensions, cellDimensions, mapDimensions,
				queue,
				writable,
				isEmptyDataset,
				options
		);
		this.imagePyramid = imagePyramid;


		// --------------------------------------------------------------------
		// Create ImgPlus with metadata and color tables.

		final Img< T > img = getImage();
		imp = new ImgPlus<>( img );
		imp.setName( getName() );
		updateImpAxes();
		updateImpColorTables();


		// --------------------------------------------------------------------
		// Instantiate multi-resolution sources.

		sources = new ArrayList<>();

		final double[][] mipmapScales = new double[ numResolutions ][ 3 ];
		Arrays.fill( mipmapScales[ 0 ], 1 );
		for ( int level = 1; level < numResolutions; ++level )
		{
			for ( int d = 0; d < 3; ++d )
			{
				final boolean half = pyramidSizes[ level - 1 ][ d ] / 2 == pyramidSizes[ level ][ d ];
				final double s = half ? 2 : 1;
				mipmapScales[ level ][ d ] = s * mipmapScales[ level - 1 ][ d ];
			}
		}

		final List< ImagePyramid< T, V > > channelPyramids = imagePyramid.splitIntoSourceStacks();
		final V volatileType = imagePyramid.getVolatileType();
		final boolean hasTimepoints = axisOrder.hasTimepoints();
		final int sc = imarisDimensions[ 3 ];
		for ( int c = 0; c < sc; ++c )
		{
			final String name = String.format( "%s - %s", getName(), dataset.GetChannelName( c ) );
			final ImagePyramid< T, V > channelPyramid = channelPyramids.get( c );
			final Source< T > source = hasTimepoints
					? new ImarisSource4D<>( calib, type, channelPyramid.getImgs(), mipmapScales, name )
					: new ImarisSource3D<>( calib, type, channelPyramid.getImgs(), mipmapScales, name );
			final Source< V > volatileSource = hasTimepoints
					? new ImarisSource4D<>( calib, volatileType, channelPyramid.getVolatileImgs(), mipmapScales, name )
					: new ImarisSource3D<>( calib, volatileType, channelPyramid.getVolatileImgs(), mipmapScales, name );
			final SourceAndConverter< V > vsoc = new SourceAndConverter<>( volatileSource, ColorTableUtils.createChannelConverterToARGB( volatileType, dataset, c ) );
			final SourceAndConverter< T > soc = new SourceAndConverter<>( source, ColorTableUtils.createChannelConverterToARGB( type, dataset, c ), vsoc );
			sources.add( soc );
		}
	}

	/**
	 * Create/update color tables for ImgPlus.
	 */
	private void updateImpColorTables() throws Error
	{
		final int[] imarisDimensions = datasetDimensions.getImarisDimensions();
		final int sz = imarisDimensions[ 2 ];
		final int sc = imarisDimensions[ 3 ];
		final int st = imarisDimensions[ 4 ];
		imp.initializeColorTables( sc * sz * st );
		for ( int c = 0; c < sc; ++c )
		{
			final ColorTable8 cT = ColorTableUtils.createChannelColorTable( dataset, c );
			for ( int t = 0; t < st; ++t )
				for ( int z = 0; z < sz; ++z )
					imp.setColorTable( cT, z + sz * ( c + sc * t ) );
		}
	}

	/**
	 * Create/update calibrated axes for ImgPlus.
	 */
	private void updateImpAxes()
	{
		final ArrayList< CalibratedAxis > axes = new ArrayList<>();
		axes.add( new DefaultLinearAxis( Axes.X, calib.unit(), calib.voxelSize( 0 ) ) );
		axes.add( new DefaultLinearAxis( Axes.Y, calib.unit(), calib.voxelSize( 1 ) ) );
		if ( axisOrder().hasZ() )
			axes.add( new DefaultLinearAxis( Axes.Z, calib.unit(), calib.voxelSize( 2 ) ) );
		if ( axisOrder().hasChannels() )
			axes.add( new DefaultLinearAxis( Axes.CHANNEL ) );
		if ( axisOrder().hasTimepoints() )
			axes.add( new DefaultLinearAxis( Axes.TIME ) );

		for ( int i = 0; i < axes.size(); ++i )
			imp.setAxis( axes.get( i ), i );
	}

	/**
	 * Update all sources with the current {@link #calib}.
	 * (This is called after {@link #setCalibration}.)
	 */
	private void updateSourceCalibrations()
	{
		for ( SourceAndConverter< T > soc : sources )
		{
			final AbstractImarisSource< ? > source = ( AbstractImarisSource< ? > ) soc.getSpimSource();
			final AbstractImarisSource< ? > volatileSource = ( AbstractImarisSource< ? > ) soc.asVolatile().getSpimSource();
			source.setCalibration( calib );
			volatileSource.setCalibration( calib );
		}
	}


	/**
	 * Sets unit, voxel size, and min coordinate from Imaris extends.
	 * <p>
	 * Note, that the given min/max extends are in Imaris conventions: {@code
	 * extendMinX} refers to the min corner of the min voxel of the dataset,
	 * {@code extendMaxX} refers to the max corner of the max voxel of the
	 * dataset.
	 * <p>
	 * This is in contrast to the ImgLib2 convention, where coordinates always
	 * refer to the voxel center.
	 */
	public void setCalibration(
			final String unit,
			final float extendMinX,
			final float extendMaxX,
			final float extendMinY,
			final float extendMaxY,
			final float extendMinZ,
			final float extendMaxZ ) throws Error // TODO: revise exception handling
	{
		calib.setExtends( unit, extendMinX, extendMaxX, extendMinY, extendMaxY, extendMinZ, extendMaxZ );
		updateSourceCalibrations();
		updateImpAxes();
		calib.applyToDataset( dataset );
	}

	/**
	 * Set the voxel size and unit.
	 * (The min coordinate is not modified).
	 */
	public void setCalibration( final VoxelDimensions voxelDimensions ) throws Error
	{
		calib.setVoxelDimensions( voxelDimensions );
		updateSourceCalibrations();
		updateImpAxes();
		calib.applyToDataset( dataset );
	}

	/**
	 * Sets unit, voxel size, and min coordinate.
	 * <p>
	 * Note, that the min coordinate is in ImgLib2 convention: It refers to the
	 * voxel center. This is in contrast to Imaris conventions, where {@code
	 * ExtendMinX, ExtendMinY, ExtendMinZ} indicate the min corner of the min voxel.
	 * <p>
	 * This method translates the given min coordinate (etc) to Imaris {@code
	 * extendMin/Max} extends.
	 */
	public void setCalibration( final VoxelDimensions voxelDimensions,
			final double minX,
			final double minY,
			final double minZ ) throws Error
	{
		calib.setMin( minX, minY, minZ );
		calib.setVoxelDimensions( voxelDimensions );
		updateSourceCalibrations();
		updateImpAxes();
		calib.applyToDataset( dataset );
	}

	/**
	 * Get the full resolution image.
	 * The image is a {@code CachedCellImg} which loads blocks from Imaris.
	 */
	public < A > Img< T > getImage() // TODO: rename to getImg()
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

	/**
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
		return calib.voxelDimensions();
	}

	/**
	 * Get the base color of a channel.
	 *
	 * @param channel index of the channel
	 * @return channel color
	 */
	public ARGBType getChannelColor( final int channel ) throws Error
	{
		return ColorTableUtils.getChannelColor( dataset, channel );
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
	 * Get the underlying {@code IDataSet} ICE proxy.
	 */
	public IDataSetPrx getIDataSetPrx()
	{
		return dataset;
	}

	/**
	 * TODO
	 *
	 * Persist changes back to Imaris.
	 * Note that only the full resolution (level 0) image is writable!
	 */
	public void persist()
	{
		this.imagePyramid.persist();
	}

	/**
	 * TODO
	 */
	public void invalidatePyramid() // TODO: rename!?
	{
		this.imagePyramid.invalidate();
	}
}
