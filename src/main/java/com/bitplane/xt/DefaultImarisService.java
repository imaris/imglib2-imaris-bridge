package com.bitplane.xt;

import Imaris.Error;
import Imaris.IApplicationPrx;
import ImarisServer.IServerPrx;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.imagej.DatasetService;
import org.scijava.Priority;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.service.AbstractService;
import org.scijava.service.Service;

import static Imaris.IApplicationPrxHelper.checkedCast;

@Plugin( type = Service.class, priority = Priority.LOW )
public class DefaultImarisService extends AbstractService implements ImarisService
{
	@Parameter
	private DatasetService datasetService;

	@Override
	public synchronized void disconnect()
	{
		closeIceClient();
	}

	@Override
	public synchronized List< ImarisApplication > getApplications()
	{
		refreshApplications();
		return unmodifiableApps;
	}

	@Override
	public synchronized ImarisApplication getApplication()
	{
		final List< ImarisApplication > apps = getApplications();
		return apps.isEmpty() ? null : apps.get( 0 );
	}

	@Override
	public synchronized ImarisApplication getApplicationByID( int applicationId )
	{
		refreshApplications();
		return idToApp.get( applicationId );
	}

	//
	// ========================================================================
	//

	private final List< ImarisApplication > apps = new ArrayList<>();
	private final List< ImarisApplication > unmodifiableApps = Collections.unmodifiableList( apps );
	private final Map< Integer, ImarisApplication > idToApp = new HashMap<>();

	private void refreshApplications()
	{
		final Map< Integer, ImarisApplication > existing = new HashMap<>();
		existing.putAll( idToApp );

		apps.clear();
		idToApp.clear();

		final IServerPrx server = getServer();
		final int numObjects = server.GetNumberOfObjects();
		if ( numObjects < 0 )
			throw error( "Server returned invalid number of objects" );

		for ( int i = 0; i < numObjects; i++ )
		{
			final int applicationId = server.GetObjectID( i );
			ImarisApplication app = existing.get( applicationId );
			if ( app == null )
			{
				final IApplicationPrx iApplicationPrx = checkedCast( server.GetObject( applicationId ) );
				app = new DefaultImarisApplication( iApplicationPrx, applicationId );
				context().inject( app );
			}
			apps.add( app );
			idToApp.put( applicationId, app );
		}
	}

	private IceClient mIceClient;

	private IServerPrx getServer()
	{
		try
		{
			if ( mIceClient == null )
				mIceClient = new IceClient( "ImarisServer", "default -p 4029", 1000 );
			return mIceClient.GetServer();
		}
		catch( Error e )
		{
			throw error( "Could not connect to Imaris", e );
		}
	}

	private void closeIceClient()
	{
		if ( mIceClient != null )
		{
			try
			{
				mIceClient.Terminate();
			}
			catch( Error e )
			{
				e.printStackTrace();
			}
			mIceClient = null;
			apps.clear();
			idToApp.clear();
		}
	}

	private RuntimeException error( final String message )
	{
		return error( message, null );
	}

	private RuntimeException error( final String message, final Error error )
	{
		closeIceClient();
		if ( error == null )
			return new RuntimeException( message );
		else
			return new RuntimeException( message, error );
	}
}
