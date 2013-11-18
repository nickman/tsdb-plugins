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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sql.DataSource;

import net.opentsdb.catalog.CatalogDBInterface;
import net.opentsdb.catalog.datasource.CatalogDataSource;
import net.opentsdb.core.TSDB;
import net.opentsdb.meta.UIDMeta;
import net.opentsdb.uid.UniqueId.UniqueIdType;

import org.helios.tsdb.plugins.service.PluginContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.AbstractService;
import com.stumbleupon.async.Callback;

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
	/** The sync queue polling period in ms. */
	protected long pollingPeriod = DEFAULT_DB_SYNCQ_POLLER_PERIOD;
	/** The scheduler for kicking off poll events */
	protected ScheduledExecutorService scheduler = null;
	/** The handle to the scheduled task */
	protected ScheduledFuture<?> taskHandle = null;
	/** The thread factory serial number factory */
	protected final AtomicInteger serial = new AtomicInteger(0);
	
	
	/** The config property name for the Sync Queue polling period in ms. */
	public static final String DB_SYNCQ_POLLER_PERIOD = "helios.search.catalog.syncq.period";
	/** The default Sync Queue polling period in ms. */
	public static final long DEFAULT_DB_SYNCQ_POLLER_PERIOD = 5000;
	
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
	}


	/**
	 * {@inheritDoc}
	 * @see com.google.common.util.concurrent.AbstractService#doStart()
	 */
	@Override
	protected void doStart() {
		log.info("\n\t=========================================\n\tStarting SyncQueueProcessor\n\t=========================================");
		scheduler = Executors.newScheduledThreadPool(2, this);
		taskHandle = scheduler.scheduleWithFixedDelay(this, pollingPeriod, pollingPeriod, TimeUnit.MILLISECONDS);
		log.info("Sync Poller Scheduled for [{}] ms. period", pollingPeriod);		
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
		log.info("\n\t=========================================\n\tSyncQueueProcessor Stopped\n\t=========================================");
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
		log.debug("Starting SyncQueue poll cycle");
		Connection conn = null;
		PreparedStatement pollPs = null;
		ResultSet pollRset = null;
		Object[] row = null;
		try {
			conn = dataSource.getConnection();
			conn.setAutoCommit(false);
			pollPs = conn.prepareStatement("SELECT * FROM SYNC_QUEUE ORDER BY OP_TYPE");
			pollRset = pollPs.executeQuery();
			while(pollRset.next()) {
				row = new Object[7];
				for(int i = 0; i < 7; i++) {
					row[i] = pollRset.getObject(i+1); 
				}
				String opType = row[3].toString();
				if("D".equals(opType)) {
					 processDelete(row);
				} else if("I".equals(opType)) {
					processInsert(row);
				} else if("U".equals(opType)) {
					processUpdate(row);
				} else {
					log.warn("yeow. Unrecognized optype in sync-processor queue [{}]", opType);
				}
			}
			pollRset.close(); pollRset = null;
			pollPs.close(); pollPs = null;
			conn.commit(); conn.close(); conn = null;
		} catch (Exception ex) {
			log.warn("SyncQueueProcessor Poll Cycle Exception", ex);
			// so do we rollback or what ?
		} finally {
			if(pollRset!=null) try { pollRset.close(); } catch (Exception x) {/* No Op */}
			if(pollPs!=null) try { pollPs.close(); } catch (Exception x) {/* No Op */}
			if(conn!=null) try { conn.close(); } catch (Exception x) {/* No Op */}
			
		}
		log.debug("SyncQueue poll cycle complete");
	}
	
	

	protected void processDelete(Object[] row) {
		if(UIDMETA_TABLES.contains(row[1])) {
			UniqueIdType type = UniqueIdType.valueOf(row[1].toString().replace("TSD_", ""));
			final UIDMeta uidMeta = new UIDMeta(type, (String)row[2]); 
			uidMeta.delete(tsdb).addCallback(new Callback<Void, UIDMeta>(){
				/**
				 * {@inheritDoc}
				 * @see com.stumbleupon.async.Callback#call(java.lang.Object)
				 */
				@Override
				public Void call(UIDMeta arg) throws Exception {
					// TODO Auto-generated method stub
					return null;
				}
			});
		}
	}
	
	protected void processInsert(Object[] row) {
		
	}
	
	protected void processUpdate(Object[] row) {
		
	}

	
}
