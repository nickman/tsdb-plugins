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
import java.util.Map;

import javax.management.ObjectName;

import net.opentsdb.meta.TSMeta;

/**
 * <p>Title: IMetaQuery</p>
 * <p>Description: Defines an OpenTSDB fully qualified metric query processor.</p>
 * <p>The premise of <b><code>IMetaQuery</code></b> is that queries are expressed in the form
 * of a JMX  <b><code>{@link ObjectName}</code></b> since OpenTSDB fully qualified metrics
 * are isomorphically identical to ObjectNames:<ul>
 * 	<li><b>ObjectName:</b>&nbsp;<code>&lt;domain&gt;:&lt;key1&gt;=&lt;value1&gt;,&lt;key2&gt;=&lt;value2&gt;,&lt;key<b><i>n</i></b>&gt;=&lt;value<b><i>n</i></b>&gt;</code></li>
 * 	<li><b>OpenTSDB Metric:</b>&nbsp;<code>&lt;metric&gt;:&lt;tag-key1&gt;=&lt;tag-value1&gt;,&lt;tag-key2&gt;=&lt;tag-value2&gt;,&lt;tag-key<b><i>n</i></b>&gt;=&lt;tag-value<b><i>n</i></b>&gt;</code></li>
 * </ul>
 * <p>More or less...</p>
 * <p><b><code>IMetaQuery</code></b> implementations accept a string in {@link ObjectName} format (or an actual {@link ObjectName}), parse the contents and build a
 * query native to the implemtation's query target. The query target could be HBase directly, HBase through a TSDB instance, or a secondary store such as <b><code>SQLCatalog</code></b>.</p>
 * <p>In some cases, in order to allow for some wildcarding unsupported by {@link ObjectName}s, future versions will support aliases serve to represent a subset of regex expression tokens.</p>
 * <p>Results can be returned as {@link TSMeta}s, {@link ObjectName}s or simple strings.</p>
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.meta.query.IMetaQuery</code></p>
 * @param <R>  The type of the returned objects
 * @param <T> The type of the raw objects returned by the MetaQuery's impl and converted to instances of R
 */

public interface IMetaQuery<R, T> {
	/** The default timeout for the entire query (ms)*/
	public static long TOTAL_TIMEOUT_DEFAULT = 60000;
	/** The default timeout while waiting for the first result (ms) */
	public static long FIRST_TIMEOUT_DEFAULT = 200;	
	/** The default timeout while waiting for the next result (ms) */
	public static long NEXT_TIMEOUT_DEFAULT = 200;
	/** The default maximum results count */
	public static long MAX_RESULTS_DEFAULT = Long.MAX_VALUE;
	/** The default queue size where results are buffered for the iterator */
	public static int QUEUE_SIZE_DEFAULT = 100;
	/** The default buffer queue put timeout (ms) */
	public static long PUT_TIMEOUT_DEFAULT = 100;
	
	/**
	 * Executes a query to find all matching TSDB metrics matching the pattern expressed in the supplied objectname.
	 * Implements the default timeouts.
	 * @param objectName The pattern expression to match
	 * @param returnType The desired type of the returned format (string, objectname or TSMeta)
	 * @return A CancelableIterator to iterate through the results
	 */
	public CancelableIterator<R> query(CharSequence objectName, Class<R> returnType);
	
	/**
	 * Executes a query to find all matching TSDB metrics matching the pattern expressed in the supplied objectname.
	 * Implements the default timeouts.
	 * @param objectName The pattern expression to match
	 * @param returnType The desired type of the returned format (string, objectname or TSMeta)
	 * @return A CancelableIterator to iterate through the results
	 */
	public CancelableIterator<R> query(ObjectName objectName, Class<R> returnType);
	
	/**
	 * Executes a query to find all matching TSDB metrics matching the pattern expressed in the supplied objectname.
	 * Implements the default timeouts.
	 * @param metric The pattern expression to match against the metric
	 * @param tags A map of tags to match against
	 * @param returnType The desired type of the returned format (string, objectname or TSMeta)
	 * @return A CancelableIterator to iterate through the results
	 */
	public CancelableIterator<R> query(CharSequence metric, Map<String, String> tags, Class<R> returnType);

	/**
	 * Indicates if the query is open, in which case it may still return more results.
	 * Otherwise it is closed and any resources have been deallocated.
	 * @return true if open, false otherwise
	 */
	public boolean isOpen();
	
	/**
	 * Converts a raw object provided by the query impl to the requested type 
	 * @param t The raw object
	 * @return the converted object
	 */
	public R convert(T t);
	
	
	/**
	 * Sets the query timeout in ms.
	 * @param timeout The timeout in ms.
	 * @return this IMetaQuery
	 */
	public IMetaQuery<R, T> timeout(long timeout);
	
	/**
	 * Returns the query timeout in ms.
	 * @return the query timeout in ms.
	 */
	public long getTimeout();

	/**
	 * Sets the query next row max wait time in ms.
	 * @param maxWait next row max wait time in ms.
	 * @return this IMetaQuery
	 */
	public IMetaQuery<R, T> maxWaitNext(long maxWait);
	
	/**
	 * Returns the query next row max wait time in ms.
	 * @return the query next row max wait time in ms.
	 */
	public long getMaxWaitNext();
	
	/**
	 * Sets the query first row max wait time in ms.
	 * @param maxWait first row max wait time in ms.
	 * @return this IMetaQuery
	 */
	public IMetaQuery<R, T> maxWaitFirst(long maxWait);

	/**
	 * Returns the query first row max wait time in ms.
	 * @return the query first row max wait time in ms.
	 */
	public long getMaxWaitFirst();
	
	/**
	 * Sets the bufer queue put timeout, used when the meta-query processes a returned
	 * raw object, converts it to the requested return type and attempts to enqueue it
	 * on the buffer queue. This is intended to prevent a slow consumer from keeping
	 * a meta-query data resource open indefinitely.
	 * @param timeout the buffer queue put timeout in ms.
	 * @return this IMetaQuery
	 */
	public IMetaQuery<R, T> bufferQueuePutTimeout(long timeout);

	/**
	 * Returns the buffer queue put timeout in ms.
	 * @return the buffer queue put timeout in ms.
	 */
	public long getBufferQueuePutTimeout();
	
	
	/**
	 * Sets the max results limit
	 * @param maxResults the max results limit
	 * @return this IMetaQuery
	 */
	public IMetaQuery<R, T> maxResults(long maxResults);
	
	/**
	 * Returns the max results limit
	 * @return the max results limit
	 */
	public long getMaxResults();
	
	/**
	 * Sets the buffering queue size
	 * @param queueSize the buffering queue size
	 * @return this IMetaQuery
	 */
	public IMetaQuery<R, T> queueSize(int queueSize);
	
	/**
	 * Returns the buffering queue size
	 * @return the buffering queue size
	 */
	public int getQueueSize();
	
}
