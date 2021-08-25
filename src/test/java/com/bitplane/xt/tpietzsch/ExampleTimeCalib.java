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
package com.bitplane.xt.tpietzsch;

import Imaris.Error;
import com.bitplane.xt.ImarisDataset;
import com.bitplane.xt.ImarisService;
import net.imagej.ImageJ;

/**
 * TODO
 *
 * @author Tobias Pietzsch
 */
public class ExampleTimeCalib
{
	public static void main( final String[] args ) throws Error
	{
		final ImageJ ij = new ImageJ();
		final ImarisService imaris = ij.get( ImarisService.class );
		final ImarisDataset< ? > dsIn = imaris.getApplication().getDataset();

		final double tDelta = dsIn.getIDataSetPrx().GetTimePointsDelta();
		System.out.println( "tDelta = " + tDelta );
		for ( int i = 0; i < dsIn.getIDataSetPrx().GetSizeT(); ++i )
		{
			final String timepoint = dsIn.getIDataSetPrx().GetTimePoint( i );
			System.out.println( "i = " + i + " timepoint = " + timepoint );
		}
//		ij.ui().showUI();
//		ij.ui().show( dsIn.asDataset() );
	}
}
