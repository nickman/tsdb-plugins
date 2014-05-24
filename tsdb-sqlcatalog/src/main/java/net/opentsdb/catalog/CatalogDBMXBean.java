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
package net.opentsdb.catalog;

import java.util.Map;

import javax.management.MXBean;

import org.helios.tsdb.plugins.handlers.logging.LoggerManager;

/**
 * <p>Title: CatalogDBMXBean</p>
 * <p>Description: JMX MXBean interface for catalog db instrumentation</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>net.opentsdb.catalog.CatalogDBMXBean</code></p>
 */

@MXBean
public interface CatalogDBMXBean extends LoggerManager {
	/** The metric name insert counter */
	public static final String METRIC_INSERT_CNT = "metric-insert"; 
	/** The metric name update counter */
	public static final String METRIC_UPDATE_CNT = "metric-update"; 
	/** The tag key insert counter */
	public static final String TAGK_INSERT_CNT = "tagk-insert"; 
	/** The tag key name update counter */
	public static final String TAGK_UPDATE_CNT = "tagk-update"; 
	/** The tag value insert counter */
	public static final String TAGV_INSERT_CNT = "tagv-insert"; 
	/** The tag value name update counter */
	public static final String TAGV_UPDATE_CNT = "tagv-update";
	
	/** The annotation insert counter */
	public static final String ANN_INSERT_CNT = "ann-insert"; 
	/** The annotation update counter */
	public static final String ANN_UPDATE_CNT = "ann-update";
    
	/** The tsmeta insert counter */
	public static final String TSMETA_INSERT_CNT = "tsmeta-insert"; 
	/** The tsmeta update counter */
	public static final String TSMETA_UPDATE_CNT = "tsmeta-update";
	
	
	/**
	 * Returns the current number of UIDMeta metrics
	 * @return the current number of UIDMeta metrics
	 */
	public int getMetricCount();
	
	/**
	 * Returns the current number of UIDMeta tag keys
	 * @return the current number of UIDMeta tag keys
	 */
	public int getTagKCount();
	
	/**
	 * Returns the current number of UIDMeta tag values
	 * @return the current number of UIDMeta tag values
	 */
	public int getTagVCount();
	
	/**
	 * Returns the current number of TSMeta time series 
	 * @return the current number of TSMeta time series
	 */
	public int getTSMetaCount();
	
	/**
	 * Returns the current number of annotations 
	 * @return the current number of annotations
	 */
	public int getAnnotationCount();
	
	/**
	 * Returns the number of TSMetas in HBase
	 * @return the number of TSMetas in HBase
	 */
	public int getHBaseTSMetaCount();
	
	
	
	/**
	 * Returns the cummlative number of metric name inserts
	 * @return the cummlative number of metric name inserts
	 */
	public long getMetricInsertCount();
	
	/**
	 * Returns the cummlative number of metric name updates
	 * @return the cummlative number of metric name updates
	 */
	public long getMetricUpdateCount();
	
	/**
	 * Returns the cummlative number of tag key inserts
	 * @return the cummlative number of tag key inserts
	 */
	public long getTagKInsertCount();
	
	/**
	 * Returns the cummlative number of tag key updates
	 * @return the cummlative number of tag key updates
	 */
	public long getTagKUpdateCount();
	
	/**
	 * Returns the cummlative number of tag value inserts
	 * @return the cummlative number of tag value inserts
	 */
	public long getTagVInsertCount();
	
	/**
	 * Returns the cummlative number of tag value updates
	 * @return the cummlative number of tag value updates
	 */
	public long getTagVUpdateCount();
	
	/**
	 * Returns the cummlative number of tsmeta inserts
	 * @return the cummlative number of tsmeta inserts
	 */
	public long getTSMetaInsertCount();
	
	/**
	 * Returns the cummlative number of tsmeta updates
	 * @return the cummlative number of tsmeta updates
	 */
	public long getTSMetaUpdateCount();
	
	/**
	 * Returns the cummlative number of annotation inserts
	 * @return the cummlative number of annotation inserts
	 */
	public long getAnnotationInsertCount();
	
	/**
	 * Returns the cummlative number of annotation updates
	 * @return the cummlative number of annotation updates
	 */
	public long getAnnotationUpdateCount();
	
	/**
	 * Returns the URL of the connected database
	 * @return the URL of the connected database
	 */
	public String getURL();

	/**
	 * Returns the username of the database user
	 * @return the username of the database user
	 */
	public String getUserName();

	/**
	 * Returns the driver name 
	 * @return the driver name 
	 */
	public String getDriverName();
	
	/**
	 * Returns the driver version
	 * @return the driver version
	 */
	public String getDriverVersion();	
	
	
	/**
	 * Retrieves the name of this database product.
	 * @return the name of this database product
	 */
	public String getDatabaseProductName();
	
	/**
	 * Retrieves the version number of this database product.
	 * @return the version number name of this database product
	 */
	public String getDatabaseProductVersion();
	
	/**
	 * Executes an OpenTSDB SearchQuery and returns the results in JSON form
	 * @param type The SearchQuery type expressed in the name of the {@link net.opentsdb.search.SearchQuery.SearchType} enum member.
	 * @param query The query expression
	 * @param limit Sets the limit to the number of results
	 * @param startIndex The index of the results to start returning at
	 * @return the JSON representation of the search results
	 */
	public String search(String type, String query, int limit, int startIndex);
	
	/**
	 * A cleanup operation that flushes all meta objects from the store to the search plugin
	 * @return The number of TSMetas written
	 * @throws Exception throw on any error
	 */
	public long synchronizeFromStore() throws Exception;	

	
	/**
	 * Returns the code source path for this plugin (i.e. where it was loaded from)
	 * @return the code source path for this plugin
	 */
	public String getPluginPath();
	
	/**
	 * Executes the passed SQL statement and returns the results as JSON
	 * @param includeMeta true to include meta-data, false to exclude it
	 * @param sqlText The SQL statement to execute
	 * @return the JSON representing the result of the SQL statement
	 */
	public String executeSQLForJson(boolean includeMeta, String sqlText);
	
	/**
	 * Executes the passed SQL statement and returns the results as JSON
	 * @param includeMeta true to include meta-data, false to exclude it
	 * @param maxRows The maximum number of rows to return. A value of <b><code>0</code></p> or less means all rows.
	 * @param startAt The row number to start at, the first row being 0.
	 * @param sqlText The SQL statement to execute
	 * @return the JSON representing the result of the SQL statement
	 */
	public String executeSQLForJson(boolean includeMeta, int maxRows, int startAt, String sqlText);
	
	/**
	 * Returns the catalog data source's total created connections
	 * @return the catalog data source's total created connections
	 */
	public int getCreatedConnections();

	/**
	 * Returns the catalog data source's total free connections
	 * @return the catalog data source's total free connections
	 */
	public int getFreeConnections();
	
	/**
	 * Returns the catalog data source's total leased connections
	 * @return the catalog data source's total leased connections
	 */
	public int getLeasedConnections();
	
	/**
	 * Indicates if text indexing is disabled
	 * @return true if text indexing is disabled, false otherwise
	 */
	public boolean isTextIndexingDisabled();	
	
	/**
	 * Returns the TSDB Sync period in seconds
	 * @return the TSDB Sync period in seconds
	 */
	public long getTSDBSyncPeriod();
	
	/**
	 * Sets the TSDB Sync period in seconds. 
	 * If this op modifies the existing value, a schedule change will be triggered.
	 * This may stop a started schedule, or start a stopped schedule. 
	 * @param newPeriod the TSDB Sync period in seconds.
	 */
	public void setTSDBSyncPeriod(final long newPeriod);
	
	/**
	 * Sets a new TSDB sync period in seconds and ticks the last sync timestamp to current for all tables
	 * @param newPeriod The TSDB sync period in seconds
	 */
	public void setTSDBSyncPeriodAndHighwater(final long newPeriod);	

	/**
	 * Returns the number of pending Sync ops we're waiting on 
	 * @return the number of pending Sync ops we're waiting on
	 */
	public long getPendingSynchOps();	
	
	/**
	 * Returns a map of UIDMeta names with the UnqiueIdType name as the value
	 * @param tsuid The TSMeta TSUID to get the UIDMeta names for
	 * @return a map of UIDMeta names with the UnqiueIdType name as the value
	 */
	public Map<String, String> getNamesForUIDs(String tsuid);
	
	/**
	 * Retrieves a list of TSMetas in JSON format
	 * @param byFqn Indicates if the query will be by FQNID, otherwise by TSUID.
	 * @param deep true to retrieve "deep" TSMetas with fully resolved metric and tag UIDs, false for "shallow" TSMetas.
	 * @param ids A string of comma separated ids
	 * @return The JSON containing the retrieved TSMetas
	 */
	public String getTSMetasJSON(boolean byFqn, boolean deep, String ids);
	
	
	
	

}
