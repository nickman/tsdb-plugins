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
package org.helios.tsdb.plugins.service;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import net.opentsdb.core.TSDB;
import net.opentsdb.search.SearchPlugin;
import net.opentsdb.tsd.RTPublisher;
import net.opentsdb.tsd.RpcPlugin;

import org.helios.tsdb.plugins.Constants;
import org.helios.tsdb.plugins.shell.Plugin;
import org.helios.tsdb.plugins.util.ConfigurationHelper;
import org.helios.tsdb.plugins.util.URLHelper;

/**
 * <p>Title: TSDBPluginServiceLoader</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.service.TSDBPluginServiceLoader</code></p>
 */

public class TSDBPluginServiceLoader {
	/** Singleton instance */
	private static volatile TSDBPluginServiceLoader instance = null;
	/** Singleton instance ctor lock */
	private static final Object lock = new Object();
	/** The core TSDB instance */
	protected final TSDB tsdb;
	/** The core TSDB configuration */
	protected final Properties config;
	/** The plugin service instance */
	protected final ITSDBPluginService pluginService;
	/** The plugin service support classpath */
	protected final ClassLoader supportClassLoader;
	
	private static void reset() {
		instance = null;		
	}
	
	/**
	 * Acquires the ITSDBPluginService singleton instance
	 * @param tsdb The core TSDB instance
	 * @param plugin The plugin to register
	 * @return the ITSDBPluginService singleton instance
	 */
	public static ITSDBPluginService getInstance(TSDB tsdb, Plugin plugin) {
		if(tsdb==null) throw new IllegalArgumentException("The passed TSDB instance was null");		
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new TSDBPluginServiceLoader(tsdb);
				}
			}
		}
		instance.pluginService.addPluginInstance(plugin);
		return instance.pluginService;
	}
	
	/**
	 * Returns the singleton instance or throws if not initialized
	 * @return the singleton instance 
	 */
	public static ITSDBPluginService getInstance() {
		if(instance==null) throw new IllegalStateException("The TSDBPluginServiceLoader is not initialized");
		if(instance.pluginService==null) throw new IllegalStateException("The ITSDBPluginService is not initialized");
		return instance.pluginService;
	}
	

	
	
	/**
	 * Creates a new TSDBPluginServiceLoader
	 * @param tsdb The core TSDB instance
	 */
	private TSDBPluginServiceLoader(TSDB tsdb) {
		this.tsdb = tsdb;
		config = new Properties();
		config.putAll(this.tsdb.getConfig().getMap());
		supportClassLoader = getSupportClassLoader(tsdb);
		String className = ConfigurationHelper.getSystemThenEnvProperty(Constants.PLUGIN_SERVICE_CLASS_NAME, Constants.DEFAULT_PLUGIN_SERVICE_CLASS_NAME, config);
		final ClassLoader ctx = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(supportClassLoader);
			Class<?> _clazz = Class.forName(className, true, supportClassLoader);
			if(!ITSDBPluginService.class.isAssignableFrom(_clazz)) {
				throw new IllegalArgumentException("The class [" + className + "] does not implement ITSDBPluginService");
			}
			@SuppressWarnings("unchecked")
			Class<ITSDBPluginService> clazz = (Class<ITSDBPluginService>)_clazz;
			pluginService = loadService(clazz);
		} catch (IllegalArgumentException iae) {
			throw iae;
		} catch (Exception ex) {
			throw new IllegalArgumentException("Failed to load PluginService [" + className + "]", ex);
		} finally {
			Thread.currentThread().setContextClassLoader(ctx);
		}
	}
	
	/**
	 * Loads an instance of the passed {@link ITSDBPluginService} implementation class
	 * @param clazz a {@link ITSDBPluginService} implementation class
	 * @return the new {@link ITSDBPluginService} instance
	 * @throws Exception thrown on any error
	 */
	protected ITSDBPluginService loadService(Class<ITSDBPluginService> clazz) throws Exception {
		try {
			Constructor<ITSDBPluginService> ctor = clazz.getDeclaredConstructor(TSDB.class, Properties.class);
			return ctor.newInstance(tsdb, config);
		} catch (NoSuchMethodException nse) {
			Method method = clazz.getDeclaredMethod("newInstance", TSDB.class, Properties.class);
			if(!Modifier.isStatic(method.getModifiers())) {
				throw new Exception(String.format("Could not find constructor [%s(TSDB, Properties)] or static factory method [%s.newInstance(TSDB, Properties)] for plugin service [%s]", clazz.getSimpleName(), clazz.getSimpleName(), clazz.getName()));
			}
			return (ITSDBPluginService)method.invoke(null, tsdb, config);
		}
		
	}
	/**
	 * Returns a classloader enabled to load jars, classes and resources 
	 * from the plugin support path defined in {@link Constants#CONFIG_PLUGIN_SUPPORT_PATH}.
	 * If no path is defined, the current thread's {@link Thread#getContextClassLoader()} is returned.
	 * @param tsdb The TSDB we're configuring for
	 * @return a classloader
	 */
	public static ClassLoader getSupportClassLoader(TSDB tsdb) {
		ClassLoader DEFAULT = Thread.currentThread().getContextClassLoader();
		Properties p = new Properties();
		p.putAll(tsdb.getConfig().getMap());
		String[] supportClassPaths = ConfigurationHelper.getSystemThenEnvPropertyArray(Constants.CONFIG_PLUGIN_SUPPORT_PATH, "", p);
		
		if(supportClassPaths==null ||supportClassPaths.length==0) return DEFAULT;
		Set<URL> urls = new HashSet<URL>(supportClassPaths.length);
		for(String path: supportClassPaths) {
			if(path==null || path.trim().isEmpty()) continue;
			path = path.trim();
			if(URLHelper.isValidURL(path)) {
				urls.add(URLHelper.toURL(path));
				continue;
			}
			File f = new File(path);
			if(f.exists()) {
				urls.add(URLHelper.toURL(f));
				continue;
			} 
		}
		if(urls.isEmpty()) return DEFAULT;
		return new URLClassLoader(urls.toArray(new URL[urls.size()]), DEFAULT);
	}

	/**
	 * Returns 
	 * @return the pluginService
	 */
	public ITSDBPluginService getPluginService() {
		return pluginService;
	}


}
