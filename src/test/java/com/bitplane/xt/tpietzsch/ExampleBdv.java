package com.bitplane.xt.tpietzsch;

import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import bdv.util.BdvStackSource;
import com.bitplane.xt.ImarisDataset;
import com.bitplane.xt.ImarisService;
import org.scijava.Context;

public class ExampleBdv
{
	public static void main( String[] args )
	{
		/*
		 * Create a SciJava context, and obtain the ImarisService instance.
		 *
		 * Note that, when you run out of Fiji, the context is usually already
		 * available. When writing plugins or scripts, you just use a @Parameter
		 * annotation to get the ImarisService.
		 */
		final Context context = new Context();
		final ImarisService imaris = context.getService( ImarisService.class );

		/*
		 * Get the currently open dataset from the first (and typically only)
		 * Imaris application.
		 */
		final ImarisDataset< ? > dataset = imaris.getApplication().getDataset();

		/*
		 * Show the multiresolution version in BigDataViewer.
		 * TODO:
		 *   This should be easier!
		 *   Add support to vistools to do this as just
		 * 		BdvFunctions.show( dataset.asBdvSource() );
		 *   or similar.
		 */
		final BdvStackSource< ? > source = BdvFunctions.show(
				dataset.getSources(),
				dataset.numTimepoints(),
				Bdv.options() );

		source.getBdvHandle().getCacheControls().addCacheControl(
				dataset.getSharedQueue() );
	}
}
