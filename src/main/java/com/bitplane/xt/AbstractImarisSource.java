package com.bitplane.xt;

import bdv.util.DefaultInterpolators;
import bdv.viewer.Source;
import com.bitplane.xt.util.ModifiableVoxelDimensions;
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
 */
abstract class AbstractImarisSource< T extends NumericType< T > > implements Source< T >
{
	final ModifiableVoxelDimensions voxelDimensions;

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
			final VoxelDimensions voxelDimensions,
			final double minX,
			final double minY,
			final double minZ,
			final T type,
			final RandomAccessibleInterval< T >[] mipmapSources,
			final double[][] mipmapScales,
			final String name )
	{
		this.voxelDimensions = new ModifiableVoxelDimensions( voxelDimensions );
		this.type = type;
		this.mipmapScales = mipmapScales;
		this.name = name;
		this.mipmapSources = mipmapSources;

		numResolutions = mipmapSources.length;
		mipmapTransforms = new AffineTransform3D[ numResolutions ];
		interpolators = new DefaultInterpolators<>();

		setCalibration( voxelDimensions, minX, minY, minZ );
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
		return voxelDimensions;
	}

	@Override
	public int getNumMipmapLevels()
	{
		return numResolutions;
	}

	/**
	 * Recompute mipmapTransforms from the given voxelDimensions and min
	 * coordinates.
	 * <p>
	 * Note, that min coordinates are in ImgLib2 convention, that is, they
	 * indicate the center coordinate min voxel. This is in contrast to Iamris
	 * conventions, where min coordinates indicate the min corner of the min
	 * voxel.
	 */
	public void setCalibration(
			final VoxelDimensions voxelDimensions,
			final double minX,
			final double minY,
			final double minZ )
	{
		this.voxelDimensions.set( voxelDimensions );

		final AffineTransform3D sourceTransform = new AffineTransform3D();
		sourceTransform.set(
				voxelDimensions.dimension( 0 ), 0, 0, minX,
				0, voxelDimensions.dimension( 1 ), 0, minY,
				0, 0, voxelDimensions.dimension( 2 ), minZ );
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
