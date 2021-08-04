package com.bitplane.xt.util;

import mpicbg.spim.data.sequence.FinalVoxelDimensions;
import mpicbg.spim.data.sequence.VoxelDimensions;

/**
 * Modifiable implementation of {@link VoxelDimensions}. Units and dimensions
 * can be changed, but number of dimensions is fixed after construction.
 *
 * @author Tobias Pietzsch
 */
public final class ModifiableVoxelDimensions implements VoxelDimensions
{
	private String unit;

	private final double[] dimensions;

	public ModifiableVoxelDimensions()
	{
		this( "pixel", new double[] { 1, 1, 1 } );
	}

	public ModifiableVoxelDimensions( final VoxelDimensions voxelDimensions )
	{
		unit = voxelDimensions.unit();
		dimensions = new double[ voxelDimensions.numDimensions() ];
		for ( int d = 0; d < dimensions.length; ++d )
			dimensions[ d ] = voxelDimensions.dimension( d );
	}

	public ModifiableVoxelDimensions( final String unit, final double... dimensions )
	{
		this.unit = unit;
		this.dimensions = dimensions.clone();
	}

	public void set( final String unit, final double... dimensions )
	{
		set( new FinalVoxelDimensions( unit, dimensions ) );
	}

	public void set( final VoxelDimensions voxelDimensions )
	{
		this.unit = voxelDimensions.unit();
		for ( int d = 0; d < Math.min( dimensions.length, voxelDimensions.numDimensions() ); ++d )
			dimensions[ d ] = voxelDimensions.dimension( d );
	}

	@Override
	public int numDimensions()
	{
		return dimensions.length;
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
			dims[ d ] = this.dimensions[ d ];
	}

	@Override
	public double dimension( final int d )
	{
		return dimensions[ d ];
	}
}
