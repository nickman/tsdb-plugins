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
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
import net.opentsdb.core.UniqueIdRegistry;
import net.opentsdb.meta.Annotation;
import net.opentsdb.meta.TSMeta;
import net.opentsdb.meta.UIDMeta;
import net.opentsdb.uid.UniqueId;

import org.helios.tsdb.plugins.service.PluginContext;
import org.helios.tsdb.plugins.util.ConfigurationHelper;
import org.helios.tsdb.plugins.util.JMXHelper;
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
 * TODO:
 * Limit number of syncs in one batch
 * Total Elapsed time histogram
 * Counts by type synced
 * Elapsed time by type histogram
 * CAS Fails by type count
 * Exceptions by type count
 * JMX Management Interface
 */

public class SyncQueueProcessor extends AbstractService implements Runnable, ThreadFactory, SyncQueueProcessorMXBean {
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
	/** The uniqueid for tag keys */
	protected final UniqueId tagKunik;
	/** The uniqueid for tag values */
	protected final UniqueId tagVunik;
	/** The uniqueid for metric names */
	protected final UniqueId tagMunik;	
	/** The configured maximum number of sync ops to execute per batch */
	protected int maxSyncOps = DEFAULT_CONFIG_MAX_SYNC_OPS;
	/** Flag indicating if there are remaining pending sync ops after a call to processNew or retryPriorFails by incrementing when a batch loop starts and decrementing when it finishes */
	protected final AtomicInteger hasRemainingOps = new AtomicInteger(0);
	/** Tracks the number of pending ops */
	protected final AtomicLong pendingOps = new AtomicLong(0);
	
	/** A map of UniqueIds keyed by the UniqueId.UniqueIdType */
	private final Map<UniqueId.UniqueIdType, UniqueId> uniques = new EnumMap<UniqueId.UniqueIdType, UniqueId>(UniqueId.UniqueIdType.class);


	
	
	
	/** The SQL to retrieve a TSMeta instance from the DB */
	public static final String GET_TSMETA_SQL = "SELECT * FROM TSD_TSMETA WHERE FQNID = ?";
	/** The SQL to retrieve an Annotation instance from the DB */
	public static final String GET_ANNOTATION_SQL = "SELECT * FROM TSD_ANNOTATION WHERE ANNID = ?";
	/** The SQL template to retrieve a TSMeta instance from the DB */
	public static final String GET_UIDMETA_SQL = "SELECT * FROM %s WHERE XUID = ?";
	
	/** Custom tag to place in a meta custom map to make sure the fake meta is ignored */
	public static final String IGNORE_TAG_NAME = "syncqueue.processor.ignore";

	/** The config property name for the maximum number of sync operations to call in one batch */
	public static final String CONFIG_MAX_SYNC_OPS = "helios.search.catalog.seq.fqn.incr";
	/** The default maximum number of sync operations to call in one batch */
	public static final int DEFAULT_CONFIG_MAX_SYNC_OPS = 1024;
	
	
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
		tagKunik = UniqueIdRegistry.getInstance().getTagKUniqueId();
		tagVunik = UniqueIdRegistry.getInstance().getTagVUniqueId();
		tagMunik = UniqueIdRegistry.getInstance().getMetricsUniqueId();
		uniques.put(UniqueId.UniqueIdType.TAGK, tagKunik);
		uniques.put(UniqueId.UniqueIdType.TAGV, tagVunik);
		uniques.put(UniqueId.UniqueIdType.METRIC, tagMunik);
		dataSource = pluginContext.getResource(CatalogDataSource.class.getSimpleName(), DataSource.class);
		dbInterface = pluginContext.getResource(CatalogDBInterface.class.getSimpleName(), CatalogDBInterface.class);
		sqlWorker = SQLWorker.getInstance(dataSource);
		syncDisabled = ConfigurationHelper.getBooleanSystemThenEnvProperty(CatalogDBInterface.TSDB_DISABLE_SYNC, CatalogDBInterface.DEFAULT_TSDB_DISABLE_SYNC, pluginContext.getExtracted());
		maxSyncOps = ConfigurationHelper.getIntSystemThenEnvProperty(CONFIG_MAX_SYNC_OPS, DEFAULT_CONFIG_MAX_SYNC_OPS, pluginContext.getExtracted());
		JMXHelper.registerMBean(this, OBJECT_NAME);
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
			//taskHandle = scheduler.scheduleWithFixedDelay(this, 5, getTSDBSyncPeriod(), TimeUnit.SECONDS);
			taskHandle = scheduler.schedule(this, 5, TimeUnit.SECONDS);
			log.info("Started TSDB Sync Scheduler with a period of {} seconds", getTSDBSyncPeriod());
		}
	}


	/**
	 * {@inheritDoc}
	 * @see java.util.concurrent.ThreadFactory#newThread(java.lang.Runnable)
	 */
	@Override
	public Thread newThread(Runnable r) {
		Thread t = new Thread(r, getClass().getSimpleName() + "SynchThread#" + serial.incrementAndGet());
		t.setDaemon(true);
		return t;
	}
	
	/**notification
	 * <p>The implementation of the scheduled polling event</p>.
	 * {@inheritDoc}
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		if(!syncInProgress.compareAndSet(false, true)) {
			log.debug("Sync already in progress. Ejecting....");
		}
		log.debug("Starting TSDB Sync Run....");
		final long startTime = System.currentTimeMillis();
		pluginContext.publishNotification(JMX_NOTIF_SYNC_STARTED, "SyncLoop started", null, OBJECT_NAME);
		hasRemainingOps.set(0);
		pendingOps.set(0);		
		final AtomicBoolean keepRunning = new AtomicBoolean(false);
		do {
			keepRunning.set(false);
			final int currentBatchSeq = hasRemainingOps.incrementAndGet();
			final AtomicBoolean syncBatchHitMax = new AtomicBoolean(false);
			log.debug("Running SyncOp Loop #{}", currentBatchSeq);
			Deferred<ArrayList<ArrayList<Object>>> processNewDeferred = processNewSyncs(syncBatchHitMax);
			if(syncBatchHitMax.get()) {
				keepRunning.set(true);
			}
			final Runnable scheduledTask = this;
			processNewDeferred.addCallback(new Callback<Void, ArrayList<ArrayList<Object>>>() {
				public Void call(ArrayList<ArrayList<Object>> arg) throws Exception {
					long elapsed = System.currentTimeMillis() - startTime;
					int remainingOps = hasRemainingOps.decrementAndGet();
					log.debug("\n\t-----------------elapsed:" + elapsed + " ms.-> Completed SyncOp Loop.\n\tLoop #{}.\n\tElapsed: [{}] ms.\n\tRemaining Ops: [{}]", currentBatchSeq, elapsed, remainingOps);
					
					if(remainingOps <= 0) {
						if(taskHandle!=null) {
							taskHandle = scheduler.schedule(scheduledTask, getTSDBSyncPeriod(), TimeUnit.SECONDS);
							log.debug("Rescheduled Sync Task:{} sec.", getTSDBSyncPeriod());
						}
						syncInProgress.set(false);
						log.debug("\n\t------------------> Completed SyncOp in [{}] ms.", elapsed);
					}
					return null;
				}
				public String toString() {
					return "GroupedDeferred for all Table Groups";
				}
			});
		} while(keepRunning.get());
		final long elapsed = SystemClock.time() - startTime;
		pluginContext.publishNotification(JMX_NOTIF_SYNC_ENDED, "SyncLoop elapsed:" + elapsed + " ms.", null, OBJECT_NAME);
		log.debug("SyncOps Outer Loop Complete.");
		// First retry the failures
		//Deferred<ArrayList<Void>> priorFailsDeferred = retryPriorFails();
		// Now look for new syncs
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.syncqueue.SyncQueueProcessorMXBean#getState()
	 */
	public String getState() {
		return this.state().name();
	}
	
	/**
	 * Finds and processes new sync objects
	 * FIXME:  Fail or no, the LAST_UPDATE should be updated.
	 * FIXME: Need to handle non-callback exceptions in SYNC
	 * FIXME: Need to specify order of tables processed
	 * @param syncBatchHitMax Tracks whether or not the current batch loop has hit the max ops threshold and left incomplete sync ops on the table 
	 * @return A deferred indicating the completion of all table syncs
	 */
	protected  Deferred<ArrayList<ArrayList<Object>>> processNewSyncs(final AtomicBoolean syncBatchHitMax) {
		Connection conn = null;
		ResultSet rset = null;
		final int maxops = maxSyncOps;	// a constant snapshot of the configured max sync ops
		int executedOps = 0; 			// the number of ops executed in the current batch
		syncBatchHitMax.set(false);
		final List<Deferred<ArrayList<Object>>> allDeferreds = new ArrayList<Deferred<ArrayList<Object>>>();
		try {
			conn = dataSource.getConnection();
			ResultSet disx = sqlWorker.executeQuery(conn, "SELECT TABLE_NAME, LAST_SYNC FROM TSD_LASTSYNC ORDER BY ORDERING", true);
			int cnt = 0;
			int totalCnt = 0;
			while(disx.next()) {
//				hasRemainingOps.incrementAndGet();
				if(syncBatchHitMax.get()) break; // we already hit the max so eject
				final TSDBTable table = TSDBTable.valueOf(disx.getString(1));
				final List<Deferred<Object>> tableDeferreds = new ArrayList<Deferred<Object>>();
				log.debug("Looking for New Syncs in [{}] with LAST_UPDATES > [{}]", table.name(), disx.getTimestamp(2));
				String sqlText = String.format("SELECT '%s' as TAG_TYPE,* FROM %s WHERE LAST_UPDATE > ?", table.name().replace("TSD_", ""), table.name());
				rset = sqlWorker.executeQuery(sqlText, false, disx.getTimestamp(2));
				cnt = 0;
				for(final Object dbObject: table.ti.getObjects(rset, dbInterface)) {
					log.debug("Submitting New Sync [{}]", dbObject);
					cnt++;
					executedOps++;
					if(dbObject instanceof UIDMeta) {
						final UIDMeta dbMeta = (UIDMeta)dbObject;
						try {
							switch(dbMeta.getType()) {
							case METRIC:
								tagMunik.getOrCreateIdAsync(dbMeta.getName()).addCallback(new ValidateUIDCallback(tableDeferreds, dbMeta, TSDBTable.TSD_METRIC));
								break;
							case TAGK:
								tagKunik.getOrCreateIdAsync(dbMeta.getName()).addCallback(new ValidateUIDCallback(tableDeferreds, dbMeta, TSDBTable.TSD_TAGK));
								break;
							case TAGV:
								tagVunik.getOrCreateIdAsync(dbMeta.getName()).addCallback(new ValidateUIDCallback(tableDeferreds, dbMeta, TSDBTable.TSD_TAGV));
								break;
							default:
								throw new Error("Should not reach here");								
							}
							pendingOps.incrementAndGet();
						} catch (Exception ex) {
							log.error("Failed to sync UISMeta [{}]", dbMeta, ex);
							try { 
								dbInterface.recordSyncQueueFailure(dbMeta, table);
							} catch (Exception ex2) {
								log.error("Failed to record UIDMeta [{}] SyncQueueFailure", dbMeta, ex2);
							}
						}
					} else if(dbObject instanceof TSMeta) {
						final TSMeta tsMeta = (TSMeta)dbObject;
						try {
							TSMeta.metaExistsInStorage(tsdb, tsMeta.getTSUID()).addCallback(new ValidateTSMetaCallback(tableDeferreds, tsMeta));
							pendingOps.incrementAndGet();
						} catch (Exception ex) {
							log.error("Failed to sync TSMeta [{}]", tsMeta, ex);
							try { 
								dbInterface.recordSyncQueueFailure(tsMeta);								
							} catch (Exception ex2) {
								log.error("Failed to record TSMeta [{}] SyncQueueFailure", tsMeta, ex2);
							}							
						}
					} else if(dbObject instanceof Annotation) {
						Annotation ann = (Annotation)dbObject;
						try {
							ann.syncToStorage(tsdb, false).addBoth(new SyncAnnotationCallback(tableDeferreds, ann));
							pendingOps.incrementAndGet();
						} catch (Exception ex) {
							log.error("Failed to sync Annotation [{}]", ann, ex);
							try { 
								dbInterface.recordSyncQueueFailure(ann);
							} catch (Exception ex2) {
								log.error("Failed to record Annotation [{}] SyncQueueFailure", ann, ex2);								
							}							
						}
					} else {
						log.error("Unrecognized Meta Object [{}]:[{}]", dbObject.getClass().getName(), dbObject);
					}
					if(executedOps==maxops) {
						syncBatchHitMax.set(true);
						break;
					}
				}	// end of table loop. if max ops is hit, we'll break to here, finish the post table loop and eject.								
				rset.close();
				rset = null;				
				conn.commit();				
				totalCnt += cnt;
				log.debug("Processed [{}] New Syncs from [{}]. Total ops this loop: [{}]", cnt, table.name(), totalCnt);
				
				Deferred<ArrayList<Object>> groupedTableDeferred = Deferred.group(tableDeferreds); 
				allDeferreds.add(groupedTableDeferred);				
				if(cnt>0) {
					groupedTableDeferred.addCallback(new Callback<Void, ArrayList<Object>>() {
						public Void call(ArrayList<Object> completedTasks) throws Exception {
							for(int x = 0; x < completedTasks.size(); x++) {
								pendingOps.decrementAndGet();
							}
									
							long currentTime = SystemClock.time();
							Timestamp ts = new Timestamp(currentTime);
							sqlWorker.execute("UPDATE TSD_LASTSYNC SET LAST_SYNC = ? WHERE TABLE_NAME = ?", ts, table.name());
							log.debug("Set Highwater on [{}] to [{}]", table.name(), new Date(currentTime));
							return null;
						}
						public String toString() {
							return "GroupedTable Completion Callback for [" + table.name() + "]"; 
						}
					});
				}
			}  // end of batch loop
			log.debug("Completed check of all tables for pending synchs");
		} catch (Throwable ex) {
			log.error("Unexpected SynQueueProcessor Error", ex);
		} finally {
			if(rset!=null) try { rset.close(); } catch (Exception ex) {/* No Op */}
			if(conn!=null) try { conn.close(); } catch (Exception ex) {/* No Op */}
		}
		
		return Deferred.group(allDeferreds);
	}
	
	/**
	 * <p>Title: SyncAnnotationCallback</p>
	 * <p>Description: Handles the callback to store an Annotation to storage</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>net.opentsdb.catalog.syncqueue.SyncProcessorQueue.ValidateTSMetaCallback</code></p>
	 */
	class SyncAnnotationCallback implements Callback<Void, Boolean> {
		/** The Annotation to be synced */
		final Annotation annotation;
		/** The accumulated list of Meta sync deferred completions */
		final List<Deferred<Object>> tableDeferreds;
		/** The sync completion deferred */
		final Deferred<Object> done = new Deferred<Object>();
		
		

		/**
		 * Creates a new SyncAnnotationCallback
		 * @param tableDeferreds The accumulated list of UIDMeta sync deferred completions
		 * @param annotation The Annotation to be synced
		 */
		public SyncAnnotationCallback(List<Deferred<Object>> tableDeferreds, Annotation annotation) {
			this.tableDeferreds = tableDeferreds;
			this.annotation = annotation;
			tableDeferreds.add(done);
		}

		/**
		 * {@inheritDoc}
		 * @see com.stumbleupon.async.Callback#call(java.lang.Object)
		 */
		@Override
		public Void call(final Boolean successful) throws Exception {
			if(successful) {
				log.debug("Annotation [{}] successfully synced", annotation);
				dbInterface.clearSyncQueueFailure(annotation);								
			} else {
				log.warn("Annotation [{}] CAS Update Failure", annotation);
				dbInterface.recordSyncQueueFailure(annotation);
			}
			done.callback(successful);
			return null;
		}
		
		/**
		 * {@inheritDoc}
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			return getClass().getSimpleName() + "-" + annotation;
		}
	}

	
	/**
	 * <p>Title: ValidateTSMetaCallback</p>
	 * <p>Description: Handles the callback of the call to validate a TSMeta id exists and completes the TSMeta sync to storage</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>net.opentsdb.catalog.syncqueue.SyncProcessorQueue.ValidateTSMetaCallback</code></p>
	 */
	class ValidateTSMetaCallback implements Callback<Void, Boolean> {
		/** The TSMeta to be synced */
		final TSMeta tsMeta;
		/** The accumulated list of UIDMeta sync deferred completions */
		final List<Deferred<Object>> tableDeferreds;
		/** The sync completion deferred */
		final Deferred<Object> done = new Deferred<Object>();
		
		

		/**
		 * Creates a new ValidateTSMetaCallback
		 * @param tableDeferreds The accumulated list of UIDMeta sync deferred completions
		 * @param tsMeta The TSMeta to be synced
		 */
		public ValidateTSMetaCallback(List<Deferred<Object>> tableDeferreds, TSMeta tsMeta) {
			this.tableDeferreds = tableDeferreds;
			this.tsMeta = tsMeta;
			tableDeferreds.add(done);
		}

		/**
		 * {@inheritDoc}
		 * @see com.stumbleupon.async.Callback#call(java.lang.Object)
		 */
		@Override
		public Void call(final Boolean tsUIDExists) throws Exception {
			log.debug("Validated TSMeta [{}] Exists: [{}]", tsMeta, tsUIDExists);
			final String op = tsUIDExists ? "syncToStorage" : "storeNew";
			final Deferred<Boolean> d = tsUIDExists ? tsMeta.syncToStorage(tsdb, false) : tsMeta.storeNew(tsdb);
			d.addCallbacks(
					new Callback<Void, Boolean>() {
						public Void call(Boolean successful) throws Exception {
							if(successful) {
								log.debug("TSMeta [{}] successfully sync op:[{}]", tsMeta, op);
								dbInterface.clearSyncQueueFailure(tsMeta);								
							} else {
								if(tsUIDExists) {
									log.warn("TSMeta [{}] CAS Update Failure", tsMeta);
									dbInterface.recordSyncQueueFailure(tsMeta);
								} else {
									log.warn("TSMeta [{}] Store CAS Failure. Not registering for retry.", tsMeta);
								}
							}
							done.callback(successful);
							return null;
						}
						public String toString() {
							return "TSMeta Sync Callback Handler for [" + tsMeta + "]:" + op;
						}						
					},
					new Callback<Void, Exception>() {
						public Void call(Exception ex) throws Exception {
							log.error("Failed to [{}] TSMeta [{}]", op, tsMeta, ex);
							dbInterface.recordSyncQueueFailure(tsMeta);
							done.callback(ex);							
							return null;
						}
						public String toString() {
							return "TSMeta Sync Errback Handler for [" + tsMeta + "]:" + op;
						}						
					}
			);			
			return null;
		}
		
		/**
		 * {@inheritDoc}
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			return getClass().getSimpleName() + "-TSMETA:" + tsMeta;
		}
	}
		
	/**
	 * <p>Title: ValidateUIDCallback</p>
	 * <p>Description: Handles the callback of the call to validate a UIDMeta id exists and completes the UIDMeta sync to storage</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>net.opentsdb.catalog.syncqueue.SyncProcessorQueue.ValidateUIDCallback</code></p>
	 */
	class ValidateUIDCallback implements Callback<Void, byte[]> {
		/** The accumulated list of UIDMeta sync deferred completions */
		final List<Deferred<Object>> tableDeferreds;
		/** The UIDMeta to be synced */
		final UIDMeta uidMeta;
		/** The TSDBTable type of the passed UIDMeta */
		final TSDBTable table;
		/** The sync completion deferred */
		final Deferred<Object> done = new Deferred<Object>();
		
		/**
		 * Creates a new ValidateUIDCallback
		 * @param tableDeferreds The accumulated list of UIDMeta sync deferred completions
		 * @param uidMeta The UIDMeta to be synced
		 * @param table The TSDBTable type of the passed UIDMeta
		 */
		public ValidateUIDCallback(List<Deferred<Object>> tableDeferreds, UIDMeta uidMeta, TSDBTable table) {
			this.tableDeferreds = tableDeferreds;
			this.uidMeta = uidMeta;
			this.table = table;
			tableDeferreds.add(done);
		}
		
		/**
		 * {@inheritDoc}
		 * @see com.stumbleupon.async.Callback#call(java.lang.Object)
		 */
		public Void call(byte[] uidByteId) throws Exception {
			log.debug("Validated UIDMeta [{}] Exists", uidMeta);
			final Deferred<Boolean> d = uidMeta.syncToStorage(tsdb, false);
			d.addCallbacks(
					new Callback<Void, Boolean>() {
						public Void call(Boolean success) throws Exception {
							if(success) {
								log.debug("UIDMeta [{}] successfully synced to store", uidMeta);
								dbInterface.clearSyncQueueFailure(uidMeta, table);
							} else {
								log.warn("UIDMeta [{}] CAS Update Failure", uidMeta);
								dbInterface.recordSyncQueueFailure(uidMeta, table);
							}
							done.callback(success);						
							return null;
						}
						public String toString() {
							return "UIDMeta Sync Callback Handler for [" + uidMeta + "]";
						}
					},
					new Callback<Void, Exception>() {
						public Void call(Exception ex) throws Exception {
							log.error("Failed to update UIDMeta [{}]", uidMeta, ex);
							dbInterface.recordSyncQueueFailure(uidMeta, table);
							done.callback(ex);
							return null;
						}
						public String toString() {
							return "UIDMeta Sync Errback Handler for [" + uidMeta + "]";
						}							
					}
			);
			return null;
		}
		/**
		 * {@inheritDoc}
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			return getClass().getSimpleName() + "-" + uidMeta;
		}
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
				rset = sqlWorker.executeQuery(conn, "SELECT * FROM TSD_LASTSYNC_FAILS WHERE TABLE_NAME = ? ORDER BY LAST_ATTEMPT", false, table.name());
				log.debug("Retrying failed syncs for {}", table.name());
				int cnt = 0;
				while(rset.next()) {
					String tableName = rset.getString(1);
					String objectId = rset.getString(2);
					int attempts = rset.getInt(3);
					Object failedObject = getDBObject(conn, tableName, objectId);
					log.debug("Submitting Failed Sync [{}]", failedObject);
//					tableDeferreds.add(table.ti.sync(failedObject, tsdb).addBoth(syncCompletionHandler(failedObject, objectId, attempts)));
					pendingOps.incrementAndGet();
					cnt++;
				}
				log.debug("Retried [{}] failed syncs", cnt);
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
		TableInfo<?> ti = TSDBTable.valueOf(tableName).ti;
		ResultSet r = sqlWorker.executeQuery(conn, ti.getByPKSql(), true, ti.getBindablePK(objectId));
		return ti.getObjects(r, dbInterface).iterator().next();
	}
	
	
	
//	final Object pk = tsdbTable.ti.getPk(conn, dbMeta);
//	final SyncCallbackContainer<UIDMeta, byte[]> scc = new SyncCallbackContainer<UIDMeta, byte[]>(tsdbTable, dbMeta, pk);
//	
//	final Callback<SyncCallbackContainer<UIDMeta, byte[]>, byte[]> cb = syncCompletionHandler(scc);
//	final Callback<SyncCallbackContainer<UIDMeta, byte[]>, Exception> ce = syncExceptionHandler(scc);
	
	
	protected <T, F> Callback<SyncCallbackContainer<T, F>, Exception> syncExceptionHandler(final SyncCallbackContainer<T, F> scc) {		
		return new Callback<SyncCallbackContainer<T, F>, Exception>() {
			public SyncCallbackContainer<T, F> call(Exception ex) throws Exception {				
				return scc.setEx(ex).incrDepth();
			}
			public String toString() {
				return "syncExceptionHandler->" + scc.toString();
			}
		};
	}
	
	protected <T, F> Callback<SyncCallbackContainer<T, F>, F> syncCompletionHandler(final SyncCallbackContainer<T, F> scc) {
		return new Callback<SyncCallbackContainer<T, F>, F>() {
//			public SyncCallbackContainer<T, F> call(F callbackObject) throws Exception {				
//				return scc.callback(callbackObject).incrDepth();
//			}
			public String toString() {
				return "syncCompletionHandler->" + scc.toString();
			}

			@Override
			public SyncCallbackContainer<T, F> call(F callbackValue) throws Exception {
				// TODO Auto-generated method stub
				return null;
			}

	
		};
	}
	
	

	/**
	 * Returns the number of pending Sync ops we're waiting on 
	 * @return the number of pending Sync ops we're waiting on
	 */
	public long getPendingSynchOps() {
		return pendingOps.get();
	}


	/**
	 * Returns the maximum number of sync ops to execute per batch 
	 * @return the maxSyncOps
	 */
	public int getMaxSyncOps() {
		return maxSyncOps;
	}


	/**
	 * Sets the maximum number of sync ops to execute per batch 
	 * @param maxSyncOps the maxSyncOps to set
	 */
	public void setMaxSyncOps(int maxSyncOps) {
		this.maxSyncOps = maxSyncOps;
	}
	
}


//protected void startTSMetaUpdateLoop(final TSMeta tMeta, final AtomicInteger retryCount) {
//	final SyncQueueProcessor SQP = this;
//	final TSMeta tsMeta = dbInterface.readTSMetas(sqlWorker.executeQuery("SELECT * FROM TSD_TSMETA WHERE TSUID = ?", true, tMeta.getTSUID())).iterator().next();
//	try {
//		tsMeta.syncToStorage(tsdb, true).addCallback(new Callback<Void, Boolean>(){
//			public Void call(Boolean success) throws Exception {
//				if(success) {
//					log.info("TSMeta Update Successful [{}] after [{}] retries", tsMeta, retryCount.get());
//				} else {
//					int retries = retryCount.incrementAndGet();
//					if(retries>=20) {
//						log.error("Exhausted retries updating TSMeta [{}]", tsMeta);
//					} else {
//						log.error("TSMeta update retry failed for [{}]. Next retry: [{}]", tsMeta, retries);
//						scheduler.schedule(new Runnable() {
//							public void run() {
//								SQP.startTSMetaUpdateLoop(tsMeta, retryCount);
//							}
//						}, 500, TimeUnit.MILLISECONDS);
//					}
//				}
//				return null;
//			}
//		}).addErrback(new Callback<Void, Exception>(){
//			public Void call(Exception ex) throws Exception {
//				log.error("TSMeta update for [{}] FAILED:", tsMeta, ex);				
//				return null;
//			}
//		});
//	} catch (Exception ex) {
//		ex.printStackTrace(System.err);
//	}
//}
//
//protected void startUIDMetaUpdateLoop(final UIDMeta uMeta, final AtomicInteger retryCount) {
//	final SyncQueueProcessor SQP = this;
//	final TSDBTable table = TSDBTable.valueOf("TSD_" + uMeta.getType().name());
//	
//	final UIDMeta uidMeta = (UIDMeta) table.ti.getObjects(sqlWorker.executeQuery(table.ti.getByPKSql(), true, uMeta.getUID()), dbInterface).iterator().next();
//	try {
//		//uidMeta.setCustom(null);
//		UIDMeta.getUIDMeta(tsdb, uidMeta.getType(), uniques.get(uidMeta.getType()).getId(uidMeta.getName())).addCallback(new Callback<Void, UIDMeta>() {
//			public Void call(UIDMeta casMeta) throws Exception {
//				casMeta.setNotes(uidMeta.getNotes());
//				casMeta.syncToStorage(tsdb, false).addCallback(new Callback<Void, Boolean>(){
////				uidMeta.syncToStorage(tsdb, false).addCallback(new Callback<Void, Boolean>(){
//					public Void call(Boolean success) throws Exception {
//						if(success) {
//							log.info("UIDMeta Update Successful [{}] after [{}] retries", uidMeta, retryCount.get());
//						} else {
//							int retries = retryCount.incrementAndGet();
//							if(retries>=20) {
//								log.error("Exhausted retries updating UIDMeta [{}]", uidMeta);
//							} else {
//								log.error("UIDMeta update retry failed for [{}]/[{}]. Next retry: [{}]", uidMeta, uidMeta.getCreated(), retries);
//								scheduler.schedule(new Runnable() {
//									public void run() {
//										SQP.startUIDMetaUpdateLoop(uidMeta, retryCount);
//									}
//								}, 500, TimeUnit.MILLISECONDS);
//							}
//						}
//						return null;
//					}
//				}).addErrback(new Callback<Void, Exception>(){
//					public Void call(Exception ex) throws Exception {
//						log.error("UIDMeta update for [{}] FAILED:", uidMeta, ex);				
//						return null;
//					}
//				});
//				
//				return null;
//			}
//		});
//	} catch (Exception ex) {
//		ex.printStackTrace(System.err);
//	}
//}
//
