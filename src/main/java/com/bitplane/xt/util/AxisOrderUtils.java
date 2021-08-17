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
import mpicbg.spim.data.sequence.FinalVoxelDimensions;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imagej.axis.CalibratedAxis;
import net.imagej.axis.LinearAxis;
import net.imagej.space.CalibratedSpace;

/**
 * Get {@code AxisOrder} corresponding to {@code CalibratedSpace}.
 *
 * @author Tobias Pietzsch
 */
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
