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
package com.bitplane.xt.examples;

import com.bitplane.xt.ImarisApplication;
import com.bitplane.xt.ImarisDataset;
import com.bitplane.xt.ImarisService;
import net.imglib2.Cursor;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import org.scijava.Context;

public class ExampleCreateDataset
{
	public static void main( String[] args )
	{
		/*
		 * Create a SciJava context, obtain the ImarisService instance, and get
		 * the first (typically only) Imaris application.
		 */
		final Context context = new Context();
		final ImarisService imaris = context.getService( ImarisService.class );
		final ImarisApplication app = imaris.getApplication();

		/*
		 * Create a new Imaris dataset with pixel type UnsignedByteType, The
		 * ImgLib2 view of the dataset is 3D (XYZ) with size 128x128x128. (On
		 * the Imaris side, its 128x128x128x1x1).
		 */
		final ImarisDataset< UnsignedByteType > dataset = app.createDataset(
				new UnsignedByteType(),
				128, 128, 128, 0, 0 );

		/*
		 * Use a ImgLib2 Cursor to fill the dataset with some values.
		 */
		final Cursor< UnsignedByteType > c = dataset.asImg().localizingCursor();
		final int[] pos = new int[ 3 ];
		while ( c.hasNext() )
		{
			c.fwd();
			c.localize( pos );
			final int value = pos[ 0 ] ^ pos[ 1 ] ^ pos[ 2 ];
			c.get().set( value );
		}

		/*
		 * Make sure that all changes are persisted to Imaris.
		 */
		dataset.persist();

		/*
		 * Show the dataset in Imaris.
		 */
		app.setDataset( dataset );
	}
}
