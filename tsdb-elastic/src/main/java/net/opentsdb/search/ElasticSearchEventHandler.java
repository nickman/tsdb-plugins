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

import httpfailover.FailoverHttpClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ThreadPoolExecutor;

import net.opentsdb.core.TSDB;
import net.opentsdb.meta.Annotation;
import net.opentsdb.meta.TSMeta;
import net.opentsdb.meta.UIDMeta;

import net.opentsdb.search.SearchQuery.SearchType;
import net.opentsdb.utils.JSON;

import org.apache.http.HttpHost;
import org.apache.http.client.fluent.Async;
import org.apache.http.client.fluent.Content;
import org.apache.http.client.fluent.Request;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.helios.tsdb.plugins.async.AsyncDispatcherExecutor;
import org.helios.tsdb.plugins.event.TSDBEvent;
import org.helios.tsdb.plugins.handlers.EmptySearchEventHandler;
import org.helios.tsdb.plugins.util.ConfigurationHelper;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.google.common.collect.ImmutableList;
import com.stumbleupon.async.Deferred;

/**
 * <p>Title: ElasticSearchEventHandler</p>
 * <p>Description: ElasticSearch Search Plugin for OpenTSDB. This only a repackaging of the original 
 * <b>otsdb-elasticsearch</b> plugin written by <b>clarsen AT euphoriaaudio DOT com</b></p> 
 * @author Chris Larsern (clarsen AT euphoriaaudio DOT com)
 * @author Whitehead (repackaged as a tsdb-plugins Async Search Event Handler)
 * <p><code>net.opentsdb.search.ElasticSearchEventHandler</code></p>
 */

public class ElasticSearchEventHandler extends EmptySearchEventHandler {
	/** The elastic search hosts */
	protected String[] esHosts = {};
	/** The pool of http connections to elastic-search hosts */
	protected PoolingClientConnectionManager http_pool;
	/** The max connections per route in the http pool */
	protected int maxConnsPerRoute = -1;
	/** The max total connections in the http pool */
	protected int maxConns = -1;
	/** The configured elastic-search http endpoints */
	protected ImmutableList<HttpHost> hosts;
	/** The http-client to execute the elastic-search http posts */
	protected FailoverHttpClient httpClient;
	/** The elastic-search index name*/
	protected String index;
	/** The type name for indexed TSMetas */
	protected String tsmeta_type;
	/** The type name for indexed TSUids */
	protected String uidmeta_type;
	/** The type name for indexed Annotations */
	protected String annotation_type;
	
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
	
	
	@Override
	public void initialize(TSDB tsdb, Properties extracted) {
		super.initialize(tsdb, extracted);
		log.info("\n\t=========================================\n\tStarting ElasticSearchEventHandler\n\t=========================================");
		esHosts = ConfigurationHelper.getSystemThenEnvPropertyArray(ES_HOSTS, null, extracted);
		if(esHosts==null|| esHosts.length==0) throw new IllegalArgumentException("There were no elasticsearch plugins defined");
		maxConnsPerRoute = ConfigurationHelper.getIntSystemThenEnvProperty(ES_MAX_PER_ROUTE, DEFAULT_ES_MAX_PER_ROUTE, extracted);
		maxConns = ConfigurationHelper.getIntSystemThenEnvProperty(ES_TOTAL_MAX, DEFAULT_ES_TOTAL_MAX, extracted);
		index = ConfigurationHelper.getSystemThenEnvProperty(ES_INDEX_NAME, DEFAULT_ES_INDEX_NAME, extracted);
		tsmeta_type = ConfigurationHelper.getSystemThenEnvProperty(ES_TSMETA_TYPE, DEFAULT_ES_TSMETA_TYPE, extracted); 
		uidmeta_type = ConfigurationHelper.getSystemThenEnvProperty(ES_UIDMETA_TYPE, DEFAULT_ES_UIDMETA_TYPE, extracted);
		annotation_type = ConfigurationHelper.getSystemThenEnvProperty(ES_ANNOT_TYPE, DEFAULT_ES_ANNOT_TYPE, extracted);		
		http_pool = new PoolingClientConnectionManager();
		http_pool.setDefaultMaxPerRoute(maxConnsPerRoute);
		http_pool.setMaxTotal(maxConns);
		threadPool = new AsyncDispatcherExecutor("esplugin", extracted);
		StringBuilder cfDump = new StringBuilder("\n\tElasticSearch Hosts:");
		for(String s: esHosts) {
			cfDump.append("\n\t\t").append(s);
		}
		cfDump.append("\n\tDefaultMaxPerRoute:").append(maxConnsPerRoute);
		cfDump.append("\n\tMaxTotal:").append(maxConns);
		
		log.info("\n\t=========================================\n\tStarted ElasticSearchEventHandler" + cfDump + "\n\t=========================================");
		
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
	
	/**
	 * Executes a search query against elastic-search
	 * @param query The submitted search query
	 * @return the search query with results added
	 */
	public Deferred<SearchQuery> executeQuery(final SearchQuery query) {
		    final Deferred<SearchQuery> result = new Deferred<SearchQuery>();		    
		    final StringBuilder uri = new StringBuilder("http://");
		    uri.append(hosts.get(0).toHostString());
		    uri.append("/").append(index).append("/");
		    switch(query.getType()) {
		      case TSMETA:
		      case TSMETA_SUMMARY:
		      case TSUIDS:
		        uri.append(tsmeta_type);
		        break;
		      case UIDMETA:
		        uri.append(uidmeta_type);
		        break;
		      case ANNOTATION:
		        uri.append(annotation_type);
		        break;
		    }
		    uri.append("/_search");
		    
		    // setup the query body
		    HashMap<String, Object> body = new HashMap<String, Object>(3);
		    body.put("size", query.getLimit());
		    body.put("from", query.getStartIndex());
		    
		    HashMap<String, Object> qs = new HashMap<String, Object>(1);
		    body.put("query", qs);
		    HashMap<String, String> query_string = new HashMap<String, String>(1);
		    query_string.put("query", query.getQuery());
		    qs.put("query_string", query_string);
		    
		    final Request request = Request.Post(uri.toString());
		    request.bodyByteArray(JSON.serializeToBytes(body));
		    
		    final Async async = Async.newInstance().use(threadPool);
		    async.execute(request, new SearchCB(query, result));
		    return result;
		  }
	
	  final class SearchCB implements FutureCallback<Content> {

		    final SearchQuery query;
		    final Deferred<SearchQuery> result;
		    
		    public SearchCB(final SearchQuery query, final Deferred<SearchQuery> result) {
		      this.query = query;
		      this.result = result;
		    }
		    
		    @Override
		    public void cancelled() {
		      result.callback(null);
		    }

		    @Override
		    public void completed(final Content content) {
		      
		      final JsonParser jp = JSON.parseToStream(content.asStream());
		      if (jp == null) {
		        log.warn("Query response was null or empty");
		        result.callback(null);
		        return;
		      }
		      
		      try {
		        JsonToken next = jp.nextToken();
		        if (next != JsonToken.START_OBJECT) {
		          log.error("Error: root should be object: quiting.");
		          result.callback(null);
		          return;
		        }
		      
		        final List<Object> objects = new ArrayList<Object>();
		        
		        // loop through the JSON structure
		        String parent = "";
		        String last = "";
		        
		        while (jp.nextToken() != null) {
		          String fieldName = jp.getCurrentName();
		          if (fieldName != null)
		            last = fieldName;
		          
		          if (jp.getCurrentToken() == JsonToken.START_ARRAY || 
		              jp.getCurrentToken() == JsonToken.START_OBJECT)
		            parent = last;
		          
		          if (fieldName != null && fieldName.equals("_source")) {
		            if (jp.nextToken() == JsonToken.START_OBJECT) {
		              // parse depending on type
		              switch (query.getType()) {
		                case TSMETA:
		                case TSMETA_SUMMARY:
		                case TSUIDS:
		                  final TSMeta meta = jp.readValueAs(TSMeta.class);
		                  if (query.getType() == SearchType.TSMETA) {
		                    objects.add(meta);
		                  } else if (query.getType() == SearchType.TSUIDS) {
		                    objects.add(meta.getTSUID());
		                  } else {
		                    final HashMap<String, Object> map = 
		                      new HashMap<String, Object>(3);
		                    map.put("tsuid", meta.getTSUID());
		                    map.put("metric", meta.getMetric().getName());
		                    final HashMap<String, String> tags = 
		                      new HashMap<String, String>(meta.getTags().size() / 2);
		                    int idx = 0;
		                    String name = "";
		                    for (final UIDMeta uid : meta.getTags()) {
		                      if (idx % 2 == 0) {
		                        name = uid.getName();
		                      } else {
		                        tags.put(name, uid.getName());
		                      }
		                      idx++;
		                    }
		                    map.put("tags", tags);
		                    objects.add(map);
		                  }
		                  break;
		                case UIDMETA:
		                  final UIDMeta uid = jp.readValueAs(UIDMeta.class);
		                  objects.add(uid);
		                  break;
		                case ANNOTATION:
		                  final Annotation note = jp.readValueAs(Annotation.class);
		                  objects.add(note);
		                  break;
		              }
		            }else
		              log.warn("Invalid _source value from ES, should have been a START_OBJECT");
		          } else if (fieldName != null && jp.getCurrentToken() != JsonToken.FIELD_NAME &&
		              parent.equals("hits") && fieldName.equals("total")){
		            log.trace("Total hits: [{}]", jp.getValueAsInt());
		            query.setTotalResults(jp.getValueAsInt());
		          } else if (fieldName != null && jp.getCurrentToken() != JsonToken.FIELD_NAME &&
		              fieldName.equals("took")){
		            log.trace("Time taken: [{}]" , jp.getValueAsInt());
		            query.setTime(jp.getValueAsInt());
		          }
		          
		          query.setResults(objects);
		        }
		        
		        result.callback(query);
		        
		      } catch (JsonParseException e) {
		        log.error("Query failed", e);
		        throw new RuntimeException(e);
		      } catch (IOException e) {
		        log.error("Query failed", e);
		        throw new RuntimeException(e);
		      }
		    }
		    @Override
		    public void failed(final Exception e) {
		      log.error("Query failed", e);
		      throw new RuntimeException(e);
		    }
		    
		  }
	
	
	
}
