package tpietzsch;

import Imaris.Error;
import Imaris.IDataSetPrx;
import bdv.util.BdvFunctions;
import com.bitplane.xt.ImarisCachedCellImg;
import com.bitplane.xt.ImarisCachedCellImgFactory;
import com.bitplane.xt.ImarisCachedCellImgOptions;
import com.bitplane.xt.ImarisService;
import ij.IJ;
import ij.ImagePlus;
import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.cache.img.CellLoader;
import net.imglib2.cache.img.DiskCachedCellImgFactory;
import net.imglib2.cache.img.SingleCellArrayImg;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;
import org.scijava.Context;

public class ExampleCellLoader
{
	public static void main( String[] args ) throws Error
	{
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

		final RandomAccessibleInterval< UnsignedByteType > output =
				new DiskCachedCellImgFactory<>( new UnsignedByteType() ).create( img, loader );

		// TODO: now replace this by ImarisCachedCellImg...

		BdvFunctions.show( output, "output" );
	}
}
