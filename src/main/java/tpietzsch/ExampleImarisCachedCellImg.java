package tpietzsch;

import Imaris.Error;
import Imaris.IApplicationPrx;
import Imaris.IDataSetPrx;
import Imaris.IFactoryPrx;
import com.bitplane.xt.ImarisCachedCellImg;
import com.bitplane.xt.ImarisCachedCellImgFactory;
import com.bitplane.xt.ImarisCachedCellImgOptions;
import com.bitplane.xt.ImarisService;
import ij.IJ;
import ij.ImagePlus;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.parallel.TaskExecutors;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.util.BenchmarkHelper;
import net.imglib2.util.Util;
import net.imglib2.view.Views;
import org.scijava.Context;

import static Imaris.tType.eTypeUInt8;

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
		imaris.app().SetImage( 0, dataset );
	}
}
