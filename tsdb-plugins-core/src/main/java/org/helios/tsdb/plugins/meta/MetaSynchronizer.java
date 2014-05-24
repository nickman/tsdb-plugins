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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import net.opentsdb.core.Const;
import net.opentsdb.core.TSDB;
import net.opentsdb.meta.TSMeta;
import net.opentsdb.meta.UIDMeta;
import net.opentsdb.uid.UniqueId;

import org.hbase.async.Bytes;
import org.hbase.async.GetRequest;
import org.hbase.async.KeyValue;
import org.hbase.async.Scanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stumbleupon.async.Callback;

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
		final Set<String> seenids = new HashSet<String>();
		try {			
			scanner = tsdb.getClient().newScanner(tsdb.dataTable());
			while(true) {
				ArrayList<ArrayList<KeyValue>> arr = scanner.nextRows().joinUninterruptibly(1000);
				if(arr==null) break;
				for(ArrayList<KeyValue> kv: arr) {
	    		    byte[] tsuid = UniqueId.getTSUIDFromKey(kv.get(0).key(), TSDB.metrics_width(), Const.TIMESTAMP_BYTES);    
	    		    String tsuid_string = UniqueId.uidToString(tsuid);
	    		    seenids.add(tsuid_string);
				}
			}
			return seenids.size();
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		} finally {
			if(scanner!=null) try { scanner.close(); } catch (Exception x) {}
		}		
	}

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
					for(int x = 0; x < Runtime.getRuntime().availableProcessors(); x++) {
						threadPool.prestartCoreThread();
					}
				}
			}
		}
	}
	
	/**
	 * Force synchronizes the HBase metas to the sql catalog
	 * @param multiThread true to run in multiple threads
	 * @return the number of TSMetas synced
	 */
	public synchronized long processX(final boolean multiThread) {
		Scanner scanner = null;
		final Set<String> seenids = new HashSet<String>();
		final Set<String> seenuids = new HashSet<String>();
		try {
			initThreadPool();
			final AtomicBoolean hasExceptions = new AtomicBoolean(false);
			threadPool.setRejectedExecutionHandler(new RejectedExecutionHandler() {
				public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) { hasExceptions.set(true);}
			});
			
			scanner = tsdb.getClient().newScanner(tsdb.dataTable());
			final AtomicInteger concurrent = new AtomicInteger(0);
			while(true) {
				final ArrayList<ArrayList<KeyValue>> arr = scanner.nextRows().joinUninterruptibly(1000);
				if(arr==null) break;
				threadPool.execute(new Runnable() {
					public void run() {
						try {
							int c = concurrent.incrementAndGet();
							log.info("MetaSync Thread Concurrency:" + c);
							for(ArrayList<KeyValue> kv: arr) {
				    		    byte[] tsuid = UniqueId.getTSUIDFromKey(kv.get(0).key(), TSDB.metrics_width(), Const.TIMESTAMP_BYTES);    
				    		    String tsuid_string = UniqueId.uidToString(tsuid);
				    		    if(seenids.add(tsuid_string)) {
				    		    	TSMeta tsMeta = TSMeta.getTSMeta(tsdb, tsuid_string).joinUninterruptibly(1000);
				    		    	if(tsMeta!=null) {
					    		    	for(UIDMeta uidMeta: tsMeta.getTags()) {
					    		    		if(seenuids.add(uidMeta.toString())) {
					    		    			tsdb.indexUIDMeta(uidMeta);
					    		    		}
					    		    	}
					    		    	UIDMeta uidMeta = tsMeta.getMetric();
				    		    		if(seenuids.add(uidMeta.toString())) {
				    		    			tsdb.indexUIDMeta(uidMeta);
				    		    		}
					    		    	tsdb.indexTSMeta(tsMeta);
				    		    	}
				    		    }			    		    
							}
						} catch (Exception ex) {
							log.error("MetaSync Error", ex);
							hasExceptions.set(true);
						} finally {
							concurrent.decrementAndGet();
						}
					}
				});
			}
			if(threadPool.awaitTermination(5 , TimeUnit.MINUTES)) {
				return seenids.size();
			} else {
				threadPool.shutdownNow();
				throw new RuntimeException("MetaSync timed out");
			}						
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		} finally {
			if(scanner!=null) try { scanner.close(); } catch (Exception x) {}
		}

	}
	
	
	/**
	 * Runs the meta synchronization
	 * @return the number of TSMeta objects processed
	 */
	public long metasync() {
		final Set<String> seenTSUids = new HashSet<String>();
		final Set<String> seenUids = new HashSet<String>();

		long max_id = getMaxMetricID(tsdb);
		if(max_id==0) return 0L;
		start_time = System.currentTimeMillis();
		final AtomicLong tsMetaCount = new AtomicLong(0);
		final long[][] segments = new long[SEGMENT_COUNT][2];
		for(int i = 0; i < SEGMENT_COUNT; i++) {
			segments[i][0] = -1L;
			segments[i][1] = -1L;
		}
		int segmentSize = -1;
		log.info("MAX ID:{}", max_id);
		if(max_id < 10) {
			segmentSize = 1; 
		} else {
			segmentSize = (int)(max_id/SEGMENT_COUNT);
		}
		long start = 0, end = segmentSize;
		for(int i = 0; i < SEGMENT_COUNT; i++) {
			segments[i][0] = start;
			segments[i][1] = end;
			if(i==SEGMENT_COUNT-1) {
				segments[i][0]--;
			}
			
			if(end>=max_id) break;
			
			start += segmentSize + 1;
			end = start + segmentSize;		
			if(end>max_id) {
				end = max_id;
			}
		}
		initThreadPool();
		final short metric_width = TSDB.metrics_width();
	    log.info("MetaSync Segments: [{}]", Arrays.deepToString(segments) );
	    final CountDownLatch latch = new CountDownLatch(SEGMENT_COUNT);
	    for(int x = 0; x < SEGMENT_COUNT; x++) {
	    	if(segments[x][0]<0 || segments[x][1]<0) {
	    		latch.countDown();
	    		continue;
	    	}
	    	final int i = x;
	    	threadPool.execute(new Runnable(){
	    		public void run() {
	    	    	Scanner scanner = null;
	    	    	try {	    		
	    	    		scanner = tsdb.getClient().newScanner(tsdb.dataTable());
	    	    		byte[] start_row =  Arrays.copyOfRange(Bytes.fromLong(segments[i][0]), 8 - metric_width, 8);
	    	    		byte[] end_row =    Arrays.copyOfRange(Bytes.fromLong(segments[i][1]), 8 - metric_width, 8);
	    	    		//end_row[2]++;
	    	    		scanner.setStartKey(start_row);
	    	    		scanner.setStopKey(end_row);
	    	    		scanner.setFamily("t".getBytes(SCANNER_CHARSET));
	    	    		ArrayList<ArrayList<KeyValue>> scanResult = scanner.nextRows().joinUninterruptibly(15000);
	    	    		if(scanResult==null) {
	    	    			log.warn("ScanResult for segment [{}] for segments between [{}] and [{}] was null", i, segments[i][0], segments[i][1]);
	    	    			return;
	    	    		}
	    	    		int size = scanResult.size();
	    	    		log.info("Processing Segment [{}] with [{}] KeyValues", i, size);
	    	    		for(ArrayList<KeyValue> kv: scanResult) {
	    	    		    byte[] tsuid = UniqueId.getTSUIDFromKey(kv.get(0).key(), TSDB.metrics_width(), Const.TIMESTAMP_BYTES);    
	    	    		    String tsuid_string = UniqueId.uidToString(tsuid);
	    	    		    if(seenTSUids.add(tsuid_string)) {
	    	    		    	TSMeta.getTSMeta(tsdb, tsuid_string).addCallback(new Callback<Void, TSMeta>(){
	    	    		    		@Override
	    	    		    		public Void call(TSMeta tsMeta) throws Exception {
	    	    		    			if(tsMeta!=null) {
	    		    	    		    	for(UIDMeta uidMeta: tsMeta.getTags()) {
	    		    	    		    		if(seenUids.add(uidMeta.toString())) {
	    		    	    		    			tsdb.indexUIDMeta(uidMeta);
	    		    	    		    		}
	    		    	    		    	}
	    		    	    		    	UIDMeta metric = tsMeta.getMetric();
	    		    	    		    	if(seenUids.add(metric.toString())) {
	    		    	    		    		tsdb.indexUIDMeta(metric);
	    		    	    		    	}
	    		    	    		    	
	    		    	    		    	tsdb.indexTSMeta(tsMeta);
	    		    	    		    	tsMetaCount.incrementAndGet();	    	    		    				
	    	    		    			}
	    	    		    			return null;
	    	    		    		}
	    	    		    	});	    		    	
	    	    		    }
	    	    		}
	    	    		scanner.close();
	    	    		scanner = null;
	    	    		log.info("Completed Segment [{}] with [{}] KeyValues", i, size);
	    	    	} catch (Exception e) {
	    				log.error("Failed to process scan", e);	    				
	    			} finally {
	    	    		seenUids.clear();
	    	    		seenTSUids.clear();
	    	    		if(scanner!=null) try { scanner.close(); } catch (Exception x) {/* No Op */}
	    	    		latch.countDown();
	    	    	}	    			
	    		}
	    	});
	    }
	    try {
	    	// FIXME: Make this configurable. Might need longer.
	    	if(!latch.await(5, TimeUnit.MINUTES)) {
	    		throw new RuntimeException("Sync timed out");
	    	}
	    } catch (Exception ex) {
	    	log.error("Sync Failed", ex);
	    	throw new RuntimeException("Sync Failed", ex);
	    }
	    long elapsed = System.currentTimeMillis()-start_time;
	    log.info("Reindexed [{}] TSMetas in [{}] seconds", tsMetaCount , elapsed);
	    return tsMetaCount.get();
	}
	
	void print(TSMeta tsMeta) {
		log.info(tsMeta.toString());
	}
	
	
	/**
	 * Returns the max metric ID from the UID table
	 * @param tsdb The TSDB to use for data access
	 * @return The max metric ID as an integer value
	 */
	public static long getMaxMetricID(final TSDB tsdb) {
		// first up, we need the max metric ID so we can split up the data table
		// amongst threads.
		log.info("Fetching MAX Metric ID");
		final GetRequest get = new GetRequest(tsdb.uidTable(), new byte[] { 0 });
		get.family("id".getBytes(CHARSET));
		get.qualifier("metrics".getBytes(CHARSET));
		ArrayList<KeyValue> row;
		try {
			row = tsdb.getClient().get(get).joinUninterruptibly(2000);
			if (row == null || row.isEmpty()) {
				log.error("No data in the metric max UID cell");
				throw new IllegalStateException("No data in the metric max UID cell");
			}
			final byte[] id_bytes = row.get(0).value();
			if (id_bytes.length != 8) {
				log.error("Invalid metric max UID, wrong # of bytes");
				throw new IllegalStateException("Invalid metric max UID, wrong # of bytes");
			}
			long maxId = Bytes.getLong(id_bytes);
			log.info("MAX ID: [{}]", maxId);
			return maxId;
		} catch (Throwable e) {
			log.error("Unexpected exception getting max metric id: [{}]", e.toString());
			//throw new RuntimeException("Shouldn't be here", e);
			return 0;
		}
	}	

	
}
