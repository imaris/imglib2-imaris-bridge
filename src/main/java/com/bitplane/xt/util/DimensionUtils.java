package com.bitplane.xt.util;

import bdv.util.AxisOrder;

public final class DimensionUtils
{
	// TODO: move to separate file?
	// TODO: javadoc
	// TODO: are all fields needed?
	public static final class DatasetDimensions
	{
		/**
		 * Maps Imaris dimension indices to imglib2 dimension indices.
		 * If i is dimension index from Imaris (0..4 means X,Y,Z,C,T)
		 * then mapDimensions[i] is the corresponding imglib2 dimension, e.g., in imagePyramid.
		 * <p>
		 * For imglib2 dimensions, Imaris dimensions with size=1 maybe skipped.
		 * E.g., for a XYC image {@code mapDimensions = {0,1,-1,2,-1}}.
		 */
		private final int[] mapDimensions;

		/**
		 * dimensions with which to create the dataset on the imaris side
		 */
		private final int[] imarisDimensions;

		private final AxisOrder axisOrder;

		public DatasetDimensions( final int sx, final int sy, final int sz, final int sc, final int st )
		{
			if ( sx < 0 || sy < 0 || sz < 0 || sc < 0 || st < 0 )
				throw new IllegalArgumentException( "Dimensions mus be ≥ 0" );

			if ( sx <= 0 || sy <= 0 )
				throw new IllegalArgumentException( "Dataset must have at least dimensions X and Y" );


			mapDimensions = new int[] { 0, 1, -1, -1, -1 };

			imarisDimensions = new int[] { sx, sy, 1, 1, 1 };

			final StringBuffer sbAxisOrder = new StringBuffer( "XY" );
			int d = 2;
			if ( sz > 0 )
			{
				sbAxisOrder.append( "Z" );
				mapDimensions[ 2 ] = d++;
				imarisDimensions[ 2 ] = sz;
			}
			if ( sc > 0 )
			{
				sbAxisOrder.append( "C" );
				mapDimensions[ 3 ] = d++;
				imarisDimensions[ 3 ] = sc;
			}
			if ( st > 0 )
			{
				sbAxisOrder.append( "T" );
				mapDimensions[ 4 ] = d++;
				imarisDimensions[ 4 ] = st;
			}
			final int numDimensions = d;

			axisOrder = AxisOrder.valueOf( sbAxisOrder.toString() );
		}

		public int[] getMapDimensions()
		{
			return mapDimensions;
		}

		public int[] getImarisDimensions()
		{
			return imarisDimensions;
		}

		public AxisOrder getAxisOrder()
		{
			return axisOrder;
		}
	}

	private DimensionUtils() {}
}
