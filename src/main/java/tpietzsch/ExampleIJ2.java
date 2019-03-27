package tpietzsch;

import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.ImgPlus;

import Imaris.Error;
import Imaris.IApplicationPrx;

public class ExampleIJ2
{
	public static void main( final String[] args ) throws Error
	{
		final ImageJ ij = new ImageJ();
		ij.ui().showUI();

		final ImarisService imaris = ij.get( ImarisService.class );
		final Dataset dataset = imaris.getDataset();
		ij.ui().show( dataset );
	}
}
