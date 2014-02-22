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
package net.opentsdb.catalog.syncqueue;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.sql.DataSource;

import net.opentsdb.catalog.CatalogDBInterface;
import net.opentsdb.catalog.SQLWorker;
import net.opentsdb.catalog.TSDBTable;
import net.opentsdb.catalog.TSDBTable.TableInfo;
import net.opentsdb.catalog.datasource.CatalogDataSource;
import net.opentsdb.core.TSDB;

import org.helios.tsdb.plugins.service.PluginContext;
import org.helios.tsdb.plugins.util.ConfigurationHelper;
import org.helios.tsdb.plugins.util.SystemClock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.AbstractService;
import com.stumbleupon.async.Callback;
import com.stumbleupon.async.Deferred;

/**
 * <p>Title: SyncQueueProcessor</p>
 * <p>Description: Service to poll the SyncQueue table in the catalog database
 * for new entries and synchronize the changes back to the OpenTSDB store.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>net.opentsdb.catalog.syncqueue.SyncQueueProcessor</code></p>
 */

public class SyncQueueProcessor extends AbstractService implements Runnable, ThreadFactory {
	/** Instance logger */
	protected final Logger log = LoggerFactory.getLogger(getClass());
	/** The plugin context to provide the processor's configuration */
	protected final PluginContext pluginContext;
	/** The catalog datasource */
	protected final DataSource dataSource;
	/** The DB interface */
	protected final CatalogDBInterface dbInterface;
	/** The tsdb instance to synchronize changes to */
	protected final TSDB tsdb;
	/** The TSDB Sync period in seconds */
	protected final AtomicLong syncPeriod = new AtomicLong(-1L);
	/** Indicates if TSDB Sync has been completely disabled */
	protected boolean syncDisabled = false;
	/** The scheduler for kicking off poll events */
	protected ScheduledExecutorService scheduler = null;
	/** The handle to the scheduled task */
	protected ScheduledFuture<?> taskHandle = null;
	/** The thread factory serial number factory */
	protected final AtomicInteger serial = new AtomicInteger(0);
	/** Flag indicating that a sync is in progress */
	protected final AtomicBoolean syncInProgress = new AtomicBoolean(false);
	/** The SQLWorker to manage JDBC Ops */
	protected SQLWorker sqlWorker = null;
	
	
	
	/** The SQL to retrieve a TSMeta instance from the DB */
	public static final String GET_TSMETA_SQL = "SELECT * FROM TSD_TSMETA WHERE FQNID = ?";
	/** The SQL to retrieve an Annotation instance from the DB */
	public static final String GET_ANNOTATION_SQL = "SELECT * FROM TSD_ANNOTATION WHERE ANNID = ?";
	/** The SQL template to retrieve a TSMeta instance from the DB */
	public static final String GET_UIDMETA_SQL = "SELECT * FROM %s WHERE XUID = ?";
	
	/** Custom tag to place in a meta custom map to make sure the fake meta is ignored */
	public static final String IGNORE_TAG_NAME = "syncqueue.processor.ignore";

	
	/** A set of the UIDMeta type tables */
	public static final Set<String> UIDMETA_TABLES = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
			"TSD_METRIC", "TSD_TAGK", "TSD_TAGV"
	)));
	

	
	/**
	 * Creates a new SyncQueueProcessor
	 * @param pluginContext The plugin context to provide the processor's configuration
	 */
	public SyncQueueProcessor(PluginContext pluginContext) {
		this.pluginContext = pluginContext;
		tsdb = pluginContext.getTsdb();
		dataSource = pluginContext.getResource(CatalogDataSource.class.getSimpleName(), DataSource.class);
		dbInterface = pluginContext.getResource(CatalogDBInterface.class.getSimpleName(), CatalogDBInterface.class);
		sqlWorker = SQLWorker.getInstance(dataSource);
		syncDisabled = ConfigurationHelper.getBooleanSystemThenEnvProperty(CatalogDBInterface.TSDB_DISABLE_SYNC, CatalogDBInterface.DEFAULT_TSDB_DISABLE_SYNC, pluginContext.getExtracted());
	}


	/**
	 * {@inheritDoc}
	 * @see com.google.common.util.concurrent.AbstractService#doStart()
	 */
	@Override
	protected void doStart() {
		log.info("\n\t=========================================\n\tStarting SyncQueueProcessor\n\t=========================================");
		if(!syncDisabled) {
			scheduler = Executors.newScheduledThreadPool(2, this);
			setTSDBSyncPeriod(ConfigurationHelper.getLongSystemThenEnvProperty(CatalogDBInterface.TSDB_SYNC_PERIOD, CatalogDBInterface.DEFAULT_TSDB_SYNC_PERIOD, pluginContext.getExtracted()));
			log.info("Sync Poller Scheduled for [{}] s. period", syncPeriod.get());
		} else {
			log.info("\n\t===========================\n\tTSDB Sync Operations Disabled\n\t===========================\n");
		}
		notifyStarted();
		log.info("\n\t=========================================\n\tSyncQueueProcessor Started\n\t=========================================");
	}


	/**
	 * {@inheritDoc}
	 * @see com.google.common.util.concurrent.AbstractService#doStop()
	 */
	@Override
	protected void doStop() {
		log.info("\n\t=========================================\n\tStopping SyncQueueProcessor\n\t=========================================");
		if(taskHandle!=null) {
			taskHandle.cancel(false);
			taskHandle = null;
		}
		scheduler.shutdownNow();
		notifyStopped();
		log.info("\n\t=========================================\n\tSyncQueueProcessor Stopped\n\t=========================================");
	}

	/**
	 * Returns the TSDB Sync period in seconds
	 * @return the TSDB Sync period in seconds
	 */
	public long getTSDBSyncPeriod() {
		return syncPeriod.get();
	}
	
	/**
	 * Indicates if all TSDB Synchronization operations have been disabled.
	 * @return true if all TSDB Synchronization operations have been disabled, false otherwise
	 */
	public boolean isTSDBSyncDisabled() {
		return syncDisabled;
	}
	
	/**
	 * Sets the TSDB Sync period in seconds. 
	 * If this op modifies the existing value, a schedule change will be triggered.
	 * This may stop a started schedule, or start a stopped schedule. 
	 * @param newPeriod the TSDB Sync period in seconds.
	 */
	public void setTSDBSyncPeriod(final long newPeriod) {
		long oldPeriod = syncPeriod.getAndSet(newPeriod);
		if(oldPeriod!=newPeriod) {
			fireSyncScheduleUpdate(oldPeriod, newPeriod);
		}
	}
	
	/**
	 * Modifies the synch schedule
	 * @param priorPeriod The prior period
	 * @param newPeriod  The new period
	 */
	protected void fireSyncScheduleUpdate(long priorPeriod, long newPeriod) {		
		if(taskHandle!=null) {
			taskHandle.cancel(false);
			taskHandle = null;
		}		
		if(newPeriod>1) {
			taskHandle = scheduler.scheduleWithFixedDelay(this, 5, getTSDBSyncPeriod(), TimeUnit.SECONDS);
			log.info("Started TSDB Sync Scheduler with a period of {} seconds", getTSDBSyncPeriod());
		}
	}


	/**
	 * {@inheritDoc}
	 * @see java.util.concurrent.ThreadFactory#newThread(java.lang.Runnable)
	 */
	@Override
	public Thread newThread(Runnable r) {
		Thread t = new Thread(r, getClass().getSimpleName() + "Thread#" + serial.incrementAndGet());
		t.setDaemon(true);
		return t;
	}
	
	/**
	 * <p>The implementation of the scheduled polling event</p>.
	 * {@inheritDoc}
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		if(!syncInProgress.compareAndSet(false, true)) {
			log.debug("Sync already in progress. Ejecting....");
		}
		// First retry the failures
		Deferred<ArrayList<Void>> priorFailsDeferred = retryPriorFails();
		// Now look for new syncs
		Deferred<ArrayList<Void>> processNewDeferred = processNewSyncs();
		
		Deferred.group(priorFailsDeferred, processNewDeferred).addBoth(new Callback<Void, ArrayList<ArrayList<Void>>>() {
			@Override
			public Void call(ArrayList<ArrayList<Void>> arg) throws Exception {
				syncInProgress.set(false);
				return null;
			}
		});
		
	}
	
	/**
	 * Finds and processes new sync objects
	 * FIXME:  Fail or no, the LAST_UPDATE should be updated.
	 * FIXME: Need to handle non-callback exceptions in SYNC
	 * FIXME: Need to specify order of tables processed
	 * @return A deferred indicating the completion of all table syncs
	 */
	protected Deferred<ArrayList<Void>> processNewSyncs() {
		Connection conn = null;
		ResultSet rset = null;
		final List<Deferred<Void>> allDeferreds = new ArrayList<Deferred<Void>>();
		try {
			conn = dataSource.getConnection();
			ResultSet dis = sqlWorker.executeQuery(conn, "SELECT TABLE_NAME, LAST_SYNC FROM TSD_LASTSYNC ORDER BY ORDERING", false);
			
			while(dis.next()) {
				final TSDBTable table = TSDBTable.valueOf(dis.getString(1));
				final List<Deferred<Void>> tableDeferreds = new ArrayList<Deferred<Void>>();
				log.info("Looking for Syncs in [{}]", table.name());
				rset = sqlWorker.executeQuery("SELECT * FROM " + table.name() + " WHERE LAST_UPDATE > ?", false, dis.getTimestamp(2));
				int cnt = 0;
				for(Object dbObject: table.ti.getObjects(rset, dbInterface)) {
					tableDeferreds.add(table.ti.sync(dbObject, tsdb).addBoth(syncCompletionHandler(dbObject, table.ti.getPk(conn, dbObject), 0)));
					cnt++;
				}
				log.info("Processed [{}] Syncs from [{}]", cnt, table.name());
				rset.close();
				rset = null;
				allDeferreds.add(Deferred.group(tableDeferreds).addCallback(new Callback<Void, ArrayList<Void>>() {
					public Void call(ArrayList<Void> arg) throws Exception {
						long currentTime = SystemClock.time();
						Timestamp ts = new Timestamp(currentTime);
						sqlWorker.execute("UPDATE TSD_LASTSYNC SET LAST_SYNC = ? WHERE TABLE_NAME = ?", ts, table.name());
						log.info("Set Highwater on [{}] to [{}]", table.name(), new Date(currentTime));
						return null;
					}
				}));		
			}			
			dis.close();
			conn.commit();
		} catch (Exception ex) {
			log.error("Unexpected SynQueueProcessor Error", ex);
		} finally {
			if(rset!=null) try { rset.close(); } catch (Exception ex) {/* No Op */}
			if(conn!=null) try { conn.close(); } catch (Exception ex) {/* No Op */}
		}
		return Deferred.group(allDeferreds);
	}
	
	
	/**
	 * Retries all the prior synch failures 
	 * @return A deferred indicating the completion of all table syncs
	 */
	protected Deferred<ArrayList<Void>> retryPriorFails() {
		Connection conn = null;
		ResultSet rset = null;
		final List<Deferred<Void>> allDeferreds = new ArrayList<Deferred<Void>>();
		try {
			conn = dataSource.getConnection();
			for(final TSDBTable table: TSDBTable.values()) {
				final List<Deferred<Void>> tableDeferreds = new ArrayList<Deferred<Void>>();
				rset = sqlWorker.executeQuery(conn, "SELECT * FROM TSD_LASTSYNC_FAILS WHERE TABLE_NAME = ? ORDER BY LAST_ATTEMPT DESC", false, table.name());
				log.info("Retrying failed syncs for {}", table.name());
				int cnt = 0;
				while(rset.next()) {
					String tableName = rset.getString(1);
					String objectId = rset.getString(2);
					int attempts = rset.getInt(3);
					Object failedObject = getDBObject(conn, tableName, objectId);
					tableDeferreds.add(table.ti.sync(failedObject, tsdb).addBoth(syncCompletionHandler(failedObject, objectId, attempts)));
					cnt++;
				}
				log.info("Retried [{}] failed syncs", cnt);
				allDeferreds.add(Deferred.group(tableDeferreds).addBoth(new Callback<Void, ArrayList<Void>>() {
					public Void call(ArrayList<Void> arg) throws Exception {
						return null;
					}
				}));
				rset.close();
				rset = null;				
			}
			conn.commit();
		} catch (Exception ex) {
			log.error("Unexpected SynQueueProcessor Error", ex);
		} finally {
			if(rset!=null) try { rset.close(); } catch (Exception ex) {/* No Op */}
			if(conn!=null) try { conn.close(); } catch (Exception ex) {/* No Op */}
		}
		return Deferred.group(allDeferreds);
	}
	
	
	/**
	 * Returns the re-constituted sync-failed object
	 * @param conn The connection to fetch on
	 * @param tableName The table name the failure was recorded for
	 * @param objectId The sync-fail recorded object id
	 * @return the re-constituted sync-failed object
	 */
	protected Object getDBObject(Connection conn, String tableName, String objectId) {
		TableInfo ti = TSDBTable.valueOf(tableName).ti;
		ResultSet r = sqlWorker.executeQuery(conn, ti.getByPKSql(), true, ti.getBindablePK(objectId));
		return ti.getObjects(r, dbInterface).iterator().next();
	}
	
	/**
	 * Creates a sync complete handler for a synchronize-to-tsdb request
	 * @param syncedObject The object that was synched
	 * @param pk The pk of the synched object
	 * @param attempts The number of attempts tried so far
	 * @return the completion callback
	 */
	protected Callback<Void, Boolean> syncCompletionHandler(final Object syncedObject, final Object pk, final int attempts) {
		return new Callback<Void, Boolean>() {
			@Override
			public Void call(Boolean success) throws Exception {
				TSDBTable tab = TSDBTable.getTableFor(syncedObject);
				
				if(success) {					
					sqlWorker.execute("DELETE FROM TSD_LASTSYNC_FAILS WHERE TABLE_NAME = ? AND OBJECT_ID = ?", tab.name(), tab.ti.getBindablePK(pk));
					log.info("Sync Complete for {}.{}", tab.name(), pk);
				} else {
					if(attempts<1) {
						sqlWorker.execute("INSERT INTO TSD_LASTSYNC_FAILS (TABLE_NAME, OBJECT_ID, ATTEMPTS, LAST_ATTEMPT) VALUES (?,?,?,?)", tab.name(), pk.toString(), 1, SystemClock.getTimestamp());
						log.info("First Sync Fail for {}.{}", tab.name(), pk);
					} else {
						sqlWorker.executeUpdate("UPDATE TSD_LASTSYNC_FAILS SET ATTEMPTS = ?, LAST_ATTEMPT  = ? WHERE TABLE_NAME = ? AND OBJECT_ID = ?", attempts+1, SystemClock.getTimestamp(), tab.name(), pk.toString());
						log.info("Sync Fail #{} for {}.{}", attempts+1, tab.name(), pk);
					}
				}
				return null;
			}
		};
	}
	
	
}
