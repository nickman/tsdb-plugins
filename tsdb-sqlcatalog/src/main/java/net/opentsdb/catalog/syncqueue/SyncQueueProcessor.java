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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sql.DataSource;

import net.opentsdb.core.TSDB;

import org.helios.tsdb.plugins.service.PluginContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.AbstractService;

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

	
	/**
	 * Creates a new SyncQueueProcessor
	 * @param pluginContext The plugin context to provide the processor's configuration
	 */
	public SyncQueueProcessor(PluginContext pluginContext) {
		this.pluginContext = pluginContext;
		tsdb = pluginContext.getTsdb();
		dataSource = pluginContext.getDataSource();
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
		try {
			conn = dataSource.getConnection();
			conn.setAutoCommit(false);
			pollPs = conn.prepareStatement("SELECT * FROM SYNC_QUEUE ORDER BY OP_TYPE");
			pollRset = pollPs.executeQuery();
			while(pollRset.next()) {
				String opType = pollRset.getString(4);
				if("D".equals(opType)) {
					 
				} else if("I".equals(opType)) {
					
				} else if("U".equals(opType)) {
					
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

}
