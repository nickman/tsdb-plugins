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
package org.helios.tsdb.plugins.remoting.json.services;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import org.helios.tsdb.plugins.util.SystemClock;
import org.helios.tsdb.plugins.util.unsafe.collections.ConcurrentLongSlidingWindow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * <p>Title: SystemStatusService</p>
 * <p>Description: Service to gather stats for system status</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.remoting.json.services.SystemStatusService</code></p>
 */

public class SystemStatusService {
	/** The thread MX bean */
	public static final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
	/** The memory MX bean */
	public static final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
	/** The GC MX beans */
	public static final Set<GarbageCollectorMXBean> gcMXBeans = Collections.unmodifiableSet(new HashSet<GarbageCollectorMXBean>(ManagementFactory.getGarbageCollectorMXBeans()));
	/** The OS MX Bean */
	public static final OperatingSystemMXBean osMXBean = ManagementFactory.getOperatingSystemMXBean();
	
	/** The shared json mapper */
	private static final ObjectMapper jsonMapper = new ObjectMapper();
	/** Static class logger */
	private static final Logger LOG = LoggerFactory.getLogger(SystemStatusService.class);
	
	/** The backlog cache */
	private static final Cache<String, ConcurrentLongSlidingWindow> backlog = CacheBuilder.newBuilder().build();
	
	/**
	 * Creates a new SystemStatusService
	 */
	private SystemStatusService() {
		
	}
	
	private static ConcurrentLongSlidingWindow getBacklog(String category, String id) {
		String key = String.format("%s/%s", category, id);
		try {
			return backlog.get(key, new Callable<ConcurrentLongSlidingWindow>(){
				@Override
				public ConcurrentLongSlidingWindow call() throws Exception {					
					return new ConcurrentLongSlidingWindow(15 * (60/15) * 2);
				}
			});
		} catch (Exception ee) {
			throw new RuntimeException("Failed to create backlog cache", ee);
		}
	}
	
	/**
	 * Collects system status metrics and returns them as a JSON Node
	 * @return the metrics as a JSON Node
	 */
	public static ObjectNode collect() {
		LOG.debug("Collecting System Status");
		ObjectNode stats = jsonMapper.createObjectNode();
		try {
			collectHeap("heap", stats);
			
			
			return stats;
		} catch (Exception ex) {
			LOG.error("Failed to collect system status", ex);
			throw new RuntimeException("Failed to collect system status", ex);
		}
	}
	
	/**
	 * Collects heap stats
	 * @return the heap stats node
	 */
	public static void collectHeap(final String cat, final ObjectNode stats) {
		ObjectNode heapStats = jsonMapper.createObjectNode();
		final long t = SystemClock.time();		
		stats.put(cat, heapStats);
		MemoryUsage heapUsage = memoryMXBean.getHeapMemoryUsage();
		record(t, heapUsage.getInit(), cat, "init", heapStats);
		record(t, heapUsage.getMax(), cat, "max", heapStats);
		record(t, heapUsage.getCommitted(), cat, "com", heapStats);
		record(t, heapUsage.getUsed(), cat, "used", heapStats);
		record(t, percent(heapUsage.getUsed(), heapUsage.getCommitted()), cat, "pused", heapStats);
		record(t, percent(heapUsage.getUsed(), heapUsage.getMax()), cat, "pcap", heapStats);
		record(t, percent(heapUsage.getCommitted(), heapUsage.getMax()), cat, "palloc", heapStats);
	}
	
	/**
	 * Records a stat into the passed node and the backlog
	 * @param timestamp The current timestamp
	 * @param value The value to record
	 * @param category The stat category
	 * @param id The stat id
	 * @param node The node to record into
	 */
	private static void record(long timestamp, long value, String category, String id, ObjectNode node) {
		node.put(id, value);
		getBacklog(category, id).insert(value, timestamp);
	}
	
	public static ObjectNode getBacklog() {
		ObjectNode b = jsonMapper.createObjectNode();
		for(Map.Entry<String, ConcurrentLongSlidingWindow> entry:  backlog.asMap().entrySet()) {
			String[] parts = entry.getKey().split("/");
			long[] window = entry.getValue().asLongArray();
			for(int i = 0; i < window.length; i++) {
				long val = window[i];
				i++;
				long ts = window[i];
				
			}
		}
		return b;
	}
	
	/**
	 * Calculates a percentage
	 * @param part The part
	 * @param whole The whole
	 * @return the percent
	 */
	public static int percent(double part, double whole) {
		if(part==0 || whole==0) return 0;
		double p = (part/whole)*100;
		return (int)p;
	}

}
