/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2014, Helios Development Group and individual contributors
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
package net.opentsdb.catalog.h2;

import java.sql.ResultSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.management.ObjectName;

import net.opentsdb.catalog.SQLWorker;
import net.opentsdb.catalog.SQLWorker.ResultSetHandler;

import org.helios.tsdb.plugins.util.JMXHelper;

/**
 * <p>Title: H2IOStats</p>
 * <p>Description: H2 Database IO Stats Monitor</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>net.opentsdb.catalog.h2.H2IOStats</code></p>
 */

public class H2IOStats implements Runnable, ResultSetHandler, H2IOStatsMBean {
	/** The sql worker to poll the table with */
	private final SQLWorker sqlWorker;
	/** A handle to the scheduled polling */
	private final ScheduledFuture<?> handle;
	/** A map of most recently polled values, keyed by the H2 column name */
	private final Map<String, Long> stats = new ConcurrentHashMap<String, Long>(10);
	/** The mbean objectname */
	private final ObjectName objectName = JMXHelper.objectName(new StringBuilder(getClass().getPackage().getName()).append(":service=H2IOStats"));
	
	
	/** The cummulative number of file writes since the DB started */
	public static final String fileWriteTotal = "info.FILE_WRITE_TOTAL";
	/** The cummulative number of file writes since the DB started */
	public static final String fileWrite = "info.FILE_WRITE";
	/** The number of file reads since the DB started */
	public static final String fileRead = "info.FILE_READ";
	/** The number of pages in the DB */
	public static final String pageCount = "info.PAGE_COUNT";
	/** The page size of the DB */
	public static final String pageSize = "info.PAGE_SIZE";
	/** The used cache size in KB */
	public static final String cacheSize = "info.CACHE_SIZE";
	/** The maximum cache size in KB */
	public static final String cacheMaxSize = "info.CACHE_MAX_SIZE";
	/** The sql to poll the stats */
	public static final String LOOKUP_SQL = "SELECT NAME, VALUE FROM INFORMATION_SCHEMA.SETTINGS where name in ('info.FILE_WRITE_TOTAL', 'info.FILE_WRITE', 'info.FILE_READ', 'info.PAGE_COUNT', 'info.PAGE_SIZE', 'info.CACHE_MAX_SIZE', 'info.CACHE_SIZE')";
	
	/**
	 * Creates a new H2IOStats
	 * @param sqlWorker The sql worker to poll the table with
	 * @param scheduler The scheduler to schedule the polls with
	 */
	public H2IOStats(SQLWorker sqlWorker, ScheduledExecutorService scheduler) {
		this.sqlWorker = sqlWorker;
		handle = scheduler.scheduleWithFixedDelay(this, 1, 5, TimeUnit.SECONDS);
		JMXHelper.registerMBean(this, objectName);
		
	}
	
	public long getTotalFileWrites() {
		return stats.get(fileWriteTotal);
	}
	
	public long getFileWrites() {
		return stats.get(fileWrite);
	}
	
	public long getFileReads() {
		return stats.get(fileRead);
	}
	
	public long getPageCount() {
		return stats.get(pageCount);
	}
	
	public long getPageSize() {
		return stats.get(pageSize);
	}
	
	public long getInUseCacheSize() {
		return stats.get(cacheSize);
	}
	
	public long getMaxCacheSize() {
		return stats.get(cacheMaxSize);
	}
	
	
	/**
	 * Unschedules and unregisters this stat monitor
	 */
	public void shutdown() {
		handle.cancel(false);
		JMXHelper.unregisterMBean(objectName);
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		sqlWorker.executeQuery(LOOKUP_SQL, this);		
	}

	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.SQLWorker.ResultSetHandler#onRow(int, java.sql.ResultSet)
	 */
	@Override
	public boolean onRow(int rowId, ResultSet rset) {
		try {
			stats.put(rset.getString(1), Long.parseLong(rset.getString(2)));
		} catch (Exception x) {/* No Op */}
		return true;
	}

}
