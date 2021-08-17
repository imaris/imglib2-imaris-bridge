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

import bdv.util.AxisOrder;
import java.util.Arrays;

/**
 * Utilities for mapping between Imaris and ImgLib2 dimension arrays.
 *
 * @author Tobias Pietzsch
 */
public final class MapDimensions
{
	/**
	 * Tries to derive a {@code mapDimensions} array matching the specified
	 * Imaris and imglib2 dimension arrays.
	 * <p>
	 * {@code mapDimensions} maps Imaris dimension indices to imglib2 dimension
	 * indices. If {@code i} is dimension index from Imaris (0..4 means
	 * X,Y,Z,C,T) then {@code mapDimensions[i]} is the corresponding dimension
	 * in {@code img}. For {@code img}, dimensions with size=1 may be skipped.
	 * <p>
	 * For example, assume we have a XYC image, that is, a multi-channel 2D
	 * image, with {@code imarisDims={800,600,1,3,1}}. Let {@code
	 * imgDims={800,600,3}}, indicating that the imglib2 {@code img} should not
	 * have Z and T dimensions (instead of dimensions having size 1). Then the
	 * result is {@code mapDimensions={0,1,-1,2,-1}}.
	 *
	 * @param imarisDims
	 * 		dimensions of the Imaris dataset ({@code int[5]}, with X,Y,Z,C,T)
	 * @param imgDims
	 * 		dimensions of the imglib2 image
	 *
	 * @return a {@code int[5]} array {@code mapDimensions}, that maps Imaris
	 *		dimension indices to imglib2 dimension indices. If {@code i} is
	 *		dimension index from Imaris (0..4 means X,Y,Z,C,T) then {@code
	 *		mapDimensions[i]} is the corresponding dimension in the imglib2
	 *		{@code Img}. E.g., for a XYC image {@code mapDimensions={0,1,-1,2,-1}}.
	 */
	public static int[] createMapDimensions( final int[] imarisDims, final long[] imgDims )
	{
		assert imarisDims.length == 5;

		final int[] mapDimension = new int[ 5 ];
		int j = 0;
		for ( int i = 0; i < imarisDims.length; ++i )
		{
			final int si = imarisDims[ i ];
			final long sj = j < imgDims.length ? imgDims[ j ] : -1;

			if ( si == sj )
			{
				mapDimension[ i ] = j;
				++j;
			}
			else if ( si == 1 ) // (and sj != 1)
				mapDimension[ i ] = -1;
			else
				throw new IllegalArgumentException( "image dimensions do not match dataset dimensions" );
		}
		return mapDimension;
	}

	public static int[] fromAxisOrder( final AxisOrder axisOrder )
	{
		return new int[] { 0, 1, axisOrder.zDimension(), axisOrder.channelDimension(), axisOrder.timeDimension() };
	}

	/**
	 * Return the reverse mapping for the given {@code mapDimensions}, such that
	 * {@code mapDimensions[inv[i]] == i} for all {@code i}.
	 * <p>
	 * E.g., for {@code mapDimensions={0,1,-1,2,-1}} returns
	 * {@code invMapDimensions={0,1,3}}.
	 * <p>
	 * The length of the returned array is determined by the maximum
	 * dimension index  occurring in {@code mapDimensions}.
	 */
	public static int[] invertMapDimensions( final int[] mapDimensions )
	{
		int n = 0;
		for ( int i = 0; i < mapDimensions.length; i++ )
			n = Math.max( mapDimensions[ i ] + 1, n );
		final int[] invMapDimensions = new int[ n ];
		Arrays.fill( invMapDimensions, -1 );
		for ( int i = 0; i < mapDimensions.length; i++ )
		{
			final int si = mapDimensions[ i ];
			if ( si >= 0 )
				invMapDimensions[ si ] = i;
		}
		return invMapDimensions;
	}

	/**
	 * Create a {@code MapIntervalDimension} that selects the {@code d}-th
	 * element of any {@code min} or {@code size} vectors. If {@code d < 0},
	 * then instead of selecting an element, {@code min} is always 0, and {@code
	 * size} is always 1.
	 */
	public static SelectIntervalDimension selectIntervalDimension( final int d )
	{
		if ( d < 0 )
			return CONSTANT_SELECT_INTERVAL_DIMENSION;

		return new SelectIntervalDimension()
		{
			@Override
			public int min( final long[] min )
			{
				return ( int ) min[ d ];
			}

			@Override
			public int size( final int[] size )
			{
				return size[ d ];
			}
		};
	}

	/**
	 * Selects a particular element of the {@code min} and {@code size} vectors of
	 * an interval. Use {@link #selectIntervalDimension(int)} to create.
	 */
	public interface SelectIntervalDimension
	{
		int min( final long[] min );

		int size( final int[] size );
	}

	private static final SelectIntervalDimension CONSTANT_SELECT_INTERVAL_DIMENSION = new SelectIntervalDimension()
	{
		@Override
		public int min( final long[] min )
		{
			return 0;
		}

		@Override
		public int size( final int[] size )
		{
			return 1;
		}
	};

	private MapDimensions() {}
}
