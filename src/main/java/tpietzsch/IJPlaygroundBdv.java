package tpietzsch;

import bdv.spimdata.SpimDataMinimal;
import bdv.spimdata.XmlIoSpimDataMinimal;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.generic.sequence.BasicSetupImgLoader;
import net.imagej.ImageJ;
import net.imagej.ImgPlus;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;

public class IJPlaygroundBdv
{

	public static void main( String[] args ) throws SpimDataException
	{
		final ImageJ ij = new ImageJ();
		ij.ui().showUI();

		final ImgPlus< UnsignedShortType > imp = getImgPlus();
		ij.ui().show( imp );
	}

	private static ImgPlus< UnsignedShortType > getImgPlus() throws SpimDataException
	{
		final SpimDataMinimal spimData = new XmlIoSpimDataMinimal().load( "/Users/pietzsch/Desktop/Mastodon/merging/Mastodon-files_SimView2_20130315/bdv data/dataset_hdf5.xml" );
		final BasicSetupImgLoader< ? > sil = spimData.getSequenceDescription().getImgLoader().getSetupImgLoader( 0 );
		final Img< UnsignedShortType > img = ( Img< UnsignedShortType > ) sil.getImage( 0 );
		return new ImgPlus< UnsignedShortType >( img );
	}
}
