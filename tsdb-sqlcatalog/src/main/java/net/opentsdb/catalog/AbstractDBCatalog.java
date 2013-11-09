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
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import net.opentsdb.catalog.datasource.CatalogDataSource;
import net.opentsdb.catalog.h2.H2Support;
import net.opentsdb.catalog.h2.json.JSONMapSupport;
import net.opentsdb.core.TSDB;
import net.opentsdb.meta.Annotation;
import net.opentsdb.meta.TSMeta;
import net.opentsdb.meta.UIDMeta;
import net.opentsdb.search.SearchQuery;
import net.opentsdb.uid.UniqueId;
import net.opentsdb.uid.UniqueId.UniqueIdType;

import org.helios.tsdb.plugins.event.TSDBSearchEvent;
import org.helios.tsdb.plugins.util.ConfigurationHelper;
import org.helios.tsdb.plugins.util.SystemClock;
import org.helios.tsdb.plugins.util.SystemClock.ElapsedTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stumbleupon.async.Deferred;

/**
 * <p>Title: AbstractDBCatalog</p>
 * <p>Description: Base abstract class for implementing concrete DB catalogs for different JDBC supporting databases.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>net.opentsdb.catalog.AbstractDBCatalog</code></p>
 */

public abstract class AbstractDBCatalog implements CatalogDBInterface {
	/** Instance logger */
	protected Logger log = LoggerFactory.getLogger(getClass());
	/** The constructed datasource */
	protected DataSource dataSource = null;
	/** The data source initializer */
	protected CatalogDataSource cds = null;
	/** The configured increment on the FQN ID sequence */
	protected int fqnSeqIncrement = 100;
	/** The current allocated FQNID sequence */
	protected long fqnAllocatedSequence = 0;
	/** The last allocated FQN sequence to be doled out before a refresh */
	protected long fqnMaxSequence = 0;
	/** The parent TSDB */
	protected TSDB tsdb = null;
	
	
	/** batched ps for tag key inserts */
	protected PreparedStatement uidMetaTagKIndexPs = null;
	/** batched ps for tag value inserts */
	protected PreparedStatement uidMetaTagVIndexPs = null;
	/** batched ps for metric name inserts */
	protected PreparedStatement uidMetaMetricIndexPs = null;
	/** batched ps for tag pair inserts */
	protected PreparedStatement uidMetaTagPairPs = null;
	/** batched ps for fqn inserts */
	protected PreparedStatement tsMetaFqnPs = null;
	/** batched ps for FQN tag pair inserts */
	protected PreparedStatement uidMetaTagPairFQNPs = null;
	/** batched ps for annotation inserts */
	protected PreparedStatement annotationsPs = null;
	
	// ========================================================================================
	//	FQN Sequence Related Constants
	// ========================================================================================
	
	/** The config property name for the increment on the FQN ID Sequence */
	public static final String DB_FQN_SEQ_INCR = "helios.search.catalog.fqn.incr";
	/** The default increment on the FQN ID Sequence */
	public static final int DEFAULT_DB_FQN_SEQ_INCR = 100;
	/** The SQL to execute to get the next batch of FQNID sequence numbers */
	public static final String NEXT_FQNID_SEQ_SQL = "SELECT FQN_SEQ.NEXTVAL";
	

	// ========================================================================================
	//	Object COUNT and EXISTS SQL
	// ========================================================================================
	/** The SQL template for verification of whether a UIDMeta has been saved or not */
	public static final String UID_EXISTS_SQL = "SELECT COUNT(*) FROM %s WHERE UID = ?";
	/** The SQL for verification of whether a UIDMeta pair has been saved or not */
	public static final String UID_PAIR_EXISTS_SQL = "SELECT COUNT(*) FROM  TSD_TAGPAIR WHERE UID = ?";
	/** The SQL for verification of whether a TSMeta has been saved or not */
	public static final String TSUID_EXISTS_SQL = "SELECT COUNT(*) FROM TSD_FQN WHERE TSUID = ?";
	
	
	
	// ========================================================================================
	//	Object INSERT SQL
	// ========================================================================================
	/** The UIDMeta indexing SQL template */	
	public static final String UID_INDEX_SQL_TEMPLATE = "INSERT INTO %s (UID,NAME,CREATED,DESCRIPTION,DISPLAY_NAME,NOTES,CUSTOM) VALUES(?,?,?,?,?,?,?)";
	/** The SQL to insert a TSMeta TSD_FQN */
	public static final String TSUID_INSERT_SQL = "INSERT INTO TSD_FQN " + 
			"(FQNID, METRIC_UID, FQN, TSUID, CREATED, MAX_VALUE, MIN_VALUE, " + 
			"DATA_TYPE, DESCRIPTION, DISPLAY_NAME, NOTES, UNITS, RETENTION) " + 
			"VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";
	/** The SQL to insert the TSMeta UID pairs */
	public static final  String TSD_FQN_TAGPAIR_SQL = "INSERT INTO TSD_FQN_TAGPAIR (FQNID, UID, PORDER, NODE) VALUES (?,?,?,?)";
	/** The SQL to insert an Annotation */
	public static final String TSD_INSERT_ANNOTATION = "INSERT INTO TSD_ANNOTATION (START_TIME,DESCRIPTION,NOTES,FQNID,END_TIME,CUSTOM) VALUES (?, ?, ?, ?, ?, ?)";
	/** The SQL to insert a Tag Pair */
	public static final String INSERT_TAGPAIR_SQL = "INSERT INTO TSD_TAGPAIR (UID, TAGK, TAGV, NAME) VALUES (?,?,?,?)";

	// ========================================================================================
	//	Object DELETE SQL
	// ========================================================================================
	/** The SQL to delete an Annotation */
	public static final String TSD_DELETE_ANNOTATION = "DELETE FROM TSD_ANNOTATION WHERE START_TIME = ? AND (TSUID = ? OR TSUID IS NULL)";
	/** The SQL template to delete a UIDMeta */
	public static final String TSD_DELETE_UID = "DELETE FROM TSD_%s WHERE UID = ?";
	/** The SQL template to delete a TSMeta */
	public static final String TSD_DELETE_TS = "DELETE FROM TSD_FQN WHERE TSUID = ?";

	
	
	//=======================================================================================
	
			
	/** The keys to insert into a new custom map for UISMetas and Annotations */
	public static final Map<String, String> INIT_CUSTOM;
	
	/** The custom map key to identify how the object was saved */
	public static final String SAVED_BY_KEY = "tsd.sql.savedby";
	/** The custom map key to specify the version of the object */
	public static final String VERSION_KEY = "tsd.sql.version";
	
	static {
		Map<String, String> tmp = new TreeMap<String, String>();
		tmp.put(SAVED_BY_KEY, H2DBCatalog.class.getSimpleName());
		tmp.put(VERSION_KEY, "1");
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
	 * @see net.opentsdb.catalog.CatalogDBInterface#getDataSource()
	 */
	@Override
	public DataSource getDataSource() {
		return dataSource;
	}

	// ========================================================================================
	//	Catalog Service Initialization
	// ========================================================================================
	
	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.CatalogDBInterface#initialize(net.opentsdb.core.TSDB, java.util.Properties)
	 */
	@Override
	public void initialize(TSDB tsdb, Properties extracted) {
		log.info("\n\t================================================\n\tStarting DB Initializer\n\tName:{}\n\t================================================", getClass().getSimpleName());
		this.tsdb = tsdb;
		fqnSeqIncrement = ConfigurationHelper.getIntSystemThenEnvProperty(DB_FQN_SEQ_INCR, DEFAULT_DB_FQN_SEQ_INCR, extracted);
		cds = CatalogDataSource.getInstance();
		cds.initialize(tsdb, extracted);
		dataSource = cds.getDataSource(); 
		doInitialize(tsdb, extracted);
		log.info("\n\t================================================\n\tDB Initializer Started\n\tJDBC URL:{}\n\t================================================", cds.getConfig().getJdbcUrl());
	}
	
	/**
	 * Perform the concrete catalog service initialzation
	 * @param tsdb The parent TSDB
	 * @param extracted The extracted configuration
	 */
	protected abstract void doInitialize(TSDB tsdb, Properties extracted);
	
	// ========================================================================================
	//	Catalog Service Shutdown
	// ========================================================================================
	
	/**
	 * Terminates the database resources
	 */
	@Override
	public void shutdown() {
		log.info("\n\t================================================\n\tStopping TSDB Catalog DB\n\tName:{}\n\t================================================", cds.getConfig().getJdbcUrl());
		if(cds!=null) {
			cds.shutdown();
			cds = null;
			dataSource = null;
		}
		doShutdown();
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
		try {			
			for(TSDBSearchEvent event: events) {
				if(BatchMileStone.class.isInstance(event)) {
					((BatchMileStone)event).countDown();
					continue;
				}
				switch(event.eventType) {
				case ANNOTATION_DELETE:
					deleteAnnotation(conn, event.annotation);
					ops++;
					break;
				case ANNOTATION_INDEX:
					annotations.add(event.annotation);
					ops++;
					break;
				case TSMETA_DELETE:
					deleteTSMeta(conn, event.tsuid);
					ops++;
					break;
				case TSMETA_INDEX:
					TSMeta tsMeta = event.tsMeta;
					if(stored(conn, tsMeta)) continue;
					processTSMeta(batchedUidPairs, conn, tsMeta);
					ops++;
					break;
				case UIDMETA_DELETE:
					deleteUIDMeta(conn, event.uidMeta);
					ops++;
					break;
				case UIDMETA_INDEX:					
					UIDMeta uidMeta = event.uidMeta;
					if(batchedUids.contains(uidMeta.toString()) || stored(conn, uidMeta)) continue;
					switch(uidMeta.getType()) {
						case METRIC:							
							if(uidMetaMetricIndexPs==null) uidMetaMetricIndexPs = conn.prepareStatement(getUIDMetaMetricIndexSQL());							
							bindUIDMeta(uidMeta, uidMetaMetricIndexPs);														
							break;
						case TAGK:
							if(uidMetaTagKIndexPs==null) uidMetaTagKIndexPs = conn.prepareStatement(getUIDMetaTagKIndexSQL());
							bindUIDMeta(uidMeta, uidMetaTagKIndexPs);							
							break;
						case TAGV:
							if(uidMetaTagVIndexPs==null) uidMetaTagVIndexPs = conn.prepareStatement(getUIDMetaTagVIndexSQL());
							bindUIDMeta(uidMeta, uidMetaTagVIndexPs);														
							break;
						default:
							log.warn("yeow. Unexpected UIDMeta type:{}", uidMeta.getType().name());
							break;
					}
					batchedUids.add(uidMeta.toString());
					ops++;
//					log.info("Bound {} Index [{}]-[{}]", uidMeta.getType().name(), uidMeta.getName(), uidMeta.getUID());
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
			// Execute batch inserts for Annotations
			if(annotationsPs!=null) {
				executeBatch(annotationsPs);
				annotationsPs.clearBatch();								
			}
			
			conn.commit();
			log.info(et.printAvg("Indexes", ops));
		} catch (Exception ex) {
			log.error("batch operations failed", ex);
			try { conn.rollback(); } catch (Exception nex) {
				log.error("Batch update failed and connection failed to rollback !!!", nex);
			}
			throw new RuntimeException("Batch update failed", ex);
			// TODO: Custom exception that indicates if rollback succeeded
		} finally {
			if(uidMetaTagKIndexPs!=null) try { uidMetaTagKIndexPs.close(); uidMetaTagKIndexPs = null;} catch (Exception x) {/* No Op */}
			if(uidMetaTagVIndexPs!=null) try { uidMetaTagVIndexPs.close(); uidMetaTagVIndexPs = null; } catch (Exception x) {/* No Op */}
			if(uidMetaMetricIndexPs!=null) try { uidMetaMetricIndexPs.close(); uidMetaMetricIndexPs = null;} catch (Exception x) {/* No Op */}
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
		ps.setString(2, uidMeta.getName());
		long created = uidMeta.getCreated();
		if(created==0) {
			created = SystemClock.time();
		}
		ps.setTimestamp(3, new Timestamp(created));
		ps.setString(4, uidMeta.getDescription());
		ps.setString(5, uidMeta.getDisplayName());
		ps.setString(6, uidMeta.getNotes());
		Map<String, String> custom = fillInCustom(uidMeta.getCustom());
		ps.setString(7, JSONMapSupport.nokToString(custom));
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
			if(uidMetaTagVIndexPs!=null) {
				executeBatch(uidMetaTagVIndexPs);
				uidMetaTagVIndexPs.clearBatch();
			}
			if(uidMetaTagKIndexPs!=null) {
				executeBatch(uidMetaTagKIndexPs);
				uidMetaTagKIndexPs.clearBatch();
			}			
		} catch (Exception ex) {
			throw new RuntimeException("Failed to execute UID Batches", ex);
		}		
	}
	
	
	
	// ==================================================================================================
	//  Object Indexing & Processing 
	// ==================================================================================================
	
	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.CatalogDBInterface#processAnnotation(java.sql.Connection, net.opentsdb.meta.Annotation)
	 */
	@Override
	public void processAnnotation(Connection conn, Annotation annotation) {
		try {
			if(annotationsPs==null) annotationsPs = conn.prepareStatement(TSD_INSERT_ANNOTATION);
			long startTime = annotation.getStartTime(); 
			if(startTime==0) {
				startTime = SystemClock.time();
			} else {
				startTime = TimeUnit.MILLISECONDS.convert(startTime, TimeUnit.SECONDS);
			}
			annotationsPs.setTimestamp(1, new Timestamp(startTime));
			annotationsPs.setString(2, annotation.getDescription());
			annotationsPs.setString(3, annotation.getNotes());
			if(annotation.getTSUID()==null) {
				annotationsPs.setNull(4, Types.BIGINT);
			} else {
				annotationsPs.setLong(4, H2Support.fqnId(conn, annotation.getTSUID()));
			}
			
			
			long endTime = annotation.getEndTime();
			if(endTime==0) {
				annotationsPs.setNull(5, Types.TIMESTAMP);
			} else {
				annotationsPs.setTimestamp(5, new Timestamp(endTime));
			}
			Map<String, String> custom = fillInCustom(annotation.getCustom());
			annotationsPs.setString(6, JSONMapSupport.nokToString(custom));
			annotationsPs.addBatch();
		} catch (SQLException sex) {
			throw new RuntimeException("Failed to store annotation [" + annotation.getDescription() + "]", sex);
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.CatalogDBInterface#processTSMeta(java.util.Set, java.sql.Connection, net.opentsdb.meta.TSMeta)
	 */
	@Override
	public void processTSMeta(final Set<String> batchUidPairs, Connection conn, TSMeta tsMeta) {
		StringBuilder fqn = new StringBuilder(tsMeta.getMetric().getName()).append(":");
		UIDMeta[] tagPair = new UIDMeta[2];
		for(UIDMeta meta: tsMeta.getTags()) {
			if(tagPair[0]==null) {
				tagPair[0] = meta;
				fqn.append(meta.getName()).append("=");
				continue;
			} else if(tagPair[1]==null) {
				tagPair[1] = meta;
				fqn.append(meta.getName()).append(",");
				processUIDMetaPair(batchUidPairs, conn, tagPair);
				tagPair[0] = null; tagPair[1] = null; 
			}
		}
		fqn.deleteCharAt(fqn.length()-1);
		try {
			if(tsMetaFqnPs==null) tsMetaFqnPs = conn.prepareStatement(TSUID_INSERT_SQL);
			long fqnSeq = getNextFQNSequence();
			int bId = 0;
			tsMetaFqnPs.setLong(++bId, fqnSeq);
			tsMetaFqnPs.setString(++bId, tsMeta.getMetric().getUID());
			tsMetaFqnPs.setString(++bId, fqn.toString());
			tsMetaFqnPs.setString(++bId, tsMeta.getTSUID());
			tsMetaFqnPs.setTimestamp(++bId, new Timestamp(utoms(tsMeta.getCreated())));
			tsMetaFqnPs.setDouble(++bId, tsMeta.getMax());
			tsMetaFqnPs.setDouble(++bId, tsMeta.getMin());
			tsMetaFqnPs.setString(++bId, tsMeta.getDataType());
			tsMetaFqnPs.setString(++bId, tsMeta.getDescription());
			tsMetaFqnPs.setString(++bId, tsMeta.getDisplayName());
			tsMetaFqnPs.setString(++bId, tsMeta.getNotes());
			tsMetaFqnPs.setString(++bId, tsMeta.getUnits());
			tsMetaFqnPs.setInt(++bId, tsMeta.getRetention());
			tsMetaFqnPs.addBatch();
			if(uidMetaTagPairFQNPs==null) uidMetaTagPairFQNPs = conn.prepareStatement(TSD_FQN_TAGPAIR_SQL);
			LinkedList<UIDMeta> pairs = new LinkedList<UIDMeta>(tsMeta.getTags());
			int pairCount = tsMeta.getTags().size()/2;
			int leaf = pairCount-1;
			for(short i = 0; i < pairCount; i++) {
				String pairUID = pairs.removeFirst().getUID() + pairs.removeFirst().getUID();
				uidMetaTagPairFQNPs.setLong(1, fqnSeq);
				uidMetaTagPairFQNPs.setString(2, pairUID);
				uidMetaTagPairFQNPs.setShort(3, i);
				if(i==leaf) {
					uidMetaTagPairFQNPs.setString(4, "L");
				} else {
					uidMetaTagPairFQNPs.setString(4, "B");
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
				UIDMeta meta = new UIDMeta(utype, UniqueId.stringToUid(rset.getString("UID")), rset.getString("NAME"));
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
	 * Determines if the passed UIDMeta has been stored
	 * @param conn A supplied connection
	 * @param uidMeta the UIDMeta to test
	 * @return true if stored, false otherwise
	 */
	public boolean stored(Connection conn, UIDMeta uidMeta) {
		PreparedStatement ps = null;
		ResultSet rset = null;		
		try {
			switch(uidMeta.getType()) {
				case METRIC:
					ps = conn.prepareStatement(String.format(UID_EXISTS_SQL, "TSD_METRIC"));
					break;
				case TAGK:
					ps = conn.prepareStatement(String.format(UID_EXISTS_SQL, "TSD_TAGK"));
					break;
				case TAGV:
					ps = conn.prepareStatement(String.format(UID_EXISTS_SQL, "TSD_TAGV"));
					break;
				default:
					throw new RuntimeException("yeow. Unrecognized UIDMeta type [" + uidMeta.getType().name() + "]");			
			}
			ps.setString(1, uidMeta.getUID());
			rset = ps.executeQuery();
			rset.next();
			return rset.getInt(1)>0;
		} catch (SQLException sex) {
			throw new RuntimeException("Failed to lookup UIDMeta [" + uidMeta + uidMeta.getName() + "]", sex);
		} finally {
			if(rset!=null) try { rset.close(); } catch (Exception x) {/* No Op */}
			if(ps!=null) try { ps.close(); } catch (Exception x) {/* No Op */}
		}
	}
	
	/**
	 * Determines if the passed TSMeta has been stored
	 * @param conn A supplied connection
	 * @param tsMeta the TSMeta to test
	 * @return true if stored, false otherwise
	 */
	public boolean stored(Connection conn, TSMeta tsMeta) {
		PreparedStatement ps = null;
		ResultSet rset = null;		
		try {
			ps = conn.prepareStatement(TSUID_EXISTS_SQL);
			ps.setString(1, tsMeta.getTSUID());
			rset = ps.executeQuery();
			rset.next();
			return rset.getInt(1)>0;
		} catch (SQLException sex) {
			throw new RuntimeException("Failed to lookup TSMeta [" + tsMeta  + "]", sex);
		} finally {
			if(rset!=null) try { rset.close(); } catch (Exception x) {/* No Op */}
			if(ps!=null) try { ps.close(); } catch (Exception x) {/* No Op */}
		}
	}
	
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
	//  FQNID and Sequence Management
	//================================================================================================
		
	/**
	 * Returns the next cached sequence.
	 * Only one thread runs in here, so no need to get excited.... yet.
	 * @return the next cached sequence
	 */
	private long getNextFQNSequence() {
		if(fqnAllocatedSequence == fqnMaxSequence) {
			fqnAllocatedSequence = refreshFQNSequence();
			fqnMaxSequence = fqnAllocatedSequence + fqnSeqIncrement;
			return fqnAllocatedSequence;
		}
		fqnAllocatedSequence++;
		return fqnAllocatedSequence;
	}
	
	
	/**
	 * Gets the next DB FQNID sequence 
	 * @return the next DB FQNID sequence 
	 */
	private long refreshFQNSequence() {
		Connection conn = null;
		Statement st = null;
		ResultSet rset = null;
		try {
			conn = dataSource.getConnection();
			st = conn.createStatement();
			rset = st.executeQuery(NEXT_FQNID_SEQ_SQL);
			rset.next();
			return rset.getLong(1);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to refresh FQNID sequence", ex);
		} finally {
			if(rset!=null) try { rset.close(); } catch (Exception ex) {/* No Op */}
			if(st!=null) try { st.close(); } catch (Exception ex) {/* No Op */}
			if(conn!=null) try { conn.close(); } catch (Exception ex) {/* No Op */}
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
	public static Map<String, String> fillInCustom(Map<String, String> customMap) {
		Map<String, String> _customMap = customMap;
		if(_customMap==null) {
			_customMap = new TreeMap<String, String>(INIT_CUSTOM);
		} else {
			_customMap.putAll(INIT_CUSTOM);
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
	

}
