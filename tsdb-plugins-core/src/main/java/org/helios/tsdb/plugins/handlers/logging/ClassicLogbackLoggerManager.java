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
package org.helios.tsdb.plugins.handlers.logging;

import java.util.Arrays;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.util.ContextSelectorStaticBinder;

/**
 * <p>Title: ClassicLogbackLoggerManager</p>
 * <p>Description: LoggerManager for Logback loggers</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.handlers.logging.ClassicLogbackLoggerManager</code></p>
 */

public class ClassicLogbackLoggerManager implements LoggerManager {
	/** The managed logger */
	protected final Logger logger;
	
	/** The central logger context */
	private static final LoggerContext loggerContext = ContextSelectorStaticBinder.getSingleton().getContextSelector().getLoggerContext();
	
	/** The supported logger levels */
	private static final String[] LEVEL_NAMES = {"OFF", "ERROR", "WARN", "INFO", "DEBUG", "TRACE", "ALL"};
	
	static {
		Arrays.sort(LEVEL_NAMES);
	}
	
	/**
	 * Creates a new ClassicLogbackLoggerManager
	 * @param loggerName The logger name of the logger to manage
	 */
	public ClassicLogbackLoggerManager(String loggerName) {
		logger = loggerContext.getLogger(loggerName);
	}
	
	/**
	 * Creates a new ClassicLogbackLoggerManager
	 * @param clazz The clazz that the managed logger logs for 
	 */
	public ClassicLogbackLoggerManager(Class<?> clazz) {
		logger = loggerContext.getLogger(clazz);
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.handlers.logging.LoggerManager#getLoggerLevel()
	 */
	@Override
	public String getLoggerLevel() {
		Level level = logger.getLevel();		
		return level!=null ? level.toString() : null;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.handlers.logging.LoggerManager#getLoggerEffectiveLevel()
	 */
	@Override
	public String getLoggerEffectiveLevel() {		
		return logger.getEffectiveLevel().toString();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.handlers.logging.LoggerManager#setLoggerLevel(java.lang.String)
	 */
	@Override
	public void setLoggerLevel(String level) {
		if(Arrays.binarySearch(LEVEL_NAMES, level)<0) {
			throw new RuntimeException("Invalid level for Classic Logback [" + level + "]. Valid levels are " + Arrays.toString(LEVEL_NAMES));
		}
		logger.setLevel(Level.toLevel(level));
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.handlers.logging.LoggerManager#getLevelNames()
	 */
	@Override
	public String[] getLevelNames() {
		return LEVEL_NAMES;
	}

}
