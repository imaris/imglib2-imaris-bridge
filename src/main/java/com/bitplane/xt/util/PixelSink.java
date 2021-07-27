package com.bitplane.xt.util;

import Imaris.Error;
import Imaris.IDataSetPrx;
import Imaris.tType;
import com.bitplane.xt.ImarisLoaderRemover;
import java.util.function.IntFunction;
import net.imglib2.img.basictypeaccess.array.ArrayDataAccess;

import static com.bitplane.xt.util.MapDimensions.selectIntervalDimension;

/**
 * Writing {@code ArrayDataAccess} as Imaris blocks
 *
 * @param <A>
 *     TODO
 *     {@code ByteArray}
 *     {@code VolatileByteArray}
 *     {@code DirtyVolatileByteArray}
 *     {@code ShortArray}
 *     {@code VolatileShortArray}
 *     {@code DirtyVolatileShortArray}
 *     {@code FloatArray}
 *     {@code VolatileFloatArray}
 *     {@code DirtyVolatileFloatArray}
 */
@FunctionalInterface
public interface PixelSink< A >
{
	/**
	 * Set sub-volume as flattened primitive array.
	 *
	 * @param data
	 *  	{@code byte[]}, {@code short[]}, {@code float[]}, depending on dataset type.
	 * @param min
	 * 		minimum of interval in {@code Img} space.
	 * 		Will be augmented to 5D if necessary.
	 * @param size
	 * 		size of interval in {@code Img} space.
	 * 		Will be augmented to 5D if necessary.
	 */
	void put( A data, long[] min, int[] size ) throws Error;


	/**
	 * TODO
	 */
	static < A > PixelSink< A > volatileArraySink(
			final IDataSetPrx dataset,
			final tType datasetType,
			final int[] mapDimensions )
	{
		final SetDataSubVolume slice = SetDataSubVolume.forDataSet( dataset, datasetType );

		final IntFunction< Object > creator;
		switch ( datasetType )
		{
		case eTypeUInt8:
			creator = byte[]::new;
			break;
		case eTypeUInt16:
			creator = short[]::new;
			break;
		case eTypeFloat:
			creator = float[]::new;
			break;
		default:
			throw new IllegalArgumentException();
		}

		final MapDimensions.SelectIntervalDimension x = selectIntervalDimension( mapDimensions[ 0 ] );
		final MapDimensions.SelectIntervalDimension y = selectIntervalDimension( mapDimensions[ 1 ] );
		final MapDimensions.SelectIntervalDimension z = selectIntervalDimension( mapDimensions[ 2 ] );
		final MapDimensions.SelectIntervalDimension c = selectIntervalDimension( mapDimensions[ 3 ] );
		final MapDimensions.SelectIntervalDimension t = selectIntervalDimension( mapDimensions[ 4 ] );

		return ( access, min, size ) ->
		{
			final Object data = ( ( ArrayDataAccess ) access ).getCurrentStorageArray();

			final int ox = x.min( min );
			final int oy = y.min( min );
			final int oz = z.min( min );
			final int oc = c.min( min );
			final int ot = t.min( min );

			final int sx = x.size( size );
			final int sy = y.size( size );
			final int sz = z.size( size );
			final int sc = c.size( size );
			final int st = t.size( size );

			if ( sc == 1 && st == 1 )
				slice.set( data, ox, oy, oz, oc, ot, sx, sy, sz );
			else
			{
				final int slicelength = sx * sy * sz;
				final Object slicedata = creator.apply( slicelength );
				for ( int dt = 0; dt < st; ++dt )
				{
					for ( int dc = 0; dc < sc; ++dc )
					{
						final int srcpos = ( dt * sc + dc ) * slicelength;
						System.arraycopy( data, srcpos, slicedata, 0, slicelength );
						slice.set( slicedata, ox, oy, oz, oc + dc, ot + dt, sx, sy, sz );
					}
				}
			}
		};
	}
}
