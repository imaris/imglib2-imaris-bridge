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
package com.bitplane.xt.util;

import Imaris.IDataSetPrx;
import net.imglib2.img.cell.CellGrid;
import net.imglib2.util.Fraction;
import net.imglib2.util.Intervals;

/**
 * Create appropriate {@code CellGrid} for wrapping {@code IDataSetPrx} into
 * {@code CachedCellImg}.
 *
 * @author Tobias Pietzsch
 */
public final class CellGridUtils
{
	/**
	 * Creates a {@code CellGrid} with the given {@code dimensions} and {@code
	 * cellDimensions}. Also checks whether the given {@code cellDimensions}
	 * lead to cells that are too large to fit into one primitive array.
	 */
	public static CellGrid createCellGrid(
			final long[] dimensions,
			final int[] cellDimensions,
			final Fraction entitiesPerPixel )
	{
		final long numEntities = entitiesPerPixel.mulCeil( Intervals.numElements( cellDimensions ) );
		if ( numEntities > Integer.MAX_VALUE )
			throw new IllegalArgumentException( "Number of entities in cell too large. Use smaller cell size." );
		return new CellGrid( dimensions, cellDimensions );
	}

	/**
	 * Compute the cell dimensions from block size of {@code dataset},
	 * optionally overridden by user-defined {@code optionalCellDimensions}.
	 *
	 * @param dataset
	 * 		dataset handle (to get block sizes from)
	 * @param invMapDimensions
	 * 		maps imglib2 dimensions to imaris dimensions.  Note that the
	 *  	length of this array is the number of imglib dimensions
	 * @param optionalCellDimensions
	 * 		optionally overrides cell dimensions derived from the dataset
	 *  	(can be {@code null} otherwise). This is truncated or extended (by
	 *  	replicating the last element) to the required number of dimensions
	 *
	 * @return imglib2 cell dimensions.
	 */
	public static int[] computeCellDimensions(
			final IDataSetPrx dataset,
			final int[] invMapDimensions,
			final int[] optionalCellDimensions )
	{
		final int n = invMapDimensions.length;
		final int[] cellDimensions = new int[ n ];
		if ( optionalCellDimensions == null )
		{
			final int[] blockSizes = dataset.GetPyramidBlockSizes()[ 0 ];
			for ( int d = 0; d < n; d++ )
			{
				// imglib dimension d maps to imaris dimension i
				final int i = invMapDimensions[ d ];
				cellDimensions[ d ] = i < 3 ? blockSizes[ i ] : 1;
			}
		}
		else
		{
			final int max = optionalCellDimensions.length - 1;
			for ( int d = 0; d < n; d++ )
			{
				cellDimensions[ d ] = optionalCellDimensions[ Math.min( d, max ) ];
				if ( invMapDimensions[ d ] < 0 )
					cellDimensions[ d ] = 1;
			}
		}
		return cellDimensions;
	}

	private CellGridUtils() {}
}
