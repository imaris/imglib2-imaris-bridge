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
