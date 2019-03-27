package tpietzsch;

import Imaris.Error;
import org.scijava.Context;
import org.scijava.ui.swing.script.InterpreterWindow;

public class ExampleREPL
{
	public static void main( final String[] args ) throws Error
	{
		final Context context = new Context();
		final InterpreterWindow window = new InterpreterWindow( context );
		window.setVisible( true );
	}
}
