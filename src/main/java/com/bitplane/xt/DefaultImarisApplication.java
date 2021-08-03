package com.bitplane.xt;

import Imaris.Error;
import Imaris.IApplicationPrx;
import Imaris.IDataSetPrx;
import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import org.scijava.AbstractContextual;
import org.scijava.plugin.Parameter;

public class DefaultImarisApplication extends AbstractContextual implements ImarisApplication
{
	@Parameter
	private DatasetService datasetService;

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
	public Dataset getIJDataset()
	{
		try
		{
			final ImarisDataset< ? > ds = getDataset();
			final Dataset ijDataset = datasetService.create( ds.getImgPlus() );
			ijDataset.setName( ds.getName() );
			ijDataset.setRGBMerged( false );
			return ijDataset;
		}
		catch ( final Error error )
		{
			throw new RuntimeException( error ); // TODO: revise exception handling
		}
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
	public ImarisDataset< ? > getImage( final int imageIndex )
	{
		try
		{
			final IDataSetPrx datasetPrx = getIApplicationPrx().GetImage( imageIndex );
			if ( datasetPrx == null )
				return null;
			final ImarisDatasetOptions options = ImarisDatasetOptions.options();
			return new ImarisDataset<>( datasetPrx, options );
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
	ImarisDataset< T > createDataset( final T type, final int sx, final int sy, final int sz, final int sc, final int st )
	{
		try
		{
			final DatasetDimensions dims = new DatasetDimensions( sx, sy, sz, sc, st );
			final IDataSetPrx dataset = ImarisUtils.createDataset( iApplicationPrx, ImarisUtils.imarisTypeFor( type ), dims );
			final boolean writable = true;
			final boolean isEmptyDataset = true;
			final ImarisDatasetOptions options = ImarisDatasetOptions.options();
			return new  ImarisDataset<>( dataset, dims, writable, isEmptyDataset, options );
		}
		catch ( final Error error )
		{
			throw new RuntimeException( error ); // TODO: revise exception handling
		}
	}
}
