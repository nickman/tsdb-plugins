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

import java.sql.ResultSet;
import java.util.NoSuchElementException;

/**
 * <p>Title: AbstractIndexProvidingIterator</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>net.opentsdb.catalog.AbstractIndexProvidingIterator</code></p>
 * @param <T> The type of the object being iterated
 */

public abstract class AbstractIndexProvidingIterator<T> implements IndexProvidingIterator<T> {
	/** The result set driving the iterator */
	private final ResultSet rset;
	/** The retained "next" result */
	private boolean hasnext = true;
	/** The current "nexted" uitem */
	private T currentItem = null;

	/**
	 * Creates a new AbstractIndexProvidingIterator
	 * @param rset The result set driving the iterator
	 */
	public AbstractIndexProvidingIterator(ResultSet rset) {
		this.rset = rset;
	}

	
	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.IndexProvidingIterator#pushBack()
	 */
	@Override
	public void pushBack() {
		currentItem = null;
		try {			
			if(!rset.previous()) {
				throw new RuntimeException("ResultSet was on first row");
			}			
		} catch (Exception ex) {
			throw new RuntimeException("Failed to pushback result set", ex);
		}
	}

	/**
	 * {@inheritDoc}
	 * @see java.util.Iterator#hasNext()
	 */
	@Override
	public boolean hasNext() {
		try {
			currentItem = null;
			hasnext = rset.next();
			return hasnext;
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	/**
	 * {@inheritDoc}
	 * @see java.util.Iterator#next()
	 */
	@Override
	public T next() {
		if(!hasnext) throw new NoSuchElementException();
		currentItem = build();
		return currentItem;
	}
	
	/**
	 * Builds the next item from the passed result set
	 * @return the built item
	 */
	protected abstract T build();
	
	/**
	 * Returns the index of the current item in scope
	 * @param t The current item in scope
	 * @return the index of the current item in scope
	 */
	protected abstract Object getIndex(T t);

	/**
	 * {@inheritDoc}
	 * @see java.util.Iterator#remove()
	 */
	@Override
	public void remove() {
		throw new UnsupportedOperationException("remove() not supported in this iterator");
		
	}

	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.IndexProvidingIterator#getIndex()
	 */
	@Override
	public Object getIndex() throws NoSuchElementException {
		if(!hasnext || currentItem==null) throw new NoSuchElementException();
		return getIndex(currentItem);
	}

}
