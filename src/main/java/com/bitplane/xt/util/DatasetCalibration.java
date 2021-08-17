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

import Imaris.Error;
import Imaris.IDataSetPrx;
import java.util.Arrays;
import mpicbg.spim.data.sequence.VoxelDimensions;

/**
 * Calibration (voxel size and unit) and min coordinate for a Imaris dataset.
 * <p>
 * Note, that the min coordinate is in ImgLib2 convention: It refers to the
 * voxel center. This is in contrast to Imaris conventions, where {@code
 * ExtendMinX, ExtendMinY, ExtendMinZ} indicate the min corner of the min voxel.
 * <p>
 * When reading/writing extends from/to a {@code IDataSetPrx}, the min is
 * translated appropriately from/to Imaris conventions.
 *
 * @author Tobias Pietzsch
 */
public final class DatasetCalibration
{
	private String unit = "pixel";
	private final int[] size = { 1, 1, 1 };
	private final double[] voxelSize = { 1, 1, 1 };
	private final double[] min = { 0, 0, 0 };
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
	 * Construct with default calibration:
	 * <ul>
	 *     <li>voxel size is 1x1x1</li>
	 *     <li>unit is "pixel"</li>
	 *     <li>min coordinate is 0</li>
	 * </ul>
	 */
	public DatasetCalibration() {}

	/**
	 * Construct by copying {@code calib}.
	 */
	public DatasetCalibration( final DatasetCalibration calib )
	{
		set( calib );
	}

	/**
	 * Construct from the size and extends of {@code dataset}.
	 */
	public DatasetCalibration( final IDataSetPrx dataset ) throws Error
	{
		setFromDataset( dataset );
	}

	public void set( final DatasetCalibration calib )
	{
		this.unit = calib.unit;
		for ( int d = 0; d < 3; d++ )
		{
			this.size[ d ] = calib.size[ d ];
			this.voxelSize[ d ] = calib.voxelSize[ d ];
			this.min[ d ] = calib.min[ d ];
		}
	}

	public void applyToDataset( final IDataSetPrx dataset ) throws Error
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

	public void setFromDataset( final IDataSetPrx dataset ) throws Error
	{
		size[ 0 ] = dataset.GetSizeX();
		size[ 1 ] = dataset.GetSizeY();
		size[ 2 ] = dataset.GetSizeZ();
		setExtends( dataset.GetUnit(),
				dataset.GetExtendMinX(), dataset.GetExtendMaxX(),
				dataset.GetExtendMinY(), dataset.GetExtendMaxY(),
				dataset.GetExtendMinZ(), dataset.GetExtendMaxZ() );
	}

	/**
	 * Sets unit, voxel size, and min coordinate from Imaris extends.
	 * <p>
	 * Note, that the given min/max extends are in Imaris conventions:
	 * {@code extendMinX} refers to the min corner of the min voxel of the dataset,
	 * {@code extendMaxX} refers to the max corner of the max voxel of the dataset.
	 * <p>
	 * This is in contrast to the ImgLib2 convention, where coordinates always refer to the
	 * voxel center. This method translates the {@code extendMin/Max} arguments to ImgLib2 conventions.
	 */
	public void setExtends( final String unit,
			final double extendMinX, final double extendMaxX,
			final double extendMinY, final double extendMaxY,
			final double extendMinZ, final double extendMaxZ )
	{
		this.unit = unit;
		voxelSize[ 0 ] = ( extendMaxX - extendMinX ) / size[ 0 ];
		voxelSize[ 1 ] = ( extendMaxY - extendMinY ) / size[ 1 ];
		voxelSize[ 2 ] = ( extendMaxZ - extendMinZ ) / size[ 2 ];
		min[ 0 ] = extendMinX + voxelSize[ 0 ] / 2;
		min[ 1 ] = extendMinY + voxelSize[ 1 ] / 2;
		min[ 2 ] = extendMinZ + voxelSize[ 2 ] / 2;
	}

	/**
	 * Set the voxel size and unit.
	 * (The min coordinate is not modified).
	 */
	public void setVoxelDimensions( final VoxelDimensions voxelDimensions )
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
	public void setMin( final double... min )
	{
		if ( min.length != 3 )
			throw new IllegalArgumentException();
		System.arraycopy( min, 0, this.min, 0, 3 );
	}

	/**
	 * Get a {@code VoxelDimensions} view of this {@code DatasetCalibration}
	 */
	public VoxelDimensions voxelDimensions()
	{
		return voxelDimensions;
	}

	/**
	 * Get units for {@link #voxelSize(int)}
	 */
	public String unit()
	{
		return unit;
	}

	/**
	 * Get size of a voxel in dimension {@code d}.
	 */
	public double voxelSize( final int d )
	{
		return this.voxelSize[ d ];
	}

	/**
	 * Get the coordinate (in dimension {@code d}) of the min corner of the dataset.
	 */
	public double min( final int d )
	{
		return min[ d ];
	}

	/**
	 * Get size of a voxel in dimension {@code d}.
	 */
	public int size( final int d )
	{
		return size[ d ];
	}

	@Override
	public String toString()
	{
		final StringBuffer sb = new StringBuffer( "DatasetCalibration{" );
		sb.append( "unit='" ).append( unit ).append( '\'' );
		sb.append( ", size=" );
		if ( size == null )
			sb.append( "null" );
		else
		{
			sb.append( '[' );
			for ( int i = 0; i < size.length; ++i )
				sb.append( i == 0 ? "" : ", " ).append( size[ i ] );
			sb.append( ']' );
		}
		sb.append( ", voxelSize=" );
		if ( voxelSize == null )
			sb.append( "null" );
		else
		{
			sb.append( '[' );
			for ( int i = 0; i < voxelSize.length; ++i )
				sb.append( i == 0 ? "" : ", " ).append( voxelSize[ i ] );
			sb.append( ']' );
		}
		sb.append( ", min=" );
		if ( min == null )
			sb.append( "null" );
		else
		{
			sb.append( '[' );
			for ( int i = 0; i < min.length; ++i )
				sb.append( i == 0 ? "" : ", " ).append( min[ i ] );
			sb.append( ']' );
		}
		sb.append( '}' );
		return sb.toString();
	}
}
