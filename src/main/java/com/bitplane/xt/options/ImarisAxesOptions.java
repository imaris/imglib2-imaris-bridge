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
package com.bitplane.xt.options;

import java.util.function.BiConsumer;
import org.scijava.optional.Options;
import org.scijava.optional.Values;

/**
 * Option to specify which axes should be included (at least) when mapping an
 * Imaris dataset to a ImgLib2 image.
 *
 * @author Tobias Pietzsch
 */
public interface ImarisAxesOptions< T > extends Options< T >
{
	enum Axis
	{
		X,Y,Z,C,T
	}

	/**
	 * Specify which axes should be included (at least) when mapping an Imaris
	 * dataset to a ImgLib2 image.
	 * <p>
	 * In Imaris, datasets are always 5D: for example, a 2D dataset (without channel
	 * or time) is represented as 5D with {@code size=1} along Z, C, T axes. In
	 * ImgLib2, there is a distinction between a 2D image and a 5D image with {@code
	 * size=1} along the 3rd, 4th, and 5th dimension. Therefore, there are several
	 * ways to represent such a dataset in ImgLib2.
	 * <p>
	 * By default, axes Z, C, and T are not represented in ImgLib2 if the
	 * dataset size along those axes is {@code s=1}. By specifying these axes as
	 * {@code includeAxes()} arguments, this can be overridden. For example, an
	 * Imaris dataset with size {@code {100, 100, 1, 1, 1}} would be represented
	 * as a 2D ImgLib2 image with size {@code {100, 100}}. Specifying {@code
	 * includeAxes(Z, T)}, would result in a 4D ImgLib2 image with size {@code
	 * {100,100,1,1}}.
	 *
	 * @param axes
	 * 		the axes that should be included (at least) when mapping the Imaris dataset to ImgLib2 image.
	 */
	default T includeAxes( final Axis... axes )
	{
		return setValue( "includeAxes", axes );
	}

	interface Val extends Values
	{
		default void forEach( BiConsumer< String, Object > action )
		{
			action.accept( "includeAxes", includeAxes() );
		}

		default Axis[] includeAxes()
		{
			return getValueOrDefault( "includeAxes", new Axis[ 0 ] );
		}
	}
}
