package com.bitplane.xt;

import Imaris.Error;
import Imaris.IApplicationPrx;
import net.imagej.Dataset;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public interface ImarisApplication
{
	/**
	 * Get the underlying {@code IApplication} ICE proxy.
	 */
	IApplicationPrx getIApplicationPrx();

	/**
	 * Get the object ID of the underlying {@code IApplication} ICE proxy.
	 */
	int getApplicationID();

	/**
	 * Get the number of images loaded in the application.
	 */
	int getNumberOfImages();

	/**
	 * Get the Imaris image at {@code imageIndex} as an {@code ImarisDataset}.
	 */
	ImarisDataset< ? > getImage( int imageIndex, ImarisDatasetOptions options );

	/**
	 * Get the Imaris image at {@code imageIndex} as an {@code ImarisDataset}.
	 */
	default ImarisDataset< ? > getImage( int imageIndex )
	{
		return getImage( imageIndex, ImarisDatasetOptions.options() );
	}

	/**
	 * Get the first Imaris image as an {@code ImarisDataset}.
	 * Equivalent to {@link #getImage getImage(0)}.
	 */
	default ImarisDataset< ? > getDataset()
	{
		return getImage( 0 );
	}

	/**
	 * Get the first Imaris image as an {@code ImarisDataset}.
	 * Equivalent to {@link #getImage getImage(0)}.
	 */
	default ImarisDataset< ? > getDataset( ImarisDatasetOptions options )
	{
		return getImage( 0, options );
	}

	/**
	 * Set an image to the application. Image index is clamped to be at most the
	 * current number of images in the application.
	 */
	void setImage( int imageIndex, ImarisDataset< ? > dataset );

	/**
	 * Equivalent to {@link #setImage setImage(0,dataset)}.
	 */
	default void setDataset( ImarisDataset< ? > dataset )
	{
		setImage( 0, dataset );
	}

	/**
	 * Create a new {@code ImarisDataset} of the specified {@code type} with the
	 * specified dimensions.
	 * <p>
	 * Sizes {@code s≤0} indicate missing dimensions. For example, {@code
	 * createDataset(new UnsignedByteType(), 300, 200, 0, 0, 0)} creates a 2D
	 * {@code ImarisDataset} with axes {@code [X,Y]}. The size along missing
	 * dimensions (Z, C, T in this example) is set to {@code s=1} on the Imaris
	 * side.
	 *
	 * @param type
	 * 		imglib2 pixel type. Must be one of {@code UnsignedByteType}, {@code UnsignedShortType}, or {@code FloatType}.
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
	 * @param options
	 */
	< T extends NativeType< T > & RealType< T > >
	ImarisDataset< T > createDataset( T type, int sx, int sy, int sz, int sc, int st, ImarisDatasetOptions options );

	/**
	 * Calls {@link #createDataset(NativeType, int, int, int, int, int, ImarisDatasetOptions)} with default options.
	 */
	default < T extends NativeType< T > & RealType< T > >
	ImarisDataset< T > createDataset( T type, int sx, int sy, int sz, int sc, int st )
	{
		return createDataset( type, sx, sy, sz, sc, st, ImarisDatasetOptions.options() );
	}

}


