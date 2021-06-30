package com.bitplane.xt;

import Imaris.Error;
import Imaris.IApplicationPrx;
import Imaris.IDataSetPrx;
import net.imagej.Dataset;
import net.imagej.DatasetService;
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
	public Dataset getDataset()
	{
		try
		{
			final ImarisDataset< ? > ds = getImarisDataset();
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
	public ImarisDataset< ? > getImarisDataset()
	{
		try
		{
			final IDataSetPrx datasetPrx = getIApplicationPrx().GetDataSet();
			if ( datasetPrx == null )
				throw new RuntimeException( "No dataset is open in Imaris" );
			return new ImarisDataset<>( datasetPrx );
		}
		catch ( final Error error )
		{
			throw new RuntimeException( error ); // TODO: revise exception handling
		}
	}
}
