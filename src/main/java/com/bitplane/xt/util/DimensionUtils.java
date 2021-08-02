package com.bitplane.xt.util;

import Imaris.Error;
import Imaris.IDataSetPrx;
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

		// TODO: javadoc
		public DatasetDimensions( final int sx, final int sy, final int sz, final int sc, final int st )
		{
			if ( sx < 0 || sy < 0 || sz < 0 || sc < 0 || st < 0 )
				throw new IllegalArgumentException( "Dimensions mus be ≥ 0" );

			if ( sx <= 0 || sy <= 0 )
				throw new IllegalArgumentException( "Dataset must have at least dimensions X and Y" );

			imarisDimensions = new int[] { sx, sy, 1, 1, 1 };

			final StringBuffer sbAxisOrder = new StringBuffer( "XY" );
			if ( sz > 0 )
			{
				sbAxisOrder.append( "Z" );
				imarisDimensions[ 2 ] = sz;
			}
			if ( sc > 0 )
			{
				sbAxisOrder.append( "C" );
				imarisDimensions[ 3 ] = sc;
			}
			if ( st > 0 )
			{
				sbAxisOrder.append( "T" );
				imarisDimensions[ 4 ] = st;
			}

			axisOrder = AxisOrder.valueOf( sbAxisOrder.toString() );
			mapDimensions = MapDimensions.fromAxisOrder( axisOrder );
		}

		// TODO: javadoc
		public DatasetDimensions( final IDataSetPrx dataset, AxisOrder axes ) throws Error
		{
			final int sx = dataset.GetSizeX();
			final int sy = dataset.GetSizeY();
			final int sz = dataset.GetSizeZ();
			final int sc = dataset.GetSizeC();
			final int st = dataset.GetSizeT();

			imarisDimensions = new int[] { sx, sy, sz, sc, st };

			if ( axes != null )
				axisOrder = axes;
			else
			{
				final StringBuffer sbAxisOrder = new StringBuffer( "XY" );
				if ( sz > 1 )
					sbAxisOrder.append( "Z" );
				if ( sc > 1 )
					sbAxisOrder.append( "C" );
				if ( st > 1 )
					sbAxisOrder.append( "T" );
				axisOrder = AxisOrder.valueOf( sbAxisOrder.toString() );
			}

			mapDimensions = MapDimensions.fromAxisOrder( axisOrder );
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
