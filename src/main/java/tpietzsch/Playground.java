package tpietzsch;

import Ice.ObjectPrx;
import Imaris.Error;
import Imaris.IApplicationPrx;
import Imaris.IDataSetPrx;
import Imaris.tType;
import ImarisServer.IServerPrx;
import com.bitplane.xt.BPImarisLib;

import static Imaris.IApplicationPrxHelper.*;

public class Playground
{
	private final BPImarisLib lib;

	private final IServerPrx server;

	public Playground()
	{
		lib = new BPImarisLib();
		server = lib.GetServer();
	}

	final IApplicationPrx getApplication()
	{
		if ( server.GetNumberOfObjects() < 1 )
			throw new IllegalStateException();
		final int id = 0;
		final ObjectPrx obj = server.GetObject( id );
		final IApplicationPrx app = checkedCast( obj );
		return app;
	}

	void disconnect()
	{
		lib.Disconnect();
	}

	public static void main( String[] args ) throws Error
	{
		final Playground pg = new Playground();
		final IApplicationPrx application = pg.getApplication();
		System.out.println( "application = " + application );

		System.out.println( "application.GetCurrentFileName() = " + application.GetCurrentFileName() );
		System.out.println( "application.GetCurrentFileName( null ) = " + application.GetCurrentFileName( null ) );;

		final IDataSetPrx dataset = application.GetDataSet();
		System.out.println( "dataset = " + dataset );

		final tType tType = dataset.GetType();
		System.out.println( "type = " + tType );

		dataset.GetDataVolumeAs1DArrayBytes(  )

		pg.disconnect();
	}
}
