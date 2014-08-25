/**
 * 
 */
package net.opentsdb.catalog;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.helios.tsdb.plugins.remoting.json.JSONRequest;
import org.helios.tsdb.plugins.remoting.json.annotations.JSONRequestHandler;
import org.helios.tsdb.plugins.remoting.json.annotations.JSONRequestService;
import org.helios.tsdb.plugins.remoting.json.serialization.TSDBTypeSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.stumbleupon.async.Callback;
import com.stumbleupon.async.Deferred;

import net.opentsdb.meta.Annotation;
import net.opentsdb.meta.TSMeta;
import net.opentsdb.meta.UIDMeta;
import net.opentsdb.meta.api.MetricsMetaAPI;
import net.opentsdb.meta.api.QueryContext;
import net.opentsdb.uid.UniqueId.UniqueIdType;
import net.opentsdb.utils.JSON;

/**
 * <p>Title: JSONMetricsAPIService</p>
 * <p>Description: JSON service to implement remoting for the Metric API over HTTP and WebSockets</p>
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><b><code>net.opentsdb.catalog.JSONMetricsAPIService</code></b>
 */
@JSONRequestService(name="meta")
public class JSONMetricsAPIService {
	/** The Metric Meta API impl used to serve this JSON service */
	protected final MetricsMetaAPI metricApi;
	/** Instance logger */
	protected Logger log = LoggerFactory.getLogger(getClass());
	
	
	/**
	 * Creates a new JSONMetricsAPIService 
	 * @param metricApi The Metric Meta API impl used to serve this JSON service
	 */
	public JSONMetricsAPIService(MetricsMetaAPI metricApi) {
		this.metricApi = metricApi;
	}
	
	/**
	 * HTTP and WebSocket exposed interface to {@link #getMetricNames(net.opentsdb.meta.api.QueryContext, java.lang.String[])} 
	 * @param request The JSON request
	 * <p>Sample request:<pre>
	 * 				{"t":"req", "rid":1, "svc":"meta", "op":"metricswtags", "q": { "pageSize" : 10 }, "tags" : {"host" : "*", "type" : "combined"}}
	 * </pre></p>
	 */
	@JSONRequestHandler(name="metricswtags", description="Returns the MetricNames that match the passed tag pairs")
	public void jsonMetricNamesWithTags(final JSONRequest request) {
		QueryContext q = JSON.parseToObject(request.getRequest().get("q").toString(), QueryContext.class);
		ObjectNode tagNode = (ObjectNode)request.getRequest().get("tags");
		final Map<String, String> tags = new TreeMap<String, String>();
		Iterator<String> titer = tagNode.fieldNames();
		while(titer.hasNext()) {
			String key = titer.next();
			tags.put(key, tagNode.get(key).asText());
		}
		log.info("Processing jsonMetricNamesWithTags. q: [{}], tags: {}", q, tags);		
		metricApi.getMetricNames(q.startExpiry(), tags).addCallback(new ResultCompleteCallback<Set<UIDMeta>>(request, q));
	}
	
	
	/**
	 * HTTP and WebSocket exposed interface to {@link MetricsMetaAPI#getMetricNames(net.opentsdb.meta.api.QueryContext, java.lang.String[])} 
	 * @param request The JSON request
	 * <p>Sample request:<pre>
	 * 				{"t":"req", "rid":1, "svc":"meta", "op":"metricnames", "q": { "pageSize" : 10 }, "keys" : ["host", "type", "cpu"] }
	 * </pre></p>
	 * API:  ws.serviceRequest("meta", "metricnames", {q: q, keys : ['host', 'type', 'cpu']})
	 */
	@JSONRequestHandler(name="metricnames", description="Returns the MetricNames that match the passed tag keys")
	public void jsonMetricNames(final JSONRequest request) {
		QueryContext q = JSON.parseToObject(request.getRequest().get("q").toString(), QueryContext.class);
		final ArrayNode keysArr = (ArrayNode)request.getRequest().get("keys");
		final String[] keys = new String[keysArr.size()];
		for(int i = 0; i < keysArr.size(); i++) {
			keys[i] = keysArr.get(i).asText();
		}
		log.info("Processing JSONMetricNames. q: [{}], keys: {}", q, Arrays.toString(keys));		
		metricApi.getMetricNames(q.startExpiry(), keys).addCallback(new ResultCompleteCallback<Set<UIDMeta>>(request, q));
	}
	
	// public Deferred<Set<UIDMeta>> getTagKeys(final QueryContext queryOptions, final String metric, final String...tagKeys) {
	/**
	 * @param request
	 */	
	@JSONRequestHandler(name="tagkeys", description="Returns the Tag Key UIDs that match the passed metric name and tag keys")
	public void jsonTagKeys(final JSONRequest request) {
		QueryContext q = JSON.parseToObject(request.getRequest().get("q").toString(), QueryContext.class);
		final String metricName = request.getRequest().get("m").textValue();
		final ArrayNode keysArr = (ArrayNode)request.getRequest().get("keys");
		final String[] keys = new String[keysArr.size()];
		for(int i = 0; i < keysArr.size(); i++) {
			keys[i] = keysArr.get(i).asText();
		}
		log.info("Processing JSONTagKeys. q: [{}], m: [{}], keys: {}", q, metricName, Arrays.toString(keys));		
		metricApi.getTagKeys(q.startExpiry(), metricName, keys).addCallback(new ResultCompleteCallback<Set<UIDMeta>>(request, q));
	}
		
	/**
	 * @param request
	 */
	@JSONRequestHandler(name="tagvalues", description="Returns the Tag Value UIDs that match the passed metric name and tag keys")
	public void jsonTagValues(final JSONRequest request) { 
		QueryContext q = JSON.parseToObject(request.getRequest().get("q").toString(), QueryContext.class);
		final String metricName = request.getRequest().get("m").textValue();
		final String tagKey = request.getRequest().get("k").textValue();
		final ObjectNode tagNode = (ObjectNode)request.getRequest().get("tags");
		final Map<String, String> tags = new TreeMap<String, String>();
		Iterator<String> titer = tagNode.fieldNames();
		while(titer.hasNext()) {
			String key = titer.next();
			tags.put(key, tagNode.get(key).asText());
		}
		log.info("Processing JSONTagValues. q: [{}], m: [{}], tags: {}", q, metricName, tags);		
		metricApi.getTagValues(q.startExpiry(), metricName, tags, tagKey).addCallback(new ResultCompleteCallback<Set<UIDMeta>>(request, q));
	}
	
	

	/**
	 * @param request
	 */
	@JSONRequestHandler(name="tsmetaexpr", description="Returns the TSMetas that match the passed expression")
	public void jsonTSMetaExpression(final JSONRequest request) { 
		QueryContext q = JSON.parseToObject(request.getRequest().get("q").toString(), QueryContext.class);		
		final String expression = request.getRequest().get("x").textValue();
		log.info("Processing JSONTSMetaExpression. q: [{}], x: [{}]", q, expression);		
		metricApi.evaluate(q.startExpiry(), expression).addCallback(new ResultCompleteCallback<Set<TSMeta>>(request, q));
	}
	
	@JSONRequestHandler(name="d3tsmeta", description="Returns the d3 json graph for the TSMetas that match the passed expression")
	public void d3JsonTSMetaExpression(final JSONRequest request) { 
		QueryContext q = JSON.parseToObject(request.getRequest().get("q").toString(), QueryContext.class);		
		final String expression = request.getRequest().get("x").textValue();
		log.info("Processing JSONTSMetaExpression. q: [{}], x: [{}]", q, expression);		
		metricApi.evaluate(q.startExpiry(), expression).addCallback(new ResultCompleteCallback<Set<TSMeta>>(request, q));
//		evaluate(q.startExpiry(), expression).addCallback(new Callback<TSMetaTree, Set<TSMeta>>() {
//			@Override
//			public TSMetaTree call(Set<TSMeta> tsMetas) throws Exception {
//				return TSMetaTree.build("org", tsMetas);
//			}
//		}).addCallback(new ResultCompleteCallback<TSMetaTree>(request, q));
	}
	
	/**
	 * HTTP and WebSocket exposed interface to {@link #getTSMetas(net.opentsdb.meta.api.QueryContext, java.lang.String, java.util.Map)} 
	 * @param request The JSON request
	 * <p>Sample request:<pre>
	 * 				{"t":"req", "rid":1, "svc":"meta", "op":"tsmetas", "q": { "pageSize" : 10 }, "m":"sys.cp*", "overflow":false,  "tags" : {"host" : "*NWHI*"}}
	 * </pre></p>
	 */
	@JSONRequestHandler(name="tsmetas", description="Returns the MetricNames that match the passed tag pairs")
	public void jsonTSMetas(final JSONRequest request) {
		final QueryContext q = JSON.parseToObject(request.getRequest().get("q").toString(), QueryContext.class);
		final String metricName = request.getRequest().get("m").textValue();
		final ObjectNode tagNode = (ObjectNode)request.getRequest().get("tags");
		final Map<String, String> tags = new TreeMap<String, String>();
		Iterator<String> titer = tagNode.fieldNames();
		while(titer.hasNext()) {
			String key = titer.next();
			tags.put(key, tagNode.get(key).asText());
		}
		log.info("Processing jsonTSMetas. q: [{}], tags: {}", q, tags);
		doJsonMetas(request, q, metricName, tags);
//		getTSMetas(q.startExpiry(), metricName, tags).addCallback(new ResultCompleteCallback<Set<TSMeta>>(request, q))
//			.addCallback(new Callback<Void, QueryContext>() {
//				@Override
//				public Void call(QueryContext ctx) throws Exception {
//					return null;
//				}
//			});		
	}
	
	protected void doJsonMetas(final JSONRequest request, final QueryContext q, final String metricName, final Map<String, String> tags) {
		metricApi.getTSMetas(q.startExpiry(), metricName, tags).addCallback(new ResultCompleteCallback<Set<TSMeta>>(request, q))
		.addCallback(new Callback<Void, QueryContext>() {
			@Override
			public Void call(QueryContext ctx) throws Exception {
				if(ctx.shouldContinue()) {
					doJsonMetas(request, ctx, metricName, tags);
				}
				return null;
			}
		});		
		
	}
	
	@JSONRequestHandler(name="finduid", description="Returns all UIDMetas of the specified type that match the passed name")
	public void find(final JSONRequest request) {
		final QueryContext q = JSON.parseToObject(request.getRequest().get("q").toString(), QueryContext.class);
		final String name = request.getRequest().get("name").textValue();
		final UniqueIdType type = UniqueIdType.valueOf(request.getRequest().get("type").textValue().trim().toUpperCase());
		doFind(request, q, type, name);		
	}
	
	
	protected void doFind(final JSONRequest request, final QueryContext q, final UniqueIdType type, final String name) {
		// public Deferred<Set<UIDMeta>> find(final QueryContext queryContext, final UniqueIdType type, final String name) {
		metricApi.find(q, type, name).addCallback(new ResultCompleteCallback<Set<UIDMeta>>(request, q))
		.addCallback(new Callback<Void, QueryContext>() {
			@Override
			public Void call(QueryContext ctx) throws Exception {
				if(ctx.shouldContinue()) {
					doFind(request, ctx, type, name);
				}
				return null;
			}
		});	
	}
	
	@JSONRequestHandler(name="annotations", description="Returns all Annotations associated to TSMetas defined in the expression")
	public void jsonGetAnnotations(final JSONRequest request) {
		final QueryContext q = JSON.parseToObject(request.getRequest().get("q").toString(), QueryContext.class);
		final String expr = request.getRequest().has("x") ? request.getRequest().get("x").asText() : null; 
		ArrayNode ar = (ArrayNode)request.getRequest().get("x");
		final int sz = ar.size();
		final long[] range = new long[ar.size()];
		for(int i = 0; i < sz; i++) {
			range[i] = ar.get(i).asLong();
		}
		doGetAnnotations(request, q, expr, range);
	}

	protected void doGetAnnotations(final JSONRequest request, final QueryContext q, final String expression, final long... range) {
		final Deferred<Set<Annotation>> def;
		if(expression==null) {
			def = metricApi.getGlobalAnnotations(q, range);
		} else {
			def = metricApi.getAnnotations(q, expression, range);
		}
		
		def.addCallback(new ResultCompleteCallback<Set<Annotation>>(request, q))
			.addCallback(new Callback<Void, QueryContext>() {
				@Override
				public Void call(QueryContext ctx) throws Exception {
					if(ctx.shouldContinue()) {
						doGetAnnotations(request, ctx, expression, range);
					}
					return null;
				}
			});	
		}
	
	
	
	// public Deferred<Set<Annotation>> getAnnotations(QueryContext queryContext, String expression, long... startTimeEndTime);
	
	/**
	 * <p>Title: ResultCompleteCallback</p>
	 * <p>Description: Suasync callback handler to write the results of a MetricMetaAPI query result back to the calling client </p>
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><b><code>net.opentsdb.catalog.JSONMetricsAPIService.ResultCompleteCallback</code></b>
	 * @param <T> The assumed type of the object being marshalled
	 */
	class ResultCompleteCallback<T> implements Callback<QueryContext, T> {
		/** The original JSON request */
		private final JSONRequest request;		
		/** The current query context */
		private final QueryContext ctx;
		/** The result node */
		private final ArrayNode an = JSON.getMapper().createArrayNode();
		/**
		 * Creates a new ResultCompleteCallback
		 * @param request The original JSON request
		 * @parma ctx The current query context
		 */
		public ResultCompleteCallback(JSONRequest request, QueryContext ctx) {
			this.request = request;
			this.ctx = ctx;
		}
		@Override
		public QueryContext call(T result) throws Exception {
			try {				
				if(ctx.isExpired()) {
					an.insertPOJO(0, ctx);
					an.insert(0, "The request timed out");	
					request.response().setOpCode("timeout").setContent(an).send();
				} else {					
					an.insertPOJO(0, ctx);
					an.insertPOJO(0, result);
					an.insertPOJO(0, TSDBTypeSerializer.DEFAULT);
					request.response().setContent(an).send();
				}
			} catch (Exception e) {
				request.error("Failed to get metric names", e);
				log.error("Failed to get metric names", e);
			}			
			return ctx;
		}
	}
	

}
