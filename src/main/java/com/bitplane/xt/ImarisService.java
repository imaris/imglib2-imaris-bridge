package com.bitplane.xt;

import Imaris.IApplicationPrx;
import net.imagej.Dataset;
import org.scijava.service.SciJavaService;

public interface ImarisService extends SciJavaService
{
	IApplicationPrx app();

	void disconnect();

	Dataset getDataset();
}
