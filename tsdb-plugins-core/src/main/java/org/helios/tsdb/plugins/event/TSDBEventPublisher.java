package org.helios.tsdb.plugins.event;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import net.opentsdb.core.TSDB;
import net.opentsdb.meta.Annotation;
import net.opentsdb.meta.TSMeta;
import net.opentsdb.meta.UIDMeta;
import net.opentsdb.search.SearchQuery;
import net.opentsdb.stats.StatsCollector;

import org.helios.tsdb.plugins.Constants;
import org.helios.tsdb.plugins.async.AsyncEventDispatcher;
import org.helios.tsdb.plugins.handlers.IEventHandler;
import org.helios.tsdb.plugins.handlers.IPublishEventHandler;
import org.helios.tsdb.plugins.handlers.ISearchEventHandler;
import org.helios.tsdb.plugins.util.ConfigurationHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.AbstractService;
import com.stumbleupon.async.Deferred;

/**
 * <p>Title: TSDBEventPublisher</p>
 * <p>Description: The central event handler which the shell plugins dispatch events to.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.event.TSDBEventPublisher</code></p>
 */
public class TSDBEventPublisher extends AbstractService {
	/** The singleton instance */
	private static volatile TSDBEventPublisher instance = null;
	/** The singleton instance ctor lock */
	private static final Object lock = new Object();
	/** Instance logger */
	protected final Logger log = LoggerFactory.getLogger(getClass());
	/** The callback supplied TSDB instance */
	protected final TSDB tsdb;
	/** The asynch dispatcher */
	protected AsyncEventDispatcher async;
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
	public void initialize(TSDB tsdb) {
		log.info("\n\t====================================\n\tConfiguring TSDBEventPublisher\n\t====================================");
		String eventHandlerNames = ConfigurationHelper.getSystemThenEnvProperty(Constants.EVENT_HANDLERS, null, config);
		String asyncDispatcherClassName = ConfigurationHelper.getSystemThenEnvProperty(Constants.ASYNC_DISPATCHER, Constants.DEFAULT_ASYNC_DISPATCHER, config);		
		String errMsg = null;
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
		loadAsyncDispatcher(asyncDispatcherClassName.trim());
		log.info("\n\t====================================\n\tTSDBEventPublisher Configuration Complete\n\t====================================");
	}
	
	/**
	 * Loads the async dispatcher
	 * @param asyncDispatcherClassName The async dispatcher class name
	 */
	protected void loadAsyncDispatcher(String asyncDispatcherClassName) {
		
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
	 * {@inheritDoc}
	 * @see com.google.common.util.concurrent.AbstractService#doStart()
	 */
	@Override
	protected void doStart() {
		
	}

	/**
	 * {@inheritDoc}
	 * @see com.google.common.util.concurrent.AbstractService#doStop()
	 */
	@Override
	protected void doStop() {
		
	}
	
	/**
	 * Acquires the singleton instance
	 * @param tsdb The callback supplied TSDB instance
	 * @return the singleton instance
	 */
	public static TSDBEventPublisher getInstance(TSDB tsdb) {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new TSDBEventPublisher(tsdb);
				}
			}
		}
		return instance;
	}

	/**
	 * Creates a new TSDBEventPublisher
	 * @param tsdb The callback supplied TSDB instance
	 */
	private TSDBEventPublisher(TSDB tsdb) {
		this.tsdb = tsdb;		
		config = new Properties();
		config.putAll(tsdb.getConfig().getMap());
	}
	
	/**
	 * Configures the downstream search plugins. Downstream plugins should throw exceptions if they can't start up properly. 
	 */
	public void configureSearch() {
		
	}

	/**
	 * Configures the downstream rpc plugins. Downstream plugins should throw exceptions if they can't start up properly. 
	 */
	public void configureRpc() {
		
	}

	/**
	 * Configures the downstream publisher plugins. Downstream plugins should throw exceptions if they can't start up properly. 
	 */
	public void configurePublisher() {
		
	}

	/**
	 * Called by the TSD when a request for statistics collection has come in.
	 * @param statsCollector The collector used for emitting statistics
	 * @param pluginType The plugin type this collector was issued for
	 * @see net.opentsdb.search.SearchPlugin#collectStats(net.opentsdb.stats.StatsCollector)
	 */	
	public void collectStats(PluginType pluginType, StatsCollector statsCollector) {
		// TODO Auto-generated method stub

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
