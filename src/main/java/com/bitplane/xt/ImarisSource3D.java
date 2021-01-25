package com.bitplane.xt;

import bdv.viewer.Interpolation;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.view.Views;

/**
 * Specialization for 3D sources (no timepoints).
 * Note that this is also used for planar sources, which are augmented by a Z dimension of size 1.
 *
 * @param <T>
 * 		pixel type of the source
 */
class ImarisSource3D< T extends NumericType< T > > extends AbstractImarisSource< T >
{
	private final RealRandomAccessible< T >[][] interpolatedMipmapSources;

	ImarisSource3D(
			final VoxelDimensions voxelDimensions,
			final double minX,
			final double minY,
			final double minZ,
			final T type,
			final RandomAccessibleInterval< T >[] mipmapSources,
			final double[][] mipmapScales,
			final String name )
	{
		super( voxelDimensions, minX, minY, minZ, type, mipmapSources, mipmapScales, name );

		interpolatedMipmapSources = new RealRandomAccessible[ interpolators.size() ][ numResolutions ];
		final T zero = getType().createVariable();
		zero.setZero();
		for ( Interpolation method : Interpolation.values() )
			for ( int s = 0; s < numResolutions; ++s )
				interpolatedMipmapSources[ method.ordinal() ][ s ] = Views.interpolate( Views.extendValue( mipmapSources[ s ], zero ), interpolators.get( method ) );
	}

	@Override
	public boolean isPresent( final int t )
	{
		return true;
	}

	@Override
	public RandomAccessibleInterval< T > getSource( final int t, final int level )
	{
		return mipmapSources[ level ];
	}

	@Override
	public RealRandomAccessible< T > getInterpolatedSource( final int t, final int level, final Interpolation method )
	{
		return interpolatedMipmapSources[ method.ordinal() ][ level ];
	}
}
