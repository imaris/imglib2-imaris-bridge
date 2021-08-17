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
import Imaris.cColorTable;
import net.imglib2.converter.Converter;
import net.imglib2.display.ColorTable8;
import net.imglib2.display.RealARGBColorConverter;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;

/**
 * Convert Imaris color tables to imglib2.
 *
 * @author Tobias Pietzsch
 */
public final class ColorTableUtils
{
	/**
	 * Create an imglib2 {@code ColorTable8} for the given {@code channel} of
	 * {@code dataset}. If the channel has a colortable, translate it.
	 * Otherwise, construct a ramp colortable from the base color of the channel.
	 */
	public static ColorTable8 createChannelColorTable( final IDataSetPrx dataset, final int channel ) throws Error
	{
		final cColorTable vColorTable = dataset.GetChannelColorTable( channel );
		if ( vColorTable != null && vColorTable.mColorRGB.length > 0 )
			return createColorTableFrom( vColorTable );
		else
		{
			final int vColor = dataset.GetChannelColorRGBA( channel );
			return createColorTableFrom( vColor );
		}
	}

	/**
	 * Get the base color of the given {@code channel} of {@code dataset} as an
	 * imglib2 {@code ARGBType}.
	 */
	public static ARGBType getChannelColor( final IDataSetPrx dataset, final int channel ) throws Error
	{
		final int rgba = dataset.GetChannelColorRGBA( channel );
		final int r = rgba & 0xff;
		final int g = ( rgba >> 8 ) & 0xff;
		final int b = ( rgba >> 16 ) & 0xff;
		final int a = ( rgba >> 24 ) & 0xff;
		return new ARGBType( ARGBType.rgba( r, g, b, 255 - a ) );
	}

	/**
	 * Construct a converters for the specified {@code channel} with display
	 * range and color set up according to Imaris.
	 */
	public static < T extends NumericType< T > & RealType< T > > Converter< T, ARGBType > createChannelConverterToARGB( final T type, final IDataSetPrx dataset, final int channel ) throws Error
	{
		final double typeMin = dataset.GetChannelRangeMin( channel );
		final double typeMax = dataset.GetChannelRangeMax( channel );
		final RealARGBColorConverter< T > converter = RealARGBColorConverter.create( type, typeMin, typeMax );
		converter.setColor( getChannelColor( dataset, channel ) );
		return converter;
	}


	/**
	 * Construct a ramp colortable from the given base color.
	 *
	 * @param aRGBA the RGBA color as encoded by Imaris
	 */
	private static ColorTable8 createColorTableFrom( final int aRGBA )
	{
		final int vSize = 256;

		final byte[] rLut = new byte[ vSize ];
		final byte[] gLut = new byte[ vSize ];
		final byte[] bLut = new byte[ vSize ];
		final byte[] aLut = new byte[ vSize ];

		final int[] vRGBA = new int[ 4 ];
		components( aRGBA, vRGBA );
		final byte alpha = UnsignedByteType.getCodedSignedByte( 255 - vRGBA[ 3 ] );
		for ( int i = 0; i < vSize; ++i )
		{
			rLut[ i ] = ( byte ) ( i * vRGBA[ 0 ] / 255 );
			gLut[ i ] = ( byte ) ( i * vRGBA[ 1 ] / 255 );
			bLut[ i ] = ( byte ) ( i * vRGBA[ 2 ] / 255 );
			aLut[ i ] = alpha;
		}
		return new ColorTable8( rLut, gLut, bLut, aLut );
	}

	/**
	 * Convert the given Imaris {@code cColorTable} to a imglib2 {@code ColorTable8}.
	 */
	private static ColorTable8 createColorTableFrom( final cColorTable aColor )
	{
		final int[] vRGB = aColor.mColorRGB;
		final byte alpha = UnsignedByteType.getCodedSignedByte(
				255 - UnsignedByteType.getUnsignedByte(aColor.mAlpha));
		final int vSize = 256;
		final int vSourceSize = vRGB.length;

		final byte[] rLut = new byte[ vSize ];
		final byte[] gLut = new byte[ vSize ];
		final byte[] bLut = new byte[ vSize ];
		final byte[] aLut = new byte[ vSize ];

		final int[] vRGBA = new int[ 4 ];
		for ( int i = 0; i < vSize; ++i )
		{
			final int vIndex = ( i * vSourceSize ) / vSize;
			components( vRGB[ vIndex ], vRGBA );
			rLut[ i ] = ( byte ) ( vRGBA[ 0 ] );
			gLut[ i ] = ( byte ) ( vRGBA[ 1 ] );
			bLut[ i ] = ( byte ) ( vRGBA[ 2 ] );
			aLut[ i ] = alpha;
		}
		return new ColorTable8( rLut, gLut, bLut, aLut );
	}

	/**
	 * Split 8-bit rgba packed into an {@code int} value into components R, G, B, A.
	 * Store in the provided {@code components} array.
	 */
	private static void components( final int rgba, final int[] components )
	{
		components[ 0 ] = rgba & 0xff;
		components[ 1 ] = ( rgba >> 8 ) & 0xff;
		components[ 2 ] = ( rgba >> 16 ) & 0xff;
		components[ 3 ] = ( rgba >> 24 ) & 0xff;
	}

	private ColorTableUtils() {}
}
