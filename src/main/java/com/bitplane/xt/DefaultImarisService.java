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

	private ImarisServerConnection server = new ImarisServerConnection();

	private synchronized ImarisApplication imaris()
	{
		server.context = context();
		return server.getApplications().get( 0 );
	}

	@Override
	public void disconnect()
	{
		server.disconnect();
	}

	@Override
	public ImarisApplication app()
	{
		return imaris();
	}
}
