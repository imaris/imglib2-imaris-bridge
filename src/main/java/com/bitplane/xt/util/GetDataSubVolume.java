package com.bitplane.xt.util;

import Imaris.Error;
import Imaris.IDataSetPrx;
import Imaris.tType;

/**
 * Common generic interface for {@link IDataSetPrx#GetPyramidDataBytes}, {@link
 * IDataSetPrx#GetPyramidDataShorts}, {@link IDataSetPrx#GetPyramidDataFloats}.
 * Use {@link #forDataSet} to construct an appropriate implementation for a
 * given {@code IDataSetPrx}.
 */
@FunctionalInterface
public interface GetDataSubVolume
{
	/**
	 * Get sub-volume as flattened primitive array.
	 *
	 * @param ox offset in X
	 * @param oy offset in Y
	 * @param oz offset in Z
	 * @param oc channel index
	 * @param ot timepoint index
	 * @param r resolution level (0 is full resolution)
	 * @param sx size in X
	 * @param sy size in Y
	 * @param sz size in Z
	 * @return {@code byte[]}, {@code short[]}, {@code float[]}, depending on dataset type.
	 */
	Object get( final int ox, final int oy, final int oz, final int oc, final int ot, final int r, final int sx, final int sy, final int sz ) throws Error;

	/**
	 * Get the appropriate {@code GetDataSubVolume} for the given {@code dataset}.
	 */
	static GetDataSubVolume forDataSet( final IDataSetPrx dataset ) throws Error
	{
		return forDataSet( dataset, dataset.GetType() );
	}

	/**
	 * Get the appropriate {@code GetDataSubVolume} for the given {@code dataset}.
	 */
	static GetDataSubVolume forDataSet( final IDataSetPrx dataset, final tType datasetType )
	{
		switch ( datasetType )
		{
		case eTypeUInt8:
			return dataset::GetPyramidDataBytes;
		case eTypeUInt16:
			return dataset::GetPyramidDataShorts;
		case eTypeFloat:
			return dataset::GetPyramidDataFloats;
		default:
			throw new IllegalArgumentException();
		}
	}
}
