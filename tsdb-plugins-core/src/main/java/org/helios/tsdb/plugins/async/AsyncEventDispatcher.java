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
package org.helios.tsdb.plugins.async;

import java.util.Collection;
import java.util.Properties;
import java.util.concurrent.Executor;

import net.opentsdb.stats.StatsCollector;

import org.helios.tsdb.plugins.handlers.IEventHandler;

/**
 * <p>Title: AsyncEventDispatcher</p>
 * <p>Description: Dispatcher that accepts TSDB plugin callbacks and dispatches them asynchronously.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.async.AsyncEventDispatcher</code></p>
 */

public interface AsyncEventDispatcher extends ISearchEventDispatcher, IPublishEventDispatcher {
	/**
	 * Configures the async event dispatcher
	 * @param config The TSDB provided config properties
	 * @param executor The executor to use
	 * @param handlers The handles to register
	 */
	public void initialize(Properties config, Executor executor, Collection<IEventHandler> handlers);
	
	/**
	 * Executes a clean shutdown on this AsyncEventDispatcher
	 */
	public void shutdown();
	
	/**
	 * Collects statistics for this AsyncEventDispatcher
	 * @param collector The collector to inject stats into 
	 */
	public void collectStats(StatsCollector collector);

	
}
