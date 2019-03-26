package tpietzsch;

import Imaris.Error;
import Imaris.IDataSetPrx;
import Imaris.cColorTable;
import Imaris.tType;
import java.util.ArrayList;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.axis.CalibratedAxis;
import net.imagej.axis.DefaultLinearAxis;
import net.imglib2.Interval;
import net.imglib2.cache.img.CachedCellImg;
import net.imglib2.cache.img.CellLoader;
import net.imglib2.cache.img.ReadOnlyCachedCellImgFactory;
import net.imglib2.display.ColorTable8;
import net.imglib2.img.Img;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;

import static net.imglib2.cache.img.ReadOnlyCachedCellImgOptions.options;

class ImarisDataset< T extends NativeType< T > >
{
	private final IDataSetPrx dataset;

	private final T type;

	private final ArrayList< CalibratedAxis > axes;

	private final long[] dimensions;

	private final int[] cellDimensions;

	private final int[] mapDimensions;

	private final double[] calib;

	public ImarisDataset( final IDataSetPrx dataset ) throws Error
	{
		final int xyzCellSize = 32; // TODO make configurable

		this.dataset = dataset;

		final tType dstype = dataset.GetType();
		switch ( dstype )
		{
		case eTypeUInt8:
			type = ( T ) new UnsignedByteType();
			break;
		case eTypeUInt16:
			type = ( T ) new UnsignedShortType();
			break;
		case eTypeFloat:
			type = ( T ) new FloatType();
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

	private static MapIntervalDimension mapIntervalDimension( final int d )
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

	public Img< T > getImage()
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

	public ImgPlus< T > getImgPlus() throws Error
	{
		final Img< T > img = getImage();
		final ImgPlus< T > imp = new ImgPlus<>( img );

		for ( int i = 0; i < axes.size(); ++i )
			imp.setAxis( axes.get( i ), i );

		final int sc = dataset.GetSizeC();
		final int sz = dataset.GetSizeZ();
		imp.initializeColorTables( sc * sz );
		for ( int c = 0; c < sc; ++c )
		{
			final ColorTable8 cT = createColorTable( c );
			for ( int z = 0; z < sz; ++z )
			{
				imp.setColorTable( cT, c * sz + z );
			}
		}

		return imp;
	}







	private ColorTable8 createColorTable( final int channel ) throws Error
	{
		final cColorTable vColorTable = dataset.GetChannelColorTable( channel );
		if ( vColorTable != null && vColorTable.mColorRGB.length > 0 )
			return createColorTableFrom( vColorTable );
		else
		{
			final int vColor = dataset.GetChannelColorRGBA( channel );
			return createColorTableFrom( vColor );
		}
	}

	private static ColorTable8 createColorTableFrom( final int aRGBA )
	{
		final int vSize = 256;

		final byte[] rLut = new byte[ vSize ];
		final byte[] gLut = new byte[ vSize ];
		final byte[] bLut = new byte[ vSize ];
		final byte[] aLut = new byte[ vSize ];

		final int[] vRGBA = new int[ 4 ];
		components( aRGBA, vRGBA );
		for ( int i = 0; i < vSize; ++i )
		{
			rLut[ i ] = ( byte ) ( i * vRGBA[ 0 ] / 255 );
			gLut[ i ] = ( byte ) ( i * vRGBA[ 1 ] / 255 );
			bLut[ i ] = ( byte ) ( i * vRGBA[ 2 ] / 255 );
			aLut[ i ] = ( byte ) ( i * vRGBA[ 3 ] / 255 );
		}
		return new ColorTable8( rLut, gLut, bLut, aLut );
	}

	private static ColorTable8 createColorTableFrom( final cColorTable aColor )
	{
		final int[] vRGB = aColor.mColorRGB;
		final int vSize = 256;
		final int vSourceSize = vRGB.length;

		final byte[] rLut = new byte[ vSize ];
		final byte[] gLut = new byte[ vSize ];
		final byte[] bLut = new byte[ vSize ];
		final byte[] aLut = new byte[ vSize ];

		final int[] vRGBA = new int[ 4 ];
		for ( int i = 0; i < vSize; ++i )
		{
			final int vIndex = ( i * vSourceSize ) / vSize;
			components( vRGB[ vIndex ], vRGBA );
			rLut[ i ] = ( byte ) ( vRGBA[ 0 ] );
			gLut[ i ] = ( byte ) ( vRGBA[ 1 ] );
			bLut[ i ] = ( byte ) ( vRGBA[ 2 ] );
			aLut[ i ] = aColor.mAlpha;
		}
		return new ColorTable8( rLut, gLut, bLut, aLut );
	}

	private static void components( final int rgba, final int[] components )
	{
		components[ 0 ] = rgba & 0xff;
		components[ 1 ] = ( rgba >> 8 ) & 0xff;
		components[ 2 ] = ( rgba >> 16 ) & 0xff;
		components[ 3 ] = ( rgba >> 24 ) & 0xff;
	}









	// for BDV?

	double[] getCalib()
	{
		return calib;
	}

	ARGBType getChannelColor( final int channel ) throws Error
	{
		final int rgba = dataset.GetChannelColorRGBA( channel );
		final int r = rgba & 0xff;
		final int g = ( rgba >> 8 ) & 0xff;
		final int b = ( rgba >> 16 ) & 0xff;
		final int a = ( rgba >> 24 ) & 0xff;
		System.out.println( "rgba = " + rgba );
		return new ARGBType( ARGBType.rgba( r, g, b, a ) );
	}
}
