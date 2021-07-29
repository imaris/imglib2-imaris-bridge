package com.bitplane.xt.util;

import java.util.Arrays;

/**
 * Utilities for mapping between Imaris and ImgLib2 dimension arrays.
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
