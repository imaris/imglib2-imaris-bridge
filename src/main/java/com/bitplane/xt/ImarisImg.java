package com.bitplane.xt;

import Imaris.IDataSetPrx;

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
