package com.bitplane.xt;

import bdv.viewer.Interpolation;
import com.bitplane.xt.util.DatasetCalibration;
import java.util.Arrays;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.view.Views;

/**
 * Specialization for 4D sources (with timepoints).
 * Note that this is also used for planar sources, which are augmented by a Z dimension of size 1.
 *
 * @param <T>
 * 		pixel type of the source
 */
class ImarisSource4D< T extends NumericType< T > > extends AbstractImarisSource< T >
{
	private final int numTimepoints;

	private int currentTimePointIndex = -1;

	private final RandomAccessibleInterval< T >[] currentMipmapSources;

	private final RealRandomAccessible< T >[][] currentInterpolatedMipmapSources;

	ImarisSource4D(
			final DatasetCalibration calib,
			final T type,
			final RandomAccessibleInterval< T >[] mipmapSources,
			final double[][] mipmapScales,
			final String name )
	{
		super( calib, type, mipmapSources, mipmapScales, name );

		numTimepoints = ( int ) mipmapSources[ 0 ].dimension( 3 );
		currentMipmapSources = new RandomAccessibleInterval[ numResolutions ];
		currentInterpolatedMipmapSources = new RealRandomAccessible[ interpolators.size() ][ numResolutions ];
	}

	@Override
	public boolean isPresent( final int t )
	{
		return 0 <= t && t < numTimepoints;
	}

	private void loadTimepoint( final int timepointIndex )
	{
		if ( currentTimePointIndex != timepointIndex )
		{
			currentTimePointIndex = timepointIndex;
			if ( isPresent( timepointIndex ) )
			{
				final T zero = getType().createVariable();
				zero.setZero();
				for ( int s = 0; s < numResolutions; ++s )
				{
					currentMipmapSources[ s ] = Views.hyperSlice( mipmapSources[ s ], 3, timepointIndex );
					for ( final Interpolation method : Interpolation.values() )
						currentInterpolatedMipmapSources[ method.ordinal() ][ s ] = Views.interpolate( Views.extendValue( currentMipmapSources[ s ], zero ), interpolators.get( method ) );
				}
			}
			else
			{
				Arrays.fill( currentMipmapSources, null );
				for ( final Interpolation method : Interpolation.values() )
					Arrays.fill( currentInterpolatedMipmapSources[ method.ordinal() ], null );
			}
		}
	}

	@Override
	public synchronized RandomAccessibleInterval< T > getSource( final int t, final int level )
	{
		loadTimepoint( t );
		return currentMipmapSources[ level ];
	}

	@Override
	public synchronized RealRandomAccessible< T > getInterpolatedSource( final int t, final int level, final Interpolation method )
	{
		loadTimepoint( t );
		return currentInterpolatedMipmapSources[ method.ordinal() ][ level ];
	}
}
