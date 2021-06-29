package com.bitplane.xt;

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

	private DefaultImarisApplication imaris;

	private synchronized ImarisApplication imaris()
	{
		if ( imaris == null )
		{
			imaris = new DefaultImarisApplication();
			context().inject( imaris );
		}
		return imaris;
	}

	@Override
	public IApplicationPrx getIApplicationPrx()
	{
		return imaris().getIApplicationPrx();
	}

	@Override
	public void disconnect()
	{
		imaris().disconnect();
	}

	@Override
	public Dataset getDataset()
	{
		return imaris().getDataset();
	}

	@Override
	public ImarisDataset< ? > getImarisDataset()
	{
		return imaris().getImarisDataset();
	}
}
