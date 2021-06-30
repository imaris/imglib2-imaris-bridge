package com.bitplane.xt;

import Imaris.IApplicationPrx;
import net.imagej.Dataset;

public interface ImarisApplication
{
	/**
	 * Get the underlying {@code IApplication} ICE proxy.
	 */
	IApplicationPrx getIApplicationPrx();

	/**
	 * Get the object ID of the underlying {@code IApplication} ICE proxy.
	 */
	int getApplicationID();

	/**
	 * Get the current Imaris image as an ImageJ {@code net.imagej.Dataset}.
	 */
	Dataset getDataset();

	/**
	 * Get the current Imaris image as an {@code ImarisDataset}.
	 */
	ImarisDataset< ? > getImarisDataset();
}
