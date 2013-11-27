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
package org.helios.tsdb.plugins.shell;

import java.util.List;

import net.opentsdb.core.TSDB;
import net.opentsdb.search.SearchQuery;
import net.opentsdb.stats.StatsCollector;
import net.opentsdb.tsd.RpcPlugin;
import net.opentsdb.utils.Config;

import org.hbase.async.HBaseException;
import org.helios.tsdb.plugins.Constants;
import org.helios.tsdb.plugins.event.PluginType;
import org.helios.tsdb.plugins.service.ITSDBPluginService;
import org.helios.tsdb.plugins.service.TSDBPluginServiceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stumbleupon.async.Deferred;

/**
 * <p>Title: RpcService</p>
 * <p>Description: The primary {@link RpcPlugin} implementation which provides some built in RPC services.</p>
 * <p>Activated with the tsdb config property <b><code>tsd.rpc.plugins</code></b></p>
 * and can load additional RPC event handlers.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.shell.RpcService</code></p>
 */

public class RpcService extends RpcPlugin implements Plugin {
	/** Instance logger */
	protected final Logger log = LoggerFactory.getLogger(getClass());
	/** The event pluginService delegate */
	protected ITSDBPluginService pluginService;
	/** The core TSDB instance */
	protected TSDB tsdb;

	/**
	 * Creates a new RpcService
	 */
	public RpcService() {
		log.debug("Created {} instance", getClass().getName());
		System.setProperty(getClass().getName() + ".loaded", "true");
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.shell.Plugin#getPluginType()
	 */
	public PluginType getPluginType() {
		return PluginType.RPC;
	}
	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.tsd.RpcPlugin#collectStats(net.opentsdb.stats.StatsCollector)
	 */
	@Override
	public void collectStats(StatsCollector statsCollector) {
		if(pluginService!=null) pluginService.collectStats(PluginType.RPC, statsCollector);
	}

	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.tsd.RpcPlugin#initialize(net.opentsdb.core.TSDB)
	 */
	@Override
	public void initialize(TSDB tsdb) {
		log.debug("Initializing instance");
		pluginService = TSDBPluginServiceLoader.getInstance(tsdb, this);
	}

	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.tsd.RpcPlugin#shutdown()
	 */
	@Override
	public Deferred<Object> shutdown() {
		try {
			return pluginService.shutdown(new Deferred<Object>());
		} finally {
			pluginService = null;
		}		
	}


	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.tsd.RpcPlugin#version()
	 */
	@Override
	public String version() {
		return Constants.PLUGIN_VERSION;
	}

	/**
	 * 
	 * @see net.opentsdb.core.TSDB#dropCaches()
	 */
	public void dropCaches() {
		tsdb.dropCaches();
	}

	/**
	 * @param arg0
	 * @return
	 * @see net.opentsdb.core.TSDB#executeSearch(net.opentsdb.search.SearchQuery)
	 */
	public Deferred<SearchQuery> executeSearch(SearchQuery arg0) {
		return tsdb.executeSearch(arg0);
	}

	/**
	 * @return
	 * @throws HBaseException
	 * @see net.opentsdb.core.TSDB#flush()
	 */
	public Deferred<Object> flush() throws HBaseException {
		return tsdb.flush();
	}

	/**
	 * @return
	 * @see net.opentsdb.core.TSDB#getConfig()
	 */
	public final Config getConfig() {
		return tsdb.getConfig();
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @return
	 * @see net.opentsdb.core.TSDB#suggestMetrics(java.lang.String, int)
	 */
	public List<String> suggestMetrics(String arg0, int arg1) {
		return tsdb.suggestMetrics(arg0, arg1);
	}

	/**
	 * @param arg0
	 * @return
	 * @see net.opentsdb.core.TSDB#suggestMetrics(java.lang.String)
	 */
	public List<String> suggestMetrics(String arg0) {
		return tsdb.suggestMetrics(arg0);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @return
	 * @see net.opentsdb.core.TSDB#suggestTagNames(java.lang.String, int)
	 */
	public List<String> suggestTagNames(String arg0, int arg1) {
		return tsdb.suggestTagNames(arg0, arg1);
	}

	/**
	 * @param arg0
	 * @return
	 * @see net.opentsdb.core.TSDB#suggestTagNames(java.lang.String)
	 */
	public List<String> suggestTagNames(String arg0) {
		return tsdb.suggestTagNames(arg0);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @return
	 * @see net.opentsdb.core.TSDB#suggestTagValues(java.lang.String, int)
	 */
	public List<String> suggestTagValues(String arg0, int arg1) {
		return tsdb.suggestTagValues(arg0, arg1);
	}

	/**
	 * @param arg0
	 * @return
	 * @see net.opentsdb.core.TSDB#suggestTagValues(java.lang.String)
	 */
	public List<String> suggestTagValues(String arg0) {
		return tsdb.suggestTagValues(arg0);
	}

	/**
	 * @return
	 * @see net.opentsdb.core.TSDB#uidCacheHits()
	 */
	public int uidCacheHits() {
		return tsdb.uidCacheHits();
	}

	/**
	 * @return
	 * @see net.opentsdb.core.TSDB#uidCacheMisses()
	 */
	public int uidCacheMisses() {
		return tsdb.uidCacheMisses();
	}

	/**
	 * @return
	 * @see net.opentsdb.core.TSDB#uidCacheSize()
	 */
	public int uidCacheSize() {
		return tsdb.uidCacheSize();
	}

	public void shutdownTSDB() {
		try {
			tsdb.shutdown().joinUninterruptibly();
		} catch (Exception e) {
			log.error("Error in TSDB Shutdown Request", e);
			throw new RuntimeException("Error in TSDB Shutdown Request", e);
		}
	}
}
