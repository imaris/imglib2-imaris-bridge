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
import net.imagej.DatasetService;
import org.scijava.Priority;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.service.AbstractService;
import org.scijava.service.Service;

import static Imaris.IApplicationPrxHelper.checkedCast;

/**
 * TODO
 *
 * @author Tobias Pietzsch
 */
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
