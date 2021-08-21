package com.bitplane.xt;

import Imaris.Error;
import Imaris.IDataSetPrx;
import java.util.Arrays;
import mpicbg.spim.data.sequence.VoxelDimensions;

/**
 * Calibration (voxel size and unit) and min coordinate for an {@code
 * ImarisDataset}.
 * <p>
 * Note, that the min coordinate is in ImgLib2 convention: It refers to the
 * voxel center. This is in contrast to Imaris conventions, where {@code
 * ExtendMinX, ExtendMinY, ExtendMinZ} indicate the min corner of the min voxel.
 * <p>
 * When reading/writing extents from/to a {@code IDataSetPrx}, the min is
 * translated appropriately from/to Imaris conventions.
 *
 * @author Tobias Pietzsch
 */
public class DatasetCalibration
{
	private String unit;
	private final double[] voxelSize;
	private final double[] min;

	public DatasetCalibration( final String unit,
			final double voxelSizeX, final double voxelSizeY, final double voxelSizeZ,
			final double minX, final double minY, final double minZ )
	{
		this.unit = unit;
		this.voxelSize = new double[] {voxelSizeX, voxelSizeY, voxelSizeZ };
		this.min = new double[] { minX, minY, minZ };
	}

	/**
	 * Get units for {@link #voxelSize(int)}
	 */
	public String unit()
	{
		return unit;
	}

	/**
	 * Get size of a voxel in dimension {@code d}, where {@code 0 ≤ d < 3}.
	 */
	public double voxelSize( final int d )
	{
		return this.voxelSize[ d ];
	}

	/**
	 * Get the coordinate (in dimension {@code d}) of the min corner of the
	 * dataset, where {@code 0 ≤ d < 3}.
	 */
	public double min( final int d )
	{
		return min[ d ];
	}

	@Override
	public String toString()
	{
		final StringBuffer sb = new StringBuffer( "DatasetCalibration{" );
		sb.append( "unit='" ).append( unit ).append( '\'' );
		sb.append( ", voxelSize=[" );
		for ( int i = 0; i < voxelSize.length; ++i )
			sb.append( i == 0 ? "" : ", " ).append( voxelSize[ i ] );
		sb.append( "], min=[" );
		for ( int i = 0; i < min.length; ++i )
			sb.append( i == 0 ? "" : ", " ).append( min[ i ] );
		sb.append( "]}" );
		return sb.toString();
	}

	// -- internal --

	DatasetCalibration()
	{
		this.voxelSize = new double[ 3 ];
		this.min = new double[ 3 ];
	}

	/**
	 * Construct from the size and extents of {@code dataset}.
	 */
	DatasetCalibration( final IDataSetPrx dataset ) throws Error
	{
		this();
		setFromDataset( dataset );
	}

	void setFromDataset( final IDataSetPrx dataset ) throws Error
	{
		setExtends( dataset.GetUnit(),
				dataset.GetExtendMinX(), dataset.GetExtendMaxX(),
				dataset.GetExtendMinY(), dataset.GetExtendMaxY(),
				dataset.GetExtendMinZ(), dataset.GetExtendMaxZ(),
				new int[] { dataset.GetSizeX(), dataset.GetSizeY(), dataset.GetSizeZ() } );
	}

	/**
	 * Sets unit, voxel size, and min coordinate from Imaris extents.
	 * <p>
	 * Note, that the given min/max extents are in Imaris conventions:
	 * {@code extendMinX} refers to the min corner of the min voxel of the dataset,
	 * {@code extendMaxX} refers to the max corner of the max voxel of the dataset.
	 * <p>
	 * This is in contrast to the ImgLib2 convention, where coordinates always refer to the
	 * voxel center. This method translates the {@code extendMin/Max} arguments to ImgLib2 conventions.
	 */
	void setExtends( final String unit,
			final double extendMinX, final double extendMaxX,
			final double extendMinY, final double extendMaxY,
			final double extendMinZ, final double extendMaxZ,
			final int[] size )
	{
		this.unit = unit;
		voxelSize[ 0 ] = ( extendMaxX - extendMinX ) / size[ 0 ];
		voxelSize[ 1 ] = ( extendMaxY - extendMinY ) / size[ 1 ];
		voxelSize[ 2 ] = ( extendMaxZ - extendMinZ ) / size[ 2 ];
		min[ 0 ] = extendMinX + voxelSize[ 0 ] / 2;
		min[ 1 ] = extendMinY + voxelSize[ 1 ] / 2;
		min[ 2 ] = extendMinZ + voxelSize[ 2 ] / 2;
	}

	void applyToDataset( final IDataSetPrx dataset, final int[] size ) throws Error
	{
		final float extendMinX = ( float ) ( min[ 0 ] - voxelSize[ 0 ] / 2 );
		final float extendMaxX = ( float ) ( extendMinX + size[ 0 ] * voxelSize[ 0 ] );
		final float extendMinY = ( float ) ( min[ 1 ] - voxelSize[ 1 ] / 2 );
		final float extendMaxY = ( float ) ( extendMinY + size[ 1 ] * voxelSize[ 1 ] );
		final float extendMinZ = ( float ) ( min[ 2 ] - voxelSize[ 2 ] / 2 );
		final float extendMaxZ = ( float ) ( extendMinZ + size[ 2 ] * voxelSize[ 2 ] );

		dataset.SetUnit( unit );
		dataset.SetExtendMinX( extendMinX );
		dataset.SetExtendMinY( extendMinY );
		dataset.SetExtendMinZ( extendMinZ );
		dataset.SetExtendMaxX( extendMaxX );
		dataset.SetExtendMaxY( extendMaxY );
		dataset.SetExtendMaxZ( extendMaxZ );
	}

	void set( final DatasetCalibration calib )
	{
		this.unit = calib.unit;
		for ( int d = 0; d < 3; d++ )
		{
			this.voxelSize[ d ] = calib.voxelSize[ d ];
			this.min[ d ] = calib.min[ d ];
		}
	}

	/**
	 * Set the voxel size and unit.
	 * (The min coordinate is not modified).
	 */
	void setVoxelDimensions( final VoxelDimensions voxelDimensions )
	{
		if ( voxelDimensions.numDimensions() != 3 )
			throw new IllegalArgumentException();

		unit = voxelDimensions.unit();
		voxelDimensions.dimensions( voxelSize );
	}

	/**
	 * Set the min coordinate.
	 * (The voxel size and unit is not modified).
	 * <p>
	 * Note, that the min coordinate is in ImgLib2 convention: It refers to the
	 * voxel center. This is in contrast to Imaris conventions, where {@code
	 * ExtendMinX, ExtendMinY, ExtendMinZ} indicate the min corner of the min voxel.
	 *
	 * @param min
	 * 		X,Y,Z of min coordinate
	 */
	void setMin( final double... min )
	{
		if ( min.length != 3 )
			throw new IllegalArgumentException();
		System.arraycopy( min, 0, this.min, 0, 3 );
	}

	private final VoxelDimensions voxelDimensions = new VoxelDimensions()
	{
		@Override
		public String unit()
		{
			return unit;
		}

		@Override
		public void dimensions( final double[] dims )
		{
			for ( int d = 0; d < dims.length; ++d )
				dims[ d ] = voxelSize[ d ];
		}

		@Override
		public double dimension( final int d )
		{
			return voxelSize[ d ];
		}

		@Override
		public int numDimensions()
		{
			return 3;
		}

		@Override
		public String toString()
		{
			final StringBuffer sb = new StringBuffer( this.getClass().getSimpleName() );
			sb.append( "{unit='" ).append( unit ).append( '\'' );
			sb.append( ", dimensions=" ).append( Arrays.toString( voxelSize ) );
			sb.append( '}' );
			return sb.toString();
		}
	};

	/**
	 * Get a {@code VoxelDimensions} view of this {@code DatasetCalibration}
	 */
	VoxelDimensions voxelDimensions()
	{
		return voxelDimensions;
	}

	DatasetCalibration copy()
	{
		final DatasetCalibration copy = new DatasetCalibration();
		copy.set( this );
		return copy;
	}
}
