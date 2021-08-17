package com.bitplane.xt;

import Imaris.Error;
import Imaris.IApplicationPrx;
import Imaris.IDataSetPrx;
import com.bitplane.xt.util.ImarisUtils;
import com.bitplane.xt.util.TypeUtils;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import org.scijava.AbstractContextual;

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
		try
		{
			final DatasetDimensions dims = new DatasetDimensions( sx, sy, sz, sc, st );
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
