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

import net.opentsdb.core.TSDB;
import net.opentsdb.stats.StatsCollector;

import org.helios.tsdb.plugins.event.PluginType;
import org.helios.tsdb.plugins.service.PluginContext;

/**
 * <p>Title: Plugin</p>
 * <p>Description: The common OpenTSDB plugin methods.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.shell.Plugin</code></p>
 */

public interface Plugin {
	/**
	 * Returns the plugin type
	 * @return the plugin type
	 */
	public PluginType getPluginType();
	
	/**
	 * Called by the TSD when a request for statistics collection has come in. The implementation may provide one or more statistics. If no statistics are available for the implementation, simply stub the method.
	 * @param collector The collector used for emitting statistics
	 */
	public void collectStats(StatsCollector collector);
	/**
	 * Called by TSDB to initialize the plugin Implementations are responsible for setting up any IO they need as well as starting any required background threads.
	 * @param tsdb The parent TSDB object
	 */
	public void initialize(TSDB tsdb);
	/**
	 * Called to gracefully shutdown the plugin.
	 * @return The shutdown response
	 */
	public com.stumbleupon.async.Deferred<Object>  shutdown();
	/**
	 * Should return the version of this plugin in the format: MAJOR.MINOR.MAINT, e.g.
	 * @return the plugin version
	 */
	public String version();
	
	/**
	 * Handles a callback from the TSDBPluginServiceLoader supplying the plugin context
	 * @param ctx the plugin context
	 */
	public void setPluginContext(PluginContext ctx);
	
}
