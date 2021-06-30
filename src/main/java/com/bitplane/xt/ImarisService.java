package com.bitplane.xt;

import java.util.List;
import org.scijava.service.SciJavaService;

public interface ImarisService extends SciJavaService
{
	/**
	 * Get list of all running Imaris instances.
	 */
	List< ImarisApplication > getApplications();

	/**
	 * Get the first running Imaris instance, or {@code null} if there are no
	 * running instances.
	 */
	ImarisApplication getApplication();

	/**
	 * Get the running Imaris instance with the specified ICE object ID.
	 */
	ImarisApplication getApplicationByID( int applicationId );

	/**
	 * Disconnect from the ICE server. Using other methods, e.g. {@link
	 * #getApplication()}, after this will re-connect.
	 */
	void disconnect();
}
