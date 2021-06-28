package com.bitplane.xt;

import Imaris.Error;
import Imaris.IApplicationPrx;
import net.imagej.Dataset;
import net.imagej.DatasetService;
import org.scijava.Priority;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.service.AbstractService;
import org.scijava.service.Service;

@Plugin( type = Service.class, priority = Priority.LOW )
public class DefaultImarisService extends AbstractService implements ImarisService
{
	@Parameter
	private DatasetService datasetService;

	private DefaultImarisInstance imaris = new DefaultImarisInstance();

	@Override
	public IApplicationPrx app()
	{
		return imaris.app();
	}

	@Override
	public void disconnect()
	{
		imaris.disconnect();
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
			throw imaris.error( error );
		}
	}

	@Override
	public ImarisDataset< ? > getImarisDataset()
	{
		return imaris.getImarisDataset();
	}
}
