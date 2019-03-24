package tpietzsch;

import Imaris.Error;
import Imaris.IApplicationPrx;
import Imaris.IDataSetPrx;
import Imaris.tType;
import bdv.tools.brightness.ConverterSetup;
import bdv.util.BdvFunctions;
import bdv.util.BdvOptions;
import bdv.util.BdvStackSource;
import bdv.util.volatiles.SharedQueue;
import bdv.util.volatiles.VolatileViews;
import java.util.List;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.Volatile;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import org.scijava.Context;

import static bdv.util.AxisOrder.XYZCT;

public class Playground
{

	public static void main( String[] args ) throws Error
	{
		Context context = new Context();
		final ImarisService imaris = context.getService( ImarisService.class );
		final IApplicationPrx application = imaris.app();

		System.out.println( "application = " + application );
		System.out.println( "application.GetCurrentFileName() = " + application.GetCurrentFileName() );
		System.out.println( "application.GetCurrentFileName( null ) = " + application.GetCurrentFileName( null ) );;

		final IDataSetPrx dataset = application.GetDataSet();
		System.out.println( "dataset = " + dataset );

		final tType tType = dataset.GetType();
		System.out.println( "type = " + tType );

//		final byte[] bytes = dataset.GetDataVolumeAs1DArrayBytes( 0, 0 );
//		final long[] dims = { dataset.GetSizeX(), dataset.GetSizeY(), dataset.GetSizeZ() };
//		final Img< UnsignedByteType > img = ArrayImgs.unsignedBytes( bytes, dims );

		final ImarisDataset ds = new ImarisDataset( dataset );
		final Img< UnsignedByteType > img = ( Img< UnsignedByteType > ) ds.getImage();
		final SharedQueue queue = new SharedQueue( 120 );
		final RandomAccessibleInterval< Volatile< UnsignedByteType > > vimg = VolatileViews.wrapAsVolatile( img, queue );
		final BdvStackSource< ? > stackSource = BdvFunctions.show( vimg, "imaris", BdvOptions.options().sourceTransform( ds.getCalib() ).axisOrder( XYZCT ) );
		final List< ConverterSetup > channels = stackSource.getConverterSetups();
		for ( int i = 0; i < channels.size(); i++ )
		{
			final ARGBType color = ds.getChannelColor( i );
			System.out.println( "type = " + color );
			channels.get( i ).setColor( color );
		}

		try
		{
			Thread.sleep( 100000 );
		}
		catch ( InterruptedException e )
		{
			e.printStackTrace();
		}
		System.out.println( "shutting down" );
		imaris.shutdown();
	}
}
