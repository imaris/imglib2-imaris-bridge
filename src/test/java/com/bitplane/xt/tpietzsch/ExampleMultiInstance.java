package com.bitplane.xt.tpietzsch;

import com.bitplane.xt.ImarisService;
import org.scijava.Context;

public class ExampleMultiInstance
{
	public static void main( String[] args )
	{
		Context context = new Context();
		final ImarisService imaris = context.getService( ImarisService.class );
		System.out.println( "imaris.app() = " + imaris.getApplication().getIApplicationPrx() );
	}
}
