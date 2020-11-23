package com.bitplane.xt;

import Imaris.Error;
import Imaris.IApplicationPrx;
import Imaris.IDataSetPrx;
import Imaris.IFactoryPrx;
import Imaris.tType;
import java.util.Arrays;
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

import static Imaris.tType.eTypeFloat;
import static Imaris.tType.eTypeUInt16;
import static Imaris.tType.eTypeUInt8;

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

	private final ImarisService imaris;

	/**
	 * Create a new {@link ImarisCachedCellImgFactory} with default configuration.
	 */
	public ImarisCachedCellImgFactory( final T type, final ImarisService imaris )
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
	public ImarisCachedCellImgFactory( final T type, final ImarisService imaris, final ImarisCachedCellImgOptions optional )
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

	private static tType imarisType( final Object type )
	{
		if ( type instanceof UnsignedByteType )
			return eTypeUInt8;
		else if ( type instanceof UnsignedShortType )
			return eTypeUInt16;
		else if ( type instanceof FloatType )
			return eTypeFloat;
		else
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
					dataset == null,
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
		final int[] mapDimensions = createMapDimensions( imarisDims, dimensions );
		final int[] invMapDimensions = invertMapDimensions( mapDimensions, dimensions.length ) ;

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
		final ImarisCellCache< A > imarisCache = options.dirtyAccesses()
				? new ImarisDirtyCellCache( dataset, mapDimensions, grid, backingLoader )
				: new ImarisCellCache( dataset, mapDimensions, grid, backingLoader );

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

	/**
	 * Tries to derive a {@code mapDimensions} array matching the specified Imaris and imglib2 dimension arrays.
	 * <p>
	 * {@code mapDimensions} maps Imaris dimension indices to imglib2 dimension indices.
	 * If {@code i} is dimension index from Imaris (0..4 means X,Y,Z,C,T)
	 * then {@code mapDimensions[i]} is the corresponding dimension in {@code img}.
	 * For {@code img} dimensions with size=1 may be skipped.
	 * E.g., for a X,Y,C image {@code mapDimensions = {0,1,-1,2,-1}}.
	 *
	 * @param imarisDims
	 * 		dimensions of the Imaris dataset ({@code int[5]}, with X,Y,Z,C,T)
	 * @param imgDims
	 * 		dimensions of the imglib2 image
	 *
	 * @return {@code mapDimensions} array
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

	private static int[] invertMapDimensions( final int[] mapDimensions, final int n )
	{
		final int[] invMapDimensions = new int[ n ];
		Arrays.fill( invMapDimensions, -1 );
		for ( int i = 0; i < mapDimensions.length; i++ )
		{
			final int si = mapDimensions[ i ];
			if ( si >= 0 )
				invMapDimensions[ si ] = i;
		}
		return invMapDimensions;
	}

	private CellGrid createCellGrid(
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
		// Verify that numDimensions < 5.
		// Verify that each for each dimension 0 < dim < Integer.MAX_VALUE.
		Dimensions.verifyAllPositive( dimensions );
		final int n = dimensions.length;
		if ( n > 5 )
			throw new IllegalArgumentException( "image must not have more than 5 dimensions" );
		for ( int i = 0; i < dimensions.length; i++ )
			if ( dimensions[ i ] > Integer.MAX_VALUE )
				throw new IllegalArgumentException( "each individual image dimension must be < 2^31" );

		// Get Imaris dimensions from dimensions: Just add "1" to fill up to 5D
		final int[] imarisDims = new int[ 5 ];
		for ( int i = 0; i < imarisDims.length; i++ )
			imarisDims[ i ] = i < n ? ( int ) dimensions[ i ] : 1;

		final int sx = imarisDims[ 0 ];
		final int sy = imarisDims[ 1 ];
		final int sz = imarisDims[ 2 ];
		final int sc = imarisDims[ 3 ];
		final int st = imarisDims[ 4 ];

		// Create Imaris dataset
		final IApplicationPrx app = imaris.app();
		final IFactoryPrx factory = app.GetFactory();
		final IDataSetPrx dataset = factory.CreateDataSet();

		dataset.Create( imarisType( type() ), sx, sy, sz, sc, st );
		dataset.SetExtendMinX( 0 );
		dataset.SetExtendMaxX( sx );
		dataset.SetExtendMinY( 0 );
		dataset.SetExtendMaxY( sy );
		dataset.SetExtendMinZ( 0 );
		dataset.SetExtendMaxZ( sz );

		return dataset;
	}

	// -- deprecated API --

	@Override
	@Deprecated
	public NativeImg< T, ? > create( final long[] dimension, final T type )
	{
		throw new UnsupportedOperationException();
	}
}
