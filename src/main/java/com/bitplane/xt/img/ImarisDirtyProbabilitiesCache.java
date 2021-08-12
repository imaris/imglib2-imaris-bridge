package com.bitplane.xt.img;

import Imaris.Error;
import Imaris.IDataSetPrx;
import java.util.concurrent.CompletableFuture;
import net.imglib2.Dirty;
import net.imglib2.cache.CacheLoader;
import net.imglib2.img.cell.Cell;
import net.imglib2.img.cell.CellGrid;
import net.imglib2.type.PrimitiveType;

/**
 * Variant of {@link ImarisProbabilitiesCache}, which writes cells to Imaris
 * {@code IDataset}, but <em>only if they were modified</em>.
 *
 * @param <A>
 * 		access type
 *
 * @author Tobias Pietzsch
 */
public class ImarisDirtyProbabilitiesCache< A extends Dirty > extends ImarisProbabilitiesCache< A >
{
	public ImarisDirtyProbabilitiesCache(
			final IDataSetPrx dataset,
			final PrimitiveType primitiveType, // primitive type underlying accesses
			final int[] mapDimensions,
			final CellGrid grid,
			final CacheLoader< Long, Cell< A > > backingLoader,
			final boolean persistOnLoad ) throws Error
	{
		super( dataset, primitiveType, mapDimensions, grid, backingLoader, persistOnLoad, true );
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