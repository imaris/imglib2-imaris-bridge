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
package com.bitplane.xt.img;

import com.bitplane.xt.options.ImarisCacheOptions;
import java.util.Set;
import java.util.function.BiConsumer;
import net.imglib2.Dirty;
import net.imglib2.cache.img.optional.AccessOptions;
import net.imglib2.cache.img.optional.CacheOptions;
import net.imglib2.cache.img.optional.CellDimensionsOptions;
import net.imglib2.img.basictypeaccess.AccessFlags;
import org.scijava.optional.AbstractOptions;

/**
 * Optional parameters for constructing a {@link ImarisCachedCellImgFactory}.
 *
 * @author Tobias Pietzsch
 */
public class ImarisCachedCellImgOptions extends AbstractOptions< ImarisCachedCellImgOptions >
		implements
		AccessOptions< ImarisCachedCellImgOptions >,
		CellDimensionsOptions< ImarisCachedCellImgOptions >,
		CacheOptions< ImarisCachedCellImgOptions >,
		ImarisCacheOptions< ImarisCachedCellImgOptions >
{
	public final Values values = new Values();

	public ImarisCachedCellImgOptions()
	{
	}

	/**
	 * Create default {@link ImarisCachedCellImgOptions}.
	 *
	 * @return default {@link ImarisCachedCellImgOptions}.
	 */
	public static ImarisCachedCellImgOptions options()
	{
		return new ImarisCachedCellImgOptions();
	}

	// NB overrides default value
	/**
	 * Specify whether the image should use {@link Dirty} accesses. Dirty
	 * accesses track whether cells were written to. Only cells that were
	 * written to are (potentially) cached to disk.
	 * <p>
	 * This is {@code true} by default.
	 * </p>
	 *
	 * @param dirty
	 *            whether the image should use {@link Dirty} accesses.
	 */
	@Override
	public ImarisCachedCellImgOptions dirtyAccesses( final boolean dirty )
	{
		return AccessOptions.super.dirtyAccesses( dirty );
	}

	@Override
	// TODO: Temporarily made public so that factories from labkit package can access it. Should these be more decoupled?
	public ImarisCachedCellImgOptions append( final ImarisCachedCellImgOptions additionalOptions )
	{
		return super.append( additionalOptions );
	}

	private ImarisCachedCellImgOptions( final ImarisCachedCellImgOptions that )
	{
		super( that );
	}

	@Override
	protected ImarisCachedCellImgOptions copyOrThis()
	{
		return new ImarisCachedCellImgOptions( this );
	}

	public class Values extends AbstractValues implements
			AccessOptions.Val,
			CellDimensionsOptions.Val,
			CacheOptions.Val,
			ImarisCacheOptions.Val
	{
		// NB overrides default value
		@Override
		public int[] cellDimensions()
		{
			return getValueOrDefault( "cellDimensions", null );
		}

		// NB overrides default value
		@Override
		public boolean dirtyAccesses()
		{
			return getValueOrDefault( "dirtyAccesses", true );
		}

		public Set< AccessFlags > accessFlags()
		{
			return AccessFlags.fromBooleansDirtyVolatile( dirtyAccesses(), volatileAccesses() );
		}

		@Override
		public void forEach( final BiConsumer< String, Object > action )
		{
			AccessOptions.Val.super.forEach( action );
			CellDimensionsOptions.Val.super.forEach( action );
			CacheOptions.Val.super.forEach( action );
			ImarisCacheOptions.Val.super.forEach( action );
		}
	}
}
