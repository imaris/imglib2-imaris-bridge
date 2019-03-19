package tpietzsch;

import Imaris.IApplicationPrx;
import org.scijava.service.SciJavaService;

public interface ImarisService extends SciJavaService
{
	IApplicationPrx app();

	void shutdown();
}
