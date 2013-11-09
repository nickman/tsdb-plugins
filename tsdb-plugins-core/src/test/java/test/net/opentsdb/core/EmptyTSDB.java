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
import net.opentsdb.uid.UniqueId.UniqueIdType;
import net.opentsdb.utils.Config;

import org.hbase.async.HBaseClient;
import org.hbase.async.HBaseException;

import com.stumbleupon.async.Deferred;

/**
 * <p>Title: EmptyTSDB</p>
 * <p>Description: Test support class with concrete methods to supply templates for transformed {@link net.opentsdb.core.TSDB} instances.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>test.net.opentsdb.core.EmptyTSDB</code></p>
 */

public class EmptyTSDB implements ITSDB {

	/**
	 * Creates a new EmptyTSDB
	 */
	public EmptyTSDB() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * {@inheritDoc}
	 * @see test.net.opentsdb.core.ITSDB#initializePlugins(boolean)
	 */
	@Override
	public void initializePlugins(boolean init_rpcs) {
		

	}

	/**
	 * {@inheritDoc}
	 * @see test.net.opentsdb.core.ITSDB#getClient()
	 */
	@Override
	public HBaseClient getClient() {
		
		return null;
	}

	/**
	 * {@inheritDoc}
	 * @see test.net.opentsdb.core.ITSDB#getConfig()
	 */
	@Override
	public Config getConfig() {
		
		return null;
	}

	/**
	 * {@inheritDoc}
	 * @see test.net.opentsdb.core.ITSDB#getUidName(net.opentsdb.uid.UniqueId.UniqueIdType, byte[])
	 */
	@Override
	public Deferred<String> getUidName(UniqueIdType type, byte[] uid) {
		
		return null;
	}

	/**
	 * {@inheritDoc}
	 * @see test.net.opentsdb.core.ITSDB#getUID(net.opentsdb.uid.UniqueId.UniqueIdType, java.lang.String)
	 */
	@Override
	public byte[] getUID(UniqueIdType type, String name) {
		
		return null;
	}

	/**
	 * {@inheritDoc}
	 * @see test.net.opentsdb.core.ITSDB#checkNecessaryTablesExist()
	 */
	@Override
	public Deferred<ArrayList<Object>> checkNecessaryTablesExist() {
		
		return null;
	}

	/**
	 * {@inheritDoc}
	 * @see test.net.opentsdb.core.ITSDB#uidCacheHits()
	 */
	@Override
	public int uidCacheHits() {
		
		return 0;
	}

	/**
	 * {@inheritDoc}
	 * @see test.net.opentsdb.core.ITSDB#uidCacheMisses()
	 */
	@Override
	public int uidCacheMisses() {
		
		return 0;
	}

	/**
	 * {@inheritDoc}
	 * @see test.net.opentsdb.core.ITSDB#uidCacheSize()
	 */
	@Override
	public int uidCacheSize() {
		
		return 0;
	}

	/**
	 * {@inheritDoc}
	 * @see test.net.opentsdb.core.ITSDB#collectStats(net.opentsdb.stats.StatsCollector)
	 */
	@Override
	public void collectStats(StatsCollector collector) {
		

	}

	/**
	 * {@inheritDoc}
	 * @see test.net.opentsdb.core.ITSDB#getPutLatencyHistogram()
	 */
	@Override
	public Histogram getPutLatencyHistogram() {
		
		return null;
	}

	/**
	 * {@inheritDoc}
	 * @see test.net.opentsdb.core.ITSDB#getScanLatencyHistogram()
	 */
	@Override
	public Histogram getScanLatencyHistogram() {
		
		return null;
	}

	/**
	 * {@inheritDoc}
	 * @see test.net.opentsdb.core.ITSDB#newQuery()
	 */
	@Override
	public Query newQuery() {
		
		return null;
	}

	/**
	 * {@inheritDoc}
	 * @see test.net.opentsdb.core.ITSDB#newDataPoints()
	 */
	@Override
	public WritableDataPoints newDataPoints() {
		
		return null;
	}

	/**
	 * {@inheritDoc}
	 * @see test.net.opentsdb.core.ITSDB#addPoint(java.lang.String, long, long, java.util.Map)
	 */
	@Override
	public Deferred<Object> addPoint(String metric, long timestamp, long value,
			Map<String, String> tags) {
		
		return null;
	}

	/**
	 * {@inheritDoc}
	 * @see test.net.opentsdb.core.ITSDB#addPoint(java.lang.String, long, double, java.util.Map)
	 */
	@Override
	public Deferred<Object> addPoint(String metric, long timestamp,
			double value, Map<String, String> tags) {
		
		return null;
	}

	/**
	 * {@inheritDoc}
	 * @see test.net.opentsdb.core.ITSDB#addPoint(java.lang.String, long, float, java.util.Map)
	 */
	@Override
	public Deferred<Object> addPoint(String metric, long timestamp,
			float value, Map<String, String> tags) {
		
		return null;
	}

	/**
	 * {@inheritDoc}
	 * @see test.net.opentsdb.core.ITSDB#flush()
	 */
	@Override
	public Deferred<Object> flush() throws HBaseException {
		
		return null;
	}

	/**
	 * {@inheritDoc}
	 * @see test.net.opentsdb.core.ITSDB#shutdown()
	 */
	@Override
	public Deferred<Object> shutdown() {
		
		return null;
	}

	/**
	 * {@inheritDoc}
	 * @see test.net.opentsdb.core.ITSDB#suggestMetrics(java.lang.String)
	 */
	@Override
	public List<String> suggestMetrics(String search) {
		
		return null;
	}

	/**
	 * {@inheritDoc}
	 * @see test.net.opentsdb.core.ITSDB#suggestMetrics(java.lang.String, int)
	 */
	@Override
	public List<String> suggestMetrics(String search, int max_results) {
		
		return null;
	}

	/**
	 * {@inheritDoc}
	 * @see test.net.opentsdb.core.ITSDB#suggestTagNames(java.lang.String)
	 */
	@Override
	public List<String> suggestTagNames(String search) {
		
		return null;
	}

	/**
	 * {@inheritDoc}
	 * @see test.net.opentsdb.core.ITSDB#suggestTagNames(java.lang.String, int)
	 */
	@Override
	public List<String> suggestTagNames(String search, int max_results) {
		
		return null;
	}

	/**
	 * {@inheritDoc}
	 * @see test.net.opentsdb.core.ITSDB#suggestTagValues(java.lang.String)
	 */
	@Override
	public List<String> suggestTagValues(String search) {
		
		return null;
	}

	/**
	 * {@inheritDoc}
	 * @see test.net.opentsdb.core.ITSDB#suggestTagValues(java.lang.String, int)
	 */
	@Override
	public List<String> suggestTagValues(String search, int max_results) {
		
		return null;
	}

	/**
	 * {@inheritDoc}
	 * @see test.net.opentsdb.core.ITSDB#dropCaches()
	 */
	@Override
	public void dropCaches() {
		

	}

	/**
	 * {@inheritDoc}
	 * @see test.net.opentsdb.core.ITSDB#assignUid(java.lang.String, java.lang.String)
	 */
	@Override
	public byte[] assignUid(String type, String name) {
		
		return null;
	}

	/**
	 * {@inheritDoc}
	 * @see test.net.opentsdb.core.ITSDB#uidTable()
	 */
	@Override
	public byte[] uidTable() {
		
		return null;
	}

	/**
	 * {@inheritDoc}
	 * @see test.net.opentsdb.core.ITSDB#dataTable()
	 */
	@Override
	public byte[] dataTable() {
		
		return null;
	}

	/**
	 * {@inheritDoc}
	 * @see test.net.opentsdb.core.ITSDB#treeTable()
	 */
	@Override
	public byte[] treeTable() {
		
		return null;
	}

	/**
	 * {@inheritDoc}
	 * @see test.net.opentsdb.core.ITSDB#metaTable()
	 */
	@Override
	public byte[] metaTable() {
		
		return null;
	}

	/**
	 * {@inheritDoc}
	 * @see test.net.opentsdb.core.ITSDB#indexTSMeta(net.opentsdb.meta.TSMeta)
	 */
	@Override
	public void indexTSMeta(TSMeta meta) {
		

	}

	/**
	 * {@inheritDoc}
	 * @see test.net.opentsdb.core.ITSDB#deleteTSMeta(java.lang.String)
	 */
	@Override
	public void deleteTSMeta(String tsuid) {
		

	}

	/**
	 * {@inheritDoc}
	 * @see test.net.opentsdb.core.ITSDB#indexUIDMeta(net.opentsdb.meta.UIDMeta)
	 */
	@Override
	public void indexUIDMeta(UIDMeta meta) {
		

	}

	/**
	 * {@inheritDoc}
	 * @see test.net.opentsdb.core.ITSDB#deleteUIDMeta(net.opentsdb.meta.UIDMeta)
	 */
	@Override
	public void deleteUIDMeta(UIDMeta meta) {
		

	}

	/**
	 * {@inheritDoc}
	 * @see test.net.opentsdb.core.ITSDB#indexAnnotation(net.opentsdb.meta.Annotation)
	 */
	@Override
	public void indexAnnotation(Annotation note) {
		

	}

	/**
	 * {@inheritDoc}
	 * @see test.net.opentsdb.core.ITSDB#deleteAnnotation(net.opentsdb.meta.Annotation)
	 */
	@Override
	public void deleteAnnotation(Annotation note) {
		

	}

	/**
	 * {@inheritDoc}
	 * @see test.net.opentsdb.core.ITSDB#processTSMetaThroughTrees(net.opentsdb.meta.TSMeta)
	 */
	@Override
	public Deferred<Boolean> processTSMetaThroughTrees(TSMeta meta) {
		
		return null;
	}

	/**
	 * {@inheritDoc}
	 * @see test.net.opentsdb.core.ITSDB#executeSearch(net.opentsdb.search.SearchQuery)
	 */
	@Override
	public Deferred<SearchQuery> executeSearch(SearchQuery query) {
		
		return null;
	}

}
