package com.bitplane.xt;

import Imaris.Error;
import Imaris.IApplicationPrx;
import Imaris.IDataSetPrx;
import Imaris.IFactoryPrx;
import Imaris.tType;
import bdv.util.AxisOrder;
import com.bitplane.xt.util.DatasetCalibration;
import mpicbg.spim.data.sequence.FinalVoxelDimensions;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imagej.axis.CalibratedAxis;
import net.imagej.axis.LinearAxis;
import net.imagej.space.CalibratedSpace;
import net.imglib2.Dimensions;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
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
	// TODO: remove?
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
	 * Get the ImgLib2 {@code Type} corresponding to the given Imaris {@code type}
	 */
	public static < T extends NativeType< T > & RealType< T > > T imglibTypeFor( final tType type )
	{
		switch ( type )
		{
		case eTypeUInt8:
			return  ( T ) new UnsignedByteType();
		case eTypeUInt16:
			return ( T ) new UnsignedShortType();
		case eTypeFloat:
			return ( T ) new FloatType();
		default:
			throw new IllegalArgumentException();
		}
	}


	/**
	 * Create an Imaris dataset.
	 */
	public static IDataSetPrx createDataset(
			final IApplicationPrx app,
			final tType type,
			final DatasetDimensions datasetDimensions ) throws Error
	{
		return createDataset( app, type, datasetDimensions.getImarisDimensions() );
	}

	/**
	 * Create an Imaris dataset.
	 */
	// TODO: remove?
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

	// TODO javadoc
	// uses Imaris conventions for min
	public static void setVoxelDimensions(
			final IDataSetPrx dataset,
			final String unit,
			final float extendMinX,
			final float extendMaxX,
			final float extendMinY,
			final float extendMaxY,
			final float extendMinZ,
			final float extendMaxZ ) throws Error
	{
		dataset.SetUnit( unit );
		dataset.SetExtendMinX( extendMinX );
		dataset.SetExtendMinY( extendMinY );
		dataset.SetExtendMinZ( extendMinZ );
		dataset.SetExtendMaxX( extendMaxX );
		dataset.SetExtendMaxY( extendMaxY );
		dataset.SetExtendMaxZ( extendMaxZ );
	}

	/**
	 * Set the extends of {@code dataset} to correspond to the specified {@code voxelDimensions}.
	 */
	// TODO remove?
	// uses ImgLib2 conventions for min
	public static void setVoxelDimensions(
			final IDataSetPrx dataset,
			final VoxelDimensions voxelDimensions ) throws Error
	{
		setVoxelDimensions( dataset, voxelDimensions, 0, 0, 0 );
	}

	/**
	 * Set the extends of {@code dataset} to correspond to the specified {@code voxelDimensions}.
	 */
	// TODO javadoc: uses ImgLib2 conventions for min
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

		final double vsx = voxelDimensions.dimension( 0 );
		final double vsy = voxelDimensions.dimension( 1 );
		final double vsz = voxelDimensions.dimension( 2 );

		final float extendMinX = ( float ) ( minX - vsx / 2 );
		final float extendMaxX = ( float ) ( extendMinX + sx * vsx );
		final float extendMinY = ( float ) ( minY - vsy / 2 );
		final float extendMaxY = ( float ) ( extendMinY + sy * vsy );
		final float extendMinZ = ( float ) ( minZ - vsz / 2 );
		final float extendMaxZ = ( float ) ( extendMinZ + sz * vsz );

		setVoxelDimensions( dataset, voxelDimensions.unit(), extendMinX, extendMaxX, extendMinY, extendMaxY, extendMinZ, extendMaxZ );
	}

	/**
	 * Get the {@code VoxelDimensions} that correspond to the extends of the specified {@code dataset}.
	 */
	public static VoxelDimensions getVoxelDimensions( final IDataSetPrx dataset ) throws Error
	{
		final int sx = dataset.GetSizeX();
		final int sy = dataset.GetSizeY();
		final int sz = dataset.GetSizeZ();

		final double maxX = dataset.GetExtendMaxX();
		final double minX = dataset.GetExtendMinX();
		final double maxY = dataset.GetExtendMaxY();
		final double minY = dataset.GetExtendMinY();
		final double maxZ = dataset.GetExtendMaxZ();
		final double minZ = dataset.GetExtendMinZ();

		final double[] calib = new double[] {
				( maxX - minX ) / sx,
				( maxY - minY ) / sy,
				( maxZ - minZ ) / sz
		};
		final String unit = dataset.GetUnit();
		return new FinalVoxelDimensions( unit, calib );
	}
}
