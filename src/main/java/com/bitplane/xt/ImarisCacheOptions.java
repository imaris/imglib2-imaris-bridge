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

import com.bitplane.xt.img.ImarisCachedCellImg;
import com.bitplane.xt.img.ImarisCachedCellImgFactory;
import java.util.function.BiConsumer;
import net.imglib2.cache.img.CellLoader;
import org.scijava.optional.Options;
import org.scijava.optional.Values;

/**
 * TODO
 * TODO
 * TODO
 * TODO
 * TODO
 * TODO
 * Optional arguments that specify the properties of {@link ImarisCachedCellImg}.
 */
public interface ImarisCacheOptions< T > extends Options< T >
{
	/**
	 * The specified number of threads is started to handle asynchronous writing
	 * of values that are evicted from the memory cache.
	 *
	 * @param numIoThreads
	 *            how many writer threads to start (default is 1).
	 */
	default T numIoThreads( final int numIoThreads )
	{
		return setValue( "numIoThreads", numIoThreads );
	}

	/**
	 * Set the maximum size of the disk write queue. When the queue is full,
	 * removing entries from the cache will block until earlier values have been
	 * written.
	 * <p>
	 * Because processing of removed entries is done whenever the cache is
	 * accessed, this may also block accesses to the cache. (This is a good
	 * thing, because it avoids running out of memory because entries cannot be
	 * cleared fast enough...)
	 * </p>
	 *
	 * @param maxIoQueueSize
	 *            the maximum size of the write queue (default is 10).
	 */
	default T maxIoQueueSize( final int maxIoQueueSize )
	{
		return setValue( "maxIoQueueSize", maxIoQueueSize );
	}

	/**
	 * Specify whether cells initialized by a {@link CellLoader} should be
	 * marked as dirty. It is useful to set this to {@code true} if
	 * initialization is a costly operation. By this, it is made sure that cells
	 * are initialized only once, and then written and retrieved from Imaris
	 * when they are next required.
	 * <p>
	 * This is {@code false} by default.
	 * </p>
	 * <p>
	 * This option only has an effect for {@link ImarisCachedCellImg} that are
	 * created with a {@link CellLoader}
	 * ({@link ImarisCachedCellImgFactory#create(long[], CellLoader)}).
	 * </p>
	 *
	 * @param initializeAsDirty
	 *            whether cells initialized by a {@link CellLoader} should be
	 *            marked as dirty.
	 */
	default T initializeCellsAsDirty( final boolean initializeAsDirty )
	{
		return setValue( "initializeCellsAsDirty", initializeAsDirty );
	}

	/**
	 * TODO
	 * TODO
	 * TODO
	 * TODO
	 * TODO
	 * TODO
	 * TODO
	 * TODO
	 * Note that it doesn't make much sense to use this with {@code
	 * #initializeCellsAsDirty}: by definition newly initialized cells will by
	 * clean because they are immediately persisted to backing storage.
	 *
	 * @param persistOnLoad
	 */
	default T persistOnLoad( final boolean persistOnLoad )
	{
		return setValue( "persistOnLoad", persistOnLoad );
	}

	interface Val extends Values
	{
		default void forEach( BiConsumer< String, Object > action )
		{
			action.accept( "numIoThreads", numIoThreads() );
			action.accept( "maxIoQueueSize", maxIoQueueSize() );
			action.accept( "persistOnLoad", persistOnLoad() );
			action.accept( "initializeCellsAsDirty", initializeCellsAsDirty() );
		}

		default int numIoThreads()
		{
			return getValueOrDefault( "numIoThreads", 1 );
		}

		default int maxIoQueueSize()
		{
			return getValueOrDefault( "maxIoQueueSize", 10 );
		}

		default boolean persistOnLoad()
		{
			return getValueOrDefault( "persistOnLoad", false );
		}

		default boolean initializeCellsAsDirty()
		{
			return getValueOrDefault( "initializeCellsAsDirty", false );
		}
	}
}
