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

import java.lang.management.ManagementFactory;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
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
import net.opentsdb.uid.UniqueId.UniqueIdType;

import org.hbase.async.Bytes;
import org.hbase.async.GetRequest;
import org.hbase.async.HBaseClient;
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
	
	  /** The start row to scan on empty search strings.  `!' = first ASCII char. */
	  private static final byte[] START_ROW = new byte[] { '!' };

	  /** The end row to scan on empty search strings.  `~' = last ASCII char. */
	  private static final byte[] END_ROW = new byte[] { '~' };
	  
	  /** The single column family used by this class. */
	  private static final byte[] ID_FAMILY = toBytes("id");
	  /** The single column family used by this class. */
	  private static final byte[] NAME_FAMILY = toBytes("name");
	  /** Row key of the special row used to track the max ID already assigned. */
	  private static final byte[] MAXID_ROW = { 0 };
	  /** How many time do we try to assign an ID before giving up. */
	  private static final short MAX_ATTEMPTS_ASSIGN_ID = 3;
	  /** How many time do we try to apply an edit before giving up. */
	  private static final short MAX_ATTEMPTS_PUT = 6;
	  /** Initial delay in ms for exponential backoff to retry failed RPCs. */
	  private static final short INITIAL_EXP_BACKOFF_DELAY = 800;
	  /** Maximum number of results to return in suggest(). */
	  private static final short MAX_SUGGESTIONS = 25;
	  
	
	
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
		if(max_id < 1L) return 0L;
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
	
	 private static byte[] toBytes(final String s) {
		    return s.getBytes(CHARSET);
		  }

		  private static String fromBytes(final byte[] b) {
		    return new String(b, CHARSET);
		  }
	
	
	  /**
	   * Creates a scanner that scans the right range of rows for suggestions.
	   * @param client The HBase client to use.
	   * @param tsd_uid_table Table where IDs are stored.
	   * @param search The string to start searching at
	   * @param kind_or_null The kind of UID to search or null for any kinds.
	   * @param max_results The max number of results to return
	   */
	  private static Scanner getSuggestScanner(final HBaseClient client,
	      final byte[] tsd_uid_table, final String search,
	      final byte[] kind_or_null, final int max_results) {
	    final byte[] start_row;
	    final byte[] end_row;
	    if (search.isEmpty()) {
	      start_row = START_ROW;
	      end_row = END_ROW;
	    } else {
	      start_row = toBytes(search);
	      end_row = Arrays.copyOf(start_row, start_row.length);
	      end_row[start_row.length - 1]++;
	    }
	    final Scanner scanner = client.newScanner(tsd_uid_table);
	    scanner.setStartKey(start_row);
	    scanner.setStopKey(end_row);
	    scanner.setFamily(ID_FAMILY);
	    if (kind_or_null != null) {
	      scanner.setQualifier(kind_or_null);
	    }
	    scanner.setMaxNumRows(max_results <= 4096 ? max_results : 4096);
	    return scanner;
	  }	
	
	public static long synchronize(final TSDB tsdb) {
	    Scanner scanner = null;
	    try {
	      long num_rows = 0;
	      scanner = getSuggestScanner(tsdb.getClient(), tsdb.uidTable(), "", null, 
	          Integer.MAX_VALUE);
	      for (ArrayList<ArrayList<KeyValue>> rows = scanner.nextRows().join();
	          rows != null;
	          rows = scanner.nextRows().join()) {
	        for (final ArrayList<KeyValue> row : rows) {
	          for (KeyValue kv: row) {
	            final String name = fromBytes(kv.key());
	            final byte[] kind = kv.qualifier();
	            final byte[] id = kv.value();
	            final String kindName = fromBytes(kind);
	            if(!"metrics".equals(kindName)) continue;
	            String tsuid = UniqueId.uidToString(id);
	            System.out.println("Processing Name [" + tsuid + "]");
	          }
	          num_rows += row.size();
	          row.clear();  // free()
	        }
	      }
	      return num_rows;
	    } catch (Exception ex) {
	    	ex.printStackTrace(System.err);
	    	throw new RuntimeException(ex);
	    }
	}
	
	// org.helios.tsdb.plugins.meta.MetaSynchronizer.synchronize(tsdb);
	
	public static long synchronizex(final TSDB tsdb) {
		log.info("\n\tSTARTING METASYNC\n");
		ThreadPoolExecutor tpe = null;
		final AtomicLong tsMetaCounter = new AtomicLong(0L);
		final AtomicLong errorCounter = new AtomicLong(0L);
		final Set<Scanner> scanners = new HashSet<Scanner>();
		try {
			
			final AtomicInteger taskCount = new AtomicInteger(0);
			final Map<String, UIDMeta> UIDMETACACHE = new ConcurrentHashMap<String, UIDMeta>(2048);
			final ThreadGroup threadGroup = new ThreadGroup("MetaSyncThreadGroup");
			final ThreadFactory threadFactory = new ThreadFactory() {
				final AtomicInteger serial = new AtomicInteger(0);
				@Override
				public Thread newThread(Runnable r) {
					Thread t = new Thread(threadGroup, r, "MetaSyncThread#" + serial.incrementAndGet());
					t.setDaemon(true);
					return t;
				}
			};
			final ArrayBlockingQueue<Runnable> queue = new ArrayBlockingQueue<Runnable>(2048, false);
			final int tcount = ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors() * 2;
			tpe = new ThreadPoolExecutor(tcount, tcount, 60000, TimeUnit.MILLISECONDS, queue, threadFactory, new ThreadPoolExecutor.CallerRunsPolicy());
			tpe.prestartAllCoreThreads();
			log.info("MetaSync ThreadPool Started");
			byte[] startKey = null;
			Scanner scanner = null;
			long totalCount = 0;
			long loopCount = 0;
			final int loopSize = 1024;
			final Set<Future<?>> tasks = new HashSet<Future<?>>(1024);
			
			while(true) {
				try {
					scanner = tsdb.getClient().newScanner(tsdb.metaTable());
					//scanners.add(scanner);
					scanner.setMaxNumRows(loopSize);
					scanner.setFamily("name".getBytes(CHARSET));
					final boolean keyWasNull = startKey==null; 
					if(keyWasNull) {
						if(loopCount>0) break;						
					} else {
						scanner.setStartKey(startKey);
					}
					final Scanner closeScanner = scanner;
					ArrayList<ArrayList<KeyValue>> rows = scanner.nextRows(loopSize).joinUninterruptibly(10000);
					if(!keyWasNull) rows.remove(0);
					final int rowCount = getRowCount(rows);
					totalCount += rowCount;
					if(rows==null) break;
					
					for(final ArrayList<KeyValue> rowSet: rows) {
						tasks.add(tpe.submit(new Runnable(){
							public void run() {
								try {
									boolean fullStack = false;
									for(KeyValue kv: rowSet) {
										final int klength = kv.key().length;
										final int vlength = kv.value().length;
										try {
											if(vlength!=8) continue;
											final byte[] tsuid = UniqueId.getTSUIDFromKey(kv.key(), TSDB.metrics_width(), Const.TIMESTAMP_BYTES);
											String tsuid_string = UniqueId.uidToString(tsuid);
											TSMeta t = TSMeta.getTSMeta(tsdb, tsuid_string).joinUninterruptibly(5000);
//											TSMeta t = TSMeta.parseFromColumn(tsdb, kv, true).joinUninterruptibly(5000);
											if(t==null) {
												log.error("Null TSMeta for tsuid: {}", tsuid_string);
												errorCounter.incrementAndGet();
												continue;
											}
											tsdb.indexUIDMeta(t.getMetric());
											for(UIDMeta u: t.getTags()) {
												tsdb.indexUIDMeta(u);
											}
											tsdb.indexTSMeta(t);											
											tsMetaCounter.incrementAndGet();
										} catch (Exception ex) {
											if(fullStack) {
												log.error("TSMeta Fetch Error k:[{}] : v:[{}] ---> {}", klength, vlength, ex.toString());
											} else {
												log.error("TSMeta Fetch Error k:[{}] : v:[{}]", klength, vlength, ex);
												fullStack = true;
											}
										}
									}
								} finally {
									try { closeScanner.close(); } catch (Exception ex) {}
								}
							}
						}));
					}
					if(rowCount < (keyWasNull ? loopCount : loopCount-1)) {
						log.info("\n\t=========\n\tRowCount was [{}], so breaking\n\t=========\n", rowCount);
						break;
					}
					loopCount++;
					if(rows.isEmpty()) break;
					ArrayList<KeyValue> lastRowSet = rows.get(rows.size()-1);
					if(lastRowSet.isEmpty()) break;
					KeyValue kv = lastRowSet.get(lastRowSet.size()-1);
					startKey = kv.key();
					log.info("Row Count: {}, Error Count: {}, TSMeta Count: {}, Last Key: {}", rowCount, errorCounter, totalCount, Arrays.toString(startKey));
				} catch (Exception ex) {
					log.error("Unexpected exception in MetaSync", ex);
					throw new RuntimeException("Unexpected exception in MetaSync", ex);
				} finally {
					//if(scanner!=null) try { scanner.close(); } catch (Exception x) {/* No Op */}
				}
			}
			try {
				tpe.shutdown();
				boolean finished = tpe.awaitTermination(120, TimeUnit.MINUTES);
				log.info("TPE Termination Status:" + finished);
			} catch (Exception ex) {
				log.error("Failure waiting on TPE Completion", ex);
			}
			return tsMetaCounter.get();
		} finally {
			for(Scanner scanner: scanners) {
				try { scanner.close(); } catch (Exception x) {/* No Op */}
			}
			scanners.clear();
			if(tpe!=null) {
				tpe.shutdownNow();
			}
			log.info("FINAL: TSMetaCount:" + tsMetaCounter.get());
		}
	}
	
	public static int getRowCount(ArrayList<ArrayList<KeyValue>> rows) {
		int cnt = 0;
		if(rows!=null) {
			for(final ArrayList<KeyValue> rowSet: rows) {
				cnt += rowSet.size();
			}
		}
		return cnt;
	}
	
	
	
	// org.helios.tsdb.plugins.meta.MetaSynchronizer.getMetricIds(tsdb);
	
	public static Set<String> getMetricIds(final TSDB tsdb) {
		final Set<String> metricIds = new LinkedHashSet<String>(1024);
//		final int metricCount = (int)getMaxMetricID(tsdb)+1;
//		log.info("Fetching Metric IDs ---> {}", metricCount);
//		
//		Scanner scanner = tsdb.getClient().newScanner(tsdb.uidTable());
//		scanner.setMaxNumRows(metricCount);
//		scanner.setFamily("id".getBytes(CHARSET));
//		scanner.setQualifier("metrics".getBytes(CHARSET));
//		try {
//			ArrayList<ArrayList<KeyValue>> rows = scanner.nextRows(metricCount).joinUninterruptibly(10000);
//			int rowCount = 0;
//			for(ArrayList<KeyValue> row: rows) {
//				rowCount += row.size();
//			}
//			log.info("Scanner Rows: [{}]", rowCount);
//			for(ArrayList<KeyValue> row: rows) {
//				for(KeyValue kv: row) {
//					if(kv.value().length!=3) continue;
//					try {						
//						metricIds.add(tsdb.getUidName(UniqueIdType.METRIC, kv.value()).joinUninterruptibly(200));
//					} catch (Exception ex) {}
//				}
//			}
//
//		} catch (Exception e1) {
//			log.info("Failed to get Scanner Rows:", e1);
//		}
		//final byte[] FAMILY = { 't' };
		//scanner.setStartKey(start_key);
		
		byte[] startKey = null;
		Scanner scanner = null;
		long totalCount = 0;
		long loopCount = 0;
		
		try {
			do {
				scanner = tsdb.getClient().newScanner(tsdb.metaTable());
				scanner.setMaxNumRows(1024);
				scanner.setFamily("name".getBytes(CHARSET));
				
				if(startKey!=null) {
					scanner.setStartKey(startKey);
				} else {
					if(loopCount!=0) break;
				}
				loopCount++;
				ArrayList<ArrayList<KeyValue>> rows = scanner.nextRows().joinUninterruptibly(10000);
				if(rows==null) break;
				log.info("Loaded RowSet");
				for(ArrayList<KeyValue> row: rows) {
					for(KeyValue kv: row) {
						startKey =  kv.key();
						
						try {
							TSMeta t = TSMeta.parseFromColumn(tsdb, kv, false).joinUninterruptibly(300);
							totalCount++;
//							StringBuilder b = new StringBuilder(t.getMetric().getName()).append(":");
//							for(UIDMeta u: t.getTags()) {
//								if(u.getType()==UniqueIdType.TAGK) b.append(u.getName()).append("=");
//								else b.append(u.getName()).append(",");
//							}
//							b.deleteCharAt(b.length()-1);
//							log.info("TSMeta: [{}]", b.toString());							
						} catch (Exception ex) {}
					}
				}
				scanner.close();
				scanner = null;
			} while(startKey!=null);
			log.info("Total TSMetas: {}", totalCount);

	} catch (Exception e1) {
		log.info("Failed to get Scanner Rows:", e1);
	} finally {
		if(scanner!=null) try { scanner.close(); } catch (Exception x) {}
	}
		
		
		
		
		return metricIds;
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
				log.warn("No data in the metric max UID cell. Empty DB ?");
				return -1L;
//				throw new IllegalStateException("No data in the metric max UID cell");
			}
			log.info("Processing [{}] Rows", row.size());
			final byte[] id_bytes = row.get(0).value();
			if (id_bytes.length != 8) {
				log.error("Invalid metric max UID, wrong # of bytes");
				throw new IllegalStateException("Invalid metric max UID, wrong # of bytes");
			}
			try { log.info("Retrieved [{}]", tsdb.getUidName(UniqueIdType.METRIC, id_bytes).join(200)); } catch (Exception ex) {}
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
