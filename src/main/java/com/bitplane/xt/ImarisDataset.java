/*-
 * #%L
 * Expose the Imaris XT interface as an ImageJ2 service backed by ImgLib2.
 * %%
 * Copyright (C) 2019 - 2021 Bitplane AG
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package com.bitplane.xt;

import Imaris.Error;
import Imaris.IDataSetPrx;
import bdv.util.AxisOrder;
import bdv.util.ChannelSources;
import bdv.util.volatiles.SharedQueue;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import com.bitplane.xt.util.ColorTableUtils;
import com.bitplane.xt.util.TypeUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.axis.CalibratedAxis;
import net.imagej.axis.DefaultLinearAxis;
import net.imglib2.EuclideanSpace;
import net.imglib2.Volatile;
import net.imglib2.cache.img.CachedCellImg;
import net.imglib2.display.ColorTable8;
import net.imglib2.img.Img;
import net.imglib2.img.basictypeaccess.volatiles.VolatileArrayDataAccess;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import org.scijava.Context;

/**
 * {@code ImarisDataset} wraps an Imaris {@code IDataSetPrx} into a lazy-loaded
 * ImgLib2 {@code CachedCellImg}.
 * <p>
 * Data is lazy-loaded from Imaris -- image blocks, when they are first
 * accessed, are retrieved through the Imaris XT API and cached. Modified image
 * blocks are persisted back to Imaris before they are evicted from the cache.
 * (Imaris then in turn persists modified blocks to disk when they are evicted
 * from its cache.)
 * <p>
 * {@code ImarisDataset} provides various views on the image data:
 * <ul>
 *     <li>an {@code Img}, via the {@link #asImg} method,</li>
 *     <li>an {@code ImgPlus} with metadata, via the {@link #asImgPlus} method,</li>
 *     <li>a {@code net.imagej.Dataset} with metadata, via the {@link #asDataset} method,</li>
 *     <li>a list of BigDataViewer sources (one for each channel), via the
 *         {@link #getSources} method. The sources are multi-resolution and have
 *         volatile versions for non-blocking display in BDV.</li>
 * </ul>
 * <p>
 * All these are views on the same data, backed by a common cache. Note, that
 * only the last one exposes the Imaris resolution pyramid. The {@code Img},
 * {@code ImgPlus}, and {@code net.imagej.Dataset} views represent the
 * full-resolution image.
 * <p>
 * The generic pixel type {@code T} is one of {@code UnsignedByteType}, {@code
 * UnsignedShortType}, {@code FloatType}, and matches the type of the Imaris
 * dataset.
 *
 * @param <T>
 * 		imglib2 pixel type
 *
 * @author Tobias Pietzsch
 */
public class ImarisDataset< T extends NativeType< T > & RealType< T > > implements EuclideanSpace, ChannelSources< T >
{
	/**
	 * The scijava context. This is needed (only) for creating {@link #ijDataset}.
	 */
	private final Context context;

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
	 * Whether the dataset is writable. If the dataset is not writable,
	 * modifying methods will throw an {@code UnsupportedOperationException}.
	 */
	private boolean writable;

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
	 * IJ2 Dataset wrapping {@link #imp}. Lazily initialized.
	 */
	private Dataset ijDataset;

	/**
	 * List of sources, one for each channel of the dataset.
	 * The sources provide nested volatile versions.
	 */
	private final List< SourceAndConverter< T > > sources;

	// open existing

	/**
	 * Wrap an existing {@code IDataSetPrx}.
	 *
	 * @param context
	 * 		a SciJava context. This is only required, if used as a {@link
	 * 		#asDataset() net.imagej.Dataset}, otherwise can be {@code null}.
	 * @param dataset
	 * 		the Imaris dataset to wrap
	 */
	public < V extends Volatile< T > & NativeType< V > & RealType< V >, A extends VolatileArrayDataAccess< A > >
	ImarisDataset( final Context context, final IDataSetPrx dataset ) throws Error
	{
		this( context, dataset, ImarisDatasetOptions.options() );
	}

	/**
	 * Wrap an existing {@code IDataSetPrx}.
	 *
	 * @param context
	 * 		a SciJava context. This is only required, if used as a {@link
	 * 		#asDataset() net.imagej.Dataset}, otherwise can be {@code null}.
	 * @param dataset
	 * 		the Imaris dataset to wrap
	 * @param options
	 * 		additional options specifying which type of cache to use, etc.
	 */
	public < V extends Volatile< T > & NativeType< V > & RealType< V >, A extends VolatileArrayDataAccess< A > >
	ImarisDataset( final Context context, final IDataSetPrx dataset, final ImarisDatasetOptions options ) throws Error
	{
		this( context, dataset, new DatasetDimensions( dataset, options.values.includeAxes() ), false, options );
	}

	< V extends Volatile< T > & NativeType< V > & RealType< V >, A extends VolatileArrayDataAccess< A > >
	ImarisDataset(
			final Context context,
			final IDataSetPrx dataset,
			final DatasetDimensions datasetDimensions,
			final boolean isEmptyDataset,
			final ImarisDatasetOptions options ) throws Error
	{
		this.context = context;
		this.dataset = dataset;
		this.calib = new DatasetCalibration( dataset );
		this.datasetDimensions = datasetDimensions;
		this.writable = !options.values.readOnly();

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

		final T type = TypeUtils.imglibTypeFor( dataset.GetType() );
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

		final Img< T > img = asImg();
		imp = new ImgPlus<>( img );
		imp.setName( getName() );
		updateImpAxes();
		updateImpColorTables();
		updateImpChannelMinMax();


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
	 * Transfer Imaris channel min/max settings to ImgPlus.
	 */
	private void updateImpChannelMinMax() throws Error
	{
		final int sc = datasetDimensions.getImarisDimensions()[ 3 ];
		for ( int c = 0; c < sc; ++c )
		{
			final double min = dataset.GetChannelRangeMin( c );
			final double max = dataset.GetChannelRangeMax( c );
			imp.setChannelMinimum( c, min );
			imp.setChannelMaximum( c, max );
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
		final AxisOrder axisOrder = datasetDimensions.getAxisOrder();
		if ( axisOrder.hasZ() )
			axes.add( new DefaultLinearAxis( Axes.Z, calib.unit(), calib.voxelSize( 2 ) ) );
		if ( axisOrder.hasChannels() )
			axes.add( new DefaultLinearAxis( Axes.CHANNEL ) );
		if ( axisOrder.hasTimepoints() )
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

	private void ensureWritable()
	{
		if ( !writable )
			throw new UnsupportedOperationException( "This dataset is not writable" );
	}

	/**
	 * Sets unit, voxel size, and min coordinate from Imaris extents.
	 * <p>
	 * Note, that the given min/max extents are in Imaris conventions: {@code
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
		ensureWritable();
		final int[] size = datasetDimensions.getImarisDimensions();
		calib.setExtends( unit, extendMinX, extendMaxX, extendMinY, extendMaxY, extendMinZ, extendMaxZ, size );
		updateSourceCalibrations();
		updateImpAxes();
		calib.applyToDataset( dataset, size );
	}

	/**
	 * Set unit and voxel size.
	 * (The min coordinate is not modified).
	 */
	public void setCalibration( final VoxelDimensions voxelDimensions ) throws Error
	{
		ensureWritable();
		calib.setVoxelDimensions( voxelDimensions );
		updateSourceCalibrations();
		updateImpAxes();
		final int[] size = datasetDimensions.getImarisDimensions();
		calib.applyToDataset( dataset, size );
	}

	/**
	 * Sets unit, voxel size, and min coordinate.
	 * <p>
	 * Note, that the min coordinate is in ImgLib2 convention: It refers to the
	 * voxel center. This is in contrast to Imaris conventions, where {@code
	 * ExtendMinX, ExtendMinY, ExtendMinZ} indicate the min corner of the min voxel.
	 * <p>
	 * This method translates the given min coordinate (etc) to Imaris {@code
	 * extendMin/Max} extents.
	 */
	public void setCalibration( final VoxelDimensions voxelDimensions,
			final double minX,
			final double minY,
			final double minZ ) throws Error
	{
		ensureWritable();
		calib.setMin( minX, minY, minZ );
		calib.setVoxelDimensions( voxelDimensions );
		updateSourceCalibrations();
		updateImpAxes();
		final int[] size = datasetDimensions.getImarisDimensions();
		calib.applyToDataset( dataset, size );
	}

	/**
	 * Sets unit, voxel size, and min coordinate.
	 * <p>
	 * Note, that the min coordinate is in ImgLib2 convention: It refers to the
	 * voxel center. This is in contrast to Imaris conventions, where {@code
	 * ExtendMinX, ExtendMinY, ExtendMinZ} indicate the min corner of the min voxel.
	 * <p>
	 * This method translates the given min coordinate (etc) to Imaris {@code
	 * extendMin/Max} extents.
	 */
	public void setCalibration( final DatasetCalibration calibration ) throws Error
	{
		ensureWritable();
		calib.set( calibration );
		updateSourceCalibrations();
		updateImpAxes();
		final int[] size = datasetDimensions.getImarisDimensions();
		calib.applyToDataset( dataset, size );
	}

	/**
	 * Set the modification flag of the Imaris dataset.
	 * <p>
	 * Imaris asks whether to save a modified dataset, if {@code modified=true}.
	 * Set {@code modified=false}, if you want Imaris to terminate without
	 * prompting.
	 */
	public void setModified( final boolean modified ) throws Error
	{
		ensureWritable();
		dataset.SetModified( modified );
	}

	/**
	 * Get the full resolution image.
	 * The image is a {@code CachedCellImg} which loads blocks from Imaris, and
	 * writes modified blocks back to Imaris.
	 */
	public CachedCellImg< T, ? > asImg()
	{
		return imagePyramid.getImg( 0 );
	}

	/**
	 * Get an {@code ImgPlus} wrapping the full resolution image (see {@link
	 * #asImg}). Metadata and color tables are set up according to Imaris (at
	 * the time of construction of this {@code ImarisDataset}).
	 */
	public ImgPlus< T > asImgPlus()
	{
		return imp;
	}

	/**
	 * Get a IJ2 {@code net.imagej.Dataset} wrapping the full resolution image
	 * (see {@link #asImg}, {@link #asImgPlus()}). Metadata and color tables are
	 * set up according to Imaris (at the time of construction of this {@code
	 * ImarisDataset}).
	 */
	public Dataset asDataset()
	{
		synchronized ( imp )
		{
			if ( ijDataset == null )
			{
				final DatasetService datasetService = context.getService( DatasetService.class );
				ijDataset = datasetService.create( imp );
				ijDataset.setName( imp.getName() );
				ijDataset.setRGBMerged( false );
			}
			return ijDataset;
		}
	}

	/**
	 * Get a list of BigDataViewer sources, one for each channel of the dataset.
	 * The sources provide nested volatile versions. The sources are
	 * multi-resolution, reflecting the resolution pyramid of the Imaris
	 * dataset.
	 */
	@Override
	public List< SourceAndConverter< T > > getSources()
	{
		return sources;
	}

	/**
	 * Get the {@code SharedQueue} used for asynchronous loading of blocks from Imaris.
	 */
	@Override
	public SharedQueue getCacheControl()
	{
		return imagePyramid.getSharedQueue();
	}

	@Override
	public int numDimensions()
	{
		return datasetDimensions.getAxisOrder().numDimensions();
	}

	/**
	 * Get the number of levels in the resolution pyramid.
	 */
	public int numResolutions()
	{
		return imagePyramid.numResolutions();
	}

	/**
	 * Get the number timepoints.
	 */
	@Override
	public int numTimepoints()
	{
		return imagePyramid.numTimepoints();
	}

	/**
	 * Get an instance of the pixel type.
	 */
	@Override
	public T getType()
	{
		return imagePyramid.getType();
	}

	/**
	 * Get the size of the underlying
	 * 5D Imaris dataset and the mapping to dimensions of the ImgLib2
	 * representation.
	 */
	public DatasetDimensions getDatasetDimensions()
	{
		return datasetDimensions;
	}

	/**
	 * Get the physical calibration: unit, voxel size, and min in XYZ.
	 */
	public DatasetCalibration getCalibration()
	{
		return calib.copy();
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
	 * Get the {@code "Image > Filename"} parameter of the dataset.
	 */
	public String getFilename() throws Error
	{
		return dataset.GetParameter( "Image", "Filename" );
	}

	/**
	 * Get the {@code "Image > Name"} parameter of the dataset.
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
	 * Persist all modifications back to Imaris.
	 */
	public void persist()
	{
		ensureWritable();
		this.imagePyramid.persist();
	}

	/**
	 * Invalidate cache for all levels of the resolution pyramid, except the
	 * full resolution. This is necessary when modifying a dataset and at the
	 * same time visualizing it in BigDataViewer. (This scenario is not very
	 * likely in practice, but still...)
	 * <p>
	 * While actual modifications to the full-resolution image are immediately
	 * visible, updating the resolution pyramid needs to go through Imaris. That
	 * is, after making modifications, first {@link #persist()} should be called
	 * to ensure all changes have been transferred to Imaris. Second, the
	 * dataset should be visible in Imaris, so that Imaris recomputes the
	 * resolution pyramid. Finally, the lower-resolution images on the ImgLib2
	 * side should be invalidated (using this method), so the recomputed pyramid
	 * data is fetched from Imaris.
	 */
	public void invalidatePyramid()
	{
		this.imagePyramid.invalidate();
	}
}
