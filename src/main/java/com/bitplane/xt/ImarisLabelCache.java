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
import net.imglib2.img.basictypeaccess.volatiles.array.DirtyVolatileByteArray;
import net.imglib2.img.basictypeaccess.volatiles.array.DirtyVolatileIntArray;
import net.imglib2.img.basictypeaccess.volatiles.array.DirtyVolatileLongArray;
import net.imglib2.img.basictypeaccess.volatiles.array.DirtyVolatileShortArray;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileByteArray;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileIntArray;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileLongArray;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileShortArray;
import net.imglib2.img.cell.Cell;
import net.imglib2.img.cell.CellGrid;
import net.imglib2.type.PrimitiveType;

/**
 * A {@link CacheRemover}/{@link CacheLoader} for writing/reading cells
 * to an Imaris {@code IDataset}. It translates integer labels on the ImgLib2 side to
 * channels on the Imaris side.
 * <p>
 * Blocks which are not in the cache (yet) are obtained from a backing
 * {@link CacheLoader}. Typically the backing loader will just create empty cells.
 * </p>
 * <p><em>
 * A {@link ImarisLabelCache} should be connected to a in-memory cache through
 * {@link IoSync} if the cache will be used concurrently by multiple threads!
 * </em></p>
 *
 * @param <A>
 *            access type
 *
 * @author Tobias Pietzsch
 */
public class ImarisLabelCache< A > implements CacheRemover< Long, Cell< A >, A >, CacheLoader< Long, Cell< A > >
{
	private final IDataSetPrx dataset;

	private final tType datasetType;

	private final PrimitiveType primitiveType; // primitive type underlying ImgLib2 type

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

	public ImarisLabelCache(
			final IDataSetPrx dataset,
			final PrimitiveType primitiveType, // primitive type underlying accesses
			final int[] mapDimensions,
			final CellGrid grid,
			final CacheLoader< Long, Cell< A > > backingLoader ) throws Error
	{
		this( dataset, primitiveType, mapDimensions, grid, backingLoader, false );
	}

	protected ImarisLabelCache(
			final IDataSetPrx dataset,
			final PrimitiveType primitiveType, // primitive type underlying accesses
			final int[] mapDimensions,
			final CellGrid grid,
			final CacheLoader< Long, Cell< A > > backingLoader,
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

	private interface MapIntervalDimension
	{
		int min( final long[] min );

		int size( final int[] size );
	}

	private static MapIntervalDimension mapIntervalDimension( final int d )
	{
		if ( d < 0 )
			return constantMapIntervalDimension;

		return new MapIntervalDimension()
		{
			@Override
			public int min( final long[] min )
			{
				return ( int ) min[ d ];
			}

			@Override
			public int size( final int[] size )
			{
				return size[ d ];
			}
		};
	}

	private static final MapIntervalDimension constantMapIntervalDimension = new MapIntervalDimension()
	{
		@Override
		public int min( final long[] min )
		{
			return 0;
		}

		@Override
		public int size( final int[] size )
		{
			return 1;
		}
	};


	// -------------------------------------------------------------------
	//  Reading Imaris blocks as primitive arrays
	// -------------------------------------------------------------------


	@FunctionalInterface
	private interface GetDataSubVolume
	{
		/**
		 * Get sub-volume as flattened primitive array.
		 *
		 * @param ox offset in X
		 * @param oy offset in Y
		 * @param oz offset in Z
		 * @param oc channel index
		 * @param ot timepoint index
		 * @param sx size in X
		 * @param sy size in Y
		 * @param sz size in Z
		 * @return {@code byte[]}, {@code short[]}, {@code float[]}, depending on dataset type.
		 */
		Object get( int ox, int oy, int oz, int oc, int ot, int sx, int sy, int sz ) throws Error;
	}

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
		final GetDataSubVolume slice;

		// creates output arrays (to be used by imglib)
		// Object is byte[], short[], int[], long[], depending on primitiveType
		final IntFunction< Object > arrayFactory;

		// creates a GetLabel to read from input arrays (one per channel, from imaris)
		// Object is byte[], short[], float[] depending on datasetType
		final Function< Object[], GetLabel > getLabelFactory;

		// creates a SetLabel to write to output array (created by arrayFactory)
		final Function< Object, SetLabel > setLabelFactory;


		switch ( primitiveType )
		{
		case BYTE:
			arrayFactory = byte[]::new;
			setLabelFactory = SetCompositeLabelByte::new;
			break;
		case SHORT:
			arrayFactory = short[]::new;
			setLabelFactory = SetCompositeLabelShort::new;
			break;
		case INT:
			arrayFactory = int[]::new;
			setLabelFactory = SetCompositeLabelInt::new;
			break;
		case LONG:
			arrayFactory = long[]::new;
			setLabelFactory = SetCompositeLabelLong::new;
			break;
		default:
			throw new IllegalArgumentException();
		}

		switch ( datasetType )
		{
		case eTypeUInt8:
			slice = dataset::GetDataSubVolumeAs1DArrayBytes;
			getLabelFactory = GetChannelLabelByte::new;
			break;
		case eTypeUInt16:
			slice = dataset::GetDataSubVolumeAs1DArrayShorts;
			getLabelFactory = GetChannelLabelShort::new;
			break;
		case eTypeFloat:
			slice = dataset::GetDataSubVolumeAs1DArrayFloats;
			getLabelFactory = GetChannelLabelFloat::new;
			break;
		default:
			throw new IllegalArgumentException();
		}

		final MapIntervalDimension x = mapIntervalDimension( mapDimensions[ 0 ] );
		final MapIntervalDimension y = mapIntervalDimension( mapDimensions[ 1 ] );
		final MapIntervalDimension z = mapIntervalDimension( mapDimensions[ 2 ] );
		final MapIntervalDimension t = mapIntervalDimension( mapDimensions[ 4 ] );

		return ( min, size ) -> {

			final int ox = x.min( min );
			final int oy = y.min( min );
			final int oz = z.min( min );
			final int oc = 0;
			final int ot = t.min( min );

			final int sx = x.size( size );
			final int sy = y.size( size );
			final int sz = z.size( size );
			final int sc = numChannels;
			final int st = t.size( size );

			final int slicelength = sx * sy * sz;
			final Object data = arrayFactory.apply( sx * sy * sz * st );
			final SetLabel output = setLabelFactory.apply( data );
			final Object[] slicedata = new Object[ sc ];

			if ( st == 1 )
			{
				for ( int dc = 0; dc < sc; ++dc )
					slicedata[ dc ] = slice.get( ox, oy, oz, oc + dc, ot, sx, sy, sz );
				final GetLabel input = getLabelFactory.apply( slicedata );
				for ( int i = 0; i < slicelength; ++i )
					output.set( i, input.get( i ) );
			}
			else
			{
				for ( int dt = 0; dt < st; ++dt )
				{
					for ( int dc = 0; dc < sc; ++dc )
						slicedata[ dc ] = slice.get( ox, oy, oz, oc + dc, ot + dt, sx, sy, sz );
					final GetLabel input = getLabelFactory.apply( slicedata );
					final int destpos = dt * slicelength;
					for ( int i = 0; i < slicelength; ++i )
						output.set( i + destpos, input.get( i ) );
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
			case BYTE:
				return ( min, size ) -> ( A ) new DirtyVolatileByteArray( ( byte[] ) ( pixels.get( min, size ) ), true );
			case SHORT:
				return ( min, size ) -> ( A ) new DirtyVolatileShortArray( ( short[] ) ( pixels.get( min, size ) ), true );
			case INT:
				return ( min, size ) -> ( A ) new DirtyVolatileIntArray( ( int[] ) ( pixels.get( min, size ) ), true );
			case LONG:
				return ( min, size ) -> ( A ) new DirtyVolatileLongArray( ( long[] ) ( pixels.get( min, size ) ), true );
			default:
				throw new IllegalArgumentException();
			}
		else
			switch ( primitiveType )
			{
			case BYTE:
				return ( min, size ) -> ( A ) new VolatileByteArray( ( byte[] ) ( pixels.get( min, size ) ), true );
			case SHORT:
				return ( min, size ) -> ( A ) new VolatileShortArray( ( short[] ) ( pixels.get( min, size ) ), true );
			case INT:
				return ( min, size ) -> ( A ) new VolatileIntArray( ( int[] ) ( pixels.get( min, size ) ), true );
			case LONG:
				return ( min, size ) -> ( A ) new VolatileLongArray( ( long[] ) ( pixels.get( min, size ) ), true );
			default:
				throw new IllegalArgumentException();
			}
	}

	private interface GetLabel
	{
		int get( final int index );
	}

	private interface SetLabel
	{
		void set( final int index, final int label );
	}

	static class SetCompositeLabelByte implements SetLabel
	{
		private final byte[] output;

		SetCompositeLabelByte( final Object output )
		{
			this.output = ( byte[] ) output;
		}

		@Override
		public void set( final int index, final int label )
		{
			output[ index ] = ( byte ) label;
		}
	}

	static class SetCompositeLabelShort implements SetLabel
	{
		private final short[] output;

		SetCompositeLabelShort( final Object output )
		{
			this.output = ( short[] ) output;
		}

		@Override
		public void set( final int index, final int label )
		{
			output[ index ] = ( short ) label;
		}
	}

	static class SetCompositeLabelInt implements SetLabel
	{
		private final int[] output;

		SetCompositeLabelInt( final Object output )
		{
			this.output = ( int[] ) output;
		}

		@Override
		public void set( final int index, final int label )
		{
			output[ index ] = ( int ) label;
		}
	}

	static class SetCompositeLabelLong implements SetLabel
	{
		private final long[] output;

		SetCompositeLabelLong( final Object output )
		{
			this.output = ( long[] ) output;
		}

		@Override
		public void set( final int index, final int label )
		{
			output[ index ] = ( long ) label;
		}
	}

	static class GetChannelLabelByte implements GetLabel
	{
		private final byte[][] channels;

		private final int numChannels;

		GetChannelLabelByte( final Object[] input )
		{
			numChannels = input.length;
			channels = new byte[ numChannels ][];
			for ( int c = 0; c < numChannels; ++c )
				channels[ c ] = ( byte[] ) input[ c ];
		}

		@Override
		public int get( final int index )
		{
			for ( int c = 0; c < numChannels; ++c )
				if ( channels[ c ][ index ] != 0 )
					return c + 1;
			return 0;
		}
	}

	static class GetChannelLabelShort implements GetLabel
	{
		private final short[][] channels;

		private final int numChannels;

		GetChannelLabelShort( final Object[] input )
		{
			numChannels = input.length;
			channels = new short[ numChannels ][];
			for ( int c = 0; c < numChannels; ++c )
				channels[ c ] = ( short[] ) input[ c ];
		}

		@Override
		public int get( final int index )
		{
			for ( int c = 0; c < numChannels; ++c )
				if ( channels[ c ][ index ] != 0 )
					return c + 1;
			return 0;
		}
	}

	static class GetChannelLabelFloat implements GetLabel
	{
		private final float[][] channels;

		private final int numChannels;

		GetChannelLabelFloat( final Object[] input )
		{
			numChannels = input.length;
			channels = new float[ numChannels ][];
			for ( int c = 0; c < numChannels; ++c )
				channels[ c ] = ( float[] ) input[ c ];
		}

		@Override
		public int get( final int index )
		{
			for ( int c = 0; c < numChannels; ++c )
				if ( channels[ c ][ index ] != 0 )
					return c + 1;
			return 0;
		}
	}

	// -------------------------------------------------------------------
	//  Writing Imaris blocks as primitive arrays
	// -------------------------------------------------------------------


	@FunctionalInterface
	private interface SetDataSubVolume
	{
		/**
		 * Set sub-volume as flattened primitive array.
		 *
		 * @param data {@code byte[]}, {@code short[]}, {@code float[]}, depending on dataset type.
		 * @param ox offset in X
		 * @param oy offset in Y
		 * @param oz offset in Z
		 * @param oc channel index
		 * @param ot timepoint index
		 * @param sx size in X
		 * @param sy size in Y
		 * @param sz size in Z
		 */
		void set( Object data, int ox, int oy, int oz, int oc, int ot, int sx, int sy, int sz ) throws Error;
	}

	static class GetCompositeLabelByte implements GetLabel
	{
		private final byte[] input;

		GetCompositeLabelByte( final Object input )
		{
			this.input = ( byte[] ) input;
		}

		@Override
		public int get( final int index )
		{
			return input[ index ];
		}
	}

	static class GetCompositeLabelShort implements GetLabel
	{
		private final short[] input;

		GetCompositeLabelShort( final Object input )
		{
			this.input = ( short[] ) input;
		}

		@Override
		public int get( final int index )
		{
			return input[ index ];
		}
	}

	static class GetCompositeLabelInt implements GetLabel
	{
		private final int[] input;

		GetCompositeLabelInt( final Object input )
		{
			this.input = ( int[] ) input;
		}

		@Override
		public int get( final int index )
		{
			return input[ index ];
		}
	}

	static class GetCompositeLabelLong implements GetLabel
	{
		private final long[] input;

		GetCompositeLabelLong( final Object input )
		{
			this.input = ( long[] ) input;
		}

		@Override
		public int get( final int index )
		{
			return ( int ) input[ index ];
		}
	}

	static class SetChannelLabelByte implements SetLabel
	{
		private final byte[][] channels;

		private final int numChannels;

		SetChannelLabelByte( final Object[] output )
		{
			numChannels = output.length;
			channels = new byte[ numChannels ][];
			for ( int c = 0; c < numChannels; ++c )
				channels[ c ] = ( byte[] ) output[ c ];
		}

		@Override
		public void set( final int index, final int label )
		{
			final int v = label - 1;
			for ( int c = 0; c < numChannels; ++c )
				channels[ c ][ index ] = v == c ? ( byte ) 1 : ( byte ) 0;
		}
	}

	static class SetChannelLabelShort implements SetLabel
	{
		private final short[][] channels;

		private final int numChannels;

		SetChannelLabelShort( final Object[] output )
		{
			numChannels = output.length;
			channels = new short[ numChannels ][];
			for ( int c = 0; c < numChannels; ++c )
				channels[ c ] = ( short[] ) output[ c ];
		}

		@Override
		public void set( final int index, final int label )
		{
			final int v = label - 1;
			for ( int c = 0; c < numChannels; ++c )
				channels[ c ][ index ] = v == c ? ( short ) 1 : ( short ) 0;
		}
	}

	static class SetChannelLabelFloat implements SetLabel
	{
		private final float[][] channels;

		private final int numChannels;

		SetChannelLabelFloat( final Object[] output )
		{
			numChannels = output.length;
			channels = new float[ numChannels ][];
			for ( int c = 0; c < numChannels; ++c )
				channels[ c ] = ( float[] ) output[ c ];
		}

		@Override
		public void set( final int index, final int label )
		{
			final int v = label - 1;
			for ( int c = 0; c < numChannels; ++c )
				channels[ c ][ index ] = v == c ? 1f : 0f;
		}
	}

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
		final SetDataSubVolume slice;

		// creates output arrays (to be send to Imaris)
		// Object is byte[], short[], float[], depending on datasetType
		final IntFunction< Object > arrayFactory;

		// creates a GetLabel to read from ArrayDataAccess.getCurrentStorageArray() of primitiveType
		// Object is byte[], short[], int[], long[] (we only support IntegerType)
		final Function< Object, GetLabel > getLabelFactory;

		// creates a SetLabel to write to output array (created by arrayFactory)
		final Function< Object[], SetLabel > setLabelFactory;

		switch ( datasetType )
		{
		case eTypeUInt8:
			slice = ( data, ox, oy, oz, oc, ot, sx, sy, sz ) ->
					dataset.SetDataSubVolumeAs1DArrayBytes( ( byte[] ) data, ox, oy, oz, oc, ot, sx, sy, sz );
			arrayFactory = byte[]::new;
			setLabelFactory = SetChannelLabelByte::new;
			break;
		case eTypeUInt16:
			slice = ( data, ox, oy, oz, oc, ot, sx, sy, sz ) ->
					dataset.SetDataSubVolumeAs1DArrayShorts( ( short[] ) data, ox, oy, oz, oc, ot, sx, sy, sz );
			arrayFactory = short[]::new;
			setLabelFactory = SetChannelLabelShort::new;
			break;
		case eTypeFloat:
			slice = ( data, ox, oy, oz, oc, ot, sx, sy, sz ) ->
					dataset.SetDataSubVolumeAs1DArrayFloats( ( float[] ) data, ox, oy, oz, oc, ot, sx, sy, sz );
			arrayFactory = float[]::new;
			setLabelFactory = SetChannelLabelFloat::new;
			break;
		default:
			throw new IllegalArgumentException();
		}
		switch ( primitiveType )
		{
		case BYTE:
			getLabelFactory = GetCompositeLabelByte::new;
			break;
		case SHORT:
			getLabelFactory = GetCompositeLabelShort::new;
			break;
		case INT:
			getLabelFactory = GetCompositeLabelInt::new;
			break;
		case LONG:
			getLabelFactory = GetCompositeLabelLong::new;
			break;
		default:
			throw new IllegalArgumentException();
		}

		final MapIntervalDimension x = mapIntervalDimension( mapDimensions[ 0 ] );
		final MapIntervalDimension y = mapIntervalDimension( mapDimensions[ 1 ] );
		final MapIntervalDimension z = mapIntervalDimension( mapDimensions[ 2 ] );
		final MapIntervalDimension t = mapIntervalDimension( mapDimensions[ 4 ] );

		final int oc = 0;
		final int sc = numChannels;

		return ( access, min, size ) ->
		{
			final Object data = ( ( ArrayDataAccess ) access ).getCurrentStorageArray();
			final GetLabel input = getLabelFactory.apply( data );

			final int ox = x.min( min );
			final int oy = y.min( min );
			final int oz = z.min( min );
			final int ot = t.min( min );

			final int sx = x.size( size );
			final int sy = y.size( size );
			final int sz = z.size( size );
			final int st = t.size( size );

			final int slicelength = sx * sy * sz;
			final Object[] slicedata = new Object[ sc ];
			for ( int dc = 0; dc < sc; ++dc )
				slicedata[ dc ] = arrayFactory.apply( slicelength );
			final SetLabel output = setLabelFactory.apply( slicedata );

			if ( st == 1 )
			{
				for ( int i = 0; i < slicelength; ++i )
					output.set( i, input.get( i ) );
				for ( int dc = 0; dc < sc; ++dc )
					slice.set( slicedata[ dc ], ox, oy, oz, oc + dc, ot, sx, sy, sz );
			}
			else
			{
				for ( int dt = 0; dt < st; ++dt )
				{
					final int srcpos = dt * slicelength;
					for ( int i = 0; i < slicelength; ++i )
						output.set( i, input.get( i + srcpos ) );
					for ( int dc = 0; dc < sc; ++dc )
						slice.set( slicedata[ dc ], ox, oy, oz, oc + dc, ot + dt, sx, sy, sz );
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
			return backingLoader.get( key );
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
