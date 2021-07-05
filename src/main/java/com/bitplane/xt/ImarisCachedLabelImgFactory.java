package com.bitplane.xt;

import Imaris.Error;
import Imaris.IDataSetPrx;
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
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.util.Fraction;

import static com.bitplane.xt.ImarisCachedCellImgFactory.createCellGrid;
import static com.bitplane.xt.ImarisCachedCellImgFactory.invertMapDimensions;

/**
 * Factory for creating {@link ImarisCachedLabelImg}s. See
 * {@link ImarisCachedCellImgOptions} for available configuration options and
 * defaults.
 * <p>
 * Integer labels on the ImgLib2 side are translated into channels on the Imaris side.
 * Therefore, because the channel dimension is used to decompose the values (values), this factory can create at most 4D images.
 * <p>
 * Images can only be created with existing Imaris dataset.
 *
 * @author Tobias Pietzsch
 */
public class ImarisCachedLabelImgFactory< T extends NativeType< T > > extends NativeImgFactory< T >
{
	private ImarisCachedCellImgOptions factoryOptions;

	private final ImarisApplication imaris;

	// TODO: TEMPORARY, REMOVE (?)
	public ImarisApplication getApplication() {
		return imaris;
	}

	/**
	 * Create a new {@link ImarisCachedLabelImgFactory} with default configuration.
	 */
	public ImarisCachedLabelImgFactory( final T type, final ImarisApplication imaris )
	{
		this( type, imaris, ImarisCachedCellImgOptions.options() );
	}

	/**
	 * Create a new {@link ImarisCachedLabelImgFactory} with the specified
	 * configuration.
	 *
	 * @param optional
	 *            configuration options.
	 */
	public ImarisCachedLabelImgFactory( final T type, final ImarisApplication imaris, final ImarisCachedCellImgOptions optional )
	{
		super( verifyType( type ) );
		this.imaris = imaris;
		this.factoryOptions = optional;
	}

	private static < T > T verifyType( final T type )
	{
		if ( type instanceof NativeType && type instanceof IntegerType )
			return type;
		throw new IllegalArgumentException( "Only types implementing NativeType & IntegerType are supported (not " + type.getClass().getSimpleName() + ")" );
	}

	@Override
	public ImarisCachedLabelImg< T, ? > create( final long... dimensions )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public ImarisCachedLabelImg< T, ? > create( final Dimensions dimensions )
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
	public ImarisCachedLabelImg< T, ? > create( final IDataSetPrx dataset, final long[] dimensions, final CellLoader< T > loader )
	{
		return create( dataset, dimensions, null, loader, type(), null );
	}

	public ImarisCachedLabelImg< T, ? > create( final IDataSetPrx dataset, final long[] dimensions, final CellLoader< T > loader, final ImarisCachedCellImgOptions additionalOptions )
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
	public ImarisCachedLabelImg< T, ? > create( final IDataSetPrx dataset, final long[] dimensions )
	{
		return create( dataset, dimensions, null, null, type(), null );
	}

	/**
	 * @see #create(IDataSetPrx, long...)
	 */
	// additional options specify cache type, access type, cell dimensions, etc
	public ImarisCachedLabelImg< T, ? > create( final IDataSetPrx dataset, final long[] dimensions, final ImarisCachedCellImgOptions additionalOptions )
	{
		return create( dataset, dimensions, null, null, type(), additionalOptions );
	}

	@SuppressWarnings( { "unchecked", "rawtypes" } )
	@Override
	public < S > ImgFactory< S > imgFactory( final S type ) throws IncompatibleTypeException
	{
		return new ImarisCachedLabelImgFactory( ( NativeType ) verifyType( type ), imaris, factoryOptions );
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
	private < A > ImarisCachedLabelImg< T, A > create(
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
			final ImarisCachedLabelImg< T, A > img = create(
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

	private < A extends ArrayDataAccess< A > > ImarisCachedLabelImg< T, ? extends A > create(
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
		final ImarisLabelCache< A > imarisCache = options.dirtyAccesses()
				? new ImarisDirtyLabelCache( dataset, typeFactory.getPrimitiveType(), mapDimensions, grid, backingLoader, options.persistOnLoad() )
				: new ImarisLabelCache( dataset, typeFactory.getPrimitiveType(), mapDimensions, grid, backingLoader, options.persistOnLoad() );

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
		final ImarisCachedLabelImg< T, ? extends A > img = new ImarisCachedLabelImg<>(
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
		mapDimension[ 3 ] = -1;
		int j = 0;
		for ( int i = 0; i < imarisDims.length; ++i )
		{
			if ( i == 3 )
				continue;
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

	// -- deprecated API --

	@Override
	@Deprecated
	public NativeImg< T, ? > create( final long[] dimension, final T type )
	{
		throw new UnsupportedOperationException();
	}
}
