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
package org.helios.tsdb.plugins.service;

import java.util.Map;

import net.opentsdb.core.TSDB;
import net.opentsdb.meta.Annotation;
import net.opentsdb.meta.TSMeta;
import net.opentsdb.meta.UIDMeta;
import net.opentsdb.search.SearchQuery;
import net.opentsdb.stats.StatsCollector;

import org.helios.tsdb.plugins.event.PluginType;
import org.helios.tsdb.plugins.shell.Plugin;

import com.stumbleupon.async.Deferred;

/**
 * <p>Title: ITSDBPluginService</p>
 * <p>Description: Defines the plugin service callback sites that generate events to be handled.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.service.ITSDBPluginService</code></p>
 */

public interface ITSDBPluginService {
	/**
	 * Registers a plugin instance
	 * @param plugin The plugin to register
	 */
	public void addPluginInstance(Plugin plugin);
	
	/**
	 * Initializes the service after creation
	 */
	public void initialize();
	
	/**
	 * Called by the TSD when a request for statistics collection has come in.
	 * @param statsCollector The collector used for emitting statistics
	 * @param pluginType The plugin type this collector was issued for
	 * @see net.opentsdb.search.SearchPlugin#collectStats(net.opentsdb.stats.StatsCollector)
	 */	
	public void collectStats(PluginType pluginType, StatsCollector statsCollector);
	
	/**
	 * Called any time a new data point is published
	 * @param metric The name of the metric associated with the data point
	 * @param timestamp Timestamp as a Unix epoch in seconds or milliseconds (depending on the TSD's configuration)
	 * @param value Value for the data point
	 * @param tags The metric tags
	 * @param tsuid Time series UID for the value
	 */
	public void publishDataPoint(String metric, long timestamp, double value, Map<String, String> tags, byte[] tsuid);

	/**
	 * Called any time a new data point is published
	 * @param metric The name of the metric associated with the data point
	 * @param timestamp Timestamp as a Unix epoch in seconds or milliseconds (depending on the TSD's configuration)
	 * @param value Value for the data point
	 * @param tags The metric tags
	 * @param tsuid Time series UID for the value
	 */
	public void publishDataPoint(String metric, long timestamp, long value, Map<String, String> tags, byte[] tsuid);
	
	/**
	 * Deletes an annotation
	 * @param annotation The annotation to delete
	 * @see net.opentsdb.search.SearchPlugin#deleteAnnotation(net.opentsdb.meta.Annotation)
	 */
	public void deleteAnnotation(Annotation annotation);
	

	/**
	 * Indexes an annotation
	 * @param annotation The annotation to index
	 * @see net.opentsdb.search.SearchPlugin#indexAnnotation(net.opentsdb.meta.Annotation)
	 */
	public void indexAnnotation(Annotation annotation);	

	/**
	 * Called when we need to remove a timeseries meta object from the engine Note: Unique Document ID = TSUID
	 * @param tsMeta The TSMeta name to delete
	 * @see net.opentsdb.search.SearchPlugin#deleteTSMeta(java.lang.String)
	 */
	public void deleteTSMeta(String tsMeta);
	
	/**
	 * Indexes a timeseries metadata object in the search engine Note: Unique Document ID = TSUID
	 * @param tsMeta The TSMeta to index
	 * @see net.opentsdb.search.SearchPlugin#indexTSMeta(net.opentsdb.meta.TSMeta)
	 */
	public void indexTSMeta(TSMeta tsMeta);	
	
	/**
	 * Indexes a UID metadata object for a metric, tagk or tagv Note: Unique Document ID = UID and the Type "TYPEUID"
	 * @param uidMeta The UIDMeta to index
	 * @see net.opentsdb.search.SearchPlugin#indexUIDMeta(net.opentsdb.meta.UIDMeta)
	 */
	public void indexUIDMeta(UIDMeta uidMeta);	

	/**
	 * Called when we need to remove a UID meta object from the engine Note: Unique Document ID = UID and the Type "TYPEUID"
	 * @param uidMeta The UIDMeta to delete
	 * @see net.opentsdb.search.SearchPlugin#deleteUIDMeta(net.opentsdb.meta.UIDMeta)
	 */
	public void deleteUIDMeta(UIDMeta uidMeta);

	/**
	 * Executes a very basic search query, returning the results in the SearchQuery object passed in.
	 * @param searchQuery The query to execute against the search engine
	 * @param toReturn The deferred to complete on query execution completion (or error).
	 * @see net.opentsdb.search.SearchPlugin#executeQuery(net.opentsdb.search.SearchQuery)
	 */
	public void executeQuery(SearchQuery searchQuery, Deferred<SearchQuery> toReturn);
	
	/**
	 * Signals the plugin service to shutdown
	 * @param deferredToAdd Add this deferred to the plugin service shutdown
	 * @return the core plugin service deferred shutdown indicator
	 */
	public Deferred<Object> shutdown(Deferred<Object> deferredToAdd); 
	
	/**
	 * Returns the provided TSDB (which might be null if it has not been initialized yet)
	 * @return the provided TSDB
	 */
	public TSDB getTSDB();
	
}
	
	
	
