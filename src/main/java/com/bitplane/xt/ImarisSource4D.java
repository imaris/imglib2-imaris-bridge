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
 *
 * @author Tobias Pietzsch
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
