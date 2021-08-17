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
import com.bitplane.xt.util.MapDimensions.SelectIntervalDimension;
import java.util.function.IntFunction;
import net.imglib2.img.basictypeaccess.volatiles.array.DirtyVolatileByteArray;
import net.imglib2.img.basictypeaccess.volatiles.array.DirtyVolatileFloatArray;
import net.imglib2.img.basictypeaccess.volatiles.array.DirtyVolatileShortArray;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileByteArray;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileFloatArray;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileShortArray;

import static com.bitplane.xt.util.MapDimensions.selectIntervalDimension;

/**
 * Reading Imaris blocks as primitive arrays
 *
 * @param <A>
 *     TODO
 *     {@code byte[]}
 *     {@code VolatileByteArray}
 *     {@code DirtyVolatileByteArray}
 *     {@code short[]}
 *     {@code VolatileShortArray}
 *     {@code DirtyVolatileShortArray}
 *     {@code float[]}
 *     {@code VolatileFloatArray}
 *     {@code DirtyVolatileFloatArray}
 *
 * @author Tobias Pietzsch
 */
@FunctionalInterface
public interface PixelSource< A >
{
	/**
	 * Get sub-volume as flattened array.
	 *
	 * @param level
	 * 		resolution level (0 is full resolution).
	 * @param min
	 * 		minimum of interval in {@code Img} space.
	 * 		Will be augmented to 5D if necessary.
	 * @param size
	 * 		size of interval in {@code Img} space.
	 * 		Will be augmented to 5D if necessary.
	 *
	 * @return the flattened data array.
	 */
	A get( int level, long[] min, int[] size ) throws Error;


	/**
	 * TODO
	 *
	 *
	 * @param dataset
	 * @param datasetType
	 * @param mapDimensions
	 * 		maps Imaris dimension indices to imglib2 dimension indices.
	 * 		If {@code i} is dimension index from Imaris (0..4 means
	 * 		X,Y,Z,C,T) then {@code mapDimensions[i]} is the corresponding
	 * 		dimension in {@code Img}. For {@code Img} dimensions with size=1
	 * 		are skipped. E.g., for a X,Y,C image {@code mapDimensions =
	 *  	{0,1,-1,2,-1}}.
	 *
	 * @return
	 */
	static PixelSource< ? > primitiveArraySource( final IDataSetPrx dataset, final tType datasetType, final int[] mapDimensions )
	{
		final GetDataSubVolume slice = GetDataSubVolume.forDataSet( dataset, datasetType );

		final IntFunction< Object > creator;
		switch ( datasetType )
		{
		case eTypeUInt8:
			creator = byte[]::new;
			break;
		case eTypeUInt16:
			creator = short[]::new;
			break;
		case eTypeFloat:
			creator = float[]::new;
			break;
		default:
			throw new IllegalArgumentException();
		}

		final SelectIntervalDimension x = selectIntervalDimension( mapDimensions[ 0 ] );
		final SelectIntervalDimension y = selectIntervalDimension( mapDimensions[ 1 ] );
		final SelectIntervalDimension z = selectIntervalDimension( mapDimensions[ 2 ] );
		final SelectIntervalDimension c = selectIntervalDimension( mapDimensions[ 3 ] );
		final SelectIntervalDimension t = selectIntervalDimension( mapDimensions[ 4 ] );

		return ( r, min, size ) -> {
			final int ox = x.min( min );
			final int oy = y.min( min );
			final int oz = z.min( min );
			final int oc = c.min( min );
			final int ot = t.min( min );

			final int sx = x.size( size );
			final int sy = y.size( size );
			final int sz = z.size( size );
			final int sc = c.size( size );
			final int st = t.size( size );

			if ( sc == 1 && st == 1 )
				return slice.get( ox, oy, oz, oc, ot, r, sx, sy, sz );
			else
			{
				final Object data = creator.apply( sx * sy * sz * sc * st );
				final int slicelength = sx * sy * sz;
				for ( int dt = 0; dt < st; ++dt )
				{
					for ( int dc = 0; dc < sc; ++dc )
					{
						final Object slicedata = slice.get( ox, oy, oz, oc + dc, ot + dt, r, sx, sy, sz );
						final int destpos = ( dt * sc + dc ) * slicelength;
						System.arraycopy( slicedata, 0, data, destpos, slicelength );
					}
				}
				return data;
			}
		};
	}

	/**
	 * TODO
	 *
	 *
	 * @param dataset
	 * @param datasetType
	 * @param mapDimensions
	 * 		maps Imaris dimension indices to imglib2 dimension indices.
	 * 		If {@code i} is dimension index from Imaris (0..4 means
	 * 		X,Y,Z,C,T) then {@code mapDimensions[i]} is the corresponding
	 * 		dimension in {@code Img}. For {@code Img} dimensions with size=1
	 * 		are skipped. E.g., for a X,Y,C image {@code mapDimensions =
	 *  	{0,1,-1,2,-1}}.
	 * @param withDirtyFlag
	 * @param <A>
	 * @return
	 */
	static < A > PixelSource< A > volatileArraySource(
			final IDataSetPrx dataset,
			final tType datasetType,
			final int[] mapDimensions,
			final boolean withDirtyFlag )
	{
		final PixelSource< ? > pixels = primitiveArraySource(dataset, datasetType, mapDimensions );
		if ( withDirtyFlag )
		{
			switch ( datasetType )
			{
			case eTypeUInt8:
				return ( r, min, size ) -> ( A ) new DirtyVolatileByteArray( ( byte[] ) ( pixels.get( r, min, size ) ), true );
			case eTypeUInt16:
				return ( r, min, size ) -> ( A ) new DirtyVolatileShortArray( ( short[] ) ( pixels.get( r, min, size ) ), true );
			case eTypeFloat:
				return ( r, min, size ) -> ( A ) new DirtyVolatileFloatArray( ( float[] ) ( pixels.get( r, min, size ) ), true );
			default:
				throw new IllegalArgumentException();
			}
		}
		else
		{
			switch ( datasetType )
			{
			case eTypeUInt8:
				return ( r, min, size ) -> ( A ) new VolatileByteArray( ( byte[] ) ( pixels.get( r, min, size ) ), true );
			case eTypeUInt16:
				return ( r, min, size ) -> ( A ) new VolatileShortArray( ( short[] ) ( pixels.get( r, min, size ) ), true );
			case eTypeFloat:
				return ( r, min, size ) -> ( A ) new VolatileFloatArray( ( float[] ) ( pixels.get( r, min, size ) ), true );
			default:
				throw new IllegalArgumentException();
			}
		}
	}

}
