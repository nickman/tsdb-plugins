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
package org.helios.tsdb.plugins.stats;


import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.procedure.TObjectIntProcedure;

import java.lang.management.ClassLoadingMXBean;
import java.lang.management.CompilationMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;

import net.opentsdb.core.TSDB;
import net.opentsdb.stats.StatsCollector;

import org.helios.tsdb.plugins.rpc.AbstractRPCService;
import org.helios.tsdb.plugins.service.AbstractTSDBPluginService.StatsCollectorImpl;
import org.helios.tsdb.plugins.util.JMXHelper;
import org.helios.tsdb.plugins.util.SystemClock;
import org.helios.tsdb.plugins.util.SystemClock.ElapsedTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Title: AgentJVMMonitor</p>
 * <p>Description: Agent to collect stats from local JMX MXBeans</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.stats.AgentJVMMonitor</code></p>
 */

public class AgentJVMMonitor extends AbstractRPCService   {
	/** The ThreadMXBean */
	private final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
	/** The ClassLoaderMXBean */
	private final ClassLoadingMXBean clMXBean = ManagementFactory.getClassLoadingMXBean();
	/** The CompilationMXBean */
	private final CompilationMXBean compMXBean = ManagementFactory.getCompilationMXBean();
	/** Flag indicates if compilation time is supported */
	private final boolean compileTimeSupported = compMXBean.isCompilationTimeMonitoringSupported();
	/** The MemoryMXBean */
	private final MemoryMXBean memMXBean = ManagementFactory.getMemoryMXBean();
	/** A set of heap memory pool mx beans */
	private final Set<MemoryPoolMXBean> heapMemPoolMXBeans;
	/** A set of non heap memory pool mx beans */
	private final Set<MemoryPoolMXBean> nonHeapMemPoolMXBeans;
	/** A set of gc mx beans */
	private final Set<GarbageCollectorMXBean> gcMXBeans = Collections.unmodifiableSet(new HashSet<GarbageCollectorMXBean>(ManagementFactory.getGarbageCollectorMXBeans()));
	/** A map of timestamp/gc time to calc deltas */
	private final Map<String, long[]> gcDeltas = new HashMap<String, long[]>(gcMXBeans.size());
	/** A map of last gc atribute availability indicators for each GC Bean */
	private final Map<String, Boolean> gcLastGCAvailable = new HashMap<String, Boolean>(gcMXBeans.size());
	/** A map of last gc event ID for each gc bean keyed by the gc name */
	private final Map<String, Long> gcLastGCId = new HashMap<String, Long>(gcMXBeans.size());
	/** A set of NIO Buffer Pool MXBean ObjectNames */
	private final Set<ObjectName> bufferPoolObjectNames;
	/** Instance logger */
	private final Logger log = LoggerFactory.getLogger(getClass());

	/** The platform MBeanServer */
	public static final MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
	
	/** Java 7 Indicator */
	public static final boolean java7 = isJava7();
	
//	/**
//	 * Acquires the AgentJVMMonitor singleton instance
//	 * @return the AgentJVMMonitor singleton instance
//	 */
//	public static AgentJVMMonitor getInstance() {
//		if(instance==null) {
//			synchronized(lock) {
//				if(instance==null) {
//					instance = new AgentJVMMonitor();
//				}
//			}
//		}
//		return instance;
//	}
	
	/**
	 * Determines if this JVM is version 7+
	 * @return true if this JVM is version 7+, false otherwise
	 */
	public static boolean isJava7() {
		try {
			Class.forName("java.lang.management.BufferPoolMXBean");
			return true;
		} catch (Exception e) {
			return false;
		}
	}
	
	/**
	 * Callback from a plugin to collect
	 * @param statsCollector The collector container
	 */
	@Override
	public void collectStats(StatsCollector statsCollector) {
		log.debug("Collecting JVM Stats.....");
		ElapsedTime et = SystemClock.startClock();
		final boolean callerLoggerEnabled = (statsCollector instanceof StatsCollectorImpl);
		if(callerLoggerEnabled) {
			((StatsCollectorImpl)statsCollector).setCallerLogger(log);
		}
		try {
			statsCollector.addHostTag(true);
			statsCollector.addExtraTag("app", "tsdb");
			statsCollector.addExtraTag("component", "JVM");
			collect(statsCollector);
			log.debug("Collected JVM Stats in {}", et.elapsedStrMs());
		} catch (Exception ex) {
			log.error("Failed to collect stats", ex);			
		} finally {
//			statsCollector.clearExtraTag("host");
			statsCollector.clearExtraTag("app");
			statsCollector.clearExtraTag("component");
			if(callerLoggerEnabled) {
				((StatsCollectorImpl)statsCollector).clearCallerLogger();
			}

		}
	}
	
	
	/**
	 * Creates a new AgentJVMMonitor, initializes the tracing resources and schedules a callback for JMV metrics collection.
	 * @param tsdb The parent tsdb
	 * @param config the extracted configuration
	 */
	public AgentJVMMonitor(TSDB tsdb, Properties config) {
		super(tsdb, config);
		Set<MemoryPoolMXBean> tmpHeapSet = new HashSet<MemoryPoolMXBean>();
		Set<MemoryPoolMXBean> tmpNonHeapSet = new HashSet<MemoryPoolMXBean>();
		for(MemoryPoolMXBean pool: ManagementFactory.getMemoryPoolMXBeans()) {
			if("HEAP".equals(pool.getType().name())) tmpHeapSet.add(pool);
			else tmpNonHeapSet.add(pool);
		}
		heapMemPoolMXBeans = Collections.unmodifiableSet(tmpHeapSet);
		nonHeapMemPoolMXBeans = Collections.unmodifiableSet(tmpNonHeapSet);
		
		for(GarbageCollectorMXBean gcBean: gcMXBeans) {
			try {
				ObjectName on = JMXHelper.objectName(new StringBuilder(ManagementFactory.GARBAGE_COLLECTOR_MXBEAN_DOMAIN_TYPE).append(",name=").append(gcBean.getName()));
				mbeanServer.getAttribute(on, "LastGcInfo");
				gcLastGCAvailable.put(gcBean.getName(), true);
			} catch(Exception e) {
				gcLastGCAvailable.put(gcBean.getName(), false);
			}
		}
		if(java7) {
			bufferPoolObjectNames = new HashSet<ObjectName>(mbeanServer.queryNames(JMXHelper.objectName("java.nio:type=BufferPool,name=*"), null));
		} else {
			bufferPoolObjectNames = Collections.emptySet();
		}
		log.info("Started AgentJVMMonitor");
	}
	
	
	/**
	 * Executes the JVM metric collection and tracing. 
	 * @param statsCollector the TSDB provided stats collector
	 */
	public void collect(StatsCollector statsCollector) {
		final ElapsedTime et = SystemClock.startClock();
		final boolean callerLoggerEnabled = (statsCollector instanceof StatsCollectorImpl);
		if(callerLoggerEnabled) {
			((StatsCollectorImpl)statsCollector).setCallerLogger(log);
		}
		
		try {
			collectThreads(statsCollector);
			collectClassLoading(statsCollector);
			if(compileTimeSupported) collectCompilation(statsCollector);
			collectMemory(statsCollector);
			collectMemoryPools(statsCollector);
			collectGc(statsCollector);
			collectOS(statsCollector);
			if(java7) collectNio(statsCollector);
			long elapsed = et.elapsedMs();
			statsCollector.record("CollectTime", elapsed);
		} catch (Exception e) {
			log.error("AgentJVMMonitor: Unexpected collection exception", e);
		} finally {
			if(callerLoggerEnabled) {
				((StatsCollectorImpl)statsCollector).clearCallerLogger();
			}
		}
	}

	
	
	/**
	 * Collects thread stats
	 * @param sc The stats collector to write collected metrics to
	 */
	protected void collectThreads(final StatsCollector sc) {
		try {
			sc.addExtraTag("group", "threads");			
			final int threadCount = threadMXBean.getThreadCount();
			int daemonThreadCount = threadMXBean.getDaemonThreadCount();
			int nonDaemonThreadCount = threadCount-daemonThreadCount;
			long[] deadLocked = null;
			deadLocked = threadMXBean.findDeadlockedThreads();
			int deadlockedThreads  = deadLocked==null ? 0 : deadLocked.length;
			deadLocked = threadMXBean.findMonitorDeadlockedThreads();
			int monitorDeadlockedThreads = deadLocked==null ? 0 : deadLocked.length;
			int peakThreadCount = threadMXBean.getPeakThreadCount();
			threadMXBean.resetPeakThreadCount();
			sc.record("ThreadCount", threadCount);
			sc.record("DaemonThreadCount", daemonThreadCount);
			sc.record("NonDaemonThreadCount", nonDaemonThreadCount);
			sc.record("PeakThreadCount", peakThreadCount);
			sc.record("PeakThreadCount", peakThreadCount);
			sc.record("DeadLockThreadCount", deadlockedThreads);
			sc.record("MonDeadLockThreadCount", monitorDeadlockedThreads);
			TObjectIntHashMap<String> threadStateMap = new TObjectIntHashMap<String>(Thread.State.values().length);
			for(ThreadInfo ti: threadMXBean.getThreadInfo(threadMXBean.getAllThreadIds())) {
				threadStateMap.adjustOrPutValue(ti.getThreadState().name(), 1, 1);
			}
			threadStateMap.forEachEntry(new TObjectIntProcedure<String>() {
				@Override
				public boolean execute(String threadState, int count) {
					sc.record("ThreadCount", threadCount, "state=" + threadState);
					return true;
				}
			});			
		} finally {
			sc.clearExtraTag("group");
		}
	}
	
	/**
	 * Collects class loader stats
	 * @param sc The stats collector to write collected metrics to
	 */
	protected void collectClassLoading(final StatsCollector sc) {
		try {
			sc.addExtraTag("group", "classloading");
			sc.record("LoadedClasses", clMXBean.getLoadedClassCount());
			sc.record("UnloadedClasses", clMXBean.getUnloadedClassCount());
		} finally {
			sc.clearExtraTag("group");
		}
	}
	
	protected void record(StatsCollector sc, String metricName, long value) {
		
	}
	
	/**
	 * Collects JIT compiler stats
	 * @param sc The stats collector to write collected metrics to
	 */
	protected void collectCompilation(final StatsCollector sc) {
		sc.record("CompilationRate", clMXBean.getLoadedClassCount(), "group=compilation");
	}
	
	/**
	 * Collects heap and non heap memory stats.
	 * @param sc The stats collector to write collected metrics to
	 */
	protected void collectMemory(final StatsCollector sc) {
		try {
			sc.addExtraTag("group", "memory");
			sc.record("PendingFinalizers", memMXBean.getObjectPendingFinalizationCount());
			MemoryUsage heap = memMXBean.getHeapMemoryUsage();
			MemoryUsage nonHeap = memMXBean.getNonHeapMemoryUsage();
			sc.addExtraTag("type", "heap");
			
			sc.record("Committed", heap.getCommitted());
			sc.record("Max", heap.getMax());
			sc.record("Init", heap.getInit());
			sc.record("Used", heap.getUsed());
			sc.record("PercentUsed", calcPercent(heap.getUsed(), heap.getCommitted()));
			sc.record("PercentCapacity", calcPercent(heap.getUsed(), heap.getMax()));
			
			sc.clearExtraTag("type");
			sc.addExtraTag("type", "nonheap");

			sc.record("Committed", nonHeap.getCommitted());
			sc.record("Max", nonHeap.getMax());
			sc.record("Init", nonHeap.getInit());
			sc.record("Used", nonHeap.getUsed());
			sc.record("PercentUsed", calcPercent(nonHeap.getUsed(), nonHeap.getCommitted()));
			sc.record("PercentCapacity", calcPercent(nonHeap.getUsed(), nonHeap.getMax()));
			
		} finally {
			sc.clearExtraTag("group");
			sc.clearExtraTag("type");
		}
		
	}
	
	/**
	 * Collects memory pool stats.
	 * @param sc The stats collector to write collected metrics to
	 */
	protected void collectMemoryPools(final StatsCollector sc) {
		try {
			sc.addExtraTag("group", "memorypool");			
			for(MemoryPoolMXBean pool: heapMemPoolMXBeans) {
				try {
					MemoryUsage usage = pool.getUsage();					
					sc.addExtraTag("pool", pool.getName().replace(' ', '_'));
					sc.addExtraTag("type", "heap");
					sc.record("Committed", usage.getCommitted());
					sc.record("Max", usage.getMax());
					sc.record("Init", usage.getInit());
					sc.record("Used", usage.getUsed());
					sc.record("PercentUsed", calcPercent(usage.getUsed(), usage.getCommitted()));
					sc.record("PercentCapacity", calcPercent(usage.getUsed(), usage.getMax()));					
				} finally {
					sc.clearExtraTag("pool");
					sc.clearExtraTag("type");
					
				}
			}			
			for(MemoryPoolMXBean pool: nonHeapMemPoolMXBeans) {
				try {
					MemoryUsage usage = pool.getUsage();					
					sc.addExtraTag("pool", pool.getName().replace(' ', '_'));
					sc.addExtraTag("type", "nonheap");
					sc.record("Committed", usage.getCommitted());
					sc.record("Max", usage.getMax());
					sc.record("Init", usage.getInit());
					sc.record("Used", usage.getUsed());
					sc.record("PercentUsed", calcPercent(usage.getUsed(), usage.getCommitted()));
					sc.record("PercentCapacity", calcPercent(usage.getUsed(), usage.getMax()));					
				} finally {
					sc.clearExtraTag("pool");
					sc.clearExtraTag("type");
				}
			}			
		} finally {
			sc.clearExtraTag("group");
//			sc.clearExtraTag("pool");
//			sc.clearExtraTag("type");
		}
	}
	
	/**
	 * Collects GC stats.
	 * @param sc The stats collector to write collected metrics to
	 */
	protected void collectGc(final StatsCollector sc) {
		long now = System.currentTimeMillis();
		try {
			sc.addExtraTag("group", "gc");
			if(!gcDeltas.isEmpty()) {
				for(GarbageCollectorMXBean gcBean: gcMXBeans) {				
					String name = gcBean.getName().replace(' ', '_');
					long[] lastPeriod = gcDeltas.get(name);
					if(lastPeriod!=null) {
						long gcTime = gcBean.getCollectionTime();
						long elapsed = now - lastPeriod[0];
						long gcElapsed = gcTime - lastPeriod[1];
						sc.record("PercentTimeInGC", calcPercent(gcElapsed, elapsed), "name=" + name);
					}				
				}
			}
			gcDeltas.clear();
			for(GarbageCollectorMXBean gcBean: gcMXBeans) {
				String name = gcBean.getName().replace(' ', '_');
				String xtag = "name=" + name;
				long gcCount = gcBean.getCollectionCount();
				long gcTime = gcBean.getCollectionTime();
				gcDeltas.put(name, new long[]{now, gcTime});
				sc.record("GCTime", gcTime, xtag);
				sc.record("GCCount", gcCount, xtag);
				if(gcLastGCAvailable.get(gcBean.getName())) {
					try {
						ObjectName on = JMXHelper.objectName(new StringBuilder(ManagementFactory.GARBAGE_COLLECTOR_MXBEAN_DOMAIN_TYPE).append(",name=").append(gcBean.getName()));
						Long lastId = gcLastGCId.get(name);
						Long thisId = null;
						CompositeData gcInfo = (CompositeData)mbeanServer.getAttribute(on, "LastGcInfo");
						if(gcInfo!=null) {
							thisId = (Long)gcInfo.get("id");
							if(thisId.equals(lastId)) continue;
							gcLastGCId.put(name, thisId);
							try { sc.record("LastDuration", (Long)gcInfo.get("duration"), xtag); } catch (Exception e) {/* No Op */}
							try { sc.record("GCThreadCount", (Integer)gcInfo.get("GcThreadCount"), xtag); } catch (Exception e) {/* No Op */}
							TabularData memoryBeforeGc = (TabularData)gcInfo.get("memoryUsageBeforeGc");
							TabularData memoryAfterGc = (TabularData)gcInfo.get("memoryUsageAfterGc");
							String[] key = new String[1];
							for(String poolName: gcBean.getMemoryPoolNames()) {
								key[0] = poolName;
								MemoryUsage beforeGc = MemoryUsage.from((CompositeData)memoryBeforeGc.get(key).get("value"));
								MemoryUsage afterGc = MemoryUsage.from((CompositeData)memoryAfterGc.get(key).get("value"));
								long used = beforeGc.getUsed()-afterGc.getUsed();
								long committed = beforeGc.getCommitted()-afterGc.getCommitted();
								if(used<0) {
									sc.record("LastConsumed", Math.abs(used), xtag); 
								} else {
									sc.record("LastCleared", used, xtag);
								}
								if(committed<0) {
									sc.record("LastAllocated", Math.abs(committed), xtag);
								} else {
									sc.record("LastReleased", committed, xtag);
								}							
							}
						}
					} catch (Exception e) {
							e.printStackTrace(System.err);
					}
				}
			}
		} finally {
			sc.clearExtraTag("group");
		}
		
	}
	
	/** The NIO Buffer Pool Attributes */
	public static final String[] NIO_BUFFER_ATTRS = new String[]{"Count", "MemoryUsed", "TotalCapacity", "Name"};	

	
	/**
	 * Collects NIO stats 
	 * @param sc The stats collector to write collected metrics to
	 */
	protected void collectNio(final StatsCollector sc) {
		try {
			sc.addExtraTag("group", "nio");
			for(ObjectName on: bufferPoolObjectNames) {
				try {
					AttributeList attrs = mbeanServer.getAttributes(on, NIO_BUFFER_ATTRS);
					Map<String, Object> attrMap = new HashMap<String, Object>(attrs.size());
					for(Attribute attr: attrs.asList()) {
						attrMap.put(attr.getName(), attr.getValue());
					}
					String name = (String)attrMap.get(NIO_BUFFER_ATTRS[3]);
					String xtag = "name=" + name;
					sc.record("Count", (Long)attrMap.get(NIO_BUFFER_ATTRS[0]), xtag);
					long used = (Long)attrMap.get(NIO_BUFFER_ATTRS[1]);
					long capacity = (Long)attrMap.get(NIO_BUFFER_ATTRS[2]);
					
					sc.record("MemoryUsed", used, xtag);
					sc.record("TotalCapacity", capacity, xtag);
					sc.record("PerecentUsed", calcPercent(used, capacity), xtag);
				} catch (Exception e) {
					log.error("Failed to collect nio buffers", e);
				} finally {
					//sc.clearExtraTag("name");
				}
			}
		} finally {
			sc.clearExtraTag("group");
//			sc.clearExtraTag("name");
		}
	}
	
	/** The OS MXBean ObjectName */
	public static final ObjectName OS_MXBEAN_ON = JMXHelper.objectName(ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME);
	
	/**
	 * Collects OS process stats
	 * @param sc The stats collector to write collected metrics to
	 */
	protected void collectOS(final StatsCollector sc) {
		try {
			sc.addExtraTag("group", "nio");
			AttributeList attrs = null;
			try {
				attrs = mbeanServer.getAttributes(OS_MXBEAN_ON, windows ? WIN_OS_STATS : UNIX_OS_STATS);
			} catch (Exception e) {
				e.printStackTrace(System.err);
				return;
			}
			Map<String, Number> attrMap = new HashMap<String, Number>(attrs.size());
			for(Attribute attr: attrs.asList()) {
				attrMap.put(attr.getName(), (Number)attr.getValue());
			}		
			try {
				long totalSwap = attrMap.get(WIN_OS_STATS[1]).longValue();
				long freeSwap = attrMap.get(WIN_OS_STATS[2]).longValue();
				long usedSwap = totalSwap - freeSwap;
				sc.record(WIN_OS_STATS[1], totalSwap);
				sc.record(WIN_OS_STATS[2], freeSwap);
				sc.record("SwapUsed", totalSwap-freeSwap);
				sc.record("PercentSwapUsed", calcPercent(usedSwap, totalSwap));
			} catch (Exception e) {}
			try {
				long virtualMem = attrMap.get("CommittedVirtualMemorySize").longValue();
				long totalMem = attrMap.get("TotalPhysicalMemorySize").longValue();
				long freeMem = attrMap.get("FreePhysicalMemorySize").longValue();
				long usedMem = totalMem - freeMem;
				sc.record(WIN_OS_STATS[0], virtualMem);
				sc.record("TotalPhysicalMemorySize", totalMem);
				sc.record("FreePhysicalMemorySize", freeMem);
				sc.record("UsedPhysicalMemorySize", usedMem);
				
				sc.record("PercentMemoryUsed", calcPercent(usedMem, totalMem));
				sc.record("PercentMemoryFree", calcPercent(freeMem, totalMem));

				sc.record("PercentVirtualUsed", calcPercent(virtualMem, totalMem));
				sc.record("PercentVirtualFree", calcPercent((totalMem-virtualMem), totalMem));
				
			} catch (Exception e) {}
			try {
				long now = System.nanoTime();
				long cpuElapsed = attrMap.get(WIN_OS_STATS[3]).longValue();
				if(processCpuTime!=null) {
					long elapsedClockTime = now - processCpuTime[0];
					long elapsedCpuTime = cpuElapsed - processCpuTime[1];
					long totalCpuTime = elapsedClockTime * processors;
					sc.record("JVMCPU", calcPercent(elapsedCpuTime, totalCpuTime));
				}
				processCpuTime = new long[]{now, cpuElapsed};
			} catch (Exception e) {}
			if(!windows) {
				try {
					long openFd = attrMap.get("OpenFileDescriptorCount").longValue();
					long maxFd = attrMap.get("MaxFileDescriptorCount").longValue();
					sc.record("OpenFileDescriptors", openFd);
					sc.record("PercentFileDescriptors", calcPercent(openFd, maxFd));
				} catch (Exception e) {}
			}
		} finally {
			sc.clearExtraTag("group");
		}
	}
	
	
	/** OSMXBean Stats for Windows */
	public static final String[] WIN_OS_STATS = new String[]{"CommittedVirtualMemorySize", "TotalSwapSpaceSize", "FreeSwapSpaceSize", "ProcessCpuTime", "FreePhysicalMemorySize", "TotalPhysicalMemorySize"};
	/** OSMXBean Stats for Unix */
	public static final String[] UNIX_OS_STATS = new String[]{"CommittedVirtualMemorySize", "TotalSwapSpaceSize", "FreeSwapSpaceSize", "ProcessCpuTime", "FreePhysicalMemorySize", "TotalPhysicalMemorySize",
		"SystemCpuLoad", "ProcessCpuLoad", "SystemLoadAverage", "OpenFileDescriptorCount", "MaxFileDescriptorCount"
	};
	/** Indicates if this platform is Windows */
	public static final boolean windows = ManagementFactory.getOperatingSystemMXBean().getName().toLowerCase().contains("windows");
	/** The number of processors */
	public static final int processors = ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors();
	/** The timestamped process cpu time */
	protected long[] processCpuTime = null;
	
	
	/**
	 * Returns the passed byte count as Kb
	 * @param value the number of bytes
	 * @return the number of Kb
	 */
	protected static long toK(float value) {
		if(value==0) return 0L;
		float k = value/1024f;
		return (long)k;
	}
	
	/**
	 * Calcs an integer percentage
	 * @param part The part
	 * @param whole The whole
	 * @return The percentage that the part is of the whole
	 */
	protected static int calcPercent(float part, float whole) {
		if(part==0 || whole==0) return 0;
		float perc = part/whole*100;
		return (int)perc;
	}

}
