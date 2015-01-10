/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2013, Helios Development Group and individual contributors
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
package org.helios.tsdb.plugins.meta;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import net.opentsdb.core.TSDB;
import net.opentsdb.meta.TSMeta;
import net.opentsdb.meta.UIDMeta;
import net.opentsdb.uid.UniqueId.UniqueIdType;
import net.opentsdb.utils.JSON;

import org.hbase.async.KeyValue;
import org.hbase.async.Scanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Title: MetaSynchronizer</p>
 * <p>Description: A service to synchronize meta objects from the OpenTSDB store to the search plugin instance.
 * Based broadly on <b><code><b></code> but only flushes located meta instances and does not fix or rewrite anything.</p> 
 * <p>Runs asynchronously in one thread so as to not overload the search plugin with what may amount to millions of metas,
 * but the full range of metric IDs is broken up into 10 segments to minimize the size of collections used to track keys that have been processed.</p>
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.meta.MetaSynchronizer</code></p>
 */

public class MetaSynchronizer {
	/** The instance logger */
	protected static final Logger log = LoggerFactory.getLogger(MetaSynchronizer.class);
	/** The TSDB instance to synchronize against */
	protected final TSDB tsdb;
    /** The start time */
    protected long start_time = -1;
    /** The thread pool for running meta-syncs */
    protected volatile ThreadPoolExecutor threadPool = null;
	protected final ThreadGroup metaSyncThreadGroup = new ThreadGroup("MetaSyncThreadGroup");
	/** Charset used to convert Strings to byte arrays and back. */
	public static final Charset CHARSET = Charset.forName("ISO-8859-1");
	
	/** The number of segments to divide the work up into */
	public static final int SEGMENT_COUNT = 10;
	
	/** The Charset to be used by the Scanner */
	public static final Charset SCANNER_CHARSET = Charset.forName("ISO-8859-1");
	
	  
	
	
//	/** A cache of already seen UIDs */
//	protected final Set<String> seenUids = new HashSet<String>();
//	/** A cache of already seen TSUIDs */
//	protected final Set<String> seenTSUids = new HashSet<String>();
	
	/**
	 * Creates a new MetaSynchronizer
	 * @param tsdb The TSDB instance to synchronize against
	 */
	public MetaSynchronizer(TSDB tsdb) {
		this.tsdb = tsdb;
	}
	
	/**
	 * Returns the number of TSMetas in the HBase store
	 * @return the number of TSMetas in the HBase store
	 */
	public int getTSMetaCount() {
		Scanner scanner = null;		
		try {			
			scanner = tsdb.getClient().newScanner(tsdb.metaTable());
		    scanner.setMaxNumRows(1024);
		    scanner.setFamily("name".getBytes(CHARSET));
		    int counter = 0;
			while(true) {
				ArrayList<ArrayList<KeyValue>> arr = scanner.nextRows().joinUninterruptibly(1000);
				if(arr==null) break;
				for(ArrayList<KeyValue> outerArr: arr) {
					for(KeyValue kv: outerArr) {
						if("ts_meta".equals(fromBytes(kv.qualifier()))) { 
							counter++;
						}						
					}
				}
			}
			return counter;
		} catch (Exception ex) {
			log.error("Failed to get TSMetaCount", ex);
			throw new RuntimeException(ex);
		} finally {
			if(scanner!=null) try { scanner.close(); } catch (Exception x) {}
		}		
	}

	/**
	 * Initializes the TSMeta resolver thread pool
	 */
	protected void initThreadPool() {
		if(threadPool==null) {
			synchronized(this) {
				if(threadPool==null) {
					threadPool = new ThreadPoolExecutor(0, Runtime.getRuntime().availableProcessors()*2, 10, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(1000, false), new ThreadFactory(){
						final AtomicLong serial = new AtomicLong();
						public Thread newThread(Runnable r) {
							Thread t = new Thread(r, "MetaSyncThread#" + serial.incrementAndGet());
							t.setDaemon(true);
							return t;
						}
					});
					threadPool.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
					threadPool.prestartAllCoreThreads();					
				}
			}
		}
	}
	
	private String fromBytes(final byte[] bytes) {
		return new String(bytes, CHARSET);
	}
	
	/**
	 * Runs the meta synchronization
	 * @param seenTSUids A set of the TSUIDs already in the SQL Catalog Database
	 * @param scanTimeOutMs The scanner timeout in ms.
	 * @param getTSMetaTimeOutMs The get TSMeta timeout in ms.
	 * @return the number of TSMeta objects processed
	 */
	public long metasync(final Set<String> seenTSUids, final long scanTimeOutMs, final long getTSMetaTimeOutMs) {		
		final AtomicLong tsMetaCount = new AtomicLong(0);
		final AtomicLong totalTSMetaFetchTime = new AtomicLong(0);
		final AtomicInteger taskCounter = new AtomicInteger(0);
		final AtomicInteger taskErrors = new AtomicInteger(0);
		final long start = System.currentTimeMillis();
		Scanner scanner = null;
		if(getTSMetaCount()<1) return 0;
		initThreadPool();
		long startScan = -1L;
		try {
		    scanner = tsdb.getClient().newScanner(tsdb.metaTable());
		    scanner.setMaxNumRows(1024);
		    scanner.setFamily("name".getBytes(CHARSET));
		    startScan = System.currentTimeMillis();
		    for(ArrayList<KeyValue> arr : scanner.nextRows().joinUninterruptibly(scanTimeOutMs)) {
		    	for(KeyValue kv: arr) {
		    		if("ts_meta".equals(fromBytes(kv.qualifier()))) { 
		    			final TSMeta shallowTSMeta = JSON.parseToObject(kv.value(), TSMeta.class);
		    			if(seenTSUids.add(shallowTSMeta.getTSUID())) {		    				
		    				threadPool.execute(new Runnable(){
		    					public void run() {
		    						try {
		    							final long startTime = System.currentTimeMillis();
										final TSMeta tsMeta = TSMeta.getTSMeta(tsdb, shallowTSMeta.getTSUID()).joinUninterruptibly(getTSMetaTimeOutMs);
										totalTSMetaFetchTime.addAndGet(System.currentTimeMillis()-startTime);
			                            tsdb.indexTSMeta(tsMeta);
			                            tsMetaCount.incrementAndGet();
									} catch (Exception e) {
										taskErrors.incrementAndGet();										
									} finally {
										taskCounter.decrementAndGet();
									}
		    					}
		    				});
		    				taskCounter.incrementAndGet();
		    			}
		    		}
		    	}
		    }
		    final long scanElapsed = System.currentTimeMillis() - startScan; 
		    int waitLoops = 0;
		    while(taskCounter.get()>0) {		    	
		    	try { Thread.currentThread().join(100); } catch (Exception x) {/* No Op */}
		    	waitLoops++;
		    	if(waitLoops%10==0) {
		    		log.info(" ---- Sync from Store waiting on {} tasks", taskCounter);
		    	}
		    }
		    long elapsed = System.currentTimeMillis()-start;
		    log.info("\n\tSync from Store Completed In [{}] ms.\n\tTSMetas: {}\n\tSeen: {}\n\tErrors: {}\n\tTotal Scan Time: {} ms\n\tTotal TSMeta Fetch Time: {} ms", elapsed, tsMetaCount.get(), seenTSUids.size(), taskErrors.get(), scanElapsed, totalTSMetaFetchTime);
		    return tsMetaCount.get();		    
		} catch (Exception ex) {
			log.error("Sync from Store Failed", ex);
			throw new RuntimeException(ex);
		} finally {
			if(scanner!=null) try { scanner.close(); } catch (Exception x) {/* No Op */}
			threadPool.shutdownNow();
		}
		
	}
	
	
}
