package tpietzsch;

import Imaris.Error;
import org.scijava.Context;
import org.scijava.ui.swing.script.InterpreterWindow;

public class ReplPlayground
{

	public static void main( String[] args ) throws Error
	{
		final Context context = new Context();
		final InterpreterWindow window = new InterpreterWindow( context );
		window.setVisible( true );
		final ImarisService imaris = context.getService( ImarisService.class );
	}
}
