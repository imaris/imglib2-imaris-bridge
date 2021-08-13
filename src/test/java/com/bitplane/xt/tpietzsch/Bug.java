package com.bitplane.xt.tpietzsch;

import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.display.ColorTable8;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.numeric.integer.UnsignedByteType;

public class Bug
{
public static void main( final String[] args )
{
    final ImageJ ij = new ImageJ();
    ij.ui().showUI();

    final int width = 100;
    final int height = 100;
    final int depth = 20;
    final int numChannels = 3;

    final Img< UnsignedByteType > img = ArrayImgs.unsignedBytes(
            width, height, depth, numChannels );
    img.forEach( t -> t.set( 255 ) );
    final ImgPlus< UnsignedByteType > imp = new ImgPlus<>( img, "bug",
            new AxisType[] { Axes.X, Axes.Y, Axes.Z, Axes.CHANNEL } );

    final byte[] zeros = new byte[ 256 ];
    final byte[] ramp = new byte[ 256 ];
    for ( int i = 0; i < 256; ++i )
        ramp[ i ] = ( byte ) i;
    final ColorTable8 reds = new ColorTable8( ramp, zeros, zeros );
    final ColorTable8 greens = new ColorTable8( zeros, ramp, zeros );
    final ColorTable8 blues = new ColorTable8( zeros, zeros, ramp );
    final ColorTable8[] channelColorTables = new ColorTable8[] { reds, greens, blues };

    final int numPlanes = depth * numChannels;
    imp.initializeColorTables( numPlanes );
    for ( int c = 0; c < numChannels; ++c )
        for ( int z = 0; z < depth; ++z )
            imp.setColorTable( channelColorTables[ c ], c * depth + z );

    final Dataset dataset = ij.dataset().create( imp );
    dataset.setRGBMerged( false );
    ij.ui().show( dataset );
}
}
