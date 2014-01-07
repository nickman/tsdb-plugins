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

/**
 * <p>Title: LoggerManager</p>
 * <p>Description: Defines a class that can set the logger level for slf4j underlying loggers</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.handlers.logging.LoggerManager</code></p>
 */

public interface LoggerManager {
	/**
	 * Returns the logger's actual level
	 * @return the logger's actual level
	 */
	public String getLoggerLevel();
	/**
	 * Returns the logger's effective level
	 * @return the logger's effective level
	 */
	public String getLoggerEffectiveLevel();
	/**
	 * Sets the logger's level
	 * @param level The level to set the logger to
	 */
	public void setLoggerLevel(String level);
	/**
	 * Returns the names of the levels supported by this logger
	 * @return the names of the levels supported by this logger
	 */
	public String[] getLevelNames();
	
}
