package com.bitplane.xt;

import net.imagej.Dataset;
import net.imagej.ImageJ;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

@Plugin( type = Command.class, menuPath = "File>Import>From Imaris" )
public class ExamplePlugin implements Command
{
	@Parameter
	private ImarisService imaris;

	@Parameter( type = ItemIO.OUTPUT )
	private Dataset dataset;

	@Override
	public void run()
	{
		dataset = imaris.getApplication().getDataset().getIJDataset();
	}

	public static void main( final String[] args )
	{
		new ImageJ().ui().showUI();
	}
}
