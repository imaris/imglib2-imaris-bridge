/*
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
package com.bitplane.xt;

import com.bitplane.xt.img.ImarisCachedCellImgFactory;
import com.bitplane.xt.options.ImarisAxesOptions;
import com.bitplane.xt.options.ImarisCacheOptions;
import com.bitplane.xt.options.ReadOnlyOptions;
import java.lang.ref.SoftReference;
import java.util.function.BiConsumer;
import net.imglib2.cache.img.optional.CacheOptions;
import net.imglib2.cache.img.optional.CellDimensionsOptions;
import org.scijava.optional.AbstractOptions;

/**
 * Optional parameters for creating {@link ImarisDataset}.
 * This allows to tweak details about the cache, etc.
 *
 * @author Tobias Pietzsch
 */
public class ImarisDatasetOptions extends AbstractOptions< ImarisDatasetOptions >
		implements
		CellDimensionsOptions< ImarisDatasetOptions >,
		CacheOptions< ImarisDatasetOptions >,
		ImarisCacheOptions< ImarisDatasetOptions >,
		ImarisAxesOptions< ImarisDatasetOptions >,
		ReadOnlyOptions< ImarisDatasetOptions >
{
	public final Values values = new Values();

	public ImarisDatasetOptions()
	{
	}

	/**
	 * Override the dimensions of a cell.
	 * <p>
	 * By default, the block dimensions of the Imaris dataset are used.
	 * <p>
	 * The argument is extended or truncated as necessary. For example if {@code
	 * cellDimensions=[64,32]} then for creating a 3D image it will be augmented
	 * to {@code [64,32,32]}. For creating a 1D image it will be truncated to
	 * {@code [64]}.
	 *
	 * @param cellDimensions
	 *            dimensions of a cell.
	 */
	@Override
	public ImarisDatasetOptions cellDimensions( final int... cellDimensions )
	{
		return CellDimensionsOptions.super.cellDimensions( cellDimensions );
	}

	// TODO: why is {@inheritDoc} not working here?
	/**
	 * Which in-memory cache type to use. The options are
	 * <ul>
	 * <li>{@link CacheType#SOFTREF SOFTREF}: The cache keeps SoftReferences to
	 * values (cells), basically relying on GC for removal. The advantage of
	 * this is that many caches can be created without needing to put a limit on
	 * the size of any of them. GC will take care of balancing that. The
	 * downside is that {@link OutOfMemoryError} may occur because
	 * {@link SoftReference}s are cleared too slow. SoftReferences are not
	 * collected for a certain time after they have been used. If there is heavy
	 * thrashing with cells being constantly swapped in and out from disk then
	 * OutOfMemory may happen because of this. This sounds worse than it is in
	 * practice and should only happen in pathological situations. Tuning the
	 * {@code -XX:SoftRefLRUPolicyMSPerMB} JVM flag does often help.</li>
	 * <li>{@link CacheType#BOUNDED BOUNDED}: The cache keeps strong references
	 * to a limited number of values (cells). The advantage is that there is
	 * never OutOfMemory because of the issues described above (fingers
	 * crossed). The downside is that the number of cells that should be cached
	 * needs to be specified beforehand. So {@link OutOfMemoryError} may occur
	 * if many caches are opened and consume too much memory in total.</li>
	 * </ul>
	 *
	 * @param cacheType
	 *            which cache type to use (default is {@code SOFTREF}).
	 */
	@Override
	public ImarisDatasetOptions cacheType( final CacheType cacheType )
	{
		return CacheOptions.super.cacheType( cacheType );
	}

	// TODO: why is {@inheritDoc} not working here?
	/**
	 * Set the maximum number of values (cells) to keep in the cache. This is
	 * only used if {@link #cacheType(CacheType)} is {@link CacheType#BOUNDED}.
	 *
	 * @param maxCacheSize
	 *            maximum number of values in the cache (default is 1000).
	 */
	@Override
	public ImarisDatasetOptions maxCacheSize( final long maxCacheSize )
	{
		return CacheOptions.super.maxCacheSize( maxCacheSize );
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ImarisDatasetOptions numIoThreads( final int numIoThreads )
	{
		return ImarisCacheOptions.super.numIoThreads( numIoThreads );
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ImarisDatasetOptions maxIoQueueSize( final int maxIoQueueSize )
	{
		return ImarisCacheOptions.super.maxIoQueueSize( maxIoQueueSize );
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ImarisDatasetOptions includeAxes( final Axis... axes )
	{
		return ImarisAxesOptions.super.includeAxes( axes );
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ImarisDatasetOptions readOnly()
	{
		return ReadOnlyOptions.super.readOnly();
	}

	/**
	 * Create default {@link ImarisDatasetOptions}.
	 *
	 * @return default {@link ImarisDatasetOptions}.
	 */
	public static ImarisDatasetOptions options()
	{
		return new ImarisDatasetOptions();
	}

	private ImarisDatasetOptions( final ImarisDatasetOptions that )
	{
		super( that );
	}

	@Override
	protected ImarisDatasetOptions copyOrThis()
	{
		return new ImarisDatasetOptions( this );
	}

	public class Values extends AbstractValues implements
			CellDimensionsOptions.Val,
			CacheOptions.Val,
			ImarisCacheOptions.Val,
			ImarisAxesOptions.Val,
			ReadOnlyOptions.Val
	{
		// NB overrides default value
		@Override
		public int[] cellDimensions()
		{
			return getValueOrDefault( "cellDimensions", null );
		}

		@Override
		public void forEach( final BiConsumer< String, Object > action )
		{
			CellDimensionsOptions.Val.super.forEach( action );
			CacheOptions.Val.super.forEach( action );
			ImarisCacheOptions.Val.super.forEach( action );
			ImarisAxesOptions.Val.super.forEach( action );
			ReadOnlyOptions.Val.super.forEach( action );
		}
	}
}
