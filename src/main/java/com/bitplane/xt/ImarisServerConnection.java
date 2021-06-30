package com.bitplane.xt;

import Imaris.Error;
import Imaris.IApplicationPrx;
import ImarisServer.IServerPrx;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.scijava.Context;

import static Imaris.IApplicationPrxHelper.checkedCast;


class ImarisServerConnection
{
	private String mEndPoints = "default -p 4029";
	private int mServerTimeoutMillisec = 1000;
	private IceClient mIceClient;

	private List< ImarisApplication > apps = new ArrayList<>();
	private List< ImarisApplication > unmodifiableApps = Collections.unmodifiableList( apps );
	private Map< Integer, ImarisApplication > idToApp = new HashMap<>();

	public Context context;

	public synchronized void reconnect()
	{
		closeIceClient();
		getServer();
	}

	public synchronized void disconnect()
	{
		closeIceClient();
	}

	public synchronized List< ImarisApplication > getApplications()
	{
		refreshApplications();
		return unmodifiableApps;
	}

	public synchronized ImarisApplication getApplication( final int applicationId )
	{
		refreshApplications();
		return idToApp.get( applicationId );
	}

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
				context.inject( app );
			}
			apps.add( app );
			idToApp.put( applicationId, app );
		}
	}

	private IServerPrx getServer()
	{
		try
		{
			if ( mIceClient == null )
				mIceClient = new IceClient( "ImarisServer", mEndPoints, mServerTimeoutMillisec );
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
