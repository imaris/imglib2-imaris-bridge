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
import Imaris.IApplicationPrx;
import Imaris.IDataSetPrx;
import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import bdv.util.BdvStackSource;
import com.bitplane.xt.img.ImarisCachedCellImg;
import com.bitplane.xt.img.ImarisCachedCellImgFactory;
import com.bitplane.xt.img.ImarisCachedCellImgOptions;
import com.bitplane.xt.ImarisDataset;
import com.bitplane.xt.ImarisService;
import ij.IJ;
import ij.ImagePlus;
import java.util.Arrays;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.view.Views;
import org.scijava.Context;

public class ExampleImgFactory
{
	public static void main( String[] args ) throws Error, InterruptedException
	{
		Context context = new Context();
		final ImarisService imaris = context.getService( ImarisService.class );

		final String path = "/Users/pietzsch/workspace/data/e002_stack_fused-8bit.tif";

		final ImagePlus imp = IJ.openImage( path );
		final long[] min = new long[ 3 ];
		final long[] max = new long[] { imp.getWidth() - 1, imp.getHeight() - 1, imp.getNSlices() - 1 };
		final RandomAccessibleInterval< UnsignedByteType > rai = Views.interval( ImageJFunctions.wrapByte( imp ), min, max );
		System.out.println( "imp = " + imp );
		System.out.println( "imp.getWidth(), imp.getHeight(), imp.getNSlices() = " + imp.getWidth() + ", " + imp.getHeight() + ", " + imp.getNSlices() );
		System.out.println( "ImageJFunctions.wrapReal( imp ) = " + ImageJFunctions.wrapReal( imp ) );
		final int sx = ( int ) rai.dimension( 0 );
		final int sy = ( int ) rai.dimension( 1 );
		final int sz = ( int ) rai.dimension( 2 );
		System.out.println( "sx, sy, sz = " + sx + ", " + sy + ", " + sz );



		final IApplicationPrx app = imaris.getApplication().getIApplicationPrx();
		System.out.println( "app = " + app );

		ImarisCachedCellImgFactory< UnsignedByteType > factory = new ImarisCachedCellImgFactory<>(
				new UnsignedByteType(),
				imaris.getApplication(),
				ImarisCachedCellImgOptions.options().cellDimensions( 100, 100, 100 ) );
		final ImarisCachedCellImg< UnsignedByteType, ? > img = factory.create( sx, sy, sz );
		LoopBuilder.setImages( rai, img ).forEachPixel( ( i, o ) -> o.set( i ) );

		System.out.println( "persisting..." );
		img.persist();
		System.out.println( "done..." );

		final IDataSetPrx dataset = img.getIDataSetPrx();

		final int[][] pyramidSizes = dataset.GetPyramidSizes();
		final int[][] pyramidBlockSizes = dataset.GetPyramidBlockSizes();
		final int numResolutions = pyramidSizes.length;
		System.out.println( "before SetImage" );
		System.out.println( "numResolutions = " + numResolutions );
		System.out.println( "pyramidSizes = " + Arrays.deepToString( pyramidSizes ) );
		System.out.println( "pyramidBlockSizes = " + Arrays.deepToString( pyramidBlockSizes ) );
		System.out.println();

//		app.SetImage( 0, dataset );
//		System.out.println( "after SetImage" );
//		System.out.println( "numResolutions = " + numResolutions );
//		System.out.println( "pyramidSizes = " + Arrays.deepToString( pyramidSizes ) );
//		System.out.println( "pyramidBlockSizes = " + Arrays.deepToString( pyramidBlockSizes ) );
//

		app.SetImage( 0, dataset );
		final ImarisDataset< ? > imarisDataset = new ImarisDataset<>( null, dataset );
		final BdvStackSource< ? > source = BdvFunctions.show( imarisDataset );

//		final IFactoryPrx factory = app.GetFactory();
//		final IDataSetPrx dataset = factory.CreateDataSet();
//
//		dataset.Create( eTypeUInt8, sx, sy, sz, 1, 1 );
//
//		dataset.SetExtendMinX( 0 );
//		dataset.SetExtendMaxX( sx );
//		dataset.SetExtendMinY( 0 );
//		dataset.SetExtendMaxY( sy );
//		dataset.SetExtendMinZ( 0 );
//		dataset.SetExtendMaxZ( sz );
//
//		final byte[] data = new byte[ sx * sy * sz ];
//		LoopBuilder.setImages( rai, ArrayImgs.unsignedBytes( data, sx, sy, sz ) ).forEachPixel( ( i, o ) -> o.set( i ) );
//		dataset.SetDataVolumeAs1DArrayBytes( data, 0, 0 );
//
//		app.SetImage( 0, dataset );
	}
}
