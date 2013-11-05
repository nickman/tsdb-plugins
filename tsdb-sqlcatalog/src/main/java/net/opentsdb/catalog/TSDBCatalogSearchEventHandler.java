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
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.sql.DataSource;

import net.opentsdb.core.TSDB;
import net.opentsdb.meta.UIDMeta;
import net.opentsdb.search.SearchQuery;

import org.helios.tsdb.plugins.event.TSDBEvent;
import org.helios.tsdb.plugins.event.TSDBEventType;
import org.helios.tsdb.plugins.event.TSDBSearchEvent;
import org.helios.tsdb.plugins.handlers.EmptySearchEventHandler;
import org.helios.tsdb.plugins.util.ConfigurationHelper;
import org.helios.tsdb.plugins.util.SystemClock;
import org.helios.tsdb.plugins.util.SystemClock.ElapsedTime;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import com.stumbleupon.async.Deferred;

/**
 * <p>Title: TSDBCatalogSearchEventHandler</p>
 * <p>Description: TSDB search event handler for populating a SQL based data dictionary of metrics.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>net.opentsdb.catalog.TSDBCatalogSearchEventHandler</code></p>
 */

public class TSDBCatalogSearchEventHandler extends EmptySearchEventHandler implements  Runnable {
	/** The singleton instance */
	protected static volatile TSDBCatalogSearchEventHandler instance = null;
	/** The singleton instance ctor lock */
	protected static final Object lock = new Object();

	
	/** Map of the relative priority ordering of events keyed by the event type enum */
	public static final Map<TSDBEventType, Integer> EVENT_ORDERING;
	/** The config property name for the JDBC DB Initializer Class */
	public static final String DB_JDBC_INITER = "helios.search.catalog.jdbc.pw";
	/** The default JDBC DB Initializer Class */
	public static final String DEFAULT_DB_JDBC_INITER = H2DBCatalog.class.getName();
	/** The config property name for the JDBC processing batch size */
	public static final String DB_JDBC_BATCH_SIZE = "helios.search.catalog.jdbc.batchsize";
	/** The default JDBC processing batch size */
	public static final int DEFAULT_DB_JDBC_BATCH_SIZE = 1024;
	/** The config property name for the event processing queue size */
	public static final String DB_PROC_QUEUE_SIZE = "helios.search.catalog.jdbc.queue.size";
	/** The default event processing queue size */
	public static final int DEFAULT_DB_PROC_QUEUE_SIZE = 2048;
	/** The config property name for the event processing shutdown timeout in ms. */
	public static final String DB_PROC_QUEUE_TIMEOUT = "helios.search.catalog.jdbc.queue.timeout";
	/** The default event processing shutdown timeout in ms. */
	public static final long DEFAULT_DB_PROC_QUEUE_TIMEOUT = 2000;
	
	/** The start latch */
	protected CountDownLatch latch = new CountDownLatch(1);		


	
	/** The configured batch size */
	protected int batchSize = 1024;
	/** The configured queue size */
	protected int queueSize = 1024;
	/** The time period allowed on shutdown to clear the processing queue in ms. */
	protected long timeout = 2000;
	
	/** The configured DB initer */
	protected CatalogDBInterface dbInterface = null;
	/** The processing queue for catalog processed events */
	protected PriorityBlockingQueue<TSDBSearchEvent> processingQueue;	
	/** The connection pool */
	protected DataSource dataSource = null;
	/** The queue processing thread */
	protected Thread queueProcessorThread = new Thread(this, "TSDBCatalogQueueProcessor");
	/** Indicates if we're shutting down */
	protected final AtomicBoolean shuttingDown = new AtomicBoolean(false);
	
	static {		
		Map<TSDBEventType, Integer> tmp = new EnumMap<TSDBEventType, Integer>(TSDBEventType.class);
		tmp.put(TSDBEventType.TSMETA_DELETE, 1);
		tmp.put(TSDBEventType.UIDMETA_DELETE, 2);
		tmp.put(TSDBEventType.UIDMETA_INDEX, 3);
		tmp.put(TSDBEventType.TSMETA_INDEX, 4);
		tmp.put(TSDBEventType.SEARCH, 5);		
		tmp.put(TSDBEventType.ANNOTATION_DELETE, 6);
		tmp.put(TSDBEventType.ANNOTATION_INDEX, 7);
		EVENT_ORDERING = Collections.unmodifiableMap(tmp);
	}
	
	/**
	 * Acquires the singleton instance
	 * @return the singleton instance
	 */
	public static TSDBCatalogSearchEventHandler getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new TSDBCatalogSearchEventHandler();
				}
			}
		}
		return instance;
	}
	
	
	/**
	 * Creates a new TSDBCatalogSearchEventHandler
	 */
	private TSDBCatalogSearchEventHandler() {
		super();
	}
	
	/**
	 * Waits the default time (5 seconds) for the event handler to complete initialization
	 */
	public static void waitForStart() {
		waitForStart(5, TimeUnit.SECONDS);
	}
	
	
	/**
	 * Waits for the event handler to complete initialization
	 * @param timeout The timeout period to wait for
	 * @param unit  The timeout unit
	 */
	public static void waitForStart(long timeout, TimeUnit unit) {
		try {
			if(!getInstance().latch.await(timeout, unit)) {
				throw new Exception("Did not start before timeout", new Throwable());
			}
		} catch (Exception ex) {
			throw new RuntimeException("Failed to wait for start", ex);
		}
	}	
	

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.handlers.EmptySearchEventHandler#initialize(net.opentsdb.core.TSDB, java.util.Properties)
	 */
	@Override
	public void initialize(TSDB tsdb, Properties extracted) {		
		super.initialize(tsdb, extracted);
		batchSize = ConfigurationHelper.getIntSystemThenEnvProperty(DB_JDBC_BATCH_SIZE, DEFAULT_DB_JDBC_BATCH_SIZE, extracted);
		queueSize = ConfigurationHelper.getIntSystemThenEnvProperty(DB_PROC_QUEUE_SIZE, DEFAULT_DB_PROC_QUEUE_SIZE, extracted);
		timeout = ConfigurationHelper.getLongSystemThenEnvProperty(DB_PROC_QUEUE_TIMEOUT, DEFAULT_DB_PROC_QUEUE_TIMEOUT, extracted);
		String initerClassName = ConfigurationHelper.getSystemThenEnvProperty(DB_JDBC_INITER, DEFAULT_DB_JDBC_INITER, extracted);
		processingQueue = new PriorityBlockingQueue<TSDBSearchEvent>(queueSize, new TSDBSearchEventComparator());
		dbInterface = loadDB(initerClassName);
		dataSource = dbInterface.getDataSource();
		log.info("Acquired DataSource");	
		queueProcessorThread.setDaemon(true);
		queueProcessorThread.start();
		latch.countDown();
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.handlers.EmptySearchEventHandler#shutdown()
	 */
	@Override
	public void shutdown() {
		shuttingDown.set(true);
		super.shutdown();
	}
	
	/**
	 * Loads and runs the Catalog DB Initializer
	 * @param initerClassName The class name of the Catalog DB Initializer
	 * @return The created and loaded CatalogDBInterface
	 */
	protected CatalogDBInterface loadDB(String initerClassName) {
		log.info("Loading Catalog DB Initializer [{}]", initerClassName);
		CatalogDBInterface idb = null;
		try {
			Class<CatalogDBInterface> clazz = (Class<CatalogDBInterface>)Class.forName(initerClassName);
			idb = clazz.newInstance();
			idb.initialize(tsdb, config);
			log.info("Catalog DB Initializer [{}] Created and Run", initerClassName);
			return idb;
		} catch (Exception ex) {
			throw new RuntimeException("Failed to load and run Catalog DB Initializer [" + initerClassName + "]", ex);
		}
		 
	}
	
	/**
	 * Returns the number of events pending in the processing queue
	 * @return the number of events pending in the processing queue
	 */
	public int getProcessingQueueDepth() {
		return processingQueue.size();
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.handlers.EmptySearchEventHandler#onEvent(org.helios.tsdb.plugins.event.TSDBSearchEvent)
	 */
	@Subscribe
	@AllowConcurrentEvents		
	@Override
	public void onEvent(TSDBSearchEvent event) throws Exception {
		this.onEvent(event, -1L, false);
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.handlers.EmptySearchEventHandler#onEvent(org.helios.tsdb.plugins.event.TSDBEvent, long, boolean)
	 */
	@Override
	public void onEvent(TSDBEvent event, long sequence, boolean endOfBatch) throws Exception {
		if(!EVENT_ORDERING.containsKey(event.eventType)) return;
		if(TSDBEventType.SEARCH==event.eventType) {
			if(!searchEnabled) return;
			executeQuery(event.searchQuery, event.deferred);
		} else {
			processingQueue.add(event.asSearchEvent());
		}
	}
	
	
	public void run() {
		log.info("Starting Catalog Processing Thread. Batch Size: [{}]", batchSize);
		Connection conn = null;
		while(!shuttingDown.get()) {
			try {
				conn = dataSource.getConnection();
				conn.setAutoCommit(false);
				while(true) {
					Set<TSDBSearchEvent> events = new LinkedHashSet<TSDBSearchEvent>(batchSize);
					events.add(processingQueue.take());
					final long ts = System.currentTimeMillis() + 2000;
					do {
						TSDBSearchEvent ev = processingQueue.poll(200, TimeUnit.MILLISECONDS);
						if(ev!=null) {
							events.add(ev);
						}						
					} while(events.size()<batchSize && ts>System.currentTimeMillis());
					log.info("Processing Batch of [{}] Events", events.size());
					dbInterface.processEvents(conn, events);
				}
			} catch (InterruptedException iex) {
				Thread.interrupted();
				if(shuttingDown.get()) {
					if(!processingQueue.isEmpty()) {
						// drain until empty or timeout elapsed
						final long timeoutEndPeriod = SystemClock.time() + timeout;
						do {
							Set<TSDBSearchEvent> events = new LinkedHashSet<TSDBSearchEvent>(batchSize);						
							processingQueue.drainTo(events, batchSize);
							dbInterface.processEvents(conn, events);						
						} while(!processingQueue.isEmpty() && SystemClock.time() < timeoutEndPeriod);
					}
				}
			} catch (Exception ex) {
				log.error("Processing Queue Error", ex);
			} finally {
				if(conn!=null) try { conn.close(); } catch (Exception ex) {/* No Op */}
			}
		}		
	}
	

	
   /**
     * Executes a search query and returns the deferred for the results
     * @param query The query to execute
     * @param result The deferred to write the query results into
     * @return the deferred results
     */
    public Deferred<SearchQuery> executeQuery(final SearchQuery query, final Deferred<SearchQuery> result) {
    	
    	return result;
    }
    
	/**
	 * <p>Title: TSDBSearchEventComparator</p>
	 * <p>Description: Comparator for {@link TSDBSearchEvent}s to enforce priority ordering in the event submission queue</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>net.opentsdb.catalog.TSDBCatalogSearchEventHandler.TSDBSearchEventComparator</code></p>
	 */
	public static class TSDBSearchEventComparator implements Comparator<TSDBSearchEvent> {
		/**
		 * {@inheritDoc}
		 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
		 */
		@Override
		public int compare(TSDBSearchEvent t1, TSDBSearchEvent t2) {
			int i1 = EVENT_ORDERING.get(t1.eventType);
			int i2 = EVENT_ORDERING.get(t2.eventType);
			return i1 < i2 ? -1 : 1;
		}
	}

	/**
	 * Returns the configured datasource for the catalog DB
	 * @return the catalog DB dataSource
	 */
	public DataSource getDataSource() {
		return dataSource;
	}
    
}
