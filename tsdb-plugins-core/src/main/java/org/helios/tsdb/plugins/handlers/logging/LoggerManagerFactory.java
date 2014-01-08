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

import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.LoggerFactory;

/**
 * <p>Title: LoggerManagerFactory</p>
 * <p>Description: Service to provide a means of setting logger levels for slf4j underlying loggers.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.handlers.logging.LoggerManagerFactory</code></p>
 */

public class LoggerManagerFactory {
	
	/** The known logger managers */
	public static final Map<String, String> LOGGER_MANAGERS;
	/** A cache of logger managers by name */
	private static final Map<String, LoggerManager> cacheByName = new ConcurrentHashMap<String, LoggerManager>();
	
	/** The selected logger manager ctor by String */
	private static volatile Constructor<LoggerManager> loggerManagerByStringCtor = null;
	/** The selected logger manager ctor by Class */
	private static volatile Constructor<LoggerManager> loggerManagerByClassCtor = null;
	/** The init lock */
	private static final Object lock = new Object();
	
	static {
		Map<String, String> tmp = new HashMap<String, String>();
		tmp.put("ch.qos.logback.classic.LoggerContext", "org.helios.tsdb.plugins.handlers.logging.ClassicLogbackLoggerManager");
		LOGGER_MANAGERS = Collections.unmodifiableMap(tmp);
	}
	
	private LoggerManagerFactory() {}
	
	private static void initialize() {
		if(loggerManagerByStringCtor==null) {
			synchronized(lock) {
				if(loggerManagerByStringCtor==null) {
					try {
						String factoryName = LoggerFactory.getILoggerFactory().getClass().getName();
						String className = LOGGER_MANAGERS.get(factoryName);
						if(className==null) {
							throw new Exception("No LoggerManager impl found for facotry [" + factoryName + "]");
						}
						Class<LoggerManager> clazz = (Class<LoggerManager>)Class.forName(className);
						loggerManagerByStringCtor = clazz.getDeclaredConstructor(String.class);
						loggerManagerByClassCtor = clazz.getDeclaredConstructor(Class.class);
					} catch (Exception ex) {
						throw new RuntimeException("Failed to initialize LoggerManagerFactory", ex);
					}					
				}
			}
		}
	}
	
	/**
	 * Returns a logger manager for the passed logger name
	 * @param loggerName The logger name to get a logger manager for
	 * @return the logger manager
	 */
	public static LoggerManager getLoggerManager(String loggerName) {
		LoggerManager lm = cacheByName.get(loggerName);
		if(lm==null) {
			synchronized(cacheByName) {
				lm = cacheByName.get(loggerName);
				if(lm==null) {
					initialize();
					try {
						lm = loggerManagerByStringCtor.newInstance(loggerName);
						cacheByName.put(loggerName, lm);
					} catch (Exception e) {
						throw new RuntimeException("Failed to create logger manager of type [" + loggerManagerByStringCtor.getDeclaringClass().getName() + "] for logger name [" + loggerName + "]", e);
					}					
				}
			}
		}
		return lm;
	}
	
	/**
	 * Returns a logger manager for the passed class
	 * @param clazz The class to get a logger manager for
	 * @return the logger manager
	 */
	public static LoggerManager getLoggerManager(Class<?> clazz) {
		return getLoggerManager(clazz.getName()); 
	}
	

}
