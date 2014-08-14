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
package org.helios.tsdb.plugins.cache;

import javax.management.ObjectName;
import org.helios.jmx.util.helpers.JMXHelper;
import com.google.common.cache.Cache;


/**
 * <p>Title: CacheStatistics</p>
 * <p>Description: JMX stats for Google Guava Caches</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.cache.CacheStatistics</code></p>
 */

public class CacheStatistics implements CacheStatisticsMXBean {
	/** The wrapped cache instance */
	protected final Cache<?, ?> cache;	
	/** The cache stats JMX object name */
	protected final ObjectName objectName;


	/**
	 * Creates a new CacheStatistics
	 * @param cache The guava cache instance to wrap
	 * @param objectName The assigned JMX ObjectName for this cache
	 */
	public CacheStatistics(Cache<?, ?> cache, ObjectName objectName) {
		this.cache = cache;
		this.objectName = objectName;		
	}

	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.cache.CacheStatisticsMXBean#invalidateAll()
	 */
	@Override
	public void invalidateAll() {
		cache.invalidateAll();
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.cache.CacheStatisticsMXBean#cleanup()
	 */
	@Override
	public void cleanup() {
		cache.cleanUp();
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.cache.CacheStatisticsMXBean#getSize()
	 */
	@Override
	public long getSize() {
		return cache.size();
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.cache.CacheStatisticsMXBean#getRequestCount()
	 */
	@Override
	public long getRequestCount() {
		return cache.stats().requestCount();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.cache.CacheStatisticsMXBean#getHitCount()
	 */
	@Override
	public long getHitCount() {
		return cache.stats().hitCount();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.cache.CacheStatisticsMXBean#getHitRate()
	 */
	@Override
	public double getHitRate() {
		return cache.stats().hitRate();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.cache.CacheStatisticsMXBean#getMissCount()
	 */
	@Override
	public long getMissCount() {
		return cache.stats().missCount();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.cache.CacheStatisticsMXBean#getMissRate()
	 */
	@Override
	public double getMissRate() {
		return cache.stats().missRate();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.cache.CacheStatisticsMXBean#getLoadCount()
	 */
	@Override
	public long getLoadCount() {
		return cache.stats().loadCount();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.cache.CacheStatisticsMXBean#getLoadSuccessCount()
	 */
	@Override
	public long getLoadSuccessCount() {
		return cache.stats().loadSuccessCount();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.cache.CacheStatisticsMXBean#getLoadExceptionCount()
	 */
	@Override
	public long getLoadExceptionCount() {
		return cache.stats().loadExceptionCount();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.cache.CacheStatisticsMXBean#getLoadExceptionRate()
	 */
	@Override
	public double getLoadExceptionRate() {
		return cache.stats().loadExceptionRate();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.cache.CacheStatisticsMXBean#getTotalLoadTime()
	 */
	@Override
	public long getTotalLoadTime() {
		return cache.stats().totalLoadTime();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.cache.CacheStatisticsMXBean#getAverageLoadPenalty()
	 */
	@Override
	public double getAverageLoadPenalty() {
		return cache.stats().averageLoadPenalty();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.cache.CacheStatisticsMXBean#getEvictionCount()
	 */
	@Override
	public long getEvictionCount() {
		return cache.stats().evictionCount();
	}

}
