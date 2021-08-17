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
import Imaris.tType;
import com.bitplane.xt.util.MapDimensions.SelectIntervalDimension;
import com.bitplane.xt.util.SetDataSubVolume;
import com.bitplane.xt.util.GetDataSubVolume;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import net.imglib2.cache.CacheLoader;
import net.imglib2.cache.CacheRemover;
import net.imglib2.cache.IoSync;
import net.imglib2.img.basictypeaccess.array.ArrayDataAccess;
import net.imglib2.img.basictypeaccess.volatiles.array.DirtyVolatileDoubleArray;
import net.imglib2.img.basictypeaccess.volatiles.array.DirtyVolatileFloatArray;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileDoubleArray;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileFloatArray;
import net.imglib2.img.cell.Cell;
import net.imglib2.img.cell.CellGrid;
import net.imglib2.type.PrimitiveType;

import static com.bitplane.xt.util.MapDimensions.selectIntervalDimension;

/**
 * A {@link CacheRemover}/{@link CacheLoader} for writing/reading cells
 * to an Imaris {@code IDataset}.
 * <p>
 * At each pixel the image is assumed to represent a probability distribution
 * over channels. For storing to Imaris {@code IDataSetPrx} backing cache, the
 * first channel ("background") is removed (only the other channels are stored).
 * If the dataset has UINT8 or UINT16 type, the [0, 1] range is scaled to the
 * [0, 2^8-1] or [0, 2^16-1], respectively. For loading data back from the cache
 * this operation is reversed.
 * <p>
 * Blocks which are not in the cache (yet) are obtained from a backing
 * {@link CacheLoader}. Typically the backing loader will just create empty cells.
 * <p>
 * <em>A {@link ImarisProbabilitiesCache} should be connected to a in-memory
 * cache through {@link IoSync} if the cache will be used concurrently by
 * multiple threads!</em>
 *
 * @param <A>
 *            access type
 *
 * @author Tobias Pietzsch
 */
public class ImarisProbabilitiesCache< A > implements CacheRemover< Long, Cell< A >, A >, CacheLoader< Long, Cell< A > >
{
	private final IDataSetPrx dataset;

	private final tType datasetType;

	private final PrimitiveType primitiveType; // primitive type underlying ImgLib2 type

	private final CellGrid grid;

	private final int n;

	private final PixelSource< A > volatileArraySource;

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


	// TODO: into how many channels to split
	private final int numChannels;

	public ImarisProbabilitiesCache(
			final IDataSetPrx dataset,
			final PrimitiveType primitiveType, // primitive type underlying accesses
			final int[] mapDimensions,
			final CellGrid grid,
			final CacheLoader< Long, Cell< A > > backingLoader,
			final boolean persistOnLoad ) throws Error
	{
		this( dataset, primitiveType, mapDimensions, grid, backingLoader, persistOnLoad, false );
	}

	protected ImarisProbabilitiesCache(
			final IDataSetPrx dataset,
			final PrimitiveType primitiveType, // primitive type underlying accesses
			final int[] mapDimensions,
			final CellGrid grid,
			final CacheLoader< Long, Cell< A > > backingLoader,
			final boolean persistOnLoad,
			final boolean withDirtyFlag ) throws Error
	{
		this.dataset = dataset;
		datasetType = dataset.GetType();
		numChannels = dataset.GetSizeC();
		this.primitiveType = primitiveType;
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

		// creates output arrays (to be used by imglib)
		// Object is float[] or double[] depending on primitiveType
		final IntFunction< Object > arrayFactory;

		// creates a GetProbability to read from input array (from imaris)
		// Object is byte[], short[], float[] depending on datasetType
		final Function< Object, GetProbability > getProbabilityFactory;

		// creates a SetLabel to write to output array (created by arrayFactory)
		final Function< Object, SetProbability > setProbabilityFactory;


		switch ( primitiveType )
		{
		case FLOAT:
			arrayFactory = float[]::new;
			setProbabilityFactory = SetProbabilityFloat::new;
			break;
		case DOUBLE:
			arrayFactory = double[]::new;
			setProbabilityFactory = SetProbabilityDouble::new;
			break;
		default:
			throw new IllegalArgumentException();
		}

		switch ( datasetType )
		{
		case eTypeUInt8:
			getProbabilityFactory = GetProbabilityByte::new;
			break;
		case eTypeUInt16:
			getProbabilityFactory = GetProbabilityShort::new;
			break;
		case eTypeFloat:
			getProbabilityFactory = GetProbabilityFloat::new;
			break;
		default:
			throw new IllegalArgumentException();
		}

		final SelectIntervalDimension x = selectIntervalDimension( mapDimensions[ 0 ] );
		final SelectIntervalDimension y = selectIntervalDimension( mapDimensions[ 1 ] );
		final SelectIntervalDimension z = selectIntervalDimension( mapDimensions[ 2 ] );
//		final SelectIntervalDimension c = selectIntervalDimension( mapDimensions[ 3 ] );
		final SelectIntervalDimension t = selectIntervalDimension( mapDimensions[ 4 ] );

		final int oc = 0;
		final int sc = numChannels + 1;
		// NB: we assume always oc == 0 and sc == img.dim(C). TODO: check this?

		return ( min, size ) -> {

			final int ox = x.min( min );
			final int oy = y.min( min );
			final int oz = z.min( min );
			final int ot = t.min( min );

			final int sx = x.size( size );
			final int sy = y.size( size );
			final int sz = z.size( size );
			final int st = t.size( size );

			final int slicelength = sx * sy * sz;
			final Object data = arrayFactory.apply( sx * sy * sz * sc * st );
			final SetProbability output = setProbabilityFactory.apply( data );

			final int tstep;
			final int cstep;
			if ( mapDimensions[ 3 ] < mapDimensions[ 4 ] )
			{
				// XYZCT etc
				tstep = slicelength * sc;
				cstep = slicelength;
			}
			else
			{
				// XYZTC etc (T and C flipped)
				tstep = slicelength;
				cstep = slicelength * st;
			}

			final GetProbability[] slices = new GetProbability[ sc - 1 ]; // (-1 because no slice for background)
			for ( int dt = 0; dt < st; ++dt )
			{
				final int toffset = dt * tstep;

				// get all sc - 1 slices from imaris (no slice for background)
				for ( int dc = 0; dc < sc - 1; ++dc )
				{
					final Object slicedata = slice.get( ox, oy, oz, oc + dc, ot + dt, 0, sx, sy, sz );
					slices[ dc ] = getProbabilityFactory.apply( slicedata );
				}

				// iterate over XYZ and write sc channels at T=t
				for ( int xyz = 0; xyz < slicelength; ++xyz )
				{
					float bg = 1f;
					for ( int dc = 0; dc < sc - 1; ++dc )
					{
						final float p = slices[ dc ].get( xyz );
						output.set( toffset + xyz + ( dc + 1 ) * cstep, p );
						bg -= p;
					}
					output.set( toffset + xyz, bg );
				}
			}
			return data;
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
			switch ( primitiveType )
			{
			case FLOAT:
				return ( min, size ) -> ( A ) new DirtyVolatileFloatArray( ( float[] ) ( pixels.get( min, size ) ), true );
			case DOUBLE:
				return ( min, size ) -> ( A ) new DirtyVolatileDoubleArray( ( double[] ) ( pixels.get( min, size ) ), true );
			default:
				throw new IllegalArgumentException();
			}
		else
			switch ( primitiveType )
			{
			case FLOAT:
				return ( min, size ) -> ( A ) new VolatileFloatArray( ( float[] ) ( pixels.get( min, size ) ), true );
			case DOUBLE:
				return ( min, size ) -> ( A ) new VolatileDoubleArray( ( double[] ) ( pixels.get( min, size ) ), true );
			default:
				throw new IllegalArgumentException();
			}
	}

	private interface GetProbability
	{
		float get( final int index );
	}

	private interface SetProbability
	{
		void set( final int index, final float value );
	}

	static class SetProbabilityByte implements SetProbability
	{
		private final byte[] output;

		SetProbabilityByte( final Object output )
		{
			this.output = ( byte[] ) output;
		}

		@Override
		public void set( final int index, final float value )
		{
			final int unsigned = Math.min( 255, Math.max( 0, ( int ) ( value * 255f ) ) );
			output[ index ] = ( byte ) ( unsigned & 0xff );
		}

	}

	static class SetProbabilityShort implements SetProbability
	{
		private final short[] output;

		SetProbabilityShort( final Object output )
		{
			this.output = ( short[] ) output;
		}

		@Override
		public void set( final int index, final float value )
		{
			final int unsigned = Math.min( 65535, Math.max( 0, ( int ) ( value * 65535f ) ) );
			output[ index ] = ( short ) ( unsigned & 0xffff );
		}
	}

	static class SetProbabilityFloat implements SetProbability
	{
		private final float[] output;

		SetProbabilityFloat( final Object output )
		{
			this.output = ( float[] ) output;
		}

		@Override
		public void set( final int index, final float value )
		{
			output[ index ] = value;
		}
	}

	static class SetProbabilityDouble implements SetProbability
	{
		private final double[] output;

		SetProbabilityDouble( final Object output )
		{
			this.output = ( double[] ) output;
		}

		@Override
		public void set( final int index, final float value )
		{
			output[ index ] = value;
		}
	}

	static class GetProbabilityByte implements GetProbability
	{
		private final byte[] input;

		GetProbabilityByte( final Object input )
		{
			this.input = ( byte[] ) input;
		}

		@Override
		public float get( final int index )
		{
			return ( 1f / 255f ) * ( input[ index ] & 0xff );
		}
	}

	static class GetProbabilityShort implements GetProbability
	{
		private final short[] input;

		GetProbabilityShort( final Object input )
		{
			this.input = ( short[] ) input;
		}

		@Override
		public float get( final int index )
		{
			return ( 1f / 65535f ) * ( input[ index ] & 0xffff );
		}
	}

	static class GetProbabilityFloat implements GetProbability
	{
		private final float[] input;

		GetProbabilityFloat( final Object input )
		{
			this.input = ( float[] ) input;
		}

		@Override
		public float get( final int index )
		{
			return input[ index ];
		}
	}

	static class GetProbabilityDouble implements GetProbability
	{
		private final double[] input;

		GetProbabilityDouble( final Object input )
		{
			this.input = ( double[] ) input;
		}

		@Override
		public float get( final int index )
		{
			return ( float ) input[ index ];
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

		// creates output arrays (to be send to Imaris)
		// Object is byte[], short[], float[], depending on datasetType
		final IntFunction< Object > arrayFactory;

		// creates a GetProbability to read from ArrayDataAccess.getCurrentStorageArray() of primitiveType
		// Object is float[], double[] depending on primitiveType
		final Function< Object, GetProbability > getProbabilityFactory;

		// creates a SetProbability to write to output array (created by arrayFactory)
		final Function< Object, SetProbability > setProbabilityFactory;

		switch ( datasetType )
		{
		case eTypeUInt8:
			arrayFactory = byte[]::new;
			setProbabilityFactory = SetProbabilityByte::new;
			break;
		case eTypeUInt16:
			arrayFactory = short[]::new;
			setProbabilityFactory = SetProbabilityShort::new;
			break;
		case eTypeFloat:
			arrayFactory = float[]::new;
			setProbabilityFactory = SetProbabilityFloat::new;
			break;
		default:
			throw new IllegalArgumentException();
		}
		switch ( primitiveType )
		{
		case FLOAT:
			getProbabilityFactory = GetProbabilityFloat::new;
			break;
		case DOUBLE:
			getProbabilityFactory = GetProbabilityDouble::new;
			break;
		default:
			throw new IllegalArgumentException();
		}

		final SelectIntervalDimension x = selectIntervalDimension( mapDimensions[ 0 ] );
		final SelectIntervalDimension y = selectIntervalDimension( mapDimensions[ 1 ] );
		final SelectIntervalDimension z = selectIntervalDimension( mapDimensions[ 2 ] );
		final SelectIntervalDimension t = selectIntervalDimension( mapDimensions[ 4 ] );

		final int oc = 0;
		final int sc = numChannels + 1;
		// NB: we assume always oc == 0 and sc == img.dim(C). TODO: check this?

		return ( access, min, size ) ->
		{
			try
			{
				final Object data = ( ( ArrayDataAccess ) access ).getCurrentStorageArray();
				final GetProbability input = getProbabilityFactory.apply( data );

				final int ox = x.min( min );
				final int oy = y.min( min );
				final int oz = z.min( min );
				final int ot = t.min( min );

				final int sx = x.size( size );
				final int sy = y.size( size );
				final int sz = z.size( size );
				final int st = t.size( size );

				final int slicelength = sx * sy * sz;
				final Object[] slicedata = new Object[ sc - 1 ]; // (-1 because no slice for background)
				final SetProbability[] slices = new SetProbability[ sc - 1 ];
				for ( int dc = 0; dc < sc - 1; ++dc )
				{
					slicedata[ dc ] = arrayFactory.apply( slicelength );
					slices[ dc ] = setProbabilityFactory.apply( slicedata[ dc ] );
				}

				final int tstep;
				final int cstep;
				if ( mapDimensions[ 3 ] < mapDimensions[ 4 ] )
				{
					// XYZCT etc
					tstep = slicelength * sc;
					cstep = slicelength;
				}
				else
				{
					// XYZTC etc (T and C flipped)
					tstep = slicelength;
					cstep = slicelength * st;
				}

				for ( int dt = 0; dt < st; ++dt )
				{
					final int toffset = dt * tstep;

					// fill all slices[] with source data (ignore background channel)
					// iterate over XYZ and write sc-1 channels at T=t
					for ( int xyz = 0; xyz < slicelength; ++xyz )
					{
						for ( int dc = 0; dc < sc - 1; ++dc )
						{
							final float p = input.get( toffset + xyz + ( dc + 1 ) * cstep );
							slices[ dc ].set( xyz, p );
						}
					}

					// send all slices[] to Imaris
					for ( int dc = 0; dc < sc - 1; ++dc )
						slice.set( slicedata[ dc ], ox, oy, oz, oc + dc, ot + dt, sx, sy, sz );
				}
			} catch ( Exception e )
			{
				e.printStackTrace();
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

	private void onRemovalImp( final Long key, final A valueData )
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
