package com.bitplane.xt;

import java.util.List;
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

	@Override
	public void disconnect()
	{
		server.disconnect();
	}

	@Override
	public List< ImarisApplication > getApplications()
	{
		server.context = context(); // TODO: pass context some other way
		return server.getApplications();
	}

	@Override
	public ImarisApplication getApplication()
	{
		final List< ImarisApplication > apps = getApplications();
		return apps.isEmpty() ? null : apps.get( 0 );
	}

	@Override
	public ImarisApplication getApplicationByID( int applicationId )
	{
		server.context = context(); // TODO: pass context some other way
		return server.getApplication( applicationId );
	}
}
