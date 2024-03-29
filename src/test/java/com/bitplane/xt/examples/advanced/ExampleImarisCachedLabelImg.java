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
import Imaris.tType;
import bdv.util.BdvFunctions;
import com.bitplane.xt.DatasetDimensions;
import com.bitplane.xt.img.ImarisCachedCellImgOptions;
import com.bitplane.xt.img.ImarisCachedLabelImg;
import com.bitplane.xt.img.ImarisCachedLabelImgFactory;
import com.bitplane.xt.ImarisService;
import com.bitplane.xt.util.ImarisUtils;
import net.imglib2.Cursor;
import net.imglib2.type.numeric.integer.ShortType;
import org.scijava.Context;

public class ExampleImarisCachedLabelImg
{
	static final long[] dimensions = { 100, 100, 100, 10 };
	// channels are folded into value (which for 3 channels can be 0, 1, 2, 3)

	static final int numChannels = 3;

	public static void main( String[] args ) throws Error
	{
		Context context = new Context();
		final ImarisService imaris = context.getService( ImarisService.class );

		toImaris( imaris );
		fromImaris( imaris );
	}

	static void fromImaris( final ImarisService imaris ) throws Error
	{
		ImarisCachedLabelImgFactory< ShortType > factory = new ImarisCachedLabelImgFactory<>(
				new ShortType(),
				imaris.getApplication(),
				ImarisCachedCellImgOptions.options()
						.cellDimensions( 64 )
						.numIoThreads( 20 ) );
		final ImarisCachedLabelImg< ShortType, ? > imarisImg = factory.create( imaris.getApplication().getIApplicationPrx().GetDataSet(), dimensions );

		BdvFunctions.show( imarisImg, "labels" );
	}

	static void toImaris( final ImarisService imaris ) throws Error
	{
		ImarisCachedLabelImgFactory< ShortType > factory = new ImarisCachedLabelImgFactory<>(
				new ShortType(),
				imaris.getApplication(),
				ImarisCachedCellImgOptions.options()
						.cellDimensions( 64 )
						.numIoThreads( 20 ) );

		final IDataSetPrx dataset = ImarisUtils.createDataset(
				imaris.getApplication().getIApplicationPrx(),
				tType.eTypeUInt8,
				new DatasetDimensions(
						( int ) dimensions[ 0 ],
						( int ) dimensions[ 1 ],
						( int ) dimensions[ 2 ],
						numChannels,
						( int ) dimensions[ 3 ] ) );

		// TODO
		final ImarisCachedLabelImg< ShortType, ? > imarisImg = factory.create( dataset, dimensions, cell -> {} );
//		final ImarisCachedLabelImg< ShortType, ? > imarisImg = factory.create( dataset, dimensions );

		final Cursor< ShortType > c = imarisImg.localizingCursor();
		while ( c.hasNext() )
		{
			c.fwd();
			c.get().set( ( short ) ( c.getIntPosition( 0 ) % numChannels + 1 ) );
		}

		imarisImg.persist();

		imaris.getApplication().getIApplicationPrx().SetImage( 0, dataset );
	}
}
