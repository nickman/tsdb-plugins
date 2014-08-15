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
package net.opentsdb.catalog.cache;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.List;

import javax.management.ObjectName;

import net.opentsdb.catalog.MetaReader;
import net.opentsdb.catalog.SQLWorker;
import net.opentsdb.catalog.SQLWorker.ResultSetHandler;
import net.opentsdb.catalog.TSDBCachedRowSetImpl;
import net.opentsdb.meta.UIDMeta;
import net.opentsdb.uid.UniqueId;

import org.helios.jmx.util.helpers.ConfigurationHelper;
import org.helios.tsdb.plugins.cache.CacheStatistics;
import org.helios.tsdb.plugins.util.JMXHelper;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.Callables;

/**
 * <p>Title: UIDCache</p>
 * <p>Description: An LRU cache for UIDMeta decodes</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>net.opentsdb.catalog.cache.UIDCache</code></p>
 */

public class UIDCache implements ResultSetHandler {
	/** The UIDMeta type this cache is for */
	protected final UniqueId.UniqueIdType uidType;
	/** A SQLWorker to execute lookups and inserts */
	protected final SQLWorker sqlWorker;
	/** The underlying guava cache of UIDMetas keyed by the UID */
	protected final Cache<String, UIDMeta> cache;
	/** The cache stats ObjectName if stats are enabled */
	protected final ObjectName objectName;
	
	/** The load sql */
	protected final String loadSql;
	/** The meta-reader */
	protected final MetaReader metaReader;
	
	
	
	/** The configuration property name for the maximum size of the cache */
	public static final String MAX_SIZE_PROP = "helios.search.catalog.uidcache.%s.maxsize";
	/** The configuration property name for the concurrency of the cache */
	public static final String CONCURRENCY_PROP = "helios.search.catalog.uidcache.%s.concurrency";
	/** The configuration property name for the stats enablement of the cache */
	public static final String STATS_ENABLED_PROP = "helios.search.catalog.uidcache.%s.stats";
	
	/** The default maximum size of the cache */
	public static final long DEFAULT_MAX_SIZE = 10000;
	/** The default concurrency of the cache */
	public static final int DEFAULT_CONCURRENCY = 4;
	/** The default stats enablement of the cache */
	public static final boolean DEFAULT_STATS_ENABLED = true;

	/** The configuration property with stats enablement of the cache */
	public static final String SPEC_TEMPLATE_WSTATS = "concurrencyLevel=%s,initialCapacity=%s,maximumSize=%s,recordStats";
	/** The configuration property without stats enablement of the cache */
	public static final String SPEC_TEMPLATE_NOSTATS = "concurrencyLevel=%s,initialCapacity=%s,maximumSize=%s";

	/** The count sql */
	public static final String COUNT_SQL = "SELECT COUNT(*) FROM TSD_%s";
	/** The initial load sql */
	public static final String INITIAL_LOAD_SQL = "SELECT * FROM TSD_%s LIMIT ?";
	
	/** The load sql */
	public static final String LOAD_SQL = "SELECT * FROM TSD_%s WHERE XUID = ?";
	
	/**
	 * Creates a new UIDCache
	 * @param uidType The UIDMeta type this cache is for
	 * @param sqlWorker A SQLWorker to execute lookups and inserts
	 * @param metaReader The meta-reader to build the UIDMeta from a resultset
	 */
	public UIDCache(UniqueId.UniqueIdType uidType, SQLWorker sqlWorker, MetaReader metaReader) {
		this.uidType = uidType;
		this.sqlWorker = sqlWorker;
		this.metaReader = metaReader;
		loadSql = String.format(LOAD_SQL, uidType.name());
		final String name = uidType.name().toLowerCase();
		final long maxSize = ConfigurationHelper.getLongSystemThenEnvProperty(String.format(MAX_SIZE_PROP, name), DEFAULT_MAX_SIZE);
		final int concurrency = ConfigurationHelper.getIntSystemThenEnvProperty(String.format(CONCURRENCY_PROP, name), DEFAULT_CONCURRENCY);
		final boolean stats = ConfigurationHelper.getBooleanSystemThenEnvProperty(String.format(STATS_ENABLED_PROP, name), DEFAULT_STATS_ENABLED);
		final long initialCount = sqlWorker.sqlForLong(String.format(COUNT_SQL, uidType.name()));
		final long initialSize = (initialCount > maxSize) ? maxSize : initialCount;
		final String spec = String.format(stats ? SPEC_TEMPLATE_WSTATS : SPEC_TEMPLATE_NOSTATS, concurrency, initialSize, maxSize);		
		cache = CacheBuilder.from(spec).build();
		if(stats) {
			objectName = JMXHelper.objectName(new StringBuilder(getClass().getPackage().getName()).append(":service=UIDCache,type=").append(uidType.name()));
			JMXHelper.registerMBean(objectName, new CacheStatistics(cache, objectName));
		} else {
			objectName = null;
		}
		put(metaReader.readUIDMetas(sqlWorker.executeQuery(String.format(INITIAL_LOAD_SQL, uidType.name()), false, initialSize), uidType).toArray(new UIDMeta[0]));
	}
	
	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.SQLWorker.ResultSetHandler#onRow(int, java.sql.ResultSet)
	 */
	@Override
	public boolean onRow(int rowId, ResultSet rset) {
		return true;
	}
	
	/**
	 * Shuts down the cache
	 */
	public void shutdown() {
		cache.invalidateAll();
		if(objectName!=null) JMXHelper.unregisterMBean(objectName);
	}

	/**
	 * Returns the value associated with {@code key} in this cache
	 * @param key The UIDMeta name name to look up
	 * @param conn The connection to use if the loader is called
	 * @return The UID name
	 */
	public UIDMeta get(final String key, final Connection conn) {
		try {
			return cache.get(key, Callables.returning(getName(key, conn)));
		} catch (Exception ex) {
			throw new RuntimeException("UIDCache [" + uidType + "] failed on looking up [" + key + "]", ex);
		}
	}

	/**
	 * Attempts to retrieve the UIDMeta from the DB
	 * @param key The UID of the target UIDMeta
	 * @param conn The optional connection
	 * @return the UIDMeta or null if one was not found
	 */
	protected UIDMeta getName(final String key, final Connection conn) {
		TSDBCachedRowSetImpl rset = (TSDBCachedRowSetImpl)sqlWorker.executeQuery(conn, loadSql, true, key);
		if(rset.size()==0) {
			return null;
		}		
		try {
			rset.setMaxRows(1);
			List<UIDMeta> uidMetas = metaReader.readUIDMetas(rset, uidType);
			if(uidMetas==null || uidMetas.isEmpty()) return null;
			return uidMetas.get(0);
		} catch (Exception ex) {
			throw new RuntimeException("UIDCache [" + uidType + "] failed on looking up [" + key + "]", ex);
		}
	}
		
	/**
	 * Determines if the UIDMeta with the passed UID is in cache
	 * @param key The UIDMeta's UID
	 * @return true if present, false otherwise
	 */
	public boolean contains(String key) {
		if(key==null || key.trim().isEmpty()) return false;
		return cache.getIfPresent(key)!=null;
	}

	/**
	 * Returns the number of entries in this cache
	 * @return the number of entries in this cache
	 */
	public long size() {
		return cache.size();
	}
	
	/**
	 * Puts an array of UIDMetas into cache
	 * @param values the UIDMetas to cache 
	 * @see com.google.common.cache.Cache#put(java.lang.Object, java.lang.Object)
	 */
	public void put(UIDMeta...values) {
		for(UIDMeta value: values) {
			if(value != null && value.getUID()!=null && !value.getUID().trim().isEmpty()) {
				cache.put(value.getUID().trim(), value);
			}
		}
	}

}
