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
package net.opentsdb.search;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import net.opentsdb.core.TSDB;
import net.opentsdb.search.index.ESInitializer;
import net.opentsdb.search.index.IndexOperations;
import net.opentsdb.stats.StatsCollector;

import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.transport.LocalTransportAddress;
import org.helios.tsdb.plugins.event.TSDBEvent;
import org.helios.tsdb.plugins.event.TSDBSearchEvent;
import org.helios.tsdb.plugins.handlers.EmptySearchEventHandler;
import org.helios.tsdb.plugins.util.ConfigurationHelper;
import org.helios.tsdb.plugins.util.JMXHelper;
import org.helios.tsdb.plugins.util.URLHelper;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

/**
 * <p>Title: ElasticSearchEventHandler</p>
 * <p>Description: ElasticSearch Search Plugin for OpenTSDB. This handler is based on original 
 * <b>otsdb-elasticsearch</b> plugin written by <b>clarsen AT euphoriaaudio DOT com</b></p> 
 * @author Chris Larsern (clarsen AT euphoriaaudio DOT com)
 * @author Whitehead (repackaged as a tsdb-plugins Async Search Event Handler)
 * <p><code>net.opentsdb.search.ElasticSearchEventHandler</code></p>
 */

public class ElasticSearchEventHandler extends EmptySearchEventHandler {
	/** The singleton instance */
	protected static volatile ElasticSearchEventHandler instance = null;
	/** The singleton instance ctor lock */
	protected static final Object lock = new Object();
	
	/** The elastic search node URIs */
	protected final Set<URI> esNodex = new HashSet<URI>();
	
	
	/** The set of configured and discovered URIs to ES instance transport interfaces */
	protected final Set<URI> transportURIs = new CopyOnWriteArraySet<URI>();
	/** The ES client */
	protected TransportClient client = null;
	/** The ES initializer */
	protected ESInitializer initializer = null;
	/** The ES cluster name */
	protected String clusterName = null;
	/** The ES timeout for synchronous ops */
	protected long esOpTimeout = -1L;
	/** The ES index configuration XML location */
	protected String indexConfig = null;
	/** The TSMeta type name */
	protected String tsmeta_type = null;
	/** The UIDMeta type name */
	protected String uidmeta_type = null;
	/** The Annotation type name */
	protected String annotation_type = null;
	/** The TSMeta index name */
	protected String tsmeta_index = null;
	/** The UIDMeta index name */
	protected String uidmeta_index = null;
	/** The Annotation index name */
	protected String annotation_index = null;		
	
	/** The index operations invoker */
	protected IndexOperations indexOps = null;
	/** The start latch */
	protected CountDownLatch latch = new CountDownLatch(1);		
	
	
	
	/** The config property name for the comma separated list of elasticsearch URIs */
	public static final String ES_HOSTS = "es.tsd.search.elasticsearch.uris";
	/** The default list of elasticsearch URIs */
	public static final String DEFAULT_ES_HOSTS = "tcp://localhost:9300";
	
	
	/** The config property name for the elastic-search index */
	public static final String ES_INDEX_NAME = "es.tsd.search.elasticsearch.index";
	/** The default name for the elastic-search index */
	public static final String DEFAULT_ES_INDEX_NAME = "opentsdb";
	/** The config property name for the elastic-search tsuid-meta type */
	public static final String ES_TSMETA_TYPE = "es.tsd.search.elasticsearch.tsmeta_type";
	/** The default name for the elastic-search tsuid-meta type */
	public static final String DEFAULT_ES_TSMETA_TYPE = "tsmeta";
	/** The config property name for the elastic-search uid-meta type */
	public static final String ES_UIDMETA_TYPE = "es.tsd.search.elasticsearch.uidmeta_type";
	/** The default name for the elastic-search uid-meta type */
	public static final String DEFAULT_ES_UIDMETA_TYPE = "uidmeta";	
	/** The config property name for the elastic-search annotation type */
	public static final String ES_ANNOT_TYPE = "es.tsd.search.elasticsearch.annotation_type";
	/** The default name for the elastic-search annotation type */
	public static final String DEFAULT_ES_ANNOT_TYPE = "annotation";

	/** The config property name for the elastic-search cluster name */
	public static final String ES_CLUSTER_NAME = "es.tsd.search.elasticsearch.cluster.name";
	/** The default name for the elastic-search cluster */
	public static final String DEFAULT_ES_CLUSTER_NAME = "opentsdb";
	
	/** The config property name for the file name or URL of the index/type configuration XML */
	public static final String ES_INDEX_CONFIG = "es.tsd.search.elasticsearch.indexconfig";
	/** The default name for the elastic-search cluster */
	public static final String DEFAULT_ES_INDEX_CONFIG = "classpath:/scripts/index-definitions.xml";

	
	
	/** The config property name to indicate if percolating should be enabled */
	public static final String ES_ENABLE_PERCOLATES = "es.tsd.search.elasticsearch.percolate.enable";
	/** The default percolating enablement */
	public static final boolean DEFAULT_ES_ENABLE_PERCOLATES = false;
	/** The config property name to indicate if es ops should be async */
	public static final String ES_ENABLE_ASYNC = "es.tsd.search.elasticsearch.ops.async";
	/** The default es ops async enablement */
	public static final boolean DEFAULT_ES_ENABLE_ASYNC = true;
	/** The config property name to specify the es ops timeout in ms. */
	public static final String ES_OP_TIMEOUT = "es.tsd.search.elasticsearch.ops.timeout";
	/** The default es ops timeout in ms. */
	public static final long DEFAULT_ES_OP_TIMEOUT = 1000;
	
	/**
	 * Acquires the singleton instance
	 * @return the singleton instance
	 */
	public static ElasticSearchEventHandler getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new ElasticSearchEventHandler();
				}
			}
		}
		return instance;
	}
	
	/**
	 * Waits the default time (5 seconds) for the event handler to complete initialization
	 */
	public static void waitForStart() {
		waitForStart(5, TimeUnit.SECONDS);
	}
	
	
	/**
	 * Waits for the event handler to complete initialization
	 * @param timeout The timeout period to wait for
	 * @param unit  The timeout unit
	 */
	public static void waitForStart(long timeout, TimeUnit unit) {
		try {
			if(!getInstance().latch.await(timeout, unit)) {
				throw new Exception("Did not start before timeout", new Throwable());
			}
		} catch (Exception ex) {
			throw new RuntimeException("Failed to wait for start", ex);
		}
	}
	
	/**
	 * Returns the transport client
	 * @return the transport client
	 * FIXME: Need to block until client is initialized
	 */
	public static TransportClient getClient() {
		return getInstance().client;
	}
	
	/**
	 * Creates a new ElasticSearchEventHandler.
	 */
	private ElasticSearchEventHandler() {
		
	}
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.handlers.AbstractTSDBEventHandler#initialize(net.opentsdb.core.TSDB, java.util.Properties)
	 */
	@Override
	public void initialize(TSDB tsdb, Properties extracted) {
		super.initialize(tsdb, extracted);
		try {
			log.info("\n\t=========================================\n\tStarting ElasticSearchEventHandler\n\t=========================================");
			tsmeta_type = ConfigurationHelper.getSystemThenEnvProperty(ES_TSMETA_TYPE, DEFAULT_ES_TSMETA_TYPE, extracted); 
			uidmeta_type = ConfigurationHelper.getSystemThenEnvProperty(ES_UIDMETA_TYPE, DEFAULT_ES_UIDMETA_TYPE, extracted);
			annotation_type = ConfigurationHelper.getSystemThenEnvProperty(ES_ANNOT_TYPE, DEFAULT_ES_ANNOT_TYPE, extracted);		
			
			esOpTimeout = ConfigurationHelper.getLongSystemThenEnvProperty(ES_OP_TIMEOUT, DEFAULT_ES_OP_TIMEOUT, extracted);
			log.info("ES Operation Timeout: {} ms.", esOpTimeout);
			clusterName = ConfigurationHelper.getSystemThenEnvProperty(ES_CLUSTER_NAME, DEFAULT_ES_CLUSTER_NAME, extracted);
			log.info("ES Cluster Name: {}", clusterName);
			indexConfig = ConfigurationHelper.getSystemThenEnvProperty(ES_INDEX_CONFIG, DEFAULT_ES_INDEX_CONFIG, extracted);
			log.info("ES Index Config: {}", indexConfig);
			String[] esURIs = ConfigurationHelper.getSystemThenEnvPropertyArray(ES_HOSTS, DEFAULT_ES_HOSTS, extracted);
			if(esURIs==null|| esURIs.length==0) throw new IllegalArgumentException("There were no elasticsearch plugins defined");
			client = getTransportClient(esURIs);
			log.info("ES Transport URIs: {}", transportURIs);
			
			boolean enablePercs = ConfigurationHelper.getBooleanSystemThenEnvProperty(ES_ENABLE_PERCOLATES, DEFAULT_ES_ENABLE_PERCOLATES, extracted);
			log.info("ES Percolating Enabled:{}", enablePercs);
			boolean enableAsync = ConfigurationHelper.getBooleanSystemThenEnvProperty(ES_ENABLE_ASYNC, DEFAULT_ES_ENABLE_ASYNC, extracted);
			log.info("ES Async Dispatching:{}", enableAsync);
			
			initializer = new ESInitializer(client.admin().indices(), esOpTimeout, annotation_type, tsmeta_type, annotation_type); 
			initializer.processIndexConfig(getXmlConfigStream());
			// indexes keyed by type name
			Map<String, String> cfx = initializer.getIndexNames();
			Map<String, String[]> typeIndexNames = new HashMap<String, String[]>(3);
			typeIndexNames.put(DEFAULT_ES_ANNOT_TYPE, new String[] {annotation_type, cfx.get(annotation_type)});
			typeIndexNames.put(DEFAULT_ES_TSMETA_TYPE, new String[] {tsmeta_type, cfx.get(tsmeta_type)});
			typeIndexNames.put(DEFAULT_ES_UIDMETA_TYPE, new String[] {uidmeta_type, cfx.get(uidmeta_type)});
			printIndexes(typeIndexNames);
			annotation_index = cfx.get(annotation_type);
			tsmeta_index = cfx.get(tsmeta_type);
			uidmeta_index = cfx.get(uidmeta_type);
			indexOps = new IndexOperations(client, esOpTimeout, 
					enablePercs, 
					enableAsync, 
					typeIndexNames);
			JMXHelper.registerMBean(indexOps, IndexOperations.OBJECT_NAME);
			latch.countDown();
			log.info("\n\t=========================================\n\tStarted ElasticSearchEventHandler\n\t=========================================");
		} catch (Exception ex) {
			throw new RuntimeException("Failed to initialize ElasticSearchEventHandler", ex);
		}		
	}
	
	@Override
	public void shutdown() {
		log.info("\n\t=========================================\n\tStopping ElasticSearchEventHandler\n\t=========================================");
		JMXHelper.unregisterMBean(IndexOperations.OBJECT_NAME);
		try { JMXHelper.unregisterMBean(IndexOperations.OBJECT_NAME); } catch (Exception ex) {/* No Op */}
		try { client.close(); } catch (Exception ex) {/* No Op */}
		this.indexOps = null;
		this.initializer = null;
		this.esNodex.clear();
		instance = null;
		super.shutdown();
		log.info("\n\t=========================================\n\tStopped ElasticSearchEventHandler\n\t=========================================");
	}
	
	/**
	 * Logs the type names and the alias for the index each will be indexed to
	 * @param typeIndexNames A map of types and aliases
	 */
	protected void printIndexes(Map<String, String[]> typeIndexNames) {
		StringBuilder b = new StringBuilder("\n\t=======================================\n\tIndex Aliases By Type\n\t=======================================");
		for(Map.Entry<String, String[]> entry: typeIndexNames.entrySet()) {
			String[] typeAndAlias = entry.getValue();
			b.append("\n\t[").append(entry.getKey()).append("]:").append(typeAndAlias[0]).append("  -->  ").append(typeAndAlias[1]);			
		}
		b.append("\n");
		log.info(b.toString());
	}
	
	/**
	 * Returns an inputstream for reading the ES index configuration XML
	 * @return an inputstream for the ES index configuration XML
	 * @throws IOException thrown on errors reading from a file or URL
	 */
	protected InputStream getXmlConfigStream() throws IOException {
		if(indexConfig.startsWith("classpath:")) {
			String resource = indexConfig.substring("classpath:".length());		
			log.info("Looking Up Configuration Resource [{}]", resource);
			InputStream is = getClass().getResourceAsStream(resource);
			if(is==null) {
				is = new FileInputStream("./src/main/resources" + resource);
			}
			return is;
		}
		if(URLHelper.isValidURL(indexConfig)) {
			return URLHelper.toURL(indexConfig).openStream();
		} else if(new File(indexConfig).canRead()) {
			return new FileInputStream(indexConfig);
		} else {
			throw new RuntimeException("Failed to locate index config [" + indexConfig + "]");
		}
	}
	
	/**
	 * Builds and returns an elasticsearch transport client
	 * @param esURIs The confguration defined URIs
	 * @return A transport client
	 */
	protected TransportClient getTransportClient(String...esURIs) {
		if(esURIs==null || esURIs.length<1) throw new RuntimeException("No transport URIs defined");
					
		client = new TransportClient(ImmutableSettings.settingsBuilder().put("client.transport.sniff", false).put("cluster.name", clusterName).build());
		
		for(String s: esURIs) {
			try {
				URI uri = new URI(s);
				String scheme = uri.getScheme();
				if("local".equals(scheme)) {
					transportURIs.add(uri);
					String id = uri.getHost();
					client.addTransportAddress(new LocalTransportAddress(id));
					log.info("We got a local URI [{}] so we're ignoring all other URIs", uri);
					return client;
				} else if("tcp".equals(scheme)) {
					transportURIs.add(uri);
					client.addTransportAddress(new InetSocketTransportAddress(uri.getHost(), uri.getPort()));
					log.info("Configured TCP URI [{}] added to client", uri);
				} else {
					log.warn("Unrecognized elasticsearch transport protocol Specified [{}]", scheme);
				}				
			} catch (Exception ex) {
				log.warn("Invalid elasticsearch transport URI Specified [{}]", s);
			}
		}
		
		return client;		
	}

	/**
	 * <p>Handles and EventBus search event </p>
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.handlers.EmptySearchEventHandler#onEvent(org.helios.tsdb.plugins.event.TSDBSearchEvent)
	 */
	@Override
	@Subscribe
	@AllowConcurrentEvents	
	public void onEvent(TSDBSearchEvent event) throws Exception {
		onEvent(event, -1L, false);
	}


	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.handlers.EmptySearchEventHandler#onEvent(org.helios.tsdb.plugins.event.TSDBEvent, long, boolean)
	 */
	@Override	
	public void onEvent(TSDBEvent event, long sequence, boolean endOfBatch) throws Exception {
		if(!event.eventType.isForSearch()) return;
		incrCount(event);
		final long start = System.currentTimeMillis();
		switch(event.eventType) {
		case ANNOTATION_DELETE:
			indexOps.deleteAnnotation(event.annotation);
			break;
		case ANNOTATION_INDEX:
			indexOps.indexAnnotation(event.annotation);
			break;
		case SEARCH:
			indexOps.executeQuery(event.searchQuery, event.deferred);
			break;
		case TSMETA_DELETE:
			indexOps.deleteTSMeta(event.tsuid);
			break;
		case TSMETA_INDEX:
			indexOps.indexTSMeta(event.tsMeta);
			break;
		case UIDMETA_DELETE:
			indexOps.deleteUIDMeta(event.uidMeta);
			break;
		case UIDMETA_INDEX:
			indexOps.indexUIDMeta(event.uidMeta);
			break;
		default:
			break;			
		}
		elapsedTime(event, System.currentTimeMillis()-start);
	}
	
	/**
	 * <p>Submits performance and volume metrics for this handler</p>
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.handlers.EmptySearchEventHandler#collectStats(net.opentsdb.stats.StatsCollector)
	 */
	@Override
	public void collectStats(StatsCollector collector) {		
		//collector.addExtraTag("", "");
		super.collectStats(collector);
		//collector.clearExtraTag("");
		
	}

	/**
	 * Returns 
	 * @return the clusterName
	 */
	public String getClusterName() {
		return clusterName;
	}

	/**
	 * Returns 
	 * @return the esOpTimeout
	 */
	public long getEsOpTimeout() {
		return esOpTimeout;
	}

	/**
	 * Returns 
	 * @return the tsmeta_type
	 */
	public String getTsmeta_type() {
		return tsmeta_type;
	}

	/**
	 * Returns 
	 * @return the uidmeta_type
	 */
	public String getUidmeta_type() {
		return uidmeta_type;
	}

	/**
	 * Returns 
	 * @return the annotation_type
	 */
	public String getAnnotation_type() {
		return annotation_type;
	}

	/**
	 * Returns 
	 * @return the tsmeta_index
	 */
	public String getTsmeta_index() {
		return tsmeta_index;
	}

	/**
	 * Returns 
	 * @return the uidmeta_index
	 */
	public String getUidmeta_index() {
		return uidmeta_index;
	}

	/**
	 * Returns 
	 * @return the annotation_index
	 */
	public String getAnnotation_index() {
		return annotation_index;
	}

	/**
	 * Returns the index operations client
	 * @return the index operations client
	 */
	public IndexOperations getIndexOpsClient() {
		return indexOps;
	}
}
