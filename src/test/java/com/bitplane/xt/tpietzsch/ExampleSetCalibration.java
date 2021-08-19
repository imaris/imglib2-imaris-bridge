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
import bdv.util.BdvFunctions;
import bdv.util.BdvStackSource;
import com.bitplane.xt.DatasetCalibration;
import com.bitplane.xt.ImarisDataset;
import com.bitplane.xt.ImarisService;
import org.scijava.Context;

public class ExampleSetCalibration
{
	public static void main( String[] args ) throws Error, InterruptedException
	{
		final Context context = new Context();
		final ImarisService imaris = context.getService( ImarisService.class );
		final ImarisDataset< ? > dataset = imaris.getApplication().getDataset();

		final DatasetCalibration original = dataset.getCalibration();
		System.out.println( "dataset.getCalibration() = " + original );

		final DatasetCalibration stretched = new DatasetCalibration(
				original.unit(),
				original.voxelSize( 0 ),
				original.voxelSize( 1 ),
				original.voxelSize( 2 ) * 1.2,
				original.min( 0 ),
				original.min( 1 ),
				original.min( 2 ) );

		final BdvStackSource< ? > source = BdvFunctions.show( dataset );

		while( true )
		{
			dataset.setCalibration( stretched );
			source.getBdvHandle().getViewerPanel().requestRepaint();
			Thread.sleep( 1000 );

			dataset.setCalibration( original );
			source.getBdvHandle().getViewerPanel().requestRepaint();
			Thread.sleep( 1000 );
		}
	}
}
