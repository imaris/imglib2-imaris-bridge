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
import com.bitplane.xt.util.PixelSource;
import java.util.concurrent.CompletableFuture;
import net.imglib2.cache.CacheLoader;
import net.imglib2.cache.CacheRemover;
import net.imglib2.img.cell.Cell;
import net.imglib2.img.cell.CellGrid;

/**
 * A {@link CacheLoader}/{@link CacheRemover} for reading cells
 * from an Imaris {@code IDataset}.
 * <p>
 * This implementation is intended for read-only images, so the
 * {@link CacheRemover} interface is implemented to do nothing.
 * </p>
 *
 * @param <A>
 *            access type
 *
 * @author Tobias Pietzsch
 */
public class ImarisLoader< A > implements CacheRemover< Long, Cell< A >, A >, CacheLoader< Long, Cell< A > >
{
	protected final CellGrid grid;

	protected final int n;

	private final PixelSource< A > volatileArraySource;

	/**
	 * TODO
	 *
	 * @param dataset
	 * @param mapDimensions
	 * 		maps Imaris dimension indices to imglib2 dimension indices.
	 * 		If {@code i} is dimension index from Imaris (0..4 means X,Y,Z,C,T)
	 * 		then {@code mapDimensions[i]} is the corresponding dimension in {@code Img}.
	 * 		For {@code Img} dimensions with size=1 are skipped.
	 * 		E.g., for a X,Y,C image {@code mapDimensions = {0,1,-1,2,-1}}.
	 * @param grid
	 *
	 * @throws Error
	 */
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
		this.grid = grid;
		n = grid.numDimensions();
		volatileArraySource = PixelSource.volatileArraySource( dataset, dataset.GetType(), mapDimensions, withDirtyFlag );
	}

	@Override
	public Cell< A > get( final Long key ) throws Exception
	{
		final long[] cellMin = new long[ n ];
		final int[] cellDims = new int[ n ];
		grid.getCellDimensions( key, cellMin, cellDims );
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
		return CompletableFuture.completedFuture( null );
	}
}
