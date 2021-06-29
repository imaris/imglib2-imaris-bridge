package com.bitplane.xt;

import org.scijava.service.SciJavaService;

public interface ImarisService extends SciJavaService
{
	ImarisApplication app();

	void disconnect();
}
