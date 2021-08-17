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

import bdv.util.AxisOrder;
import java.util.Arrays;
import net.imglib2.RandomAccessibleInterval;

/**
 * Contains images and volatile images for each resolution level.
 * Holds metadata about axis order and number of resolutions, channels, and timepoints.
 *
 * @param <T>
 * 		pixel type
 * @param <V>
 * 		corresponding volatile pixel type
 *
 * @author Tobias Pietzsch
 */
interface ImagePyramid< T, V >
{
	int numResolutions();

	AxisOrder axisOrder();

	default boolean hasChannels()
	{
		return axisOrder().hasChannels();
	}

	default boolean hasTimepoints()
	{
		return axisOrder().hasTimepoints();
	}

	default int numChannels()
	{
		return ( int ) axisOrder().numChannels( getImg( 0 ) );
	}

	default int numTimepoints()
	{
		return ( int ) axisOrder().numTimepoints( getImg( 0 ) );
	}

	RandomAccessibleInterval< T > getImg( final int resolutionLevel );

	RandomAccessibleInterval< V > getVolatileImg( final int resolutionLevel );

	default RandomAccessibleInterval< T >[] getImgs()
	{
		final RandomAccessibleInterval< T >[] result = new RandomAccessibleInterval[ numResolutions() ];
		Arrays.setAll( result, i -> getImg( i ) );
		return result;
	}

	default RandomAccessibleInterval< V >[] getVolatileImgs()
	{
		final RandomAccessibleInterval< V >[] result = new RandomAccessibleInterval[ numResolutions() ];
		Arrays.setAll( result, i -> getVolatileImg( i ) );
		return result;
	}

	/**
	 * Get an instance of the pixel type.
	 *
	 * @return instance of pixel type.
	 */
	T getType();

	/**
	 * Get an instance of the volatile pixel type.
	 *
	 * @return instance of volatile pixel type.
	 */
	V getVolatileType();
}
