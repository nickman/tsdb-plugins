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
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import javax.sql.DataSource;

import net.opentsdb.catalog.datasource.CatalogDataSource;
import net.opentsdb.catalog.h2.H2Support;
import net.opentsdb.catalog.h2.json.JSONMapSupport;
import net.opentsdb.catalog.sequence.LocalSequenceCache;
import net.opentsdb.catalog.syncqueue.SyncQueueProcessor;
import net.opentsdb.core.TSDB;
import net.opentsdb.meta.Annotation;
import net.opentsdb.meta.TSMeta;
import net.opentsdb.meta.UIDMeta;
import net.opentsdb.search.SearchQuery;
import net.opentsdb.uid.UniqueId;
import net.opentsdb.uid.UniqueId.UniqueIdType;

import org.helios.tsdb.plugins.event.TSDBSearchEvent;
import org.helios.tsdb.plugins.service.PluginContext;
import org.helios.tsdb.plugins.util.ConfigurationHelper;
import org.helios.tsdb.plugins.util.JMXHelper;
import org.helios.tsdb.plugins.util.SystemClock;
import org.helios.tsdb.plugins.util.SystemClock.ElapsedTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.stumbleupon.async.Deferred;

/**
 * <p>Title: AbstractDBCatalog</p>
 * <p>Description: Base abstract class for implementing concrete DB catalogs for different JDBC supporting databases.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>net.opentsdb.catalog.AbstractDBCatalog</code></p>
 */

public abstract class AbstractDBCatalog implements CatalogDBInterface, CatalogDBMXBean {
	/** Instance logger */
	protected Logger log = LoggerFactory.getLogger(getClass());
	/** The constructed datasource */
	protected DataSource dataSource = null;
	/** The data source initializer */
	protected CatalogDataSource cds = null;
	/** The parent TSDB */
	protected TSDB tsdb = null;
	/** The plugin context */
	protected PluginContext pluginContext = null;
	/** The extracted TSDB config properties */
	protected Properties extracted = null;
	/** Indicates if the SyncQueue poller is enabled */
	protected boolean syncQueuePollerEnabled = false;
	/** The sync queue processor instance */
	protected SyncQueueProcessor syncQueueProcessor = null;
	

	// ========================================================================================
	//	The batched prepared statements
	// ========================================================================================
	
	/** batched ps for tag key inserts */
	protected PreparedStatement uidMetaTagKIndexPs = null;
	/** batched ps for tag key updates */
	protected PreparedStatement uidMetaTagKUpdatePs = null;
	/** batched ps for tag value inserts */
	protected PreparedStatement uidMetaTagVIndexPs = null;
	/** batched ps for tag value updates */
	protected PreparedStatement uidMetaTagVUpdatePs = null;	
	/** batched ps for metric name inserts */
	protected PreparedStatement uidMetaMetricIndexPs = null;  
	/** batched ps for metric name updates */
	protected PreparedStatement uidMetaMetricUpdatePs = null;
	/** batched ps for tag pair inserts */
	protected PreparedStatement uidMetaTagPairPs = null;
	/** batched ps for fqn inserts */
	protected PreparedStatement tsMetaFqnPs = null;
	/** batched ps for fqn updates */
	protected PreparedStatement tsMetaFqnUpdatePs = null;
	
	/** batched ps for FQN tag pair inserts */
	protected PreparedStatement uidMetaTagPairFQNPs = null;
	/** batched ps for annotation inserts */
	protected PreparedStatement annotationsPs = null;
	/** batched ps for annotation updates */
	protected PreparedStatement annotationsUpdatePs = null;   

	// ========================================================================================
	//	The local sequence managers
	// ========================================================================================
	/** The sequence for the FQN PK */
	protected LocalSequenceCache fqnSequence = null; // FQN_SEQ
	/** The sequence for the FQN Tag Pairs PK */
	protected LocalSequenceCache fqnTpSequence = null; // FQN_TP_SEQ
	/** The sequence for the Annotation PK */
	protected LocalSequenceCache annSequence = null; // ANN_SEQ

	// ========================================================================================
	//	Some informational database meta-data for the JMX interface
	// ========================================================================================
	
	/** The database JDBC URL */
	protected String dbUrl = null;
	/** The database JDBC User Name */
	protected String dbUser = null;
	/** The database JDBC Driver name */
	protected String dbDriverName = null;
	/** The database JDBC Driver version */
	protected String dbDriverVersion = null;
	/** The database product name */
	protected String dbName = null;
	/** The database product version */
	protected String dbVersion = null;
	
	
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

	/** The config property name for the increment on the Sync Queue ID Sequence */
	public static final String DB_SYNCQ_SEQ_INCR = "helios.search.catalog.seq.syncq.incr";
	/** The default increment on the Annotation ID Sequence */
	public static final int DEFAULT_DB_SYNCQ_SEQ_INCR = 50;
	
	/** The config property name for the enablement of the Sync Queue polling processor */
	public static final String DB_SYNCQ_POLLER_ENABLED = "helios.search.catalog.syncq.enabled";
	/** The default enablement of the Sync Queue polling processor */
	public static final boolean DEFAULT_DB_SYNCQ_POLLER_ENABLED = true;
	
	// ========================================================================================
	//	Object COUNT and EXISTS SQL
	// ========================================================================================
	/** The SQL template for verification of whether a UIDMeta has been saved or not */
	public static final String UID_EXISTS_SQL = "SELECT COUNT(*) FROM TSD_%s WHERE XUID = ?";
	/** The SQL for verification of whether a UIDMeta pair has been saved or not */
	public static final String UID_PAIR_EXISTS_SQL = "SELECT COUNT(*) FROM  TSD_TAGPAIR WHERE XUID = ?";
	/** The SQL for verification of whether a TSMeta has been saved or not */
	public static final String TSUID_EXISTS_SQL = "SELECT COUNT(*) FROM TSD_TSMETA WHERE TSUID = ?";
	/** The SQL for verification of whether an Annotation has been saved or not */
	public static final String ANNOTATION_EXISTS_SQL = "SELECT COUNT(*) FROM TSD_ANNOTATION A WHERE START_TIME = ? AND (FQNID IS NULL OR EXISTS (SELECT FQNID FROM TSD_TSMETA T WHERE T.FQNID = A.FQNID  AND TSUID = ?))";
	/** The SQL to retrieve the ANNID for a given annotation */
	public static final String GET_ANNOTATION_ID_SQL = "SELECT ANNID FROM TSD_ANNOTATION A WHERE START_TIME = ? AND (FQNID IS NULL OR EXISTS (SELECT FQNID FROM TSD_TSMETA T WHERE T.FQNID = A.FQNID  AND TSUID = ?))";
	
	
	
	// ========================================================================================
	//	Object INSERT and UPDATE SQL
	// ========================================================================================
	/** The UIDMeta indexing SQL template */	
	public static final String UID_INDEX_SQL_TEMPLATE = "INSERT INTO %s (XUID,VERSION, NAME,CREATED,DESCRIPTION,DISPLAY_NAME,NOTES,CUSTOM) VALUES(?,?,?,?,?,?,?,?)";
	/** The UIDMeta update SQL template */	
	public static final String UID_UPDATE_SQL_TEMPLATE = "UPDATE %s SET VERSION = ?, NAME = ?, DESCRIPTION = ?, DISPLAY_NAME = ?, NOTES = ?, CUSTOM = ? WHERE XUID = ?";
	
	
	/** The SQL to insert a TSMeta TSD_TSMETA */
	public static final String TSUID_INSERT_SQL = "INSERT INTO TSD_TSMETA " + 
			"(FQNID, VERSION, METRIC_UID, FQN, TSUID, CREATED, MAX_VALUE, MIN_VALUE, " + 
			"DATA_TYPE, DESCRIPTION, DISPLAY_NAME, NOTES, UNITS, RETENTION, CUSTOM) " + 
			"VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
	/** The SQL to update TSD_TSMETA with a changed TSMeta */
	public static final String TSUID_UPDATE_SQL = "UPDATE TSD_TSMETA SET " +
			"VERSION = ?, METRIC_UID = ?, FQN = ?, MAX_VALUE = ?, MIN_VALUE = ?," +
			"DATA_TYPE = ?, DESCRIPTION = ?, DISPLAY_NAME = ?, NOTES = ?, UNITS = ?," +
			"RETENTION = ?, CUSTOM = ?" + 
			" WHERE FQNID = ?";
	/** The SQL to get the FQNID from TSD_TSMETA for a given TSMeta TSUID */
	public static final String GET_FQNID_FOR_TSUID_SQL = "SELECT FQNID FROM TSD_TSMETA WHERE TSUID = ?";
	
	
	/** The SQL to insert the TSMeta UID pairs */
	public static final  String TSD_FQN_TAGPAIR_SQL = "INSERT INTO TSD_FQN_TAGPAIR (FQN_TP_ID, FQNID, XUID, PORDER, NODE) VALUES (?,?,?,?,?)";
	/** The SQL to insert an Annotation */
	public static final String TSD_INSERT_ANNOTATION = "INSERT INTO TSD_ANNOTATION (ANNID,VERSION,START_TIME,DESCRIPTION,NOTES,FQNID,END_TIME,CUSTOM) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
	/** The SQL to update an Annotation */
	public static final String TSD_UPDATE_ANNOTATION = "UPDATE TSD_ANNOTATION SET VERSION = ?, START_TIME = ?, DESCRIPTION = ?, NOTES = ?, FQNID = ?, END_TIME = ?, CUSTOM = ? WHERE ANNID = ?";
	
	
	
	/** The SQL to insert a Tag Pair */
	public static final String INSERT_TAGPAIR_SQL = "INSERT INTO TSD_TAGPAIR (XUID, TAGK, TAGV, NAME) VALUES (?,?,?,?)";

	// ========================================================================================
	//	Object DELETE SQL
	// ========================================================================================
	/** The SQL to delete an Annotation */
	public static final String TSD_DELETE_ANNOTATION = "DELETE FROM TSD_ANNOTATION WHERE START_TIME = ? AND (TSUID = ? OR TSUID IS NULL)";
	/** The SQL template to delete a UIDMeta */
	public static final String TSD_DELETE_UID = "DELETE FROM TSD_%s WHERE XUID = ?";
	/** The SQL template to delete a TSMeta */
	public static final String TSD_DELETE_TS = "DELETE FROM TSD_TSMETA WHERE TSUID = ?";

	
	
	//=======================================================================================
	
			
	/** The keys to insert into a new custom map for UISMetas and Annotations */
	public static final Map<String, String> INIT_CUSTOM;
	
	
	static {
		Map<String, String> tmp = new TreeMap<String, String>();
		tmp.put(SAVED_BY_KEY, H2DBCatalog.class.getSimpleName());
		//tmp.put(VERSION_KEY, "1");
		INIT_CUSTOM = Collections.unmodifiableMap(tmp);
	}

	


	/**
	 * Creates a new AbstractDBCatalog
	 */
	protected AbstractDBCatalog() {
		log.info("Created DB Initializer: [{}]", getClass().getSimpleName());
	}
	
	// ========================================================================================
	//	Object INSERT template builders for UID insert SQL
	// ========================================================================================

	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.CatalogDBInterface#getUIDMetaTagKIndexSQL()
	 */
	@Override
	public String getUIDMetaTagKIndexSQL() {
		return String.format(UID_INDEX_SQL_TEMPLATE, "TSD_TAGK");
	}
	
	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.CatalogDBInterface#getUIDMetaTagVIndexSQL()
	 */
	@Override
	public String getUIDMetaTagVIndexSQL() {
		return String.format(UID_INDEX_SQL_TEMPLATE, "TSD_TAGV");
	}

	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.CatalogDBInterface#getUIDMetaMetricIndexSQL()
	 */
	@Override
	public String getUIDMetaMetricIndexSQL() {
		return String.format(UID_INDEX_SQL_TEMPLATE, "TSD_METRIC");
	}

	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.CatalogDBInterface#getUIDMetaMetricUpdateSQL()
	 */
	@Override
	public String getUIDMetaMetricUpdateSQL() { 
		return String.format(UID_UPDATE_SQL_TEMPLATE, "TSD_METRIC");
	}
	
	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.CatalogDBInterface#getUIDMetaTagVUpdateSQL()
	 */
	@Override
	public String getUIDMetaTagVUpdateSQL() {
		return String.format(UID_UPDATE_SQL_TEMPLATE, "TSD_TAGV");
	}
	
	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.CatalogDBInterface#getUIDMetaTagKUpdateSQL()
	 */
	@Override
	public String getUIDMetaTagKUpdateSQL() {
		return String.format(UID_UPDATE_SQL_TEMPLATE, "TSD_TAGK");
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.CatalogDBInterface#getDataSource()
	 */
	@Override
	public DataSource getDataSource() {
		return dataSource;
	}
	
	/**
	 * <p>Default impl, which does nothing</p>
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.CatalogDBInterface#initConnection(java.sql.Connection)
	 */
	public void initConnection(Connection conn) {
		setConnectionProperty(conn, TSD_CONN_TYPE, EQ_CONN_FLAG);
	}

	// ========================================================================================
	//	Catalog Service Initialization
	// ========================================================================================
	
	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.CatalogDBInterface#initialize(org.helios.tsdb.plugins.service.PluginContext)
	 */
	@Override
	public void initialize(PluginContext pc) {
		log.info("\n\t================================================\n\tStarting DB Initializer\n\tName:{}\n\t================================================", getClass().getSimpleName());
		pluginContext = pc;
		tsdb = pluginContext.getTsdb();
		extracted = pluginContext.getExtracted();
		syncQueuePollerEnabled = ConfigurationHelper.getBooleanSystemThenEnvProperty(DB_SYNCQ_POLLER_ENABLED, DEFAULT_DB_SYNCQ_POLLER_ENABLED, extracted);  
		
		cds = CatalogDataSource.getInstance();
		cds.initialize(pluginContext);
		dataSource = cds.getDataSource();
		popDbInfo();
		doInitialize();
		extracted = pluginContext.getExtracted();
		
		fqnSequence = createLocalSequenceCache(
				ConfigurationHelper.getIntSystemThenEnvProperty(DB_FQN_SEQ_INCR, DEFAULT_DB_FQN_SEQ_INCR, extracted), 
				"FQN_SEQ", dataSource); // FQN_SEQ		
		fqnTpSequence = createLocalSequenceCache(
				ConfigurationHelper.getIntSystemThenEnvProperty(DB_TP_FQN_SEQ_INCR, DEFAULT_DB_TP_FQN_SEQ_INCR, extracted), 
				"FQN_TP_SEQ", dataSource); // FQN_TP_SEQ
		annSequence = createLocalSequenceCache(
				ConfigurationHelper.getIntSystemThenEnvProperty(DB_ANN_SEQ_INCR, DEFAULT_DB_ANN_SEQ_INCR, extracted), 
				"ANN_SEQ", dataSource); // ANN_SEQ
		pc.setResource(CatalogDBInterface.class.getSimpleName(), this);
		if(syncQueuePollerEnabled) {
			syncQueueProcessor = new SyncQueueProcessor(pc);
			syncQueueProcessor.startAndWait();
		}		
		JMXHelper.registerMBean(this, JMXHelper.objectName("tsd.catalog:service=DBInterface"));
		log.info("\n\t================================================\n\tDB Initializer Started\n\tJDBC URL:{}\n\t================================================", cds.getConfig().getJdbcUrl());
	}
	
	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.CatalogDBInterface#triggerSyncQueueFlush()
	 */
	public void triggerSyncQueueFlush() {
		if(syncQueuePollerEnabled) {
			if(syncQueueProcessor==null) throw new RuntimeException("SyncQueue Polling Enabled but syncQueueProcessor is null");
			syncQueueProcessor.run();
		}
	}
	
	/**
	 * Populates the DB meta-data
	 */
	protected void popDbInfo() {
		Connection conn = null;
		try {
			conn = dataSource.getConnection();
			DatabaseMetaData dmd = conn.getMetaData();
			dbUrl = dmd.getURL();
			dbUser = dmd.getUserName();
			dbDriverName = dmd.getDriverName();
			dbDriverVersion = dmd.getDriverVersion();
			dbName = dmd.getDatabaseProductName();
			dbVersion = dmd.getDatabaseProductVersion();			
		} catch (Exception ex) {
			log.warn("Failed to get DB Metadata", ex);
		} finally {
			if(conn!=null) try { conn.close(); } catch (Exception x) {/* No Op */}
		}
		
	}
	
	/**
	 * Perform the concrete catalog service initialzation
	 */
	protected abstract void doInitialize();
	
	
	/**
	 * Creates a local sequence cache suitable for this database
	 * @param increment The local sequence increment
	 * @param sequenceName The DB Sequence name, fully qualified if necessary
	 * @param dataSource The datasource to provide connections to refresh the sequence cache
	 * @return the created local sequence cache
	 */
	protected LocalSequenceCache createLocalSequenceCache(int increment, String sequenceName, DataSource dataSource) {
		return new LocalSequenceCache(increment, sequenceName, dataSource);
	}
	
	
	// ========================================================================================
	//	Catalog Service Shutdown
	// ========================================================================================
	
	/**
	 * Terminates the database resources
	 */
	@Override
	public void shutdown() {
		log.info("\n\t================================================\n\tStopping TSDB Catalog DB\n\tName:{}\n\t================================================", cds.getConfig().getJdbcUrl());
		doShutdown();
		if(cds!=null) {
			cds.shutdown();
			cds = null;
			dataSource = null;
		}
		
		log.info("\n\t================================================\n\tTSDB Catalog DB Stopped\n\t================================================");
	}
	
	/**
	 * Concrete service instance shutdown
	 */
	protected abstract void doShutdown();
	
	// ==================================================================================================
	//  Event Processing
	// ==================================================================================================
	
	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.CatalogDBInterface#processEvents(java.sql.Connection, java.util.Set)
	 */
	@Override
	public void processEvents(Connection conn, Set<TSDBSearchEvent> events) {		
		int ops = 0;
		ElapsedTime et = SystemClock.startClock();
		Set<String> batchedUids = new HashSet<String>(events.size());
		Set<String> batchedUidPairs = new HashSet<String>(events.size());
		Set<Annotation> annotations = new HashSet<Annotation>();
		BatchMileStone latch = null;
		try {			
			for(TSDBSearchEvent event: events) {
				if(BatchMileStone.class.isInstance(event)) {
					log.info("==== Set Milestone ====");
					latch = (BatchMileStone)event;
					continue;
				}
				ops++;
				switch(event.eventType) {
				case ANNOTATION_DELETE:
					deleteAnnotation(conn, event.annotation);
					break;
				case ANNOTATION_INDEX:
					annotations.add(event.annotation);
					break;
				case TSMETA_DELETE:
					deleteTSMeta(conn, event.tsuid);
					break;
				case TSMETA_INDEX:
					TSMeta tsMeta = event.tsMeta;
					processTSMeta(batchedUidPairs, conn, tsMeta);
					break;
				case UIDMETA_DELETE:
					deleteUIDMeta(conn, event.uidMeta);
					break;
				case UIDMETA_INDEX:		
					if(!batchedUids.contains(event.uidMeta.toString())) {
						processUIDMeta(conn, event.uidMeta);
						batchedUids.add(event.uidMeta.toString());
					}
					//log.info("Bound {} Index [{}]-[{}]", uidMeta.getType().name(), uidMeta.getName(), uidMeta.getUID());
					break;
				default:
					log.warn("Unexpected event type found in event queue [{}]", event.eventType.name());
					break;					
				}
			} // end for event processor for loop
			
			// Execute batch inserts for TAGK, TAGV and METRIC
			executeUIDBatches(conn);
			// Execute batch inserts for TAG PAIRS
			if(uidMetaTagPairPs!=null) {
				executeBatch(uidMetaTagPairPs);
				uidMetaTagPairPs.clearBatch();
			}

			// Execute batch updates for TSMetas
			if(tsMetaFqnUpdatePs!=null) {
				executeBatch(tsMetaFqnUpdatePs);
				tsMetaFqnUpdatePs.clearBatch();				
			}
			
			// Execute batch inserts for TSMetas
			if(tsMetaFqnPs!=null) {
				executeBatch(tsMetaFqnPs);
				tsMetaFqnPs.clearBatch();				
			}
			// Execute batch inserts for FQN TAG Pairs
			if(uidMetaTagPairFQNPs!=null) {
				executeBatch(uidMetaTagPairFQNPs);
				uidMetaTagPairFQNPs.clearBatch();								
			}
			if(!annotations.isEmpty())
			// Insert annotations
			for(Annotation a: annotations) {
				processAnnotation(conn, a);
			}
			// Execute batch updates for Annotations
			if(annotationsUpdatePs!=null) {
				executeBatch(annotationsUpdatePs);
				annotationsUpdatePs.clearBatch();								
			}			
			// Execute batch inserts for Annotations
			if(annotationsPs!=null) {
				executeBatch(annotationsPs);
				annotationsPs.clearBatch();								
			}
			conn.commit();
			log.info(et.printAvg("Indexes", ops));
			if(latch!=null) {
				latch.countDown();
				latch = null;
			}			
		} catch (Exception ex) {
			log.error("batch operations failed", ex);
			try { conn.rollback(); } catch (Exception nex) {
				log.error("Batch update failed and connection failed to rollback !!!", nex);
			}
			if(ex instanceof SQLException) {
				SQLException sex = (SQLException)ex;
				SQLException sex2 = sex.getNextException();
				if(sex2!=null) {
					sex2.printStackTrace(System.err);
				}
			}
			throw new RuntimeException("Batch update failed", ex);
			// TODO: Custom exception that indicates if rollback succeeded
		} finally {
			if(uidMetaTagKIndexPs!=null) try { uidMetaTagKIndexPs.close(); uidMetaTagKIndexPs = null;} catch (Exception x) {/* No Op */}
			if(uidMetaTagKUpdatePs!=null) try { uidMetaTagKUpdatePs.close(); uidMetaTagKUpdatePs = null;} catch (Exception x) {/* No Op */}
			if(uidMetaTagVIndexPs!=null) try { uidMetaTagVIndexPs.close(); uidMetaTagVIndexPs = null; } catch (Exception x) {/* No Op */}
			if(uidMetaTagVUpdatePs!=null) try { uidMetaTagVUpdatePs.close(); uidMetaTagVUpdatePs = null; } catch (Exception x) {/* No Op */}
			if(uidMetaMetricIndexPs!=null) try { uidMetaMetricIndexPs.close(); uidMetaMetricIndexPs = null;} catch (Exception x) {/* No Op */}
			if(uidMetaMetricUpdatePs!=null) try { uidMetaMetricUpdatePs.close(); uidMetaMetricUpdatePs = null;} catch (Exception x) {/* No Op */}
			if(uidMetaTagPairPs!=null) try { uidMetaTagPairPs.close(); uidMetaTagPairPs = null;} catch (Exception x) {/* No Op */}
			if(tsMetaFqnPs!=null) try { tsMetaFqnPs.close(); tsMetaFqnPs = null;} catch (Exception x) {/* No Op */}
			if(uidMetaTagPairFQNPs!=null) try { uidMetaTagPairFQNPs.close(); uidMetaTagPairFQNPs = null;} catch (Exception x) {/* No Op */}
			if(annotationsPs!=null) try { annotationsPs.close(); annotationsPs = null;} catch (Exception x) {/* No Op */}
		}
	}
	
	// ==================================================================================================
	//  Prepared Statement Batching 
	// ==================================================================================================
	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.CatalogDBInterface#executeBatch(java.sql.PreparedStatement)
	 */
	@Override
	public void executeBatch(PreparedStatement ps) throws SQLException {
		int[] results = ps.executeBatch();
		if(results!=null && results.length>0) {
			Arrays.sort(results);
			if(results[0] < Statement.SUCCESS_NO_INFO) {
				ps.getConnection().rollback();
				throw new SQLException("Batch results had failed result code [" + results[0] + "]");
			}
		} else {
			log.warn("SQL Batch Execution for [{}] returned zero results. Probable programmer error", ps);
		}
		//log.info("Processed Batch of Size:{}", results.length);
	}
	
	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.CatalogDBInterface#bindUIDMeta(net.opentsdb.meta.UIDMeta, java.sql.PreparedStatement)
	 */
	@Override
	public void bindUIDMeta(UIDMeta uidMeta, PreparedStatement ps) throws SQLException {
		ps.setString(1, uidMeta.getUID());		
		ps.setString(3, uidMeta.getName());
		long created = uidMeta.getCreated();
		if(created==0) {
			created = SystemClock.time();
		}
		ps.setTimestamp(4, new Timestamp(created));
		ps.setString(5, uidMeta.getDescription());
		ps.setString(6, uidMeta.getDisplayName());
		ps.setString(7, uidMeta.getNotes());
		HashMap<String, String> custom = fillInCustom(uidMeta.getCustom());
		ps.setString(8, JSONMapSupport.nokToString(custom));
		ps.setInt(2, Integer.parseInt(custom.get(VERSION_KEY)));
		uidMeta.setCustom(custom);		
		ps.addBatch();
	}
	
	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.CatalogDBInterface#bindUIDMetaUpdate(net.opentsdb.meta.UIDMeta, java.sql.PreparedStatement)
	 */
	@Override
	public void bindUIDMetaUpdate(UIDMeta uidMeta, PreparedStatement ps) throws SQLException {
		Map<String, String> custom = uidMeta.getCustom();
		if(custom==null) {
			custom = new HashMap<String, String>(1);
		}
		int version = JSONMapSupport.incrementAndGet(custom, 1, VERSION_KEY);
		ps.setInt(1, version);
		ps.setString(2, uidMeta.getName());
		ps.setString(3, uidMeta.getDescription());
		ps.setString(4, uidMeta.getDisplayName());
		ps.setString(5, uidMeta.getNotes());
		ps.setString(6, JSONMapSupport.nokToString(custom));
		ps.setString(7, uidMeta.getUID());
		ps.addBatch();
	}
	
	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.CatalogDBInterface#executeUIDBatches(java.sql.Connection)
	 */
	@Override
	public void executeUIDBatches(Connection conn) {
		try {
			if(uidMetaMetricIndexPs!=null) {
				executeBatch(uidMetaMetricIndexPs);
				uidMetaMetricIndexPs.clearBatch();
			}
			if(uidMetaMetricUpdatePs!=null) {
				executeBatch(uidMetaMetricUpdatePs);
				uidMetaMetricUpdatePs.clearBatch();
			}
			
			if(uidMetaTagVIndexPs!=null) {
				executeBatch(uidMetaTagVIndexPs);
				uidMetaTagVIndexPs.clearBatch();
			}
			if(uidMetaTagVUpdatePs!=null) {
				executeBatch(uidMetaTagVUpdatePs);
				uidMetaTagVUpdatePs.clearBatch();
			}
			
			if(uidMetaTagKIndexPs!=null) {
				executeBatch(uidMetaTagKIndexPs);
				uidMetaTagKIndexPs.clearBatch();
			}
			if(uidMetaTagKUpdatePs!=null) {
				executeBatch(uidMetaTagKUpdatePs);
				uidMetaTagKUpdatePs.clearBatch();
			}			
			
		} catch (Exception ex) {
			if(ex instanceof SQLException) {
				SQLException sex = (SQLException)ex;
				SQLException sex2 = sex.getNextException();
				if(sex2!=null) {
					sex2.printStackTrace(System.err);
				}
			}
			throw new RuntimeException("Failed to execute UID Batches", ex);
		}		
	}
	
	/** A cache of counters counting the number of ops of each type keyed by the optype name */
	protected final Cache<String, AtomicLong> opCounters = CacheBuilder.newBuilder().initialCapacity(32).build();
	
	
	
	/**
	 * Increments the opCounter identified by the passed key
	 * @param key The op counter key
	 * @return the new counter value
	 */
	protected long incrementOpCounter(String key) {
		try {
			return opCounters.get(key, new Callable<AtomicLong>() {
				public AtomicLong call() {
					return new AtomicLong(0L);
				}
			}).incrementAndGet();
		} catch (Exception ex) {
			/* should not happen */
			throw new RuntimeException("OpCounter Update Failure for [" + key + "]", ex);
		}
	}
	
	/**
	 * Returns the count of operations represented by the passed key
	 * @param key The op key
	 * @return the number of ops of the indicated type
	 */
	protected long getOpCount(String key) {
		AtomicLong counter = opCounters.getIfPresent(key);
		return counter==null ? 0 : counter.get();
	}
	
	// ==================================================================================================
	//  Object Indexing & Processing 
	// ==================================================================================================
	
	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.CatalogDBInterface#processUIDMeta(java.sql.Connection, net.opentsdb.meta.UIDMeta)
	 */
	@Override
	public void processUIDMeta(Connection conn, UIDMeta uidMeta) {
		try {
			if(!exists(conn, uidMeta)) {
				switch(uidMeta.getType()) {
					case METRIC:							
						if(uidMetaMetricIndexPs==null) uidMetaMetricIndexPs = conn.prepareStatement(getUIDMetaMetricIndexSQL());							
						bindUIDMeta(uidMeta, uidMetaMetricIndexPs);
						incrementOpCounter(METRIC_INSERT_CNT);
						break;
					case TAGK:
						if(uidMetaTagKIndexPs==null) uidMetaTagKIndexPs = conn.prepareStatement(getUIDMetaTagKIndexSQL());
						bindUIDMeta(uidMeta, uidMetaTagKIndexPs);
						incrementOpCounter(TAGK_INSERT_CNT);
						break;
					case TAGV:
						if(uidMetaTagVIndexPs==null) uidMetaTagVIndexPs = conn.prepareStatement(getUIDMetaTagVIndexSQL());
						bindUIDMeta(uidMeta, uidMetaTagVIndexPs);
						incrementOpCounter(TAGV_INSERT_CNT);
						break;
					default:
						log.warn("yeow. Unexpected UIDMeta type:{}", uidMeta.getType().name());
						break;
				}				
			} else {
				switch(uidMeta.getType()) {
					case METRIC:							
						if(uidMetaMetricUpdatePs==null) uidMetaMetricUpdatePs = conn.prepareStatement(getUIDMetaMetricUpdateSQL());							
						bindUIDMetaUpdate(uidMeta, uidMetaMetricUpdatePs);
						incrementOpCounter(METRIC_UPDATE_CNT);
						break;
					case TAGK:
						if(uidMetaTagKUpdatePs==null) uidMetaTagKUpdatePs = conn.prepareStatement(getUIDMetaTagKUpdateSQL());
						bindUIDMetaUpdate(uidMeta, uidMetaTagKUpdatePs);
						incrementOpCounter(TAGK_UPDATE_CNT);
						break;
					case TAGV:
						if(uidMetaTagVUpdatePs==null) uidMetaTagVUpdatePs = conn.prepareStatement(getUIDMetaTagVUpdateSQL());
						bindUIDMetaUpdate(uidMeta, uidMetaTagVUpdatePs);
						incrementOpCounter(TAGV_UPDATE_CNT);
						break;
					default:
						log.warn("yeow. Unexpected UIDMeta type:{}", uidMeta.getType().name());
						break;
				}								
			}
		} catch (SQLException sex) {
			throw new RuntimeException("Failed to process UIDMeta [" + uidMeta + "]", sex);
		}
	}
	
	/**
	 * Retrieves the ANNID for the passed annotation
	 * @param conn The connection to use
	 * @param annotation The annotation to search for
	 * @return the ANNID of the passed annotation
	 */
	protected long getAnnIdForAnnotation(Connection conn, Annotation annotation) {
		PreparedStatement ps = null;
		ResultSet rset = null;
		try {
			ps = conn.prepareStatement(GET_ANNOTATION_ID_SQL);
			ps.setTimestamp(1, new Timestamp(utoms(annotation.getStartTime())));
			if(annotation.getTSUID()==null) {
				ps.setNull(2, Types.VARCHAR);
			} else {
				ps.setString(2, annotation.getTSUID());
			}			
			rset = ps.executeQuery();
			rset.next();
			return rset.getLong(1);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to get ANNID for annotation [" + annotation + "]", ex);
		} finally {
			if(rset!=null) try { rset.close(); } catch (Exception x) {/* No Op */}
			if(ps!=null) try { ps.close(); } catch (Exception x) {/* No Op */}			
		}
	}
	
	/**
	 * Executes a batched update for the passed modified Annotation
	 * @param conn The connection to update on
	 * @param a The changed Annotation
	 */
	protected void updateAnnotation(Connection conn, Annotation a) {
		try {
			long annId = getAnnIdForAnnotation(conn, a);
			if(annotationsUpdatePs==null) annotationsUpdatePs = conn.prepareStatement(TSD_UPDATE_ANNOTATION);
			Map<String, String> custom = a.getCustom();
			if(custom==null) {
				custom = new HashMap<String, String>(1);
			}
			int version = JSONMapSupport.incrementAndGet(custom, 1, VERSION_KEY);
			annotationsUpdatePs.setInt(1, version);		
			annotationsUpdatePs.setTimestamp(2, new Timestamp(utoms(a.getStartTime())));
			annotationsUpdatePs.setString(3, a.getDescription());
			annotationsUpdatePs.setString(4, a.getNotes());
			if(a.getTSUID()==null) {
				annotationsUpdatePs.setNull(5, Types.BIGINT);
			} else {
				annotationsUpdatePs.setLong(5, getFqnIdForTsUid(conn, a.getTSUID()));
			}
			if(a.getEndTime()==0) {
				annotationsUpdatePs.setNull(6, Types.TIMESTAMP);
			} else {
				annotationsUpdatePs.setTimestamp(6, new Timestamp(utoms(a.getEndTime())));
			}
			annotationsUpdatePs.setString(7, JSONMapSupport.nokToString(custom));
			annotationsUpdatePs.setLong(8, annId);
			annotationsUpdatePs.addBatch();
			// TSD_UPDATE_ANNOTATION = "UPDATE TSD_ANNOTATION SET VERSION = ?, START_TIME = ?, DESCRIPTION = ?, 
			// NOTES = ?, FQNID = ?, END_TIME = ?, CUSTOM = ? WHERE ANNID = ?";
		} catch (Exception ex) {
			throw new RuntimeException("Failed to update Annotation [" + a + "]", ex);
		}
	}	
	
	
	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.CatalogDBInterface#processAnnotation(java.sql.Connection, net.opentsdb.meta.Annotation)
	 */
	@Override
	public void processAnnotation(Connection conn, Annotation annotation) {
		try {
			if(exists(conn, annotation)) {
				updateAnnotation(conn, annotation);
				return;
			}
			if(annotationsPs==null) annotationsPs = conn.prepareStatement(TSD_INSERT_ANNOTATION);
			long startTime = annotation.getStartTime(); 
			if(startTime==0) {
				startTime = SystemClock.unixTime();
				annotation.setStartTime(startTime);
			}
			long annId = annSequence.next();
			annotationsPs.setLong(1, annId);
			annotationsPs.setTimestamp(3, new Timestamp(utoms(startTime)));
			annotationsPs.setString(4, annotation.getDescription());
			annotationsPs.setString(5, annotation.getNotes());
			if(annotation.getTSUID()==null) {
				annotationsPs.setNull(6, Types.BIGINT);
			} else {
				annotationsPs.setLong(6, H2Support.fqnId(conn, annotation.getTSUID()));
			}
			
			
			long endTime = annotation.getEndTime();
			if(endTime==0) {
				annotationsPs.setNull(7, Types.TIMESTAMP);
			} else {
				annotationsPs.setTimestamp(7, new Timestamp(utoms(endTime)));
			}			
			HashMap<String, String> custom = fillInCustom(annotation.getCustom());
			custom.put(PK_KEY, Long.toString(annId));
			annotationsPs.setString(8, JSONMapSupport.nokToString(custom));
			annotationsPs.setInt(2, Integer.parseInt(custom.get(VERSION_KEY)));
			annotation.setCustom(custom);
			annotationsPs.addBatch();
		} catch (SQLException sex) {
			throw new RuntimeException("Failed to store annotation [" + annotation.getDescription() + "]", sex);
		}
	}
	
	
	
	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.CatalogDBInterface#isNaNToNull()
	 */
	@Override
	public boolean isNaNToNull() {
		return false;
	}

	/**
	 * Retrieves the FQNID for the passed TSUID
	 * @param conn The connection to use
	 * @param tsuid The TSUID to search with
	 * @return the FQNID
	 */
	protected long getFqnIdForTsUid(Connection conn, String tsuid) {
		PreparedStatement ps = null;
		ResultSet rset = null;
		try {
			ps = conn.prepareStatement(GET_FQNID_FOR_TSUID_SQL);
			ps.setString(1, tsuid);
			rset = ps.executeQuery();
			rset.next();
			return rset.getLong(1);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to get FQNID for tssuid [" + tsuid + "]", ex);
		} finally {
			if(rset!=null) try { rset.close(); } catch (Exception x) {/* No Op */}
			if(ps!=null) try { ps.close(); } catch (Exception x) {/* No Op */}			
		}
	}

	
	/**
	 * Executes a batched update for the passed modified TSMeta
	 * @param conn The connection to update on
	 * @param tsMeta The changed TSMeta 
	 */
	protected void updateTSMeta(Connection conn, TSMeta tsMeta) {
		try {
			long fqnId = getFqnIdForTsUid(conn, tsMeta.getTSUID());
			if(tsMetaFqnUpdatePs==null) tsMetaFqnUpdatePs = conn.prepareStatement(TSUID_UPDATE_SQL);
			Map<String, String> custom = tsMeta.getCustom();
			if(custom==null) {
				custom = new HashMap<String, String>(1);
			}
			int version = JSONMapSupport.incrementAndGet(custom, 1, VERSION_KEY);
			tsMetaFqnUpdatePs.setInt(1, version);			
			tsMetaFqnUpdatePs.setString(2, tsMeta.getMetric().getUID());
			tsMetaFqnUpdatePs.setString(3, getFQN(tsMeta));
			if(isNaNToNull() && Double.isNaN(tsMeta.getMax())) {
				tsMetaFqnUpdatePs.setNull(4, Types.DOUBLE);
			} else {
				tsMetaFqnUpdatePs.setDouble(4, tsMeta.getMax());
			}
			if(isNaNToNull() && Double.isNaN(tsMeta.getMin())) {
				tsMetaFqnUpdatePs.setNull(5, Types.DOUBLE);
			} else {
				tsMetaFqnUpdatePs.setDouble(5, tsMeta.getMin());
			}
			tsMetaFqnUpdatePs.setString(6, tsMeta.getDataType());
			tsMetaFqnUpdatePs.setString(7, tsMeta.getDescription());
			tsMetaFqnUpdatePs.setString(8, tsMeta.getDisplayName());
			tsMetaFqnUpdatePs.setString(9, tsMeta.getNotes());
			tsMetaFqnUpdatePs.setString(10, tsMeta.getUnits());
			tsMetaFqnUpdatePs.setInt(11, tsMeta.getRetention());
			tsMetaFqnUpdatePs.setString(12, JSONMapSupport.nokToString(custom));
			tsMetaFqnUpdatePs.setLong(13, fqnId);
			tsMetaFqnUpdatePs.addBatch();
//			TSUID_UPDATE_SQL = "UPDATE TSD_TSMETA " +
//					"VERSION = ?, METRIC_UID = ?, FQN = ?, MAX_VALUE = ?, MIN_VALUE = ?," +
//					"DATA_TYPE = ?, DESCRIPTION = ?, DISPLAY_NAME = ?, NOTES = ?, UNITS = ?" +
//					"RETENTION = ?, CUSTOM = ?" + 
//					" WHERE FQNID = ?";			
		} catch (Exception ex) {
			throw new RuntimeException("Failed to update tsmeta [" + tsMeta + "]", ex);
		}
	}
	
	/**
	 * Creates an {@link javax.management.ObjectName} like string representing the 
	 * fully qualified flat name of the passed TSMeta, where the props are sorted in key alpha order. 
	 * @param tsMeta The TSMeta to get the FQN for
	 * @return the flat FQN
	 */
	public static String getFQN(TSMeta tsMeta) {
		StringBuilder fqn = new StringBuilder(tsMeta.getMetric().getName()).append(":");
		TreeMap<String, String> tags = new TreeMap<String, String>();
		UIDMeta[] tagPair = new UIDMeta[2];
		for(UIDMeta meta: tsMeta.getTags()) {
			if(tagPair[0]==null) {
				tagPair[0] = meta;
				continue;
			} else if(tagPair[1]==null) {
				tagPair[1] = meta;
				tags.put(tagPair[0].getName(), tagPair[1].getName());
				tagPair[0] = null; tagPair[1] = null; 
			}			
		}
		for(Map.Entry<String, String> tag: tags.entrySet()) {
			fqn.append(tag.getKey()).append("=").append(tag.getValue()).append(",");
		}		
		return fqn.deleteCharAt(fqn.length()-1).toString();
	}
	
	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.CatalogDBInterface#processTSMeta(java.util.Set, java.sql.Connection, net.opentsdb.meta.TSMeta)
	 */
	@Override
	public void processTSMeta(final Set<String> batchUidPairs, Connection conn, TSMeta tsMeta) {
		if(exists(conn, tsMeta)) {
			updateTSMeta(conn, tsMeta);
			return;
		} 
		StringBuilder fqn = new StringBuilder(tsMeta.getMetric().getName()).append(":");
		UIDMeta[] tagPair = new UIDMeta[2];
		TreeMap<String, String> tags = new TreeMap<String, String>();
		for(UIDMeta meta: tsMeta.getTags()) {
			if(tagPair[0]==null) {
				tagPair[0] = meta;
				continue;
			} else if(tagPair[1]==null) {
				tagPair[1] = meta;
				// ===========================================================
				//	This guy saves the tag pairs
				// ===========================================================
				processUIDMetaPair(batchUidPairs, conn, tagPair);
				// ===========================================================
				tags.put(tagPair[0].getName(), tagPair[1].getName());				
				tagPair[0] = null; tagPair[1] = null; 
			}
		}
		for(Map.Entry<String, String> tag: tags.entrySet()) {
			fqn.append(tag.getKey()).append("=").append(tag.getValue()).append(",");
		}
		fqn.deleteCharAt(fqn.length()-1);
		try {
			// annotationsPs.setInt(2, Integer.parseInt(custom.get(VERSION_KEY)));
//			Map<String, String> custom = fillInCustom(annotation.getCustom());
//			annotationsPs.setString(8, JSONMapSupport.nokToString(custom));
//			annotationsPs.setInt(2, Integer.parseInt(custom.get(VERSION_KEY)));			
			
			if(tsMetaFqnPs==null) tsMetaFqnPs = conn.prepareStatement(TSUID_INSERT_SQL);
			long fqnSeq = fqnSequence.next();
			tsMetaFqnPs.setLong(1, fqnSeq);
			tsMetaFqnPs.setString(3, tsMeta.getMetric().getUID());
			tsMetaFqnPs.setString(4, fqn.toString());
			tsMetaFqnPs.setString(5, tsMeta.getTSUID());
			tsMetaFqnPs.setTimestamp(6, new Timestamp(utoms(tsMeta.getCreated())));
			if(isNaNToNull() && Double.isNaN(tsMeta.getMax())) {
				tsMetaFqnPs.setNull(7, Types.DOUBLE);
			} else {
				tsMetaFqnPs.setDouble(7, tsMeta.getMax());
			}
			if(isNaNToNull() && Double.isNaN(tsMeta.getMin())) {
				tsMetaFqnPs.setNull(8, Types.DOUBLE);
			} else {
				tsMetaFqnPs.setDouble(8, tsMeta.getMin());
			}
			tsMetaFqnPs.setString(9, tsMeta.getDataType());
			tsMetaFqnPs.setString(10, tsMeta.getDescription());
			tsMetaFqnPs.setString(11, tsMeta.getDisplayName());
			tsMetaFqnPs.setString(12, tsMeta.getNotes());
			tsMetaFqnPs.setString(13, tsMeta.getUnits());
			tsMetaFqnPs.setInt(14, tsMeta.getRetention());
			HashMap<String, String> custom = fillInCustom(tsMeta.getCustom());
			custom.put(PK_KEY, Long.toString(fqnSeq));
			tsMetaFqnPs.setString(15, JSONMapSupport.nokToString(custom));
			tsMetaFqnPs.setInt(2, Integer.parseInt(custom.get(VERSION_KEY)));			
			tsMeta.setCustom(custom);
			tsMetaFqnPs.addBatch();
			if(uidMetaTagPairFQNPs==null) uidMetaTagPairFQNPs = conn.prepareStatement(TSD_FQN_TAGPAIR_SQL); 
			LinkedList<UIDMeta> pairs = new LinkedList<UIDMeta>(tsMeta.getTags());
			int pairCount = tsMeta.getTags().size()/2;
			int leaf = pairCount-1;
			for(short i = 0; i < pairCount; i++) {
				String pairUID = pairs.removeFirst().getUID() + pairs.removeFirst().getUID();
				uidMetaTagPairFQNPs.setLong(1, fqnTpSequence.next());
				uidMetaTagPairFQNPs.setLong(2, fqnSeq);
				uidMetaTagPairFQNPs.setString(3, pairUID);
				uidMetaTagPairFQNPs.setShort(4, i);
				if(i==leaf) {
					uidMetaTagPairFQNPs.setString(5, "L");
				} else {
					uidMetaTagPairFQNPs.setString(5, "B");
				}
				uidMetaTagPairFQNPs.addBatch();
			}
		} catch (Exception ex) {
			throw new RuntimeException("Failed to store TSMeta [" + tsMeta + "]", ex);
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.CatalogDBInterface#processUIDMetaPair(java.util.Set, java.sql.Connection, net.opentsdb.meta.UIDMeta[])
	 */
	@Override
	public String processUIDMetaPair(final Set<String> batchUidPairs, Connection conn, UIDMeta[] tagPair) {
		if(tagPair[0].getType()!=UniqueIdType.TAGK) throw new IllegalArgumentException("Provided uidMetaKey was expected to be of type TAGK but was actually [" + tagPair[0].getType() + "]");
		if(tagPair[1].getType()!=UniqueIdType.TAGV) throw new IllegalArgumentException("Provided uidMetaValue was expected to be of type TAGV but was actually [" + tagPair[1].getType() + "]");
		String tagPairUid = tagPair[0].getUID() + tagPair[1].getUID();
		
		if(batchUidPairs.contains(tagPairUid)) return tagPairUid; 
		
		if(tagPairStored(conn, tagPairUid)) return tagPairUid;
		
		try {
			if(uidMetaTagPairPs==null) {
				uidMetaTagPairPs = conn.prepareStatement(INSERT_TAGPAIR_SQL);
			}
			uidMetaTagPairPs.setString(1, tagPairUid);
			uidMetaTagPairPs.setString(2, tagPair[0].getUID());
			uidMetaTagPairPs.setString(3, tagPair[1].getUID());
			uidMetaTagPairPs.setString(4, tagPair[0].getName() + "=" + tagPair[1].getName());
			uidMetaTagPairPs.addBatch();
			batchUidPairs.add(tagPairUid);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to write tagpair [" + tagPairUid + "]", ex);
		} 
		return null;
	}
	
	// ==================================================================================================
	//  Object Deletion 
	// ==================================================================================================
	
	/**
	 * Deletes a UIDMeta
	 * @param conn The connection to use
	 * @param uidMeta The UIDMeta to delete
	 */
	@Override
	public void deleteUIDMeta(Connection conn, UIDMeta uidMeta) {		
		PreparedStatement ps = null;		
		try {
			ps = conn.prepareStatement(String.format(TSD_DELETE_UID, uidMeta.getType().name()));
			ps.setString(1, uidMeta.getUID());
			ps.executeUpdate();
		} catch (Exception ex) {
			throw new RuntimeException("Failed to delete UID [" + uidMeta.getUID() + "]", ex);
		} finally {
			if(ps!=null) try { ps.close(); } catch (Exception x) {/* No Op */}
		}
	}
	
	/**
	 * Deletes a TSMeta
	 * @param conn The connection to use
	 * @param tsUid The TSUid of the TSMeta to delete
	 */
	@Override
	public void deleteTSMeta(Connection conn, String tsUid) {		
		PreparedStatement ps = null;		
		try {
			ps = conn.prepareStatement(TSD_DELETE_TS);
			ps.setString(1, tsUid);
			ps.executeUpdate();
		} catch (Exception ex) {
			throw new RuntimeException("Failed to delete TSUID [" + tsUid + "]", ex);
		} finally {
			if(ps!=null) try { ps.close(); } catch (Exception x) {/* No Op */}
		}
	}
	
	/**
	 * Deletes an Annotation
	 * @param conn The connection to use
	 * @param annotation The annotation to delete
	 */
	@Override
	public void deleteAnnotation(Connection conn, Annotation annotation) {		
		PreparedStatement ps = null;		
		try {
			ps = conn.prepareStatement(TSD_DELETE_ANNOTATION);
			ps.setTimestamp(1, new Timestamp(TimeUnit.MILLISECONDS.convert(annotation.getStartTime(), TimeUnit.SECONDS)));
			ps.setString(1, annotation.getTSUID());
			ps.executeUpdate();
		} catch (Exception ex) {
			throw new RuntimeException("Failed to delete Annotation [" + annotation + "]", ex);
		} finally {
			if(ps!=null) try { ps.close(); } catch (Exception x) {/* No Op */}
		}
	}
	
	
	
	/**
	 * Retrieves a locally scoped property on the passed connection
	 * @param conn The connection to set the property on
	 * @param key The property key
	 * @return The property value or null 
	 */
	public String getConnectionProperty(Connection conn, String key) {
		return getConnectionProperty(conn, key, null);
	}
	
	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.CatalogDBInterface#purge()
	 */
	@Override
	public void purge() {
		Connection conn = null;
		PreparedStatement ps = null;
		int rowsDeleted = 0;
		StringBuilder b = new StringBuilder("\n\t================================\n\tDatabase Purge\n\t================================");
		fqnSequence.reset();
		fqnTpSequence.reset();
		annSequence.reset();		
		try {
			conn = dataSource.getConnection();
			conn.setAutoCommit(false);
			setConnectionProperty(conn, TSD_CONN_TYPE, SYNC_CONN_FLAG);
			rowsDeleted = prepareAndExec(conn,"DELETE FROM TSD_TSMETA");
			conn.commit();
			b.append("\n").append("TSD_TSMETA:").append(rowsDeleted);
			rowsDeleted = prepareAndExec(conn,"DELETE FROM TSD_TAGPAIR");
			conn.commit();
			b.append("\n").append("TSD_TAGPAIR:").append(rowsDeleted);
			rowsDeleted = prepareAndExec(conn,"DELETE FROM TSD_TAGK");
			conn.commit();
			b.append("\n").append("TSD_TAGK:").append(rowsDeleted);
			rowsDeleted = prepareAndExec(conn,"DELETE FROM TSD_METRIC");
			conn.commit();
			b.append("\n").append("TSD_METRIC:").append(rowsDeleted);			
			rowsDeleted = prepareAndExec(conn,"DELETE FROM TSD_TAGV");
			conn.commit();
			b.append("\n").append("TSD_TAGV:").append(rowsDeleted);
			rowsDeleted = prepareAndExec(conn,"DELETE FROM TSD_FQN_TAGPAIR");
			conn.commit();
			b.append("\n").append("TSD_FQN_TAGPAIR:").append(rowsDeleted);
			rowsDeleted = prepareAndExec(conn, "DELETE FROM TSD_ANNOTATION");
			conn.commit();
			b.append("\n").append("TSD_ANNOTATION:").append(rowsDeleted);
			rowsDeleted = prepareAndExec(conn,"DELETE FROM SYNC_QUEUE");
			conn.commit();
			b.append("\n").append("SYNC_QUEUE:").append(rowsDeleted);
			log.info(b.append("\n\t======================================").toString());
		} catch (Exception ex) {
			throw new RuntimeException("Failed to purge Store", ex);
		} finally {			
			if(conn!=null) try { conn.close(); } catch (Exception x) {/* No Op */}
		}
	}
	
	private int prepareAndExec(Connection conn, String sql) throws SQLException {
		PreparedStatement ps = null;
		try {
			ps = conn.prepareStatement(sql);
			return ps.executeUpdate();
		} finally {
			if(ps!=null) try { ps.close(); } catch (Exception x) {/*No Op*/}
		}
			
	}

	// ==================================================================================================
	//  Object Unmarshalling  (i.e. ResultSet to List of Objects)
	// ==================================================================================================
	
	/**
	 * Returns a collection of {@link UIDMeta}s read from the passed {@link ResultSet}.
	 * @param rset The result set to read from
	 * @return a [possibly empty] collection of UIDMetas
	 */
	@Override
	public List<UIDMeta> readUIDMetas(ResultSet rset) {
		if(rset==null) throw new IllegalArgumentException("The passed result set was null");
		List<UIDMeta> uidMetas = new ArrayList<UIDMeta>();
		
		try {
			String tName = rset.getMetaData().getTableName(1).toUpperCase().replace("TSD_", "");
			UniqueIdType utype = UniqueIdType.valueOf(tName); 
			while(rset.next()) {
				UIDMeta meta = new UIDMeta(utype, UniqueId.stringToUid(rset.getString("XUID")), rset.getString("NAME"));
				meta.setCreated(mstou(rset.getTimestamp("CREATED").getTime()));
				meta.setCustom((HashMap<String, String>) JSONMapSupport.read(rset.getString("CUSTOM")));
				meta.setDescription(rset.getString("DESCRIPTION"));
				meta.setNotes(rset.getString("NOTES"));
				meta.setDisplayName(rset.getString("DISPLAY_NAME"));
				uidMetas.add(meta);
			}
		} catch (Exception ex) {
			throw new RuntimeException("Failed to read UIDMetas from ResultSet", ex);
		}
		return uidMetas;
	}
	
	/**
	 * Returns a collection of {@link UIDMeta}s read from the passed {@link ResultSet}.
	 * @param rset The result set to read from
	 * @return a [possibly empty] collection of UIDMetas
	 */
	@Override
	public List<TSMeta> readTSMetas(ResultSet rset) {
		if(rset==null) throw new IllegalArgumentException("The passed result set was null");
		List<TSMeta> tsMetas = new ArrayList<TSMeta>();
		try {
			while(rset.next()) {
				TSMeta meta = new TSMeta(UniqueId.stringToUid(rset.getString("TSUID")), mstou(rset.getTimestamp("CREATED").getTime()));
				meta.setCustom((HashMap<String, String>) JSONMapSupport.read(rset.getString("CUSTOM")));
				meta.setDescription(rset.getString("DESCRIPTION"));
				meta.setNotes(rset.getString("NOTES"));
				meta.setDisplayName(rset.getString("DISPLAY_NAME"));
				meta.setDataType(rset.getString("DATA_TYPE"));
				meta.setMax(rset.getDouble("MAX_VALUE"));
				meta.setMin(rset.getDouble("MIN_VALUE"));
				meta.setRetention(rset.getInt("RETENTION"));
				meta.setUnits(rset.getString("UNITS"));
				tsMetas.add(meta);
			}
		} catch (Exception ex) {
			throw new RuntimeException("Failed to read TSMetas from ResultSet", ex);
		}
		return tsMetas;
	}
	
	/**
	 * Returns a collection of {@link Annotation}s read from the passed {@link ResultSet}.
	 * @param rset The result set to read from
	 * @return a [possibly empty] collection of Annotations
	 */
	@Override
	public List<Annotation> readAnnotations(ResultSet rset) {
		if(rset==null) throw new IllegalArgumentException("The passed result set was null");
		List<Annotation> annotations = new ArrayList<Annotation>();
		try {
			while(rset.next()) {
				Annotation meta = new Annotation();
				
				meta.setCustom((HashMap<String, String>) JSONMapSupport.read(rset.getString("CUSTOM")));
				meta.setDescription(rset.getString("DESCRIPTION"));
				meta.setNotes(rset.getString("NOTES"));
				meta.setStartTime(mstou(rset.getTimestamp("START_TIME").getTime()));
				Timestamp ts = rset.getTimestamp("END_TIME");
				if(ts!=null) {
					meta.setEndTime(mstou(ts.getTime()));
				}
				annotations.add(meta);
			}
		} catch (Exception ex) {
			throw new RuntimeException("Failed to read Annotations from ResultSet", ex);
		}
		return annotations;		
	}
	
	// ===================================================================================================
	// Object Exists (INSERT or UPDATE ?)
	// ===================================================================================================
	
	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.CatalogDBInterface#exists(java.sql.Connection, net.opentsdb.meta.TSMeta)
	 */
	@Override
	public boolean exists(Connection conn, TSMeta tsMeta) {
		if(tsMeta==null) throw new IllegalArgumentException("The passed TSMeta was null");
		PreparedStatement ps = null;
		ResultSet rset = null;
		try {
			ps = conn.prepareStatement(TSUID_EXISTS_SQL);
			ps.setString(1, tsMeta.getTSUID());
			rset = ps.executeQuery();
			rset.next();
			return rset.getInt(1) > 0;
		} catch (Exception ex) {
			throw new RuntimeException("Failed to lookup exists on TSMeta [" + tsMeta + "]", ex);
		} finally {
			if(rset!=null) try { rset.close(); } catch (Exception x) {/* No Op */}
			if(ps!=null) try { ps.close(); } catch (Exception x) {/* No Op */}
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.CatalogDBInterface#exists(net.opentsdb.meta.TSMeta)
	 */
	@Override
	public boolean exists(TSMeta tsMeta) {
		Connection conn = null;
		try {
			conn = dataSource.getConnection();
			return exists(conn, tsMeta);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to lookup exists on TSMeta [" + tsMeta + "]", ex);
		} finally {
			if(conn!=null) try { conn.close(); } catch (Exception x) {/* No Op */}
		}		
	}


	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.CatalogDBInterface#exists(java.sql.Connection, net.opentsdb.meta.UIDMeta)
	 */
	@Override
	public boolean exists(Connection conn, UIDMeta uidMeta) {
		if(uidMeta==null) throw new IllegalArgumentException("The passed UIDMeta was null");
		PreparedStatement ps = null;
		ResultSet rset = null;
		try {
			ps = conn.prepareStatement(String.format(UID_EXISTS_SQL, uidMeta.getType().name()));
			ps.setString(1, uidMeta.getUID()); 
			rset = ps.executeQuery();
			rset.next();
			return rset.getInt(1) > 0;
		} catch (Exception ex) {
			throw new RuntimeException("Failed to lookup exists on UIDMeta [" + uidMeta + "]", ex);
		} finally {
			if(rset!=null) try { rset.close(); } catch (Exception x) {/* No Op */}
			if(ps!=null) try { ps.close(); } catch (Exception x) {/* No Op */}
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.CatalogDBInterface#exists(net.opentsdb.meta.UIDMeta)
	 */
	@Override
	public boolean exists(UIDMeta uidMeta) {
		Connection conn = null;
		try {
			conn = dataSource.getConnection();
			return exists(conn, uidMeta);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to lookup exists on UIDMeta [" + uidMeta + "]", ex);
		} finally {
			if(conn!=null) try { conn.close(); } catch (Exception x) {/* No Op */}
		}		
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.CatalogDBInterface#exists(net.opentsdb.meta.Annotation)
	 */
	@Override
	public boolean exists(Annotation annotation) {
		Connection conn = null;
		try {
			conn = dataSource.getConnection();
			return exists(conn, annotation);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to lookup exists on Annotation [" + annotation + "]", ex);
		} finally {
			if(conn!=null) try { conn.close(); } catch (Exception x) {/* No Op */}
		}
	}
	

	/**
	 * Determines if the passed annotation is already stored
	 * @param conn The connection to query on
	 * @param annotation The annotation to verify
	 * @return true if the passed annotation is already stored, false otherwise
	 */
	public boolean exists(Connection conn, Annotation annotation) {
		if(annotation==null) throw new IllegalArgumentException("The passed Annotation was null");
		PreparedStatement ps = null;
		ResultSet rset = null;
		try {
			ps = conn.prepareStatement(ANNOTATION_EXISTS_SQL);
			ps.setTimestamp(1, new Timestamp(utoms(annotation.getStartTime())));
			if(annotation.getTSUID()==null) {
				ps.setNull(2, Types.VARCHAR);
			} else {
				ps.setString(2, annotation.getTSUID());
			}
			rset = ps.executeQuery();
			rset.next();
			return rset.getInt(1) > 0;
		} catch (Exception ex) {
			throw new RuntimeException("Failed to lookup exists on Annotation [" + annotation + "]", ex);
		} finally {
			if(rset!=null) try { rset.close(); } catch (Exception x) {/* No Op */}
			if(ps!=null) try { ps.close(); } catch (Exception x) {/* No Op */}
		}
		
	}
	
	
	// ========================================================================================
	//	Search Impl.
	// ========================================================================================
	

	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.CatalogDBInterface#executeSearch(java.sql.Connection, net.opentsdb.search.SearchQuery, java.util.Set)
	 */
	@Override
	public abstract ResultSet executeSearch(Connection conn, SearchQuery query, Set<Closeable> closeables);
	
   /**
     * Executes a search query and returns the deferred for the results
     * @param query The query to execute
     * @param result The deferred to write the query results into
     * @return the deferred results
     */
    @Override
	public Deferred<SearchQuery> executeQuery(final SearchQuery query, final Deferred<SearchQuery> result) {
    	final ElapsedTime et = SystemClock.startClock();
    	final Set<Closeable> closeables = new HashSet<Closeable>();
    	Connection conn = null;
    	ResultSet rset = null;
    	try {
    		conn = dataSource.getConnection();
    		rset = executeSearch(conn, query, closeables);
	    	switch(query.getType()) {
				case ANNOTATION:
					List<Annotation> annResults = readAnnotations(rset);
					query.setTotalResults(annResults.size());
					query.setResults(new ArrayList<Object>(annResults));
					break;
				case UIDMETA:
					List<UIDMeta> uidResults = readUIDMetas(rset);
					query.setTotalResults(uidResults.size());
					query.setResults(new ArrayList<Object>(uidResults));					
					break;					
				case TSMETA:
					List<TSMeta> tsResults = readTSMetas(rset);
					query.setTotalResults(tsResults.size());
					query.setResults(new ArrayList<Object>(tsResults));					
					break;
				case TSUIDS:
					tsResults = readTSMetas(rset);
					query.setTotalResults(tsResults.size());
					List<Object> tsuids = new ArrayList<Object>(tsResults.size());
					for(TSMeta tsmeta: tsResults) {
						tsuids.add(tsmeta.getTSUID());
					}
					query.setResults(tsuids);
					break;
				case TSMETA_SUMMARY:
					tsResults = readTSMetas(rset);
					query.setTotalResults(tsResults.size());
					List<Object> tsummary = new ArrayList<Object>(tsResults.size());
					for(TSMeta tsmeta: tsResults) {
						tsummary.add(summarize(tsmeta));
					}
					query.setResults(tsummary);
					break;
				default:
					throw new RuntimeException("yeow. Unrecognized Query Type [" + query.getType() + "]");
	    	}
	    	query.setTime(et.elapsedMs());
	    	result.callback(query);
    	} catch (Exception ex) {
    		log.error("Failed to execute SearchQuery [{}]", query, ex);
    		result.callback(ex);
    	} finally {
    		if(rset!=null) try { rset.close(); } catch (Exception x) {/* No Op */}
    		if(conn!=null) try { conn.close(); } catch (Exception x) {/* No Op */}
    	}
    	return result;
    }

	
	
	
	// ======================================================================================================
	// Default Object exists impls.
	// ======================================================================================================
	
	
	
	/**
	 * Determines if the passed UIDMeta pair has been stored
	 * @param conn A supplied connection
	 * @param tagPairUid The UID of the tag pair
	 * @return true if stored, false otherwise
	 */
	public boolean tagPairStored(Connection conn, String tagPairUid) {
		PreparedStatement ps = null;
		ResultSet rset = null;
		try {
			ps = conn.prepareStatement(UID_PAIR_EXISTS_SQL);
			ps.setString(1, tagPairUid);
			rset = ps.executeQuery();
			rset.next();
			return rset.getInt(1)>0;
		} catch (SQLException sex) {
			throw new RuntimeException("Failed to lookup UIDMeta Pair [" + tagPairUid + "]", sex);
		} finally {
			if(rset!=null) try { rset.close(); } catch (Exception x) {/* No Op */}
			if(ps!=null) try { ps.close(); } catch (Exception x) {/* No Op */}
		}
	}

	
	
	//================================================================================================
	//  Static utility methods
	//================================================================================================
	
	/**
	 * Converts a millisecond based timestamp to a unix seconds based timestamp
	 * @param time The millisecond timestamp to convert
	 * @return a unix timestamp
	 */
	public static long mstou(long time) {
		return TimeUnit.SECONDS.convert(time, TimeUnit.MILLISECONDS);
	}
	
	/**
	 * Converts a unix second based timestamp to a long millisecond based timestamp
	 * @param time The unix timestamp to convert
	 * @return a long millisecond timestamp
	 */
	public static long utoms(long time) {
		return TimeUnit.MILLISECONDS.convert(time, TimeUnit.SECONDS);
	}
	
	/**
	 * Creates or updates a custom map
	 * @param customMap The map to create or update
	 * @return a map with the custom tags plus whatever was in the map passed in
	 */
	public static HashMap<String, String> fillInCustom(Map<String, String> customMap) {
		HashMap<String, String> _customMap = new HashMap<String, String>();
		_customMap.putAll(INIT_CUSTOM);
		if(customMap!=null) {
			_customMap.putAll(customMap);
		}
		if(_customMap.containsKey(VERSION_KEY)) {
			_customMap.put(VERSION_KEY, "" + (Integer.parseInt(customMap.get(VERSION_KEY))+1));
		} else {
			_customMap.put(VERSION_KEY, "1");
		}
		return _customMap;
	}	
	
    /**
     * Converts the passed TSMeta into a map summary
     * @param meta The meta to summarize
     * @return the summary map
     */
    public static Map<String, Object> summarize(TSMeta meta) {
    	final HashMap<String, Object> map = 
    			new HashMap<String, Object>(3);
    	map.put("tsuid", meta.getTSUID());
    	map.put("metric", meta.getMetric().getName());
    	final HashMap<String, String> tags = 
    			new HashMap<String, String>(meta.getTags().size() / 2);
    	int idx = 0;
    	String name = "";
    	for (final UIDMeta uid : meta.getTags()) {
    		if (idx % 2 == 0) {
    			name = uid.getName();
    		} else {
    			tags.put(name, uid.getName());
    		}
    		idx++;
    	}
    	map.put("tags", tags);
    	return map;
    }
    
	/**
	 * A closeable wrapper for a SQL ResultSet since it does not implement closeable...?
	 * @param rset The result set to close
	 * @return A closeable which will close the passed ResultSet when called
	 */
	public static Closeable close(final ResultSet rset) {
		return new Closeable() {
			@Override
			public void close() throws IOException {
				if(rset!=null) {
					try { rset.close(); } catch (Exception x) {/* No Op */}
				}
				
			}
		};
	}
	
	/**
	 * A closeable wrapper for a SQL Statement since it does not implement closeable...?
	 * @param statement The statement to close
	 * @return A closeable which will close the passed Statement when called
	 */
	public static Closeable close(final Statement statement) {
		return new Closeable() {
			@Override
			public void close() throws IOException {
				if(statement!=null) {
					try { statement.close(); } catch (Exception x) {/* No Op */}
				}
				
			}
		};
	}

	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.CatalogDBMXBean#getMetricInsertCount()
	 */
	@Override
	public long getMetricInsertCount() {
		return getOpCount(METRIC_INSERT_CNT);
	}

	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.CatalogDBMXBean#getMetricUpdateCount()
	 */
	@Override
	public long getMetricUpdateCount() {
		return getOpCount(METRIC_UPDATE_CNT);
	}

	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.CatalogDBMXBean#getTagKInsertCount()
	 */
	@Override
	public long getTagKInsertCount() {
		return getOpCount(TAGK_INSERT_CNT);
	}

	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.CatalogDBMXBean#getTagKUpdateCount()
	 */
	@Override
	public long getTagKUpdateCount() {
		return getOpCount(TAGK_UPDATE_CNT);
	}

	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.CatalogDBMXBean#getTagVInsertCount()
	 */
	@Override
	public long getTagVInsertCount() {
		return getOpCount(TAGV_INSERT_CNT);
	}

	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.CatalogDBMXBean#getTagVUpdateCount()
	 */
	@Override
	public long getTagVUpdateCount() {
		return getOpCount(TAGV_UPDATE_CNT);
	}

	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.CatalogDBMXBean#getTSMetaInsertCount()
	 */
	@Override
	public long getTSMetaInsertCount() {
		return getOpCount(TSMETA_INSERT_CNT);
	}

	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.CatalogDBMXBean#getTSMetaUpdateCount()
	 */
	@Override
	public long getTSMetaUpdateCount() {
		return getOpCount(TSMETA_UPDATE_CNT);
	}

	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.CatalogDBMXBean#getAnnotationInsertCount()
	 */
	@Override
	public long getAnnotationInsertCount() {
		return getOpCount(ANN_INSERT_CNT);
	}

	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.CatalogDBMXBean#getAnnotationUpdateCount()
	 */
	@Override
	public long getAnnotationUpdateCount() {
		return getOpCount(ANN_UPDATE_CNT);
	}
	

	/**
	 * Returns the URL of the connected database
	 * @return the URL of the connected database
	 */
	public String getURL() {
		return dbUrl;
	}

	/**
	 * Returns the username of the database user
	 * @return the username of the database user
	 */
	public String getUserName() {
		return dbUser;
	}

	/**
	 * Returns the driver name 
	 * @return the driver name 
	 */
	public String getDriverName() {
		return dbDriverName;
	}
	
	/**
	 * Returns the driver version
	 * @return the driver version
	 */
	public String getDriverVersion() {
		return dbDriverVersion;
	}
	
	
	/**
	 * Retrieves the name of this database product.
	 * @return the name of this database product
	 */
	public String getDatabaseProductName() {
		return dbName;
	}
	
	/**
	 * Retrieves the version number of this database product.
	 * @return the version number name of this database product
	 */
	public String getDatabaseProductVersion() {
		return dbVersion;
	}
	

}
