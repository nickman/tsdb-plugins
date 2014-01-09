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

import java.util.Map;

import net.opentsdb.core.TSDB;
import net.opentsdb.stats.StatsCollector;
import net.opentsdb.tsd.RTPublisher;

import org.helios.tsdb.plugins.Constants;
import org.helios.tsdb.plugins.event.PluginType;
import org.helios.tsdb.plugins.service.ITSDBPluginService;
import org.helios.tsdb.plugins.service.PluginContext;
import org.helios.tsdb.plugins.service.TSDBPluginServiceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stumbleupon.async.Deferred;

/**
 * <p>Title: Publisher</p>
 * <p>Description: A passthrough {@link RTPublisher} that delegates to the @link TSDBEventpluginService} asynch multiplexer.</p> 
 * <p>Activated with the tsdb config property <b><code>tsd.rtpublisher.plugin</code></b> and <b><code>tsd.rtpublisher.enable</code></b> must be true</p>
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.shell.Publisher</code></p>
 */

public class Publisher extends RTPublisher implements Plugin {
	/** Instance logger */
	protected final Logger log = LoggerFactory.getLogger(getClass());
	/** The event pluginService delegate */
	protected ITSDBPluginService pluginService;
	/** The plugin context */
	protected PluginContext pluginContext = null;
	
	
	
	/**
	 * Creates a new Publisher
	 */
	public Publisher() {
		log.debug("Created {} instance", getClass().getName());
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.shell.Plugin#getPluginType()
	 */
	public PluginType getPluginType() {
		return PluginType.PUBLISH;
	}
	
	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.tsd.RTPublisher#initialize(net.opentsdb.core.TSDB)
	 */
	@Override
	public void initialize(TSDB tsdb) {
		log.debug("Initializing instance");
		pluginService = TSDBPluginServiceLoader.getInstance(tsdb, this);
	}
	

	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.tsd.RTPublisher#collectStats(net.opentsdb.stats.StatsCollector)
	 */
	@Override
	public void collectStats(StatsCollector statsCollector) {
		if(pluginService!=null) pluginService.collectStats(PluginType.PUBLISH, statsCollector);
	}


	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.tsd.RTPublisher#publishDataPoint(java.lang.String, long, long, java.util.Map, byte[])
	 */
	@Override
	public Deferred<Object> publishDataPoint(String metric, long timestamp, long value, Map<String, String> tags, byte[] tsuid) {
		if(pluginService!=null) pluginService.publishDataPoint(metric, timestamp, value, tags, tsuid);
		return Constants.NULL_DEFERED;
	}

	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.tsd.RTPublisher#publishDataPoint(java.lang.String, long, double, java.util.Map, byte[])
	 */
	@Override
	public Deferred<Object> publishDataPoint(String metric, long timestamp, double value, Map<String, String> tags, byte[] tsuid) {
		if(pluginService!=null) pluginService.publishDataPoint(metric, timestamp, value, tags, tsuid);
		return Constants.NULL_DEFERED;
	}
	

	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.tsd.RTPublisher#version()
	 */
	@Override
	public String version() {		
		return Constants.PLUGIN_VERSION;
	}

	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.tsd.RTPublisher#shutdown()
	 */
	@Override
	public Deferred<Object> shutdown() {
		if(pluginService==null) return Deferred.fromResult(null);
		try {
			return pluginService.shutdown(new Deferred<Object>());
		} finally {
			pluginService = null;
		}		
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.shell.Plugin#setPluginContext(org.helios.tsdb.plugins.service.PluginContext)
	 */
	@Override
	public void setPluginContext(PluginContext ctx) {
		pluginContext = ctx;
	}

}
