package com.bitplane.xt;

import Imaris.Error;
import Imaris.IApplicationPrx;
import Imaris.IDataSetPrx;
import Imaris.IFactoryPrx;
import Imaris.tType;
import bdv.util.AxisOrder;
import java.awt.geom.Dimension2D;
import mpicbg.spim.data.sequence.FinalVoxelDimensions;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imagej.axis.CalibratedAxis;
import net.imagej.axis.LinearAxis;
import net.imagej.space.CalibratedSpace;
import net.imglib2.Dimensions;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;

import static Imaris.tType.eTypeFloat;
import static Imaris.tType.eTypeUInt16;
import static Imaris.tType.eTypeUInt8;

/**
 * Helper functions mostly related to translating axes metadata.
 */
public class ImarisUtils
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

	/**
	 * Get the Imaris {@code tType} corresponding to the given imglib2 {@code type}
	 */
	public static tType imarisTypeFor( final Object type )
	{
		if ( type instanceof UnsignedByteType )
			return eTypeUInt8;
		else if ( type instanceof UnsignedShortType )
			return eTypeUInt16;
		else if ( type instanceof FloatType )
			return eTypeFloat;
		else
			throw new IllegalArgumentException( "Only UnsignedByteType, UnsignedShortType, FloatType are supported (not " + type.getClass().getSimpleName() + ")" );
	}

	/**
	 * Create an Imaris dataset.
	 * <p>
	 * {@code axisOrder} indicates the meaning of {@code dimensions},
	 * and must match the number of {@code dimensions} arguments.
	 * {@code axisOrder} must be a subset of XYZCT (in this order).
	 */
	public static IDataSetPrx createDataset(
			final IApplicationPrx app,
			final tType type,
			final AxisOrder axisOrder,
			final long... dimensions ) throws Error
	{
		// Verify that numDimensions < 5.
		// Verify that each for each dimension 0 < dim < Integer.MAX_VALUE.
		Dimensions.verifyAllPositive( dimensions );
		final int n = dimensions.length;
		if ( n > 5 )
			throw new IllegalArgumentException( "image must not have more than 5 dimensions" );
		for ( int i = 0; i < dimensions.length; i++ )
			if ( dimensions[ i ] > Integer.MAX_VALUE )
				throw new IllegalArgumentException( "each individual image dimension must be < 2^31" );

		checkAxisOrder( axisOrder );
		if ( axisOrder.numDimensions() != n )
			throw new IllegalArgumentException( "AxisOrder must match number of dimensions" );

		final int sx = ( int ) dimensions[ 0 ];
		final int sy = ( int ) dimensions[ 1 ];
		final int sz = axisOrder.hasZ() ? ( int ) dimensions[ axisOrder.zDimension() ] : 1;
		final int sc = axisOrder.hasChannels() ? ( int ) dimensions[ axisOrder.channelDimension() ] : 1;
		final int st = axisOrder.hasTimepoints() ? ( int ) dimensions[ axisOrder.timeDimension() ] : 1;

		return createDataset( app, type, sx, sy, sz, sc, st );
	}

	/**
	 * Create an Imaris dataset.
	 */
	public static IDataSetPrx createDataset(
			final IApplicationPrx app,
			final tType type,
			final int... dimensions ) throws Error
	{
		// Verify that numDimensions == 5, and each dimension >= 1.
		Dimensions.verifyAllPositive( dimensions );
		if ( dimensions.length != 5 )
			throw new IllegalArgumentException( "exactly 5 dimensions expected" );
		Dimensions.verifyAllPositive( dimensions );

		// Create Imaris dataset
		final IFactoryPrx factory = app.GetFactory();
		final IDataSetPrx dataset = factory.CreateDataSet();

		dataset.Create( type, dimensions[ 0 ], dimensions[ 1 ], dimensions[ 2 ], dimensions[ 3 ], dimensions[ 4 ] );
		dataset.SetExtendMinX( 0 );
		dataset.SetExtendMaxX( dimensions[ 0 ] );
		dataset.SetExtendMinY( 0 );
		dataset.SetExtendMaxY( dimensions[ 1 ] );
		dataset.SetExtendMinZ( 0 );
		dataset.SetExtendMaxZ( dimensions[ 2 ] );

		return dataset;
	}

	/**
	 * Set the extends of {@code dataset} to correspond to the specified {@code voxelDimensions}.
	 */
	public static void setVoxelDimensions(
			final IDataSetPrx dataset,
			final VoxelDimensions voxelDimensions ) throws Error
	{
		setVoxelDimensions( dataset, voxelDimensions, 0, 0, 0 );
	}

	/**
	 * Set the extends of {@code dataset} to correspond to the specified {@code voxelDimensions}.
	 */
	public static void setVoxelDimensions(
			final IDataSetPrx dataset,
			final VoxelDimensions voxelDimensions,
			final double minX,
			final double minY,
			final double minZ ) throws Error
	{
		final int sx = dataset.GetSizeX();
		final int sy = dataset.GetSizeY();
		final int sz = dataset.GetSizeZ();

		dataset.SetUnit( voxelDimensions.unit() );

		dataset.SetExtendMinX( ( float ) minX );
		dataset.SetExtendMinY( ( float ) minY );
		dataset.SetExtendMinZ( ( float ) minZ );
		dataset.SetExtendMaxX( ( float ) ( minX + sx * voxelDimensions.dimension( 0 ) ) );
		dataset.SetExtendMaxY( ( float ) ( minY + sy * voxelDimensions.dimension( 1 ) ) );
		dataset.SetExtendMaxZ( ( float ) ( minZ + sz * voxelDimensions.dimension( 2 ) ) );
	}
}
