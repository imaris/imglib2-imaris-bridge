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
package com.bitplane.xt.img;

import Imaris.Error;
import Imaris.IDataSetPrx;
import com.bitplane.xt.ImarisApplication;
import com.bitplane.xt.util.MapDimensions;
import net.imglib2.Dimensions;
import net.imglib2.cache.Cache;
import net.imglib2.cache.CacheLoader;
import net.imglib2.cache.IoSync;
import net.imglib2.cache.LoaderRemoverCache;
import net.imglib2.cache.img.CellLoader;
import net.imglib2.cache.img.EmptyCellCacheLoader;
import net.imglib2.cache.img.LoadedCellCacheLoader;
import net.imglib2.cache.ref.GuardedStrongRefLoaderRemoverCache;
import net.imglib2.cache.ref.SoftRefLoaderRemoverCache;
import net.imglib2.exception.ImgLibException;
import net.imglib2.exception.IncompatibleTypeException;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.NativeImg;
import net.imglib2.img.NativeImgFactory;
import net.imglib2.img.basictypeaccess.ArrayDataAccessFactory;
import net.imglib2.img.basictypeaccess.DataAccess;
import net.imglib2.img.basictypeaccess.array.ArrayDataAccess;
import net.imglib2.img.cell.Cell;
import net.imglib2.img.cell.CellGrid;
import net.imglib2.type.NativeType;
import net.imglib2.type.NativeTypeFactory;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Fraction;

import static com.bitplane.xt.util.CellGridUtils.computeCellDimensions;
import static com.bitplane.xt.util.CellGridUtils.createCellGrid;

/**
 * Factory for creating {@link ImarisCachedProbabilitiesImg}s. See
 * {@link ImarisCachedCellImgOptions} for available configuration options and
 * defaults.
 * <p>
 * At each pixel the image is assumed to represent a probability distribution
 * over channels. For storing to Imaris {@code IDataSetPrx} backing cache, the
 * first channel ("background") is removed (only the other channels are stored).
 * If the dataset has UINT8 or UINT16 type, the [0, 1] range is scaled to the
 * [0, 2^8-1] or [0, 2^16-1], respectively. For loading data back from the cache
 * this operation is reversed.
 * <p>
 * Images can only be created with existing Imaris dataset. The number of
 * channels in the Imaris dataset + 1 must be equal to the number of channels in
 * the imglib dimensions.
 * TODO: For now, tt is assumed that in the imglib dimensions, the last dimension represents the channel.
 *
 *
 * @author Tobias Pietzsch
 */
public class ImarisCachedProbabilitiesImgFactory< T extends NativeType< T > > extends NativeImgFactory< T >
{
	private ImarisCachedCellImgOptions factoryOptions;

	private final ImarisApplication imaris;

	// TODO: TEMPORARY, REMOVE (?)
	public ImarisApplication getApplication() {
		return imaris;
	}

	/**
	 * Create a new {@link ImarisCachedProbabilitiesImgFactory} with default configuration.
	 */
	public ImarisCachedProbabilitiesImgFactory( final T type, final ImarisApplication imaris )
	{
		this( type, imaris, ImarisCachedCellImgOptions.options() );
	}

	/**
	 * Create a new {@link ImarisCachedProbabilitiesImgFactory} with the specified
	 * configuration.
	 *
	 * @param optional
	 *            configuration options.
	 */
	public ImarisCachedProbabilitiesImgFactory( final T type, final ImarisApplication imaris, final ImarisCachedCellImgOptions optional )
	{
		super( verifyType( type ) );
		this.imaris = imaris;
		this.factoryOptions = optional;
	}

	private static < T > T verifyType( final T type )
	{
		if ( type instanceof FloatType || type instanceof DoubleType )
			return type;
		throw new IllegalArgumentException( "Only DoubleType and FloatType are supported (not " + type.getClass().getSimpleName() + ")" );
	}

	@Override
	public ImarisCachedProbabilitiesImg< T, ? > create( final long... dimensions )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public ImarisCachedProbabilitiesImg< T, ? > create( final Dimensions dimensions )
	{
		throw new UnsupportedOperationException();
	}

	/**
	 * Create writable image around existing <em>empty</em> Imaris dataset.
	 * <p>
	 * {@code dataset} dimensions and {@code dimensions} must match.
	 * (But {@code dimensions} is allowed to strip dimensions with extent 1.)
	 * <p>
	 * It is assumed that {@code dataset} is empty and cells will be populated from the given {@code loader}.
	 * Existing data in {@code dataset} will thus be potentially overwritten!
	 */
	public ImarisCachedProbabilitiesImg< T, ? > create( final IDataSetPrx dataset, final long[] dimensions, final CellLoader< T > loader )
	{
		return create( dataset, dimensions, null, loader, type(), null );
	}

	public ImarisCachedProbabilitiesImg< T, ? > create( final IDataSetPrx dataset, final long[] dimensions, final CellLoader< T > loader, final ImarisCachedCellImgOptions additionalOptions )
	{
		return create( dataset, dimensions, null, loader, type(), additionalOptions );
	}

	/**
	 * Create writable image around existing Imaris dataset.
	 * <p>
	 * {@code dataset} dimensions and {@code dimensions + 1} must match.
	 * (But {@code dimensions} is allowed to strip dimensions with extent 1.)
	 * <p>
	 * <em>Note that this creates a writable image, and modifying the image will result in modifying the Imaris dataset!
	 * (eventually, when writing back modified data from the cache).
	 * </em>
	 */
	public ImarisCachedProbabilitiesImg< T, ? > create( final IDataSetPrx dataset, final long[] dimensions )
	{
		return create( dataset, dimensions, null, null, type(), null );
	}

	/**
	 * @see #create(IDataSetPrx, long...)
	 */
	// additional options specify cache type, access type, cell dimensions, etc
	public ImarisCachedProbabilitiesImg< T, ? > create( final IDataSetPrx dataset, final long[] dimensions, final ImarisCachedCellImgOptions additionalOptions )
	{
		return create( dataset, dimensions, null, null, type(), additionalOptions );
	}

	@SuppressWarnings( { "unchecked", "rawtypes" } )
	@Override
	public < S > ImgFactory< S > imgFactory( final S type ) throws IncompatibleTypeException
	{
		return new ImarisCachedProbabilitiesImgFactory( ( NativeType ) verifyType( type ), imaris, factoryOptions );
	}

	/**
	 * Create image.
	 *
	 * @param dataset
	 * 		Imaris dataset to use as backing cache.
	 * 		If {@code dataset == null}, a new one is created.
	 * @param dimensions
	 * 		dimensions of the image to create.
	 * 		Must match the dataset dimensions, but is allowed to strip dimensions with extent 1.
	 * @param cacheLoader
	 * 		TODO
	 * @param cellLoader
	 * 		TODO
	 * @param type
	 * 		type of the image to create.
	 * 		assumed to match the Imaris dataset type.
	 * @param additionalOptions
	 * 		additional options that partially override general factory
	 * 		options, or {@code null}.
	 */
	private < A extends DataAccess > ImarisCachedProbabilitiesImg< T, A > create(
			final IDataSetPrx dataset,
			final long[] dimensions,
			final CacheLoader< Long, ? extends Cell< ? extends A > > cacheLoader,
			final CellLoader< T > cellLoader,
			final T type,
			final ImarisCachedCellImgOptions additionalOptions )
	{
		try
		{
			@SuppressWarnings( { "unchecked", "rawtypes" } )
			final ImarisCachedProbabilitiesImg< T, A > img = create(
					dataset,
					cellLoader != null || cacheLoader != null,
					dimensions,
					cacheLoader,
					cellLoader,
					type,
					( NativeTypeFactory ) type.getNativeTypeFactory(),
					additionalOptions );
			return img;
		}
		catch ( Error error )
		{
			throw new ImgLibException( error );
		}
	}

	private < A extends ArrayDataAccess< A > > ImarisCachedProbabilitiesImg< T, ? extends A > create(
			final IDataSetPrx dataset,
			final boolean isEmptyDataset,
			final long[] dimensions,
			final CacheLoader< Long, ? extends Cell< ? > > cacheLoader,
			final CellLoader< T > cellLoader,
			final T type,
			final NativeTypeFactory< T, A > typeFactory,
			final ImarisCachedCellImgOptions additionalOptions ) throws Error
	{
		final int[] imarisDims = {
				dataset.GetSizeX(),
				dataset.GetSizeY(),
				dataset.GetSizeZ(),
				dataset.GetSizeC(),
				dataset.GetSizeT() };
		final int[] mapDimensions = createMapDimensions( imarisDims, dimensions );
		final int[] invMapDimensions = MapDimensions.invertMapDimensions( mapDimensions ) ;

		final ImarisCachedCellImgOptions.Values options = factoryOptions.append( additionalOptions ).values;
		final Fraction entitiesPerPixel = type.getEntitiesPerPixel();
		final int[] cellDimensions = computeCellDimensions( dataset, invMapDimensions, options.cellDimensions() );
		final CellGrid grid = createCellGrid( dimensions, cellDimensions, entitiesPerPixel );

		@SuppressWarnings( "unchecked" )
		CacheLoader< Long, Cell< A > > backingLoader = ( CacheLoader< Long, Cell< A > > ) cacheLoader;
		if ( backingLoader == null )
		{
			if ( cellLoader != null )
			{
				final CellLoader< T > actualCellLoader = options.initializeCellsAsDirty()
						? cell -> {
							cellLoader.load( cell );
							cell.setDirty();
						}
						: cellLoader;
				backingLoader = LoadedCellCacheLoader.get( grid, actualCellLoader, type, options.accessFlags() );
			}
			else if ( isEmptyDataset )
				backingLoader = EmptyCellCacheLoader.get( grid, type, options.accessFlags() );
		}

		@SuppressWarnings( { "rawtypes", "unchecked" } )
		final ImarisProbabilitiesCache< A > imarisCache = options.dirtyAccesses()
				? new ImarisDirtyProbabilitiesCache( dataset, typeFactory.getPrimitiveType(), mapDimensions, grid, backingLoader, options.persistOnLoad() )
				: new ImarisProbabilitiesCache( dataset, typeFactory.getPrimitiveType(), mapDimensions, grid, backingLoader, options.persistOnLoad() );

		final IoSync< Long, Cell< A >, A > iosync = new IoSync<>(
				imarisCache,
				options.numIoThreads(),
				options.maxIoQueueSize() );

		LoaderRemoverCache< Long, Cell< A >, A > listenableCache;
		switch ( options.cacheType() )
		{
		case BOUNDED:
			listenableCache = new GuardedStrongRefLoaderRemoverCache<>( options.maxCacheSize() );
			break;
		case SOFTREF:
		default:
			listenableCache = new SoftRefLoaderRemoverCache<>();
			break;
		}

		final Cache< Long, Cell< A > > cache = listenableCache
				.withRemover( iosync )
				.withLoader( iosync );

		final A accessType = ArrayDataAccessFactory.get( typeFactory, options.accessFlags() );
		final ImarisCachedProbabilitiesImg< T, ? extends A > img = new ImarisCachedProbabilitiesImg<>(
				this,
				dataset,
				grid,
				entitiesPerPixel,
				cache,
				iosync,
				accessType );
		img.setLinkedType( typeFactory.createLinkedType( img ) );
		return img;
	}

	/**
	 * Modified version of {@link MapDimensions#createMapDimensions}.
	 * We assume that the channel dimensions is the last of {@code imgDims}.
	 * All other dimensions are matched in order (XYZT) and by size.
	 */
	private static int[] createMapDimensions( final int[] imarisDims, final long[] imgDims )
	{
		assert imarisDims.length == 5;

		// NB: We assume that the channel dimensions is the last of imgDims
		final int[] mapDimension = new int[ 5 ];
		mapDimension[ 3 ] = imgDims.length - 1;
		if ( imarisDims[ 3 ] != imgDims[ mapDimension[ 3 ] ] - 1 )
			throw new IllegalArgumentException();

		// for everything else: match in order and by size
		int j = 0;
		for ( int i = 0; i < imarisDims.length; ++i )
		{
			if ( i == 3 )
				continue;
			final int si = imarisDims[ i ];
			final long sj = j < ( imgDims.length - 1 ) ? imgDims[ j ] : -1;

			if ( si == sj )
			{
				mapDimension[ i ] = j;
				++j;
			}
			else if ( si == 1 ) // (and sj != 1)
				mapDimension[ i ] = -1;
			else
				throw new IllegalArgumentException( "image dimensions do not match dataset dimensions" );
		}
		return mapDimension;
	}

	// -- deprecated API --

	@Override
	@Deprecated
	public NativeImg< T, ? > create( final long[] dimension, final T type )
	{
		throw new UnsupportedOperationException();
	}
}
