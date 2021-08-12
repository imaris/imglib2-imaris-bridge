/*
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

import com.bitplane.xt.img.ImarisCachedCellImgFactory;
import java.util.function.BiConsumer;
import net.imglib2.cache.img.optional.CacheOptions;
import net.imglib2.cache.img.optional.CellDimensionsOptions;
import org.scijava.optional.AbstractOptions;

/**
 * Optional parameters for constructing a {@link ImarisCachedCellImgFactory}.
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
	 * Create default {@link ImarisDatasetOptions}.
	 *
	 * @return default {@link ImarisDatasetOptions}.
	 */
	public static ImarisDatasetOptions options()
	{
		return new ImarisDatasetOptions();
	}

	@Override
	// TODO: Temporarily made public so that factories from labkit package can access it. Should these be more decoupled?
	public ImarisDatasetOptions append( final ImarisDatasetOptions additionalOptions )
	{
		return super.append( additionalOptions );
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
