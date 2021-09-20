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

import Imaris.Error;
import Imaris.IDataSetPrx;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import net.imglib2.cache.CacheLoader;
import net.imglib2.cache.CacheRemover;
import net.imglib2.cache.IoSync;
import net.imglib2.img.cell.Cell;
import net.imglib2.img.cell.CellGrid;

/**
 * Basic {@link CacheLoader}/{@link CacheRemover} for writing/reading cells
 * to an Imaris {@code IDataset}.
 * <p>
 * Blocks which are not in the cache (yet) are obtained from a backing
 * {@link CacheLoader}. Typically the backing loader will just create empty cells.
 * </p>
 * <p><em>
 * A {@link ImarisLoaderRemover} should be connected to a in-memory cache through
 * {@link IoSync} if the cache will be used concurrently by multiple threads!
 * </em></p>
 *
 * @param <A>
 *            access type
 *
 * @author Tobias Pietzsch
 */
public class ImarisLoaderRemover< A > extends ImarisLoader< A >
{
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
	 * @param backingLoader
	 * @param persistOnLoad
	 *
	 * @throws Error
	 */
	public ImarisLoaderRemover(
			final IDataSetPrx dataset,
			final int[] mapDimensions,
			final CellGrid grid,
			final CacheLoader< Long, Cell< A > > backingLoader,
			final boolean persistOnLoad ) throws Error
	{
		this( dataset, mapDimensions, grid, backingLoader, persistOnLoad, false );
	}

	protected ImarisLoaderRemover(
			final IDataSetPrx dataset,
			final int[] mapDimensions,
			final CellGrid grid,
			final CacheLoader< Long, Cell< A > > backingLoader,
			final boolean persistOnLoad,
			final boolean withDirtyFlag ) throws Error
	{
		super( dataset, mapDimensions, grid, 0, withDirtyFlag );
		this.backingLoader = backingLoader;
		this.persistOnLoad = persistOnLoad;
		volatileArraySink = PixelSink.volatileArraySink( dataset, dataset.GetType(), mapDimensions );
		written = backingLoader == null ? null : ConcurrentHashMap.newKeySet();
	}

	@Override
	public Cell< A > get( final Long key ) throws Exception
	{
		if ( written == null || written.contains( key ) )
			return super.get( key );
		else
		{
			final Cell< A > cell = backingLoader.get( key );
			if ( persistOnLoad )
				onRemovalImp( key, cell.getData() );
			return cell;
		}
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

	// For now, we always load/save to imaris, i.e., there is no "clean
	// version", so invalidate() doesn't do anything.
	//
	// Later, this should possibly clear the Imaris dataset (is there a function for this?).
	// It should clear the map of written blocks (blocks that were written to imaris once,
	// and will therefore be loaded from imaris when they are next needed).

	// TODO there should be a method to say that the image has been modified on the imaris side.
	//  This would then clear the cache and mark all cells as written, so that they will be loaded from Imaris always.
}
