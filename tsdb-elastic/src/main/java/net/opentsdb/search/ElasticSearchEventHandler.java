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

import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;

import net.opentsdb.core.TSDB;

import org.helios.tsdb.plugins.event.TSDBEvent;
import org.helios.tsdb.plugins.handlers.EmptySearchEventHandler;

/**
 * <p>Title: ElasticSearchEventHandler</p>
 * <p>Description: ElasticSearch Search Plugin for OpenTSDB. This handler is based on original 
 * <b>otsdb-elasticsearch</b> plugin written by <b>clarsen AT euphoriaaudio DOT com</b></p> 
 * @author Chris Larsern (clarsen AT euphoriaaudio DOT com)
 * @author Whitehead (repackaged as a tsdb-plugins Async Search Event Handler)
 * <p><code>net.opentsdb.search.ElasticSearchEventHandler</code></p>
 */

public class ElasticSearchEventHandler extends EmptySearchEventHandler {
	/** The elastic search node URIs */
	protected final Set<URI> esNodex = new HashSet<URI>();
	
	/** The index name for TSMetas */
	protected String tsmeta_index;
	/** The type name for indexed TSMetas */
	protected String tsmeta_type;
	/** The index settings for the TSMeta index */
	protected final Map<String, String> tsmeta_settings = new HashMap<String, String>();	
	/** The URL or file name of the override tsmeta mapping json file */
	protected URL tsmeta_mapping;
	
	
	/** The index name for UIDMetas */
	protected String uidmeta_index;
	/** The type name for indexed UIDMetas */
	protected String uidmeta_type;
	/** The index settings for the UIDMeta index */
	protected final Map<String, String> uidmeta_settings = new HashMap<String, String>();	

	
	/** The index name for Anotations */
	protected String annotation_index;
	/** The type name for indexed Annotations */
	protected String annotation_type;
	/** The index settings for the Annotations index */
	protected final Map<String, String> annotation_settings = new HashMap<String, String>();	
	
	/** Thread pool for handling async http request callbacks/responses */
	protected ThreadPoolExecutor threadPool;
	
	
	/** The config property name for the comma separated list of elasticsearch hosts */
	public static final String ES_HOSTS = "es.tsd.search.elasticsearch.hosts";
	/** The config property name for the default max connections per route in the http pool */
	public static final String ES_MAX_PER_ROUTE = "es.tsd.search.elasticsearch.hosts";
	/** The default max connections per route in the http pool */
	public static final int DEFAULT_ES_MAX_PER_ROUTE = 25;
	/** The config property name for the default total max connections in the http pool */
	public static final String ES_TOTAL_MAX = "es.tsd.search.elasticsearch.hosts";
	/** The default max connections per route in the http pool */
	public static final int DEFAULT_ES_TOTAL_MAX = 50;
	
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
	
	/** The config property name to indicate if percolating should be enabled */
	public static final String ES_ENABLE_PERCOLATES = "es.tsd.search.elasticsearch.percolate.enable";
	/** The default percolating enablement */
	public static final boolean DEFAULT_ES_ENABLE_PERCOLATES = false;
	
	
	@Override
	public void initialize(TSDB tsdb, Properties extracted) {
		super.initialize(tsdb, extracted);
		log.info("\n\t=========================================\n\tStarting ElasticSearchEventHandler\n\t=========================================");
//		esHosts = ConfigurationHelper.getSystemThenEnvPropertyArray(ES_HOSTS, null, extracted);
//		if(esHosts==null|| esHosts.length==0) throw new IllegalArgumentException("There were no elasticsearch plugins defined");
//		maxConnsPerRoute = ConfigurationHelper.getIntSystemThenEnvProperty(ES_MAX_PER_ROUTE, DEFAULT_ES_MAX_PER_ROUTE, extracted);
//		maxConns = ConfigurationHelper.getIntSystemThenEnvProperty(ES_TOTAL_MAX, DEFAULT_ES_TOTAL_MAX, extracted);
//		index = ConfigurationHelper.getSystemThenEnvProperty(ES_INDEX_NAME, DEFAULT_ES_INDEX_NAME, extracted);
//		tsmeta_type = ConfigurationHelper.getSystemThenEnvProperty(ES_TSMETA_TYPE, DEFAULT_ES_TSMETA_TYPE, extracted); 
//		uidmeta_type = ConfigurationHelper.getSystemThenEnvProperty(ES_UIDMETA_TYPE, DEFAULT_ES_UIDMETA_TYPE, extracted);
//		annotation_type = ConfigurationHelper.getSystemThenEnvProperty(ES_ANNOT_TYPE, DEFAULT_ES_ANNOT_TYPE, extracted);		
//		http_pool = new PoolingClientConnectionManager();
//		http_pool.setDefaultMaxPerRoute(maxConnsPerRoute);
//		http_pool.setMaxTotal(maxConns);
//		threadPool = new AsyncDispatcherExecutor("esplugin", extracted);
//		StringBuilder cfDump = new StringBuilder("\n\tElasticSearch Hosts:");
//		for(String s: esHosts) {
//			cfDump.append("\n\t\t").append(s);
//		}
//		cfDump.append("\n\tDefaultMaxPerRoute:").append(maxConnsPerRoute);
//		cfDump.append("\n\tMaxTotal:").append(maxConns);
		
		//log.info("\n\t=========================================\n\tStarted ElasticSearchEventHandler" + cfDump + "\n\t=========================================");
		
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.handlers.EmptySearchEventHandler#onEvent(org.helios.tsdb.plugins.event.TSDBEvent, long, boolean)
	 */
	@Override
	public void onEvent(TSDBEvent event, long sequence, boolean endOfBatch) throws Exception {
		switch(event.eventType) {
		case ANNOTATION_DELETE:
			break;
		case ANNOTATION_INDEX:
			break;
		case SEARCH:
			break;
		case TSMETA_DELETE:
			break;
		case TSMETA_INDEX:
			break;
		case UIDMETA_DELETE:
			break;
		case UIDMETA_INDEX:
			break;
		default:
			break;
			
		}
	}
}
