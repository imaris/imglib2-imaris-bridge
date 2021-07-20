package com.bitplane.xt;

import Imaris.Error;
import Imaris.IDataSetPrx;
import Imaris.tType;
import bdv.img.cache.CreateInvalidVolatileCell;
import bdv.img.cache.VolatileCachedCellImg;
import bdv.util.AxisOrder;
import bdv.util.volatiles.SharedQueue;
import bdv.util.volatiles.VolatileTypeMatcher;
import com.bitplane.xt.util.PixelSource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.Volatile;
import net.imglib2.cache.Cache;
import net.imglib2.cache.CacheLoader;
import net.imglib2.cache.CacheRemover;
import net.imglib2.cache.IoSync;
import net.imglib2.cache.LoaderRemoverCache;
import net.imglib2.cache.img.CachedCellImg;
import net.imglib2.cache.ref.SoftRefLoaderRemoverCache;
import net.imglib2.cache.ref.WeakRefVolatileCache;
import net.imglib2.cache.util.KeyBimap;
import net.imglib2.cache.volatiles.CacheHints;
import net.imglib2.cache.volatiles.UncheckedVolatileCache;
import net.imglib2.img.basictypeaccess.AccessFlags;
import net.imglib2.img.basictypeaccess.ArrayDataAccessFactory;
import net.imglib2.img.cell.Cell;
import net.imglib2.img.cell.CellGrid;
import net.imglib2.type.NativeType;
import net.imglib2.type.NativeTypeFactory;
import net.imglib2.type.numeric.RealType;

import static net.imglib2.cache.volatiles.LoadingStrategy.BUDGETED;

/**
 * Implementation of {@link ImagePyramid} with images that are backed by a joint
 * cache which loads blocks from Imaris.
 */
class CachedImagePyramid< T extends NativeType< T > & RealType< T >, V extends Volatile< T > & NativeType< V > & RealType< V >, A >
	implements ImagePyramid< T, V >
{
	/**
	 * Number of resolutions pyramid levels
	 */
	private final int numResolutions;

	private final AxisOrder axisOrder;

	/**
	 * Dimensions of the resolution pyramid images.
	 * {@code dimensions[level][d]}.
	 */
	private final long[][] dimensions;

	/**
	 * Cell sizes of the resolution pyramid images.
	 * {@code cellDimensions[level][d]}.
	 * C, and T cell size are always {@code = 1}.
	 */
	private final int[][] cellDimensions;

	private final T type;

	private final V volatileType;

	// TODO make constructor argument ?
	private final SharedQueue queue;

	// TODO make constructor argument ?
	// shared cache for cells of all resolution levels
	private final LoaderRemoverCache< Key, Cell< A >, A > backingCache;

	private final CachedCellImg< T, A >[] imgs;

	private final VolatileCachedCellImg< V, A >[] vimgs;

	private final int numChannels;

	private final int numTimepoints;

	public CachedImagePyramid(
			final T type,
			final AxisOrder axisOrder,
			final long[][] dimensions,
			final int[][] cellDimensions,
			final IDataSetPrx dataset,
			final tType datasetType,
			final int[] mapDimensions ) throws Error
	{
		this.axisOrder = axisOrder;
		numResolutions = dimensions.length;
		this.dimensions = dimensions;
		this.cellDimensions = cellDimensions;
		final int numDimensions = dimensions[ 0 ].length;
		numChannels = axisOrder.hasChannels() ? ( int ) dimensions[ 0 ][ axisOrder.channelDimension() ] : 1;
		numTimepoints = axisOrder.hasTimepoints() ? ( int ) dimensions[ 0 ][ axisOrder.timeDimension() ] : 1;

		this.type = type;
		volatileType = ( V ) VolatileTypeMatcher.getVolatileTypeForType( type );

		queue = new SharedQueue( 16, numResolutions );
		backingCache = new SoftRefLoaderRemoverCache<>();

		imgs = new CachedCellImg[ numResolutions ];
		vimgs = new VolatileCachedCellImg[ numResolutions ];

		final PixelSource< A > volatileArraySource = PixelSource.volatileArraySource( dataset, datasetType, mapDimensions, false );

		// in: numResolutions, queue, backingCache, s, dimensions, cellDimensions
		//     numResolutions == dimensions.length
		//     queue and backingCache can be defined inside Images constructor
		// so
		// 	s, dimensions, cellDimensions
		// are the real parameters required
		// and type, so that we can figure out the rest of the types ourselves...
		for ( int resolution = 0; resolution < numResolutions; ++resolution )
		{
			final CellGrid grid = new CellGrid( dimensions[ resolution ], cellDimensions[ resolution ] );

			final int level = resolution;
			final KeyBimap< Long, Key > bimap = KeyBimap.build(
					index -> new Key( level, index ),
					key -> key.level == level ? key.index : null );
			final Cache< Long, Cell< A > > cache;
			if ( level == 0 )
			{
				final CacheLoader< Long, Cell< A > > backingLoader = null;
				// TODO: ImarisCellCache / ImarisDirtyCellCache
				final ImarisCellCache< A > lr = new ImarisCellCache<>( dataset, mapDimensions, grid, backingLoader, false );
				// TODO: wrap in IoSync
				final IoSync< Long, Cell< A >, A > iosync = new IoSync<>(
						lr, 1, 10 ); // TODO
//						options.numIoThreads(), // TODO
//						options.maxIoQueueSize() ); // TODO
				cache = backingCache.mapKeys( bimap ).withLoader( iosync ).withRemover( iosync );
			}
			else
			{
				final CacheLoader< Long, Cell< A > > loader = key -> {
					final long[] cellMin = new long[ numDimensions ];
					final int[] cellDims = new int[ numDimensions ];
					grid.getCellDimensions( key, cellMin, cellDims );
					return new Cell<>(
							cellDims,
							cellMin,
							volatileArraySource.get( level, cellMin, cellDims ) );
				};
				// ------------------------------------------------------------------------------
				// TODO
				// TODO
				// TODO
				final CacheRemover< Long, Cell< A >, A > DUMMY_REMOVER = new CacheRemover< Long, Cell< A >, A >()
				{
					@Override
					public void onRemoval( final Long key, final A valueData )
					{
					}

					@Override
					public CompletableFuture< Void > persist( final Long key, final A valueData )
					{
						return null;
					}

					@Override
					public A extract( final Cell< A > value )
					{
						return value.getData();
					}

					@Override
					public Cell< A > reconstruct( final Long key, final A valueData )
					{
						final long index = key;
						final long[] cellMin = new long[ numDimensions ];
						final int[] cellDims = new int[ numDimensions ];
						grid.getCellDimensions( index, cellMin, cellDims );
						return new Cell<>( cellDims, cellMin, valueData );
					}
				};
				// TODO
				// TODO
				// TODO
				// ------------------------------------------------------------------------------
				cache = backingCache.mapKeys( bimap ).withLoader( ( CacheLoader ) loader ).withRemover( DUMMY_REMOVER );
			}

			final int priority = numResolutions - resolution - 1;
			final CacheHints hints = new CacheHints( BUDGETED, priority, false );

			final NativeTypeFactory< T, A > typeFactory = ( NativeTypeFactory< T, A > ) type.getNativeTypeFactory();
			final A accessType = ArrayDataAccessFactory.get( typeFactory, AccessFlags.setOf( AccessFlags.VOLATILE ) );
			final CachedCellImg< T, A > img = new CachedCellImg( grid, type, cache, accessType );
			img.setLinkedType( typeFactory.createLinkedType( img ) );

			final CreateInvalidVolatileCell createInvalid = CreateInvalidVolatileCell.get( grid, volatileType, false );
			final UncheckedVolatileCache< Long, Cell > vcache = new WeakRefVolatileCache<>( cache, queue, createInvalid ).unchecked();
			final VolatileCachedCellImg< V, A > vimg = new VolatileCachedCellImg<>( grid, volatileType, hints, vcache::get );

			imgs[ resolution ] = img;
			vimgs[ resolution ] = vimg;
		}
	}

	/**
	 * Key for a cell identified by resolution level and index
	 * (flattened spatial coordinate).
	 */
	static class Key
	{
		private final int level;

		private final long index;

		private final int hashcode;

		/**
		 * Create a Key for the specified cell. Note that {@code cellDims} and
		 * {@code cellMin} are not used for {@code hashcode()/equals()}.
		 *
		 * @param level
		 *            level coordinate of the cell
		 * @param index
		 *            index of the cell (flattened spatial coordinate of the
		 *            cell)
		 */
		public Key( final int level, final long index )
		{
			this.level = level;
			this.index = index;
			hashcode = 31 * Long.hashCode( index ) + level;
		}

		@Override
		public boolean equals( final Object other )
		{
			if ( this == other )
				return true;
			if ( !( other instanceof Key ) )
				return false;
			final Key that = ( Key ) other;
			return ( this.index == that.index ) && ( this.level == that.level );
		}

		@Override
		public int hashCode()
		{
			return hashcode;
		}
	}

	@Override
	public int numResolutions()
	{
		return numResolutions;
	}

	@Override
	public AxisOrder axisOrder()
	{
		return axisOrder;
	}

	@Override
	public int numChannels()
	{
		return numChannels;
	}

	@Override
	public int numTimepoints()
	{
		return numTimepoints;
	}

	@Override
	public CachedCellImg< T, A > getImg( final int resolutionLevel )
	{
		return imgs[ resolutionLevel ];
	}

	@Override
	public VolatileCachedCellImg< V, A > getVolatileImg( final int resolutionLevel )
	{
		return vimgs[ resolutionLevel ];
	}

	@Override
	public T getType()
	{
		return type;
	}

	@Override
	public V getVolatileType()
	{
		return volatileType;
	}

	/**
	 * Persist changes back to Imaris.
	 * Note that only the full resolution (level 0) image is writable!
	 */
	public void persist()
	{
		imgs[ 0 ].getCache().persistAll();
	}

	public SharedQueue getSharedQueue()
	{
		return queue;
	}

	/**
	 * Split this {@code ImagePyramid} along the channel axis (according to the {@code axisOrder}.
	 * Returns a list of {@code ImagePyramid}s, one for each channel.
	 * If this {@code ImagePyramid} has no Z dimension, it is augmented by a Z dimension of size 1.
	 * Thus, the returned {@code ImagePyramid}s are always 3D (XYZ) or 4D (XYZT).
	 */
	public List< ImagePyramid< T, V > > splitIntoSourceStacks()
	{
		return splitIntoSourceStacks( this );
	}

	/**
	 * Takes an {@code ImagePyramid} and splits it along the channel axis (according to the pyramid's {@code axisOrder}.
	 * Returns a list of {@code ImagePyramid}s, one for each channel.
	 * If the input {@code ImagePyramid} has no Z dimension, it is augmented by a Z dimension of size 1.
	 * Thus, the returned {@code ImagePyramid}s are always 3D (XYZ) or 4D (XYZT).
	 */
	private static < T, V > List< ImagePyramid< T, V > > splitIntoSourceStacks( final ImagePyramid< T, V > input )
	{
		final int numResolutions = input.numResolutions();
		final int numChannels = input.numChannels();

		final List< DefaultImagePyramid< T, V > > channels = new ArrayList<>( numChannels );
		final AxisOrder axisOrder = splitIntoSourceStacks( input.axisOrder() );
		for ( int c = 0; c < numChannels; c++ )
			channels.add( new DefaultImagePyramid<>( input.getType(), input.getVolatileType(), numResolutions, axisOrder ) );

		for ( int l = 0; l < numResolutions; ++l )
		{
			final List< RandomAccessibleInterval< T > > channelImgs = AxisOrder.splitInputStackIntoSourceStacks( input.getImg( l ), input.axisOrder() );
			final List< RandomAccessibleInterval< V > > channelVolatileImgs = AxisOrder.splitInputStackIntoSourceStacks( input.getVolatileImg( l ), input.axisOrder() );
			for ( int c = 0; c < numChannels; ++c )
			{
				channels.get( c ).imgs[ l ] = channelImgs.get( c );
				channels.get( c ).vimgs[ l ] = channelVolatileImgs.get( c );
			}
		}

		return new ArrayList<>( channels );
	}

	/**
	 * Returns the {@code AxisOrder} of (each channel) of a {@code ImagePyramid} split into source stacks.
	 * Basically: remove the channel dimension. If there is no Z dimension, add one.
	 */
	private static AxisOrder splitIntoSourceStacks( final AxisOrder axisOrder )
	{
		switch ( axisOrder )
		{
		case XYZ:
		case XYZC:
		case XY:
		case XYC:
		case XYCZ:
			return AxisOrder.XYZ;
		case XYZT:
		case XYZCT:
		case XYZTC:
		case XYCZT:
		case XYT:
		case XYCT:
		case XYTC:
			return AxisOrder.XYZT;
		case DEFAULT:
		default:
			throw new IllegalArgumentException();
		}
	}
}
