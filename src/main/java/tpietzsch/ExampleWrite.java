package tpietzsch;

import Imaris.Error;
import Imaris.IApplicationPrx;
import Imaris.IDataSetPrx;
import Imaris.IFactoryPrx;
import com.bitplane.xt.ImarisService;
import ij.IJ;
import ij.ImagePlus;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.view.Views;
import org.scijava.Context;

import static Imaris.tType.eTypeUInt8;

public class ExampleWrite
{
	public static void main( String[] args ) throws Error
	{
		Context context = new Context();
		final ImarisService imaris = context.getService( ImarisService.class );

		final String path = "/Users/pietzsch/workspace/data/e002_stack_fused-8bit.tif";

		final ImagePlus imp = IJ.openImage( path );
		final long[] min = new long[ 3 ];
		final long[] max = new long[] { imp.getWidth() - 1, imp.getHeight() - 1, imp.getNSlices() - 1 };
		final RandomAccessibleInterval< UnsignedByteType > rai = Views.interval( ImageJFunctions.wrapByte( imp ), min, max );
		System.out.println( "imp = " + imp );
		System.out.println( "imp.getWidth(), imp.getHeight(), imp.getNSlices() = " + imp.getWidth() + ", " + imp.getHeight() + ", " + imp.getNSlices() );
		System.out.println( "ImageJFunctions.wrapReal( imp ) = " + ImageJFunctions.wrapReal( imp ) );
		final int sx = ( int ) rai.dimension( 0 );
		final int sy = ( int ) rai.dimension( 1 );
		final int sz = ( int ) rai.dimension( 2 );
		System.out.println( "sx, sy, sz = " + sx + ", " + sy + ", " + sz );



		final IApplicationPrx app = imaris.getIApplicationPrx();
		final IFactoryPrx factory = app.GetFactory();
		final IDataSetPrx dataset = factory.CreateDataSet();

		dataset.Create( eTypeUInt8, sx, sy, sz, 1, 1 );

		dataset.SetExtendMinX( 0 );
		dataset.SetExtendMaxX( sx );
		dataset.SetExtendMinY( 0 );
		dataset.SetExtendMaxY( sy );
		dataset.SetExtendMinZ( 0 );
		dataset.SetExtendMaxZ( sz );

		final byte[] data = new byte[ sx * sy * sz ];
		LoopBuilder.setImages( rai, ArrayImgs.unsignedBytes( data, sx, sy, sz ) ).forEachPixel( ( i, o ) -> o.set( i ) );
		dataset.SetDataVolumeAs1DArrayBytes( data, 0, 0 );

		app.SetImage( 0, dataset );
	}
}
