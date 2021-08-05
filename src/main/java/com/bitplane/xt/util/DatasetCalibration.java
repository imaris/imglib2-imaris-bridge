package com.bitplane.xt.util;

import Imaris.Error;
import Imaris.IDataSetPrx;
import mpicbg.spim.data.sequence.VoxelDimensions;

/**
 * Modifiable implementation of {@link VoxelDimensions}. Units and dimensions
 * can be changed, but number of dimensions is fixed after construction.
 *
 * @author Tobias Pietzsch
 */
// TODO rename
public final class DatasetCalibration implements VoxelDimensions
{
	private String unit = "pixel";

	private final int[] size = { 1, 1, 1 };

	private final double[] voxelSize = { 1, 1, 1 };

	private final double[] min = { 0, 0, 0 };

	public DatasetCalibration()
	{
	}

	public DatasetCalibration( final DatasetCalibration that )
	{
		set( that );
	}

	public void set( final DatasetCalibration that )
	{
		this.unit = that.unit;
		for ( int d = 0; d < 3; d++ )
		{
			this.voxelSize[ d ] = that.voxelSize[ d ];
			this.min[ d ] = that.min[ d ];
		}
	}

	/**
	 * TODO
	 * Get the {@code VoxelDimensions} that correspond to the extends of the specified {@code dataset}.
	 */
	public DatasetCalibration( final IDataSetPrx dataset ) throws Error
	{
		setFrom( dataset );
	}

	public void setTo( final IDataSetPrx dataset ) throws Error
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

	public void setFrom( final IDataSetPrx dataset ) throws Error
	{
		set( dataset.GetUnit(),
				dataset.GetSizeX(),
				dataset.GetSizeY(),
				dataset.GetSizeZ(),
				dataset.GetExtendMinX(),
				dataset.GetExtendMaxX(),
				dataset.GetExtendMinY(),
				dataset.GetExtendMaxY(),
				dataset.GetExtendMinZ(),
				dataset.GetExtendMaxZ() );
	}

	private void set(
			final String unit,
			final int sizeX,
			final int sizeY,
			final int sizeZ,
			final double extendMinX,
			final double extendMaxX,
			final double extendMinY,
			final double extendMaxY,
			final double extendMinZ,
			final double extendMaxZ )
	{
		size[ 0 ] = sizeX;
		size[ 1 ] = sizeY;
		size[ 2 ] = sizeZ;
		set( unit, extendMinX, extendMaxX, extendMinY, extendMaxY, extendMinZ, extendMaxZ );
	}

	public void set(
			final String unit,
			final double extendMinX,
			final double extendMaxX,
			final double extendMinY,
			final double extendMaxY,
			final double extendMinZ,
			final double extendMaxZ )
	{
		this.unit = unit;
		voxelSize[ 0 ] = ( extendMaxX - extendMinX ) / size[ 0 ];
		voxelSize[ 1 ] = ( extendMaxY - extendMinY ) / size[ 1 ];
		voxelSize[ 2 ] = ( extendMaxZ - extendMinZ ) / size[ 2 ];
		min[ 0 ] = extendMinX + voxelSize[ 0 ] / 2;
		min[ 1 ] = extendMinY + voxelSize[ 1 ] / 2;
		min[ 2 ] = extendMinZ + voxelSize[ 2 ] / 2;
	}

	public DatasetCalibration( final String unit, final double[] voxelSize, final double[] min )
	{
		if ( voxelSize.length != 3 )
			throw new IllegalArgumentException();
		if ( min.length != 3 )
			throw new IllegalArgumentException();
		this.unit = unit;
		System.arraycopy( voxelSize, 0, this.voxelSize, 0, 3 );
		System.arraycopy( min, 0, this.min, 0, 3 );
	}

	public void set( final VoxelDimensions voxelDimensions, final double... min )
	{
		if ( voxelDimensions.numDimensions() != 3 )
			throw new IllegalArgumentException();
		if ( min.length != 3 )
			throw new IllegalArgumentException();
		this.unit = voxelDimensions.unit();
		for ( int d = 0; d < 3; ++d )
		{
			voxelSize[ d ] = voxelDimensions.dimension( d );
			this.min[ d ] = min[ d ];
		}
	}

	@Override
	public int numDimensions()
	{
		return 3;
	}

	@Override
	public String unit()
	{
		return unit;
	}

	@Override
	public void dimensions( final double[] dims )
	{
		for ( int d = 0; d < dims.length; ++d )
			dims[ d ] = this.voxelSize[ d ];
	}

	@Override
	public double dimension( final int d )
	{
		return voxelSize[ d ];
	}

	public VoxelDimensions voxelDimensions()
	{
		return this;
	}

	public double min( final int d )
	{
		return min[ d ];
	}

	public int size( final int d )
	{
		return size[ d ];
	}
}
