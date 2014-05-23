/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2007, Helios Development Group and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org. 
 *
 */
package org.helios.tsdb.plugins.meta.query;

import java.io.Closeable;
import java.io.IOException;
import java.util.Iterator;

/**
 * <p>Title: IMetaQueryRawIterator</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.meta.query.IMetaQueryRawIterator</code></p>
 * @param <T> The type of the raw object returned by this iterator
 */

public interface IMetaQueryRawIterator<T> extends Iterator<T>, Closeable {
	/**
	 * <p>Attempts to determine if the raw iterator has a next object to read</p>
	 * {@inheritDoc}
	 * @see java.util.Iterator#hasNext()
	 * @throws MetaQueryRawClosedException thrown if the raw data iterator is closed by another thread
	 */
	@Override
	public boolean hasNext() throws MetaQueryRawClosedException;
	
	/**
	 * <p>Attempts to read the next object from the raw iterator</p>
	 * {@inheritDoc}
	 * @see java.util.Iterator#next()
	 * @throws MetaQueryRawClosedException thrown if the raw data iterator is closed by another thread
	 */
	@Override
	public T next() throws MetaQueryRawClosedException;
	
	/**
	 * <p>Closes the raw data iterator, releasing the underlying cursors and allocated resources.</p>
	 * {@inheritDoc}
	 * @see java.io.Closeable#close()
	 */
	@Override
	public void close() throws IOException;
	
	/**
	 * Determines if this iterator is open
	 * @return true if open, false if closed
	 */
	public boolean isOpen();
	
}
