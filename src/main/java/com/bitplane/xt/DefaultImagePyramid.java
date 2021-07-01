package com.bitplane.xt;

import bdv.util.AxisOrder;
import net.imglib2.RandomAccessibleInterval;

/**
 * Default implementation of {@link ImagePyramid} with empty image arrays (that
 * need to be filled by the creator).
 */
class DefaultImagePyramid< T, V > implements ImagePyramid< T, V >
{
	private final T type;

	private final V volatileType;

	private final int numResolutions;

	private final AxisOrder axisOrder;

	final RandomAccessibleInterval< T >[] imgs;

	final RandomAccessibleInterval< V >[] vimgs;

	DefaultImagePyramid( final T type, final V volatileType, final int numResolutions, final AxisOrder axisOrder )
	{
		this.type = type;
		this.volatileType = volatileType;
		this.numResolutions = numResolutions;
		this.axisOrder = axisOrder;
		imgs = new RandomAccessibleInterval[ numResolutions ];
		vimgs = new RandomAccessibleInterval[ numResolutions ];
	}

	@Override
	public int numResolutions()
	{
		return numResolutions;
	}

	@Override
	public AxisOrder axisOrder()
	{
		return axisOrder;
	}

	@Override
	public RandomAccessibleInterval< T > getImg( final int resolutionLevel )
	{
		return imgs[ resolutionLevel ];
	}

	@Override
	public RandomAccessibleInterval< V > getVolatileImg( final int resolutionLevel )
	{
		return vimgs[ resolutionLevel ];
	}

	@Override
	public T getType()
	{
		return type;
	}

	@Override
	public V getVolatileType()
	{
		return volatileType;
	}
}
