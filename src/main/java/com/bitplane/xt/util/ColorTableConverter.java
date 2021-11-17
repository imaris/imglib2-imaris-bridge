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

import net.imglib2.converter.Converter;
import net.imglib2.display.ColorConverter;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;

public class ColorTableConverter< R extends RealType< ? > > implements ColorConverter, Converter< R, ARGBType >
{
	private float min;
	private float max;
	private float scale;
	private final int[] lut;

	public ColorTableConverter( final double min, final double max, final int[] lut )
	{
		this.min = (float)min;
		this.max = (float)max;
		this.lut = lut;
		updateScale();
	}

	private void updateScale()
	{
		final float invscale = max - min;
		if ( Math.abs( invscale ) < 1E-06 )
			scale = 0;
		else
			scale = ( lut.length - 1 ) / invscale;
	}

	private int lookupClamped( final int i )
	{
		return lut[ Math.max( 0, Math.min( lut.length - 1, i ) ) ];
	}

	@Override
	public void convert( final R input, final ARGBType output )
	{
		final float v = input.getRealFloat();
		final int i = Math.round( ( v - min ) * scale );
		output.set( lookupClamped( i ) );
	}

	@Override
	public ARGBType getColor()
	{
		return new ARGBType( lut[ lut.length - 1 ] );
	}

	@Override
	public void setColor( final ARGBType c )
	{}

	@Override
	public boolean supportsColor()
	{
		return false;
	}

	@Override
	public double getMin()
	{
		return min;
	}

	@Override
	public double getMax()
	{
		return max;
	}

	@Override
	public void setMin( final double min )
	{
		this.min = ( float ) min;
		updateScale();
	}

	@Override
	public void setMax( final double max )
	{
		this.max = ( float ) max;
		updateScale();
	}
}
