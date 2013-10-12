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
package org.helios.tsdb.plugins.handlers;

import net.opentsdb.core.TSDB;
import net.opentsdb.stats.StatsCollector;

/**
 * <p>Title: IEventHandler</p>
 * <p>Description: Defines the common methods of all event handlers</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.handlers.IEventHandler</code></p>
 */

public interface IEventHandler {
	
	/**
	 * Called by TSDB to initialize the plugin Implementations are responsible for setting up 
	 * any IO they need as well as starting any required background threads. 
	 * Note: Implementations should throw exceptions if they can't start up properly. 
	 * The TSD will then shutdown so the operator can fix the problem. Please use IllegalArgumentException for configuration issues.
	 * @param tsdb The parent TSDB object
	 */
	public void initialize(TSDB tsdb);
	
	/**
	 * Called to gracefully shutdown the plugin. Implementations should close any IO they have open
	 */
	public void shutdown();
	
	/**
	 * Called by the TSD when a request for statistics collection has come in. The implementation may provide one or more statistics.
	 * @param collector The collector used for emitting statistics
	 */
	public void collectStats(StatsCollector collector);
}
