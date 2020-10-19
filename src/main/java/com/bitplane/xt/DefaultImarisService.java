package com.bitplane.xt;

import Ice.ObjectPrx;
import Imaris.Error;
import Imaris.IApplicationPrx;
import Imaris.IDataSetPrx;
import ImarisServer.IServerPrx;
import net.imagej.Dataset;
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

	private IceClient mIceClient;

	private IApplicationPrx app;

	@Override
	public IApplicationPrx app()
	{
		if ( app == null )
		{
			final IServerPrx server = getServer();
			if ( server.GetNumberOfObjects() < 1 )
				throw error();
			final int id = 0;
			final ObjectPrx obj = server.GetObject( server.GetObjectID( id ) );
			app = checkedCast( obj );
		}
		return app;
	}

	@Override
	public void disconnect()
	{
		closeIceClient();
	}

	@Override
	public Dataset getDataset()
	{
		try
		{
			final ImarisDataset< ? > ds = getImarisDataset();
			final Dataset ijDataset = datasetService.create( ds.getImgPlus() );
			ijDataset.setName( ds.getName() );
			return ijDataset;
		}
		catch ( final Error error )
		{
			throw error( error );
		}
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

	private RuntimeException error()
	{
		return error( null );
	}

	private RuntimeException error( final Error error )
	{
		closeIceClient();
		if ( error == null )
			return new RuntimeException( "Could not connect to Imaris" );
		else
			return new RuntimeException( "Could not connect to Imaris", error );
	}
}
