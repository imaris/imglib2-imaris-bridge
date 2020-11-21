package tpietzsch;

import Imaris.Error;
import bdv.util.BdvFunctions;
import bdv.util.volatiles.VolatileViews;
import com.bitplane.xt.ImarisCachedCellImg;
import com.bitplane.xt.ImarisCachedCellImgFactory;
import com.bitplane.xt.ImarisCachedCellImgOptions;
import com.bitplane.xt.ImarisService;
import ij.IJ;
import ij.ImagePlus;
import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.cache.img.CellLoader;
import net.imglib2.cache.img.SingleCellArrayImg;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;
import org.scijava.Context;

public class ExampleCellLoader
{
	public static void main( String[] args ) throws Error
	{
		Context context = new Context();
		final ImarisService imaris = context.getService( ImarisService.class );

		final String path = "/Users/pietzsch/workspace/data/e002_stack_fused-8bit.tif";
		final ImagePlus imp = IJ.openImage( path );
		final RandomAccessibleInterval< UnsignedByteType > img = ImageJFunctions.wrapReal( imp );

		final CellLoader< UnsignedByteType > loader = new CellLoader< UnsignedByteType >()
		{
			@Override
			public void load( final SingleCellArrayImg< UnsignedByteType, ? > cell ) throws Exception
			{
				System.out.println( "cell = " + cell );
				final Cursor< UnsignedByteType > out = cell.cursor();
				final Cursor< UnsignedByteType > in = Views.interval( img, cell ).cursor();
				while ( out.hasNext() )
					out.next().set( in.next() );
			}
		};

		ImarisCachedCellImgFactory< UnsignedByteType > factory = new ImarisCachedCellImgFactory<>(
				Util.getTypeFromInterval( img ),
				imaris,
				ImarisCachedCellImgOptions.options()
						.initializeCellsAsDirty( true )
						.cellDimensions( 64 )
						.numIoThreads( 20 ) );

		final ImarisCachedCellImg< UnsignedByteType, ? > imarisImg = factory.create( img, loader );

		BdvFunctions.show( VolatileViews.wrapAsVolatile( imarisImg ), "imarisImg" );
	}
}
