package tpietzsch;

import net.imagej.ImageJ;
import net.imagej.ImgPlus;

import Imaris.Error;
import Imaris.IApplicationPrx;

public class IJPlayground
{

	public static void main( final String[] args ) throws Error
	{
		final ImageJ ij = new ImageJ();
		ij.ui().showUI();

		final ImarisService imaris = ij.context().getService( ImarisService.class );
		final IApplicationPrx application = imaris.app();

		System.out.println( "application = " + application );
		System.out.println( "application.GetCurrentFileName() = " + application.GetCurrentFileName() );
		System.out.println( "application.GetCurrentFileName( null ) = " + application.GetCurrentFileName( null ) );;

		final ImarisDataset< ? > ds = new ImarisDataset<>( application.GetDataSet() );
		final ImgPlus< ? > imp = ds.getImgPlus();
		ij.ui().show( imp );

		try
		{
			Thread.sleep( 100000 );
		}
		catch ( final InterruptedException e )
		{
			e.printStackTrace();
		}
		System.out.println( "shutting down" );
		imaris.shutdown();
	}
}
