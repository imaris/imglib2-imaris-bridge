package com.bitplane.xt;

import Imaris.Error;
import Imaris.IDataSetPrx;
import java.util.concurrent.CompletableFuture;
import net.imglib2.Dirty;
import net.imglib2.img.cell.CellGrid;

/**
 * Variant of {@link ImarisCellCache}, which writes cells Imaris {@code IDataset}, but <em>only if they were modified</em>.
 *
 * @param <A>
 * 		access type
 *
 * @author Tobias Pietzsch
 */
public class ImarisDirtyCellCache< A extends Dirty > extends ImarisCellCache< A >
{
	public ImarisDirtyCellCache(
			final IDataSetPrx dataset,
			final int[] mapDimensions,
			final CellGrid grid ) throws Error
	{
		super( dataset, mapDimensions, grid );
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
