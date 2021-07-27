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
import com.bitplane.xt.util.MapDimensions.SelectIntervalDimension;
import com.bitplane.xt.util.PixelSource;
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
import net.imglib2.img.cell.Cell;
import net.imglib2.img.cell.CellGrid;

import static com.bitplane.xt.util.MapDimensions.selectIntervalDimension;

/**
 * Basic {@link CacheRemover}/{@link CacheLoader} for writing/reading cells
 * to an Imaris {@code IDataset}.
 * <p>
 * Blocks which are not in the cache (yet) are obtained from a backing
 * {@link CacheLoader}. Typically the backing loader will just create empty cells.
 * </p>
 * <p><em>
 * A {@link ImarisLoader} should be connected to a in-memory cache through
 * {@link IoSync} if the cache will be used concurrently by multiple threads!
 * </em></p>
 *
 * @param <A>
 *            access type
 *
 * @author Tobias Pietzsch
 */
public class ImarisLoader< A > implements CacheRemover< Long, Cell< A >, A >, CacheLoader< Long, Cell< A > >
{
	private final IDataSetPrx dataset;

	private final tType datasetType;

	private final CellGrid grid;

	private final int n;

	private final PixelSource< A > volatileArraySource;

	public ImarisLoader(
			final IDataSetPrx dataset,
			final int[] mapDimensions,
			final CellGrid grid ) throws Error
	{
		this( dataset, mapDimensions, grid, false );
	}

	protected ImarisLoader(
			final IDataSetPrx dataset,
			final int[] mapDimensions,
			final CellGrid grid,
			final boolean withDirtyFlag ) throws Error
	{
		this.dataset = dataset;
		datasetType = dataset.GetType();
		this.grid = grid;
		n = grid.numDimensions();
		volatileArraySource = PixelSource.volatileArraySource( dataset, datasetType, mapDimensions, withDirtyFlag );
	}

	@Override
	public Cell< A > get( final Long key ) throws Exception
	{
		final long index = key;
		final long[] cellMin = new long[ n ];
		final int[] cellDims = new int[ n ];
		grid.getCellDimensions( index, cellMin, cellDims );
		return new Cell<>(
				cellDims,
				cellMin,
				volatileArraySource.get( 0, cellMin, cellDims ) );
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
	}

	@Override
	public CompletableFuture< Void > persist( final Long key, final A valueData )
	{
		onRemoval( key, valueData );
		return CompletableFuture.completedFuture( null );
	}
}
