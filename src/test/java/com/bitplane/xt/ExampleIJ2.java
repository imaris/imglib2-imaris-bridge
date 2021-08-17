package com.bitplane.xt;

import Imaris.Error;
import net.imagej.ImageJ;

public class ExampleIJ2
{
	public static void main( final String[] args ) throws Error
	{
		final ImageJ ij = new ImageJ();
		final ImarisService imaris = ij.get( ImarisService.class );
		final ImarisDataset< ? > dataset = imaris.getApplication().getDataset();

		ij.ui().showUI();
		ij.ui().show( dataset.getIJDataset() );
	}
}
