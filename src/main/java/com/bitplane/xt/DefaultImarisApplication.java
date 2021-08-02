package com.bitplane.xt;

import Imaris.Error;
import Imaris.IApplicationPrx;
import Imaris.IDataSetPrx;
import com.bitplane.xt.util.DimensionUtils;
import com.bitplane.xt.util.DimensionUtils.DatasetDimensions;
import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import org.scijava.AbstractContextual;
import org.scijava.plugin.Parameter;

public class DefaultImarisApplication extends AbstractContextual implements ImarisApplication
{
	@Parameter
	private DatasetService datasetService;

	private IApplicationPrx iApplicationPrx;

	private int applicationId;

	public DefaultImarisApplication(
			final IApplicationPrx iApplicationPrx,
			final int applicationId )
	{
		this.iApplicationPrx = iApplicationPrx;
		this.applicationId = applicationId;
	}

	@Override
	public IApplicationPrx getIApplicationPrx()
	{
		return iApplicationPrx;
	}

	@Override
	public int getApplicationID()
	{
		return applicationId;
	}

	@Override
	public Dataset getDataset()
	{
		try
		{
			final ImarisDataset< ? > ds = getImarisDataset();
			final Dataset ijDataset = datasetService.create( ds.getImgPlus() );
			ijDataset.setName( ds.getName() );
			ijDataset.setRGBMerged( false );
			return ijDataset;
		}
		catch ( final Error error )
		{
			throw new RuntimeException( error ); // TODO: revise exception handling
		}
	}

	@Override
	public ImarisDataset< ? > getImarisDataset()
	{
		try
		{
			final IDataSetPrx datasetPrx = getIApplicationPrx().GetDataSet();
			if ( datasetPrx == null )
				throw new RuntimeException( "No dataset is open in Imaris" );
			return new ImarisDataset<>( datasetPrx );
		}
		catch ( final Error error )
		{
			throw new RuntimeException( error ); // TODO: revise exception handling
		}
	}

	@Override
	public < T extends NativeType< T > & RealType< T > >
	ImarisDataset< T > createDataset( final T type, final int sx, final int sy, final int sz, final int sc, final int st )
	{
		try
		{
			final DatasetDimensions dims = new DatasetDimensions( sx, sy, sz, sc, st );
			final IDataSetPrx dataset = ImarisUtils.createDataset( iApplicationPrx, ImarisUtils.imarisTypeFor( type ), dims.getImarisDimensions() );
			final boolean writable = true;
			final ImarisDatasetOptions options = ImarisDatasetOptions.options();
			return new ImarisDataset<>( dataset, dims.getAxisOrder(), writable, options );
		}
		catch ( final Error error )
		{
			throw new RuntimeException( error ); // TODO: revise exception handling
		}
	}
}
