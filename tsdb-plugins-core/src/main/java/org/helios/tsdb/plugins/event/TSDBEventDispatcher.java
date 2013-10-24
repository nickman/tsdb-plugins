package org.helios.tsdb.plugins.event;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ThreadPoolExecutor;

import net.opentsdb.core.TSDB;
import net.opentsdb.meta.Annotation;
import net.opentsdb.meta.TSMeta;
import net.opentsdb.meta.UIDMeta;
import net.opentsdb.search.SearchQuery;

import org.helios.tsdb.plugins.Constants;
import org.helios.tsdb.plugins.async.AsyncDispatcherExecutor;
import org.helios.tsdb.plugins.async.AsyncEventDispatcher;
import org.helios.tsdb.plugins.async.EventBusEventDispatcher;
import org.helios.tsdb.plugins.service.AbstractTSDBPluginService;
import org.helios.tsdb.plugins.util.ConfigurationHelper;

import com.stumbleupon.async.Deferred;

/**
 * <p>Title: TSDBEventDispatcher</p>
 * <p>Description: The central event handler which the shell plugins dispatch events to.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.event.TSDBEventDispatcher</code></p>
 */
public class TSDBEventDispatcher extends AbstractTSDBPluginService {
	/** The asynch dispatcher */
	protected AsyncEventDispatcher asyncDispatcher;
	/** The asynch dispatcher's executor */
	protected ThreadPoolExecutor asyncExecutor;

	/**
	 * Stops the event dispatcher and all subsidiary services
	 */
	public void doPreShutdown() {
		if(asyncDispatcher!=null) {
			asyncDispatcher.shutdown();
			log.info("Shutdown AsyncDispatcher.");
		}
		if(asyncExecutor!=null) {
			int remainingTasks = asyncExecutor.shutdownNow().size();
			log.info("Shutdown AsyncExecutor. Remaining Tasks:{}", remainingTasks);
		}				
	}
	
	
	
	/**
	 * Initialize this plugin service: create and init async thread pool, create and init async dispatcher
	 */	
	protected void doInitialize() {
		
		String asyncDispatcherClassName = ConfigurationHelper.getSystemThenEnvProperty(Constants.ASYNC_DISPATCHER, Constants.DEFAULT_ASYNC_DISPATCHER, config);		
		log.info("Initializing Async Dispatcher [{}]", asyncDispatcherClassName);
		asyncExecutor = new AsyncDispatcherExecutor(config);
		
		loadAsyncDispatcher(asyncDispatcherClassName.trim());
		asyncDispatcher.initialize(config, asyncExecutor, allHandlers);
		log.info("Async Dispatcher [{}] Initialized.", asyncDispatcher.getClass().getSimpleName());
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
			@SuppressWarnings("unchecked")
			Class<AsyncEventDispatcher> clazz = (Class<AsyncEventDispatcher>)_clazz;
			asyncDispatcher = clazz.newInstance();
		} catch (Exception ex) {
			log.warn("Failed to load AsyncDispatcher Class [" + asyncDispatcherClassName + "]\nDefault AsyncDispatcher will be loaded. Stack trace follows:", ex);
			asyncDispatcher = new EventBusEventDispatcher();
		}
	}
	
	

	/**
	 * Creates a new TSDBEventDispatcher
	 * @param tsdb The callback supplied TSDB instance
	 * @param config The extracted configuration 
	 */
	public TSDBEventDispatcher(TSDB tsdb, Properties config) {
		super(tsdb, config);
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
		asyncDispatcher.publishDataPoint(metric, timestamp, value, tags, tsuid);
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
		asyncDispatcher.publishDataPoint(metric, timestamp, value, tags, tsuid);
	}
	

	
	/**
	 * Deletes an annotation
	 * @param annotation The annotation to delete
	 * @see net.opentsdb.search.SearchPlugin#deleteAnnotation(net.opentsdb.meta.Annotation)
	 */
	public void deleteAnnotation(Annotation annotation) {
		if(annotation!=null) {
			asyncDispatcher.deleteAnnotation(annotation);
		}
	}
	

	/**
	 * Indexes an annotation
	 * @param annotation The annotation to index
	 * @see net.opentsdb.search.SearchPlugin#indexAnnotation(net.opentsdb.meta.Annotation)
	 */
	public void indexAnnotation(Annotation annotation) {
		if(annotation!=null) {
			asyncDispatcher.indexAnnotation(annotation);
		}
	}	

	/**
	 * Called when we need to remove a timeseries meta object from the engine Note: Unique Document ID = TSUID
	 * @param tsMeta The TSMeta name to delete
	 * @see net.opentsdb.search.SearchPlugin#deleteTSMeta(java.lang.String)
	 */
	public void deleteTSMeta(String tsMeta) {
		if(tsMeta!=null) {
			asyncDispatcher.deleteTSMeta(tsMeta);
		}
	}
	
	/**
	 * Indexes a timeseries metadata object in the search engine Note: Unique Document ID = TSUID
	 * @param tsMeta The TSMeta to index
	 * @see net.opentsdb.search.SearchPlugin#indexTSMeta(net.opentsdb.meta.TSMeta)
	 */
	public void indexTSMeta(TSMeta tsMeta) {
		if(tsMeta!=null) {
			asyncDispatcher.indexTSMeta(tsMeta);
		}
	}	
	
	/**
	 * Indexes a UID metadata object for a metric, tagk or tagv Note: Unique Document ID = UID and the Type "TYPEUID"
	 * @param uidMeta The UIDMeta to index
	 * @see net.opentsdb.search.SearchPlugin#indexUIDMeta(net.opentsdb.meta.UIDMeta)
	 */
	public void indexUIDMeta(UIDMeta uidMeta) {
		if(uidMeta!=null) {
			asyncDispatcher.indexUIDMeta(uidMeta);
		}
	}	

	/**
	 * Called when we need to remove a UID meta object from the engine Note: Unique Document ID = UID and the Type "TYPEUID"
	 * @param uidMeta The UIDMeta to delete
	 * @see net.opentsdb.search.SearchPlugin#deleteUIDMeta(net.opentsdb.meta.UIDMeta)
	 */
	public void deleteUIDMeta(UIDMeta uidMeta) {
		if(uidMeta!=null) {
			asyncDispatcher.deleteUIDMeta(uidMeta);
		}
	}

	/**
	 * 
	 * @see net.opentsdb.search.SearchPlugin#executeQuery(net.opentsdb.search.SearchQuery)
	 */
	public void executeQuery(SearchQuery searchQuery, Deferred<SearchQuery> toComplete) {
		asyncDispatcher.executeQuery(searchQuery, toComplete);
	}

	/**
	 * Returns true if the async executor has been shut down.
	 * @return true if the async has been shut down, false otherwise
	 * @see java.util.concurrent.ThreadPoolExecutor#isShutdown()
	 */
	public boolean isAsyncShutdown() {
		return asyncExecutor.isShutdown();
	}

	/**
	 * Returns true if the asynch executor is in the process of terminating after shutdown() or shutdownNow() but has not completely terminated.
	 * @return true if terminating, false otherwise
	 * @see java.util.concurrent.ThreadPoolExecutor#isTerminating()
	 */
	public boolean isAsyncTerminating() {
		return asyncExecutor.isTerminating();
	}

	/**
	 * Returns true if all the async executor's tasks have completed following shut down.
	 * @return true if terminated, false otherwise
	 * @see java.util.concurrent.ThreadPoolExecutor#isTerminated()
	 */
	public boolean isAsyncTerminated() {
		return asyncExecutor.isTerminated();
	}

	/**
	 * Tries to remove from all Future tasks that have been cancelled from the async executor's work queue
	 * @see java.util.concurrent.ThreadPoolExecutor#purge()
	 */
	public void purgeAsync() {
		asyncExecutor.purge();
	}

	





}
