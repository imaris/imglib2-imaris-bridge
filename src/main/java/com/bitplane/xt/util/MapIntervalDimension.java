package com.bitplane.xt.util;

import com.bitplane.xt.ImarisCellCache;

/**
 * Selects a particular element of the {@code min} and {@code size} vectors
 * of an interval. Use {@link #mapIntervalDimension(int)} to create.
 */
public interface MapIntervalDimension
{
	int min( final long[] min );

	int size( final int[] size );

	MapIntervalDimension constantMapIntervalDimension = new MapIntervalDimension()
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

	/**
	 * Create a {@code MapIntervalDimension} that selects the {2code d}-th
	 * element of any {@code min} or {@code size} vectors. If {@code d < 0},
	 * then instead of selecting an element, {@code min} is always 0, and
	 * {@code size} is always 1.
	 */
	static MapIntervalDimension mapIntervalDimension( final int d )
	{
		if ( d < 0 )
			return constantMapIntervalDimension;

		return new MapIntervalDimension()
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
}
