package tpietzsch;

import bdv.util.BdvFunctions;
import bdv.util.BdvOptions;
import bdv.util.BdvStackSource;
import bdv.viewer.SourceAndConverter;
import com.bitplane.xt.ImarisDataset;
import com.bitplane.xt.ImarisService;
import org.scijava.Context;

public class ExampleBdv
{
	public static void main( String[] args )
	{
		final Context context = new Context();
		final ImarisService imaris = context.getService( ImarisService.class );
		final ImarisDataset< ? > dataset = imaris.getImarisDataset();

		final BdvOptions options = BdvOptions.options();
		for ( SourceAndConverter< ? > source : dataset.getSources() )
		{
			final BdvStackSource< ? > show = BdvFunctions.show( source, dataset.numTimepoints(), options );
			show.getBdvHandle().getCacheControls().addCacheControl( dataset.getSharedQueue() );
			options.addTo( show );
		}
	}
}
