package com.bitplane.xt;

import Imaris.Error;
import Imaris.IDataSetPrx;
import java.util.concurrent.CompletableFuture;
import net.imglib2.Dirty;
import net.imglib2.cache.CacheLoader;
import net.imglib2.img.cell.Cell;
import net.imglib2.img.cell.CellGrid;
import net.imglib2.type.PrimitiveType;

/**
 * Variant of {@link ImarisCellCache}, which writes cells Imaris {@code IDataset}, but <em>only if they were modified</em>.
 *
 * @param <A>
 * 		access type
 *
 * @author Tobias Pietzsch
 */
public class ImarisDirtyLabelCache< A extends Dirty > extends ImarisLabelCache< A >
{
	public ImarisDirtyLabelCache(
			final IDataSetPrx dataset,
			final PrimitiveType primitiveType, // primitive type underlying accesses
			final int[] mapDimensions,
			final CellGrid grid,
			final CacheLoader< Long, Cell< A > > backingLoader ) throws Error
	{
		super( dataset, primitiveType, mapDimensions, grid, backingLoader, true );
	}

	@Override
	public void onRemoval( final Long key, final A valueData )
	{
		if ( valueData.isDirty() )
			super.onRemoval( key, valueData );
	}

	@Override
	public CompletableFuture< Void > persist( final Long key, final A valueData )
	{
		if ( valueData.isDirty() )
		{
			final CompletableFuture< Void > result = super.persist( key, valueData );
			valueData.setDirty( false );
			return result;
		}
		else
			return CompletableFuture.completedFuture( null );
	}
}
