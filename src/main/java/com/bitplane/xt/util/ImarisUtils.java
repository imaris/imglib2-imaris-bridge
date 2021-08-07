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
