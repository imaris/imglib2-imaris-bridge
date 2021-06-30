package com.bitplane.xt;

import Ice.InitializationData;
import Ice.Properties;
import Imaris.Error;
import ImarisServer.IServerPrx;
import ImarisServer.IServerPrxHelper;

class IceClient
{
	private Ice.Communicator mCommunicator = null;
	private IServerPrx mServer = null;

	public IceClient( String aName, String aEndPoints, int aServerTimeoutMillisec ) throws Error
	{
		try
		{
			InitializationData vData = new InitializationData();
			Properties vProperties = Ice.Util.createProperties();
			vProperties.setProperty( "Ice.Default.EncodingVersion", "1.0" );
			vProperties.setProperty( "Ice.MessageSizeMax", "1000000000" );
			String vRetryIntervals = "0";
			for ( int vIndex = 0; vIndex < 5; vIndex++ )
			{
				vRetryIntervals += " " + ( aServerTimeoutMillisec * ( vIndex * 2 + 1 ) / 25 );
			}
			vProperties.setProperty( "Ice.RetryIntervals", vRetryIntervals );
			vData.properties = vProperties;
			mCommunicator = Ice.Util.initialize( vData );
			Ice.ObjectPrx vObject = mCommunicator.stringToProxy( aName + ":" + aEndPoints );
			mServer = IServerPrxHelper.checkedCast( vObject );
		}
		catch ( Exception e )
		{
			throw new Error( e );
		}
	}

	public void Terminate() throws Error
	{
		if ( mCommunicator != null )
		{
			try
			{
				mCommunicator.shutdown();
				mCommunicator.destroy();
			}
			catch ( Exception e )
			{
				throw new Error( e );
			}
		}
	}

	public IServerPrx GetServer()
	{
		return mServer;
	}
}
