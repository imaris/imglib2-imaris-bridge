package com.bitplane.xt.util;

import Imaris.Error;
import Imaris.IDataSetPrx;
import Imaris.tType;

/**
 * Common generic interface for {@link
 * IDataSetPrx#SetDataSubVolumeAs1DArrayBytes}, {@link
 * IDataSetPrx#SetDataSubVolumeAs1DArrayShorts}, {@link
 * IDataSetPrx#SetDataSubVolumeAs1DArrayFloats}. Use {@link #forDataSet} to
 * construct an appropriate implementation for a given {@code IDataSetPrx}.
 */
@FunctionalInterface
public interface SetDataSubVolume
{
	/**
	 * Set sub-volume as flattened primitive array.
	 *
	 * @param data {@code byte[]}, {@code short[]}, {@code float[]}, depending on dataset type.
	 * @param ox offset in X
	 * @param oy offset in Y
	 * @param oz offset in Z
	 * @param oc channel index
	 * @param ot timepoint index
	 * @param sx size in X
	 * @param sy size in Y
	 * @param sz size in Z
	 */
	void set( Object data, int ox, int oy, int oz, int oc, int ot, int sx, int sy, int sz ) throws Error;

	/**
	 * Get the appropriate {@code SetDataSubVolume} for the given {@code dataset}.
	 */
	static SetDataSubVolume forDataSet( final IDataSetPrx dataset ) throws Error
	{
		return forDataSet( dataset, dataset.GetType() );
	}

	/**
	 * Get the appropriate {@code GetDataSubVolume} for the given {@code dataset}.
	 */
	static SetDataSubVolume forDataSet( final IDataSetPrx dataset, final tType datasetType )
	{
		switch ( datasetType )
		{
		case eTypeUInt8:
			return ( data, ox, oy, oz, oc, ot, sx, sy, sz ) ->
					dataset.SetDataSubVolumeAs1DArrayBytes( ( byte[] ) data, ox, oy, oz, oc, ot, sx, sy, sz );
		case eTypeUInt16:
			return ( data, ox, oy, oz, oc, ot, sx, sy, sz ) ->
					dataset.SetDataSubVolumeAs1DArrayShorts( ( short[] ) data, ox, oy, oz, oc, ot, sx, sy, sz );
		case eTypeFloat:
			return ( data, ox, oy, oz, oc, ot, sx, sy, sz ) ->
					dataset.SetDataSubVolumeAs1DArrayFloats( ( float[] ) data, ox, oy, oz, oc, ot, sx, sy, sz );
		default:
			throw new IllegalArgumentException();
		}
	}
}
