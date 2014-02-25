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
package net.opentsdb.core;

/**
 * <p>Title: UniqueIdRegistryMXBean</p>
 * <p>Description: JMX MXBean interface for the {@link UniqueIdRegistry} </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>net.opentsdb.core.UniqueIdRegistryMXBean</code></p>
 */

public interface UniqueIdRegistryMXBean {

	/**
	 * Returns the Metrics UniqueId cache hits
	 * @return  the Metrics UniqueId cache hits
	 */
	public int getMetricCacheHits();
	
	/**
	 * Returns the Metrics UniqueId cache misses
	 * @return  the Metrics UniqueId cache misses
	 */
	public int getMetricCacheMisses();
	
	/**
	 * Returns the Metrics UniqueId cache size
	 * @return  the Metrics UniqueId cache size
	 */
	public int getMetricCacheSize();
	
	/**
	 * Empties the metric cache
	 */
	public void purgeMetricCache();
	
	/**
	 * Returns the TAGK UniqueId cache hits
	 * @return  the TAGK UniqueId cache hits
	 */
	public int getTagKCacheHits();
	
	/**
	 * Returns the TAGK UniqueId cache misses
	 * @return  the TAGK UniqueId cache misses
	 */
	public int getTagKCacheMisses();
	
	/**
	 * Returns the TAGK UniqueId cache size
	 * @return  the TAGK UniqueId cache size
	 */
	public int getTagKCacheSize();
	
	/**
	 * Empties the tagk cache
	 */
	public void purgeTagKCache();
	
	
	/**
	 * Returns the TAGV UniqueId cache hits
	 * @return  the TAGV UniqueId cache hits
	 */
	public int getTagVCacheHits();
	
	/**
	 * Returns the TAGV UniqueId cache misses
	 * @return  the TAGV UniqueId cache misses
	 */
	public int getTagVCacheMisses();
	
	/**
	 * Returns the TAGV UniqueId cache size
	 * @return  the TAGV UniqueId cache size
	 */
	public int getTagVCacheSize();
	
	/**
	 * Empties the tagv cache
	 */
	public void purgeTagVCache();
	
	/**
	 * Empties all the caches
	 */
	public void purgeAllCaches();
	


}
