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

import Imaris.IDataSetPrx;
import com.bitplane.xt.ImarisApplication;
import net.imglib2.cache.Cache;
import net.imglib2.cache.IoSync;
import net.imglib2.cache.img.CachedCellImg;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.cell.Cell;
import net.imglib2.img.cell.CellGrid;
import net.imglib2.img.cell.LazyCellImg;
import net.imglib2.type.NativeType;
import net.imglib2.util.Fraction;

/**
 * A {@link LazyCellImg} that creates empty Cells lazily when they are accessed
 * and sends (modified) Cells to Imaris when memory runs full.
 * <p>
 * At each pixel the image is assumed to represent a probability distribution
 * over channels. For storing to Imaris {@code IDataSetPrx} backing cache, the
 * first channel ("background") is removed (only the other channels are stored).
 * If the dataset has UINT8 or UINT16 type, the [0, 1] range is scaled to the
 * [0, 2^8-1] or [0, 2^16-1], respectively. For loading data back from the cache
 * this operation is reversed.
 *
 * @param <T>
 *            the pixel type
 * @param <A>
 *            the underlying native access type
 *
 * @author Tobias Pietzsch
 */
public class ImarisCachedProbabilitiesImg< T extends NativeType< T >, A > extends CachedCellImg< T, A >
		implements ImarisImg
{
	private final ImarisCachedProbabilitiesImgFactory< T > factory;

	private final IDataSetPrx dataset;

	private final Cache< Long, Cell< A > > cache;

	private final IoSync< ?, ?, ? > iosync;

	public ImarisCachedProbabilitiesImg(
			final ImarisCachedProbabilitiesImgFactory< T > factory,
			final IDataSetPrx dataset,
			final CellGrid grid,
			final Fraction entitiesPerPixel,
			final Cache< Long, Cell< A > > cache,
			final IoSync< ?, ?, ? > iosync,
			final A accessType )
	{
		super( grid, entitiesPerPixel, cache, accessType );
		this.factory = factory;
		this.dataset = dataset;
		this.cache = cache;
		this.iosync = iosync;
	}

	@Override
	public ImgFactory< T > factory()
	{
		return factory;
	}

	/**
	 * Shutdown the internal {@link IoSync} to free resources via
	 * {@link IoSync#shutdown()}. No data will be written to disk after
	 * shutdown.
	 */
	public void shutdown()
	{
		iosync.shutdown();
	}

	/**
	 * Persist all changes back to Imaris
	 */
	@Override
	public void persist()
	{
		cache.persistAll();
	}

	@Override
	public IDataSetPrx getIDataSetPrx()
	{
		return dataset;
	}

	@Override
	public ImarisApplication getApplication()
	{
		return factory.getApplication();
	}
}
