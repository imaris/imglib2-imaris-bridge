package com.bitplane.xt.util;

import Imaris.tType;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;

import static Imaris.tType.eTypeFloat;
import static Imaris.tType.eTypeUInt16;
import static Imaris.tType.eTypeUInt8;

public final class TypeUtils
{
	/**
	 * Get the Imaris {@code tType} corresponding to the given imglib2 {@code type}
	 */
	public static tType imarisTypeFor( final Object type )
	{
		if ( type instanceof UnsignedByteType )
			return eTypeUInt8;
		else if ( type instanceof UnsignedShortType )
			return eTypeUInt16;
		else if ( type instanceof FloatType )
			return eTypeFloat;
		else
			throw new IllegalArgumentException( "Only UnsignedByteType, UnsignedShortType, FloatType are supported (not " + type.getClass().getSimpleName() + ")" );
	}

	/**
	 * Get the ImgLib2 {@code Type} corresponding to the given Imaris {@code type}
	 */
	public static < T extends NativeType< T > & RealType< T > > T imglibTypeFor( final tType type )
	{
		switch ( type )
		{
		case eTypeUInt8:
			return  ( T ) new UnsignedByteType();
		case eTypeUInt16:
			return ( T ) new UnsignedShortType();
		case eTypeFloat:
			return ( T ) new FloatType();
		default:
			throw new IllegalArgumentException();
		}
	}

	private TypeUtils() {}
}
