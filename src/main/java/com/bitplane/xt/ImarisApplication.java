package com.bitplane.xt;

import Imaris.IApplicationPrx;
import net.imagej.Dataset;
import org.scijava.service.SciJavaService;

public interface ImarisApplication
{
	/**
	 * Get the underlying {@code IApplication} ICE proxy.
	 */
	IApplicationPrx getIApplicationPrx();

	void disconnect();

	/**
	 * Get the current Imaris image as an {@code ImarisDataset}.
	 */
	ImarisDataset< ? > getImarisDataset();
}
