package tpietzsch;

import Imaris.Error;
import Imaris.IDataSetPrx;
import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import bdv.util.BdvStackSource;
import com.bitplane.xt.ImarisDataset;
import com.bitplane.xt.ImarisService;
import java.util.Arrays;
import mpicbg.spim.data.sequence.FinalVoxelDimensions;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.FinalInterval;
import org.scijava.Context;

public class ExampleSetCalibration
{
	public static void main( String[] args ) throws Error, InterruptedException
	{
		final Context context = new Context();
		final ImarisService imaris = context.getService( ImarisService.class );
		final ImarisDataset< ? > dataset = imaris.getApplication().getDataset();

		final VoxelDimensions voxelDimensions = dataset.getVoxelDimensions();
		System.out.println( "dataset.getVoxelDimensions() = " + voxelDimensions );

		final VoxelDimensions original = new FinalVoxelDimensions( dataset.getVoxelDimensions() );
		final VoxelDimensions stretched = new FinalVoxelDimensions(
				original.unit(),
				original.dimension( 0 ),
				original.dimension( 1 ),
				original.dimension( 2 ) * 1.2 );

		final BdvStackSource< ? > source = BdvFunctions.show( dataset.getSources(), dataset.numTimepoints(), Bdv.options() );
		source.getBdvHandle().getCacheControls().addCacheControl( dataset.getSharedQueue() );

		while( true )
		{
			dataset.setCalibration( stretched );
			source.getBdvHandle().getViewerPanel().requestRepaint();
			Thread.sleep( 1000 );

			dataset.setCalibration( original );
			source.getBdvHandle().getViewerPanel().requestRepaint();
			Thread.sleep( 1000 );
		}
	}
}
