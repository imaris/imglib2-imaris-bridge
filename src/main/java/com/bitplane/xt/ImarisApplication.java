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

import Imaris.IApplicationPrx;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

/**
 * {@code ImarisApplication} wraps {@code IApplicationPrx} and represents one
 * particular Imaris instance.
 *
 * @author Tobias Pietzsch
 */
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

	/**
	 * TODO
	 */
	< T extends NativeType< T > & RealType< T > >
	ImarisDataset< T > createDataset( T type, DatasetDimensions size, ImarisDatasetOptions options );

	/**
	 * TODO
	 */
	default < T extends NativeType< T > & RealType< T > >
	ImarisDataset< T > createDataset( T type, DatasetDimensions size )
	{
		return createDataset( type, size, ImarisDatasetOptions.options() );
	}

}


