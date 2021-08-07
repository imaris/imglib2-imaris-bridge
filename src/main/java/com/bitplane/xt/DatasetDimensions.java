package com.bitplane.xt;

import Imaris.Error;
import Imaris.IDataSetPrx;
import bdv.util.AxisOrder;
import com.bitplane.xt.ImarisAxesOptions.Axis;
import com.bitplane.xt.util.MapDimensions;
import java.util.Arrays;
import java.util.List;

import static com.bitplane.xt.ImarisAxesOptions.Axis.Z;
import static com.bitplane.xt.ImarisAxesOptions.Axis.C;
import static com.bitplane.xt.ImarisAxesOptions.Axis.T;

/**
 * Dimensions of an ImarisDataset.
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
}
