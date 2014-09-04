/**
 * 
 */
package net.opentsdb.catalog;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import net.opentsdb.meta.Annotation;
import net.opentsdb.meta.TSMeta;
import net.opentsdb.meta.api.MetricsMetaAPI;
import net.opentsdb.meta.api.QueryContext;
import net.opentsdb.uid.UniqueId.UniqueIdType;
import net.opentsdb.utils.JSON;

import org.helios.tsdb.plugins.remoting.json.JSONRequest;
import org.helios.tsdb.plugins.remoting.json.JSONResponse;
import org.helios.tsdb.plugins.remoting.json.annotations.JSONRequestHandler;
import org.helios.tsdb.plugins.remoting.json.annotations.JSONRequestService;
import org.helios.tsdb.plugins.remoting.json.serialization.TSDBTypeSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.composable.Promise;
import reactor.core.composable.Stream;
import reactor.function.Consumer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.stumbleupon.async.Deferred;

/**
 * <p>Title: JSONMetricsAPIService</p>
 * <p>Description: JSON service to implement remoting for the Metric API over HTTP and WebSockets</p>
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><b><code>net.opentsdb.catalog.JSONMetricsAPIService</code></b>
 */
@JSONRequestService(name="meta", description="JSON service to implement remoting for the Metric API over HTTP and WebSockets")
public class JSONMetricsAPIService {
	/** The Metric Meta API impl used to serve this JSON service */
	protected final MetricsMetaAPI metricApi;
	/** Instance logger */
	protected final Logger log = LoggerFactory.getLogger(getClass());
	
	/** The ctx name for this class */
	protected final String ctxName = getClass().getSimpleName();
	/** The ctx name for the accept time */
	protected final String ctxAcceptName = getClass().getSimpleName() + "Accepted";
	
	/**
	 * Creates a new JSONMetricsAPIService 
	 * @param metricApi The Metric Meta API impl used to serve this JSON service
	 */
	public JSONMetricsAPIService(MetricsMetaAPI metricApi) {
		this.metricApi = metricApi;
	}
	
	/**
	 * HTTP and WebSocket exposed interface to {@link MetricsMetaAPI#getMetricNames(net.opentsdb.meta.api.QueryContext, java.lang.String[])}
	 * @param request The JSON request
	 * @param q The query context
	 * @param tags The TSMeta tags to match the metric names for
	 * <p>Sample request:<pre>
		{
		  "t": "req",
		  "rid": 1,
		  "svc": "meta",
		  "op": "metricswtags",
		  "q": {
		    "pageSize": 10
		  },
		  "tags": {
		    "host": "*",
		    "type": "combined"
		  }
		}
	 * </pre></p>
	 */
	@JSONRequestHandler(name="metricswtags", description="Returns the MetricNames that match the passed tag pairs")
	public void getMetricNamesWithTagsJSON(final JSONRequest request, final QueryContext q, final Map<String, String> tags) {
		if(q==null) {
			getMetricNamesWithTagsJSON(
				request,
				JSON.parseToObject(request.getRequest().get("q").toString(), QueryContext.class).addCtx(ctxName, System.currentTimeMillis()),
				getMap(request, "tags")				
			);
		} else {
			log.info("Processing JSONMetricNames. q: [{}], tags: {}", q, tags);					
			attachBatchHandlers(metricApi.getMetricNames(q, tags), q, request);			
		}
	}
	
	/**
	 * HTTP and WebSocket exposed interface to {@link MetricsMetaAPI#getMetricNames(net.opentsdb.meta.api.QueryContext, java.lang.String[])}
	 * @param request The JSON request
	 * @param q The query context
	 * @param tagKeys an array of tag keys to exclude
	 * <p>Sample request:<pre>
	 * 
	 * </pre></p>
	 * FIXME: merge metric name functions
	 */
	@JSONRequestHandler(name="metricnames", description="Returns the MetricNames that match the passed tag keys")
	public void getMetricNamesJSON(final JSONRequest request, final QueryContext q, final String...tagKeys) {
		if(q==null) {
			getMetricNamesJSON(
				request,
				JSON.parseToObject(request.getRequest().get("q").toString(), QueryContext.class).addCtx(ctxName, System.currentTimeMillis()),
				getStringArray(request, "keys")				
			);
		} else {
			log.info("Processing JSONMetricNames. q: [{}], keys: {}", q, Arrays.toString(tagKeys));		
			attachBatchHandlers(metricApi.getMetricNames(q.startExpiry(), tagKeys), q, request);	
		}
	}
	
	/**
	 * HTTP and WebSocket exposed interface to {@link MetricsMetaAPI#getTagKeys(QueryContext, String, String...)}
	 * @param request The JSON request
	 * @param q The query context
	 * @param metricName The optional TSMeta metric name to match
	 * @param tagKeys The tag keys to match
	 * <p>Sample request:<pre>
			{
			  "t": "req",
			  "rid": 4,
			  "svc": "meta",
			  "op": "tagkeys",
			  "q": {
			    "nextIndex": null,
			    "pageSize": 100,
			    "maxSize": 2000,
			    "timeout": 500,
			    "continuous": false,
			    "format": "DEFAULT",
			    "exhausted": false,
			    "cummulative": 0,
			    "elapsed": -1,
			    "expired": false
			  },
			  "keys": [
			    "dc",
			    "host"
			  ],
			  "m": "sys*"
			}
	 * </pre></p>
	 */
	@JSONRequestHandler(name="tagkeys", description="Returns the Tag Key UIDs that match the passed metric name and tag keys")
	public void getTagKeysJSON(final JSONRequest request, final QueryContext q, final String metricName, final String...tagKeys) {
		if(q==null) {
			getTagKeysJSON(
				request,
				JSON.parseToObject(request.getRequest().get("q").toString(), QueryContext.class).addCtx(ctxName, System.currentTimeMillis()),
				request.getRequest().get("m").textValue(),
				getStringArray(request, "keys")	
			);
		} else {
			log.info("Processing JSONTagKeys. q: [{}], m: [{}], keys: {}", q, metricName, Arrays.toString(tagKeys));
			attachBatchHandlers(metricApi.getTagKeys(q.startExpiry(), metricName, tagKeys), q, request);			
		}
	}
		
	/**
	 * HTTP and WebSocket exposed interface to {@link MetricsMetaAPI#getTagValues(QueryContext, String, Map, String)}
	 * @param request The JSON request
	 * @param q The query context
	 * @param metricName The optional TSMeta metric name to match
	 * @param tags The TSMeta tags to match
	 * @param tagKey The value of the tag key to get values for
	 * <p>Sample request:<pre>
			{
			  "t": "req",
			  "rid": 12,
			  "svc": "meta",
			  "op": "tagvalues",
			  "q": {
			    "nextIndex": null,
			    "pageSize": 100,
			    "maxSize": 2000,
			    "timeout": 500,
			    "continuous": false,
			    "format": "DEFAULT",
			    "exhausted": false,
			    "cummulative": 0,
			    "elapsed": -1,
			    "expired": false
			  },
			  "tags": {
			    "host": "*Server*",
			    "cpu": "*"
			  },
			  "m": "sys.cpu",
			  "k": "type"
			}
	 * </pre></p>
	 */
	@JSONRequestHandler(name="tagvalues", description="Returns the Tag Value UIDs that match the passed metric name and tag keys")
	public void getTagValuesJSON(final JSONRequest request, final QueryContext q, final String metricName, final Map<String, String> tags, final String tagKey) {
		if(q==null) {
			getTagValuesJSON(
				request, 
				JSON.parseToObject(request.getRequest().get("q").toString(), QueryContext.class).addCtx(ctxName, System.currentTimeMillis()),
				request.getRequest().get("m").textValue(),
				getMap(request, "tags"),
				request.getRequest().get("k").textValue()
			);
		} else {
			log.info("Processing JSONTagValues. q: [{}], m: [{}], tags: {}", q, metricName, tags);
			attachBatchHandlers(metricApi.getTagValues(q.startExpiry(), metricName, tags, tagKey), q, request);
		}
	}

	
	/**
	 * HTTP and WebSocket exposed interface to {@link MetricsMetaAPI#getTagValues(QueryContext, String, Map, String)}
	 * @param request The JSON request
	 * @param q The query context
	 * @param expression The TSMeta fully qualified metric name pattern to match. An expression is basically an {@link javax.management.ObjectName} analog where
	 * the {@link javax.management.ObjectName#getDomain()} value is the metric name and the {@link javax.management.ObjectName#getKeyPropertyList()}
	 * map represents the tags. Supports <b><code>*</code></b> wildcards for all segments and <b><code>|</code></b> multipliers for tag keys. 
	 * <p>Sample request:<pre>
			{
			  "t": "req",
			  "rid": 13,
			  "svc": "meta",
			  "op": "tsMetaEval",
			  "q": {
			    "nextIndex": null,
			    "pageSize": 100,
			    "maxSize": 2000,
			    "timeout": 500,
			    "continuous": false,
			    "format": "DEFAULT",
			    "exhausted": false,
			    "cummulative": 0,
			    "elapsed": -1,
			    "expired": false
			  },
			  "x": "sys*:dc=dc*,host=WebServer1|WebServer5"
			}
	 * </pre></p>
	 */
	@JSONRequestHandler(name="tsMetaEval", description="Returns the TSMetas that match the passed expression")
	public void evaluateJSON(final JSONRequest request, final QueryContext q, final String expression) {		
		if(q==null) {
			evaluateJSON(
				request,
				JSON.parseToObject(request.getRequest().get("q").toString(), QueryContext.class).addCtx(ctxName, System.currentTimeMillis()),
				request.getRequest().get("x").textValue()					
			);			
		} else {
			log.debug("Processing JSONTSMetaExpression. \n\tQueryContext: [{}], \n\tExpression: [{}]", q, expression);
			attachBatchHandlers(metricApi.evaluate(q, expression), q, request);			
		}
	}
	
	@JSONRequestHandler(name="overlap", description="Determines how may items are common between the two passed expressions")
	public void overlap(final JSONRequest request, final QueryContext q, final String expressionOne, final String expressionTwo) {
		if(q==null) {
			overlap(
				request,
				JSON.parseToObject(request.getRequest().get("q").toString(), QueryContext.class),
				request.getRequest().get("x").textValue(),
				request.getRequest().get("y").textValue()
			);
		} else {
			metricApi.overlap(expressionOne, expressionTwo).onSuccess(new Consumer<Long>() {
				@Override
				public void accept(Long t) {					
					request.response().setContent(JSON.getMapper().createObjectNode().putPOJO("q", q).put("result", t)).send();
				}
			}).onError(new Consumer<Throwable>() {
				@Override
				public void accept(Throwable t) {
					request.error("Failed to execute overlap", t).send();
				}
			});
		}
	}
	
	
	
	/**
	 * Attaches the consume and error handlers to the passed stream
	 * @param stream The stream to attach handlers to
	 * @param q The query context
	 * @param request The JSONRequest
	 */
	protected final <T> void attachBatchHandlers(Stream<T> stream, final QueryContext q, final JSONRequest request) {
		stream.consume(new Consumer<T>(){
			@Override
			public void accept(T t) {
				try {
					
					final JSONResponse response = request.response();
					response.setOpCode("results");											
					JsonGenerator jgen = response.writeHeader(true);
					jgen.setCodec(q.getMapper());
					jgen.writeObjectField("results", t);
					q.addCtx(ctxAcceptName, System.currentTimeMillis());
					jgen.writeObjectField("q", q);	
					response.closeGenerator();
				}  catch (Exception ex) {
					log.error("Failed to write result batch", ex);
					throw new RuntimeException("Failed to write result batch", ex);
				}					
			}
		})
		.when(Throwable.class, new Consumer<Throwable>(){
			@Override
			public void accept(Throwable t) {
				q.setExhausted(true);
				final JSONResponse response = request.response();					
				try {
					response.resetChannelOutputStream();
					response.setOpCode("error");					
					final JsonGenerator jgen = response.writeHeader(true);							
					String message = t.getMessage();
					if(message==null || message.trim().isEmpty()) {
						message = t.getClass().getSimpleName();
					}
					jgen.writeObjectField("error", message);
					jgen.writeObjectField("q", q);
					response.closeGenerator();
					log.warn("Exception message dispatched");
				} catch (Exception ex) {
					throw new RuntimeException("Failed to write timeout response to JSON output streamer", ex);
				}												
			}
		});
//		.timeout(q.getTimeout());		
	}
	
	/**
	 * Attaches the D3 serialization consumee and error handlers to the passed stream
	 * @param stream The stream to attach handlers to
	 * @param q The query context
	 * @param request The JSONRequest
	 */
	protected final <T> void attachD3Handlers(Stream<T> stream, final QueryContext q, final JSONRequest request) {
		stream.consume(new Consumer<T>(){
			final LinkedHashSet<TSMeta> set = new LinkedHashSet<TSMeta>(q.getNextMaxLimit());
			@SuppressWarnings("unchecked")
			@Override
			public void accept(T t) {
				try {
					set.addAll((Collection<TSMeta>) t);
					final JSONResponse response = request.response();
					response.setOpCode("results");											
					JsonGenerator jgen = response.writeHeader(true);
					jgen.setCodec(q.getMapper());
					jgen.writeObjectField("results", set);
					q.addCtx(ctxAcceptName, System.currentTimeMillis());
					jgen.writeObjectField("q", q);	
					response.closeGenerator();
				}  catch (Exception ex) {
					log.error("Failed to write result batch", ex);
					throw new RuntimeException("Failed to write result batch", ex);
				}					
			}
		})
		.when(Throwable.class, new Consumer<Throwable>(){
			@Override
			public void accept(Throwable t) {
				q.setExhausted(true);
				final JSONResponse response = request.response();					
				try {
					response.resetChannelOutputStream();
					response.setOpCode("error");					
					final JsonGenerator jgen = response.writeHeader(true);							
					String message = t.getMessage();
					if(message==null || message.trim().isEmpty()) {
						message = t.getClass().getSimpleName();
					}
					jgen.writeObjectField("error", message);
					jgen.writeObjectField("q", q);
					response.closeGenerator();
					log.warn("Exception message dispatched");
				} catch (Exception ex) {
					throw new RuntimeException("Failed to write timeout response to JSON output streamer", ex);
				}												
			}
		});
//		.timeout(q.getTimeout());		
	}
	

	
	
	/**
	 * HTTP and WebSocket exposed interface to {@link MetricsMetaAPI#getTagValues(QueryContext, String, Map, String)}
	 * returning <a href="http://d3js.org"></a> friendly formatted JSON.
	 * @param request The JSON request
	 * @param q The query context
	 * @param expression The TSMeta fully qualified metric name pattern to match. An expression is basically an {@link javax.management.ObjectName} analog where
	 * the {@link javax.management.ObjectName#getDomain()} value is the metric name and the {@link javax.management.ObjectName#getKeyPropertyList()}
	 * map represents the tags. Supports <b><code>*</code></b> wildcards for all segments and <b><code>|</code></b> multipliers for tag keys. 
	 * <p>Sample request:<pre>
			{
			  "t": "req",
			  "rid": 14,
			  "svc": "meta",
			  "op": "tsMetaEval",
			  "q": {
			    "nextIndex": null,
			    "pageSize": 100,
			    "maxSize": 2000,
			    "timeout": 500,
			    "continuous": false,
			    "format": "D3",
			    "exhausted": false,
			    "cummulative": 0,
			    "elapsed": -1,
			    "expired": false
			  },
			  "x": "sys*:dc=dc*,host=WebServer1|WebServer5"
			}
	 * </pre></p>
	 */
	@JSONRequestHandler(name="d3tsmeta", description="Returns the d3 json graph for the TSMetas that match the passed expression")
	public void evaluateD3JSON(final JSONRequest request, final QueryContext q, final String expression) {
		if(q==null) {
			evaluateD3JSON(
				request,
				JSON.parseToObject(request.getRequest().get("q").toString(), QueryContext.class),
				request.getRequest().get("x").textValue()
			);
		} else {
			attachD3Handlers(metricApi.evaluate(q, expression), q, request);
		}
	}
	
	/**
	 * HTTP and WebSocket exposed interface to {@link MetricsMetaAPI#getTSMetas(net.opentsdb.meta.api.QueryContext, java.lang.String, java.util.Map)}
	 * @param request The JSON request
	 * @param q The query context
	 * @param metricName The TSMeta metric name
	 * @param tags The TSMeta metric tags
	 * <p>Sample request:<pre>
			{
			  "t": "req",
			  "rid": 1,
			  "svc": "meta",
			  "op": "tsmetas",
			  "q": {
			    "nextIndex": null,
			    "pageSize": 100,
			    "maxSize": 2000,
			    "timeout": 500,
			    "continuous": false,
			    "format": "DEFAULT",
			    "exhausted": false,
			    "cummulative": 0,
			    "elapsed": -1,
			    "expired": false
			  },
			  "m" : "sys*",
			  "tags": {"dc" : "dc*", "host" : "WebServer1|WebServer5"}
			}
	 * </pre></p>
	 */
	@JSONRequestHandler(name="tsmetas", description="Returns the MetricNames that match the passed tag pairs")
	public void getTSMetasJSON(final JSONRequest request, final QueryContext q, final String metricName, final Map<String, String> tags) {
		if(q==null) {
			getTSMetasJSON(
				request, 
				JSON.parseToObject(request.getRequest().get("q").toString(), QueryContext.class).addCtx(ctxName, System.currentTimeMillis()),
				request.getRequest().get("m").textValue(),
				getMap(request, "tags")
			);
		} else {
//			request.response().setOverrideObjectMapper(TSDBTypeSerializer.valueOf(q.getFormat()).getMapper());
//			metricApi.getTSMetas(q, metricName, tags).consume(new ResultConsumer<List<TSMeta>>(request, q));
			attachBatchHandlers(metricApi.getTSMetas(q, metricName, tags), q, request);
		}
	}
	
	
	/**
	 * HTTP and WebSocket exposed interface to {@link MetricsMetaAPI#find(QueryContext, UniqueIdType, String)}
	 * @param request The JSON request
	 * @param q The query context
	 * @param type The UID type as enumerated in {@link UniqueIdType}
	 * @param name The UID name pattern to match. Supports <b><code>*</code></b> wildcards for all segments and <b><code>|</code></b> multipliers for tag keys.
	 * <p>Sample request:<pre>
			{
			  "t": "req",
			  "rid": 5,
			  "svc": "meta",
			  "op": "finduid",
			  "q": {
			    "nextIndex": null,
			    "pageSize": 100,
			    "maxSize": 2000,
			    "timeout": 500,
			    "continuous": false,
			    "format": "DEFAULT",
			    "exhausted": false,
			    "cummulative": 0,
			    "elapsed": -1,
			    "expired": false
			  },
			  "name": "sys*",
			  "type": "METRIC"
			} 
	 * </pre></p>
	 */
	@JSONRequestHandler(name="finduid", description="Returns all UIDMetas of the specified type that match the passed name")
	public void findJSON(final JSONRequest request, final QueryContext q, final UniqueIdType type, final String name) {
		if(q==null) {
			findJSON(
				request,
				JSON.parseToObject(request.getRequest().get("q").toString(), QueryContext.class).addCtx(ctxName, System.currentTimeMillis()),
				UniqueIdType.valueOf(request.getRequest().get("type").textValue().trim().toUpperCase()),
				request.getRequest().get("name").textValue()
			);
		} else {			
			attachBatchHandlers(metricApi.find(q.startExpiry(), type, name), q, request);			
		}
	}
	
//	/**
//	 * @param request
//	 * @param q
//	 * @param expression
//	 * @param range
//	 */
//	@JSONRequestHandler(name="annotations", description="Returns all Annotations associated to TSMetas defined in the expression")
//	public void jsonGetAnnotations(final JSONRequest request, final QueryContext q, final String expression, final long... range) {
//		if(q==null) {
//			jsonGetAnnotations(
//					request,
//					JSON.parseToObject(request.getRequest().get("q").toString(), QueryContext.class).addCtx(ctxName, System.currentTimeMillis()),
//					request.getRequest().has("x") ? request.getRequest().get("x").asText() : null,
//							getLongArray(request, "r")
//					);
//		} else {
//			request.response().setOverrideObjectMapper(TSDBTypeSerializer.valueOf(q.getFormat()).getMapper());
//			final Deferred<Set<Annotation>> def;
//			if(expression==null) {
//				def = metricApi.getGlobalAnnotations(q, range);
//			} else {
//				def = metricApi.getAnnotations(q, expression, range);
//			}
////			def.addCallback(new ResultCompleteCallback<Set<Annotation>>(request, q))
////			.addCallback(new Callback<Void, QueryContext>() {
////				@Override
////				public Void call(QueryContext ctx) throws Exception {
////					if(ctx.shouldContinue()) {
////						jsonGetAnnotations(request, ctx, expression, range);
////					}
////					return null;
////				}
////			});	
//		}
//	}

	
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
	
	

}



/*
protected final <T> void attachHandlers(Stream<T> stream, final QueryContext q, final JSONRequest request) {
	stream.consume(new Consumer<T>(){
		boolean firstRow = true;
		JSONResponse response = request.response();			
		JsonGenerator jgen = null;
		final AtomicLong msgId = new AtomicLong();
		@Override
		public synchronized void accept(T t) {
			try {
				if(t==null) {
					if(firstRow) {
						// empty results
						jgen.writeObjectField("results", Collections.emptyList());
					} else {
						jgen.writeEndArray();
					}
					jgen.writeObjectField("q", q);	
					jgen.writeNumberField("msgid", msgId.incrementAndGet());
					response = response.closeGenerator();
					firstRow = true;
					return;
				}
				if(firstRow) {
					jgen = response.writeHeader(true);
					response.setOpCode("results");
					jgen.setCodec(q.getMapper());
					jgen.writeFieldName("results");
					jgen.writeStartArray();
					firstRow = false;
				}																
				jgen.writeObject(t);	
			}  catch (Exception ex) {
				log.error("Failed to write result instance. FR: {}, t: {}, jg: {}", firstRow, t, jgen, ex);
				throw new RuntimeException("Failed to write result instance", ex);
			}					
		}
	})
	.when(Throwable.class, new Consumer<Throwable>(){
		@Override
		public void accept(Throwable t) {
			q.setExhausted(true);
			final JSONResponse response = request.response();					
			try {
				response.resetChannelOutputStream();
				response.setOpCode("error");
				@SuppressWarnings("resource")
				final JsonGenerator jgen = response.writeHeader(true);							
				String message = t.getMessage();
				if(message==null || message.trim().isEmpty()) {
					message = t.getClass().getSimpleName();
				}
				jgen.writeObjectField("error", message);
				jgen.writeObjectField("q", q);
				response.closeGenerator();
				log.warn("Exception message dispatched");
			} catch (Exception ex) {
				throw new RuntimeException("Failed to write timeout response to JSON output streamer", ex);
			}												
		}
	});
//	.timeout(q.getTimeout());		
}
*/