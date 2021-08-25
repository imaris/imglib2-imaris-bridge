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
package com.bitplane.xt;

import bdv.util.BdvFunctions;
import bdv.util.BdvStackSource;
import org.scijava.Context;

public class ExampleBdv
{
	public static void main( String[] args )
	{
		/*
		 * Create a SciJava context, and obtain the ImarisService instance.
		 *
		 * Note that, when you run out of Fiji, the context is usually already
		 * available. When writing plugins or scripts, you just use a @Parameter
		 * annotation to get the ImarisService.
		 */
		final Context context = new Context();
		final ImarisService imaris = context.getService( ImarisService.class );

		/*
		 * Get the currently open dataset from the first (and typically only)
		 * Imaris application.
		 */
		final ImarisDataset< ? > dataset = imaris.getApplication().getDataset();

		/*
		 * Show the multiresolution version in BigDataViewer.
		 */
		final BdvStackSource< ? > source = BdvFunctions.show( dataset );
	}
}
