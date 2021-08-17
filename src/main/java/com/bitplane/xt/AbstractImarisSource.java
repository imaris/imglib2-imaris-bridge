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
package com.bitplane.xt;

import bdv.util.DefaultInterpolators;
import bdv.viewer.Source;
import com.bitplane.xt.util.DatasetCalibration;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.NumericType;

/**
 * Implement the {@link Source} interface for resolution pyramids,
 * where each resolution level is given by a {@code RandomAccessibleInterval}.
 * <p>
 * The {@code RandomAccessibleInterval}s are either 3D (XYZ) or 4D (XYZT), depending on whether there are timepoints.
 * These cases are handled in derived classes {@link ImarisSource3D} and {@link ImarisSource4D}.
 *
 * @param <T>
 * 		pixel type of the source
 *
 * @author Tobias Pietzsch
 */
abstract class AbstractImarisSource< T extends NumericType< T > > implements Source< T >
{
	final DatasetCalibration calib;

	final T type;

	final String name;

	/**
	 * The 3D or 4D sources (one for each resolution level)
	 */
	final RandomAccessibleInterval< T >[] mipmapSources;

	/**
	 * Stores the scale factors for each resolution level to the full
	 * resolution.
	 * <p>
	 * A voxel at resolution level {@code l} has the same size in dimension
	 * {@code d=0,1,2} as {@code mipmapScales[l][d]} voxels in the full
	 * resolution. For example, if the second pyramid level is at half
	 * resolution in every dimension, {@code mipmapScales[1]={2,2,2}}.
	 */
	private final double[][] mipmapScales;

	/**
	 * The number of resolution levels
	 */
	final int numResolutions;

	/**
	 * The transformations from source to world space (one for each resolution level).
	 * This comprises the anisotropy (from voxelDimensions) as well as the scaling
	 * of the respective resolution level to full resolution.
	 */
	final AffineTransform3D[] mipmapTransforms;

	/**
	 * InterpolatorFactories for NEARESTNEIGHBOR and NLINEAR interpolation.
	 */
	final DefaultInterpolators< T > interpolators;

	AbstractImarisSource(
			final DatasetCalibration calib,
			final T type,
			final RandomAccessibleInterval< T >[] mipmapSources,
			final double[][] mipmapScales,
			final String name )
	{
		this.calib = new DatasetCalibration();
		this.type = type;
		this.mipmapScales = mipmapScales;
		this.name = name;
		this.mipmapSources = mipmapSources;

		numResolutions = mipmapSources.length;
		mipmapTransforms = new AffineTransform3D[ numResolutions ];
		interpolators = new DefaultInterpolators<>();

		setCalibration( calib );
	}

	@Override
	public void getSourceTransform( final int t, final int level, final AffineTransform3D transform )
	{
		transform.set( mipmapTransforms[ level ] );
	}

	@Override
	public T getType()
	{
		return type;
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public VoxelDimensions getVoxelDimensions()
	{
		return calib.voxelDimensions();
	}

	@Override
	public int getNumMipmapLevels()
	{
		return numResolutions;
	}

	/**
	 * Recompute mipmapTransforms from the given {@code DatasetCalibration}.
	 */
	public void setCalibration( final DatasetCalibration calib )
	{
		this.calib.set( calib );

		final AffineTransform3D sourceTransform = new AffineTransform3D();
		sourceTransform.set(
				calib.voxelSize( 0 ), 0, 0, calib.min( 0 ),
				0, calib.voxelSize( 1 ), 0, calib.min( 1 ),
				0, 0, calib.voxelSize( 2 ), calib.min( 2 ) );
		for ( int s = 0; s < numResolutions; ++s )
		{
			final AffineTransform3D mipmapTransform = new AffineTransform3D();
			mipmapTransform.set(
					mipmapScales[ s ][ 0 ], 0, 0, 0.5 * ( mipmapScales[ s ][ 0 ] - 1 ),
					0, mipmapScales[ s ][ 1 ], 0, 0.5 * ( mipmapScales[ s ][ 1 ] - 1 ),
					0, 0, mipmapScales[ s ][ 2 ], 0.5 * ( mipmapScales[ s ][ 2 ] - 1 ) );
			mipmapTransform.preConcatenate(sourceTransform);
			mipmapTransforms[ s ] = mipmapTransform;
		}
	}
}
