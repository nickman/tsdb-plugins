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

import java.util.EnumMap;
import java.util.Map;

import javax.management.ObjectName;

import net.opentsdb.uid.UniqueId;

import org.helios.tsdb.plugins.service.TSDBPluginServiceLoader;
import org.helios.tsdb.plugins.util.JMXHelper;

/**
 * <p>Title: UniqueIdRegistry</p>
 * <p>Description: Service to expose the TSDB's {@link UniqueId} instances so all consumers are using the same instance and to provide JMX based cache stats</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>net.opentsdb.core.UniqueIdRegistry</code></p>
 */

public class UniqueIdRegistry implements UniqueIdRegistryMXBean {
	/** The singleton instance */
	private static volatile UniqueIdRegistry instance = null;
	/** The singleton instance ctor lock */
	private static final Object lock = new Object();
	
	/** The TSDB instance */
	private final TSDB tsdb;
	/** Unique IDs for the metric names. */
	private final UniqueId metrics;
	/** Unique IDs for the tag names. */
	private final UniqueId tag_names;
	/** Unique IDs for the tag values. */
	private final UniqueId tag_values;
	
	/** A map of UniqueIds keyed by the UniqueId type  */
	private final Map<UniqueId.UniqueIdType, UniqueId> byType = new EnumMap<UniqueId.UniqueIdType, UniqueId>(UniqueId.UniqueIdType.class);
	
	/** The JMX service MBean's ObjectName */
	public static final ObjectName OBJECT_NAME = JMXHelper.objectName(new StringBuilder(UniqueIdRegistry.class.getPackage().getName()).append(":service=").append(UniqueIdRegistry.class.getSimpleName()));
	
	/**
	 * Acquires the UniqueIdRegistry singleton instance
	 * @return the UniqueIdRegistry singleton instance
	 */
	public static UniqueIdRegistry getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					TSDB tsdb = TSDBPluginServiceLoader.getInstance().getTSDB();
					instance = new UniqueIdRegistry(tsdb);
				}
			}
		}
		return instance;
	}
	
	/**
	 * Acquires the UniqueIdRegistry singleton instance
	 * @param tsdb The TSDB instance to initialize with
	 * @return the UniqueIdRegistry singleton instance
	 */
	public static UniqueIdRegistry getInstance(TSDB tsdb) {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new UniqueIdRegistry(tsdb);
				}
			}
		}
		return instance;
	}
	
	
	/**
	 * Creates a new UniqueIdRegistry
	 * @param The TSDB used by this service
	 */
	private UniqueIdRegistry(TSDB tsdb) {
		this.tsdb = tsdb;
		metrics = this.tsdb.metrics;
		byType.put(UniqueId.UniqueIdType.METRIC, metrics);
		tag_names = this.tsdb.tag_names;
		byType.put(UniqueId.UniqueIdType.TAGK, tag_names);
		tag_values = this.tsdb.tag_values;		
		byType.put(UniqueId.UniqueIdType.TAGV, tag_values);
		JMXHelper.registerMBean(this, OBJECT_NAME);
	}

	/**
	 * Returns the Metrics UniqueId instance
	 * @return the Metrics UniqueId instance
	 */
	public UniqueId getMetricsUniqueId() {
		return metrics;
	}

	/**
	 * Returns the tagk UniqueId instance
	 * @return the tagk UniqueId instance
	 */
	public UniqueId getTagKUniqueId() {
		return tag_names;
	}

	/**
	 * Returns the tagv UniqueId instance
	 * @return the tagv UniqueId instance
	 */
	public UniqueId getTagVUniqueId() {
		return tag_values;
	}
	
	/**
	 * Returns the UniqueId for the passed UniqueIdType
	 * @param type The UniqueIdType to get the UniqueId for
	 * @return the UniqueIdType
	 */
	public UniqueId forType(UniqueId.UniqueIdType type) {
		if(type==null) throw new IllegalArgumentException("The passed UniqueIdType was null");
		return byType.get(type);
	}
	
	/**
	 * Returns the UniqueId for the passed UniqueIdType name
	 * @param name The UniqueIdType name to get the UniqueId for
	 * @return the UniqueIdType
	 */
	public UniqueId forType(String name) {
		if(name==null) throw new IllegalArgumentException("The passed UniqueIdType name was null");
		name = name.trim().toUpperCase();
		UniqueId.UniqueIdType type = null;
		try {
			type = UniqueId.UniqueIdType.valueOf(name);
			return forType(type);
		} catch (Exception ex) {
			throw new IllegalArgumentException("The name [" + name + "] is not a valid UniqueIdType");
		}
	}
	
	/**
	 * Returns the Metrics UniqueId cache hits
	 * @return  the Metrics UniqueId cache hits
	 */
	public int getMetricCacheHits() {
		return metrics.cacheHits();
	}
	
	/**
	 * Returns the Metrics UniqueId cache misses
	 * @return  the Metrics UniqueId cache misses
	 */
	public int getMetricCacheMisses() {
		return metrics.cacheMisses();
	}
	
	/**
	 * Returns the Metrics UniqueId cache size
	 * @return  the Metrics UniqueId cache size
	 */
	public int getMetricCacheSize() {
		return metrics.cacheSize();
	}
	
	/**
	 * Returns the TAGK UniqueId cache hits
	 * @return  the TAGK UniqueId cache hits
	 */
	public int getTagKCacheHits() {
		return tag_names.cacheHits();
	}
	
	/**
	 * Returns the TAGK UniqueId cache misses
	 * @return  the TAGK UniqueId cache misses
	 */
	public int getTagKCacheMisses() {
		return tag_names.cacheMisses();
	}
	
	/**
	 * Returns the TAGK UniqueId cache size
	 * @return  the TAGK UniqueId cache size
	 */
	public int getTagKCacheSize() {
		return tag_names.cacheSize();
	}
	
	/**
	 * Returns the TAGV UniqueId cache hits
	 * @return  the TAGV UniqueId cache hits
	 */
	public int getTagVCacheHits() {
		return tag_values.cacheHits();
	}
	
	/**
	 * Returns the TAGV UniqueId cache misses
	 * @return  the TAGV UniqueId cache misses
	 */
	public int getTagVCacheMisses() {
		return tag_values.cacheMisses();
	}
	
	/**
	 * Returns the TAGV UniqueId cache size
	 * @return  the TAGV UniqueId cache size
	 */
	public int getTagVCacheSize() {
		return tag_values.cacheSize();
	}

	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.core.UniqueIdRegistryMXBean#purgeMetricCache()
	 */
	@Override
	public void purgeMetricCache() {
		metrics.dropCaches();
	}

	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.core.UniqueIdRegistryMXBean#purgeTagKCache()
	 */
	@Override
	public void purgeTagKCache() {
		tag_names.dropCaches();
	}

	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.core.UniqueIdRegistryMXBean#purgeTagVCache()
	 */
	@Override
	public void purgeTagVCache() {
		tag_values.dropCaches();		
	}
	
	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.core.UniqueIdRegistryMXBean#purgeAllCaches()
	 */
	@Override
	public void purgeAllCaches() {
		tag_names.dropCaches();
		tag_values.dropCaches();
		metrics.dropCaches();
	}
	
	public void dumpMetricNameCache() {
		
	}
	
	
	

}
