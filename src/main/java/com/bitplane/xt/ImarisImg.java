package com.bitplane.xt;

import Imaris.IDataSetPrx;

public interface ImarisImg
{
	// TODO: may be this should return a wrapper instead of IDataSetPrx
	IDataSetPrx getDataSet();

	/**
	 * Persist all changes back to Imaris
	 */
	void persist();

	// TODO:
	//   Future<Void> populateAndPersist();
	//   This would be great to have if ImarisImg is the endpoint of a computation that should be fully realized

	ImarisApplication imaris();
}
