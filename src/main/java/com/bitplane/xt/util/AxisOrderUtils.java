package com.bitplane.xt.util;

import Imaris.tType;
import bdv.util.AxisOrder;
import mpicbg.spim.data.sequence.FinalVoxelDimensions;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imagej.axis.CalibratedAxis;
import net.imagej.axis.LinearAxis;
import net.imagej.space.CalibratedSpace;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;

import static Imaris.tType.eTypeFloat;
import static Imaris.tType.eTypeUInt16;
import static Imaris.tType.eTypeUInt8;

public final class AxisOrderUtils
{
	/**
	 * Get the {@link AxisOrder} corresponding to {@code space}.
	 * Axes of the {@code space} must be an ordered subset of XYZCT, containing at least XY.
	 */
	public static AxisOrder getAxisOrder( final CalibratedSpace< ? > space ) throws IllegalArgumentException
	{
		final int n = space.numDimensions();
		final StringBuffer sb = new StringBuffer();
		for ( int d = 0; d < n; ++d )
		{
			final CalibratedAxis axis = space.axis( d );
			final AxisType axisType = axis.type();
			if ( axisType.equals( Axes.X ) )
				sb.append( "X" );
			else if ( axisType.equals( Axes.Y ) )
				sb.append( "Y" );
			else if ( axisType.equals( Axes.Z ) )
				sb.append( "Z" );
			else if ( axisType.equals( Axes.CHANNEL ) )
				sb.append( "C" );
			else if ( axisType.equals( Axes.TIME ) )
				sb.append( "T" );
		}
		try
		{
			return checkAxisOrder( AxisOrder.valueOf( sb.toString() ) );
		}
		catch ( IllegalArgumentException e )
		{
			throw new IllegalArgumentException( "Specified space cannot be matched to AxisOrder enum."
					+ "Axes should be an ordered subset of XYZCT, containing at least XY.", e );
		}
	}

	/**
	 * Verify that {@code axisOrder} is subset of XYZCT in this order.
	 * @return {@code axisOrder}
	 */
	private static AxisOrder checkAxisOrder( AxisOrder axisOrder ) throws IllegalArgumentException
	{
		if ( axisOrder.hasChannels() && axisOrder.channelDimension() < axisOrder.zDimension() )
			throw new IllegalArgumentException( "C dimension cannot occur before Z");

		if ( axisOrder.hasTimepoints() && axisOrder.timeDimension() < axisOrder.zDimension() )
			throw new IllegalArgumentException( "T dimension cannot occur before Z");

		if ( axisOrder.hasTimepoints() && axisOrder.timeDimension() < axisOrder.channelDimension() )
			throw new IllegalArgumentException( "T dimension cannot occur before C");

		return axisOrder;
	}

	/**
	 * Get XYZ calibration of {@code space} as {@code VoxelDimensions}.
	 */
	public static VoxelDimensions getVoxelDimensions( final CalibratedSpace< ? > space ) throws IllegalArgumentException
	{
		String unit = null;
		final double[] dimensions = new double[] { 1, 1, 1 };
		final AxisType[] spatialAxes = { Axes.X, Axes.Y, Axes.Z };

		final int n = space.numDimensions();
		for ( int d = 0; d < n; ++d )
		{
			final CalibratedAxis axis = space.axis( d );
			final AxisType axisType = axis.type();
			for ( int a = 0; a < spatialAxes.length; ++a )
			{
				if ( axisType.equals( spatialAxes[ a ] ) )
				{
					if ( unit == null )
						unit = axis.unit();
					if ( axis instanceof LinearAxis )
						dimensions[ a ] = ( ( LinearAxis ) axis ).scale();
				}
			}
		}
		if ( unit == null )
			unit = "px";
		return new FinalVoxelDimensions( unit, dimensions );
	}

	private AxisOrderUtils() {}
}
