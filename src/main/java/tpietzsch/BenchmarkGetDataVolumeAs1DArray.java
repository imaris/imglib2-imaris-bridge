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

public class BenchmarkGetDataVolumeAs1DArray
{
	public static void main( String[] args ) throws Error
	{
		final IApplicationPrx app = getApplication();
		final IDataSetPrx dataset = app.GetDataSet();
		final long[] dims = { dataset.GetSizeX(), dataset.GetSizeY(), dataset.GetSizeZ() };

		while ( true )
		{
			System.out.println( "Benchmark GetDataVolumeAs1DArrayBytes" );
			System.out.println( "Receiving " + 10 * Intervals.numElements( dims ) + " bytes per iteration" );
			BenchmarkHelper.benchmarkAndPrint( 10, false, () -> {
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

			System.out.println( "Benchmark GetDataSubVolumeAs1DArrayBytes" );
			System.out.println( "Receiving " + ( 100 * 32 * 32 * 32 ) + " bytes per iteration" );
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
	}

	static IApplicationPrx getApplication()
	{
		final IceClient client = new IceClient( "ImarisServer", "default -p 4029", 1000 );
		final IServerPrx server = client.GetServer();
		if ( server.GetNumberOfObjects() < 1 )
			throw new IllegalStateException();
		final ObjectPrx obj = server.GetObject( server.GetObjectID( 0 ) );
		final IApplicationPrx app = checkedCast( obj );
		return app;
	}
}
