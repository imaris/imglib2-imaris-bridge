package tpietzsch;

import Imaris.Error;
import Imaris.IApplicationPrx;
import Imaris.IDataSetPrx;
import Imaris.tType;
import bdv.tools.brightness.ConverterSetup;
import bdv.util.BdvFunctions;
import bdv.util.BdvOptions;
import bdv.util.BdvStackSource;
import bdv.util.volatiles.VolatileViews;
import java.util.List;
import net.imagej.ImageJ;
import net.imagej.ImgPlus;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import org.scijava.Context;

import static bdv.util.AxisOrder.XYZCT;

public class IJPlayground
{

	public static void main( String[] args ) throws Error
	{
		final ImageJ ij = new ImageJ();
		ij.ui().showUI();

		final ImarisService imaris = ij.context().getService( ImarisService.class );
		final IApplicationPrx application = imaris.app();

		System.out.println( "application = " + application );
		System.out.println( "application.GetCurrentFileName() = " + application.GetCurrentFileName() );
		System.out.println( "application.GetCurrentFileName( null ) = " + application.GetCurrentFileName( null ) );;

		final ImarisDataset ds = new ImarisDataset( application.GetDataSet() );
		final ImgPlus< ? > imp = ds.getImgPlus();
		ij.ui().show( imp );

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
