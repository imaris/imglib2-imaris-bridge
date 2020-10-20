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
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import net.imagej.axis.Axes;
import net.imagej.axis.DefaultLinearAxis;
import net.imglib2.cache.CacheLoader;
import net.imglib2.cache.CacheRemover;
import net.imglib2.cache.IoSync;
import net.imglib2.cache.img.AccessIo;
import net.imglib2.cache.img.DiskCellCache;
import net.imglib2.img.basictypeaccess.array.ArrayDataAccess;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileByteArray;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileFloatArray;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileShortArray;
import net.imglib2.img.cell.Cell;
import net.imglib2.img.cell.CellGrid;
import net.imglib2.util.Fraction;
import net.imglib2.util.Intervals;

/**
 * Basic {@link CacheRemover}/{@link CacheLoader} for writing/reading cells
 * to an Imaris {@code IDataset}.
 * <p>
 * TODO: How, can we even know this?
 * Blocks which are not in the cache (yet) are obtained from a backing
 * {@link CacheLoader}. Typically the backing loader will just create empty cells.
 * </p>
 * <p><em>
 * A {@link DiskCellCache} should be connected to a in-memory cache through
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

	private final Fraction entitiesPerPixel;

	private final AccessIo< A > accessIo;

	private PixelSource< A > volatileArraySource = volatileArraySource();

	private PixelSink< A > volatileArraySink = volatileArraySink();

	// TODO for now, we always load/save to imaris
	// private final CacheLoader< Long, Cell< A > > backingLoader;

	public ImarisCellCache(
			final IDataSetPrx dataset,
			final CellGrid grid,
			final AccessIo< A > accessIo,
			final Fraction entitiesPerPixel ) throws Error
	{
		this.dataset = dataset;
		this.datasetType = dataset.GetType();

		this.grid = grid;
		this.n = grid.numDimensions();
		this.entitiesPerPixel = entitiesPerPixel;
		this.accessIo = accessIo;

		// This is also initialized in ImarisDataset in the same way
		// TODO: refactor
		mapDimensions = new int[] { 0, 1, -1, -1, -1 };
		int d = 2;
		if ( dataset.GetSizeZ() > 1 )
			mapDimensions[ 2 ] = d++;
		if ( dataset.GetSizeC() > 1 )
			mapDimensions[ 3 ] = d++;
		if ( dataset.GetSizeT() > 1 )
			mapDimensions[ 4 ] = d;
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
	 * For {@code Img} dimensions with size=1 are skipped present.
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

	/**
	 * Get the appropriate {@code GetDataSubVolume} for {@link #datasetType}.
	 */
	private GetDataSubVolume dataSource()
	{
		switch ( datasetType )
		{
		case eTypeUInt8:
			return dataset::GetDataSubVolumeAs1DArrayBytes;
		case eTypeUInt16:
			return dataset::GetDataSubVolumeAs1DArrayShorts;
		case eTypeFloat:
			return dataset::GetDataSubVolumeAs1DArrayFloats;
		default:
			throw new IllegalArgumentException();
		}
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
	 * Apply {@link #mapDimensions} to {@link #dataSource}.
	 */
	// TODO: Rename, "volatileArray" part seems not so relevant, it's just to distinguish the various
	//  PixelSource kinds flying around. There must be a better way to do this.
	private PixelSource< A > volatileArraySource()
	{
		final GetDataSubVolume getDataSubVolume = dataSource();

		// Apply mapDimensions to getDataSubVolume
		final MapIntervalDimension x = mapIntervalDimension( mapDimensions[ 0 ] );
		final MapIntervalDimension y = mapIntervalDimension( mapDimensions[ 1 ] );
		final MapIntervalDimension z = mapIntervalDimension( mapDimensions[ 2 ] );
		final MapIntervalDimension c = mapIntervalDimension( mapDimensions[ 3 ] );
		final MapIntervalDimension t = mapIntervalDimension( mapDimensions[ 4 ] );
		final PixelSource< ? > pixels = ( min, size ) -> getDataSubVolume.get(
				x.min( min ), y.min( min ), z.min( min ), c.min( min ), t.min( min ),
				x.size( size ), y.size( size ), z.size( size ) );

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

	// ===================================================================
	// ===================================================================
	// ===================================================================

	@FunctionalInterface
	private interface SetDataSubVolume< A >
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
		void set( A data, int ox, int oy, int oz, int oc, int ot, int sx, int sy, int sz ) throws Error;
	}

	/**
	 * Get the appropriate {@code SetDataSubVolume} for {@link #datasetType}.
	 */
	// TODO: Rename. "Sink" is not the best name probably, despite pairing up with "Source" nicely?
	private SetDataSubVolume< ? > dataSink()
	{
		switch ( datasetType )
		{
		case eTypeUInt8:
			return ( SetDataSubVolume< byte[] > ) dataset::SetDataSubVolumeAs1DArrayBytes;
		case eTypeUInt16:
			return ( SetDataSubVolume< short[] > ) dataset::SetDataSubVolumeAs1DArrayShorts;
		case eTypeFloat:
			return ( SetDataSubVolume< float[] > ) dataset::SetDataSubVolumeAs1DArrayFloats;
		default:
			throw new IllegalArgumentException();
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
	 * Apply {@link #mapDimensions} to {@link #dataSink}.
	 */
	// TODO: Rename, "volatileArray" part seems not so relevant, it's just to distinguish the various
	//  PixelSource kinds flying around. There must be a better way to do this.
	// TODO: Rename. "Sink" is not the best name probably, despite pairing up with "Source" nicely?
	private PixelSink< A > volatileArraySink()
	{
		final SetDataSubVolume setDataSubVolume = dataSink();

		// Apply mapDimensions to getDataSubVolume
		final MapIntervalDimension x = mapIntervalDimension( mapDimensions[ 0 ] );
		final MapIntervalDimension y = mapIntervalDimension( mapDimensions[ 1 ] );
		final MapIntervalDimension z = mapIntervalDimension( mapDimensions[ 2 ] );
		final MapIntervalDimension c = mapIntervalDimension( mapDimensions[ 3 ] );
		final MapIntervalDimension t = mapIntervalDimension( mapDimensions[ 4 ] );
		return ( data, min, size ) -> setDataSubVolume.set(
				( ( ArrayDataAccess ) data ).getCurrentStorageArray(),
				x.min( min ), y.min( min ), z.min( min ), c.min( min ), t.min( min ),
				x.size( size ), y.size( size ), z.size( size ) );
	}

	// ===================================================================
	// ===================================================================
	// ===================================================================















	@Override
	public Cell< A > get( final Long key ) throws Exception
	{
		final long index = key;
		final long[] cellMin = new long[ n ];
		final int[] cellDims = new int[ n ];
		// TODO
		// TODO
		// TODO reuse
		final PixelSource< A > s = volatileArraySource();
		// TODO
		// TODO
		// TODO
		grid.getCellDimensions( index, cellMin, cellDims );
		return new Cell<>(
				cellDims,
				cellMin,
				s.get( cellMin, cellDims ) );
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
		// TODO
		// TODO
		// TODO reuse
		final PixelSink< A > s = volatileArraySink();
		// TODO
		// TODO
		// TODO
		try
		{
			s.put( valueData, cellMin, cellDims );
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
}
