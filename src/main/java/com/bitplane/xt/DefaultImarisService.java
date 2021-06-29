package com.bitplane.xt;

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

	private synchronized DefaultImarisApplication imaris()
	{
		if ( imaris == null )
		{
			imaris = new DefaultImarisApplication();
			context().inject( imaris );
		}
		return imaris;
	}

	@Override
	public void disconnect()
	{
		imaris().disconnect();
	}

	@Override
	public ImarisApplication app()
	{
		return imaris();
	}
}
