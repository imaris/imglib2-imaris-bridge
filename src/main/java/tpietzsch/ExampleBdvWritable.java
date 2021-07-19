package tpietzsch;

import Imaris.IDataSetPrx;
import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import bdv.util.BdvStackSource;
import com.bitplane.xt.ImarisDataset;
import com.bitplane.xt.ImarisService;
import java.util.Arrays;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.view.Views;
import org.scijava.Context;

public class ExampleBdvWritable
{
	public static void main( String[] args )
	{
		final Context context = new Context();
		final ImarisService imaris = context.getService( ImarisService.class );
		final ImarisDataset< ? > dataset = imaris.getApplication().getImarisDataset();

		final IDataSetPrx ds = dataset.getIDataSetPrx();
		final int[][] pyramidSizes = ds.GetPyramidSizes();
		final int[][] pyramidBlockSizes = ds.GetPyramidBlockSizes();
		final int numResolutions = pyramidSizes.length;

		System.out.println( "numResolutions = " + numResolutions );
		System.out.println( "pyramidSizes = " + Arrays.deepToString( pyramidSizes ) );
		System.out.println( "pyramidBlockSizes = " + Arrays.deepToString( pyramidBlockSizes ) );

		final BdvStackSource< ? > source = BdvFunctions.show( dataset.getSources(), dataset.numTimepoints(), Bdv.options() );
		source.getBdvHandle().getCacheControls().addCacheControl( dataset.getSharedQueue() );

		System.out.println( "type = " + dataset.getType().getClass() );
		final Img< UnsignedByteType > img = ( Img< UnsignedByteType > ) dataset.getImage();
		final long[] dims = img.dimensionsAsLongArray();
		final long[] min = new long[ dims.length ];
		final long[] max = new long[ dims.length ];
		for ( int d = 0; d < dims.length; ++d )
		{
			min[ d ] = dims[ d ] / 2 - dims[ d ] / 8;
			max[ d ] = dims[ d ] / 2 + dims[ d ] / 8;
		}
		Views.interval( img, min, max ).forEach( t -> t.set( 200 ) );
		System.out.println("done");
	}
}
