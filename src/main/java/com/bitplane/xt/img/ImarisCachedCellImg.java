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
package com.bitplane.xt.img;

import Imaris.IDataSetPrx;
import com.bitplane.xt.ImarisApplication;
import com.bitplane.xt.ImarisImg;
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
 *
 * @param <T>
 *            the pixel type
 * @param <A>
 *            the underlying native access type
 *
 * @author Tobias Pietzsch
 */
public class ImarisCachedCellImg< T extends NativeType< T >, A > extends CachedCellImg< T, A >
		implements ImarisImg
{
	private final ImarisCachedCellImgFactory< T > factory;

	private final IDataSetPrx dataset;

	private final Cache< Long, Cell< A > > cache;

	private final IoSync< ?, ?, ? > iosync;

	public ImarisCachedCellImg(
			final ImarisCachedCellImgFactory< T > factory,
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
