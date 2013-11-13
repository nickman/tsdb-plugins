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

import java.io.Closeable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

import javax.sql.DataSource;

import net.opentsdb.meta.Annotation;
import net.opentsdb.meta.TSMeta;
import net.opentsdb.meta.UIDMeta;
import net.opentsdb.search.SearchQuery;

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
	 * Binds and batches a UIDMeta indexing operation
	 * @param uidMeta The UIDMeta to index
	 * @param ps The prepared statement to bind against and batch
	 * @throws SQLException thrown on binding or batching failures
	 */
	public void bindUIDMeta(UIDMeta uidMeta, PreparedStatement ps) throws SQLException;	
	
	/**
	 * Executes the UID indexing batches
	 * @param conn The connection to execute the batches with
	 */
	public void executeUIDBatches(Connection conn);

	// ===================================================================================================
	// Object Unmarshalling (i.e. ResultSet to collection of Objects)
	// ===================================================================================================

	
	/**
	 * Returns a collection of {@link UIDMeta}s read from the passed {@link ResultSet}.
	 * @param rset The result set to read from
	 * @return a [possibly empty] collection of UIDMetas
	 */
	public List<UIDMeta> readUIDMetas(ResultSet rset);
	
	/**
	 * Returns a collection of {@link TSMeta}s read from the passed {@link ResultSet}.
	 * @param rset The result set to read from
	 * @return a [possibly empty] collection of TSMetas
	 */
	public List<TSMeta> readTSMetas(ResultSet rset);
	
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
	 * Stores a new annotation
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
	 * Stores the passed TSMeta
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
	
	/**
	 * Executes the passed search query and returns a result set of the rows that will be used to read the objects to be returned.
	 * If the returned result set is null, it will be assumed the matches were zero
	 * @param conn The connection to search on
	 * @param query The search query to execute
	 * @param closeables A set for the impl to put closeables into so the caller can close them when complete
	 * @return A result set of the rows that will be used to read the objects to be returned.
	 */
	public ResultSet executeSearch(Connection conn, SearchQuery query, Set<Closeable> closeables);

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
	
}
