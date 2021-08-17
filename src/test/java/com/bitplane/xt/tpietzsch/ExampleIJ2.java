package com.bitplane.xt.tpietzsch;

import com.bitplane.xt.ImarisDataset;
import com.bitplane.xt.ImarisService;
import net.imagej.ImageJ;

import Imaris.Error;

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
