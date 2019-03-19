package tpietzsch;

import Ice.ObjectPrx;
import Imaris.Error;
import Imaris.IApplicationPrx;
import Imaris.IDataSetPrx;
import Imaris.tType;
import ImarisServer.IServerPrx;
import bdv.util.BdvFunctions;
import bdv.util.BdvOptions;
import bdv.util.volatiles.VolatileViews;
import com.bitplane.xt.BPImarisLib;
import net.imglib2.cache.img.CachedCellImg;
import net.imglib2.cache.img.CellLoader;
import net.imglib2.cache.img.ReadOnlyCachedCellImgFactory;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.integer.UnsignedByteType;

import static Imaris.IApplicationPrxHelper.checkedCast;
import static net.imglib2.cache.img.ReadOnlyCachedCellImgOptions.options;

public class Playground
{
	private final BPImarisLib lib;

	private final IServerPrx server;

	public Playground()
	{
		lib = new BPImarisLib();
		server = lib.GetServer();
	}

	final IApplicationPrx getApplication()
	{
		if ( server.GetNumberOfObjects() < 1 )
			throw new IllegalStateException();
		final int id = 0;
		final ObjectPrx obj = server.GetObject( id );
		final IApplicationPrx app = checkedCast( obj );
		return app;
	}

	void disconnect()
	{
		lib.Disconnect();
	}

	static class ImarisDataset
	{
		private final IDataSetPrx dataset;

		private final long[] xyzDims;

		private final double[] calib;

		public ImarisDataset( final IDataSetPrx dataset ) throws Error
		{
			this.dataset = dataset;
			final int sx = dataset.GetSizeX();
			final int sy = dataset.GetSizeY();
			final int sz = dataset.GetSizeZ();

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

			xyzDims = new long[] { sx, sy, sz };
		}

		final Img< UnsignedByteType > getImage( final int channel, final int timepoint )
		{
			final ReadOnlyCachedCellImgFactory factory = new ReadOnlyCachedCellImgFactory();
			final CellLoader< UnsignedByteType > loader = cell -> {
				final byte[] out = ( byte[] ) cell.getStorageArray();
				final byte[] in = dataset.GetDataSubVolumeAs1DArrayBytes(
						( int ) cell.min( 0 ),
						( int ) cell.min( 1 ),
						( int ) cell.min( 2 ),
						channel,
						timepoint,
						( int ) cell.dimension( 0 ),
						( int ) cell.dimension( 1 ),
						( int ) cell.dimension( 2 )
				);
				System.arraycopy( in, 0, out, 0, out.length );
			};
			final CachedCellImg< UnsignedByteType, ? > img = factory.create(
					xyzDims,
					new UnsignedByteType(),
					loader,
					options().cellDimensions( 16 ) );
			return img;
		}
	}

	public static void main( String[] args ) throws Error
	{
		final Playground pg = new Playground();
		final IApplicationPrx application = pg.getApplication();
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
		final Img< UnsignedByteType > img = ds.getImage( 0, 0 );
		BdvFunctions.show( VolatileViews.wrapAsVolatile( img ), "imaris", BdvOptions.options().sourceTransform( ds.calib ) );

		try
		{
			Thread.sleep( 10000 );
		}
		catch ( InterruptedException e )
		{
			e.printStackTrace();
		}
		System.out.println( "shutting down" );
		pg.disconnect();
	}
}
