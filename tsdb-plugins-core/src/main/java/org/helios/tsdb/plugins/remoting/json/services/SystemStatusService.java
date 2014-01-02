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
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

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
	
	/**
	 * Creates a new SystemStatusService
	 */
	private SystemStatusService() {
		
	}
	
	/**
	 * Collects system status metrics and returns them as a JSON Node
	 * @return the metrics as a JSON Node
	 */
	public static ObjectNode collect() {
		LOG.debug("Collecting System Status");
		ObjectNode stats = jsonMapper.createObjectNode();
		try {
			stats.put("heap", collectHeap());
			
			
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
	public static ObjectNode collectHeap() {
		ObjectNode heapStats = jsonMapper.createObjectNode();
		MemoryUsage heapUsage = memoryMXBean.getHeapMemoryUsage();
		heapStats.put("init", heapUsage.getInit());
		heapStats.put("max", heapUsage.getMax());
		heapStats.put("com", heapUsage.getCommitted());
		heapStats.put("used", heapUsage.getUsed());
		heapStats.put("pused", percent(heapUsage.getUsed(), heapUsage.getCommitted()));
		heapStats.put("pcap", percent(heapUsage.getUsed(), heapUsage.getMax()));
		heapStats.put("palloc", percent(heapUsage.getCommitted(), heapUsage.getMax()));
		return heapStats;
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
