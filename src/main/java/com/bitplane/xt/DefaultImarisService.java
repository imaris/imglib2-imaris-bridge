/*-
 * #%L
 * Expose the Imaris XT interface as an ImageJ2 service backed by ImgLib2.
 * %%
 * Copyright (C) 2019 - 2021 Bitplane AG
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package com.bitplane.xt;

import Imaris.Error;
import Imaris.IApplicationPrx;
import ImarisServer.IServerPrx;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.scijava.Priority;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.service.AbstractService;
import org.scijava.service.Service;

import static Imaris.IApplicationPrxHelper.checkedCast;

/**
 * Default implementation of {@code ImarisService} for connecting to the Imaris
 * XT API, and providing access to the running Imaris instance(s).
 *
 * @author Tobias Pietzsch
 */
@Plugin( type = Service.class, priority = Priority.LOW )
public class DefaultImarisService extends AbstractService implements ImarisService
{
	@Override
	public synchronized void disconnect()
	{
		closeIceClient();
	}

	@Override
	public synchronized List< ImarisApplication > getApplications()
	{
		final List< ImarisApplication > apps = new ArrayList<>();

		final IServerPrx server = getServer();
		final int numObjects = server.GetNumberOfObjects();
		for ( int i = 0; i < numObjects; i++ )
		{
			final int applicationId = server.GetObjectID( i );
			final ImarisApplication app = getApplicationByID( applicationId, server );
			if ( app != null )
				apps.add( app );
		}

		return apps;
	}

	@Override
	public synchronized ImarisApplication getApplication()
	{
		final IServerPrx server = getServer();
		final int applicationId = server.GetObjectID( 0 );
		return getApplicationByID( applicationId, server );
	}

	@Override
	public synchronized ImarisApplication getApplicationByID( int applicationId )
	{
		return getApplicationByID( applicationId, getServer() );
	}

	//
	// ========================================================================
	//

	private final Map< Integer, ImarisApplication > idToApp = new HashMap<>();

	private ImarisApplication getApplicationByID( int applicationId, IServerPrx server )
	{
		ImarisApplication app = idToApp.get( applicationId );
		if ( app == null )
		{
			app = initApplication( applicationId, server );
			if ( app != null )
				idToApp.put( applicationId, app );
		}
		return app;
	}

	private ImarisApplication initApplication( int applicationId, IServerPrx server )
	{
		try
		{
			final IApplicationPrx iApplicationPrx = checkedCast( server.GetObject( applicationId ) );
			final ImarisApplication app = new DefaultImarisApplication( iApplicationPrx, applicationId );
			context().inject( app );
			return app;
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return null;
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
