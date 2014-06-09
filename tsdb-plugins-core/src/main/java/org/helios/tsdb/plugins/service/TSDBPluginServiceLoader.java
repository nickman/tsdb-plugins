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
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import net.opentsdb.core.TSDB;
import net.opentsdb.core.UniqueIdRegistry;
import net.opentsdb.utils.Config;

import org.helios.tsdb.plugins.Constants;
import org.helios.tsdb.plugins.shell.Plugin;
import org.helios.tsdb.plugins.util.ConfigurationHelper;
import org.helios.tsdb.plugins.util.URLHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * <p>Title: TSDBPluginServiceLoader</p>
 * <p>Description: A loader utility to manage the location and classloading of the plugin service and related classes.</p> 
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
	/** The plugin context */
	protected final PluginContext pluginContext;

	/** Static class logger */
	private static final Logger LOG = LoggerFactory.getLogger(TSDBPluginServiceLoader.class);
	
	/**
	 * This is a testing hook.
	 */
	private static void reset() {
		System.err.println("\n\t***************************\n\tRESET PluginService\n\t***************************");
		if(instance!=null && instance.pluginService!=null) {
			
			try {
				Method m = instance.pluginService.getClass().getDeclaredMethod("reset");
				m.setAccessible(true);
				if(Modifier.isStatic(m.getModifiers())) {
					m.invoke(null);
				} else {
					m.invoke(instance.pluginService);
				}
				System.err.println("\n\tPlugin Service Instance Reset"); 
			} catch (Exception e) {}
		}
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
		plugin.setPluginContext(instance.pluginContext);
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
	 * Returns the loader instance
	 * @return the loader instance
	 */
	public static TSDBPluginServiceLoader getLoaderInstance() {
		return instance;
	}
	
	
	
	
	/**
	 * Creates a new TSDBPluginServiceLoader
	 * @param tsdb The core TSDB instance
	 */
	private TSDBPluginServiceLoader(TSDB tsdb) {
		this.tsdb = tsdb;
		UniqueIdRegistry.getInstance(this.tsdb);
		config = new Properties();
		config.putAll(this.tsdb.getConfig().getMap());
		supportClassLoader = getSupportClassLoader(tsdb.getConfig());
		pluginContext = new PluginContextImpl(this.tsdb, this.tsdb.getConfig(), config, supportClassLoader);
		String className = ConfigurationHelper.getSystemThenEnvProperty(Constants.PLUGIN_SERVICE_CLASS_NAME, Constants.DEFAULT_PLUGIN_SERVICE_CLASS_NAME, config);
		final ClassLoader ctx = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(supportClassLoader);
			Class<?> _clazz = Class.forName(className, true, supportClassLoader);
			//Class<?> _clazz = Class.forName(className);
			if(!ITSDBPluginService.class.isAssignableFrom(_clazz)) {
				throw new IllegalArgumentException("The class [" + className + "] does not implement ITSDBPluginService");
			}
			@SuppressWarnings("unchecked")
			Class<ITSDBPluginService> clazz = (Class<ITSDBPluginService>)_clazz;
			pluginService = loadService(clazz);
			pluginService.initialize();			
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
		return ConfigurationHelper.inst(clazz, new Class[] {PluginContext.class}, pluginContext);
	}
	/**
	 * Returns a classloader enabled to load jars, classes and resources 
	 * from the plugin support path defined in {@link Constants#CONFIG_PLUGIN_SUPPORT_PATH}.
	 * If no path is defined, the current thread's {@link Thread#getContextClassLoader()} is returned.
	 * @param tsdbConfig The TSDB config we're configuring for
	 * @return a classloader
	 */
	public static ClassLoader getSupportClassLoader(Config tsdbConfig) {
		ClassLoader DEFAULT = Thread.currentThread().getContextClassLoader();
		Properties p = new Properties();
		p.putAll(tsdbConfig.getMap());
		String[] supportClassPaths = ConfigurationHelper.getSystemThenEnvPropertyArray(Constants.CONFIG_PLUGIN_SUPPORT_PATH, "", p);		
		String corePluginPath = null;
		try {
			corePluginPath = tsdbConfig.getString("tsd.core.plugin_path");
		} catch (Exception ex) {}
		if(supportClassPaths==null ||supportClassPaths.length==0) return DEFAULT;
		LOG.info("Plugin Support Classpaths:[{}]", Arrays.toString(supportClassPaths));
		Set<URL> urls = new HashSet<URL>(supportClassPaths.length);
		for(String path: supportClassPaths) {
			if(path==null || path.trim().isEmpty()) continue;
			path = path.trim();
			LOG.info("Plugin Support Path: [{}]", path);
			File f = new File(path);
			if(f.exists() && f.isDirectory()) {
				LOG.info("Plugin Support Directory: [{}]", path);
				for(File jar: f.listFiles()) {
					if(jar.getName().toLowerCase().endsWith(".jar")) {
						LOG.info("Plugin Support JAR: [{}]", path);
						urls.add(URLHelper.toURL(jar));
					}
				}
			}
			if(URLHelper.isValidURL(path)) {
				urls.add(URLHelper.toURL(path));
				continue;
			}
			
			if(f.exists()) {
				urls.add(URLHelper.toURL(f));
				continue;
			} 
		}
		if(corePluginPath!=null) {
			File corePluginPathFile = new File(corePluginPath);
			if(corePluginPathFile.exists() && corePluginPathFile.isDirectory()) {
				for(File f: corePluginPathFile.listFiles()) {
					if(f.getName().toLowerCase().endsWith(".jar")) {
						urls.add(URLHelper.toURL(f));
					}
				}
			}
		}
		if(urls.isEmpty()) return DEFAULT;
		URLClassLoader classLoader = new URLClassLoader(urls.toArray(new URL[urls.size()]), DEFAULT); 
		StringBuilder b = new StringBuilder("\n\t===============================\n\tTSDB Plugin Support Classpath\n\t===============================");
		for(URL url: urls) {
			b.append("\n\t").append(url);
		}
		b.append("\n\t===============================\n\tClassLoader:").append(classLoader).append("\n\t===============================");
		LOG.info(b.toString());
		return classLoader;
	}

	/**
	 * Returns the created plugin service (which might be null if it has not been initialized yet)
	 * @return the created plugin service
	 */
	public ITSDBPluginService getPluginService() {
		return pluginService;
	}
	

	
	/**
	 * Returns the provided TSDB (which might be null if it has not been initialized yet)
	 * @return the provided TSDB
	 */
	public TSDB getTSDB() {
		return tsdb;
	}
	
	/**
	 * Returns the plugin context
	 * @return the plugin context
	 */
	public PluginContext getPluginContext() {
		return pluginContext;
	}
	


}
