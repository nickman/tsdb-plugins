package org.helios.tsdb.plugins.asynch;

import java.util.Map;

import net.opentsdb.core.TSDB;
import net.opentsdb.meta.Annotation;
import net.opentsdb.meta.TSMeta;
import net.opentsdb.meta.UIDMeta;
import net.opentsdb.search.SearchQuery;
import net.opentsdb.stats.StatsCollector;

import org.helios.tsdb.plugins.event.PluginType;
import org.helios.tsdb.plugins.event.TSDBEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.AbstractService;
import com.lmax.disruptor.RingBuffer;
import com.stumbleupon.async.Deferred;

/**
 * <p>Title: TSDBEventPublisher</p>
 * <p>Description: The asynch plugin event multiplexer</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.asynch.TSDBEventPublisher</code></p>
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
	/** The RingBuffer instance events are published to */
	protected RingBuffer<TSDBEvent> ringBuffer = null;
	
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

	
	/**
	 * Called by TSDB to initialize the plugin Implementations are responsible for setting up any IO they need as well as starting any required background threads. Note: Implementations should throw exceptions if they can't start up properly. The TSD will then shutdown so the operator can fix the problem. Please use IllegalArgumentException for configuration issues.
	 * @param tsdb The parent TSDB object
	 * @see net.opentsdb.tsd.RTPublisher#initialize(net.opentsdb.core.TSDB)
	 */	
	public void initialize(TSDB tsdb) {
		 
	}




}
