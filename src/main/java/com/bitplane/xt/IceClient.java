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

import Ice.InitializationData;
import Ice.Properties;
import Imaris.Error;
import ImarisServer.IServerPrx;
import ImarisServer.IServerPrxHelper;

/**
 * TODO
 *
 * @author Igor Beati
 * @author Tobias Pietzsch
 */
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
