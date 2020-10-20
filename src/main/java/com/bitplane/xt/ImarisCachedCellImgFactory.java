package com.bitplane.xt;

import Imaris.Error;
import Imaris.IDataSetPrx;
import java.nio.file.Path;
import net.imglib2.Dimensions;
import net.imglib2.cache.Cache;
import net.imglib2.cache.CacheLoader;
import net.imglib2.cache.IoSync;
import net.imglib2.cache.LoaderRemoverCache;
import net.imglib2.cache.img.AccessIo;
import net.imglib2.cache.img.DirtyDiskCellCache;
import net.imglib2.cache.img.DiskCellCache;
import net.imglib2.cache.ref.GuardedStrongRefLoaderRemoverCache;
import net.imglib2.cache.ref.SoftRefLoaderRemoverCache;
import net.imglib2.exception.IncompatibleTypeException;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.NativeImg;
import net.imglib2.img.NativeImgFactory;
import net.imglib2.img.basictypeaccess.ArrayDataAccessFactory;
import net.imglib2.img.basictypeaccess.array.ArrayDataAccess;
import net.imglib2.img.cell.Cell;
import net.imglib2.img.cell.CellGrid;
import net.imglib2.img.cell.CellImgFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.NativeTypeFactory;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Fraction;
import net.imglib2.util.Intervals;
import net.imglib2.util.Util;

/**
 * Factory for creating {@link ImarisCachedCellImg}s. See
 * {@link ImarisCachedCellImgOptions} for available configuration options and
 * defaults.
 *
 * @author Tobias Pietzsch
 */
public class ImarisCachedCellImgFactory< T extends NativeType< T > > extends NativeImgFactory< T >
{
	private ImarisCachedCellImgOptions factoryOptions;

	/**
	 * Create a new {@link ImarisCachedCellImgFactory} with default configuration.
	 */
	public ImarisCachedCellImgFactory( final T type )
	{
		this( type, ImarisCachedCellImgOptions.options() );
	}

	/**
	 * Create a new {@link ImarisCachedCellImgFactory} with the specified
	 * configuration.
	 *
	 * @param optional
	 *            configuration options.
	 */
	public ImarisCachedCellImgFactory( final T type, final ImarisCachedCellImgOptions optional )
	{
		super( verifyType( type ) );
		this.factoryOptions = optional;
	}

	private static < T > T verifyType( final T type )
	{
		if ( type instanceof UnsignedByteType || type instanceof UnsignedShortType || type instanceof FloatType )
			return type;
		throw new IllegalArgumentException( "Only UnsignedByteType, UnsignedShortType, FloatType are supported (not " + type.getClass().getSimpleName() + ")" );
	}

	@Override
	public ImarisCachedCellImg< T, ? > create( final long... dimensions )
	{
		return create( dimensions, type(), null );
	}

	@Override
	public ImarisCachedCellImg< T, ? > create( final Dimensions dimensions )
	{
		return create( Intervals.dimensionsAsLongArray( dimensions ) );
	}

	@Override
	public ImarisCachedCellImg< T, ? > create( final int[] dimensions )
	{
		return create( Util.int2long( dimensions ) );
	}

	@Override
	@Deprecated
	public NativeImg< T, ? > create( final long[] dimension, final T type )
	{
		throw new UnsupportedOperationException();
	}


	private < A > ImarisCachedCellImg< T, A > create(
			final IDataSetPrx dataset,
			final long[] dimensions,
			final T type,
			final ImarisCachedCellImgOptions additionalOptions ) throws Error
	{
		final int[] imarisDims = {
				dataset.GetSizeX(),
				dataset.GetSizeY(),
				dataset.GetSizeZ(),
				dataset.GetSizeC(),
				dataset.GetSizeT() };
		final int[] mapDimensions = createMapDimensions( imarisDims, dimensions );
	}

	/**
	 * Tries to derive a {@code mapDimensions} array matching the specified Imaris and imglib2 dimension arrays.
	 * <p>
	 * {@code mapDimensions} maps Imaris dimension indices to imglib2 dimension indices.
	 * If {@code i} is dimension index from Imaris (0..4 means X,Y,Z,C,T)
	 * then {@code mapDimensions[i]} is the corresponding dimension in {@code img}.
	 * For {@code img} dimensions with size=1 may be skipped.
	 * E.g., for a X,Y,C image {@code mapDimensions = {0,1,-1,2,-1}}.
	 */
	private static int[] createMapDimensions( final int[] imarisDims, final long[] imgDims )
	{
		assert imarisDims.length == 5;

		final int[] mapDimension = new int[ 5 ];
		int j = 0;
		for ( int i = 0; i < imarisDims.length; ++i )
		{
			final int si = imarisDims[ i ];
			final long sj = j < imgDims.length ? imgDims[ j ] : -1;

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


	/**
	 * Create image.
	 *
	 * @param dimensions
	 *            dimensions of the image to create.
	 * @param type
	 *            type of the image to create
	 * @param additionalOptions
	 *            additional options that partially override general factory
	 *            options, or {@code null}.
	 */
	private < A > ImarisCachedCellImg< T, A > create(
			final long[] dimensions,
			final T type,
			final ImarisCachedCellImgOptions additionalOptions )
	{
		Dimensions.verify( dimensions );
		@SuppressWarnings( { "unchecked", "rawtypes" } )
		final ImarisCachedCellImg< T, A > img = create( dimensions, type, ( NativeTypeFactory ) type.getNativeTypeFactory(), additionalOptions );
		return img;
	}

	private < A extends ArrayDataAccess< A > > ImarisCachedCellImg< T, ? extends A > create(
			final long[] dimensions,
			final T type,
			final NativeTypeFactory< T, A > typeFactory,
			final ImarisCachedCellImgOptions additionalOptions )
	{
		final ImarisCachedCellImgOptions.Values options = factoryOptions.append( additionalOptions ).values;

		final Fraction entitiesPerPixel = type.getEntitiesPerPixel();

		final CellGrid grid = createCellGrid( dimensions, entitiesPerPixel, options );

		CacheLoader< Long, Cell< A > > backingLoader = null;
		final Path blockcache = null;

		@SuppressWarnings( { "rawtypes", "unchecked" } )
		final DiskCellCache< A > diskcache = options.dirtyAccesses()
				? new DirtyDiskCellCache(
						blockcache, grid, backingLoader,
						AccessIo.get( type, options.accessFlags() ),
						entitiesPerPixel )
				: new DiskCellCache<>(
						blockcache, grid, backingLoader,
						AccessIo.get( type, options.accessFlags() ),
						entitiesPerPixel );

		final IoSync< Long, Cell< A >, A > iosync = new IoSync<>(
				diskcache,
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
		final ImarisCachedCellImg< T, ? extends A > img = new ImarisCachedCellImg<>(
				this,
				grid,
				entitiesPerPixel,
				cache,
				iosync,
				accessType );
		img.setLinkedType( typeFactory.createLinkedType( img ) );
		return img;
	}

	@SuppressWarnings( { "unchecked", "rawtypes" } )
	@Override
	public < S > ImgFactory< S > imgFactory( final S type ) throws IncompatibleTypeException
	{
		if ( type instanceof UnsignedByteType || type instanceof UnsignedShortType || type instanceof FloatType )
			return new ImarisCachedCellImgFactory( ( NativeType ) type, factoryOptions );
		throw new IncompatibleTypeException( this, "Only UnsignedByteType, UnsignedShortType, FloatType are supported (not " + type.getClass().getSimpleName() + ")" );
	}

	private CellGrid createCellGrid( final long[] dimensions, final Fraction entitiesPerPixel, final ImarisCachedCellImgOptions.Values options )
	{
		Dimensions.verify( dimensions );
		final int n = dimensions.length;
		final int[] cellDimensions = CellImgFactory.getCellDimensions( options.cellDimensions(), n, entitiesPerPixel );
		return new CellGrid( dimensions, cellDimensions );
	}
}
