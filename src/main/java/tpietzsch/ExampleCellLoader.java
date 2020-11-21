package tpietzsch;

import Imaris.Error;
import Imaris.IDataSetPrx;
import bdv.util.BdvFunctions;
import bdv.util.volatiles.VolatileViews;
import com.bitplane.xt.ImarisCachedCellImg;
import com.bitplane.xt.ImarisCachedCellImgFactory;
import com.bitplane.xt.ImarisCachedCellImgOptions;
import com.bitplane.xt.ImarisService;
import ij.IJ;
import ij.ImagePlus;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.cache.Cache;
import net.imglib2.cache.img.CellLoader;
import net.imglib2.cache.img.SingleCellArrayImg;
import net.imglib2.img.cell.Cell;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.util.Intervals;
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

		final Cache< Long, ? extends Cell< ? > > cache = imarisImg.getCache();
		final long numCells = Intervals.numElements( imarisImg.getCellGrid().getGridDimensions() );
		for ( long i = 0; i < numCells; i++ )
		{
			final Long key = i;
			final Callable< Void > c = () -> {
				cache.get( key );
				cache.persist( key );
				return null;
			};
			try
			{
				cache.get( i );
				cache.persist( i );
			}
			catch ( ExecutionException e )
			{
				e.printStackTrace();
			}
		}

		final IDataSetPrx dataset = imarisImg.getDataSet();
		imaris.app().SetImage( 0, dataset );
	}
}
