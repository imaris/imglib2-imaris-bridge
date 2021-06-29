package tpietzsch;

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
		final Context context = new Context();
		final ImarisService imaris = context.getService( ImarisService.class );
		final ImarisDataset< ? > dataset = imaris.app().getImarisDataset();

		final BdvStackSource< ? > source = BdvFunctions.show( dataset.getSources(), dataset.numTimepoints(), Bdv.options() );
		source.getBdvHandle().getCacheControls().addCacheControl( dataset.getSharedQueue() );
	}
}
