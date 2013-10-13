package org.helios.tsdb.plugins.event;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import net.opentsdb.core.TSDB;
import net.opentsdb.meta.Annotation;
import net.opentsdb.meta.TSMeta;
import net.opentsdb.meta.UIDMeta;
import net.opentsdb.search.SearchQuery;
import net.opentsdb.stats.StatsCollector;

import org.helios.tsdb.plugins.Constants;
import org.helios.tsdb.plugins.async.AsyncDispatcherExecutor;
import org.helios.tsdb.plugins.async.AsyncEventDispatcher;
import org.helios.tsdb.plugins.async.EventBusEventDispatcher;
import org.helios.tsdb.plugins.handlers.IEventHandler;
import org.helios.tsdb.plugins.handlers.IPublishEventHandler;
import org.helios.tsdb.plugins.handlers.ISearchEventHandler;
import org.helios.tsdb.plugins.shell.Publisher;
import org.helios.tsdb.plugins.shell.Search;
import org.helios.tsdb.plugins.util.ConfigurationHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.AbstractService;
import com.stumbleupon.async.Deferred;

/**
 * <p>Title: TSDBEventDispatcher</p>
 * <p>Description: The central event handler which the shell plugins dispatch events to.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.event.TSDBEventDispatcher</code></p>
 */
public class TSDBEventDispatcher {
	/** The singleton instance */
	private static volatile TSDBEventDispatcher instance = null;
	/** The singleton instance ctor lock */
	private static final Object lock = new Object();
	/** Instance logger */
	protected final Logger log = LoggerFactory.getLogger(getClass());
	/** The callback supplied TSDB instance */
	protected final TSDB tsdb;
	/** The asynch dispatcher */
	protected AsyncEventDispatcher asyncDispatcher;
	/** The asynch dispatcher's executor */
	protected Executor asyncExecutor;
	
	/** Indicates if event dispatcher configuration has started */
	protected final AtomicBoolean configured = new AtomicBoolean(false);
	
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
	
	/** The TSDB Configuration as properties */
	protected final Properties config;
	/**
	 * Called by TSDB to initialize the plugin Implementations are responsible for setting up any IO they need as well as starting any required background threads. Note: Implementations should throw exceptions if they can't start up properly. The TSD will then shutdown so the operator can fix the problem. Please use IllegalArgumentException for configuration issues.
	 * @param tsdb The parent TSDB object
	 * @see net.opentsdb.tsd.RTPublisher#initialize(net.opentsdb.core.TSDB)
	 */	
	protected void initialize(TSDB tsdb) {
		if(!configured.compareAndSet(false, true)) return;  // We only initialize once even if there are multiple shells.
		log.info("\n\t====================================\n\tConfiguring TSDBEventDispatcher\n\t====================================");
		String errMsg = null;
		searchEnabled = ConfigurationHelper.getBooleanSystemThenEnvProperty(Constants.CONFIG_ENABLE_SEARCH, false, config) &&  Search.class.getName().equals(ConfigurationHelper.getSystemThenEnvProperty(Constants.CONFIG_SEARCH_PLUGIN, null, config));
		publishEnabled = ConfigurationHelper.getBooleanSystemThenEnvProperty(Constants.CONFIG_ENABLE_PUBLISH, false, config) &&  Publisher.class.getName().equals(ConfigurationHelper.getSystemThenEnvProperty(Constants.CONFIG_PUBLISH_PLUGIN, null, config));
		log.info("\n\tCallback Plugins Enabled for EventDispatcher:\n\t\tSearch:{}\n\t\tPublish:{}\n", searchEnabled, publishEnabled);
		if(!searchEnabled && !publishEnabled) {
			errMsg = "Somehow, the TSDBEventDispatcher was initialized by neither the Search or Publish plugins are enabled.";
			log.error(errMsg);
			throw new IllegalArgumentException(errMsg);						
		}
		String eventHandlerNames = ConfigurationHelper.getSystemThenEnvProperty(Constants.EVENT_HANDLERS, null, config);
		String asyncDispatcherClassName = ConfigurationHelper.getSystemThenEnvProperty(Constants.ASYNC_DISPATCHER, Constants.DEFAULT_ASYNC_DISPATCHER, config);		
		
		if(eventHandlerNames==null || eventHandlerNames.trim().isEmpty()) {
			errMsg = "No event handler names configured in property [" + Constants.EVENT_HANDLERS + "]";
			log.error(errMsg);
			throw new IllegalArgumentException(errMsg);
		}
		loadHandlers(eventHandlerNames.trim());
		if(allHandlers.isEmpty()) {
			errMsg = "No event handlers were loaded.";
			log.error(errMsg);
			throw new IllegalArgumentException(errMsg);			
		}
		asyncExecutor = new AsyncDispatcherExecutor(config);
		loadAsyncDispatcher(asyncDispatcherClassName.trim());
		for(IEventHandler handler: allHandlers) {
			handler.initialize(tsdb);
		}
		asyncDispatcher.initialize(config, asyncExecutor, allHandlers);
		log.info("\n\t====================================\n\tTSDBEventPublisher Configuration Complete\n\t====================================");
	}
	
	/**
	 * Loads the async dispatcher
	 * @param asyncDispatcherClassName The async dispatcher class name
	 */
	protected void loadAsyncDispatcher(String asyncDispatcherClassName) {
		try {
			Class<?> _clazz = Class.forName(asyncDispatcherClassName, true, this.getClass().getClassLoader());
			if(!AsyncEventDispatcher.class.isAssignableFrom(_clazz)) {
				log.warn("The configured class [" + asyncDispatcherClassName + "] is not an AsyncEventDispatcher. Failing back to default");
				asyncDispatcher = new EventBusEventDispatcher();
				return;
			}
			Class<AsyncEventDispatcher> clazz = (Class<AsyncEventDispatcher>)_clazz;
			asyncDispatcher = clazz.newInstance();
		} catch (Exception ex) {
			log.warn("Failed to load AsyncDispatcher Class [" + asyncDispatcherClassName + "]\nDefault AsyncDispatcher will be loaded. Stack trace follows:", ex);
			asyncDispatcher = new EventBusEventDispatcher();
		}
	}
	
	/**
	 * Loads the configured event handlers
	 * @param eventHandlerNames A comma separated string of event handler class names
	 */
	protected void loadHandlers(String eventHandlerNames) {
		String[] classNames = eventHandlerNames.split(",");
		if(log.isDebugEnabled()) log.debug("EventHandler Class Names:" + Arrays.toString(classNames));
		Set<Class<IEventHandler>> handlerClasses = new HashSet<Class<IEventHandler>>(classNames.length);
		for(String className: classNames) {
			try {
				className = className.trim();
				
				Class<?> _clazz = Class.forName(className, true, this.getClass().getClassLoader());
				if(!IEventHandler.class.isAssignableFrom(_clazz)) {
					log.warn("The class [" + className + "] does not implement [" + IEventHandler.class.getName() + "]");
					continue;
				}
				Class<IEventHandler> clazz = (Class<IEventHandler>)_clazz; 
				if(!handlerClasses.add(clazz)) {
					log.warn("Duplicate Event Handler Instance [" + clazz.getName() + "]");
					continue;
				}
				if(!checkRequired(clazz)) {
					log.warn("The configured handler [{}] is not required", clazz.getName());
					continue;
				}
				IEventHandler eventHandler = clazz.newInstance();
				boolean installed = false;
				if(eventHandler instanceof IPublishEventHandler) {
					installed = true;
					publishHandlers.add((IPublishEventHandler)eventHandler);
					allHandlers.add(eventHandler);
					log.info("Loaded PublishEvent Handler [" + className + "]");
				}
				if(eventHandler instanceof ISearchEventHandler) {
					installed = true;
					searchHandlers.add((ISearchEventHandler)eventHandler);
					allHandlers.add(eventHandler);
					log.info("Loaded SearchEvent Handler [" + className + "]");
				}				
				if(!installed) {
					log.warn("The event handler [" + className + "] was not registered");
				}
			} catch (Exception ex) {
				log.error("Failed to load event handler: [" + className + "]", ex);
			}
		}
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
	 * Acquires the singleton instance
	 * @param tsdb The callback supplied TSDB instance
	 * @return the singleton instance
	 */
	public static TSDBEventDispatcher getInstance(TSDB tsdb) {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					final ClassLoader current = Thread.currentThread().getContextClassLoader();
					try {
						Thread.currentThread().setContextClassLoader(getSupportClassLoader(tsdb));
						instance = new TSDBEventDispatcher(tsdb);
					} finally {
						Thread.currentThread().setContextClassLoader(current);
					}
				}
			}
		}
		return instance;
	}
	
	/**
	 * Returns a classloader enabled to load jars, classes and resources 
	 * from the plugin support path defined in {@link Constants#CONFIG_PLUGIN_SUPPORT_PATH}.
	 * If no path is defined, the current thread's {@link Thread#getContextClassLoader()} is returned.
	 * @param tsdb The TSDB we're configuring for
	 * @return a classloader
	 */
	protected static ClassLoader getSupportClassLoader(TSDB tsdb) {
		ClassLoader DEFAULT = Thread.currentThread().getContextClassLoader();
		Properties p = new Properties();
		p.putAll(tsdb.getConfig().getMap());
		String[] supportClassPaths = ConfigurationHelper.getSystemThenEnvPropertyArray(Constants.CONFIG_PLUGIN_SUPPORT_PATH, "", p);
		if(supportClassPaths==null ||supportClassPaths.length==0) return DEFAULT;
		Set<URL> urls = new HashSet<URL>(supportClassPaths.length);		
		return new URLClassLoader(urls.toArray(new URL[urls.size()]), DEFAULT);
	}

	/**
	 * Creates a new TSDBEventDispatcher
	 * @param tsdb The callback supplied TSDB instance
	 */
	private TSDBEventDispatcher(TSDB tsdb) {
		this.tsdb = tsdb;		
		config = new Properties();
		config.putAll(tsdb.getConfig().getMap());
		initialize(tsdb);
	}
	


	/**
	 * Called by the TSD when a request for statistics collection has come in.
	 * @param statsCollector The collector used for emitting statistics
	 * @param pluginType The plugin type this collector was issued for
	 * @see net.opentsdb.search.SearchPlugin#collectStats(net.opentsdb.stats.StatsCollector)
	 */	
	public void collectStats(PluginType pluginType, StatsCollector statsCollector) {

	}
	
	/**
	 * Called any time a new data point is published
	 * @param metric The name of the metric associated with the data point
	 * @param timestamp Timestamp as a Unix epoch in seconds or milliseconds (depending on the TSD's configuration)
	 * @param value Value for the data point
	 * @param tags The metric tags
	 * @param tsuid Time series UID for the value
	 */
	public void publishDataPoint(String metric, long timestamp, double value, Map<String, String> tags, byte[] tsuid) {
		
	}

	/**
	 * Called any time a new data point is published
	 * @param metric The name of the metric associated with the data point
	 * @param timestamp Timestamp as a Unix epoch in seconds or milliseconds (depending on the TSD's configuration)
	 * @param value Value for the data point
	 * @param tags The metric tags
	 * @param tsuid Time series UID for the value
	 */
	public void publishDataPoint(String metric, long timestamp, long value, Map<String, String> tags, byte[] tsuid) {
		
	}
	

	
	/**
	 * Deletes an annotation
	 * @param annotation The annotation to delete
	 * @see net.opentsdb.search.SearchPlugin#deleteAnnotation(net.opentsdb.meta.Annotation)
	 */
	public void deleteAnnotation(Annotation annotation) {
		if(annotation!=null) {
			
		}
	}
	

	/**
	 * Indexes an annotation
	 * @param annotation The annotation to index
	 * @see net.opentsdb.search.SearchPlugin#indexAnnotation(net.opentsdb.meta.Annotation)
	 */
	public void indexAnnotation(Annotation annotation) {
		if(annotation!=null) {
			
		}
	}	

	/**
	 * Called when we need to remove a timeseries meta object from the engine Note: Unique Document ID = TSUID
	 * @param tsMeta The TSMeta name to delete
	 * @see net.opentsdb.search.SearchPlugin#deleteTSMeta(java.lang.String)
	 */
	public void deleteTSMeta(String tsMeta) {

	}
	
	/**
	 * Indexes a timeseries metadata object in the search engine Note: Unique Document ID = TSUID
	 * @param tsMeta The TSMeta to index
	 * @see net.opentsdb.search.SearchPlugin#indexTSMeta(net.opentsdb.meta.TSMeta)
	 */
	public void indexTSMeta(TSMeta tsMeta) {

	}	
	
	/**
	 * Indexes a UID metadata object for a metric, tagk or tagv Note: Unique Document ID = UID and the Type "TYPEUID"
	 * @param uidMeta The UIDMeta to index
	 * @see net.opentsdb.search.SearchPlugin#indexUIDMeta(net.opentsdb.meta.UIDMeta)
	 */
	public void indexUIDMeta(UIDMeta uidMeta) {

	}	

	/**
	 * Called when we need to remove a UID meta object from the engine Note: Unique Document ID = UID and the Type "TYPEUID"
	 * @param uidMeta The UIDMeta to delete
	 * @see net.opentsdb.search.SearchPlugin#deleteUIDMeta(net.opentsdb.meta.UIDMeta)
	 */
	public void deleteUIDMeta(UIDMeta uidMeta) {

	}

	/**
	 * 
	 * @see net.opentsdb.search.SearchPlugin#executeQuery(net.opentsdb.search.SearchQuery)
	 */
	public Deferred<SearchQuery> executeQuery(SearchQuery searchQuery) {
		Deferred<SearchQuery> defSearch = new Deferred<SearchQuery>();
		defSearch.callback(searchQuery);
		return defSearch;
	}

	





}
