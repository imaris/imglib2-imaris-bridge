/*-
 * #%L
 * Expose the Imaris XT interface as an ImageJ2 service backed by ImgLib2.
 * %%
 * Copyright (C) 2019 - 2021 Bitplane AG
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package com.bitplane.xt.options;

import com.bitplane.xt.ImarisDataset;
import java.util.function.BiConsumer;
import org.scijava.optional.Options;
import org.scijava.optional.Values;

/**
 * Specify whether {@link ImarisDataset} should be opened as read-only (vs
 * modifiable).
 *
 * @author Tobias Pietzsch
 */
public interface ReadOnlyOptions< T > extends Options< T >
{
	/**
	 * Open the dataset as read-only (vs modifiable).
	 * <p>
	 * Modifying methods like {@link ImarisDataset#setCalibration} will throw
	 * {@code UnsupportedOperationException}. Changes to pixel values are
	 * possible but will not be written back to Imaris and are forgotten when
	 * the respective pixel is evicted from cache.
	 * <p>
	 * By default, all datasets are modifiable.
	 */
	default T readOnly()
	{
		return setValue( "readOnly", true );
	}


	interface Val extends Values
	{
		default void forEach( BiConsumer< String, Object > action )
		{
			action.accept( "readOnly", readOnly() );
		}

		default boolean readOnly()
		{
			return getValueOrDefault( "readOnly", false );
		}
	}
}
