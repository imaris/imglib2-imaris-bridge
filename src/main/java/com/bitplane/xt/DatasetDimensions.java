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

import Imaris.Error;
import Imaris.IDataSetPrx;
import bdv.util.AxisOrder;
import com.bitplane.xt.options.ImarisAxesOptions.Axis;
import com.bitplane.xt.util.MapDimensions;
import java.util.Arrays;
import java.util.List;

import static com.bitplane.xt.options.ImarisAxesOptions.Axis.Z;
import static com.bitplane.xt.options.ImarisAxesOptions.Axis.C;
import static com.bitplane.xt.options.ImarisAxesOptions.Axis.T;

/**
 * Dimensions of an {@code ImarisDataset}.
 * <p>
 * Stores the 5D {@link #getImarisDimensions() XYZCT dimensions} of the
 * underlying Imaris dataset, as well as the {@link #getMapDimensions() mapping}
 * from Imaris to ImgLib2 dimensions.
 * <p>
 * In Imaris, datasets are always 5D: for example, a 2D dataset (without channel
 * or time) is represented as 5D with {@code size=1} along Z, C, T axes. In
 * ImgLib2, there is a distinction between a 2D image and a 5D image with {@code
 * size=1} along the 3rd, 4th, and 5th dimension. Therefore, there are several
 * ways to represent such an Imaris dataset in ImgLib2. Which way is chosen is
 * specified by the {@link #getMapDimensions() mapping} from Imaris to ImgLib2
 * dimensions, as well as (redundantly) the {@code #getAxisOrder axis order} (of
 * the ImgLib2 representation).
 *
 * @author Tobias Pietzsch
 */
public final class DatasetDimensions
{
	/**
	 * 5D dimensions of the dataset on the imaris side
	 */
	private final int[] imarisDimensions;

	/**
	 * Maps Imaris dimension indices to imglib2 dimension indices.
	 * If i is dimension index from Imaris (0..4 means X,Y,Z,C,T)
	 * then mapDimensions[i] is the corresponding imglib2 dimension, e.g., in imagePyramid.
	 * <p>
	 * For imglib2 dimensions, Imaris dimensions with size=1 maybe skipped.
	 * E.g., for a XYC image {@code mapDimensions = {0,1,-1,2,-1}}.
	 */
	private final int[] mapDimensions;

	private final AxisOrder axisOrder;

	/**
	 * Create {@code DatasetDimensions}.
	 * <p>
	 * Sizes {@code s≤0} indicate missing dimensions. For example, {@code new
	 * DatasetDimension(new UnsignedByteType(), 300, 200, 0, 0, 0)} represents a
	 * 2D ImgLib2 image with axes {@code [X,Y]}. The size along missing
	 * dimensions (Z, C, T in this example) is set to {@code s=1} on the Imaris
	 * side.
	 *
	 * @param sx
	 * 		size in X dimension
	 * @param sy
	 * 		size in Y dimension
	 * @param sz
	 * 		size in Z dimension
	 * @param sc
	 * 		size in C (channel) dimension
	 * @param st
	 * 		size in T (time) dimension
	 */
	public DatasetDimensions( final int sx, final int sy, final int sz, final int sc, final int st )
	{
		if ( sx < 0 || sy < 0 || sz < 0 || sc < 0 || st < 0 )
			throw new IllegalArgumentException( "Dimensions must be ≥ 0" );

		if ( sx <= 0 || sy <= 0 )
			throw new IllegalArgumentException( "Dataset must have at least dimensions X and Y" );

		imarisDimensions = new int[] { sx, sy, 1, 1, 1 };

		final StringBuffer sbAxisOrder = new StringBuffer( "XY" );
		if ( sz > 0 )
		{
			sbAxisOrder.append( "Z" );
			imarisDimensions[ 2 ] = sz;
		}
		if ( sc > 0 )
		{
			sbAxisOrder.append( "C" );
			imarisDimensions[ 3 ] = sc;
		}
		if ( st > 0 )
		{
			sbAxisOrder.append( "T" );
			imarisDimensions[ 4 ] = st;
		}

		axisOrder = AxisOrder.valueOf( sbAxisOrder.toString() );
		mapDimensions = MapDimensions.fromAxisOrder( axisOrder );
	}

	/**
	 * Create {@code DatasetDimensions} from the given Imaris {@code IDataSetPrx}.
	 * <p>
	 * The optional {@code includeAxes} arguments specify which axes must be
	 * included (at least) in the ImgLib2 representation.
	 * <p>
	 * In Imaris, datasets are always 5D: for example, a 2D dataset (without channel
	 * or time) is represented as 5D with {@code size=1} along Z, C, T axes. In
	 * ImgLib2, there is a distinction between a 2D image and a 5D image with {@code
	 * size=1} along the 3rd, 4th, and 5th dimension. Therefore, there are several
	 * ways to represent such an Imaris dataset in ImgLib2. Which way is chosen is
	 * specified by the {@link #getMapDimensions() mapping} from Imaris to ImgLib2
	 * dimensions, as well as (redundantly) the {@code #getAxisOrder axis order} (of
	 * the ImgLib2 representation).
	 * <p>
	 * By default, axes Z, C, and T are not represented in ImgLib2 if the
	 * dataset size along those axes is {@code s=1}. By specifying these axes as
	 * {@code includeAxes} arguments, this can be overridden. For example, an
	 * Imaris dataset with size {@code {100, 100, 1, 1, 1}} would be represented
	 * as a 2D ImgLib2 image with size {@code {100, 100}}. Specifying {@code
	 * includeAxes = Z, T}, would result in a 4D ImgLib2 image with size {@code
	 * {100,100,1,1}}.
	 */
	public DatasetDimensions( final IDataSetPrx dataset, final Axis... includeAxes ) throws Error
	{
		final int sx = dataset.GetSizeX();
		final int sy = dataset.GetSizeY();
		final int sz = dataset.GetSizeZ();
		final int sc = dataset.GetSizeC();
		final int st = dataset.GetSizeT();
		imarisDimensions = new int[] { sx, sy, sz, sc, st };

		final List< Axis > axes = Arrays.asList( includeAxes );
		final StringBuffer sbAxisOrder = new StringBuffer( "XY" );
		if ( sz > 1 || axes.contains( Z ) )
			sbAxisOrder.append( "Z" );
		if ( sc > 1 || axes.contains( C ) )
			sbAxisOrder.append( "C" );
		if ( st > 1  || axes.contains( T ))
			sbAxisOrder.append( "T" );
		axisOrder = AxisOrder.valueOf( sbAxisOrder.toString() );

		mapDimensions = MapDimensions.fromAxisOrder( axisOrder );
	}

	/**
	 * Get the mapping between dimension indices in the Imaris and in the
	 * ImgLib2 representation.
	 * <p>
	 * The returned {@code int[5]} array maps Imaris dimension indices to
	 * imglib2 dimension indices. If i is dimension index from Imaris (0..4
	 * means X,Y,Z,C,T) then mapDimensions[i] is the corresponding imglib2
	 * dimension, e.g., in imagePyramid.
	 * <p>
	 * For imglib2 dimensions, Imaris dimensions with size=1 maybe skipped.
	 * E.g., for a XYC image {@code mapDimensions = {0,1,-1,2,-1}}.
	 */
	public int[] getMapDimensions()
	{
		return mapDimensions;
	}

	/**
	 * Returns the 5D dimensions of the dataset on the imaris side.
	 */
	public int[] getImarisDimensions()
	{
		return imarisDimensions;
	}

	/**
	 * Returns the {@code AxisOrder} of the ImgLib2 representation. The returned
	 * value will be one of {@code XY}, {@code XYZ}, {@code XYC}, {@code XYT},
	 * {@code XYZC}, {@code XYZT}, {@code XYCT}, {@code XYZCT}.
	 */
	public AxisOrder getAxisOrder()
	{
		return axisOrder;
	}

	/**
	 * Get the index of the X dimension in the ImgLib2 representation.
	 * (This is always {@code 0}.)
	 */
	public int dimX()
	{
		return mapDimensions[ 0 ];
	}

	/**
	 * Returns the size along X.
	 */
	public int sizeX()
	{
		return imarisDimensions[ 0 ];
	}

	/**
	 * Get the index of the Y dimension in the ImgLib2 representation.
	 * (This is always {@code 1}.)
	 */
	public int dimY()
	{
		return mapDimensions[ 1 ];
	}

	/**
	 * Returns the size along Y.
	 */
	public int sizeY()
	{
		return imarisDimensions[ 1 ];
	}

	/**
	 * Returns {@code true} if the ImgLib2 representation has a Z dimension.
	 */
	public boolean hasZ()
	{
		return dimZ() >= 0;
	}

	/**
	 * Get the index of the Z dimension in the ImgLib2 representation.
	 *
	 * @return the index of the Z dimension, or -1 if the ImgLib2 representation doesn't have Z
	 */
	public int dimZ()
	{
		return mapDimensions[ 2 ];
	}

	/**
	 * Returns the size along Z.
	 * If the ImgLib2 representation does not have a Z dimension, returns {@code 1}.
	 */
	public int sizeZ()
	{
		return imarisDimensions[ 2 ];
	}

	/**
	 * Returns {@code true} if the ImgLib2 representation has a channel dimension.
	 */
	public boolean hasC()
	{
		return dimC() >= 0;
	}

	/**
	 * Get the index of the channel dimension in the ImgLib2 representation.
	 *
	 * @return the index of the channel dimension, or -1 if the ImgLib2 representation doesn't have channels
	 */
	public int dimC()
	{
		return mapDimensions[ 3 ];
	}

	/**
	 * Returns the number of channels.
	 * If the ImgLib2 representation does not have channels, returns {@code 1}.
	 */
	public int sizeC()
	{
		return imarisDimensions[ 3 ];
	}

	/**
	 * Returns {@code true} if the ImgLib2 representation has a time dimension.
	 */
	public boolean hasT()
	{
		return dimT() >= 0;
	}

	/**
	 * Get the index of the time dimension in the ImgLib2 representation.
	 *
	 * @return the index of the time dimension, or -1 if the ImgLib2 representation doesn't have a time dimension.
	 */
	public int dimT()
	{
		return mapDimensions[ 4 ];
	}

	/**
	 * Returns the number of timepoints.
	 * If the ImgLib2 representation does not have timepoints, return {@code 1}.
	 */
	public int sizeT()
	{
		return imarisDimensions[ 4 ];
	}
}
