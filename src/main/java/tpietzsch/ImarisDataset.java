package tpietzsch;

import Imaris.Error;
import Imaris.IDataSetPrx;
import Imaris.tType;
import java.util.ArrayList;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.axis.CalibratedAxis;
import net.imagej.axis.DefaultLinearAxis;
import net.imagej.axis.LinearAxis;
import net.imglib2.Interval;
import net.imglib2.cache.img.CachedCellImg;
import net.imglib2.cache.img.CellLoader;
import net.imglib2.cache.img.ReadOnlyCachedCellImgFactory;
import net.imglib2.img.Img;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;

import static net.imglib2.cache.img.ReadOnlyCachedCellImgOptions.options;

class ImarisDataset
{
	private final IDataSetPrx dataset;

	private final ArrayList< CalibratedAxis > axes;

	private final long[] dimensions;

	private final int[] cellDimensions;

	private final int[] mapDimensions;

	private final double[] calib;

	private final NativeType type;

	public ImarisDataset( final IDataSetPrx dataset ) throws Error
	{
		final int xyzCellSize = 32; // TODO make configurable

		this.dataset = dataset;

		final tType dstype = dataset.GetType();
		switch ( dstype )
		{
		case eTypeUInt8:
			type = new UnsignedByteType();
			break;
		case eTypeUInt16:
			type = new UnsignedShortType();
			break;
		case eTypeFloat:
			type = new FloatType();
			break;
		default:
			throw new IllegalArgumentException();
		}

		axes = new ArrayList<>();
		mapDimensions = new int[] { 0, 1, -1, -1, -1 };

		final int sx = dataset.GetSizeX();
		final int sy = dataset.GetSizeY();
		final int sz = dataset.GetSizeZ();
		final int sc = dataset.GetSizeC();
		final int st = dataset.GetSizeT();

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

		final String unit = dataset.GetUnit();

		final ArrayList< Long > dimensionsList = new ArrayList<>();
		final ArrayList< Integer > cellDimensionsList = new ArrayList<>();

		axes.add( new DefaultLinearAxis( Axes.X, unit, calib[ 0 ] ) );
		dimensionsList.add( ( long ) sx );
		cellDimensionsList.add( xyzCellSize );

		axes.add( new DefaultLinearAxis( Axes.Y, unit, calib[ 1 ] ) );
		dimensionsList.add( ( long ) sy );
		cellDimensionsList.add( xyzCellSize );

		int d = 2;
		if ( sz > 1 )
		{
			axes.add( new DefaultLinearAxis( Axes.Z, unit, calib[ 2 ] ) );
			dimensionsList.add( ( long ) sz );
			cellDimensionsList.add( xyzCellSize );
			mapDimensions[ 2 ] = d++;
		}

		if ( sc > 1 )
		{
			axes.add( new DefaultLinearAxis( Axes.CHANNEL ) );
			dimensionsList.add( ( long ) sc );
			cellDimensionsList.add( 1 );
			mapDimensions[ 3 ] = d++;
		}

		if ( st > 1 )
		{
			axes.add( new DefaultLinearAxis( Axes.TIME ) );
			dimensionsList.add( ( long ) st );
			cellDimensionsList.add( 1 );
			mapDimensions[ 4 ] = d;
		}

		dimensions = dimensionsList.stream().mapToLong( Long::longValue ).toArray();
		cellDimensions = cellDimensionsList.stream().mapToInt( Integer::intValue ).toArray();
	}

	@FunctionalInterface
	private interface GetDataSubVolume
	{
		/**
		 * @return {@code byte[]}, {@code short[]}, {@code float[]}, depending on dataset type.
		 */
		Object get( final int ox, final int oy, final int oz, final int oc, final int ot, final int sx, final int sy, final int sz ) throws Error;
	}

	@FunctionalInterface
	private interface PixelSource
	{
		Object getData( final Interval interval ) throws Error;
	}

	private interface MapIntervalDimension
	{
		int min( final Interval interval );
		int size( final Interval interval );
	}

	private static MapIntervalDimension mapIntervalDimension( int d )
	{
		if ( d < 0 )
			return constantMapIntervalDimension;

		return new MapIntervalDimension()
		{
			@Override
			public int min( final Interval interval )
			{
				return ( int ) interval.min( d );
			}

			@Override
			public int size( final Interval interval )
			{
				return ( int ) interval.dimension( d );
			}
		};
	}

	private static MapIntervalDimension constantMapIntervalDimension = new MapIntervalDimension()
	{
		@Override
		public int min( final Interval interval )
		{
			return 0;
		}

		@Override
		public int size( final Interval interval )
		{
			return 1;
		}
	};

	private PixelSource pixelSource( final GetDataSubVolume datasource )
	{
		final MapIntervalDimension x = mapIntervalDimension( mapDimensions[ 0 ] );
		final MapIntervalDimension y = mapIntervalDimension( mapDimensions[ 1 ] );
		final MapIntervalDimension z = mapIntervalDimension( mapDimensions[ 2 ] );
		final MapIntervalDimension c = mapIntervalDimension( mapDimensions[ 3 ] );
		final MapIntervalDimension t = mapIntervalDimension( mapDimensions[ 4 ] );
		return i -> datasource.get(	x.min( i ), y.min( i ), z.min( i ), c.min( i ), t.min( i ), x.size( i ), y.size( i ), z.size( i ) );
	}

	Img< ? > getImage()
	{
		return getImageInternal( type );
	}

	private < T extends NativeType< T > > Img< T > getImageInternal( T type )
	{
		final PixelSource s = pixelSource( dataset::GetDataSubVolumeAs1DArrayBytes );
		final ReadOnlyCachedCellImgFactory factory = new ReadOnlyCachedCellImgFactory();
		final CellLoader< T > loader = cell -> System.arraycopy( s.getData( cell ), 0, cell.getStorageArray(), 0, ( int ) cell.size() );
		final CachedCellImg< T, ? > img = factory.create(
				dimensions,
				type,
				loader,
				options().cellDimensions( cellDimensions ) );
		return img;
	}

	ImgPlus< ? > getImgPlus() throws Error
	{
		final Img< ? > img = getImage();
		final ImgPlus< ? > imp = new ImgPlus<>( img );

		for ( int c = 0; c < 5; ++c )
			imp.setAxis( axis( c ), c );

//		imp.setColorTable(  );

		return imp;
	}

	LinearAxis axis( int c ) throws Error
	{
		final String unit = dataset.GetUnit();
		switch ( c )
		{
		case 0:
			return new DefaultLinearAxis( Axes.X, unit, calib[ 0 ] );
		case 1:
			return new DefaultLinearAxis( Axes.Y, unit, calib[ 1 ] );
		case 2:
			return new DefaultLinearAxis( Axes.Z, unit, calib[ 2 ] );
		case 3:
			return new DefaultLinearAxis( Axes.CHANNEL );
		case 4:
			return new DefaultLinearAxis( Axes.TIME );
		default:
			throw new IllegalArgumentException();
		}
	}

	double[] getCalib()
	{
		return calib;
	}

	ARGBType getChannelColor( int channel ) throws Error
	{
		int rgba = dataset.GetChannelColorRGBA( channel );
		int r = rgba & 0xff;
		int g = ( rgba >> 8 ) & 0xff;
		int b = ( rgba >> 16 ) & 0xff;
		int a = ( rgba >> 24 ) & 0xff;
		System.out.println( "rgba = " + rgba );
		return new ARGBType( ARGBType.rgba( r, g, b, a ) );
	}
}
