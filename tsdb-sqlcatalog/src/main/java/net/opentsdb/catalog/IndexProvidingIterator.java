/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2014, Helios Development Group and individual contributors
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
package net.opentsdb.catalog;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * <p>Title: IndexProvidingIterator</p>
 * <p>Description: An iterator for TSDB objects that provides the most recently "nexted" object's index</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>net.opentsdb.catalog.IndexProvidingIterator</code></p>
 * @param <T> The object type being iterated
 */

public interface IndexProvidingIterator<T> extends Iterator<T> {
	/**
	 * Returns the index of the most recently "nexted" value
	 * @return the object index
	 * @throws NoSuchElementException if the "next" op was not called
	 */
	public Object getIndex() throws NoSuchElementException;
	
	/**
	 * If the iterator end has not been reached, calling this will reverse the most recent "next"
	 */
	public void pushBack();
}
