package tpietzsch;

import Imaris.Error;
import Imaris.IDataSetPrx;
import com.bitplane.xt.ImarisCachedCellImg;
import com.bitplane.xt.ImarisCachedCellImgFactory;
import com.bitplane.xt.ImarisCachedCellImgOptions;
import com.bitplane.xt.ImarisService;
import ij.IJ;
import ij.ImagePlus;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.util.Util;
import org.scijava.Context;

public class ExampleImarisCachedCellImg
{
	public static void main( String[] args ) throws Error
	{
		Context context = new Context();
		final ImarisService imaris = context.getService( ImarisService.class );

		final String path = "/Users/pietzsch/workspace/data/e002_stack_fused-8bit.tif";

		final ImagePlus imp = IJ.openImage( path );
		final RandomAccessibleInterval< UnsignedByteType > img = ImageJFunctions.wrapReal( imp );

		ImarisCachedCellImgFactory< UnsignedByteType > factory = new ImarisCachedCellImgFactory<>(
				Util.getTypeFromInterval( img ),
				imaris,
				ImarisCachedCellImgOptions.options()
						.cellDimensions( 64 )
						.numIoThreads( 20 ) );

		final ImarisCachedCellImg< UnsignedByteType, ? > imarisImg = factory.create( img );

		LoopBuilder
				.setImages( img, imarisImg )
				.multiThreaded()
				.forEachPixel( ( i, o ) -> o.set( i ) );
		imarisImg.persist();

		final IDataSetPrx dataset = imarisImg.getDataSet();
		imaris.getIApplicationPrx().SetImage( 0, dataset );
	}
}
