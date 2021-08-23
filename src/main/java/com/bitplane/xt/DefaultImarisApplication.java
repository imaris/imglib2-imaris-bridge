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
import Imaris.IApplicationPrx;
import Imaris.IDataSetPrx;
import com.bitplane.xt.util.ImarisUtils;
import com.bitplane.xt.util.TypeUtils;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import org.scijava.AbstractContextual;

/**
 * Default implementation of {@code ImarisApplication}, which  wraps {@code
 * IApplicationPrx} and represents one particular Imaris instance.
 *
 * @author Tobias Pietzsch
 */
public class DefaultImarisApplication extends AbstractContextual implements ImarisApplication
{
	private IApplicationPrx iApplicationPrx;

	private int applicationId;

	public DefaultImarisApplication(
			final IApplicationPrx iApplicationPrx,
			final int applicationId )
	{
		this.iApplicationPrx = iApplicationPrx;
		this.applicationId = applicationId;
	}

	@Override
	public IApplicationPrx getIApplicationPrx()
	{
		return iApplicationPrx;
	}

	@Override
	public int getApplicationID()
	{
		return applicationId;
	}

	@Override
	public	int getNumberOfImages()
	{
		try
		{
			return getIApplicationPrx().GetNumberOfImages();
		}
		catch ( final Error error )
		{
			throw new RuntimeException( error ); // TODO: revise exception handling
		}
	}

	@Override
	public ImarisDataset< ? > getImage( final int imageIndex, final ImarisDatasetOptions options )
	{
		try
		{
			final IDataSetPrx datasetPrx = getIApplicationPrx().GetImage( imageIndex );
			if ( datasetPrx == null )
				return null;
			return new ImarisDataset<>( getContext(), datasetPrx, options );
		}
		catch ( final Error error )
		{
			throw new RuntimeException( error ); // TODO: revise exception handling
		}
	}

	@Override
	public void setImage( final int imageIndex, final ImarisDataset< ? > dataset )
	{
		try
		{
			iApplicationPrx.SetImage( imageIndex, dataset.getIDataSetPrx() );
		}
		catch ( final Error error )
		{
			throw new RuntimeException( error ); // TODO: revise exception handling
		}
	}

	@Override
	public < T extends NativeType< T > & RealType< T > >
	ImarisDataset< T > createDataset( final T type,
			final int sx, final int sy, final int sz, final int sc, final int st,
			final ImarisDatasetOptions options )
	{
		return createDataset( type, new DatasetDimensions( sx, sy, sz, sc, st ), options );
	}

	@Override
	public < T extends NativeType< T > & RealType< T > >
	ImarisDataset< T > createDataset( final T type,
			final DatasetDimensions dims,
			final ImarisDatasetOptions options )
	{
		try
		{
			final IDataSetPrx dataset = ImarisUtils.createDataset( iApplicationPrx, TypeUtils.imarisTypeFor( type ), dims );
			final boolean isEmptyDataset = true;
			return new ImarisDataset<>( getContext(), dataset, dims, isEmptyDataset, options );
		}
		catch ( final Error error )
		{
			throw new RuntimeException( error ); // TODO: revise exception handling
		}
	}
}
