package tpietzsch;

import Imaris.Error;
import Imaris.IDataSetPrx;
import Imaris.tType;
import bdv.util.AxisOrder;
import bdv.util.BdvFunctions;
import com.bitplane.xt.ImarisCachedCellImgOptions;
import com.bitplane.xt.ImarisCachedLabelImg;
import com.bitplane.xt.ImarisCachedLabelImgFactory;
import com.bitplane.xt.ImarisService;
import com.bitplane.xt.ImarisUtils;
import net.imglib2.Cursor;
import net.imglib2.cache.img.CellLoader;
import net.imglib2.cache.img.SingleCellArrayImg;
import net.imglib2.type.numeric.integer.ShortType;
import org.scijava.Context;

public class ExampleImarisCachedLabelImg
{
	static final long[] dimensions = { 100, 100, 100, 10 };
	// channels are folded into value (which for 3 channels can be 0, 1, 2, 3)

	static final int numChannels = 3;

	public static void main( String[] args ) throws Error
	{
		Context context = new Context();
		final ImarisService imaris = context.getService( ImarisService.class );

		toImaris( imaris );
		fromImaris( imaris );
	}

	static void fromImaris( final ImarisService imaris ) throws Error
	{
		ImarisCachedLabelImgFactory< ShortType > factory = new ImarisCachedLabelImgFactory<>(
				new ShortType(),
				imaris,
				ImarisCachedCellImgOptions.options()
						.cellDimensions( 64 )
						.numIoThreads( 20 ) );
		final ImarisCachedLabelImg< ShortType, ? > imarisImg = factory.create( imaris.app().GetDataSet(), dimensions );

		BdvFunctions.show( imarisImg, "labels" );
	}

	static void toImaris( final ImarisService imaris ) throws Error
	{
		ImarisCachedLabelImgFactory< ShortType > factory = new ImarisCachedLabelImgFactory<>(
				new ShortType(),
				imaris,
				ImarisCachedCellImgOptions.options()
						.cellDimensions( 64 )
						.numIoThreads( 20 ) );

		final IDataSetPrx dataset = ImarisUtils.createDataset(
				imaris.app(),
				tType.eTypeUInt8,
				AxisOrder.XYZCT,
				dimensions[ 0 ], dimensions[ 1 ], dimensions[ 2 ], numChannels, dimensions[ 3 ] );

		// TODO
		final ImarisCachedLabelImg< ShortType, ? > imarisImg = factory.create( dataset, dimensions, cell -> {} );
//		final ImarisCachedLabelImg< ShortType, ? > imarisImg = factory.create( dataset, dimensions );

		final Cursor< ShortType > c = imarisImg.localizingCursor();
		while ( c.hasNext() )
		{
			c.fwd();
			c.get().set( ( short ) ( c.getIntPosition( 0 ) % numChannels + 1 ) );
		}

		imarisImg.persist();

		imaris.app().SetImage( 0, dataset );
	}
}
