package com.bitplane.xt;

import Imaris.Error;
import Imaris.IDataSetPrx;
import bdv.util.AxisOrder;
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
import net.imglib2.img.basictypeaccess.array.ArrayDataAccess;
import net.imglib2.img.cell.Cell;
import net.imglib2.img.cell.CellGrid;
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

	private final ImarisApplication imaris;

	// TODO: TEMPORARY, REMOVE (?)
	public ImarisApplication getApplication() {
		return imaris;
	}

	/**
	 * Create a new {@link ImarisCachedCellImgFactory} with default configuration.
	 */
	public ImarisCachedCellImgFactory( final T type, final ImarisApplication imaris )
	{
		this( type, imaris, ImarisCachedCellImgOptions.options() );
	}

	/**
	 * Create a new {@link ImarisCachedCellImgFactory} with the specified
	 * configuration.
	 *
	 * @param optional
	 *            configuration options.
	 */
	public ImarisCachedCellImgFactory( final T type, final ImarisApplication imaris, final ImarisCachedCellImgOptions optional )
	{
		super( verifyType( type ) );
		this.imaris = imaris;
		this.factoryOptions = optional;
	}

	private static < T > T verifyType( final T type )
	{
		if ( type instanceof UnsignedByteType || type instanceof UnsignedShortType || type instanceof FloatType )
			return type;
		throw new IllegalArgumentException( "Only UnsignedByteType, UnsignedShortType, FloatType are supported (not " + type.getClass().getSimpleName() + ")" );
	}

	// creates new Imaris dataset
	// initializes cells as empty
	@Override
	public ImarisCachedCellImg< T, ? > create( final long... dimensions )
	{
		return create( null, dimensions, null, null, type(), null );
	}

	// creates new Imaris dataset
	// initializes cells as empty
	@Override
	public ImarisCachedCellImg< T, ? > create( final Dimensions dimensions )
	{
		return create( Intervals.dimensionsAsLongArray( dimensions ) );
	}

	// creates new Imaris dataset
	// initializes cells as empty
	@Override
	public ImarisCachedCellImg< T, ? > create( final int[] dimensions )
	{
		return create( Util.int2long( dimensions ) );
	}

	// creates new Imaris dataset
	// initializes cells as empty
	// additional options specify cache type, access type, cell dimensions, etc
	public ImarisCachedCellImg< T, ? > create( final long[] dimensions, final ImarisCachedCellImgOptions additionalOptions )
	{
		return create( null, dimensions, null, null, type(), additionalOptions );
	}

	// creates new Imaris dataset
	// initializes cells as empty
	// additional options specify cache type, access type, cell dimensions, etc
	public ImarisCachedCellImg< T, ? > create( final Dimensions dimensions, final ImarisCachedCellImgOptions additionalOptions )
	{
		return create( null, Intervals.dimensionsAsLongArray( dimensions ), null, null, type(), additionalOptions );
	}

	// creates new Imaris dataset
	// initializes cells using the given loader
	// once loaded, cells are pushed to Imaris when evicted, and retrieved from Imaris when they are accessed again.
	public ImarisCachedCellImg< T, ? > create( final long[] dimensions, final CellLoader< T > loader )
	{
		return create( null, dimensions, null, loader, type(), null );
	}

	// creates new Imaris dataset
	// initializes cells using the given loader
	// once loaded, cells are pushed to Imaris when evicted, and retrieved from Imaris when they are accessed again.
	public ImarisCachedCellImg< T, ? > create( final Dimensions dimensions, final CellLoader< T > loader )
	{
		return create( null, Intervals.dimensionsAsLongArray( dimensions ), null, loader, type(), null );
	}

	// creates new Imaris dataset
	// initializes cells using the given loader
	// once loaded, cells are pushed to Imaris when evicted, and retrieved from Imaris when they are accessed again.
	// additional options specify cache type, access type, cell dimensions, etc
	public ImarisCachedCellImg< T, ? > create( final long[] dimensions, final CellLoader< T > loader, final ImarisCachedCellImgOptions additionalOptions )
	{
		return create( null, dimensions, null, loader, type(), additionalOptions );
	}

	// creates new Imaris dataset
	// initializes cells using the given loader
	// once loaded, cells are pushed to Imaris when evicted, and retrieved from Imaris when they are accessed again.
	// additional options specify cache type, access type, cell dimensions, etc
	public ImarisCachedCellImg< T, ? > create( final Dimensions dimensions, final CellLoader< T > loader, final ImarisCachedCellImgOptions additionalOptions )
	{
		return create( null, Intervals.dimensionsAsLongArray( dimensions ), null, loader, type(), additionalOptions );
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
	// creates new Imaris dataset
	// initializes cells using the given loader
	// once loaded, cells are pushed to Imaris when evicted, and retrieved from Imaris when they are accessed again.
	public ImarisCachedCellImg< T, ? > create( final IDataSetPrx dataset, final long[] dimensions, final CellLoader< T > loader )
	{
		return create( dataset, dimensions, null, loader, type(), null );
	}

	// creates new Imaris dataset
	// initializes cells using the given loader
	// once loaded, cells are pushed to Imaris when evicted, and retrieved from Imaris when they are accessed again.
	public ImarisCachedCellImg< T, ? > create( final IDataSetPrx dataset, final Dimensions dimensions, final CellLoader< T > loader )
	{
		return create( dataset, Intervals.dimensionsAsLongArray( dimensions ), null, loader, type(), null );
	}

	// creates new Imaris dataset
	// initializes cells using the given loader
	// once loaded, cells are pushed to Imaris when evicted, and retrieved from Imaris when they are accessed again.
	// additional options specify cache type, access type, cell dimensions, etc
	public ImarisCachedCellImg< T, ? > create( final IDataSetPrx dataset, final long[] dimensions, final CellLoader< T > loader, final ImarisCachedCellImgOptions additionalOptions )
	{
		return create( dataset, dimensions, null, loader, type(), additionalOptions );
	}

	// creates new Imaris dataset
	// initializes cells using the given loader
	// once loaded, cells are pushed to Imaris when evicted, and retrieved from Imaris when they are accessed again.
	// additional options specify cache type, access type, cell dimensions, etc
	public ImarisCachedCellImg< T, ? > create( final IDataSetPrx dataset, final Dimensions dimensions, final CellLoader< T > loader, final ImarisCachedCellImgOptions additionalOptions )
	{
		return create( dataset, Intervals.dimensionsAsLongArray( dimensions ), null, loader, type(), additionalOptions );
	}








	// creates new Imaris dataset
	// initializes cells using the given backingLoader
	// once loaded, cells are pushed to Imaris when evicted, and retrieved from Imaris when they are accessed again.
	public < A > ImarisCachedCellImg< T, A > createWithCacheLoader( final long[] dimensions, final CacheLoader< Long, Cell< A > > backingLoader )
	{
		return create( null, dimensions, backingLoader, null, type(), null );
	}

	// creates new Imaris dataset
	// initializes cells using the given backingLoader
	// once loaded, cells are pushed to Imaris when evicted, and retrieved from Imaris when they are accessed again.
	public < A > ImarisCachedCellImg< T, A > createWithCacheLoader( final Dimensions dimensions, final CacheLoader< Long, Cell< A > > backingLoader )
	{
		return create( null, Intervals.dimensionsAsLongArray( dimensions ), backingLoader, null, type(), null );
	}

	// creates new Imaris dataset
	// initializes cells using the given backingLoader
	// once loaded, cells are pushed to Imaris when evicted, and retrieved from Imaris when they are accessed again.
	// additional options specify cache type, access type, cell dimensions, etc
	public < A > ImarisCachedCellImg< T, A > createWithCacheLoader( final long[] dimensions, final CacheLoader< Long, Cell< A > > backingLoader, final ImarisCachedCellImgOptions additionalOptions )
	{
		return create( null, dimensions, backingLoader, null, type(), additionalOptions );
	}

	// creates new Imaris dataset
	// initializes cells using the given backingLoader
	// once loaded, cells are pushed to Imaris when evicted, and retrieved from Imaris when they are accessed again.
	// additional options specify cache type, access type, cell dimensions, etc
	public < A > ImarisCachedCellImg< T, A > createWithCacheLoader( final Dimensions dimensions, final CacheLoader< Long, Cell< A > > backingLoader, final ImarisCachedCellImgOptions additionalOptions )
	{
		return create( null, Intervals.dimensionsAsLongArray( dimensions ), backingLoader, null, type(), additionalOptions );
	}

	/**
	 * Create writable image around existing Imaris dataset.
	 * <p>
	 * {@code dataset} dimensions and {@code dimensions} must match.
	 * (But {@code dimensions} is allowed to strip dimensions with extent 1.)
	 * <p>
	 * <em>Note that this creates a writable image, and modifying the image will result in modifying the Imaris dataset!
	 * (eventually, when writing back modified data from the cache).
	 * </em>
	 */
	public ImarisCachedCellImg< T, ? > create( final IDataSetPrx dataset, final long... dimensions )
	{
		return create( dataset, dimensions, null, null, type(), null );
	}

	/**
	 * @see #create(IDataSetPrx, long...)
	 */
	public ImarisCachedCellImg< T, ? > create( final IDataSetPrx dataset, final Dimensions dimensions )
	{
		return create( dataset, Intervals.dimensionsAsLongArray( dimensions ), null, null, type(), null );
	}

	/**
	 * @see #create(IDataSetPrx, long...)
	 */
	// additional options specify cache type, access type, cell dimensions, etc
	public ImarisCachedCellImg< T, ? > create( final IDataSetPrx dataset, final long[] dimensions, final ImarisCachedCellImgOptions additionalOptions )
	{
		return create( dataset, dimensions, null, null, type(), additionalOptions );
	}

	/**
	 * @see #create(IDataSetPrx, long...)
	 */
	// additional options specify cache type, access type, cell dimensions, etc
	public ImarisCachedCellImg< T, ? > create( final IDataSetPrx dataset, final Dimensions dimensions, final ImarisCachedCellImgOptions additionalOptions )
	{
		return create( dataset, Intervals.dimensionsAsLongArray( dimensions ), null, null, type(), additionalOptions );
	}


	// TODO: Add missing create methods:
	//  with IDataSetPrx dataset and CacheLoader
	//  with IDataSetPrx dataset and CellLoader
	//  with IDataSetPrx dataset and CacheLoader and additionalOptions
	//  with IDataSetPrx dataset and CellLoader and additionalOptions
	//  (each of those for long[] and for Dimensions


	@SuppressWarnings( { "unchecked", "rawtypes" } )
	@Override
	public < S > ImgFactory< S > imgFactory( final S type ) throws IncompatibleTypeException
	{
		return new ImarisCachedCellImgFactory( ( NativeType ) verifyType( type ), imaris, factoryOptions );
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
	private < A > ImarisCachedCellImg< T, A > create(
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
			final ImarisCachedCellImg< T, A > img = create(
					dataset != null ? dataset : createDataset( dimensions ),
					dataset == null || cellLoader != null || cacheLoader != null,
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

	private < A extends ArrayDataAccess< A > > ImarisCachedCellImg< T, ? extends A > create(
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
		final int[] mapDimensions = MapDimensions.createMapDimensions( imarisDims, dimensions );
		final int[] invMapDimensions = MapDimensions.invertMapDimensions( mapDimensions ) ;

		final ImarisCachedCellImgOptions.Values options = factoryOptions.append( additionalOptions ).values;
		final Fraction entitiesPerPixel = type.getEntitiesPerPixel();
		final CellGrid grid = createCellGrid( dimensions, invMapDimensions, entitiesPerPixel, options );

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
		final ImarisLoaderRemover< A > imarisCache = options.dirtyAccesses()
				? new ImarisDirtyLoaderRemover( dataset, mapDimensions, grid, backingLoader, options.persistOnLoad() )
				: new ImarisLoaderRemover( dataset, mapDimensions, grid, backingLoader, options.persistOnLoad() );

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
		final ImarisCachedCellImg< T, ? extends A > img = new ImarisCachedCellImg<>(
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

	// TODO: move to util?
	//   (temporarily made public)
	public static CellGrid createCellGrid(
			final long[] dimensions,
			final int[] invMapDimensions,
			final Fraction entitiesPerPixel,
			final ImarisCachedCellImgOptions.Values options )
	{
		final int n = dimensions.length;
		final int[] cellDimensions = new int[ n ];

		final int[] defaultCellDimensions = options.cellDimensions();
		final int max = defaultCellDimensions.length - 1;
		for ( int i = 0; i < n; i++ )
		{
			cellDimensions[ i ] = defaultCellDimensions[ Math.min( i, max ) ];
			if ( invMapDimensions[ i ] < 0 )
				cellDimensions[ i ] = 1;
		}

		final long numEntities = entitiesPerPixel.mulCeil( Intervals.numElements( cellDimensions ) );
		if ( numEntities > Integer.MAX_VALUE )
			throw new IllegalArgumentException( "Number of entities in cell too large. Use smaller cell size." );

		return new CellGrid( dimensions, cellDimensions );
	}

	private IDataSetPrx createDataset( final long... dimensions ) throws Error
	{
		final AxisOrder axisOrder;
		switch ( dimensions.length )
		{
		case 2:
			axisOrder = AxisOrder.XY;
			break;
		case 3:
			axisOrder = AxisOrder.XYZ;
			break;
		case 4:
			axisOrder = AxisOrder.XYZC;
			break;
		case 5:
			axisOrder = AxisOrder.XYZCT;
			break;
		default:
			throw new IllegalArgumentException( "image must not have more than 5 or less than 2 dimensions" );
		}
		return ImarisUtils.createDataset( imaris.getIApplicationPrx(), ImarisUtils.imarisTypeFor( type() ), axisOrder, dimensions );
	}

	// -- deprecated API --

	@Override
	@Deprecated
	public NativeImg< T, ? > create( final long[] dimension, final T type )
	{
		throw new UnsupportedOperationException();
	}
}
