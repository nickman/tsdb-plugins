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
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
import net.opentsdb.search.SearchQuery.SearchType;
import net.opentsdb.uid.UniqueId;
import net.opentsdb.uid.UniqueId.UniqueIdType;
import net.opentsdb.utils.JSONException;

import org.helios.tsdb.plugins.event.TSDBSearchEvent;
import org.helios.tsdb.plugins.handlers.logging.LoggerManager;
import org.helios.tsdb.plugins.handlers.logging.LoggerManagerFactory;
import org.helios.tsdb.plugins.meta.MetaSynchronizer;
import org.helios.tsdb.plugins.remoting.json.JSONRequest;
import org.helios.tsdb.plugins.remoting.json.JSONRequestRouter;
import org.helios.tsdb.plugins.remoting.json.annotations.JSONRequestHandler;
import org.helios.tsdb.plugins.remoting.json.annotations.JSONRequestService;
import org.helios.tsdb.plugins.service.IPluginContextResourceFilter;
import org.helios.tsdb.plugins.service.IPluginContextResourceListener;
import org.helios.tsdb.plugins.service.PluginContext;
import org.helios.tsdb.plugins.util.ConfigurationHelper;
import org.helios.tsdb.plugins.util.JMXHelper;
import org.helios.tsdb.plugins.util.SystemClock;
import org.helios.tsdb.plugins.util.SystemClock.ElapsedTime;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferFactory;
import org.jboss.netty.buffer.ChannelBufferOutputStream;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.buffer.DirectChannelBufferFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
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
@JSONRequestService(name="sqlcatalog", description="The SQL metric catalog service")
public abstract class AbstractDBCatalog implements CatalogDBInterface, CatalogDBMXBean {
	/** Instance logger */
	protected Logger log = LoggerFactory.getLogger(getClass());
	/** Logger adapter for setting logger levels */
	protected LoggerManager loggerManager = LoggerManagerFactory.getLoggerManager(getClass());
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
	
	/** The SQLWorker to manage JDBC Ops */
	protected SQLWorker sqlWorker = null;
	

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
	
	
	/** The number of batched TSMeta inserts */
	protected int batchedtsMetaInserts = 0;
	
	/** Creates direct buffers for streaming conversion from SQL webrowset XML documents to JSON */
	protected final ChannelBufferFactory streamBuffers = new DirectChannelBufferFactory(2048);

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
	public static final String TSD_DELETE_ANNOTATION = "DELETE FROM TSD_ANNOTATION WHERE START_TIME = ? AND (FQNID = ? OR FQNID IS NULL)";
	/** The SQL template to delete a UIDMeta */
	public static final String TSD_DELETE_UID = "DELETE FROM TSD_%s WHERE XUID = ?";
	/** The SQL template to delete a TAGK or TAGV UIDMeta TagPair Parent */
	public static final String TSD_DELETE_UID_PARENT = "DELETE FROM TSD_TAGPAIR WHERE %s = ?";
	
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
		
		cds = CatalogDataSource.getInstance();
		cds.initialize(pluginContext);
		dataSource = cds.getDataSource();
		sqlWorker = SQLWorker.getInstance(dataSource);
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
		JMXHelper.registerMBean(this, JMXHelper.objectName(new StringBuilder(getClass().getPackage().getName()).append(":service=TSDBCatalog")));
		final AbstractDBCatalog finalMe = this;
		pluginContext.addResourceListener(
				new IPluginContextResourceListener() {
					@Override
					public void onResourceRegistered(String name, Object resource) {
						((JSONRequestRouter)resource).registerJSONService(finalMe);	
					}
				},
				new IPluginContextResourceFilter() {
					@Override
					public boolean include(String name, Object resource) {						
						return name.equals(JSONRequestRouter.class.getSimpleName());
					}
				}
		);		
		log.info("\n\t================================================\n\tDB Initializer Started\n\tJDBC URL:{}\n\t================================================", cds.getConfig().getJdbcUrl());
	}
	
	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.CatalogDBInterface#triggerSyncQueueFlush()
	 */
	public void triggerSyncQueueFlush() {
//		if(syncQueuePollerEnabled) {
//			if(syncQueueProcessor==null) throw new RuntimeException("SyncQueue Polling Enabled but syncQueueProcessor is null");
//			syncQueueProcessor.run();
//		}
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
	
	/** Keeps track of batched UIDs for the duration of processEvents. */
	protected final Set<String> batchedUids = new HashSet<String>(1024);
	
	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.CatalogDBInterface#processEvents(java.sql.Connection, java.util.Set)
	 */
	@Override
	public void processEvents(Connection conn, Set<TSDBSearchEvent> events) {		
		int ops = 0;
		setConnectionProperty(conn, TSD_CONN_TYPE, SYNC_CONN_FLAG);
		ElapsedTime et = SystemClock.startClock();
		
		Set<String> batchedUidPairs = new HashSet<String>(events.size());
		Set<Annotation> annotations = new HashSet<Annotation>();
		BatchMileStone latch = null;
		final boolean trace = log.isTraceEnabled();
		try {			
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
						if(shouldIgnore(event.annotation)) continue;
						if(trace) log.trace("Deleting annotation [{}]", event.annotation);
						deleteAnnotation(conn, event.annotation);
						break;
					case ANNOTATION_INDEX:
						if(shouldIgnore(event.annotation)) continue;
						if(trace) log.trace("Indexing annotation [{}]", event.annotation);
						annotations.add(event.annotation);
						break;
					case TSMETA_DELETE:					
						deleteTSMeta(conn, event.tsuid);
						if(trace) log.trace("Deleting TSMeta [{}]", event.tsuid);
						break;
					case TSMETA_INDEX:
						if(shouldIgnore(event.tsMeta)) continue;
						TSMeta tsMeta = event.tsMeta;
						if(trace) log.trace("Indexing TSMeta [{}]", event.tsMeta);
						processTSMeta(batchedUidPairs, conn, tsMeta);
						break;
					case UIDMETA_DELETE:
						if(shouldIgnore(event.uidMeta)) continue;
						if(trace) log.trace("Deleting UIDMeta [{}]", event.uidMeta);
						deleteUIDMeta(conn, event.uidMeta);
						break;
					case UIDMETA_INDEX:		
						if(shouldIgnore(event.uidMeta)) continue;
						if(trace) log.trace("Indexing UIDMeta [{}]", event.uidMeta);
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
			} catch (Throwable tx) {
				log.error("Event Process Loop Error", tx);
				throw new RuntimeException("Event Process Loop Error", tx);
			}
			
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
			log.info("Executing [{}] Batched TSMeta Inserts", batchedtsMetaInserts);
			if(tsMetaFqnPs!=null) {
				executeBatch(tsMetaFqnPs);
				tsMetaFqnPs.clearBatch();				
				batchedtsMetaInserts = 0;
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
			setConnectionProperty(conn, TSD_CONN_TYPE, "");
			batchedUids.clear();
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
	 * Examines the custom map of the passed object if it is an Annotation, UIDMeta or TSMeta.
	 * If null or not one of those classes, returns true.
	 * If the class matches, returns true if the instances custom map contains the {@link SyncQueueProcessor.IGNORE_TAG_NAME} tag set to true.
	 * Otherwise returns false.
	 * @param custom The object to test for an ignore tag
	 * @return true to ignore, false otherwise
	 */
	protected boolean shouldIgnore(Object custom) {
		if(custom==null) return true;
		Map<String, String> cmap = null;
		if(custom instanceof Annotation) {
			cmap = ((Annotation)custom).getCustom();
		} else if(custom instanceof UIDMeta) {
			cmap = ((UIDMeta)custom).getCustom();
		} else if(custom instanceof TSMeta) {
			cmap = ((TSMeta)custom).getCustom();
		} else {
			return true;
		}
		if(cmap==null) return false;
		String enabled = cmap.get(SyncQueueProcessor.IGNORE_TAG_NAME);
		return ("true".equalsIgnoreCase(enabled));
	}
	
	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.CatalogDBInterface#bindUIDMeta(net.opentsdb.meta.UIDMeta, java.sql.PreparedStatement)
	 */
	@Override
	public void bindUIDMeta(UIDMeta uidMeta, PreparedStatement ps) throws SQLException {
		throw new UnsupportedOperationException("Deperectaed");
	}
	
	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.CatalogDBInterface#bindUIDMetaUpdate(net.opentsdb.meta.UIDMeta, java.sql.PreparedStatement)
	 */
	@Override
	public void bindUIDMetaUpdate(UIDMeta uidMeta, PreparedStatement ps) throws SQLException {
		throw new UnsupportedOperationException("Deperectaed");
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
						uidMetaMetricIndexPs = sqlWorker.batch(conn, uidMetaMetricIndexPs, getUIDMetaMetricIndexSQL(), getInsertBinds(uidMeta));
						incrementOpCounter(METRIC_INSERT_CNT);
						break;
					case TAGK:
						uidMetaTagKIndexPs = sqlWorker.batch(conn, uidMetaTagKIndexPs, getUIDMetaTagKIndexSQL(), getInsertBinds(uidMeta));								
						incrementOpCounter(TAGK_INSERT_CNT);
						break;
					case TAGV:
						uidMetaTagVIndexPs = sqlWorker.batch(conn, uidMetaTagVIndexPs, getUIDMetaTagVIndexSQL(), getInsertBinds(uidMeta));								
						incrementOpCounter(TAGV_INSERT_CNT);
						break;
					default:
						log.warn("yeow. Unexpected UIDMeta type:{}", uidMeta.getType().name());
						break;
				}				
			} else {
				switch(uidMeta.getType()) {  
					case METRIC:							
						uidMetaMetricUpdatePs = sqlWorker.batch(conn, uidMetaMetricUpdatePs, getUIDMetaMetricUpdateSQL(), getUpdateBinds(uidMeta));
						incrementOpCounter(METRIC_UPDATE_CNT);
						break;
					case TAGK:
						uidMetaTagKUpdatePs = sqlWorker.batch(conn, uidMetaTagKUpdatePs, getUIDMetaTagKUpdateSQL(), getUpdateBinds(uidMeta));
						incrementOpCounter(TAGK_UPDATE_CNT);
						break;
					case TAGV:
						uidMetaTagVUpdatePs = sqlWorker.batch(conn, uidMetaTagVUpdatePs, getUIDMetaTagVUpdateSQL(), getUpdateBinds(uidMeta));
						incrementOpCounter(TAGV_UPDATE_CNT);
						break;
					default:
						log.warn("yeow. Unexpected UIDMeta type:{}", uidMeta.getType().name());
						break;
				}								
			}
		} catch (Exception ex) {
			log.error("Failed to process UIDMeta [" + uidMeta + "]", ex);
			throw new RuntimeException("Failed to process UIDMeta [" + uidMeta + "]", ex);
		}
	}
	
	/**
	 * Returns an array of binding values for a UIDMeta insertion
	 * @param uidMeta The UIDMeta to create binding values for
	 * @return the insertion binding values
	 */
	protected Object[] getInsertBinds(UIDMeta uidMeta) {
		int version = incrementVersion(uidMeta);
		long created = uidMeta.getCreated();						
		return new Object[]{
				uidMeta.getUID(), version, uidMeta.getName(), 
				created>0 ? new Timestamp(utoms(created)) : new Timestamp(SystemClock.time()),
				uidMeta.getDescription(),
				uidMeta.getDisplayName(),
				uidMeta.getNotes(),
				JSONMapSupport.nokToString(uidMeta.getCustom())				
		};
	}
	
	/**
	 * Returns an array of binding values for a UIDMeta update
	 * @param uidMeta The UIDMeta to create binding values for
	 * @return the update binding values
	 */
	protected Object[] getUpdateBinds(UIDMeta uidMeta) {
		int version = incrementVersion(uidMeta);					
		return new Object[]{
			version, uidMeta.getName(),
			uidMeta.getDescription(),
			uidMeta.getDisplayName(),
			uidMeta.getNotes(),
			JSONMapSupport.nokToString(uidMeta.getCustom()),				
			uidMeta.getUID()
		};				
	}
	
	
	
	/**
	 * Retrieves the ANNID for the passed annotation
	 * @param conn The connection to use
	 * @param annotation The annotation to search for
	 * @return the ANNID of the passed annotation
	 */
	protected long getAnnIdForAnnotation(Connection conn, Annotation annotation) {
		return sqlWorker.sqlForLong(GET_ANNOTATION_ID_SQL,
				new Timestamp(utoms(annotation.getStartTime())),
				annotation.getTSUID()
		);
	}
	
	/*
	 * PreparedStatement batch(PreparedStatement ps, SQL,
	 * 
	 * if ps is null, prepare --- return
	 * bind
	 * addBatch
	 * 
	 */
	
	/**
	 * Increments or sets the version key on an annotation
	 * @param a The annotation to version increment 
	 * @return the new version
	 */
	protected int incrementVersion(Annotation a) {
		HashMap<String, String> custom = a.getCustom();
		if(custom==null) {
			custom = new HashMap<String, String>(1);
			a.setCustom(custom);
		}
		return JSONMapSupport.incrementAndGet(custom, 1, VERSION_KEY);		
	}
	
	/**
	 * Increments or sets the version key on a TSMeta
	 * @param tsmeta The TSMeta to version increment 
	 * @return the new version
	 */
	protected int incrementVersion(TSMeta tsmeta) {
		HashMap<String, String> custom = tsmeta.getCustom();
		if(custom==null) {
			custom = new HashMap<String, String>(1);
			tsmeta.setCustom(custom);
		}
		return JSONMapSupport.incrementAndGet(custom, 1, VERSION_KEY);				
	}
	
	/**
	 * Increments or sets the version key on a UIDMeta
	 * @param uidmeta The UIDMeta to version increment 
	 * @return the new version
	 */
	protected int incrementVersion(UIDMeta uidmeta) {
		Map<String, String> custom = uidmeta.getCustom();
		if(custom==null) {
			custom = new HashMap<String, String>(1);
			uidmeta.setCustom((HashMap<String, String>)custom);
		}
		return JSONMapSupport.incrementAndGet(custom, 1, VERSION_KEY);				
	}
	
	
	/**
	 * Executes a batched update for the passed modified Annotation
	 * @param conn The connection to update on
	 * @param a The changed Annotation
	 */
	protected void updateAnnotation(Connection conn, Annotation a) {
		long annId = getAnnIdForAnnotation(conn, a);
		annotationsUpdatePs = sqlWorker.batch(conn, annotationsUpdatePs, TSD_UPDATE_ANNOTATION, 
				incrementVersion(a),
				new Timestamp(utoms(a.getStartTime())),
				a.getDescription(),
				a.getNotes(),
				a.getTSUID()==null ? null : getFqnIdForTsUid(conn, a.getTSUID()),
				a.getEndTime()==0 ? null : new Timestamp(utoms(a.getEndTime())),
				JSONMapSupport.nokToString(a.getCustom()),
				annId
		);		
	}	
	
	
	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.CatalogDBInterface#processAnnotation(java.sql.Connection, net.opentsdb.meta.Annotation)
	 */
	@Override
	public void processAnnotation(Connection conn, Annotation annotation) {
		if(exists(conn, annotation)) {
			updateAnnotation(conn, annotation);
			return;
		}
		long startTime = annotation.getStartTime(); 
		if(startTime==0) {
			startTime = SystemClock.unixTime();
			annotation.setStartTime(startTime);
		}
		long annId = annSequence.next();		
		int version = incrementVersion(annotation);
		fillInCustom(annotation.getCustom());
		long endTime = annotation.getEndTime();
		annotationsPs = sqlWorker.batch(conn, annotationsPs, TSD_INSERT_ANNOTATION, 
				annId, version,
				new Timestamp(utoms(startTime)),
				annotation.getDescription(),
				annotation.getNotes(),
				annotation.getTSUID()==null ? null : H2Support.fqnId(conn, annotation.getTSUID()),
				endTime==0 ? null : new Timestamp(utoms(endTime)),
				JSONMapSupport.nokToString(annotation.getCustom())
		);
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
		long fqnId = getFqnIdForTsUid(conn, tsMeta.getTSUID());
		int version = incrementVersion(tsMeta);
		tsMetaFqnUpdatePs = sqlWorker.batch(conn, tsMetaFqnUpdatePs, TSUID_UPDATE_SQL, 
				version,
				tsMeta.getMetric().getUID(),
				getFQN(tsMeta),
				isNaNToNull() && Double.isNaN(tsMeta.getMax()) ? null : tsMeta.getMax(),
				isNaNToNull() && Double.isNaN(tsMeta.getMin()) ? null : tsMeta.getMin(),
				tsMeta.getDataType(),
				tsMeta.getDescription(),
				tsMeta.getDisplayName(),
				tsMeta.getNotes(),
				tsMeta.getUnits(),
				tsMeta.getRetention(),
				JSONMapSupport.nokToString(tsMeta.getCustom()),
				fqnId						
		);
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
	 * Preprocesses the UIDMetas in a new TSMeta incase they were missed before now
	 * @param conn The connection to process on
	 * @param uidMetas The UIDMetas to process
	 */
	protected void preProcessUIDMeta(Connection conn, Collection<UIDMeta> uidMetas) {
		for(UIDMeta uidMeta: uidMetas) {
			if(!batchedUids.contains(uidMeta.toString())) {
				processUIDMeta(conn, uidMeta);
				batchedUids.add(uidMeta.toString());
			}
		}
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
		Collection<UIDMeta> uidMetas = new ArrayList<UIDMeta>(tsMeta.getTags());
		uidMetas.add(tsMeta.getMetric());
		preProcessUIDMeta(conn, uidMetas);
		
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
		long fqnSeq = fqnSequence.next();		
		int version = incrementVersion(tsMeta);
		tsMetaFqnPs = sqlWorker.batch(conn, tsMetaFqnPs, TSUID_INSERT_SQL, 
				fqnSeq,	version,
				tsMeta.getMetric().getUID(),
				fqn.toString(),
				tsMeta.getTSUID(),
				new Timestamp(utoms(tsMeta.getCreated())),
				isNaNToNull() && Double.isNaN(tsMeta.getMax()) ? null : tsMeta.getMax(),
				isNaNToNull() && Double.isNaN(tsMeta.getMin()) ? null : tsMeta.getMin(),
				tsMeta.getDataType(),
				tsMeta.getDescription(),
				tsMeta.getDisplayName(),
				tsMeta.getNotes(),
				tsMeta.getUnits(),
				tsMeta.getRetention(),
				JSONMapSupport.nokToString(tsMeta.getCustom())					
		);
		batchedtsMetaInserts++;
		LinkedList<UIDMeta> pairs = new LinkedList<UIDMeta>(tsMeta.getTags());
		int pairCount = tsMeta.getTags().size()/2;
		int leaf = pairCount-1;
		for(short i = 0; i < pairCount; i++) {
			String pairUID = pairs.removeFirst().getUID() + pairs.removeFirst().getUID();
			uidMetaTagPairFQNPs = sqlWorker.batch(conn, uidMetaTagPairFQNPs, TSD_FQN_TAGPAIR_SQL, 
					fqnTpSequence.next(),
					fqnSeq,
					pairUID,
					i,
					(i==leaf) ? "L" : "B"
			);				
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
		uidMetaTagPairPs = sqlWorker.batch(conn, uidMetaTagPairPs, INSERT_TAGPAIR_SQL, tagPairUid, tagPair[0].getUID(), tagPair[1].getUID(), tagPair[0].getName() + "=" + tagPair[1].getName());
		batchUidPairs.add(tagPairUid);
		return null;
	}
	
	// ==================================================================================================
	//  Object Deletion 
	// ==================================================================================================
	
	/**
	 * Deletes a UIDMeta.
	 * Note that this deletes the parent tag-pair.
	 * @param conn The connection to use
	 * @param uidMeta The UIDMeta to delete
	 */
	@Override
	public void deleteUIDMeta(Connection conn, UIDMeta uidMeta) {
		if(uidMeta.getType()==UniqueIdType.TAGK || uidMeta.getType()==UniqueIdType.TAGV) {
			sqlWorker.executeUpdate(conn, String.format(TSD_DELETE_UID_PARENT, uidMeta.getType().name()), uidMeta.getUID());
		}
		sqlWorker.executeUpdate(conn, String.format(TSD_DELETE_UID, uidMeta.getType().name()), uidMeta.getUID());
	}
	
	/**
	 * Deletes a TSMeta
	 * @param conn The connection to use
	 * @param tsUid The TSUid of the TSMeta to delete
	 */
	@Override
	public void deleteTSMeta(Connection conn, String tsUid) {		
		sqlWorker.executeUpdate(conn, TSD_DELETE_TS, tsUid);
	}
	
	/**
	 * Deletes an Annotation
	 * @param conn The connection to use
	 * @param annotation The annotation to delete
	 */
	@Override
	public void deleteAnnotation(Connection conn, Annotation annotation) {		
		String pk = annotation.getCustom().get(PK_KEY);
		if(pk!=null) {
			long annId = Long.parseLong(pk);
			sqlWorker.executeUpdate(conn, "DELETE FROM TSD_ANNOTATION WHERE ANNID = ?", annId);
			return;
		} else {
			sqlWorker.executeUpdate(conn, TSD_DELETE_ANNOTATION, 						
					new Timestamp(utoms(annotation.getStartTime())),
					annotation.getTSUID()==null ? null : 
						H2Support.fqnId(conn, annotation.getTSUID())==-1 ? null : 
							H2Support.fqnId(conn, annotation.getTSUID())
			);
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
				Map<String, String> custom = meta.getCustom();
				if(custom==null) {
					custom = new HashMap<String, String>(3);
					custom.put(PK_KEY, rset.getString("FQNID"));
				}
				custom.put(TSMETA_METRIC_KEY, rset.getString("METRIC_UID"));
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
			ResultSetMetaData rsmd = rset.getMetaData();
			boolean hasTsUid = false;
			String tsUidColName = null;
			int colCount = rsmd.getColumnCount();
			for(int i = 0; i < colCount; i++) {
				if(rsmd.getColumnName(i+1).equalsIgnoreCase("TSUID")) {
					tsUidColName = rsmd.getColumnName(i+1);
					hasTsUid = true;
					break;
				}
			}
			
			while(rset.next()) {
				Annotation meta = new Annotation();
				
				meta.setCustom((HashMap<String, String>) JSONMapSupport.read(rset.getString("CUSTOM")));
				meta.setDescription(rset.getString("DESCRIPTION"));
				meta.setNotes(rset.getString("NOTES"));
				if(hasTsUid) {
					meta.setTSUID(rset.getString(tsUidColName));
				} else {
					long fqnId = rset.getLong("FQNID");
					meta.setTSUID(getTSUIDForFQNId(fqnId));
				}
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
	
	/**
	 * Returns the TSUID for the passed FQNID
	 * @param fqnId The FQNID of the TSMeta to get the TSUID for
	 * @return the TSUID
	 */
	public String getTSUIDForFQNId(long fqnId) {
		return sqlWorker.sqlForString("SELECT TSUID from TSD_TSMETA WHERE FQNID = ?", fqnId);
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
		return sqlWorker.sqlForBool(conn, TSUID_EXISTS_SQL, tsMeta.getTSUID());
	}
	
	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.CatalogDBInterface#exists(net.opentsdb.meta.TSMeta)
	 */
	@Override
	public boolean exists(TSMeta tsMeta) {
		if(tsMeta==null) throw new IllegalArgumentException("The passed TSMeta was null");
		return sqlWorker.sqlForBool(TSUID_EXISTS_SQL, tsMeta.getTSUID());
	}


	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.CatalogDBInterface#exists(java.sql.Connection, net.opentsdb.meta.UIDMeta)
	 */
	@Override
	public boolean exists(Connection conn, UIDMeta uidMeta) {
		if(uidMeta==null) throw new IllegalArgumentException("The passed UIDMeta was null");
		return sqlWorker.sqlForBool(conn, String.format(UID_EXISTS_SQL, uidMeta.getType().name()), uidMeta.getUID());
	}
	
	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.CatalogDBInterface#exists(net.opentsdb.meta.UIDMeta)
	 */
	@Override
	public boolean exists(UIDMeta uidMeta) {
		if(uidMeta==null) throw new IllegalArgumentException("The passed UIDMeta was null");
		return sqlWorker.sqlForBool(String.format(UID_EXISTS_SQL, uidMeta.getType().name()), uidMeta.getUID());
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.CatalogDBInterface#exists(net.opentsdb.meta.Annotation)
	 */
	@Override
	public boolean exists(Annotation annotation) {
		if(annotation==null) throw new IllegalArgumentException("The passed Annotation was null");
		return sqlWorker.sqlForBool(ANNOTATION_EXISTS_SQL, new Timestamp(utoms(annotation.getStartTime())), annotation.getTSUID());
	}
	

	/**
	 * Determines if the passed annotation is already stored
	 * @param conn The connection to query on
	 * @param annotation The annotation to verify
	 * @return true if the passed annotation is already stored, false otherwise
	 */
	public boolean exists(Connection conn, Annotation annotation) {
		if(annotation==null) throw new IllegalArgumentException("The passed Annotation was null");
		return sqlWorker.sqlForBool(conn, ANNOTATION_EXISTS_SQL, new Timestamp(utoms(annotation.getStartTime())), annotation.getTSUID());
	}
	
	
	// ========================================================================================
	//	Search Impl.
	// ========================================================================================
	
	
	

//	/**
//	 * {@inheritDoc}
//	 * @see net.opentsdb.catalog.CatalogDBInterface#executeSearch(java.sql.Connection, net.opentsdb.search.SearchQuery, java.util.Set)
//	 */
//	@Override
//	public abstract ResultSet executeSearch(Connection conn, SearchQuery query, Set<Closeable> closeables);
//	
	
	public String search(String type, String query, int limit, int startIndex) {
		SearchType searchType = null;
		try {
			searchType = SearchType.valueOf(type.trim().toUpperCase());
		} catch (Exception x) {
			throw new RuntimeException("Invalid Search Type [" + type + "]");
		}
		if(limit<0) throw new RuntimeException("Invalid Limit [" + limit + "]");
		if(startIndex<0) throw new RuntimeException("Invalid Start Index [" + startIndex + "]");
		SearchQuery searchQuery = new SearchQuery();
		searchQuery.setLimit(limit);
		searchQuery.setStartIndex(startIndex);
		searchQuery.setType(searchType);
		searchQuery.setQuery(query);
		
		try {
			Deferred<SearchQuery> d = new Deferred<SearchQuery>();
			executeQuery(searchQuery, d);
			d.joinUninterruptibly(30000);
			return serializeToString(searchQuery);
		} catch (Exception ex) {
			Throwable cause = ex.getCause();
			throw new RuntimeException("DBInterface error executing search:" + ex + "\n:" + cause);
		}	
	}
	
	protected static final ObjectMapper jsonMapper = new ObjectMapper();
	
	static {
		jsonMapper.enable(SerializationFeature.INDENT_OUTPUT);
	}
	 

	
	  /**
	   * Serializes the given object to a JSON string
	   * @param object The object to serialize
	   * @return A JSON formatted string
	   * @throws IllegalArgumentException if the object was null
	   * @throws JSONException if the object could not be serialized
	   */
	  public static final String serializeToString(final Object object) {
	    if (object == null)
	      throw new IllegalArgumentException("Object was null");
	    try {
	      return jsonMapper.writeValueAsString(object);
	    } catch (JsonProcessingException e) {
	      throw new JSONException(e);
	    }
	  }	
	
	
   /**
     * Executes a search query and returns the deferred for the results
     * @param query The query to execute
     * @param result The deferred to write the query results into
     * @return the deferred results
     */
    @Override
	public Deferred<SearchQuery> executeQuery(final SearchQuery query, final Deferred<SearchQuery> result) {
    	final ElapsedTime et = SystemClock.startClock();
    	
    	Connection conn = null;
    	try {
    		conn = dataSource.getConnection();
    		List<?> matches = executeSearch(conn, query);
    		query.setTotalResults(matches.size());
	    	switch(query.getType()) {
				case ANNOTATION:
				case UIDMETA:
				case TSMETA:					
					query.setResults((List<Object>) matches);
					break;
				case TSUIDS:
					List<Object> tsuids = new ArrayList<Object>(matches.size());
					for(Object meta: matches) {
						tsuids.add(((TSMeta)meta).getTSUID());
					}
					query.setResults(tsuids);
					break;
				case TSMETA_SUMMARY:
					List<Object> tsummary = new ArrayList<Object>(matches.size());
					for(Object meta: matches) {
						tsummary.add(summarize((TSMeta)meta));
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
		return sqlWorker.sqlForBool(conn, UID_PAIR_EXISTS_SQL, tagPairUid);
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
    @SuppressWarnings("resource")
	public Map<String, Object> summarize(TSMeta meta) {
    	try {
	    	
    		final HashMap<String, Object> map = 
	    			new HashMap<String, Object>(3);
	    	map.put("tsuid", meta.getTSUID());
	    	String name = sqlWorker.sqlForString("SELECT NAME FROM TSD_METRIC WHERE XUID = ?", meta.getCustom().remove(TSMETA_METRIC_KEY));
	    	if(name!=null) {
	    		map.put("metric", name);
	    	}
	    	ResultSet rset = sqlWorker.executeQuery("SELECT NAME FROM TSD_TAGPAIR T ,TSD_FQN_TAGPAIR F WHERE F.XUID = T.XUID AND F.FQNID = ? ORDER BY PORDER", true, Long.parseLong(meta.getCustom().get(PK_KEY)));
	    	final Map<String, String> tags = 
	    			new LinkedHashMap<String, String>();

	    	while(rset.next()) {
	    		String[] pair = rset.getString(1).split("=");
	    		tags.put(pair[0], pair[1]);	    		
	    	}
	    	map.put("tags", tags);
	    	return map;
    	} catch (Exception ex) {
    		throw new RuntimeException("Failed to summarize TSMeta list", ex);
    	}
    }
    
	/**
	 * WebSocket service exposed method for {@link #executeSQLForJson(boolean, String)}.
	 * @param request The JSON request
	 * <p>e.g.:
	 * 	<pre>
	 * 		{"t": "req", "rid": 7, "svc": "sqlcatalog", "op": "execsql", "args": {"includemeta":"true", "sql":"SELECT * FROM TSD_TSMETA"}}
	 * 	</pre>
	 * </p>
	 */
	@JSONRequestHandler(name="execsql", description="Executes the passed SQL statement and returns the results as JSON")
	public void executeSQLForJson(JSONRequest request) {
		log.info("JSONRequest:" + request);
		request.allowDefaults(true);
		boolean includeMeta =  request.get("includemeta", false);
		int maxRows = request.get("maxrows", 0);
		int startAt = request.get("startat", 0);

		request.allowDefaults(false);
		String sqlText = request.get("sql", "");
		if(sqlText.trim().isEmpty()) {
			request.error("The passed SQL was null or empty").send();
			return;
		}
		log.info("Executing SQL [{}], Options: meta:{}, maxrows:{}, startat:{} ", sqlText, includeMeta, maxRows, startAt);
		request.response().setContent(_executeSQLForJson(includeMeta, maxRows, startAt, sqlText)).send();
	}
    
	/** UTF-8 Charset */
	public static final Charset UTF8_CHARSET = Charset.forName("UTF-8");
	
	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.CatalogDBInterface#executeSQLForJson(boolean, java.lang.String)
	 */
	@Override
	public String executeSQLForJson(boolean includeMeta, String sqlText) {
		return executeSQLForJson(includeMeta, 0, 0, sqlText);
	}
	
	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.CatalogDBInterface#executeSQLForJson(boolean, int, int, java.lang.String)
	 */
	@Override
    public String executeSQLForJson(boolean includeMeta, int maxRows, int startAt, String sqlText) {
		return _executeSQLForJson(includeMeta, maxRows, startAt, sqlText).toString(UTF8_CHARSET);
	}

	/**
	 * Executes the passed SQL statement and returns the results as JSON
	 * @param includeMeta true to include meta-data, false to exclude it
	 * @param sqlText The SQL statement to execute
	 * @return a ChannelBuffer containing the JSON representing the result of the SQL statement
	 */
    protected ChannelBuffer _executeSQLForJson(boolean includeMeta, String sqlText) {
    	return _executeSQLForJson(includeMeta, 0, 0, sqlText); 
    }
	
	
	/**
	 * Executes the passed SQL statement and returns the results as JSON
	 * @param includeMeta true to include meta-data, false to exclude it
	 * @param maxRows The maximum number of rows to return. A value of <b><code>0</code></p> or less means all rows.
	 * @param startAt The row number to start at, the first row being 0.
	 * @param sqlText The SQL statement to execute
	 * @return a ChannelBuffer containing the JSON representing the result of the SQL statement
	 */
    protected ChannelBuffer _executeSQLForJson(boolean includeMeta, int maxRows, int startAt, String sqlText) {
    	if(sqlText==null || sqlText.trim().isEmpty()) throw new IllegalArgumentException("The passed SQL statement was null or empty");
    	final ElapsedTime et = SystemClock.startClock();
    	boolean isSelect = sqlText.trim().toUpperCase().startsWith("SELECT");
    	Connection conn = null;
    	PreparedStatement ps = null;
    	ResultSet rset = null;
    	final int _maxRows = maxRows<1 ? Integer.MAX_VALUE : maxRows;
    	final int _startAt = startAt-1;    	
    	final ChannelBuffer streamBuffer = ChannelBuffers.dynamicBuffer(streamBuffers);
    	final ChannelBufferOutputStream out = new ChannelBufferOutputStream(streamBuffer); 
    	final JsonGenerator generator; 
    	try {
    		generator = jsonMapper.getFactory().createJsonGenerator(out, JsonEncoding.UTF8);    				    		
    		conn = dataSource.getConnection();
    		ps = conn.prepareStatement(sqlText);
    		if(isSelect) {
    			// ===== start root =====
    			generator.writeStartObject();
    			
    			rset = ps.executeQuery();    							
				ResultSetMetaData rsmd = rset.getMetaData();
				int colCount = rsmd.getColumnCount();
				
				// ===== start meta data =====
				
				Map<Integer, String> nameDecode = new HashMap<Integer, String>(colCount);
				if(includeMeta) {
					generator.writeFieldName("meta");
					generator.writeStartArray();
				}
				for(int i = 1; i <= colCount; i++) {
					nameDecode.put(i, rsmd.getColumnName(i).toLowerCase());
					if(includeMeta) {
						generator.writeStartObject();
						generator.writeStringField("name", rsmd.getColumnName(i));
						generator.writeStringField("type", rsmd.getColumnTypeName(i));
						generator.writeStringField("class", rsmd.getColumnClassName(i));
						generator.writeEndObject();
					}
				}
    			if(includeMeta) {
    				generator.writeEndArray(); 
    			}

    			// ===== start data =====
    			
				generator.writeFieldName("data");
				generator.writeStartArray();
    			
//    			ArrayNode dataNode = jsonMapper.createArrayNode();
    			int cnt = 0;
    			
    			// Fast-forward if start-at > 0
    			if(startAt > 0) {
    				while(rset.next() && cnt <= _startAt) {
    					cnt++;
    				}
    			}
    			
    			cnt = 0;
    			while(rset.next()) {
    				cnt++;
    				if(cnt > _maxRows) break;
    				//ObjectNode dataEntryNode = jsonMapper.createObjectNode();
    				generator.writeStartObject();
    				for(int i = 1; i <= colCount; i++) {
    					generator.writeFieldName(nameDecode.get(i));
    					Object value = rset.getObject(i);
    					if(value instanceof Double) {
    						double d = ((Double)value).doubleValue();
    						if(Double.isInfinite(d)) value = "Infinity";
    						else if(Double.isNaN(d)) value = "NaN";
    					}    					
    					
    					if(value instanceof Number) {
    						generator.writeNumber(value.toString());
    					} else {
    						generator.writeString(value.toString());
    					}    			    					
    				}
    				generator.writeEndObject();    				
    			}
    			generator.writeEndArray(); 
    			generator.writeNumberField("rows", cnt);
    			if(startAt > 0) {
    				generator.writeNumberField("startat", startAt);
    			}
    			long elapsed = et.elapsedMs();
    			generator.writeNumberField("elapsedms", elapsed);
    			generator.writeEndObject();    	
    			generator.flush();
    			generator.close();
    			try {
    				out.flush();
    				out.close();
    			} catch (Exception ex) {}
    			log.info("SQL Result Written:{} bytes", out.writtenBytes());
    			return streamBuffer;
    		} else {
    			int rcode = ps.executeUpdate();
    			long elapsed = et.elapsedMs();
    			generator.writeStartObject();
    			generator.writeNumberField("resultcode", rcode);
    			generator.writeNumberField("elapsedms", elapsed);
    			generator.writeEndObject();
    			generator.flush();
    			generator.close();
    			try {
    				out.flush();
    				out.close();
    			} catch (Exception ex) {}
    			log.info("SQL Result Written:{} bytes", out.writtenBytes());
    			return streamBuffer;
    		}
    	} catch (Exception ex) {
    		log.error("Failed to process SQL statement [" + sqlText + "]", ex);
    		throw new RuntimeException("Failed to process SQL statement [" + sqlText + "]", ex);
    	} finally {
			if(rset!=null) try { rset.close(); } catch (Exception x) {/* No Op */}
			if(ps!=null) try { ps.close(); } catch (Exception x) {/* No Op */}
			if(conn!=null) try { conn.close(); } catch (Exception x) {/* No Op */}    		    		
    	}    	
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
	
	
	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.CatalogDBMXBean#getMetricCount()
	 */
	@Override
	public int getMetricCount() {
		return getCount("TSD_METRIC");
	}
	
	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.CatalogDBMXBean#getTagKCount()
	 */
	@Override
	public int getTagKCount() {
		return getCount("TSD_TAGK");
	}
	
	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.CatalogDBMXBean#getTagVCount()
	 */
	@Override
	public int getTagVCount() {
		return getCount("TSD_TAGV");
	}
	
	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.CatalogDBMXBean#getTSMetaCount()
	 */
	@Override
	public int getTSMetaCount() {
		return getCount("TSD_TSMETA");
	}
	
	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.CatalogDBMXBean#getAnnotationCount()
	 */
	@Override
	public int getAnnotationCount() {
		return getCount("TSD_ANNOTATION");
	}
	
	/**
	 * Returns the count from the passed table name
	 * @param tName The name of the table to get a count for
	 * @return the count
	 */
	protected int getCount(String tName) {
		return sqlWorker.sqlForInt("SELECT COUNT(*) FROM " + tName);
	}
	
	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.CatalogDBMXBean#synchronizeFromStore()
	 */
	@Override
	public long synchronizeFromStore() throws Exception {
		MetaSynchronizer ms = new MetaSynchronizer(tsdb);
		return ms.process();
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.CatalogDBMXBean#getPluginPath()
	 */
	@Override
	public String getPluginPath() {
		try {
			return getClass().getProtectionDomain().getCodeSource().getLocation().toString();
		} catch (Exception ex) {
			return "Not Available";
		}
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.handlers.logging.LoggerManager#getLoggerLevel()
	 */
	public String getLoggerLevel() {
		return loggerManager.getLoggerLevel();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.handlers.logging.LoggerManager#getLoggerEffectiveLevel()
	 */
	public String getLoggerEffectiveLevel() {
		return loggerManager.getLoggerEffectiveLevel();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.handlers.logging.LoggerManager#setLoggerLevel(java.lang.String)
	 */
	public void setLoggerLevel(String level) {
		loggerManager.setLoggerLevel(level);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.handlers.logging.LoggerManager#getLevelNames()
	 */
	public String[] getLevelNames() {
		return loggerManager.getLevelNames();
	}
	
}
