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

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
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
import net.opentsdb.meta.TSMeta;
import net.opentsdb.meta.UIDMeta;
import net.opentsdb.uid.UniqueId;

import org.helios.tsdb.plugins.meta.MetaSynchronizer;
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
	/** Tracks the number of pending ops */
	protected final AtomicLong pendingOps = new AtomicLong(0);
	/** The uniqueid for tag keys */
	protected final UniqueId tagKunik;
	/** The uniqueid for tag values */
	protected final UniqueId tagVunik;
	/** The uniqueid for metric names */
	protected final UniqueId tagMunik;
	
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
		log.debug("Starting TSDB Sync Run....");
		final long startTime = System.currentTimeMillis();
		// First retry the failures
		//Deferred<ArrayList<Void>> priorFailsDeferred = retryPriorFails();
		// Now look for new syncs
		Deferred<ArrayList<Object>> processNewDeferred = processNewSyncs();
		final Runnable scheduledTask = this;
		processNewDeferred.addBothDeferring(new Callback<Deferred<Object>, ArrayList<Object>>() {

			@Override
			public Deferred<Object> call(ArrayList<Object> arg) throws Exception {
				long elapsed = System.currentTimeMillis() - startTime;
				log.debug("\n\t------------------> Completed Sync in [{}] ms.", elapsed);
//				new MetaSynchronizer(tsdb).process(true);
				if(taskHandle!=null) {
					taskHandle = scheduler.schedule(scheduledTask, getTSDBSyncPeriod(), TimeUnit.SECONDS);
					log.debug("Rescheduled Sync Task:{} sec.", getTSDBSyncPeriod());
				}
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
	protected Deferred<ArrayList<Object>> processNewSyncs() {
		Connection conn = null;
		ResultSet rset = null;
		final List<Deferred<Object>> allDeferreds = new ArrayList<Deferred<Object>>();
		try {
			conn = dataSource.getConnection();
			ResultSet dis = sqlWorker.executeQuery(conn, "SELECT TABLE_NAME, LAST_SYNC FROM TSD_LASTSYNC ORDER BY ORDERING", false);
			
			while(dis.next()) {
				final TSDBTable table = TSDBTable.valueOf(dis.getString(1));
				final List<Deferred<Boolean>> tableDeferreds = new ArrayList<Deferred<Boolean>>();
				log.debug("Looking for New Syncs in [{}] with LAST_UPDATES > [{}]", table.name(), dis.getTimestamp(2));
				String sqlText = String.format("SELECT '%s' as TAG_TYPE,* FROM %s WHERE LAST_UPDATE > ?", table.name().replace("TSD_", ""), table.name());
				rset = sqlWorker.executeQuery(sqlText, false, dis.getTimestamp(2));
				int cnt = 0;
				for(final Object dbObject: table.ti.getObjects(rset, dbInterface)) {
					log.info("Submitting New Sync [{}]", dbObject);
					if(dbObject instanceof UIDMeta) {
						final UIDMeta dbMeta = (UIDMeta)dbObject;
						Deferred<byte[]> deferredDbMetaKey = null;
						try {
							switch(dbMeta.getType()) {
								case METRIC:
									deferredDbMetaKey = tagMunik.getOrCreateIdAsync(dbMeta.getName());
									break;
								case TAGK:
									deferredDbMetaKey = tagKunik.getOrCreateIdAsync(dbMeta.getName());
									break;
								case TAGV:
									deferredDbMetaKey = tagVunik.getOrCreateIdAsync(dbMeta.getName());
									break;
								default:
									throw new Error("Should not reach here");								
							}
							
							deferredDbMetaKey.addCallback(new Callback<UIDMeta, byte[]>() {
								public UIDMeta call(byte[] arg) throws Exception {
									log.info("Updating [{}]/[{}] with Notes:[{}] and Custom:[{}]", dbMeta, dbMeta.getName(), dbMeta.getNotes(), dbMeta.getCustom());
									dbMeta.syncToStorage(tsdb, true)
										.addCallback(new Callback<Object, Boolean>() {
											public Object call(Boolean success) throws Exception {
												if(success) {
													log.info("UID Sync Callback for [{}] -->  [{}]", dbObject, success);
												} else {
													log.info("UID Sync CAS Failure for UIDMeta [{}]. Starting Update Loop", dbObject);
													startUIDMetaUpdateLoop(dbMeta, new AtomicInteger(0));
												}
												return dbMeta;
											}
										})
										.addErrback(new Callback<Object, Boolean>() {
											public Object call(Boolean success) throws Exception {	
												log.error("Failed to synch [{}]", dbObject);
												return dbMeta;
											}
										});																		
									return dbMeta;
								}
							}).addErrback(new Callback<Void, Exception>() {
								public Void call(Exception ex) throws Exception {
									log.error("Failed to get byte[] for UIDMeta [{}]", dbMeta, ex);
									return null;
								}
							});
						} catch (Exception ex) {
							log.error("Unexpected exception processing UIDMeta", ex);
						}
					} else if(dbObject instanceof TSMeta) {
						final TSMeta dbMeta = (TSMeta)dbObject;
						try {
							
							
							
							ensureUIDsExist(dbMeta).addCallback(new Callback<Void, ArrayList<Boolean>>() {
								public Void call(ArrayList<Boolean> success) throws Exception {
									TSMeta.metaExistsInStorage(tsdb, dbMeta.getTSUID())
									.addCallback(new Callback<Void, Boolean>() {
										public Void call(Boolean exists) throws Exception {
											if(exists) {
												try {
													dbMeta.syncToStorage(tsdb, true)
													.addCallback(new Callback<Object, Boolean>() {
														public Object call(Boolean success) throws Exception {	
															log.info("TSUID Sync Callback for [{}] -->  [{}]", dbObject, success);
															return dbMeta;
														}
													})
													.addErrback(new Callback<Object, Boolean>() {
														public Object call(Boolean success) throws Exception {	
															log.error("Failed to synch [{}]", dbObject);
															return dbMeta;
														}
													});
												} catch (IllegalStateException ise) {
													/* No Op. Means no changes detected */
												}
											} else {
												log.info("TSMeta [{}] does not exist. Need to create.....", dbMeta.getTSUID());
												//dbMeta.storeNew(tsdb).joinUninterruptibly(5000);
												dbMeta.storeNew(tsdb)
													.addCallback(new Callback<Void, Boolean>(){
														public Void call(Boolean createdOk) throws Exception {
															if(createdOk) {
																log.info("TSMeta [{}] Created", dbMeta);
															} else {
																log.info("TSMeta [{}] Not Created", dbMeta);
															}
															return null;
														}
													}).addErrback(new Callback<Void, Exception>(){
														public Void call(Exception ex) throws Exception {
															log.error("TSMeta [{}] Creation Error", dbMeta, ex);
															return null;
														}
													});
											}
											return null;
										}
									})
									.addErrback(new Callback<Void, Exception>() {
										public Void call(Exception ex) throws Exception {
											log.error("Failed to get byte[] for UIDMeta [{}]", dbMeta, ex);
											return null;
										}
									});
									dbMeta.syncToStorage(tsdb, true).addBoth(new Callback<Object, Boolean>() {
										@SuppressWarnings("cast")
										public Object call(Boolean success) throws Exception {	
											if(success) {
												log.info("TSUID Sync Callback for [{}] -->  [{}]", dbObject, success);
											} else {
//												sqlWorker.execute("INSERT INTO TSD_LASTSYNC_FAILS (TABLE_NAME, OBJECT_ID, ATTEMPTS, LAST_ATTEMPT) VALUES (?,?,?,?)", table.name(), table.ti.getPk(null, dbMeta), 1, SystemClock.getTimestamp());
//												log.info("First Sync Fail for {}.{}", "TSMeta", table.ti.getBindablePK(dbMeta));
												try {
													startTSMetaUpdateLoop((TSMeta)dbMeta, new AtomicInteger(0));
												} catch (Exception ex) {
													ex.printStackTrace(System.err);
												}
											}
											
											return dbMeta;
										}
									});									
									return null;
								}

								
							});
						} catch (Throwable t) {
							log.error("Failed to sync UIDMeta", t);
						}
					}

					
//					table.ti.sync(dbObject, tsdb).addBoth(new Callback<Object, Boolean>() {
//						public Object call(Boolean success) throws Exception {	
//							log.info("Sync Callback for [{}] -->  [{}]", dbObject, success);
//							return dbObject;
//						}
//					});
//					Deferred<Boolean> syncDeferred = table.ti.sync(dbObject, tsdb);
//					syncDeferred.addBothDeferring(syncCompletionHandler(dbObject, table.ti.getPk(conn, dbObject), 0));
//					tableDeferreds.add(syncDeferred);
					pendingOps.incrementAndGet();
					cnt++;
				}				
				log.debug("Processed [{}] New Syncs from [{}]", cnt, table.name());
				rset.close();
				rset = null;
				allDeferreds.add(Deferred.group(tableDeferreds).addBothDeferring(new Callback<Deferred<Object>, ArrayList<Boolean>>() {
					/**
					 * {@inheritDoc}
					 * @see com.stumbleupon.async.Callback#call(java.lang.Object)
					 */
					@Override
					public Deferred<Object> call(ArrayList<Boolean> arg) throws Exception {
						long currentTime = SystemClock.time();
						Timestamp ts = new Timestamp(currentTime);
						sqlWorker.execute("UPDATE TSD_LASTSYNC SET LAST_SYNC = ? WHERE TABLE_NAME = ?", ts, table.name());
						log.debug("Set Highwater on [{}] to [{}]", table.name(), new Date(currentTime));
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
	
	
	
	protected void startTSMetaUpdateLoop(final TSMeta tMeta, final AtomicInteger retryCount) {
		final SyncQueueProcessor SQP = this;
		final TSMeta tsMeta = dbInterface.readTSMetas(sqlWorker.executeQuery("SELECT * FROM TSD_TSMETA WHERE TSUID = ?", true, tMeta.getTSUID())).iterator().next();
		try {
			tsMeta.syncToStorage(tsdb, true).addCallback(new Callback<Void, Boolean>(){
				public Void call(Boolean success) throws Exception {
					if(success) {
						log.info("TSMeta Update Successful [{}] after [{}] retries", tsMeta, retryCount.get());
					} else {
						int retries = retryCount.incrementAndGet();
						if(retries>=20) {
							log.error("Exhausted retries updating TSMeta [{}]", tsMeta);
						} else {
							log.error("TSMeta update retry failed for [{}]. Next retry: [{}]", tsMeta, retries);
							scheduler.schedule(new Runnable() {
								public void run() {
									SQP.startTSMetaUpdateLoop(tsMeta, retryCount);
								}
							}, 500, TimeUnit.MILLISECONDS);
						}
					}
					return null;
				}
			}).addErrback(new Callback<Void, Exception>(){
				public Void call(Exception ex) throws Exception {
					log.error("TSMeta update for [{}] FAILED:", tsMeta, ex);				
					return null;
				}
			});
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		}
	}
	
	protected void startUIDMetaUpdateLoop(final UIDMeta uMeta, final AtomicInteger retryCount) {
		final SyncQueueProcessor SQP = this;
		final TSDBTable table = TSDBTable.valueOf("TSD_" + uMeta.getType().name());
		
		final UIDMeta uidMeta = (UIDMeta) table.ti.getObjects(sqlWorker.executeQuery(table.ti.getByPKSql(), true, uMeta.getUID()), dbInterface).iterator().next();
		try {
			//uidMeta.setCustom(null);
			UIDMeta.getUIDMeta(tsdb, uidMeta.getType(), uniques.get(uidMeta.getType()).getId(uidMeta.getName())).addCallback(new Callback<Void, UIDMeta>() {
				public Void call(UIDMeta casMeta) throws Exception {
					casMeta.setNotes(uidMeta.getNotes());
					casMeta.syncToStorage(tsdb, true).addCallback(new Callback<Void, Boolean>(){
						public Void call(Boolean success) throws Exception {
							if(success) {
								log.info("UIDMeta Update Successful [{}] after [{}] retries", uidMeta, retryCount.get());
							} else {
								int retries = retryCount.incrementAndGet();
								if(retries>=20) {
									log.error("Exhausted retries updating UIDMeta [{}]", uidMeta);
								} else {
									log.error("UIDMeta update retry failed for [{}]. Next retry: [{}]", uidMeta, retries);
									scheduler.schedule(new Runnable() {
										public void run() {
											SQP.startUIDMetaUpdateLoop(uidMeta, retryCount);
										}
									}, 500, TimeUnit.MILLISECONDS);
								}
							}
							return null;
						}
					}).addErrback(new Callback<Void, Exception>(){
						public Void call(Exception ex) throws Exception {
							log.error("UIDMeta update for [{}] FAILED:", uidMeta, ex);				
							return null;
						}
					});
					
					return null;
				}
			});
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		}
	}
	
	
	protected Deferred<ArrayList<Boolean>> ensureUIDsExist(final TSMeta tsMeta) {
		tsMeta.setRetention(tsMeta.getRetention()+1);
		log.info("Ensuring UIDs exist for TSMeta [{}]", tsMeta);
		List<Deferred<Boolean>> defs = new ArrayList<Deferred<Boolean>>();
		for(final Map.Entry<String, String> entry: dbInterface.getNamesForUIDs(tsMeta.getTSUID()).entrySet()) {
			final String name = entry.getKey();
			final UniqueId.UniqueIdType utype = UniqueId.UniqueIdType.valueOf(entry.getValue());
			final UniqueId u = uniques.get(utype);
			log.info("Resolved Name:---------------------------[{}]", name);
			defs.add(u.getOrCreateIdAsync(name).addCallback(new Callback<Boolean, byte[]>(){
				public Boolean call(byte[] arg) throws Exception {
					log.info("Checked UID [{}] for TSMeta [{}]", name, tsMeta);
					return true;
				}
			}));								
		}
		return Deferred.group(defs);
	}
	
	
	  /**
	   * Extracts a map of tagk/tagv pairs from a tsuid
	   * @param tsuid The tsuid to parse
	   * @param metric_width The width of the metric tag in bytes
	   * @param tagk_width The width of tagks in bytes
	   * @param tagv_width The width of tagvs in bytes
	   * @return A map of byte keys for tge metric/tagk/tagv keys with the enum type as the value 
	   * @throws IllegalArgumentException if the TSUID is malformed
	   */
	   public static Map<byte[], UniqueId.UniqueIdType> getTagPairsFromTSUID(final String tsuid,
	      final short metric_width, final short tagk_width, 
	      final short tagv_width) {
		   
		   
		   
	    if (tsuid == null || tsuid.isEmpty()) {
	      throw new IllegalArgumentException("Missing TSUID");
	    }
	    if (tsuid.length() <= metric_width * 2) {
	      throw new IllegalArgumentException(
	          "TSUID is too short, may be missing tags");
	    }
	    Map<byte[], UniqueId.UniqueIdType> byteKeys = new HashMap<byte[], UniqueId.UniqueIdType>();
	    final int pair_width = (tagk_width * 2) + (tagv_width * 2);
	    
	    
	    byteKeys.put(UniqueId.stringToUid(tsuid.substring(0, (metric_width * 2))), UniqueId.UniqueIdType.METRIC);
	    
	    // start after the metric then iterate over each tagk/tagv pair
	    for (int i = metric_width * 2; i < tsuid.length(); i+= pair_width) {
	      if (i + pair_width > tsuid.length()){
	        throw new IllegalArgumentException(
	            "The TSUID appears to be malformed, improper tag width");
	      }
	      String tag = tsuid.substring(i, i + (tagk_width * 2));
	      byteKeys.put(UniqueId.stringToUid(tag), UniqueId.UniqueIdType.TAGK);
	      tag = tsuid.substring(i + (tagk_width * 2), i + pair_width);
	      byteKeys.put(UniqueId.stringToUid(tag), UniqueId.UniqueIdType.TAGV);	     
	    }
	    return byteKeys;
	   }
	  
	protected void flagTSMeta(TSMeta tsMeta) {
		try {
			Field f = TSMeta.class.getDeclaredField("changed");
			f.setAccessible(true);
			HashMap<String, Boolean> changed  = (HashMap<String, Boolean>)f.get(tsMeta);
			changed.put("I Changed !", true);
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
			/* No Op */
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
				rset = sqlWorker.executeQuery(conn, "SELECT * FROM TSD_LASTSYNC_FAILS WHERE TABLE_NAME = ? ORDER BY LAST_ATTEMPT DESC", false, table.name());
				log.info("Retrying failed syncs for {}", table.name());
				int cnt = 0;
				while(rset.next()) {
					String tableName = rset.getString(1);
					String objectId = rset.getString(2);
					int attempts = rset.getInt(3);
					Object failedObject = getDBObject(conn, tableName, objectId);
					log.info("Submitting Failed Sync [{}]", failedObject);
//					tableDeferreds.add(table.ti.sync(failedObject, tsdb).addBoth(syncCompletionHandler(failedObject, objectId, attempts)));
					pendingOps.incrementAndGet();
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
	protected Callback<Deferred<Object>, Boolean> syncCompletionHandler(final Object syncedObject, final Object pk, final int attempts) {
		return new Callback<Deferred<Object>, Boolean>() {
			@Override
			public Deferred<Object> call(Boolean success) throws Exception {
				TSDBTable tab = TSDBTable.getTableFor(syncedObject);
				pendingOps.decrementAndGet();
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
				return Deferred.fromResult(syncedObject);
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
	
	
}
