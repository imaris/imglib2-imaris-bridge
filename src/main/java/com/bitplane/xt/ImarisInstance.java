package com.bitplane.xt;

import Imaris.IApplicationPrx;
import net.imagej.Dataset;
import org.scijava.service.SciJavaService;

public interface ImarisInstance
{
	IApplicationPrx app();

	void disconnect();

	/**
	 * Get the current Imaris image as an {@code ImarisDataset}.
	 */
	ImarisDataset< ? > getImarisDataset();
}
