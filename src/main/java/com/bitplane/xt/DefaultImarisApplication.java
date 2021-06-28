package com.bitplane.xt;

import Ice.ObjectPrx;
import Imaris.Error;
import Imaris.IApplicationPrx;
import Imaris.IDataSetPrx;
import ImarisServer.IServerPrx;

import static Imaris.IApplicationPrxHelper.checkedCast;

public class DefaultImarisApplication implements ImarisApplication
{
	private int applicationId;

	private IceClient mIceClient;

	private IApplicationPrx app;

	public DefaultImarisApplication()
	{
		this( -1 );
	}

	public DefaultImarisApplication( final int applicationId )
	{
		this.applicationId = applicationId;
	}

	@Override
	public void disconnect()
	{
		closeIceClient();
	}

	@Override
	public IApplicationPrx app()
	{
		if ( app == null )
		{
			final IServerPrx server = getServer();
			final int numObjects = server.GetNumberOfObjects();
			System.out.println( "numObjects = " + numObjects );
			if ( numObjects < 1 )
				throw error();
			if ( numObjects > 1 )
			{
				try
				{
					for ( int i = 0; i < numObjects; i++ )
					{
						final int applicationId = server.GetObjectID(i);
						IApplicationPrx app = checkedCast( server.GetObject( applicationId ) );
						String vDescription = app.GetVersion() + " " + app.GetCurrentFileName();
						System.out.println( applicationId + "vDescription = " + vDescription );
					}
				}
				catch ( final Error error )
				{
					throw error( error );
				}
			}
			if (applicationId == -1 )
				applicationId = server.GetObjectID( 0 );
			final ObjectPrx obj = server.GetObject( applicationId );
			app = checkedCast( obj );
		}
		return app;
	}

	@Override
	public ImarisDataset< ? > getImarisDataset()
	{
		try
		{
			final IDataSetPrx datasetPrx = app().GetDataSet();
			if ( datasetPrx == null )
				throw new RuntimeException( "No dataset is open in Imaris" );
			return new ImarisDataset<>( datasetPrx );
		}
		catch ( final Error error )
		{
			throw error( error );
		}
	}

	private IceClient getIceClient()
	{
		if ( mIceClient == null )
			mIceClient = new IceClient( "ImarisServer", "default -p 4029", 1000 );
		return mIceClient;
	}

	private IServerPrx getServer()
	{
		final IServerPrx server = getIceClient().GetServer();
		if ( server == null )
			throw error();
		return server;
	}

	private void closeIceClient()
	{
		if ( mIceClient != null )
		{
			app = null;
			mIceClient.Terminate();
			mIceClient = null;
		}
	}

	public RuntimeException error()
	{
		return error( null );
	}

	public RuntimeException error( final Error error )
	{
		closeIceClient();
		if ( error == null )
			return new RuntimeException( "Could not connect to Imaris" );
		else
			return new RuntimeException( "Could not connect to Imaris", error );
	}
}
