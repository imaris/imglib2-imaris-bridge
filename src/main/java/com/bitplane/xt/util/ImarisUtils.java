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
package com.bitplane.xt.util;

import Imaris.Error;
import Imaris.IApplicationPrx;
import Imaris.IDataSetPrx;
import Imaris.IFactoryPrx;
import Imaris.tType;
import com.bitplane.xt.DatasetDimensions;
import net.imglib2.Dimensions;

/**
 * Helper functions for creating DataSets.
 *
 * @author Tobias Pietzsch
 */
public class ImarisUtils
{

	/**
	 * Create an Imaris dataset.
	 */
	public static IDataSetPrx createDataset(
			final IApplicationPrx app,
			final tType type,
			final DatasetDimensions datasetDimensions ) throws Error
	{
		return createDataset( app, type, datasetDimensions.getImarisDimensions() );
	}

	/**
	 * Create an Imaris dataset.
	 *
	 * @param dimensions
	 * 		must be {@code int[5] = {sx, sy, sz, sc, st}} with all elements ≥1.
	 */
	public static IDataSetPrx createDataset(
			final IApplicationPrx app,
			final tType type,
			final int... dimensions ) throws Error
	{
		// Verify that numDimensions == 5, and each dimension >= 1.
		Dimensions.verifyAllPositive( dimensions );
		if ( dimensions.length != 5 )
			throw new IllegalArgumentException( "exactly 5 dimensions expected" );

		// Create Imaris dataset
		final IFactoryPrx factory = app.GetFactory();
		final IDataSetPrx dataset = factory.CreateDataSet();

		dataset.Create( type, dimensions[ 0 ], dimensions[ 1 ], dimensions[ 2 ], dimensions[ 3 ], dimensions[ 4 ] );
		dataset.SetExtendMinX( 0 );
		dataset.SetExtendMaxX( dimensions[ 0 ] );
		dataset.SetExtendMinY( 0 );
		dataset.SetExtendMaxY( dimensions[ 1 ] );
		dataset.SetExtendMinZ( 0 );
		dataset.SetExtendMaxZ( dimensions[ 2 ] );

		return dataset;
	}
}
