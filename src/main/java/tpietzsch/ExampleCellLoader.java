package tpietzsch;

import Imaris.Error;
import Imaris.IDataSetPrx;
import bdv.util.BdvFunctions;
import bdv.util.volatiles.VolatileViews;
import com.bitplane.xt.ImarisCachedCellImg;
import com.bitplane.xt.ImarisCachedCellImgFactory;
import com.bitplane.xt.ImarisCachedCellImgOptions;
import com.bitplane.xt.ImarisService;
import ij.IJ;
import ij.ImagePlus;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.cache.Cache;
import net.imglib2.cache.img.CachedCellImg;
import net.imglib2.cache.img.CellLoader;
import net.imglib2.cache.img.SingleCellArrayImg;
import net.imglib2.img.cell.Cell;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.parallel.Parallelization;
import net.imglib2.parallel.TaskExecutor;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.util.Intervals;
import net.imglib2.util.Util;
import net.imglib2.view.Views;
import org.scijava.Context;

public class ExampleCellLoader
{
	public static void main( String[] args ) throws Error
	{
		Context context = new Context();
		final ImarisService imaris = context.getService( ImarisService.class );

		final String path = "/Users/pietzsch/workspace/data/e002_stack_fused-8bit.tif";
		final ImagePlus imp = IJ.openImage( path );
		final RandomAccessibleInterval< UnsignedByteType > img = ImageJFunctions.wrapReal( imp );

		final CellLoader< UnsignedByteType > loader = new CellLoader< UnsignedByteType >()
		{
			@Override
			public void load( final SingleCellArrayImg< UnsignedByteType, ? > cell ) throws Exception
			{
				final Cursor< UnsignedByteType > out = cell.cursor();
				final Cursor< UnsignedByteType > in = Views.interval( img, cell ).cursor();
				while ( out.hasNext() )
					out.next().set( in.next() );
			}
		};

		ImarisCachedCellImgFactory< UnsignedByteType > factory = new ImarisCachedCellImgFactory<>(
				Util.getTypeFromInterval( img ),
				imaris,
				ImarisCachedCellImgOptions.options()
						.initializeCellsAsDirty( true )
						.cellDimensions( 64 )
						.numIoThreads( 20 ) );

		final ImarisCachedCellImg< UnsignedByteType, ? > imarisImg = factory.create( img, loader );

		BdvFunctions.show( VolatileViews.wrapAsVolatile( imarisImg ), "imarisImg" );

		Parallelization.runMultiThreaded( () -> populateAndPersist( imarisImg ).get() );

		final IDataSetPrx dataset = imarisImg.getDataSet();
		imaris.getIApplicationPrx().SetImage( 0, dataset );
	}

	public static Future< Void > populateAndPersist( final CachedCellImg< ?, ? > img ) throws InterruptedException
	{
		final TaskExecutor exec = Parallelization.getTaskExecutor();
		return populateAndPersist( img, exec.getExecutorService(), exec.getParallelism() );
	}

	public static Future< Void > populateAndPersist(
			final CachedCellImg< ?, ? > img,
			final ExecutorService executor,
			final int numThreads ) throws InterruptedException
	{
		final long numCells = Intervals.numElements( img.getCellGrid().getGridDimensions() );
		final Cache< Long, ? extends Cell< ? > > cache = img.getCache();
		final AtomicLong nextCellIndex = new AtomicLong();
		final List< Callable< Void > > tasks = new ArrayList<>();

		for ( int threadNum = 0; threadNum < numThreads; ++threadNum )
		{
			tasks.add( () -> {
				for ( long i = nextCellIndex.getAndIncrement(); i < numCells; i = nextCellIndex.getAndIncrement() )
				{
					if ( Thread.interrupted() )
						throw new InterruptedException();
					cache.get( i );
					cache.persist( i );
				}
				return null;
			} );
		}
		final List< Future< Void > > futures = executor.invokeAll( tasks );
		return new Future< Void >()
		{
			private boolean cancelled;

			@Override
			public synchronized boolean cancel( final boolean mayInterruptIfRunning )
			{
				if ( !cancelled )
				{
					cancelled = true;
					for ( final Future< Void > future : futures )
						cancelled &= future.cancel( mayInterruptIfRunning );
				}
				return cancelled;
			}

			@Override
			public synchronized boolean isCancelled()
			{
				return cancelled;
			}

			@Override
			public synchronized boolean isDone()
			{
				boolean isDone = true;
				for ( final Future< Void > future : futures )
					isDone &= future.isDone();
				return true;
			}

			@Override
			public Void get() throws InterruptedException, ExecutionException
			{
				for ( final Future< Void > future : futures )
					future.get();
				return null;
			}

			@Override
			public Void get( final long timeout, final TimeUnit unit ) throws InterruptedException, ExecutionException, TimeoutException
			{
				final long tstart = System.currentTimeMillis();
				for ( final Future< Void > future : futures )
				{
					final long telapsed = System.currentTimeMillis() - tstart;
					final long tleft = unit.toMillis( timeout ) - telapsed;
					if ( tleft <= 0 )
						throw new TimeoutException();
					future.get( tleft, TimeUnit.MILLISECONDS );
				}
				return null;
			}
		};
	}
}
