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

import java.util.Properties;

import net.opentsdb.core.TSDB;
import net.opentsdb.stats.StatsCollector;

import org.helios.tsdb.plugins.handlers.logging.LoggerManager;
import org.helios.tsdb.plugins.handlers.logging.LoggerManagerFactory;
import org.helios.tsdb.plugins.service.PluginContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Title: AbstractTSDBEventHandler</p>
 * <p>Description: Abstract base class for implementing TSDB event handlers </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.handlers.AbstractTSDBEventHandler</code></p>
 */

public class AbstractTSDBEventHandler implements IEventHandler {
	/** The handler logger */
	protected final Logger log = LoggerFactory.getLogger(getClass());
	/** The handler logger leel manager */
	protected final LoggerManager loggerManager = LoggerManagerFactory.getLoggerManager(getClass());
	
	/** The TSDB instance */
	protected TSDB tsdb;
	/** The extracted config */
	protected Properties config;
	/** The plugin support classloader */
	protected ClassLoader supportClassLoader = null;
	/** The plugin context */
	protected PluginContext pluginContext = null;
	
	/**
	 * Creates a new AbstractTSDBEventHandler
	 */
	public AbstractTSDBEventHandler() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.handlers.IEventHandler#initialize(org.helios.tsdb.plugins.service.PluginContext)
	 */
	@Override
	public void initialize(PluginContext pc) {
		pluginContext = pc;
		tsdb = pluginContext.getTsdb();
		config = pluginContext.getExtracted();
		supportClassLoader = pluginContext.getSupportClassLoader();		
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.handlers.IEventHandler#shutdown()
	 */
	@Override
	public void shutdown() {
		// TODO Auto-generated method stub
		
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.handlers.IEventHandler#collectStats(net.opentsdb.stats.StatsCollector)
	 */
	@Override
	public void collectStats(StatsCollector collector) {
		// To be optionally implemented by concrete class
		
	}

	/**
	 * Returns the actual logging level for this event handler
	 * @return the actual logging level for this event handler
	 * @see org.helios.tsdb.plugins.handlers.logging.LoggerManager#getLoggerLevel()
	 */
	public String getLoggerLevel() {
		return loggerManager.getLoggerLevel();
	}

	/**
	 * Returns the effective logging level for this event handler
	 * @return the effective logging level for this event handler
	 * @see org.helios.tsdb.plugins.handlers.logging.LoggerManager#getLoggerEffectiveLevel()
	 */
	public String getLoggerEffectiveLevel() {
		return loggerManager.getLoggerEffectiveLevel();
	}

	/**
	 * Sets the logger level for this event handler
	 * @param level The log level name
	 * @see org.helios.tsdb.plugins.handlers.logging.LoggerManager#setLoggerLevel(java.lang.String)
	 */
	public void setLoggerLevel(String level) {
		loggerManager.setLoggerLevel(level);
	}


}
