package tpietzsch;

import Imaris.IDataSetPrx;
import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import bdv.util.BdvStackSource;
import com.bitplane.xt.ImarisDataset;
import com.bitplane.xt.ImarisService;
import java.util.Arrays;
import org.scijava.Context;

import static com.bitplane.xt.ImarisAxesOptions.Axis.C;
import static com.bitplane.xt.ImarisAxesOptions.Axis.T;
import static com.bitplane.xt.ImarisAxesOptions.Axis.X;
import static com.bitplane.xt.ImarisAxesOptions.Axis.Y;
import static com.bitplane.xt.ImarisAxesOptions.Axis.Z;
import static com.bitplane.xt.ImarisDatasetOptions.options;

public class ExampleIncludeAxes
{
	public static void main( String[] args )
	{
		final Context context = new Context();
		final ImarisService imaris = context.getService( ImarisService.class );
		final ImarisDataset< ? > dataset = imaris.getApplication().getDataset( options()
				.includeAxes( X, Y, Z, C, T ) );

		System.out.println( "dataset.getImage().dimensionsAsLongArray() = " + Arrays.toString( dataset.getImage().dimensionsAsLongArray() ) );

//		final BdvStackSource< ? > source = BdvFunctions.show( dataset.getSources(), dataset.numTimepoints(), Bdv.options() );
//		source.getBdvHandle().getCacheControls().addCacheControl( dataset.getSharedQueue() );
	}
}
