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
package com.bitplane.xt.examples.advanced;

import Imaris.Error;
import Imaris.IDataSetPrx;
import bdv.util.BdvFunctions;
import bdv.util.BdvStackSource;
import com.bitplane.xt.ImarisDataset;
import com.bitplane.xt.ImarisService;
import java.util.Arrays;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.view.Views;
import org.scijava.Context;

public class ExampleBdvWritable
{
	public static void main( String[] args ) throws InterruptedException, Error
	{
		final Context context = new Context();
		final ImarisService imaris = context.getService( ImarisService.class );
		final ImarisDataset< ? > dataset = imaris.getApplication().createDataset( new UnsignedByteType(), 600, 400, 100, 0, 0 );

		final IDataSetPrx ds = dataset.getIDataSetPrx();
		final int[][] pyramidSizes = ds.GetPyramidSizes();
		final int[][] pyramidBlockSizes = ds.GetPyramidBlockSizes();
		final int numResolutions = pyramidSizes.length;

		System.out.println( "numResolutions = " + numResolutions );
		System.out.println( "pyramidSizes = " + Arrays.deepToString( pyramidSizes ) );
		System.out.println( "pyramidBlockSizes = " + Arrays.deepToString( pyramidBlockSizes ) );

		final BdvStackSource< ? > source = BdvFunctions.show( dataset );
		Thread.sleep( 3000 );

		System.out.println( "type = " + dataset.getType().getClass() );
		final Img< UnsignedByteType > img = ( Img< UnsignedByteType > ) dataset.asImg();
		final long[] dims = img.dimensionsAsLongArray();
		final long[] min = new long[ dims.length ];
		final long[] max = new long[ dims.length ];
		for ( int d = 0; d < dims.length; ++d )
		{
			min[ d ] = dims[ d ] / 2 - dims[ d ] / 8;
			max[ d ] = dims[ d ] / 2 + dims[ d ] / 8;
		}
		Views.interval( img, min, max ).forEach( t -> t.set( 200 ) );

		System.out.println("persisting");
		dataset.persist();

		System.out.println("setting dataset");
		imaris.getApplication().getIApplicationPrx().SetDataSet( dataset.getIDataSetPrx() );

		System.out.println("invalidatePyramid");
		dataset.invalidatePyramid();
		source.getBdvHandle().getViewerPanel().requestRepaint();

		System.out.println("done");
	}
}
