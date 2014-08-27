/**
 * 
 */
package net.opentsdb.catalog;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import net.opentsdb.meta.Annotation;
import net.opentsdb.meta.TSMeta;
import net.opentsdb.meta.UIDMeta;
import net.opentsdb.meta.api.MetricsMetaAPI;
import net.opentsdb.meta.api.QueryContext;
import net.opentsdb.uid.UniqueId.UniqueIdType;
import net.opentsdb.utils.JSON;

import org.helios.tsdb.plugins.remoting.json.JSONRequest;
import org.helios.tsdb.plugins.remoting.json.annotations.JSONRequestHandler;
import org.helios.tsdb.plugins.remoting.json.annotations.JSONRequestService;
import org.helios.tsdb.plugins.remoting.json.serialization.Serializers.TSMetaTree;
import org.helios.tsdb.plugins.remoting.json.serialization.TSDBTypeSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.stumbleupon.async.Callback;
import com.stumbleupon.async.Deferred;

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
	/**
	 * @param request
	 * @param q
	 * @param tags
	 */
	@JSONRequestHandler(name="metricswtags", description="Returns the MetricNames that match the passed tag pairs")
	public void jsonMetricNamesWithTags(final JSONRequest request, final QueryContext q, final Map<String, String> tags) {
		if(q==null) {
			jsonMetricNamesWithTags(
				request,
				JSON.parseToObject(request.getRequest().get("q").toString(), QueryContext.class),
				getMap(request, "tags")				
			);
		} else {
			log.info("Processing JSONMetricNames. q: [{}], tags: {}", q, tags);		
			request.response().setOverrideObjectMapper(TSDBTypeSerializer.valueOf(q.getFormat()).getMapper());
			metricApi.getMetricNames(q.startExpiry(), tags).addCallback(new ResultCompleteCallback<Set<UIDMeta>>(request, q))
			.addCallback(new Callback<Void, QueryContext>() {
				@Override
				public Void call(QueryContext ctx) throws Exception {
					if(ctx.shouldContinue()) {
						jsonMetricNamesWithTags(request, ctx, tags);
					}
					return null;
				}
			});					
		}
	}
	
	/**
	 * HTTP and WebSocket exposed interface to {@link MetricsMetaAPI#getMetricNames(net.opentsdb.meta.api.QueryContext, java.lang.String[])} 
	 * @param request The JSON request
	 * <p>Sample request:<pre>
	 * 				{"t":"req", "rid":1, "svc":"meta", "op":"metricnames", "q": { "pageSize" : 10 }, "keys" : ["host", "type", "cpu"] }
	 * </pre></p>
	 * API:  ws.serviceRequest("meta", "metricnames", {q: q, keys : ['host', 'type', 'cpu']})
	 */
	/**
	 * @param request
	 * @param q
	 * @param tagKeys
	 */
	@JSONRequestHandler(name="metricnames", description="Returns the MetricNames that match the passed tag keys")
	public void jsonMetricNames(final JSONRequest request, final QueryContext q, final String...tagKeys) {
		if(q==null) {
			jsonMetricNames(
				request,
				JSON.parseToObject(request.getRequest().get("q").toString(), QueryContext.class),
				getStringArray(request, "keys")				
			);
		} else {
			log.info("Processing JSONMetricNames. q: [{}], keys: {}", q, Arrays.toString(tagKeys));		
			request.response().setOverrideObjectMapper(TSDBTypeSerializer.valueOf(q.getFormat()).getMapper());
			metricApi.getMetricNames(q.startExpiry(), tagKeys).addCallback(new ResultCompleteCallback<Set<UIDMeta>>(request, q))
			.addCallback(new Callback<Void, QueryContext>() {
				@Override
				public Void call(QueryContext ctx) throws Exception {
					if(ctx.shouldContinue()) {
						jsonMetricNames(request, ctx, tagKeys);
					}
					return null;
				}
			});					
		}
	}
	
	/**
	 * @param request
	 * @param q
	 * @param metric
	 * @param tagKeys
	 */
	@JSONRequestHandler(name="tagkeys", description="Returns the Tag Key UIDs that match the passed metric name and tag keys")
	public void jsonTagKeys(final JSONRequest request, final QueryContext q, final String metric, final String...tagKeys) {
		if(q==null) {
			jsonTagKeys(
				request,
				JSON.parseToObject(request.getRequest().get("q").toString(), QueryContext.class),
				request.getRequest().get("m").textValue(),
				getStringArray(request, "keys")	
			);
		} else {
			log.info("Processing JSONTagKeys. q: [{}], m: [{}], keys: {}", q, metric, Arrays.toString(tagKeys));					
			request.response().setOverrideObjectMapper(TSDBTypeSerializer.valueOf(q.getFormat()).getMapper());
			metricApi.getTagKeys(q.startExpiry(), metric, tagKeys).addCallback(new ResultCompleteCallback<Set<UIDMeta>>(request, q))
			.addCallback(new Callback<Void, QueryContext>() {
				@Override
				public Void call(QueryContext ctx) throws Exception {
					if(ctx.shouldContinue()) {
						jsonTagKeys(request, ctx, metric, tagKeys);
					}
					return null;
				}
			});
		}
	}
		
	/**
	 * @param request
	 * @param q
	 * @param metricName
	 * @param tags
	 * @param tagKey
	 */
	@JSONRequestHandler(name="tagvalues", description="Returns the Tag Value UIDs that match the passed metric name and tag keys")
	public void jsonTagValues(final JSONRequest request, final QueryContext q, final String metricName, final Map<String, String> tags, final String tagKey) {
		if(q==null) {
			jsonTagValues(
				request, 
				JSON.parseToObject(request.getRequest().get("q").toString(), QueryContext.class),
				request.getRequest().get("m").textValue(),
				getMap(request, "tags"),
				request.getRequest().get("k").textValue()
			);
		} else {
			log.info("Processing JSONTagValues. q: [{}], m: [{}], tags: {}", q, metricName, tags);
			request.response().setOverrideObjectMapper(TSDBTypeSerializer.valueOf(q.getFormat()).getMapper());
			metricApi.getTagValues(q.startExpiry(), metricName, tags, tagKey).addCallback(new ResultCompleteCallback<Set<UIDMeta>>(request, q))
			.addCallback(new Callback<Void, QueryContext>() {
				@Override
				public Void call(QueryContext ctx) throws Exception {
					if(ctx.shouldContinue()) {
						jsonTagValues(request, q, metricName, tags, tagKey);
					}
					return null;
				}
			});							
		}
	}

	/**
	 * @param request
	 * @param q 
	 * @param expression 
	 */
	@JSONRequestHandler(name="tsMetaEval", description="Returns the TSMetas that match the passed expression")
	public void tsMetaX(final JSONRequest request, final QueryContext q, final String expression) {		
		if(q==null) {
			tsMetaX(
				request,
				JSON.parseToObject(request.getRequest().get("q").toString(), QueryContext.class),
				request.getRequest().get("x").textValue()					
			);			
		} else {
			log.info("Processing JSONTSMetaExpression. q: [{}], x: [{}]", q, expression);
			request.response().setOverrideObjectMapper(TSDBTypeSerializer.valueOf(q.getFormat()).getMapper());
			metricApi.evaluate(q.startExpiry(), expression).addCallback(new ResultCompleteCallback<Set<TSMeta>>(request, q))			
			.addCallback(new Callback<Void, QueryContext>() {
				@Override
				public Void call(QueryContext ctx) throws Exception {
					if(ctx.shouldContinue()) {
						tsMetaX(request, ctx, expression);
					}
					return null;
				}
			});					
		}
	}
	
	
	/**
	 * @param request
	 * @param q
	 * @param expression
	 */
	@JSONRequestHandler(name="d3tsmeta", description="Returns the d3 json graph for the TSMetas that match the passed expression")
	public void d3JsonTSMetaExpression(final JSONRequest request, final QueryContext q, final String expression) {
		if(q==null) {
			d3JsonTSMetaExpression(
				request,
				JSON.parseToObject(request.getRequest().get("q").toString(), QueryContext.class),
				request.getRequest().get("x").textValue()
			);
		} else {
			request.response().setOverrideObjectMapper(TSDBTypeSerializer.valueOf(q.getFormat()).getMapper());
			metricApi.evaluate(q.startExpiry(), expression).addCallback(new Callback<TSMetaTree, Set<TSMeta>>() {
				@Override
				public TSMetaTree call(Set<TSMeta> tsMetas) throws Exception {
					return TSMetaTree.build("org", tsMetas);
				}
			}).addCallback(new ResultCompleteCallback<TSMetaTree>(request, q));			
		}
	}
	
	/**
	 * HTTP and WebSocket exposed interface to {@link #getTSMetas(net.opentsdb.meta.api.QueryContext, java.lang.String, java.util.Map)} 
	 * @param request The JSON request
	 * <p>Sample request:<pre>
	 * 				{"t":"req", "rid":1, "svc":"meta", "op":"tsmetas", "q": { "pageSize" : 10 }, "m":"sys.cp*", "overflow":false,  "tags" : {"host" : "*NWHI*"}}
	 * </pre></p>
	 */
	/**
	 * @param request
	 * @param q
	 * @param metricName
	 * @param tags
	 */
	@JSONRequestHandler(name="tsmetas", description="Returns the MetricNames that match the passed tag pairs")
	public void jsonTSMetas(final JSONRequest request, final QueryContext q, final String metricName, final Map<String, String> tags) {
		if(q==null) {
			jsonTSMetas(
				request, 
				JSON.parseToObject(request.getRequest().get("q").toString(), QueryContext.class),
				request.getRequest().get("m").textValue(),
				getMap(request, "tags")
			);
		} else {
			request.response().setOverrideObjectMapper(TSDBTypeSerializer.valueOf(q.getFormat()).getMapper());
			metricApi.getTSMetas(q.startExpiry(), metricName, tags).addCallback(new ResultCompleteCallback<Set<TSMeta>>(request, q))
			.addCallback(new Callback<Void, QueryContext>() {
				@Override
				public Void call(QueryContext ctx) throws Exception {
					if(ctx.shouldContinue()) {
						jsonTSMetas(request, ctx, metricName, tags);
					}
					return null;
				}
			});		
		}
	}
	
	
	/**
	 * @param request
	 * @param q
	 * @param type
	 * @param name
	 */
	@JSONRequestHandler(name="finduid", description="Returns all UIDMetas of the specified type that match the passed name")
	public void find(final JSONRequest request, final QueryContext q, final UniqueIdType type, final String name) {
		if(q==null) {
			find(
				request,
				JSON.parseToObject(request.getRequest().get("q").toString(), QueryContext.class),
				UniqueIdType.valueOf(request.getRequest().get("type").textValue().trim().toUpperCase()),
				request.getRequest().get("name").textValue()
			);
		} else {
			request.response().setOverrideObjectMapper(TSDBTypeSerializer.valueOf(q.getFormat()).getMapper());
			metricApi.find(q.startExpiry(), type, name).addCallback(new ResultCompleteCallback<Set<UIDMeta>>(request, q))
			.addCallback(new Callback<Void, QueryContext>() {
				@Override
				public Void call(QueryContext ctx) throws Exception {
					if(ctx.shouldContinue()) {
						find(request, ctx, type, name);
					}
					return null;
				}
			});				
		}
	}
	
	/**
	 * @param request
	 * @param q
	 * @param expression
	 * @param range
	 */
	@JSONRequestHandler(name="annotations", description="Returns all Annotations associated to TSMetas defined in the expression")
	public void jsonGetAnnotations(final JSONRequest request, final QueryContext q, final String expression, final long... range) {
		if(q==null) {
			jsonGetAnnotations(
					request,
					JSON.parseToObject(request.getRequest().get("q").toString(), QueryContext.class),
					request.getRequest().has("x") ? request.getRequest().get("x").asText() : null,
							getLongArray(request, "r")
					);
		} else {
			request.response().setOverrideObjectMapper(TSDBTypeSerializer.valueOf(q.getFormat()).getMapper());
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
						jsonGetAnnotations(request, ctx, expression, range);
					}
					return null;
				}
			});	
		}
	}

	
	/**
	 * Extracts the named string array from the JSONRequest
	 * @param request the JSONRequest to get the array from
	 * @param key the json name of the array
	 * @return the read string array
	 */
	public static String[] getStringArray(final JSONRequest request, final String key) {
		final ArrayNode arrayNode = (ArrayNode)request.getRequest().get(key);
		final String[] arr = new String[arrayNode.size()];
		for(int i = 0; i < arrayNode.size(); i++) {
			arr[i] = arrayNode.get(i).asText();
		}		
		return arr;
	}
	
	/**
	 * Extracts the named long array from the JSONRequest
	 * @param request the JSONRequest to get the array from
	 * @param key the json name of the array
	 * @return the read long array
	 */
	public static long[] getLongArray(final JSONRequest request, final String key) {
		final ArrayNode arrayNode = (ArrayNode)request.getRequest().get(key);
		final long[] arr = new long[arrayNode.size()];
		for(int i = 0; i < arrayNode.size(); i++) {
			arr[i] = arrayNode.get(i).asLong();
		}		
		return arr;
	}
	
	
	/**
	 * Extracts the named map from the JSONRequest
	 * @param request the JSONRequest to get the map from
	 * @param key the json name of the map
	 * @return the read map
	 */
	public static Map<String, String> getMap(final JSONRequest request, final String key) {
		ObjectNode tagNode = (ObjectNode)request.getRequest().get(key);
		final Map<String, String> map = new LinkedHashMap<String, String>();
		Iterator<String> titer = tagNode.fieldNames();
		while(titer.hasNext()) {
			String k = titer.next();
			map.put(k, tagNode.get(k).asText());
		}
		return map;		
	}
	
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
		 * @param q The current query context
		 */
		public ResultCompleteCallback(JSONRequest request, QueryContext q) {
			this.request = request;
			this.ctx = q;
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
					request.response()
						.setOverrideObjectMapper(ctx.getMapper())
						.setContent(an)
						.send();
//					request.response().setContent(an).send();
				}
			} catch (Exception e) {
				request.error("Failed to get metric names", e);
				log.error("Failed to get metric names", e);
			}			
			return ctx;
		}
	}
	

}
