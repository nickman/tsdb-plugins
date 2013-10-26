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
package net.opentsdb.search.index;

import static net.opentsdb.search.ElasticSearchEventHandler.DEFAULT_ES_ANNOT_TYPE;
import static net.opentsdb.search.ElasticSearchEventHandler.DEFAULT_ES_INDEX_NAME;
import static net.opentsdb.search.ElasticSearchEventHandler.DEFAULT_ES_TSMETA_TYPE;
import static net.opentsdb.search.ElasticSearchEventHandler.DEFAULT_ES_UIDMETA_TYPE;
import static net.opentsdb.search.SearchQuery.SearchType.ANNOTATION;
import static net.opentsdb.search.SearchQuery.SearchType.TSMETA;
import static net.opentsdb.search.SearchQuery.SearchType.TSMETA_SUMMARY;
import static net.opentsdb.search.SearchQuery.SearchType.TSUIDS;
import static net.opentsdb.search.SearchQuery.SearchType.UIDMETA;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.opentsdb.meta.Annotation;
import net.opentsdb.meta.TSMeta;
import net.opentsdb.meta.UIDMeta;
import net.opentsdb.search.ElasticSearchEventHandler;
import net.opentsdb.search.SearchQuery;
import net.opentsdb.search.SearchQuery.SearchType;
import net.opentsdb.utils.JSON;

import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.admin.cluster.node.info.NodeInfo;
import org.elasticsearch.action.admin.cluster.node.info.NodesInfoRequest;
import org.elasticsearch.action.admin.cluster.node.info.NodesInfoResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.ClusterAdminClient;
import org.elasticsearch.client.internal.InternalClient;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.threadpool.ThreadPoolStats.Stats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stumbleupon.async.Deferred;


/**
 * <p>Title: IndexOperations</p>
 * <p>Description: Handles all the search event callacks</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>net.opentsdb.search.index.IndexOperations</code></p>
 */

public class IndexOperations  {
	/** Instance logger */
	protected final Logger log = LoggerFactory.getLogger(getClass());
	/** The ES client */
	protected final InternalClient client;
	/** The timeout in ms. for index update operations */
	protected long indexOpsTimeout;
	
	/** The configured annotation type name */
	protected final String annotationTypeName;
	/** The configured annotation index name */
	protected final String annotationIndexName;
	
	/** The configured TSMeta name */
	protected final String tsMetaTypeName;
	/** The configured TSMeta index name */
	protected final String tsMetaIndexName;
	
	/** The configured UIDMeta name */
	protected final String uidMetaTypeName;
	/** The configured UIDMeta index name */
	protected final String uidMetaIndexName;
	
	/** Indicates if percolates should be enabled */
	protected boolean enablePercolates = false;
	/** Indicates if indexing and deletion operations should be async */
	protected boolean async = true;
	
	/** A map of indexes to search keyed by the search type */
	protected final Map<SearchType, String> indexesBySearchType = new EnumMap<SearchType, String>(SearchType.class);
	/** A map of types to search keyed by the search type */
	protected final Map<SearchType, String> mappingBySearchType = new EnumMap<SearchType, String>(SearchType.class);
	/** A map of OpenTSDB types to deserialize search results against by the search type */
	protected final Map<SearchType, Class<?>> classBySearchType = new EnumMap<SearchType, Class<?>>(SearchType.class);
	
//	/** The search query template. Populate with: 0: the query, 1: the from index, 2: the size limit */
//	public static final String SEARCH_TEMPLATE = "{\"query\":{\"query_string\":{\"query\":\"%s\"}},\"from\":0,\"size\":10}";
	
	/** The response listener for indexing events */
	protected final ActionListener<IndexResponse> indexResponseListener = new ActionListener<IndexResponse>() {
		@Override
		public void onResponse(IndexResponse response) {
			log.debug("IndexOp for Type [{}] on Index [{}] Complete. ID: [{}]", response.getType(), response.getIndex(), response.getId());
			List<String> percolateMatches = response.getMatches();
			if(percolateMatches!=null) {
				log.debug("IndexOp Matched [{}] Registered Queries: {}", percolateMatches.size(), percolateMatches);
				// TODO: Broadcast percolates
			}
		}
		@Override
		public void onFailure(Throwable e) {
			log.error("IndexOp Failure", e);
		}	
	};
	
	/** The response listener for deletion events */
	protected final ActionListener<DeleteResponse> deleteResponseListener = new ActionListener<DeleteResponse>() {		
		@Override
		public void onResponse(DeleteResponse response) {
			log.debug("DeleteOp for Type [{}] on Index [{}] Complete. ID: [{}]", response.getType(), response.getIndex(), response.getId());
		}
		@Override
		public void onFailure(Throwable e) {
			log.error("DeleteOp Failure", e);
		}	
	};
	
	

	/**
	 * Creates a new IndexOperations
	 * @param client The ES index client 
	 * @param indexOpsTimeout The timeout in ms. for index factory operations
	 * @param enablePercolates Indicates if percolating should be enabled
	 * @param async Indicates if indexing and deletion operations should be async
	 * @param typeIndexNames A map keyed by the default type names (e.g. {@link ElasticSearchEventHandler#DEFAULT_ES_ANNOT_TYPE}
	 * and where the values are string arrays where index 0 is the configured type name and index 1 is the index name.
	 */
	public IndexOperations(InternalClient client, long indexOpsTimeout, boolean enablePercolates, boolean async, Map<String, String[]> typeIndexNames) {
		this.client = client;
		this.indexOpsTimeout = indexOpsTimeout;
		annotationTypeName = typeIndexNames.get(DEFAULT_ES_ANNOT_TYPE)[0];
		annotationIndexName = typeIndexNames.get(DEFAULT_ES_ANNOT_TYPE)[1];
		tsMetaTypeName = typeIndexNames.get(DEFAULT_ES_TSMETA_TYPE)[0];
		tsMetaIndexName = typeIndexNames.get(DEFAULT_ES_TSMETA_TYPE)[1];
		uidMetaTypeName = typeIndexNames.get(DEFAULT_ES_UIDMETA_TYPE)[0];
		uidMetaIndexName = typeIndexNames.get(DEFAULT_ES_UIDMETA_TYPE)[1];
		
		indexesBySearchType.put(ANNOTATION, annotationIndexName);
		indexesBySearchType.put(TSMETA, tsMetaIndexName);
		indexesBySearchType.put(TSMETA_SUMMARY, tsMetaIndexName);
		indexesBySearchType.put(TSUIDS, tsMetaIndexName);
		indexesBySearchType.put(UIDMETA, tsMetaIndexName);

		mappingBySearchType.put(ANNOTATION, annotationTypeName);
		mappingBySearchType.put(TSMETA, tsMetaTypeName);
		mappingBySearchType.put(TSMETA_SUMMARY, tsMetaTypeName);
		mappingBySearchType.put(TSUIDS, tsMetaTypeName);
		mappingBySearchType.put(UIDMETA, tsMetaTypeName);
		
		classBySearchType.put(ANNOTATION, Annotation.class);
		classBySearchType.put(TSMETA, TSMeta.class);
		classBySearchType.put(TSMETA_SUMMARY, TSMeta.class);
		classBySearchType.put(TSUIDS, TSMeta.class);
		classBySearchType.put(UIDMETA, UIDMeta.class);

		this.enablePercolates = enablePercolates; 		
		this.async = async;
		log.info("Created IndexOperations with timeout [{}]", indexOpsTimeout);
	}
	
	
	
    /**
     * Indexes a TSMeta object in ElasticSearch
     * @param meta The meta data to publish
     */
    public void indexTSUID(TSMeta meta) { 
    	log.debug("Indexing TSMeta [{}]", meta);
    	index(tsMetaIndexName, tsMetaTypeName, meta.getTSUID(), JSON.serializeToString(meta), async ? indexResponseListener : null);
    }
    
    /**
     * Deletes a TSMeta object in ElasticSearch
     * @param tsuid The id of the TSMeta doc to delete
     */
    public void indexUID(String tsuid) { 
    	log.debug("Deleting TSMeta [{}]", tsuid);
    	delete(tsMetaIndexName, tsMetaTypeName, tsuid, async ? deleteResponseListener : null);
    }
    
    /**
     * Indexes a UIDMeta object in ElasticSearch
     * @param meta The meta data to publish
     */
    public void indexTSUID(UIDMeta meta) { 
    	log.debug("Indexing UIDMeta [{}]", meta);
    	index(uidMetaIndexName, uidMetaTypeName, meta.getUID() + uidMetaTypeName, JSON.serializeToString(meta), async ? indexResponseListener : null);
    }
    
    /**
     * Deletes a UIDMeta object in ElasticSearch
     * @param meta The UIDMeta to delete the doc for
     */
    public void deleteUID(UIDMeta meta) { 
    	log.debug("Deleting UIDMeta [{}]", meta.getUID());
    	delete(uidMetaIndexName, uidMetaTypeName, meta.getUID() + uidMetaTypeName, async ? deleteResponseListener : null);
    }
    
    /**
     * Indexes an Annotation object in ElasticSearch
     * @param note The annotation to index
     */
    public void indexAnnotation(Annotation note) {
    	log.debug("Indexing Annotation [{}]", note);
    	index(annotationIndexName, annotationTypeName, note.getTSUID() + note.getStartTime(), JSON.serializeToString(note), async ? indexResponseListener : null);
    }
    
    /**
     * Deletes an Annotation object in ElasticSearch
     * @param note The annotation to delete the doc for
     */
    public void deleteAnnotation(Annotation note) {
    	log.debug("Deleting Annotation [{}]", note);
    	delete(annotationIndexName, annotationTypeName, note.getTSUID() + note.getStartTime(), async ? deleteResponseListener : null); 
    }
    
    
    /**
     * Generic json indexer
     * @param indexName The index name
     * @param typeName The type name
     * @param id The id of the document to index
     * @param jsonToIndex The content to index
     * @param responseListener The async response handler
     */
    protected void index(String indexName, String typeName, String id, String jsonToIndex, ActionListener<IndexResponse> responseListener) {
    	IndexRequest ir = new IndexRequest(indexName, typeName).source(jsonToIndex).id(id);
    	if(enablePercolates) ir.percolate("*");
    	if(responseListener==null) {    		
    		IndexResponse response = null;
    		ActionFuture<IndexResponse> af = null;
    		try {
    			af = client.index(ir);    			
    			response = af.actionGet(indexOpsTimeout);    			
    			if(af.getRootFailure()!=null) {
    				indexResponseListener.onFailure(af.getRootFailure());
    			} else {
    				indexResponseListener.onResponse(response);
    			}
    		} catch (Exception ex) {
    			indexResponseListener.onFailure(ex);
    		}
    	} else {
    		client.index(ir, responseListener);
    	}
    }
    
    /**
     * Generic json deleter
     * @param indexName The index name
     * @param typeName The type name
     * @param id The id of the document to index
     * @param deleteListener The async response handler
     */
    protected void delete(String indexName, String typeName, String id, ActionListener<DeleteResponse> deleteListener) {
    	DeleteRequest dr = new DeleteRequest(indexName, typeName, id);
    	if(deleteListener==null) {    		
    		DeleteResponse response = null;
    		ActionFuture<DeleteResponse> af = null;
    		try {
    			af = client.delete(dr);    			
    			response = af.actionGet(indexOpsTimeout);    			
    			if(af.getRootFailure()!=null) {
    				deleteResponseListener.onFailure(af.getRootFailure());
    			} else {
    				deleteResponseListener.onResponse(response);
    			}
    		} catch (Exception ex) {
    			indexResponseListener.onFailure(ex);
    		}
    	} else {
    		client.delete(dr, deleteListener);
    	}
    }
    
    /**
     * Executes a search query and returns the deferred for the results
     * @param query The query to execute
     * @return the deferred results
     */
    public Deferred<SearchQuery> executeQuery(final SearchQuery query) {
    	final Deferred<SearchQuery> result = new Deferred<SearchQuery>();
    	final SearchType searchType = query.getType();
    	SearchRequest searchRequest = new SearchRequest(indexesBySearchType.get(searchType));
        HashMap<String, Object> body = new HashMap<String, Object>(3);
        body.put("size", query.getLimit());
        body.put("from", query.getStartIndex());
        
        HashMap<String, Object> qs = new HashMap<String, Object>(1);
        body.put("query", qs);
        HashMap<String, String> query_string = new HashMap<String, String>(1);
        query_string.put("query", query.getQuery());
        qs.put("query_string", query_string);

        final byte[] source = JSON.serializeToBytes(body);
        searchRequest.source(source, 0, source.length, true);
        
        client.search(searchRequest, new ActionListener<SearchResponse>() {
			@Override
			public void onResponse(SearchResponse r) {
				Class<?> deserTo = classBySearchType.get(searchType);
				SearchHits searchHits = r.getHits();
				SearchHit[] hits = searchHits.getHits();
				final List<Object> objects = new ArrayList<Object>(hits.length);
				Object o = null;
				for(SearchHit hit: hits) {
					o = JSON.parseToObject(hit.source(), deserTo);
					switch(searchType) {
		                case TSMETA:
		                case UIDMETA:
		                case ANNOTATION:
		                	objects.add(o);
		                	break;
		                case TSMETA_SUMMARY:
		                	objects.add(summarize((TSMeta)o));
		                	break;
		                case TSUIDS:
		                	objects.add(((TSMeta)o).getTSUID());
		                	break;
					}					
				}
				query.setResults(objects);
				query.setTime(r.getTookInMillis());
				query.setTotalResults((int) searchHits.getTotalHits());
				result.callback(query);
			}
			@Override
			public void onFailure(Throwable e) {
				log.error("Search Failure. Query:\n{}", new String(source), e);
				result.callback(e);
			}
        });
    	return result;
    }
    
    /**
     * Converts the passed TSMeta into a map summary
     * @param meta The meta to summarize
     * @return the summary map
     */
    public static Map<String, Object> summarize(TSMeta meta) {
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
    	return map;
    }
	

	/**
	 * Quickie standalone test
	 * @param args None
	 */
	public static void main(String[] args) {
		TransportClient client  = null;
		try {
			log("IndexVerifier Test");
			Set<TransportAddress> addresses = new HashSet<TransportAddress>();
			InetSocketTransportAddress boot = new InetSocketTransportAddress("localhost", 9300);
			addresses.add(boot);
			Settings settings = ImmutableSettings.settingsBuilder()
			        .put("client.transport.sniff", false)
			        .put("cluster.name", "opentsdb")
			        .build();			
			client = new TransportClient(settings).addTransportAddress(boot);
			ClusterAdminClient cadmin = client.admin().cluster();
			NodesInfoResponse nir = cadmin.nodesInfo(new NodesInfoRequest().all()).actionGet(2000);
			for(NodeInfo ni: nir.getNodes()) {
				String host = ni.getHostname();
				TransportAddress ta = ni.getTransport().getAddress().boundAddress();
				if(!(ta instanceof InetSocketTransportAddress)) {
					continue;
				}
				InetSocketTransportAddress sta = (InetSocketTransportAddress)ta;
				int port = sta.address().getPort();
				log("Discovered Cluster TCP Transport: [%s:%s]", host, port);
				sta = new InetSocketTransportAddress(host, port);
				if(addresses.contains(sta)) {
					log("Boot Address. Discarding.");
				} else {
					addresses.add(sta);
					client.addTransportAddress(sta);
					log("New Address. Adding to client.");
				}
			}
			log("Connected Clients:%s", client.connectedNodes().size());
			
			Map<String, String[]> typeIndexNames = new HashMap<String, String[]>(3);
			typeIndexNames.put(DEFAULT_ES_ANNOT_TYPE, new String[] {DEFAULT_ES_ANNOT_TYPE, DEFAULT_ES_INDEX_NAME});
			typeIndexNames.put(DEFAULT_ES_TSMETA_TYPE, new String[] {DEFAULT_ES_TSMETA_TYPE, DEFAULT_ES_INDEX_NAME});
			typeIndexNames.put(DEFAULT_ES_UIDMETA_TYPE, new String[] {DEFAULT_ES_UIDMETA_TYPE, DEFAULT_ES_INDEX_NAME});
			IndexOperations iOps = new IndexOperations(client, 2000, true, false, typeIndexNames);
			for(int i = 0; i < 10; i++) {
				Annotation ann = new Annotation();
				ann.setCustom(new HashMap<String, String>(Collections.singletonMap("key" + i, "value" + i)));
				ann.setDescription("This is Annotation #" + i);
				ann.setEndTime(System.currentTimeMillis()-60000 + (i * 1000));
				ann.setStartTime(System.currentTimeMillis()-120000 + (i * 1000));
				ann.setNotes("Notes for Annotation #" + i);
				ann.setTSUID(UUID.randomUUID().toString());				
				iOps.indexAnnotation(ann);
				log("Indexed Annotation [%s]", ann);
				Thread.currentThread().join(5000);
			}
			//try { Thread.sleep(5000); } catch (Exception ex) {}
			log("Annotations indexed");
			for(Stats stats : client.threadPool().stats()) {
				log(JSON.serializeToString(stats));
			}
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		} finally {
			try { client.close(); } catch (Exception ex) {}
		}
	}
	
	public static void log(String format, Object...args) {
		System.out.println(String.format(format, args));
	}




}
