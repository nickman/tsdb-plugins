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
package test.net.opentsdb.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.opentsdb.core.Query;
import net.opentsdb.core.WritableDataPoints;
import net.opentsdb.meta.Annotation;
import net.opentsdb.meta.TSMeta;
import net.opentsdb.meta.UIDMeta;
import net.opentsdb.search.SearchQuery;
import net.opentsdb.stats.Histogram;
import net.opentsdb.stats.StatsCollector;
import net.opentsdb.uid.NoSuchUniqueName;
import net.opentsdb.uid.UniqueId.UniqueIdType;
import net.opentsdb.utils.Config;

import org.hbase.async.HBaseClient;
import org.hbase.async.HBaseException;

import com.stumbleupon.async.Deferred;


/**
 * <p>Title: ITSDB</p>
 * <p>Description: Test support interface for {@link net.opentsdb.core.TSDB}</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>test.net.opentsdb.core.ITSDB</code></p>
 */

public interface ITSDB {

	/**
	 * Should be called immediately after construction to initialize plugins and
	 * objects that rely on such. It also moves most of the potential exception
	 * throwing code out of the constructor so TSDMain can shutdown clients and
	 * such properly.
	 * @param init_rpcs Whether or not to initialize RPC plugins as well
	 * @throws RuntimeException if the plugin path could not be processed
	 * @throws IllegalArgumentException if a plugin could not be initialized
	 * @since 2.0
	 */
	public void initializePlugins(boolean init_rpcs);

	/** 
	 * Returns the configured HBase client 
	 * @return The HBase client
	 * @since 2.0 
	 */
	public HBaseClient getClient();

	/** 
	 * Getter that returns the configuration object
	 * @return The configuration object
	 * @since 2.0 
	 */
	public Config getConfig();

	/**
	 * Attempts to find the name for a unique identifier given a type
	 * @param type The type of UID
	 * @param uid The UID to search for
	 * @return The name of the UID object if found
	 * @throws IllegalArgumentException if the type is not valid
	 * @throws NoSuchUniqueId if the UID was not found
	 * @since 2.0
	 */
	public Deferred<String> getUidName(UniqueIdType type, byte[] uid);

	/**
	 * Attempts to find the UID matching a given name
	 * @param type The type of UID
	 * @param name The name to search for
	 * @throws IllegalArgumentException if the type is not valid
	 * @throws NoSuchUniqueName if the name was not found
	 * @since 2.0
	 */
	public byte[] getUID(UniqueIdType type, String name);

	/**
	 * Verifies that the data and UID tables exist in HBase and optionally the
	 * tree and meta data tables if the user has enabled meta tracking or tree
	 * building
	 * @return An ArrayList of objects to wait for
	 * @throws TableNotFoundException
	 * @since 2.0
	 */
	public Deferred<ArrayList<Object>> checkNecessaryTablesExist();

	/** Number of cache hits during lookups involving UIDs. */
	public int uidCacheHits();

	/** Number of cache misses during lookups involving UIDs. */
	public int uidCacheMisses();

	/** Number of cache entries currently in RAM for lookups involving UIDs. */
	public int uidCacheSize();

	/**
	 * Collects the stats and metrics tracked by this instance.
	 * @param collector The collector to use.
	 */
	public void collectStats(StatsCollector collector);

	/** Returns a latency histogram for Put RPCs used to store data points. */
	public Histogram getPutLatencyHistogram();

	/** Returns a latency histogram for Scan RPCs used to fetch data points.  */
	public Histogram getScanLatencyHistogram();

	/**
	 * Returns a new {@link Query} instance suitable for this TSDB.
	 */
	public Query newQuery();

	/**
	 * Returns a new {@link WritableDataPoints} instance suitable for this TSDB.
	 * <p>
	 * If you want to add a single data-point, consider using {@link #addPoint}
	 * instead.
	 */
	public WritableDataPoints newDataPoints();

	/**
	 * Adds a single integer value data point in the TSDB.
	 * @param metric A non-empty string.
	 * @param timestamp The timestamp associated with the value.
	 * @param value The value of the data point.
	 * @param tags The tags on this series.  This map must be non-empty.
	 * @return A deferred object that indicates the completion of the request.
	 * The {@link Object} has not special meaning and can be {@code null} (think
	 * of it as {@code Deferred<Void>}). But you probably want to attach at
	 * least an errback to this {@code Deferred} to handle failures.
	 * @throws IllegalArgumentException if the timestamp is less than or equal
	 * to the previous timestamp added or 0 for the first timestamp, or if the
	 * difference with the previous timestamp is too large.
	 * @throws IllegalArgumentException if the metric name is empty or contains
	 * illegal characters.
	 * @throws IllegalArgumentException if the tags list is empty or one of the
	 * elements contains illegal characters.
	 * @throws HBaseException (deferred) if there was a problem while persisting
	 * data.
	 */
	public Deferred<Object> addPoint(String metric, long timestamp, long value,
			Map<String, String> tags);

	/**
	 * Adds a double precision floating-point value data point in the TSDB.
	 * @param metric A non-empty string.
	 * @param timestamp The timestamp associated with the value.
	 * @param value The value of the data point.
	 * @param tags The tags on this series.  This map must be non-empty.
	 * @return A deferred object that indicates the completion of the request.
	 * The {@link Object} has not special meaning and can be {@code null} (think
	 * of it as {@code Deferred<Void>}). But you probably want to attach at
	 * least an errback to this {@code Deferred} to handle failures.
	 * @throws IllegalArgumentException if the timestamp is less than or equal
	 * to the previous timestamp added or 0 for the first timestamp, or if the
	 * difference with the previous timestamp is too large.
	 * @throws IllegalArgumentException if the metric name is empty or contains
	 * illegal characters.
	 * @throws IllegalArgumentException if the value is NaN or infinite.
	 * @throws IllegalArgumentException if the tags list is empty or one of the
	 * elements contains illegal characters.
	 * @throws HBaseException (deferred) if there was a problem while persisting
	 * data.
	 * @since 1.2
	 */
	public Deferred<Object> addPoint(String metric, long timestamp,
			double value, Map<String, String> tags);

	/**
	 * Adds a single floating-point value data point in the TSDB.
	 * @param metric A non-empty string.
	 * @param timestamp The timestamp associated with the value.
	 * @param value The value of the data point.
	 * @param tags The tags on this series.  This map must be non-empty.
	 * @return A deferred object that indicates the completion of the request.
	 * The {@link Object} has not special meaning and can be {@code null} (think
	 * of it as {@code Deferred<Void>}). But you probably want to attach at
	 * least an errback to this {@code Deferred} to handle failures.
	 * @throws IllegalArgumentException if the timestamp is less than or equal
	 * to the previous timestamp added or 0 for the first timestamp, or if the
	 * difference with the previous timestamp is too large.
	 * @throws IllegalArgumentException if the metric name is empty or contains
	 * illegal characters.
	 * @throws IllegalArgumentException if the value is NaN or infinite.
	 * @throws IllegalArgumentException if the tags list is empty or one of the
	 * elements contains illegal characters.
	 * @throws HBaseException (deferred) if there was a problem while persisting
	 * data.
	 */
	public Deferred<Object> addPoint(String metric, long timestamp,
			float value, Map<String, String> tags);

	/**
	 * Forces a flush of any un-committed in memory data.
	 * <p>
	 * For instance, any data point not persisted will be sent to HBase.
	 * @return A {@link Deferred} that will be called once all the un-committed
	 * data has been successfully and durably stored.  The value of the deferred
	 * object return is meaningless and unspecified, and can be {@code null}.
	 * @throws HBaseException (deferred) if there was a problem sending
	 * un-committed data to HBase.  Please refer to the {@link HBaseException}
	 * hierarchy to handle the possible failures.  Some of them are easily
	 * recoverable by retrying, some are not.
	 */
	public Deferred<Object> flush() throws HBaseException;

	/**
	 * Gracefully shuts down this TSD instance.
	 * <p>
	 * The method must call {@code shutdown()} on all plugins as well as flush the
	 * compaction queue.
	 * @return A {@link Deferred} that will be called once all the un-committed
	 * data has been successfully and durably stored, and all resources used by
	 * this instance have been released.  The value of the deferred object
	 * return is meaningless and unspecified, and can be {@code null}.
	 * @throws HBaseException (deferred) if there was a problem sending
	 * un-committed data to HBase.  Please refer to the {@link HBaseException}
	 * hierarchy to handle the possible failures.  Some of them are easily
	 * recoverable by retrying, some are not.
	 */
	public Deferred<Object> shutdown();

	/**
	 * Given a prefix search, returns a few matching metric names.
	 * @param search A prefix to search.
	 */
	public List<String> suggestMetrics(String search);

	/**
	 * Given a prefix search, returns matching metric names.
	 * @param search A prefix to search.
	 * @param max_results Maximum number of results to return.
	 * @since 2.0
	 */
	public List<String> suggestMetrics(String search, int max_results);

	/**
	 * Given a prefix search, returns a few matching tag names.
	 * @param search A prefix to search.
	 */
	public List<String> suggestTagNames(String search);

	/**
	 * Given a prefix search, returns matching tagk names.
	 * @param search A prefix to search.
	 * @param max_results Maximum number of results to return.
	 * @since 2.0
	 */
	public List<String> suggestTagNames(String search, int max_results);

	/**
	 * Given a prefix search, returns a few matching tag values.
	 * @param search A prefix to search.
	 */
	public List<String> suggestTagValues(String search);

	/**
	 * Given a prefix search, returns matching tag values.
	 * @param search A prefix to search.
	 * @param max_results Maximum number of results to return.
	 * @since 2.0
	 */
	public List<String> suggestTagValues(String search, int max_results);

	/**
	 * Discards all in-memory caches.
	 * @since 1.1
	 */
	public void dropCaches();

	/**
	 * Attempts to assign a UID to a name for the given type
	 * Used by the UniqueIdRpc call to generate IDs for new metrics, tagks or 
	 * tagvs. The name must pass validation and if it's already assigned a UID,
	 * this method will throw an error with the proper UID. Otherwise if it can
	 * create the UID, it will be returned
	 * @param type The type of uid to assign, metric, tagk or tagv
	 * @param name The name of the uid object
	 * @return A byte array with the UID if the assignment was successful
	 * @throws IllegalArgumentException if the name is invalid or it already 
	 * exists
	 * @2.0
	 */
	public byte[] assignUid(String type, String name);

	/** @return the name of the UID table as a byte array for client requests */
	public byte[] uidTable();

	/** @return the name of the data table as a byte array for client requests */
	public byte[] dataTable();

	/** @return the name of the tree table as a byte array for client requests */
	public byte[] treeTable();

	/** @return the name of the meta table as a byte array for client requests */
	public byte[] metaTable();

	/**
	 * Index the given timeseries meta object via the configured search plugin
	 * @param meta The meta data object to index
	 * @since 2.0
	 */
	public void indexTSMeta(TSMeta meta);

	/**
	 * Delete the timeseries meta object from the search index
	 * @param tsuid The TSUID to delete
	 * @since 2.0
	 */
	public void deleteTSMeta(String tsuid);

	/**
	 * Index the given UID meta object via the configured search plugin
	 * @param meta The meta data object to index
	 * @since 2.0
	 */
	public void indexUIDMeta(UIDMeta meta);

	/**
	 * Delete the UID meta object from the search index
	 * @param meta The UID meta object to delete
	 * @since 2.0
	 */
	public void deleteUIDMeta(UIDMeta meta);

	/**
	 * Index the given Annotation object via the configured search plugin
	 * @param note The annotation object to index
	 * @since 2.0
	 */
	public void indexAnnotation(Annotation note);

	/**
	 * Delete the annotation object from the search index
	 * @param note The annotation object to delete
	 * @since 2.0
	 */
	public void deleteAnnotation(Annotation note);

	/**
	 * Processes the TSMeta through all of the trees if configured to do so
	 * @param meta The meta data to process
	 * @since 2.0
	 */
	public Deferred<Boolean> processTSMetaThroughTrees(TSMeta meta);

	/**
	 * Executes a search query using the search plugin
	 * @param query The query to execute
	 * @return A deferred object to wait on for the results to be fetched
	 * @throws IllegalStateException if the search plugin has not been enabled or
	 * configured
	 * @since 2.0
	 */
	public Deferred<SearchQuery> executeSearch(SearchQuery query);

}
