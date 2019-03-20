package tpietzsch;

import Ice.ObjectPrx;
import Imaris.IApplicationPrx;
import ImarisServer.IServerPrx;
import com.bitplane.xt.BPImarisLib;
import org.scijava.Priority;
import org.scijava.plugin.Plugin;
import org.scijava.service.AbstractService;
import org.scijava.service.Service;

import static Imaris.IApplicationPrxHelper.checkedCast;

@Plugin( type = Service.class, priority = Priority.LOW )
public class DefaultImarisService extends AbstractService implements ImarisService
{
	private BPImarisLib lib;

	private IServerPrx server;

	@Override
	public IApplicationPrx app()
	{
		checkLibInitialized();
		if ( server.GetNumberOfObjects() < 1 )
			throw new IllegalStateException();
		final int id = 0;
		final ObjectPrx obj = server.GetObject( server.GetObjectID( id ) );
		final IApplicationPrx app = checkedCast( obj );
		return app;
	}

	@Override
	public void shutdown()
	{
		lib.Disconnect();
		lib = null;
	}

	private void checkLibInitialized()
	{
		if ( lib == null )
		{
			lib = new BPImarisLib();
			server = lib.GetServer();
			if ( server == null )
				shutdown();
		}
	}
}
