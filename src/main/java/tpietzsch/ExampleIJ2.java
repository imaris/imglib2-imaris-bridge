package tpietzsch;

import com.bitplane.xt.ImarisService;
import net.imagej.Dataset;
import net.imagej.ImageJ;

import Imaris.Error;

public class ExampleIJ2
{
	public static void main( final String[] args ) throws Error
	{
		final ImageJ ij = new ImageJ();
		ij.ui().showUI();

		final ImarisService imaris = ij.get( ImarisService.class );
		final Dataset dataset = imaris.getApplication().getDataset();
		ij.ui().show( dataset );
	}
}
