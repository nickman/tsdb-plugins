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
package org.helios.tsdb.plugins.async;

import net.opentsdb.meta.Annotation;
import net.opentsdb.meta.TSMeta;
import net.opentsdb.meta.UIDMeta;
import net.opentsdb.search.SearchQuery;

import com.stumbleupon.async.Deferred;

/**
 * <p>Title: ISearchEventDispatcher</p>
 * <p>Description: Defines a dispatcher that accepts parameterized callbacks from the {@link net.opentsdb.search.SearchPlugin} shell plugins
 * through the {@link org.helios.tsdb.plugins.event.TSDBEventDispatcher}, converts them into {@link org.helios.tsdb.plugins.event.TSDBEvent}s 
 * and dispatches them asynchronously.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.async.ISearchEventDispatcher</code></p>
 */

public interface ISearchEventDispatcher {
	/**
	 * Executes a very basic search query, returning the results in the SearchQuery object passed in.
	 * Note that only one search event handler instance can handle search query results.
	 * @param searchQuery The query to execute against the search engine
	 * @return The search results
	 */
	public Deferred<SearchQuery> executeQuery(SearchQuery searchQuery);

	/**
	 * Indexes an annotation object Note: Unique Document ID = TSUID and Start Time
	 * @param annotation The annotation to index
	 */
	public void indexAnnotation(Annotation annotation);
	
	/**
	 * Called to remove an annotation object from the index Note: Unique Document ID = TSUID and Start Time
	 * @param annotation The annotation to remove
	 */
	public void deleteAnnotation(Annotation annotation);
	
	/**
	 * Indexes a timeseries metadata object in the search engine Note: Unique Document ID = TSUID
	 * @param tsMeta The TSMeta to index
	 */
	public void indexTSMeta(TSMeta tsMeta);

	/**
	 * Called when we need to remove a timeseries meta object from the engine Note: Unique Document ID = TSUID
	 * @param tsMeta The hex encoded TSUID to remove
	 */
	public void deleteTSMeta(String tsMeta);
	
	/**
	 * Indexes a UID metadata object for a metric, tagk or tagv Note: Unique Document ID = UID and the Type "TYPEUID"
	 * @param uidMeta The UIDMeta to index
	 */
	public void indexUIDMeta(UIDMeta uidMeta);
	

	/**
	 * Called when we need to remove a UID meta object from the engine Note: Unique Document ID = UID and the Type "TYPEUID"
	 * @param uidMeta The UIDMeta to remove
	 */
	public void deleteUIDMeta(UIDMeta uidMeta);

}
