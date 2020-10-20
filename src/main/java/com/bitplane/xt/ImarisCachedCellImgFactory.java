package com.bitplane.xt;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.imglib2.Dimensions;
import net.imglib2.cache.Cache;
import net.imglib2.cache.CacheLoader;
import net.imglib2.cache.IoSync;
import net.imglib2.cache.LoaderRemoverCache;
import net.imglib2.cache.img.AccessIo;
import net.imglib2.cache.img.CellLoader;
import net.imglib2.cache.img.DirtyDiskCellCache;
import net.imglib2.cache.img.DiskCachedCellImgOptions;
import net.imglib2.cache.img.DiskCellCache;
import net.imglib2.cache.img.EmptyCellCacheLoader;
import net.imglib2.cache.img.LoadedCellCacheLoader;
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
 * {@link DiskCachedCellImgOptions} for available configuration options and
 * defaults.
 *
 * @author Tobias Pietzsch
 */
public class ImarisCachedCellImgFactory< T extends NativeType< T > > extends NativeImgFactory< T >
{
	private DiskCachedCellImgOptions factoryOptions;

	/**
	 * Create a new {@link ImarisCachedCellImgFactory} with default configuration.
	 */
	public ImarisCachedCellImgFactory( final T type )
	{
		this( type, DiskCachedCellImgOptions.options() );
	}

	/**
	 * Create a new {@link ImarisCachedCellImgFactory} with the specified
	 * configuration.
	 *
	 * @param optional
	 *            configuration options.
	 */
	public ImarisCachedCellImgFactory( final T type, final DiskCachedCellImgOptions optional )
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
		return create( dimensions, null, null, type(), null );
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
	public NativeImg< T, ? > create( final long[] dimension, final T type )
	{
		// TODO (?)
		throw new UnsupportedOperationException("TODO (?)");
	}

	public ImarisCachedCellImg< T, ? > create( final long[] dimensions, final DiskCachedCellImgOptions additionalOptions )
	{
		return create( dimensions, null, null, type(), additionalOptions );
	}

	public ImarisCachedCellImg< T, ? > create( final Dimensions dimensions, final DiskCachedCellImgOptions additionalOptions )
	{
		return create( Intervals.dimensionsAsLongArray( dimensions ), additionalOptions );
	}

	public ImarisCachedCellImg< T, ? > create( final long[] dimensions, final CellLoader< T > loader )
	{
		return create( dimensions, null, loader, type(), null );
	}

	public ImarisCachedCellImg< T, ? > create( final Dimensions dimensions, final CellLoader< T > loader )
	{
		return create( Intervals.dimensionsAsLongArray( dimensions ), null, loader, type(), null );
	}

	public ImarisCachedCellImg< T, ? > create( final long[] dimensions, final CellLoader< T > loader, final DiskCachedCellImgOptions additionalOptions )
	{
		return create( dimensions, null, loader, type(), additionalOptions );
	}

	public ImarisCachedCellImg< T, ? > create( final Dimensions dimensions, final CellLoader< T > loader, final DiskCachedCellImgOptions additionalOptions )
	{
		return create( Intervals.dimensionsAsLongArray( dimensions ), null, loader, type(), additionalOptions );
	}

	public < A > ImarisCachedCellImg< T, A > createWithCacheLoader( final long[] dimensions, final CacheLoader< Long, Cell< A > > backingLoader )
	{
		return create( dimensions, backingLoader, null, type(), null );
	}

	public < A > ImarisCachedCellImg< T, A > createWithCacheLoader( final Dimensions dimensions, final CacheLoader< Long, Cell< A > > backingLoader )
	{
		return create( Intervals.dimensionsAsLongArray( dimensions ), backingLoader, null, type(), null );
	}

	public < A > ImarisCachedCellImg< T, A > createWithCacheLoader( final long[] dimensions, final CacheLoader< Long, Cell< A > > backingLoader, final DiskCachedCellImgOptions additionalOptions )
	{
		return create( dimensions, backingLoader, null, type(), additionalOptions );
	}

	public < A > ImarisCachedCellImg< T, A > createWithCacheLoader( final Dimensions dimensions, final CacheLoader< Long, Cell< A > > backingLoader, final DiskCachedCellImgOptions additionalOptions )
	{
		return create( Intervals.dimensionsAsLongArray( dimensions ), backingLoader, null, type(), additionalOptions );
	}

	/**
	 * Create image.
	 *
	 * @param dimensions
	 *            dimensions of the image to create.
	 * @param cacheLoader
	 *            user-specified backing loader or {@code null}.
	 * @param cellLoader
	 *            user-specified {@link CellLoader} or {@code null}. Has no
	 *            effect if {@code cacheLoader != null}.
	 * @param type
	 *            type of the image to create
	 * @param additionalOptions
	 *            additional options that partially override general factory
	 *            options, or {@code null}.
	 */
	private < A > ImarisCachedCellImg< T, A > create(
			final long[] dimensions,
			final CacheLoader< Long, ? extends Cell< ? extends A > > cacheLoader,
			final CellLoader< T > cellLoader,
			final T type,
			final DiskCachedCellImgOptions additionalOptions )
	{
		Dimensions.verify( dimensions );
		@SuppressWarnings( { "unchecked", "rawtypes" } )
		final ImarisCachedCellImg< T, A > img = create( dimensions, cacheLoader, cellLoader, type, ( NativeTypeFactory ) type.getNativeTypeFactory(), additionalOptions );
		return img;
	}

	private < A extends ArrayDataAccess< A > > ImarisCachedCellImg< T, ? extends A > create(
			final long[] dimensions,
			final CacheLoader< Long, ? extends Cell< ? > > cacheLoader,
			final CellLoader< T > cellLoader,
			final T type,
			final NativeTypeFactory< T, A > typeFactory,
			final DiskCachedCellImgOptions additionalOptions )
	{
		final DiskCachedCellImgOptions.Values options = factoryOptions.append( additionalOptions ).values;

		final Fraction entitiesPerPixel = type.getEntitiesPerPixel();

		final CellGrid grid = createCellGrid( dimensions, entitiesPerPixel, options );

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
			else
				backingLoader = EmptyCellCacheLoader.get( grid, type, options.accessFlags() );
		}

		final Path blockcache = createBlockCachePath( options );

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
		if ( NativeType.class.isInstance( type ) )
			return new ImarisCachedCellImgFactory( ( NativeType ) type, factoryOptions );
		throw new IncompatibleTypeException( this, type.getClass().getCanonicalName() + " does not implement NativeType." );
	}

	private CellGrid createCellGrid( final long[] dimensions, final Fraction entitiesPerPixel, final DiskCachedCellImgOptions.Values options )
	{
		Dimensions.verify( dimensions );
		final int n = dimensions.length;
		final int[] cellDimensions = CellImgFactory.getCellDimensions( options.cellDimensions(), n, entitiesPerPixel );
		return new CellGrid( dimensions, cellDimensions );
	}

	private Path createBlockCachePath( final DiskCachedCellImgOptions.Values options )
	{
		try
		{
			final Path cache = options.cacheDirectory();
			final Path dir = options.tempDirectory();
			final String prefix = options.tempDirectoryPrefix();
			final boolean deleteOnExit = options.deleteCacheDirectoryOnExit();
			if ( cache != null )
			{
				if ( !Files.isDirectory( cache ) )
				{
					Files.createDirectories( cache );
					if ( deleteOnExit )
						DiskCellCache.addDeleteHook( cache );
				}
				return cache;
			}
			else if ( dir != null )
				return DiskCellCache.createTempDirectory( dir, prefix, deleteOnExit );
			else
				return DiskCellCache.createTempDirectory( prefix, deleteOnExit );
		}
		catch ( final IOException e )
		{
			throw new RuntimeException( e );
		}
	}
}
