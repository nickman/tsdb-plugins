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
package net.opentsdb.meta;

import java.util.Map;
import java.util.Set;

import com.stumbleupon.async.Deferred;

/**
 * <p>Title: MetricsMetaAPI</p>
 * <p>Description: Defines a proposed OpenTSDB metrics meta-data access API</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>net.opentsdb.meta.MetricsMetaAPI</code></p>
 * <p>NOTES: <ul>
 * 	<li>Read Only</li>
 * 	<li>No meta requiring extended reads, such as {@link net.opentsdb.meta.TSMeta#getLastReceived()} or {@link net.opentsdb.meta.TSMeta#getTotalDatapoints()}</li>
 * </ul></p>
 */

public interface MetricsMetaAPI {
	

	/**
	 * Returns the tag keys associated with the passed metric name.
	 * Wildcards will be honoured on metric names and tag keys.
	 * @param queryOptions The query options for this call
	 * @param metric The metric name to match
	 * @param tagKeys The tag keys to match
	 * @return A deferred set of matching UIDMetas
	 */
	public Deferred<Set<UIDMeta>> getTagKeysFor(QueryOptions queryOptions, String metric, String...tagKeys);
	
	/**
	 * Returns the associated metric names (metric UIDs) for the passed tag keys.
	 * Wildcards will be honoured on tag keys.
	 * @param queryOptions The query options for this call
	 * @param tagKeys The tag keys to match
	 * @return A deferred set of matching UIDMetas
	 */
	public Deferred<Set<UIDMeta>> getMetricNamesFor(QueryOptions queryOptions, String...tagKeys);
	
	/**
	 * Returns the associated metric names (metric UIDs) for the passed tag pairs.
	 * Wildcards will be honoured on metric names and tag keys.
	 * @param queryOptions The query options for this call
	 * @param tags The tag pairs to match
	 * @return A deferred set of matching UIDMetas
	 */
	public Deferred<Set<UIDMeta>> getMetricNamesFor(QueryOptions queryOptions, Map<String, String> tags);
	
	/**
	 * Returns the TSMetas matching the passed metric name and tags
	 * @param queryOptions The query options for this call
	 * @param overflow true to return TSMetas that match the passed criteria 
	 * but which have additional tags beyond the supplied ones, false to return exact matches only 
	 * @param metricName The metric name to match
	 * @param tags The tag pairs to match
	 * @return A deferred set of matching TSMetas 
	 */
	public Deferred<Set<TSMeta>> getTSMetas(QueryOptions queryOptions, boolean overflow, String metricName, Map<String, String> tags);
	
	

	
	/**
	 * Evaluates the passed TSUIDEXPR expression and returns the matches.
	 * Wildcards will be honoured on metric names, tag keys and tag values.
	 * @param expressions The TSUIDEXPR expressions to evaluate
	 * @param queryOptions The query options for this call
	 * @return the result object in the format specified
	 */
	public Deferred<Set<TSMeta>> evaluateExpression(QueryOptions queryOptions, String...expressions);
	
}
