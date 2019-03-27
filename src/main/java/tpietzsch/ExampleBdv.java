package tpietzsch;

import Imaris.Error;
import bdv.tools.brightness.ConverterSetup;
import bdv.util.AxisOrder;
import bdv.util.BdvFunctions;
import bdv.util.BdvOptions;
import bdv.util.BdvStackSource;
import bdv.util.volatiles.SharedQueue;
import bdv.util.volatiles.VolatileViews;
import java.util.ArrayList;
import java.util.List;
import net.imagej.Dataset;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imagej.axis.CalibratedAxis;
import net.imagej.axis.LinearAxis;
import net.imglib2.type.numeric.ARGBType;
import org.scijava.Context;

public class ExampleBdv
{
	public static void main( String[] args ) throws Error
	{
		Context context = new Context();
		final ImarisService imaris = context.getService( ImarisService.class );
		final Dataset dataset = imaris.getDataset();

		final SharedQueue queue = new SharedQueue( 10 );
		final BdvStackSource< ? > stackSource = BdvFunctions.show(
				VolatileViews.wrapAsVolatile( dataset.getImgPlus().getImg(), queue ),
				dataset.getName(),
				BdvOptions.options().sourceTransform( getCalibration( dataset ) ).axisOrder( getAxisOrder( dataset ) ) );
		final List< ConverterSetup > channels = stackSource.getConverterSetups();
		for ( int i = 0; i < channels.size(); i++ )
			channels.get( i ).setColor( defaultColor( i ) );
	}

	private static double[] getCalibration( final Dataset dataset )
	{
		final int n = dataset.numDimensions();
		final ArrayList< Double > calib = new ArrayList<>();
		for ( int d = 0; d < n; ++d )
		{
			final CalibratedAxis axis = dataset.axis( d );
			if ( axis.type().isSpatial() )
			{
				if ( axis instanceof LinearAxis )
					calib.add( ( ( LinearAxis ) axis ).scale() );
				else
					calib.add( 1.0 );
			}
		}
		return calib.stream().mapToDouble( Double::doubleValue ).toArray();
	}

	private static AxisOrder getAxisOrder( final Dataset dataset )
	{
		final int n = dataset.numDimensions();
		final StringBuffer sb = new StringBuffer();
		for ( int d = 0; d < n; ++d )
		{
			final CalibratedAxis axis = dataset.axis( d );
			final AxisType axisType = axis.type();
			if ( axisType.equals( Axes.X ) )
				sb.append( "X" );
			else if ( axisType.equals( Axes.Y ) )
				sb.append( "Y" );
			else if ( axisType.equals( Axes.Z ) )
				sb.append( "Z" );
			else if ( axisType.equals( Axes.CHANNEL ) )
				sb.append( "C" );
			else if ( axisType.equals( Axes.TIME ) )
				sb.append( "T" );
		}
		AxisOrder axisOrder = AxisOrder.DEFAULT;
		try
		{
			axisOrder = AxisOrder.valueOf( sb.toString() );
		}
		catch ( IllegalArgumentException e )
		{}
		return axisOrder;
	}

	private static final ARGBType[] defaultColors = new ARGBType[] {
			new ARGBType( ARGBType.rgba( 255, 0, 0, 255 ) ),
			new ARGBType( ARGBType.rgba( 0, 255, 0, 255 ) ),
			new ARGBType( ARGBType.rgba( 0, 0, 255, 255 ) ),
			new ARGBType( ARGBType.rgba( 255, 255, 255, 255 ) )
	};

	private static ARGBType defaultColor( final int i )
	{
		return i < defaultColors.length ? defaultColors[ i ] : defaultColors[ defaultColors.length - 1 ];
	}
}
