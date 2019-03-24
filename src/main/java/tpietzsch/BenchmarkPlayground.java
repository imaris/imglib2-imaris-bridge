package tpietzsch;

import Ice.ObjectPrx;
import Imaris.Error;
import Imaris.IApplicationPrx;
import Imaris.IDataSetPrx;
import ImarisServer.IServerPrx;
import com.bitplane.xt.IceClient;
import net.imglib2.util.BenchmarkHelper;
import net.imglib2.util.Intervals;

import static Imaris.IApplicationPrxHelper.checkedCast;

public class BenchmarkPlayground
{
	private final IServerPrx server;

	private final IceClient mIceClient;

	public BenchmarkPlayground()
	{
		String mEndPoints = "default -p 4029";
		mIceClient = new IceClient( "ImarisServer", mEndPoints, 1000 );
		server = mIceClient.GetServer();
	}

	final IApplicationPrx getApplication()
	{
		System.out.println( "GetNumberOfObjects() = " + server.GetNumberOfObjects() );
		if ( server.GetNumberOfObjects() < 1 )
			throw new IllegalStateException();
		final int id = 0;
		final ObjectPrx obj = server.GetObject( server.GetObjectID( id ) );
		final IApplicationPrx app = checkedCast( obj );
		return app;
	}

	void disconnect()
	{
		mIceClient.Terminate();
	}

	public static void main( String[] args ) throws Error
	{
		final BenchmarkPlayground pg = new BenchmarkPlayground();
		final IApplicationPrx application = pg.getApplication();
		System.out.println( "application = " + application );
		System.out.println( "application.GetCurrentFileName() = " + application.GetCurrentFileName() );

		final IDataSetPrx dataset = application.GetDataSet();
		final long[] dims = { dataset.GetSizeX(), dataset.GetSizeY(), dataset.GetSizeZ() };

//		final byte[] bytes = dataset.GetDataVolumeAs1DArrayBytes( 0, 0 );
//		final long[] dims = { dataset.GetSizeX(), dataset.GetSizeY(), dataset.GetSizeZ() };
//		final Img< UnsignedByteType > img = ArrayImgs.unsignedBytes( bytes, dims );

		for ( int j = 0; j < 10000; ++j )
		{
			System.out.println( 10 * Intervals.numElements( dims ) + " bytes" );
			BenchmarkHelper.benchmarkAndPrint( 10, true, () -> {
				try
				{
					for ( int i = 0; i < 10; ++i )
						dataset.GetDataVolumeAs1DArrayBytes( 0, i );
				}
				catch ( Error error )
				{
					error.printStackTrace();
				}
			} );

			System.out.println( ( 100 * 32 * 32 * 32 ) + " bytes" );
			BenchmarkHelper.benchmarkAndPrint( 10, false, () -> {
				try
				{
					for ( int i = 0; i < 100; ++i )
						dataset.GetDataSubVolumeAs1DArrayBytes( 0, 0, 0, 0, i % 10, 32, 32, 32 );
				}
				catch ( Error error )
				{
					error.printStackTrace();
				}
			} );
		}
		pg.disconnect();
	}
}
