package tpietzsch;

import Imaris.Error;
import Imaris.IApplicationPrx;
import Imaris.IDataSetPrx;
import Imaris.tType;
import bdv.tools.brightness.ConverterSetup;
import bdv.util.AxisOrder;
import bdv.util.BdvFunctions;
import bdv.util.BdvOptions;
import bdv.util.BdvStackSource;
import bdv.util.volatiles.VolatileViews;
import bdv.viewer.SourceAndConverter;
import java.util.List;
import net.imglib2.Volatile;
import net.imglib2.cache.img.CachedCellImg;
import net.imglib2.cache.img.CellLoader;
import net.imglib2.cache.img.ReadOnlyCachedCellImgFactory;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import org.scijava.Context;

import static bdv.util.AxisOrder.XYZCT;
import static net.imglib2.cache.img.ReadOnlyCachedCellImgOptions.options;

public class Playground
{
	static class ImarisDataset
	{
		private final IDataSetPrx dataset;

		private final long[] dimensions;

		private final double[] calib;

		private final int sx;
		private final int sy;
		private final int sz;
		private final int sc;
		private final int st;

		public ImarisDataset( final IDataSetPrx dataset ) throws Error
		{
			this.dataset = dataset;

			sx = dataset.GetSizeX();
			sy = dataset.GetSizeY();
			sz = dataset.GetSizeZ();
			sc = dataset.GetSizeC();
			st = dataset.GetSizeT();

			final double maxX = dataset.GetExtendMaxX();
			final double minX = dataset.GetExtendMinX();
			final double maxY = dataset.GetExtendMaxY();
			final double minY = dataset.GetExtendMinY();
			final double maxZ = dataset.GetExtendMaxZ();
			final double minZ = dataset.GetExtendMinZ();

			calib = new double[] {
					( maxX - minX ) / sx,
					( maxY - minY ) / sy,
					( maxZ - minZ ) / sz
			};

			dimensions = new long[] { sx, sy, sz, sc, st };
		}

		final Img< UnsignedByteType > getImage()
		{
			final ReadOnlyCachedCellImgFactory factory = new ReadOnlyCachedCellImgFactory();
			final CellLoader< UnsignedByteType > loader = cell -> {
				final byte[] out = ( byte[] ) cell.getStorageArray();
				final byte[] in = dataset.GetDataSubVolumeAs1DArrayBytes(
						( int ) cell.min( 0 ),
						( int ) cell.min( 1 ),
						( int ) cell.min( 2 ),
						( int ) cell.min( 3 ),
						( int ) cell.min( 4 ),
						( int ) cell.dimension( 0 ),
						( int ) cell.dimension( 1 ),
						( int ) cell.dimension( 2 )
				);
				System.arraycopy( in, 0, out, 0, out.length );
			};
			final int xyzCellSize = 32;
			final CachedCellImg< UnsignedByteType, ? > img = factory.create(
					dimensions,
					new UnsignedByteType(),
					loader,
					options().cellDimensions( xyzCellSize, xyzCellSize, xyzCellSize, 1, 1 ) );
			return img;
		}
	}

	public static void main( String[] args ) throws Error
	{
		Context context = new Context();
		final ImarisService imaris = context.getService( ImarisService.class );
		final IApplicationPrx application = imaris.app();

		System.out.println( "application = " + application );
		System.out.println( "application.GetCurrentFileName() = " + application.GetCurrentFileName() );
		System.out.println( "application.GetCurrentFileName( null ) = " + application.GetCurrentFileName( null ) );;

		final IDataSetPrx dataset = application.GetDataSet();
		System.out.println( "dataset = " + dataset );

		final tType tType = dataset.GetType();
		System.out.println( "type = " + tType );

//		final byte[] bytes = dataset.GetDataVolumeAs1DArrayBytes( 0, 0 );
//		final long[] dims = { dataset.GetSizeX(), dataset.GetSizeY(), dataset.GetSizeZ() };
//		final Img< UnsignedByteType > img = ArrayImgs.unsignedBytes( bytes, dims );

		final ImarisDataset ds = new ImarisDataset( dataset );
		final Img< UnsignedByteType > img = ds.getImage();
		final BdvStackSource< ? > stackSource = BdvFunctions.show( VolatileViews.wrapAsVolatile( img ), "imaris", BdvOptions.options().sourceTransform( ds.calib ).axisOrder( XYZCT ) );
		final List< ConverterSetup > channels = stackSource.getConverterSetups();
		for ( int i = 0; i < channels.size(); i++ )
		{
			int rgba = dataset.GetChannelColorRGBA( i );
			int r = rgba & 0xff;
			int g = ( rgba >> 8 ) & 0xff;
			int b = ( rgba >> 16 ) & 0xff;
			int a = ( rgba >> 24 ) & 0xff;
			System.out.println( "rgba = " + rgba );
			final ARGBType color = new ARGBType( ARGBType.rgba( r, g, b, a ) );
			System.out.println( "type = " + color );
			channels.get( i ).setColor( color );
		}

		try
		{
			Thread.sleep( 100000 );
		}
		catch ( InterruptedException e )
		{
			e.printStackTrace();
		}
		System.out.println( "shutting down" );
		imaris.shutdown();
	}
}
