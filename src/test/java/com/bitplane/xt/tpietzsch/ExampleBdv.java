package com.bitplane.xt.tpietzsch;

import Imaris.IDataSetPrx;
import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import bdv.util.BdvStackSource;
import com.bitplane.xt.ImarisDataset;
import com.bitplane.xt.ImarisService;
import java.util.Arrays;
import org.scijava.Context;

public class ExampleBdv
{
	public static void main( String[] args )
	{
		final Context context = new Context();
		final ImarisService imaris = context.getService( ImarisService.class );
		final ImarisDataset< ? > dataset = imaris.getApplication().getDataset();

		final IDataSetPrx ds = dataset.getIDataSetPrx();
		final int[][] pyramidSizes = ds.GetPyramidSizes();
		final int[][] pyramidBlockSizes = ds.GetPyramidBlockSizes();
		final int numResolutions = pyramidSizes.length;

		System.out.println( "numResolutions = " + numResolutions );
		System.out.println( "pyramidSizes = " + Arrays.deepToString( pyramidSizes ) );
		System.out.println( "pyramidBlockSizes = " + Arrays.deepToString( pyramidBlockSizes ) );

		final BdvStackSource< ? > source = BdvFunctions.show( dataset.getSources(), dataset.numTimepoints(), Bdv.options() );
		source.getBdvHandle().getCacheControls().addCacheControl( dataset.getSharedQueue() );
	}
}
