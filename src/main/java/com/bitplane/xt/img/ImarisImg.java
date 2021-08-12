package com.bitplane.xt.img;

import Imaris.IDataSetPrx;
import com.bitplane.xt.ImarisApplication;

/**
 * Implemented by the various ImarisCachedImgs. Provides access to the
 * underlying ICE proxies.
 */
public interface ImarisImg
{
	/**
	 * Get the underlying {@code IDataSet} ICE proxy.
	 */
	IDataSetPrx getIDataSetPrx();

	/**
	 * Persist all changes back to Imaris
	 */
	void persist();

	// TODO:
	//   Future<Void> populateAndPersist();
	//   This would be great to have if ImarisImg is the endpoint of a computation that should be fully realized

	/**
	 * Get the {@code ImarisApplication} which holds (the backing cache for) this image.
	 */
	ImarisApplication getApplication();
}
