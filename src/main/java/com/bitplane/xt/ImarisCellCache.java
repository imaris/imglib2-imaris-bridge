/*-
 * #%L
 * ImgLib2: a general-purpose, multidimensional image processing library.
 * %%
 * Copyright (C) 2017 - 2020 Tobias Pietzsch, Stephan Preibisch, Stephan Saalfeld,
 * John Bogovic, Albert Cardona, Barry DeZonia, Christian Dietz, Jan Funke,
 * Aivar Grislis, Jonathan Hale, Grant Harris, Stefan Helfrich, Mark Hiner,
 * Martin Horn, Steffen Jaensch, Lee Kamentsky, Larry Lindsey, Melissa Linkert,
 * Mark Longair, Brian Northan, Nick Perry, Curtis Rueden, Johannes Schindelin,
 * Jean-Yves Tinevez and Michael Zinsmaier.
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
import Imaris.tType;
import com.bitplane.xt.util.GetDataSubVolume;
import com.bitplane.xt.util.MapIntervalDimension;
import com.bitplane.xt.util.SetDataSubVolume;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import net.imglib2.cache.CacheLoader;
import net.imglib2.cache.CacheRemover;
import net.imglib2.cache.IoSync;
import net.imglib2.img.basictypeaccess.array.ArrayDataAccess;
import net.imglib2.img.basictypeaccess.volatiles.array.DirtyVolatileByteArray;
import net.imglib2.img.basictypeaccess.volatiles.array.DirtyVolatileFloatArray;
import net.imglib2.img.basictypeaccess.volatiles.array.DirtyVolatileShortArray;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileByteArray;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileFloatArray;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileShortArray;
import net.imglib2.img.cell.Cell;
import net.imglib2.img.cell.CellGrid;

import static com.bitplane.xt.util.MapIntervalDimension.mapIntervalDimension;

/**
 * Basic {@link CacheRemover}/{@link CacheLoader} for writing/reading cells
 * to an Imaris {@code IDataset}.
 * <p>
 * Blocks which are not in the cache (yet) are obtained from a backing
 * {@link CacheLoader}. Typically the backing loader will just create empty cells.
 * </p>
 * <p><em>
 * A {@link ImarisCellCache} should be connected to a in-memory cache through
 * {@link IoSync} if the cache will be used concurrently by multiple threads!
 * </em></p>
 *
 * @param <A>
 *            access type
 *
 * @author Tobias Pietzsch
 */
public class ImarisCellCache< A > implements CacheRemover< Long, Cell< A >, A >, CacheLoader< Long, Cell< A > >
{
	private final IDataSetPrx dataset;

	private final tType datasetType;

	private final CellGrid grid;

	private final int n;

	// TODO: Rename, "volatileArray" part seems not so relevant, it's just to distinguish the various
	//  PixelSource kinds flying around. There must be a better way to do this.
	private final PixelSource< A > volatileArraySource;

	// TODO: Rename, "volatileArray" part seems not so relevant, it's just to distinguish the various
	//  PixelSource kinds flying around. There must be a better way to do this.
	// TODO: Rename. "Sink" is not the best name probably, despite pairing up with "Source" nicely?
	private final PixelSink< A > volatileArraySink;

	/**
	 * Used to generate Cells that have not yet been stored to Imaris
	 * (via {@link #onRemoval})
	 */
	private final CacheLoader< Long, Cell< A > > backingLoader;

	/**
	 * Whether to immediately persist cells that have been loaded from {@code
	 * backingLoader} to Imaris.
	 */
	private final boolean persistOnLoad;

	/**
	 * Contains the keys that have been stored to Imaris (via {@link #onRemoval}).
	 * If a key is present in this set, the corresponding Cell is loaded from Imaris.
	 * Otherwise, it is obtained from the {@code backingLoader}.
	 * <p>
	 * If there is no {@code backingLoader}, this is {@code null}, and all Cells
	 * are loaded from Imaris.
	 */
	// TODO
	//  This should be eventually replaced by something with a smaller memory footprint,
	//  for example a BitSet. But then we need to take care of concurrency ourselves, so...
	private final Set< Long > written;

	public ImarisCellCache(
			final IDataSetPrx dataset,
			final int[] mapDimensions,
			final CellGrid grid,
			final CacheLoader< Long, Cell< A > > backingLoader,
			final boolean persistOnLoad ) throws Error
	{
		this( dataset, mapDimensions, grid, backingLoader, persistOnLoad, false );
	}

	protected ImarisCellCache(
			final IDataSetPrx dataset,
			final int[] mapDimensions,
			final CellGrid grid,
			final CacheLoader< Long, Cell< A > > backingLoader,
			final boolean persistOnLoad,
			final boolean withDirtyFlag ) throws Error
	{
		this.dataset = dataset;
		datasetType = dataset.GetType();
		this.grid = grid;
		n = grid.numDimensions();
		this.mapDimensions = mapDimensions;
		this.backingLoader = backingLoader;
		this.persistOnLoad = persistOnLoad;
		volatileArraySource = volatileArraySource( withDirtyFlag );
		volatileArraySink = volatileArraySink();
		written = backingLoader == null ? null : ConcurrentHashMap.newKeySet();
	}







	// ===================================================================
	// The following code is mostly copied from ImarisDataset.
	// TODO: refactor
	// ===================================================================


	// -------------------------------------------------------------------
	//  Mapping dimensions between Imaris (always 5D) and ImgLib (2D..5D)
	// -------------------------------------------------------------------

	/**
	 * Maps Imaris dimension indices to imglib2 dimension indices.
	 * If {@code i} is dimension index from Imaris (0..4 means X,Y,Z,C,T)
	 * then {@code mapDimensions[i]} is the corresponding dimension in {@code Img}.
	 * For {@code Img} dimensions with size=1 are skipped.
	 * E.g., for a X,Y,C image {@code mapDimensions = {0,1,-1,2,-1}}.
	 */
	private final int[] mapDimensions;

	// -------------------------------------------------------------------
	//  Reading Imaris blocks as primitive arrays
	// -------------------------------------------------------------------


	@FunctionalInterface
	private interface PixelSource< A >
	{
		/**
		 * Get sub-volume as flattened primitive array.
		 *
		 * @param min
		 * 		minimum of interval in {@code Img} space.
		 * 		Will be augmented to 5D if necessary (See {@link #mapDimensions}).
		 * @param size
		 * 		size of interval in {@code Img} space.
		 * 		Will be augmented to 5D if necessary (See {@link #mapDimensions}).
		 *
		 * @return {@code byte[]}, {@code short[]}, {@code float[]}, depending on dataset type.
		 */
		A get( long[] min, int[] size ) throws Error;
	}

	/**
	 * TODO
	 * @return
	 */
	private PixelSource< ? > arraySource()
	{
		final GetDataSubVolume slice = GetDataSubVolume.forDataSet( dataset, datasetType );

		final IntFunction< Object > creator;
		switch ( datasetType )
		{
		case eTypeUInt8:
			creator = byte[]::new;
			break;
		case eTypeUInt16:
			creator = short[]::new;
			break;
		case eTypeFloat:
			creator = float[]::new;
			break;
		default:
			throw new IllegalArgumentException();
		}

		final MapIntervalDimension x = mapIntervalDimension( mapDimensions[ 0 ] );
		final MapIntervalDimension y = mapIntervalDimension( mapDimensions[ 1 ] );
		final MapIntervalDimension z = mapIntervalDimension( mapDimensions[ 2 ] );
		final MapIntervalDimension c = mapIntervalDimension( mapDimensions[ 3 ] );
		final MapIntervalDimension t = mapIntervalDimension( mapDimensions[ 4 ] );

		return ( min, size ) -> {

			final int ox = x.min( min );
			final int oy = y.min( min );
			final int oz = z.min( min );
			final int oc = c.min( min );
			final int ot = t.min( min );

			final int sx = x.size( size );
			final int sy = y.size( size );
			final int sz = z.size( size );
			final int sc = c.size( size );
			final int st = t.size( size );

			if ( sc == 1 && st == 1 )
				return slice.get( ox, oy, oz, oc, ot, 0, sx, sy, sz);
			else
			{
				final Object data = creator.apply( sx * sy * sz * sc * st );
				final int slicelength = sx * sy * sz;
				for ( int dt = 0; dt < st; ++dt )
				{
					for ( int dc = 0; dc < sc; ++dc )
					{
						final Object slicedata = slice.get( ox, oy, oz, oc + dc, ot + dt, 0, sx, sy, sz );
						final int destpos = ( dt * sc + dc ) * slicelength;
						System.arraycopy( slicedata, 0, data, destpos, slicelength );
					}
				}
				return data;
			}
		};
	}

	/**
	 * TODO
	 */
	// TODO: Rename, "volatileArray" part seems not so relevant, it's just to distinguish the various
	//  PixelSource kinds flying around. There must be a better way to do this.
	private PixelSource< A > volatileArraySource( final boolean withDirtyFlag )
	{
		final PixelSource< ? > pixels = arraySource();
		if ( withDirtyFlag )
			switch ( datasetType )
			{
			case eTypeUInt8:
				return ( min, size ) -> ( A ) new DirtyVolatileByteArray( ( byte[] ) ( pixels.get( min, size ) ), true );
			case eTypeUInt16:
				return ( min, size ) -> ( A ) new DirtyVolatileShortArray( ( short[] ) ( pixels.get( min, size ) ), true );
			case eTypeFloat:
				return ( min, size ) -> ( A ) new DirtyVolatileFloatArray( ( float[] ) ( pixels.get( min, size ) ), true );
			default:
				throw new IllegalArgumentException();
			}
		else
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


	// -------------------------------------------------------------------
	//  Writing Imaris blocks as primitive arrays
	// -------------------------------------------------------------------

	@FunctionalInterface
	// TODO: Rename. "Sink" is not the best name probably, despite pairing up with "Source" nicely?
	private interface PixelSink< A >
	{
		/**
		 * Set sub-volume as flattened primitive array.
		 *
		 * @param data
		 *  	{@code byte[]}, {@code short[]}, {@code float[]}, depending on dataset type.
		 * @param min
		 * 		minimum of interval in {@code Img} space.
		 * 		Will be augmented to 5D if necessary (See {@link #mapDimensions}).
		 * @param size
		 * 		size of interval in {@code Img} space.
		 * 		Will be augmented to 5D if necessary (See {@link #mapDimensions}).
		 */
		void put( A data, long[] min, int[] size ) throws Error;
	}

	/**
	 * TODO
	 */
	// TODO: Rename, "volatileArray" part seems not so relevant, it's just to distinguish the various
	//  PixelSource kinds flying around. There must be a better way to do this.
	// TODO: Rename. "Sink" is not the best name probably, despite pairing up with "Source" nicely?
	private PixelSink< A > volatileArraySink()
	{
		final SetDataSubVolume slice = SetDataSubVolume.forDataSet( dataset, datasetType );

		final IntFunction< Object > creator;
		switch ( datasetType )
		{
		case eTypeUInt8:
			creator = byte[]::new;
			break;
		case eTypeUInt16:
			creator = short[]::new;
			break;
		case eTypeFloat:
			creator = float[]::new;
			break;
		default:
			throw new IllegalArgumentException();
		}

		final MapIntervalDimension x = mapIntervalDimension( mapDimensions[ 0 ] );
		final MapIntervalDimension y = mapIntervalDimension( mapDimensions[ 1 ] );
		final MapIntervalDimension z = mapIntervalDimension( mapDimensions[ 2 ] );
		final MapIntervalDimension c = mapIntervalDimension( mapDimensions[ 3 ] );
		final MapIntervalDimension t = mapIntervalDimension( mapDimensions[ 4 ] );

		return ( access, min, size ) ->
		{
			final Object data = ( ( ArrayDataAccess ) access ).getCurrentStorageArray();

			final int ox = x.min( min );
			final int oy = y.min( min );
			final int oz = z.min( min );
			final int oc = c.min( min );
			final int ot = t.min( min );

			final int sx = x.size( size );
			final int sy = y.size( size );
			final int sz = z.size( size );
			final int sc = c.size( size );
			final int st = t.size( size );

			if ( sc == 1 && st == 1 )
				slice.set( data, ox, oy, oz, oc, ot, sx, sy, sz );
			else
			{
				final int slicelength = sx * sy * sz;
				final Object slicedata = creator.apply( slicelength );
				for ( int dt = 0; dt < st; ++dt )
				{
					for ( int dc = 0; dc < sc; ++dc )
					{
						final int srcpos = ( dt * sc + dc ) * slicelength;
						System.arraycopy( data, srcpos, slicedata, 0, slicelength );
						slice.set( slicedata, ox, oy, oz, oc + dc, ot + dt, sx, sy, sz );
					}
				}
			}
		};
	}

	// ===================================================================









	@Override
	public Cell< A > get( final Long key ) throws Exception
	{
		final long index = key;
		if ( written == null || written.contains( key ) )
		{
			final long[] cellMin = new long[ n ];
			final int[] cellDims = new int[ n ];
			grid.getCellDimensions( index, cellMin, cellDims );
			return new Cell<>(
					cellDims,
					cellMin,
					volatileArraySource.get( cellMin, cellDims ) );
		}
		else
		{
			final Cell< A > cell = backingLoader.get( key );
			if ( persistOnLoad )
				onRemovalImp( key, cell.getData() );
			return cell;
		}
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
		final long[] cellMin = new long[ n ];
		final int[] cellDims = new int[ n ];
		grid.getCellDimensions( index, cellMin, cellDims );
		return new Cell<>( cellDims, cellMin, valueData );
	}

	@Override
	public void onRemoval( final Long key, final A valueData )
	{
		onRemovalImp( key, valueData );
	}

	public void onRemovalImp( final Long key, final A valueData )
	{
		final long index = key;
		final long[] cellMin = new long[ n ];
		final int[] cellDims = new int[ n ];
		grid.getCellDimensions( index, cellMin, cellDims );
		try
		{
			volatileArraySink.put( valueData, cellMin, cellDims );
			if ( written != null )
				written.add( key );
		}
		catch ( Error error )
		{
			throw new RuntimeException( error );
		}
	}

	@Override
	public CompletableFuture< Void > persist( final Long key, final A valueData )
	{
		onRemoval( key, valueData );
		return CompletableFuture.completedFuture( null );
	}

	@Override
	public void invalidate( final Long key )
	{
		// TODO For now, we always load/save to imaris, i.e., there is no "clean version", so invalidate() doesn't make sense.
		throw new UnsupportedOperationException( "TODO. not implemented yet" );
	}

	@Override
	public void invalidateIf( final long parallelismThreshold, final Predicate< Long > condition )
	{
		// TODO For now, we always load/save to imaris, i.e., there is no "clean version", so invalidate() doesn't make sense.
		throw new UnsupportedOperationException( "TODO. not implemented yet" );
	}

	@Override
	public void invalidateAll( final long parallelismThreshold )
	{
		// TODO For now, we always load/save to imaris, i.e., there is no "clean version", so invalidate() doesn't make sense.
		//
		//  Later, this should possibly clear the Imaris dataset (is there a function for this?).
		//  It should clear the map of written blocks (blocks that were written to imaris once,
		//  and will therefore be loaded from imaris when they are next needed).
		throw new UnsupportedOperationException( "TODO. not implemented yet" );
	}

	// TODO there should be a method to say that the image has been modified on the imaris side.
	//  This would then clear the cache and mark all cells as written, so that they will be loaded from Imaris always.
}
