/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2013, Helios Development Group and individual contributors
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
package org.helios.tsdb.plugins.remoting;

import net.opentsdb.stats.StatsCollector;

import org.helios.tsdb.plugins.service.PluginContext;
import org.helios.tsdb.plugins.util.JMXHelper;

/**
 * <p>Title: IRemoteService</p>
 * <p>Description: Defines the remotely invocable services available for OpenTSDB and Plugin Services.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.remoting.IRemoteService</code></p>
 * <br>
 * <p><ul>
 * <li><b></b>:</li>
 * </ul></p>
 */

public interface IRemoteService {
	
	/**
	 * Initializes the remoting service
	 * @param pc The plugin context
	 */
	public void initialize(PluginContext pc);

	/**
	 * Shuts down and cleans up the remoting service
	 */
	public void shutdown();

	/**
	 * Collects stats on the remoting service
	 * @param collector the stats collector
	 */
	public void collectStats(StatsCollector collector);

	/**
	 * Returns the actual logger level for the remoting service
	 * @return the actual logger level
	 */
	public String getLoggerLevel();

	/**
	 * Returns the effective logger level for the remoting service
	 * @return the effective  logger level
	 */
	public String getLoggerEffectiveLevel();

	/**
	 * Sets the effective logger level for the remoting service
	 * @param level the level name to set
	 */
	public void setLoggerLevel(String level);
	
}
