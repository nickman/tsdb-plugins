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

import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import net.opentsdb.core.TSDB;
import net.opentsdb.meta.Annotation;
import net.opentsdb.meta.TSMeta;
import net.opentsdb.meta.UIDMeta;
import net.opentsdb.search.SearchPlugin;
import net.opentsdb.search.SearchQuery;
import net.opentsdb.stats.StatsCollector;
import net.opentsdb.tsd.RTPublisher;
import net.opentsdb.tsd.RpcPlugin;

import org.helios.tsdb.plugins.Constants;
import org.helios.tsdb.plugins.event.PluginType;
import org.helios.tsdb.plugins.handlers.IEventHandler;
import org.helios.tsdb.plugins.handlers.IPublishEventHandler;
import org.helios.tsdb.plugins.handlers.ISearchEventHandler;
import org.helios.tsdb.plugins.rpc.IRPCService;
import org.helios.tsdb.plugins.shell.Plugin;
import org.helios.tsdb.plugins.shell.Publisher;
import org.helios.tsdb.plugins.shell.RpcService;
import org.helios.tsdb.plugins.shell.Search;
import org.helios.tsdb.plugins.util.ConfigurationHelper;
import org.helios.tsdb.plugins.util.SystemClock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stumbleupon.async.Deferred;

/**
 * <p>Title: AbstractTSDBPluginService</p>
 * <p>Description: A base abstract {@link ITSDBPluginService}</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.service.AbstractTSDBPluginService</code></p>
 */

public abstract class AbstractTSDBPluginService implements ITSDBPluginService, Runnable {
	/** Instance logger */
	protected final Logger log = LoggerFactory.getLogger(getClass());
	/** A map of plugins keyed by the base plugin type */
	protected final Map<PluginType, Plugin> plugins = new EnumMap<PluginType, Plugin>(PluginType.class);
	/** The callback supplied TSDB instance */
	protected final TSDB tsdb;
	/** The TSDB instance extracted config */
	protected final Properties config;
	/** The plugin service context */
	protected PluginContext pluginContext = null;
	
	/** The plugin support classloader */
	protected ClassLoader supportClassLoader = null;
	
	/** Indicates if event dispatcher configuration has started */
	protected final AtomicBoolean configured = new AtomicBoolean(false);
	/** Indicates if event dispatcher is shutdown */
	protected final AtomicBoolean shutdown = new AtomicBoolean(false);
	
	/** Indicates if search is enabled AND the configured search plugin is shell.Search */
	protected boolean searchEnabled = false;
	/** Indicates if publish is enabled AND the configured search plugin is shell.Publish */
	protected boolean publishEnabled = false;
	
	/** A set of registered publish event handlers */
	protected final Set<IPublishEventHandler> publishHandlers = new CopyOnWriteArraySet<IPublishEventHandler>();
	/** A set of registered search event handlers */
	protected final Set<ISearchEventHandler> searchHandlers = new CopyOnWriteArraySet<ISearchEventHandler>();
	/** A set of all registered event handlers */
	protected final Set<IEventHandler> allHandlers = new CopyOnWriteArraySet<IEventHandler>();
	/** A set of all registered RPC services */
	protected final Set<IRPCService> rpcServices = new CopyOnWriteArraySet<IRPCService>();
	
	/** The startup/shutdown counter */
	protected final AtomicInteger startupShutdownCount = new AtomicInteger();
	/** The shutdown deferred returned on each shutdown request, completed when the startupShutdownCount is decremented to zero */
	protected final Deferred<Object> shutdownDeferred = new Deferred<Object>();
	
	/** Indicates if the query handling search handler has been assigned */
	protected boolean primarySearchSet = false;
	
	/** Stats collector scheduler */
	protected final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2, new ThreadFactory(){
		protected final AtomicInteger  _serial = new AtomicInteger();
		@Override
		public Thread newThread(Runnable r) {
			Thread t = new Thread(r, "TSDBPluginStatsCollector#" + _serial.incrementAndGet());
			t.setDaemon(true);
			return t;
		}		
	});
	
	/** The schedule handle for the stats collector */
	protected ScheduledFuture<?> scheduleHandle = null;

	/**
	 * Creates a new AbstractTSDBPluginService
	 * @param pc The plugin context
	 */
	protected AbstractTSDBPluginService(PluginContext pc) {
		this.pluginContext = pc;
		this.tsdb = pc.getTsdb();
		this.config = pc.getExtracted();
		this.supportClassLoader = pc.getSupportClassLoader();
		log.info("Created TSDBPluginService [{}]", getClass().getName());
	}
	

	
	/**
	 * Abandons a graceful shutdown and forces it. Used when a startup/initialize fails and we need
	 * to backout any startup that needs to be cleaned up.
	 * @param cause The cause of the forced shutdown
	 */
	protected void forceShutdown(Throwable cause) {
		if(cause!=null) {
			log.error("Forced shutdown called. Exception follows:", cause);
		} else {
			log.error("Forced shutdown called.");
		}
		startupShutdownCount.set(0);
		shutdown(null);
		log.error("Forced shutdown complete");
	}
	
	/**
	 * Stops the event dispatcher and all subsidiary services
	 */
	public Deferred<Object>  shutdown(Deferred<Object> deferredToAdd) {
		if(scheduleHandle!=null) {
			scheduleHandle.cancel(false);
			scheduleHandle = null;
		}
		if(deferredToAdd!=null) {
			shutdownDeferred.chain(deferredToAdd);
		}
		final int shutdownCount = startupShutdownCount.decrementAndGet();
		if(shutdownCount>0) {
			log.info("Deferred Shutdown Request. Count:{}", shutdownCount);
			return shutdownDeferred;
		}
		log.info("Final Shutdown Request");
		if(!shutdown.compareAndSet(false, true)) {
			log.warn("Shutdown already final. Unexpected State");
			return shutdownDeferred;
		}
		log.info("\n\t====================================\n\tStopping PluginService [{}]\n\t====================================", getClass().getSimpleName());
		doPreShutdown();
		log.info("Stopping handlers");
		for(IEventHandler ie: allHandlers) {
			ie.shutdown();
		}		
		allHandlers.clear();
		searchHandlers.clear();
		publishHandlers.clear();
		log.info("All handlers stopped");
		log.info("Stopping RPCServices");
		for(IRPCService rpcService: rpcServices) {
			rpcService.stopAndWait();
		}
		rpcServices.clear();
		plugins.clear();
		log.info("All RPCServices stopped");
		doPostShutdown();
		log.info("\n\t====================================\n\tStopped PluginService [{}]\n\t====================================", getClass().getSimpleName());
		shutdownDeferred.callback(true);
		return shutdownDeferred;
	}
	
	/**
	 * The plugin service impl specific shutdown, called before handlers have been stopped and cleared.
	 */
	protected void doPreShutdown(){};

	/**
	 * The plugin service impl specific shutdown, called after handlers have been stopped and cleared.
	 */
	protected void doPostShutdown(){};
	
	/**
	 * Initialize the plugin handlers that are responsible for setting up any IO they need as well as starting any required background threads. Note: Implementations should throw exceptions if they can't start up properly. The TSD will then shutdown so the operator can fix the problem. Please use IllegalArgumentException for configuration issues.
	 */
	public void initialize() {
		try {
			if(!configured.compareAndSet(false, true)) return;  // We only initialize once even if there are multiple shells.
			log.info("\n\t====================================\n\tConfiguring Plugin Service [{}]\n\t====================================", getClass().getSimpleName()); 
			searchEnabled = ConfigurationHelper.getBooleanSystemThenEnvProperty(Constants.CONFIG_ENABLE_SEARCH, false, config) &&  Search.class.getName().equals(ConfigurationHelper.getSystemThenEnvProperty(Constants.CONFIG_SEARCH_PLUGIN, null, config));
			publishEnabled = ConfigurationHelper.getBooleanSystemThenEnvProperty(Constants.CONFIG_ENABLE_PUBLISH, false, config) &&  Publisher.class.getName().equals(ConfigurationHelper.getSystemThenEnvProperty(Constants.CONFIG_PUBLISH_PLUGIN, null, config));
			log.info("\n\tCallback Plugins Enabled for EventDispatcher:\n\t\tSearch:{}\n\t\tPublish:{}\n", searchEnabled, publishEnabled);
			doInitialize();
			if(!ConfigurationHelper.getBooleanSystemThenEnvProperty(Constants.PLUGIN_ENABLE_STATS_POLLING, Constants.DEFAULT_PLUGIN_ENABLE_STATS_POLLING, config)) {
				scheduleHandle = scheduler.scheduleWithFixedDelay(this, 15, 15, TimeUnit.SECONDS);
				log.info("Started Stats Collector Scheduling");
			}
			log.info("\n\t====================================\n\tPluginService [{}] Configuration Complete\n\t====================================", getClass().getSimpleName());
		} catch (Exception ex) {
			forceShutdown(ex);
			if(ex instanceof IllegalArgumentException) {
				throw (IllegalArgumentException)ex;
			}
			throw new IllegalArgumentException("Initialization failed", ex);			
		}
	}
	
	/**
	 * Executes stats collection 
	 */
	public void run() {
		log.info("Collecting...");
		StatsCollectorImpl collector = new StatsCollectorImpl(tsdb, true);
		try {			
			tsdb.collectStats(collector);
			collector.clear();
			for(ISearchEventHandler handler: searchHandlers) {
				handler.collectStats(collector);
			}
			collector.restore();
			collector.clear();
			for(IPublishEventHandler handler: publishHandlers) {
				handler.collectStats(collector);
			}
			collector.restore();
			collector.clear();			
			for(IRPCService handler: rpcServices) {
				handler.collectStats(collector);
			}
			collector.restore();
			log.info("Collect done.");
		} catch (Exception ex) {
			log.error("Stats Collection Exception", ex);
		}
	}
	
	private static class StatsCollectorImpl extends StatsCollector {
		/** Static class logger */
		private static final Logger LOG = LoggerFactory.getLogger("StatsCollection");
		/** Indicates if collected metrics should be recorded to the TSDB */
		private final boolean relay;
		/** The parent TSDB instance */
		private final TSDB tsdb;
		
		/** The tag backup/restore stack */
		private final Stack<Map<String, String>> tagStack = new Stack<Map<String, String>>(); 
		
		
		
		public StatsCollectorImpl(TSDB tsdb, boolean relay) {
			super("tsd");
			this.tsdb = tsdb;
			this.relay = relay;
		}
		
		/**
		 * Pushes the extra tags onto the tag stack and clears the current map
		 */
		public void clear() {
			if(extratags==null) extratags = new HashMap<String, String>();
			tagStack.push(new HashMap<String, String>(extratags));
			extratags.clear();
		}
		
		/**
		 * Restores the current extratags from the tagstack
		 */
		public void restore() {
			extratags.clear();
			extratags.putAll(tagStack.pop());
		}
		
		/**
		 * Splits an xtraTag into a name/value pair array
		 * @param xtraTag The xtraTag to split
		 * @return a name/value pair array
		 */
		public String[] splitXtraTag(String xtraTag) {
			int index = xtraTag.indexOf('=');
			if(index==-1) throw new IllegalArgumentException("The xtraTag [" + xtraTag + "] is not a tag pair");
			String[] pair = new String[2];
			pair[0] = xtraTag.substring(0, index).trim();
			pair[1] = xtraTag.substring(index+1).trim();
			return pair;
		}
		
		@Override
		public void emit(String datapoint) {
			if(LOG.isDebugEnabled()) 
				LOG.debug(datapoint.replace("\n", ""));			
		}
		@Override
		public void record(String name, long value, String xtratag) {	
			try {
				super.record(name, value, xtratag);
				if(relay) {
					String[] tg = null;
					if(xtratag!=null) {
						tg = splitXtraTag(xtratag);
						this.addExtraTag(tg[0], tg[1]);
					}
					tsdb.addPoint(name, SystemClock.unixTime(), value, this.extratags);
					if(tg!=null) {
						this.clearExtraTag(tg[1]);					
					}				
				}
			} catch (Exception ex) {
				
			}
		}		
		
		
	}
	
	/**
	 * Performs the plugin service impl specific initialization 
	 */
	protected void doInitialize() {
		/* No Op */
	}
	
	
	
	/**
	 * Loads the configured event handlers
	 * @param eventHandlerNames A comma separated string of event handler class names
	 */
	protected void loadHandlers(String eventHandlerNames) {
		String[] classNames = eventHandlerNames.split(",");
		if(log.isDebugEnabled()) log.debug("EventHandler Class Names:" + Arrays.toString(classNames));
		log.info("Starting EventHandler Configuration. Loading classes with classloader [{}]", supportClassLoader);
		Set<Class<IEventHandler>> handlerClasses = new HashSet<Class<IEventHandler>>(classNames.length);
		for(String className: classNames) {
			try {
				className = className.trim();
				
				Class<?> _clazz = Class.forName(className, true, supportClassLoader);
				//Class<?> _clazz = Class.forName(className);
				if(!IEventHandler.class.isAssignableFrom(_clazz)) {
					log.warn("The class [" + className + "] does not implement [" + IEventHandler.class.getName() + "]");
					continue;
				}
				@SuppressWarnings("unchecked")
				Class<IEventHandler> clazz = (Class<IEventHandler>)_clazz; 
				if(!handlerClasses.add(clazz)) {
					log.warn("Duplicate Event Handler Instance [" + clazz.getName() + "]");
					continue;
				}
				if(!checkRequired(clazz)) {
					log.warn("The configured handler [{}] is not required", clazz.getName());
					continue;
				}
				IEventHandler eventHandler = ConfigurationHelper.inst(clazz);
				boolean installed = false;
				if(eventHandler instanceof IPublishEventHandler) {
					installed = true;
					publishHandlers.add((IPublishEventHandler)eventHandler);
					allHandlers.add(eventHandler);
					log.info("Loaded PublishEvent Handler [" + className + "]");
				}
				if(eventHandler instanceof ISearchEventHandler) {
					installed = true;
					if(!primarySearchSet) {
						((ISearchEventHandler)eventHandler).setExecuteSearchEnabled(true);
						primarySearchSet = true;
					} else {
						((ISearchEventHandler)eventHandler).setExecuteSearchEnabled(false);
					}
					searchHandlers.add((ISearchEventHandler)eventHandler);
					allHandlers.add(eventHandler);
					log.info("Loaded SearchEvent Handler [{}] from ClassLoader [{}]", className, eventHandler.getClass().getClassLoader());
				}				
				if(!installed) {
					log.warn("The event handler [" + className + "] was not registered");
				}
			} catch (Exception ex) {
				log.error("Failed to load event handler: [" + className + "]", ex);
			}
		}
	}

	/** The ctor or factory signature for IRPCServices */
	private static final Class<?>[] RPC_SVC_SIG = {TSDB.class, Properties.class};

	/**
	 * Loads RPC services defined in {@link Constants#CONFIG_RPC_SERVICES} if the shell was defined in {@link Constants#CONFIG_RPC_PLUGINS}. 
	 */
	protected void loadRPCServices() {		
		log.info("Loading RPC Services");
		String[] rpcServiceClassNames = ConfigurationHelper.getSystemThenEnvPropertyArray(Constants.CONFIG_RPC_SERVICES, "", config);
		if(rpcServiceClassNames!=null && rpcServiceClassNames.length>0) {
			for(String className: rpcServiceClassNames) {
				if(className==null || className.trim().isEmpty()) continue;
				try {
					Class<?> _clazz = Class.forName(className);
					if(!IRPCService.class.isAssignableFrom(_clazz)) {
						throw new Exception("The class [" + className + "] does not implement [" + IRPCService.class.getName() + "]");
					}
					@SuppressWarnings("unchecked")
					Class<IRPCService> clazz = (Class<IRPCService>)_clazz;
					rpcServices.add(ConfigurationHelper.inst(clazz, RPC_SVC_SIG, tsdb, config));
					log.info("Configured RPC Service [{}]", className);
				} catch (Exception ex) {
					throw new IllegalArgumentException("Failed to load configured RPC Service [" + className + "]", ex);
				}
			}
			
		}
		
		log.info("Loaded [{}] RPC Services", rpcServices.size());
		if(!rpcServices.isEmpty()) {
			for(IRPCService rpc: rpcServices) {
				rpc.startAndWait();
				log.info("Started [{}]", rpc.getClass().getSimpleName());
			}
		}
		log.info("Started [{}] RPC Services", rpcServices.size());
	}
	
	/**
	 * Ensures that this event handler is required based on the enabled services
	 * @param clazz The handler class to test
	 * @return true if required, false otherwise
	 */
	protected boolean checkRequired(Class<IEventHandler> clazz) {
		return (
				(ISearchEventHandler.class.isAssignableFrom(clazz) && searchEnabled)
				|| 
				(IPublishEventHandler.class.isAssignableFrom(clazz) && publishEnabled)
		);
	}

	/**
	 * Called by the TSD when a request for statistics collection has come in.
	 * @param statsCollector The collector used for emitting statistics
	 * @param pluginType The plugin type this collector was issued for
	 * @see net.opentsdb.search.SearchPlugin#collectStats(net.opentsdb.stats.StatsCollector)
	 */	
	public void collectStats(PluginType pluginType, StatsCollector statsCollector) {
		switch(pluginType) {
		case PUBLISH:
			for(IEventHandler handler: publishHandlers) {
				handler.collectStats(statsCollector);
			}
			break;
		case RPC:
			for(IRPCService rpcService: rpcServices) {
				rpcService.collectStats(statsCollector);
			}			
			break;
		case SEARCH:
			for(IEventHandler handler: searchHandlers) {
				handler.collectStats(statsCollector);
			}			
			break;
		default:
			break;
			
		}
	}	
	
	
//	/**
//	 * Test hook to reset event dispatcher
//	 */
//	@SuppressWarnings("unused")
//	private void reset() {
//		shutdown();
//		instance = null;
//		configured.set(false);
//		shutdown.set(false);
//		log.warn("\n\t****************************\n\tTSDBEventDispatcher RESET\n\t****************************\n");
//	}
	
	

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.service.ITSDBPluginService#addPluginInstance(org.helios.tsdb.plugins.shell.Plugin)
	 */
	@Override
	public void addPluginInstance(Plugin plugin) {
		try {
			if(plugin==null) throw new IllegalArgumentException("The passed plugin instance was null");
			PluginType pluginType =  plugin.getPluginType();
			log.info("Adding [{}] plugin instance [{}]", pluginType.name(), plugin.getClass().getName());
			Plugin existingPlugin = plugins.get(pluginType);
			
			if(existingPlugin!=null) {
				throw new IllegalArgumentException("Cannot register [" + plugin.getClass().getName() + "]. A plugin of type [" + pluginType.name() + "] has alreayd been registered"); 
			}
			plugins.put(plugin.getPluginType(), plugin);
			if(plugin instanceof RpcService) {
				loadRPCServices();
			}
			startupShutdownCount.incrementAndGet();
		} catch (Exception ex) {
			forceShutdown(ex);
			if(ex instanceof IllegalArgumentException) {
				throw (IllegalArgumentException)ex;
			} else {
				throw new IllegalArgumentException("Initialization failed", ex);
			}
		}			
	}
	
	/**
	 * Returns the plugin base class type of the passed plugin instance
	 * @param plugin The plugin instance
	 * @return the plugin base class 
	 */
	protected Class<?> pluginKey(Plugin plugin) {
		if(plugin instanceof SearchPlugin) {
			return SearchPlugin.class;
		} else if(plugin instanceof RTPublisher) {
			return RTPublisher.class;
		} else if(plugin instanceof RpcPlugin) {
			return RpcPlugin.class;
		} else {
			throw new IllegalArgumentException("The class [" + plugin.getClass() + "] does not implement any plugin abstract");
		}
	}
	


	@Override
	public void executeQuery(SearchQuery searchQuery, Deferred<SearchQuery> toReturn) {
	}

}
