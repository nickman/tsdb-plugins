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

import net.opentsdb.core.Const;
import net.opentsdb.core.TSDB;
import net.opentsdb.meta.TSMeta;
import net.opentsdb.uid.UniqueId;

import org.hbase.async.Bytes;
import org.hbase.async.GetRequest;
import org.hbase.async.KeyValue;
import org.hbase.async.Scanner;
import org.helios.tsdb.plugins.util.SystemClock;
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
	protected final Logger log = LoggerFactory.getLogger(getClass());
	/** The TSDB instance to synchronize against */
	protected final TSDB tsdb;
    /** The start time */
    protected long start_time = -1;
    /** The max metric ID from the UID table */
    protected final long max_id;
	
	/** Charset used to convert Strings to byte arrays and back. */
	public static final Charset CHARSET = Charset.forName("ISO-8859-1");
	
	/** The number of segments to divide the work up into */
	public static final int SEGMENT_COUNT = 10;
	
	/** A cache of already seen UIDs */
	protected final Set<String> seenUids = new HashSet<String>();
	/** A cache of already seen TSUIDs */
	protected final Set<String> seenTSUids = new HashSet<String>();
	
	/**
	 * Creates a new MetaSynchronizer
	 * @param tsdb The TSDB instance to synchronize against
	 */
	public MetaSynchronizer(TSDB tsdb) {
		this.tsdb = tsdb;
		max_id = getMaxMetricID(tsdb);
		
	}
	
	
	/**
	 * Runs the meta synchronization
	 * @return the number of TSMeta objects processed
	 */
	public long process() {
		start_time = SystemClock.unixTime();
		long tsMetaCount = 0;
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
		short metric_width = TSDB.metrics_width();
	    log.info("MetaSync Segments: [{}]", Arrays.deepToString(segments) );
	    Scanner scanner = null;
	    for(int i = 0; i < SEGMENT_COUNT; i++) {
	    	if(segments[i][0]<0 || segments[i][1]<0) continue;
	    	try {
	    		scanner = tsdb.getClient().newScanner(tsdb.dataTable());
	    		byte[] start_row =  Arrays.copyOfRange(Bytes.fromLong(segments[i][0]), 8 - metric_width, 8);
	    		byte[] end_row =    Arrays.copyOfRange(Bytes.fromLong(segments[i][1]), 8 - metric_width, 8);
	    		scanner.setStartKey(start_row);
	    		scanner.setStopKey(end_row);
	    		scanner.setFamily("t".getBytes(Charset.forName("ISO-8859-1")));
	    		ArrayList<ArrayList<KeyValue>> scanResult = scanner.nextRows().joinUninterruptibly(15000);
	    		if(scanResult==null) {
	    			log.warn("ScanResult for segment [{}] for segments between [{}] and [{}] was null", i, segments[i][0], segments[i][1]);
	    			continue;
	    		}
	    		int size = scanResult.size();
	    		log.info("Processing Segment [{}] with [{}] KeyValues", i, size);
	    		for(ArrayList<KeyValue> kv: scanResult) {
	    		    byte[] tsuid = UniqueId.getTSUIDFromKey(kv.get(0).key(), TSDB.metrics_width(), Const.TIMESTAMP_BYTES);    
	    		    String tsuid_string = UniqueId.uidToString(tsuid);
	    		    if(seenTSUids.add(tsuid_string)) {
	    		    	TSMeta tsMeta = TSMeta.getTSMeta(tsdb, tsuid_string).joinUninterruptibly(1000);
	    		    	tsdb.indexTSMeta(tsMeta);
	    		    	tsMetaCount++;
	    		    }
	    		}
	    		scanner.close();
	    		scanner = null;
	    		log.info("Completed Segment [{}] with [{}] KeyValues", i, size);
	    	} catch (Exception e) {
				log.error("Failed to process scan", e);
				break;
			} finally {
	    		seenUids.clear();
	    		seenTSUids.clear();
	    		if(scanner!=null) try { scanner.close(); } catch (Exception x) {/* No Op */}
	    	}
	    }
	    long elapsed = SystemClock.unixTime()-start_time;
	    log.info("Reindexed [{}] TSMetas in [{}] seconds", tsMetaCount , elapsed);
	    return tsMetaCount;
	}
	
	
	  /**
	   * Returns the max metric ID from the UID table
	   * @param tsdb The TSDB to use for data access
	   * @return The max metric ID as an integer value
	   */
	  public static long getMaxMetricID(final TSDB tsdb) {
	    // first up, we need the max metric ID so we can split up the data table
	    // amongst threads.
	    final GetRequest get = new GetRequest(tsdb.uidTable(), new byte[] { 0 });
	    get.family("id".getBytes(CHARSET));
	    get.qualifier("metrics".getBytes(CHARSET));
	    ArrayList<KeyValue> row;
	    try {
	      row = tsdb.getClient().get(get).joinUninterruptibly();
	      if (row == null || row.isEmpty()) {
	        throw new IllegalStateException("No data in the metric max UID cell");
	      }
	      final byte[] id_bytes = row.get(0).value();
	      if (id_bytes.length != 8) {
	        throw new IllegalStateException("Invalid metric max UID, wrong # of bytes");
	      }
	      return Bytes.getLong(id_bytes);
	    } catch (Exception e) {
	      throw new RuntimeException("Shouldn't be here", e);
	    }
	  }	
	
	
}
