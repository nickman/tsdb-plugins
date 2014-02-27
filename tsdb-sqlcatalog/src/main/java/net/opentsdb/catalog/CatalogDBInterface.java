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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import net.opentsdb.meta.Annotation;
import net.opentsdb.meta.TSMeta;
import net.opentsdb.meta.UIDMeta;
import net.opentsdb.search.SearchQuery;
import net.opentsdb.uid.UniqueId.UniqueIdType;

import org.helios.tsdb.plugins.event.TSDBSearchEvent;
import org.helios.tsdb.plugins.service.PluginContext;

import com.stumbleupon.async.Deferred;


/**
 * <p>Title: CatalogDBInterface</p>
 * <p>Description: Defines a SQL catalog DB initializer</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>net.opentsdb.catalog.CatalogDBInterface</code></p>
 */

public interface CatalogDBInterface {

	
	/*
	 * Remote JDBC URL
	 * jdbc:h2:tcp://localhost:9092/mem:tsdb
	 */
	
//	/** The custom map key to specify the version of the object */
//	public static final String VERSION_KEY = "v";
//	/** The custom map key to specify the DB pk of the object */
//	public static final String PK_KEY = "p";
//	/** The custom map key to specify the TSD_METRIC pk of a TSMeta object */
//	public static final String TSMETA_METRIC_KEY = "t";
	
	// ========================================================================================
	//	FQN Sequence Related Constants
	// ========================================================================================
	
	/** The config property name for the increment on the FQN ID Sequence */
	public static final String DB_FQN_SEQ_INCR = "helios.search.catalog.seq.fqn.incr";
	/** The default increment on the FQN ID Sequence */
	public static final int DEFAULT_DB_FQN_SEQ_INCR = 50;
	
	/** The config property name for the increment on the FQN TagPair ID Sequence */
	public static final String DB_TP_FQN_SEQ_INCR = "helios.search.catalog.seq.fqntp.incr";
	/** The default increment on the FQN TagPair ID Sequence */
	public static final int DEFAULT_DB_TP_FQN_SEQ_INCR = DEFAULT_DB_FQN_SEQ_INCR * 4;

	/** The config property name for the increment on the Annotation ID Sequence */
	public static final String DB_ANN_SEQ_INCR = "helios.search.catalog.seq.ann.incr";
	/** The default increment on the Annotation ID Sequence */
	public static final int DEFAULT_DB_ANN_SEQ_INCR = 50;

	// ========================================================================================
	//	Text Search and Indexing Related Constants
	// ========================================================================================
	
	/** The config property name to disable text search indexing */
	public static final String DB_DISABLE_TEXT_INDEXING = "helios.search.catalog.text.indexing.disable";
	/** The default indicator for disabling the text search indexing */
	public static final boolean DEFAULT_DB_DISABLE_TEXT_INDEXING = false;

	// ========================================================================================
	//	TSDB Sync Polling Related Constants
	// ========================================================================================
	
	/** The config property name for the polling period (in seconds) of the TSDB Sync Process */
	public static final String TSDB_SYNC_PERIOD = "helios.search.catalog.tsdb.sync.period";
	/** The default indicator for disabling the scheduling of syncs (but still allowing manual syncs) */
	public static final long DEFAULT_TSDB_SYNC_PERIOD = 60;
	
	/** The config property name for disabling all invocations of the TSDB Sync Process */
	public static final String TSDB_DISABLE_SYNC = "helios.search.catalog.tsdb.sync.disable";
	/** The default indicator for disabling all invocations of the TSDB Sync Process */
	public static final boolean DEFAULT_TSDB_DISABLE_SYNC = false;
	
	/**
	 * Returns the created data source
	 * @return a connection pool data source
	 */
	public DataSource getDataSource();
	
	/**
	 * Initializes a JDBC connection to prep any special requirements for event queue processing
	 * @param conn The connection to configure
	 */
	public void initConnection(Connection conn);
	
	// ===================================================================================================
	// Connection Properties
	// ===================================================================================================
	
	/**
	 * Sets a locally scoped property on the passed connection
	 * @param conn The connection to set the property on
	 * @param key The property key
	 * @param value The property value
	 */
	public void setConnectionProperty(Connection conn, String key, String value);
	
	/**
	 * Retrieves a locally scoped property on the passed connection
	 * @param conn The connection to set the property on
	 * @param key The property key
	 * @param defaultValue value returned if no value is bound to the name, or the value is null
	 * @return The property value
	 */
	public String getConnectionProperty(Connection conn, String key, String defaultValue);
	
	/**
	 * Retrieves a locally scoped property on the passed connection
	 * @param conn The connection to set the property on
	 * @param key The property key
	 * @return The property value
	 */
	public String getConnectionProperty(Connection conn, String key);

	
	
	// ===================================================================================================
	// Lifecycle Ops
	// ===================================================================================================
	/**
	 * Runs the initialization routine
	 * @param pluginContext The plugin context
	 */
	public void initialize(PluginContext pluginContext);
	
	/**
	 * Terminates the database resources
	 */
	public void shutdown();

	// ===================================================================================================
	// SQL Batch Control and Event Processing
	// ===================================================================================================
	
	/**
	 * Processes a batch of events
	 * @param conn The connection to execute the events against
	 * @param events An ordered batch of events to process
	 */
	public void processEvents(Connection conn, Set<TSDBSearchEvent> events);
	
	/**
	 * Executes the batched statements in the passed statement and validates the results.
	 * @param ps The statement to execute batch on
	 * @throws SQLException thrown on a batch execution error
	 */
	public void executeBatch(PreparedStatement ps) throws SQLException;
	
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
	 * Binds and batches a UIDMeta indexing operation
	 * @param uidMeta The UIDMeta to index
	 * @param ps The prepared statement to bind against and batch
	 * @throws SQLException thrown on binding or batching failures
	 */
	public void bindUIDMeta(UIDMeta uidMeta, PreparedStatement ps) throws SQLException;
	
	/**
	 * Binds and batches a UIDMeta update operation
	 * @param uidMeta The UIDMeta to index
	 * @param ps The prepared statement to bind against and batch
	 * @throws SQLException thrown on binding or batching failures
	 */
	public void bindUIDMetaUpdate(UIDMeta uidMeta, PreparedStatement ps) throws SQLException;
	
	/**
	 * Executes the UID indexing batches
	 * @param conn The connection to execute the batches with
	 */
	public void executeUIDBatches(Connection conn);
	

	
	// ===================================================================================================
	// Object Exists (INSERT or UPDATE ?)
	// ===================================================================================================
	
	/**
	 * Determines if the passed tsmeta is already stored
	 * @param conn The connection to query on
	 * @param tsMeta The tsmeta to verify
	 * @return true if the passed tsmeta is already stored, false otherwise
	 */
	public boolean exists(Connection conn, TSMeta tsMeta);

	/**
	 * Determines if the passed tsmeta is already stored
	 * @param tsMeta The tsmeta to verify
	 * @return true if the passed tsmeta is already stored, false otherwise
	 */
	public boolean exists(TSMeta tsMeta);

	/**
	 * Determines if the passed uidmeta is already stored
	 * @param conn The connection to query on
	 * @param uidMeta The uidmeta to verify
	 * @return true if the passed uidmeta is already stored, false otherwise
	 */
	public boolean exists(Connection conn, UIDMeta uidMeta);
	
	/**
	 * Determines if the passed uidmeta is already stored
	 * @param uidMeta The uidmeta to verify
	 * @return true if the passed uidmeta is already stored, false otherwise
	 */
	public boolean exists(UIDMeta uidMeta);
	

	/**
	 * Determines if the passed annotation is already stored
	 * @param conn The connection to query on
	 * @param annotation The annotation to verify
	 * @return true if the passed annotation is already stored, false otherwise
	 */
	public boolean exists(Connection conn, Annotation annotation);
	
	/**
	 * Determines if the passed annotation is already stored
	 * @param annotation The annotation to verify
	 * @return true if the passed annotation is already stored, false otherwise
	 */
	public boolean exists(Annotation annotation);
	

	// ===================================================================================================
	// Object Unmarshalling (i.e. ResultSet to collection of Objects)
	// ===================================================================================================

	/**
	 * Returns a collection of {@link UIDMeta}s read from the passed {@link ResultSet}.
	 * @param rset The result set to read from
	 * @param uidType The UIDMeta type name we're reading
	 * @return a [possibly empty] collection of UIDMetas
	 */
	public List<UIDMeta> readUIDMetas(ResultSet rset, String uidType);
	
	
	/**
	 * Returns a collection of {@link UIDMeta}s read from the passed {@link ResultSet}.
	 * @param rset The result set to read from
	 * @param uidType The UIDMeta type we're reading
	 * @return a [possibly empty] collection of UIDMetas
	 */
	public List<UIDMeta> readUIDMetas(ResultSet rset, UniqueIdType uidType);
	
	/**
	 * Returns a collection of shallow (no UIDMetas for the metric or tags) {@link TSMeta}s read from the passed {@link ResultSet}.
	 * @param rset The result set to read from
	 * @return a [possibly empty] collection of TSMetas
	 */
	public List<TSMeta> readTSMetas(ResultSet rset);
	
	/**
	 * Returns a collection of {@link TSMeta}s read from the passed {@link ResultSet}.
	 * @param rset The result set to read from
	 * @param includeUIDs If true, the metric and tags will be loaded, otherwise shallow TSMetas will be returned
	 * @return a [possibly empty] collection of TSMetas
	 */
	public List<TSMeta> readTSMetas(ResultSet rset, boolean includeUIDs);
	
	/**
	 * Retrieves a list of TSMetas in JSON format
	 * @param byFqn Indicates if the query will be by FQNID, otherwise by TSUID.
	 * @param deep true to retrieve "deep" TSMetas with fully resolved metric and tag UIDs, false for "shallow" TSMetas.
	 * @param ids A string of comma separated ids
	 * @return The JSON containing the retrieved TSMetas
	 */
	public String getTSMetasJSON(boolean byFqn, boolean deep, String ids);
	
	
	/**
	 * Returns a collection of {@link Annotation}s read from the passed {@link ResultSet}.
	 * @param rset The result set to read from
	 * @return a [possibly empty] collection of Annotations
	 */
	public List<Annotation> readAnnotations(ResultSet rset);
	
	
	// ========================================================================================
	//	Object INSERT template builders for UID insert SQL
	// ========================================================================================

	/**
	 * Returns the indexing SQL for a TagK UIDMeta
	 * @return the indexing SQL for a TagK UIDMeta
	 */
	public String getUIDMetaTagKIndexSQL();
	
	/**
	 * Returns the indexing SQL for a TagV UIDMeta
	 * @return the indexing SQL for a TagV UIDMeta
	 */
	public String getUIDMetaTagVIndexSQL();

	/**
	 * Returns the indexing SQL for a Metric UIDMeta
	 * @return the indexing SQL for a Metric UIDMeta
	 */
	public String getUIDMetaMetricIndexSQL();
	
	/**
	 * Returns the update SQL for a Metric UIDMeta
	 * @return the update SQL for a Metric UIDMeta
	 */
	public String getUIDMetaMetricUpdateSQL();
	/**
	 * Returns the update SQL for a TagV UIDMeta
	 * @return the update SQL for a TagV UIDMeta
	 */
	public String getUIDMetaTagVUpdateSQL();
	/**
	 * Returns the update SQL for a TagK UIDMeta
	 * @return the update SQL for a TagK UIDMeta
	 */
	public String getUIDMetaTagKUpdateSQL();
	
	
	// ==================================================================================================
	//  Object Deletion 
	// ==================================================================================================
	
	/**
	 * Deletes a UIDMeta
	 * @param conn The connection to use
	 * @param uidMeta The UIDMeta to delete
	 */
	public void deleteUIDMeta(Connection conn, UIDMeta uidMeta);
	
	/**
	 * Deletes a TSMeta
	 * @param conn The connection to use
	 * @param tsUid The TSUid of the TSMeta to delete
	 */
	public void deleteTSMeta(Connection conn, String tsUid);
	
	/**
	 * Deletes an Annotation
	 * @param conn The connection to use
	 * @param annotation The annotation to delete
	 */
	public void deleteAnnotation(Connection conn, Annotation annotation);		
	
	
	// ========================================================================================
	//	Object Processing
	// ========================================================================================
	
	/**
	 * Saves or updates a UIDMeta
	 * @param conn The connection to save on
	 * @param uidMeta The UIDMeta to save or update
	 */
	public void processUIDMeta(Connection conn, UIDMeta uidMeta);
	
	/**
	 * Saves or updates the passed annotation
	 * @param conn The connection to write to
	 * @param annotation The annotation to save
	 */
	public void processAnnotation(Connection conn, Annotation annotation);	
	
	/**
	 * Saves a tag UIDMeta pair
	 * @param batchUidPairs A set of UIDMeta tag pair keys that have already been saved in this batch.
	 * @param conn The connection to write to
	 * @param tagPair A pair of UIDMetas where [0] is the key and [1] is the value
	 * @return the pair key
	 */
	public String processUIDMetaPair(final Set<String> batchUidPairs, Connection conn, UIDMeta[] tagPair);	
	
	/**
	 * Saves or updates the passed TSMeta
	 * @param batchUidPairs A set of UIDMeta tag pair keys that have already been saved in this batch.
	 * @param conn The connection to write to
	 * @param tsMeta The TSMeta to save
	 */
	public void processTSMeta(final Set<String> batchUidPairs, Connection conn, TSMeta tsMeta);
	
	/**
	 * Indicates if NaNs should be passed to the DB as nulls
	 * @return true to pass NaNs as nulls, false if the DB supports storing NaNs.
	 */
	public boolean isNaNToNull();
	
	// ========================================================================================
	//	Search Impl.
	// ========================================================================================
	
//	/**
//	 * Executes the passed search query and returns a result set of the rows that will be used to read the objects to be returned.
//	 * If the returned result set is null, it will be assumed the matches were zero
//	 * @param conn The connection to search on
//	 * @param query The search query to execute
//	 * @param closeables A set for the impl to put closeables into so the caller can close them when complete
//	 * @return A result set of the rows that will be used to read the objects to be returned.
//	 */
//	public ResultSet executeSearch(Connection conn, SearchQuery query, Set<Closeable> closeables);
	
	/**
	 * Executes a text search on the passed connection using the passed search query as search parameters 
	 * @param conn The connection to search on
	 * @param query The query parameters
	 * @return a list of matching objects
	 */
	public List<?> executeSearch(Connection conn, SearchQuery query);

	/**
	 * Executes a search query against this catalog
	 * @param query The OpenTSDB SearchQuery
	 * @param result The deferred to write the result (or error) to
	 * @return The deferred
	 */
	public Deferred<SearchQuery> executeQuery(SearchQuery query, Deferred<SearchQuery> result);
	
	/**
	 * Purges all tables in the store
	 */
	public void purge();
	
	/**
	 * A cleanup operation that flushes all meta objects from the store to the search plugin
	 * @param dumpOnly If true, only dumps the TSMeta data to the logger, otherwise processes the synch
	 * @return The number of TSMetas written
	 * @throws Exception throw on any error
	 */
	public long synchronizeFromStore(boolean dumpOnly) throws Exception;	
	
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
	 * Creates a realtime connection usage snippet
	 * @return A message showing the created, free and in use connections
	 */
	public String getConnectionUsage();
	
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
	 * Sets a new TSDB sync period in seconds and ticks the last sync timestamp to current for all tables
	 * @param newPeriod The TSDB sync period in seconds
	 */
	public void setTSDBSyncPeriodAndHighwater(final long newPeriod);
	
	/**
	 * Sets the TSDB Sync period in seconds. 
	 * If this op modifies the existing value, a schedule change will be triggered.
	 * This may stop a started schedule, or start a stopped schedule. 
	 * @param newPeriod the TSDB Sync period in seconds.
	 */
	public void setTSDBSyncPeriod(final long newPeriod);
	
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
	
	// ========================================================================================================
	//    SynQueue Operations, defined here since they may be DB specific.
	// ========================================================================================================
	
	/**
	 * Records a UIDMeta sync from the DB Catalog failure by inserting or updating the UIDMeta's row in TSD_LASTSYNC_FAILS.
	 * @param uidMeta The UIDMeta that failed sync
	 * @param tsdbTable The tsdb table for the passed UIDMeta
	 */
	public void recordSyncQueueFailure(UIDMeta uidMeta, TSDBTable tsdbTable);
	
	/**
	 * Records a TSMeta sync from the DB Catalog failure by inserting or updating the TSMeta's row in TSD_LASTSYNC_FAILS.
	 * @param tsMeta The TSMeta that failed sync
	 */
	public void recordSyncQueueFailure(TSMeta tsMeta);
	
	/**
	 * Records an Annotation sync from the DB Catalog failure by inserting or updating the Annotation's row in TSD_LASTSYNC_FAILS.
	 * @param ann The Annotation that failed sync
	 */
	public void recordSyncQueueFailure(Annotation ann);
	
	/**
	 * Clears a UIDMeta sync failure from TSD_LASTSYNC_FAILS.
	 * @param uidMeta The UIDMeta to clear the sync failure for
	 * @param tsdbTable The tsdb table for the passed UIDMeta
	 */
	public void clearSyncQueueFailure(UIDMeta uidMeta, TSDBTable tsdbTable);
	
	/**
	 * Clears a TSMeta sync failure from TSD_LASTSYNC_FAILS.
	 * @param tsMeta The TSMeta to clear the sync failure for
	 */
	public void clearSyncQueueFailure(TSMeta tsMeta);
	
	/**
	 * Clears an Annotation sync failure from TSD_LASTSYNC_FAILS.
	 * @param ann The Annotation to clear the sync failure for
	 */
	public void clearSyncQueueFailure(Annotation ann);
	
}
