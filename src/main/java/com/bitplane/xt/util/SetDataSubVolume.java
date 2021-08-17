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
import Imaris.IDataSetPrx;
import Imaris.tType;

/**
 * Common generic interface for {@link
 * IDataSetPrx#SetDataSubVolumeAs1DArrayBytes}, {@link
 * IDataSetPrx#SetDataSubVolumeAs1DArrayShorts}, {@link
 * IDataSetPrx#SetDataSubVolumeAs1DArrayFloats}. Use {@link #forDataSet} to
 * construct an appropriate implementation for a given {@code IDataSetPrx}.
 *
 * @author Tobias Pietzsch
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
