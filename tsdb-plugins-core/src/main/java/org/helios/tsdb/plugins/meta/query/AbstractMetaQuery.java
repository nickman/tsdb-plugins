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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.ObjectName;

import net.opentsdb.meta.TSMeta;

import org.helios.jmx.util.helpers.JMXHelper;

/**
 * <p>Title: AbstractMetaQuery</p>
 * <p>Description: Abstract base class for {@link IMetaQuery} concrete implementations</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.meta.query.AbstractMetaQuery</code></p>
 * @param <R> The type to be returned by the query
 */

public abstract class AbstractMetaQuery<R> implements IMetaQuery<R> {
	/** A set of allowed return type classes */
	@SuppressWarnings("unchecked")
	protected static final Set<Class<?>> allowedReturnTypes = Collections.unmodifiableSet(new HashSet<Class<?>>(Arrays.asList(
			String.class, ObjectName.class, TSMeta.class
	)));
	/** A message listing the allowed return type classes */
	protected static final String allowedReturnTypeMessage;
	
	/** The query timeout in ms. */
	protected long queryTimeout = TOTAL_TIMEOUT_DEFAULT;
	/** The max wait time for the first row in ms. */
	protected long maxWaitFirst = FIRST_TIMEOUT_DEFAULT;
	/** The max wait time for the next row in ms. */
	protected long maxWaitNext = NEXT_TIMEOUT_DEFAULT;
	/** The maximum result count limit */
	protected long maxResults = MAX_RESULTS_DEFAULT;	
	/** The result buffering queue */
	protected final ArrayBlockingQueue<R> bufferQueue;
	/** Indicates if the query is still open (i.e. still retruning results) */
	protected final AtomicBoolean open = new AtomicBoolean(false);

	
	static {
		StringBuilder b = new StringBuilder("Supported Return Types [");
		for(Class<?> allowed: allowedReturnTypes) {
			b.append(allowed.getName()).append(",");
		}
		allowedReturnTypeMessage = b.deleteCharAt(b.length()-1).append("]").toString();
	}
	
	/**
	 * Creates a new AbstractMetaQuery
	 * @param queueSize The buffer queue size
	 */
	protected AbstractMetaQuery(int queueSize) {
		bufferQueue = new ArrayBlockingQueue<R>(queueSize, false);
	}
	
	/**
	 * Creates a new AbstractMetaQuery with the default queue size ({@value IMetaQuery#QUEUE_SIZE_DEFAULT}.)
	 */
	protected AbstractMetaQuery() {
		this(QUEUE_SIZE_DEFAULT);
	}
	

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.meta.query.IMetaQuery#query(java.lang.CharSequence, java.lang.Class)
	 */
	@Override
	public CancelableIterator<R> query(CharSequence objectName,	Class<R> returnType) {
		return query(JMXHelper.objectName(objectName), returnType);
	}


	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.meta.query.IMetaQuery#query(java.lang.CharSequence, java.util.Map, java.lang.Class)
	 */
	@Override
	public CancelableIterator<R> query(CharSequence metric, Map<String, String> tags, Class<R> returnType) {
		return query(JMXHelper.objectName(metric, new Hashtable<String, String>(tags)), getReturnTypeFor(returnType));
	}
	
	/**
	 * <p>Title: ResultIterator</p>
	 * <p>Description: The cancelable iterator implementation</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.tsdb.plugins.meta.query.AbstractMetaQuery.ResultIterator</code></p>
	 */
	protected class ResultIterator implements CancelableIterator<R>, Iterator<R> {
		/** The results read count */
		protected final AtomicLong resultCount = new AtomicLong(0L);
		/** The future timestamp by when the query will timeout */
		protected final long timeoutBy = System.currentTimeMillis() + queryTimeout;
		/** The current row wait timeout in ms. */
		protected long waitTimeout = maxWaitFirst;
		
		@Override
		public Iterator<R> iterator() {
			return this;
		}

		/**
		 * {@inheritDoc}
		 * @see java.util.Iterator#hasNext()
		 */
		@Override
		public boolean hasNext() {
			return !bufferQueue.isEmpty() || open.get();
		}

		/**
		 * {@inheritDoc}
		 * @see java.util.Iterator#next()
		 */
		@Override
		public R next() {
			final long rCount = resultCount.incrementAndGet(); 
			if(rCount==1L) waitTimeout = maxWaitNext;
			R r = null;
			try {
				r = bufferQueue.poll(waitTimeout, TimeUnit.MILLISECONDS);
			} catch (InterruptedException iex) {
				throw new RuntimeException("Thread interrupted while waiting for next result", iex);
			}
			return null;
		}

		/**
		 * <p>Unimplemented operation. Does nothing.</p>
		 * {@inheritDoc}
		 * @see java.util.Iterator#remove()
		 */
		@Override
		public void remove() {
			/* No op */
		}
		
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.meta.query.IMetaQuery#timeout(long)
	 */
	@Override
	public IMetaQuery<R> timeout(long timeout) {
		queryTimeout = timeout;
		return this;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.meta.query.IMetaQuery#getTimeout()
	 */
	@Override
	public long getTimeout() {
		return queryTimeout;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.meta.query.IMetaQuery#maxWaitNext(long)
	 */
	@Override
	public IMetaQuery<R> maxWaitNext(long maxWait) {
		maxWaitNext = maxWait;
		return this;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.meta.query.IMetaQuery#getMaxWaitNext()
	 */
	@Override
	public long getMaxWaitNext() {
		return maxWaitNext;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.meta.query.IMetaQuery#maxWaitFirst(long)
	 */
	@Override
	public IMetaQuery<R> maxWaitFirst(long maxWait) {
		maxWaitFirst = maxWait;
		return this;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.meta.query.IMetaQuery#getMaxWaitFirst()
	 */
	@Override
	public long getMaxWaitFirst() {
		return maxWaitFirst;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.meta.query.IMetaQuery#maxResults(long)
	 */
	@Override
	public IMetaQuery<R> maxResults(long maxResults) {
		this.maxResults = maxResults;
		return this;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.meta.query.IMetaQuery#getMaxResults()
	 */
	@Override
	public long getMaxResults() {
		return maxResults;
	}
	
	
	/**
	 * Validates and maps the passed return type class to the actual type that will be used
	 * @param clazz The class to validate and map
	 * @return the actual type that will be returned
	 */
	@SuppressWarnings({ "cast", "unchecked" })
	protected static <R> Class<R> getReturnTypeFor(Class<R> clazz) {
		if(clazz==null) throw new IllegalArgumentException("The passed class was null");
		if(allowedReturnTypes.contains(clazz)) return (Class<R>) clazz;
		for(Class<?> allowed: allowedReturnTypes) {
			if(clazz.isAssignableFrom(allowed)) return (Class<R>) allowed;
		}
		throw new IllegalArgumentException("The passed class [" + clazz.getName() + "] is not a supported return type. " + allowedReturnTypeMessage);
	}

}
