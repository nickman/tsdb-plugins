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
package net.opentsdb.tsd;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import net.opentsdb.core.TSDB;

import org.helios.tsdb.plugins.groovy.GroovyService;
import org.helios.tsdb.plugins.remoting.json.JSONRequest;
import org.helios.tsdb.plugins.remoting.json.JSONResponse;
import org.helios.tsdb.plugins.remoting.json.annotations.JSONRequestHandler;
import org.helios.tsdb.plugins.remoting.json.annotations.JSONRequestService;
import org.helios.tsdb.plugins.remoting.json.services.InvocationChannel;
import org.helios.tsdb.plugins.service.IPluginContextResourceListener;
import org.helios.tsdb.plugins.service.PluginContext;
import org.helios.tsdb.plugins.service.TSDBPluginServiceLoader;
import org.helios.tsdb.plugins.util.SystemClock;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferInputStream;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.handler.codec.base64.Base64;
import org.jboss.netty.handler.codec.http.DefaultHttpRequest;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.stumbleupon.async.Callback;

/**
 * <p>Title: TSDBJSONService</p>
 * <p>Description: Websocket RPC service adapter for OpenTSDB's HTTP API</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>net.opentsdb.tsd.TSDBJSONService</code></p>
 */
@JSONRequestService(name="tsdb", description="TSDB JSON Remoting Services")
public class TSDBJSONService implements IPluginContextResourceListener {
	/** The json node factory */
	private final JsonNodeFactory nodeFactory = JsonNodeFactory.instance; 
	/** The jackson object mapper */
	private static final ObjectMapper jsonMapper = new ObjectMapper();
	
	/** Instance logger */
	protected final Logger log = LoggerFactory.getLogger(getClass());
	/** The tsdb instance */
	protected final TSDB tsdb;
	/** The plugin context */
	protected final PluginContext pluginContext;
	/** A map of http rpcs which we can piggy-back on */
	protected final Map<String, HttpRpc> http_commands = new HashMap<String, HttpRpc>(11);
	/** The groovy service instance */
	protected GroovyService groovyService = null;
	/**
	 * Creates a new TSDBJSONService
	 */
	public TSDBJSONService() {
		
		tsdb = TSDBPluginServiceLoader.getLoaderInstance().getTSDB();
		pluginContext = TSDBPluginServiceLoader.getLoaderInstance().getPluginContext();		
		pluginContext.addResourceListener(this);
		RpcHandler rpcHandler = RpcHandler.getInstance(tsdb);
//		loadInstanceOf("net.opentsdb.tsd.RpcHandler$DieDieDie", "diediedie", rpcHandler);
//		final StaticFileRpc staticfile = new StaticFileRpc();
//	    http_commands.put("favicon.ico", staticfile);
//	    http_commands.put("s", staticfile);
//	    final StatsRpc stats = new StatsRpc();
//	    http_commands.put("api/stats", stats);
//	    loadInstanceOf("net.opentsdb.tsd.RpcHandler$Version", "api/version", null);
//	    loadInstanceOf("net.opentsdb.tsd.RpcHandler$DropCaches", "api/dropcaches", null);
//        final PutDataPointRpc put = new PutDataPointRpc();	    
//	    http_commands.put("api/put", put);
//	    loadInstanceOf("net.opentsdb.tsd.RpcHandler$ListAggregators", "api/aggregators", null);
//	    http_commands.put("logs", new LogsRpc());
//	    http_commands.put("q", new GraphHandler());
//	    final SuggestRpc suggest_rpc = new SuggestRpc();
//	    http_commands.put("api/suggest", suggest_rpc);
//	    http_commands.put("api/uid", new UniqueIdRpc());
//	    http_commands.put("api/query", new QueryRpc());
//	    http_commands.put("api/tree", new TreeRpc());
//	    http_commands.put("api/annotation", new AnnotationRpc());
//	    http_commands.put("api/search", new SearchRpc());
//	    loadInstanceOf("net.opentsdb.tsd.RpcHandler$Serializers", "api/serializers", null);
//	    loadInstanceOf("net.opentsdb.tsd.RpcHandler$ShowConfig", "api/config", null);
	    pluginContext.setResource(getClass().getSimpleName(), this);
	}
	
	/**
	 * Handles a prepared WebSocket API invocation to retrieve a static file
	 * @param request The prepared HTTP request so we can piggy-back on the existing RpcHandler services.
	 * @param response The JSONResponse to write back to
	 * @throws IOException thrown on IO errors
	 */
	protected void invokeForFile(HttpRequest request, JSONResponse response) throws IOException {
		JsonGenerator generator = response.writeHeader(true);		
		InvocationChannel ichannel = new InvocationChannel();		
		HttpQuery query = new HttpQuery(tsdb, request, ichannel);
		String baseRoute = query.getQueryBaseRoute();
		http_commands.get(baseRoute).execute(tsdb, query);
		HttpResponse resp = (HttpResponse)ichannel.getWrites().get(0);
		byte[] regionBytes = (byte[])ichannel.getWrites().get(1);
		
		List<Map.Entry<String, String>> responseHeaders = resp.getHeaders();
		for(Map.Entry<String, String> entry: responseHeaders) {
			generator.writeStringField(entry.getKey(), entry.getValue());
			//log.info("Reponse Header: [{}] : [{}]", entry.getKey(), entry.getValue());
		}
		
		String cType = resp.getHeader("Content-Type");
		if(cType!=null) {
			if(cType.startsWith("text")) {
				generator.writeStringField("content", new String(regionBytes));
			} else {
				ChannelBuffer b64 = Base64.encode(ChannelBuffers.wrappedBuffer(regionBytes));
				byte[] bytes = new byte[b64.readableBytes()];
				b64.readBytes(bytes);
				generator.writeBinaryField("content", bytes);
			}
		}
		
		response.closeGenerator();
	}
	
	/**
	 * Handles a prepared WebSocket API invocation
	 * @param asMap true if the JSON reponse is a map, false if it is an array
	 * @param request The prepared HTTP request so we can piggy-back on the existing RpcHandler services.
	 * @param response The JSONResponse to write back to
	 * @throws IOException thrown on IO errors
	 */
	protected void invoke(boolean asMap, HttpRequest request, JSONResponse response) throws IOException {
		try {
			@SuppressWarnings("resource")
			JsonGenerator generator = response.writeHeader(asMap);
			InvocationChannel ichannel = new InvocationChannel();
			HttpQuery query = new HttpQuery(tsdb, request, ichannel);
			String baseRoute = query.getQueryBaseRoute();
			http_commands.get(baseRoute).execute(tsdb, query);
			HttpResponse resp = (HttpResponse)ichannel.getWrites().get(0);			
			ChannelBuffer content = resp.getContent();
			ChannelBufferInputStream cbis =  new ChannelBufferInputStream(content);
			ObjectReader reader = jsonMapper.reader();
			JsonNode contentNode = reader.readTree(cbis);
			cbis.close();
			if(asMap) {
				ObjectNode on = (ObjectNode)contentNode;
				Iterator<Map.Entry<String, JsonNode>> nodeIter = on.fields();
				while(nodeIter.hasNext()) {
					Map.Entry<String, JsonNode> node = nodeIter.next();
					generator.writeObjectField(node.getKey(), node.getValue());
				}
			} else {
				ArrayNode an = (ArrayNode)contentNode;
				for(int i = 0; i < an.size(); i++) {
					generator.writeObject(an.get(i));
				}			
			}
			response.closeGenerator();
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
		
	}
	
	/**
	 * WebSocket invoker for shutting down the OpenTSDB HTTP
	 * @param request The JSONRequest
	 */
	@JSONRequestHandler(name="die", description="Shuts down the TSDB")
	public void die(final JSONRequest request) {
		try {
			Thread t = new Thread("DieDieDieThread") {
				public void run() {
					tsdb.shutdown();
				}
			};
			t.setDaemon(false);
			request.response().setContent("ok").send();
			t.start();			
		} catch (Exception ex) {
			log.error("Failed to invoke die", ex);
			request.error("Failed to invoke die", ex).send();
		}
	}
	
	/**
	 * WebSocket invoker for OpenTSDB HTTP <a href="http://opentsdb.net/docs/build/html/api_http/stats.html">api/stats</a> API call
	 * @param request The JSONRequest
	 */
	@JSONRequestHandler(name="stats", description="Collects TSDB wide stats and returns them in JSON format to the caller")
	public void stats(JSONRequest request) {
		try {
			HttpRequest httpRequest = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/api/stats?json=true");
			invoke(false, httpRequest, request.response());
		} catch (Exception ex) {
			log.error("Failed to invoke stats", ex);
			request.error("Failed to invoke stats", ex).send();
		}
	}
	
	/**
	 * WebSocket invoker for OpenTSDB HTTP <a href="http://opentsdb.net/docs/build/html/api_http/s.html">/s</a> static file retrieval API call
	 * @param request The JSONRequest
	 */
	@JSONRequestHandler(name="s", description="Retrieves a static file")
	public void file(JSONRequest request) {
		try {
			String fileName = request.getArgument("s").replace("\"", "");
			HttpRequest httpRequest = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, ("/s/" + fileName));
			invokeForFile(httpRequest, request.response());
		} catch (Exception ex) {
			log.error("Failed to invoke s/file", ex);
			request.error("Failed to invoke s/file", ex).send();
		}
	}
	
	/**
	 * WebSocket invoker for OpenTSDB HTTP <a href="http://opentsdb.net/docs/build/html/api_http/version.html">/s</a> OpenTSDB version info API call
	 * @param request The JSONRequest
	 */
	@JSONRequestHandler(name="dropcaches", description="Drops all OpenTSDB server caches")
	public void dropcaches(JSONRequest request) {
		try {
			HttpRequest httpRequest = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/api/dropcaches?json=true");
			invoke(true, httpRequest, request.response());
		} catch (Exception ex) {
			log.error("Failed to invoke dropcaches", ex);
			request.error("Failed to invoke dropcaches", ex).send();
		}
	}
	
	/**
	 * WebSocket invoker for OpenTSDB HTTP <a href="http://opentsdb.net/docs/build/html/api_http/logs.html">/s</a> logs API call
	 * @param request The JSONRequest
	 */
	@JSONRequestHandler(name="logs", description="Returns JSONized OpenTSDB log file entries")
	public void logs(JSONRequest request) {
		try {
			HttpRequest httpRequest = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/logs?json=true");
			invoke(true, httpRequest, request.response());
		} catch (Exception ex) {
			log.error("Failed to invoke logs", ex);
			request.error("Failed to invoke logs", ex).send();
		}
	}
	
	
	/**
	 * WebSocket invoker for OpenTSDB HTTP <a href="http://opentsdb.net/docs/build/html/api_http/dropcaches.html">/s</a> drop caches API call
	 * @param request The JSONRequest
	 */
	@JSONRequestHandler(name="version", description="Retrieves the OpenTSDB version info")
	public void version(JSONRequest request) {
		try {
			HttpRequest httpRequest = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/api/version?json=true");
			invoke(true, httpRequest, request.response());
		} catch (Exception ex) {
			log.error("Failed to invoke version", ex);
			request.error("Failed to invoke version", ex).send();
		}
	}
	
	/**
	 * WebSocket invoker for OpenTSDB HTTP <a href="http://opentsdb.net/docs/build/html/api_http/dropcaches.html">/s</a> aggregators API call
	 * @param request The JSONRequest
	 */
	@JSONRequestHandler(name="aggregators", description="Retrieves the OpenTSDB available aggregator names")
	public void aggregators(JSONRequest request) {
		try {
			HttpRequest httpRequest = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/api/aggregators?json=true");
			invoke(false, httpRequest, request.response());
		} catch (Exception ex) {
			log.error("Failed to invoke aggregators", ex);
			request.error("Failed to invoke aggregators", ex).send();
		}
	}
	
	/**
	 * WebSocket invoker for OpenTSDB HTTP <a href="http://opentsdb.net/docs/build/html/api_http/config.html">/s</a> config API call
	 * @param request The JSONRequest
	 */
	@JSONRequestHandler(name="config", description="Retrieves the OpenTSDB configuration")
	public void config(JSONRequest request) {
		try {
			HttpRequest httpRequest = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/api/config?json=true");
			invoke(true, httpRequest, request.response());
		} catch (Exception ex) {
			log.error("Failed to invoke config", ex);
			request.error("Failed to invoke config", ex).send();
		}
	}
	
	/** The recognized numeric type codes (F=float, L=long, D=double) in uppercase  */
	public static final Set<String> ALLOWED_N_TYPES = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList("F", "L", "D")));
	
	/**
	 * Writes out the system properties as JSON to the caller
	 * @param request the request
	 * <p>This is a simple example of a JavaScript invocation of this service over websockets:
	 * <pre>
	 * var c = '{"t":"req", "rid":1, "svc":"tsdb", "op":"points", "points":[{"m":"sys.cpu", "v":"32", "tags": {"host":"webserver1"}}, {"m":"sys.cpu", "v":"19","tags": {"host":"webserver2"}}]}';
	 * ws.send(c);
	 * </pre></p>
	 * <p>The above example uses default timestamping meaning that the timestamp submitted to the TSDB is taken as
	 * the current time at the point that the request is received and then used for all submitted points.</p>.
	 * <p>Alternatively, the timestamp can be specified as the request level and then used for each submitted point:
	 * <pre>
	 * var b = '{"t":"req", "rid":1, "svc":"tsdb", "op":"points", "ts":"' + Math.round(new Date().getTime()/1000) + '", "points":[{"m":"sys.cpu", "v":"32", "tags": {"host":"webserver1"}}, {"m":"sys.cpu", "v":"19","tags": {"host":"webserver2"}}]}';
	 * ws.send(b);
	 * </pre></p>
	 * <p>Lastly, the timestamp can be specified for each point:
	 * <pre>
	 * var a = '{"t":"req", "rid":1, "svc":"tsdb", "op":"points", "points":[{"m":"sys.cpu", "ts":"' + Math.round(new Date().getTime()/1000) + '", "v":"32", "tags": {"host":"webserver1"}}, {"m":"sys.cpu", "ts":"' + Math.round(new Date().getTime()/1000) + '","v":"19","tags": {"host":"webserver2"}}]}';
	 * ws.send(a);
	 * </pre></p>
	 * <p>If a point timestamp is specified, it will override the request specified timestamp. If neither a point or a request timestamp
	 * is specified, the default timestamp will be used. The submitted points can interlace specified and non-specified timestamps.</p>
	 * <p>The numeric type of the point value (keyed with the json key <b><code>"v"</code></b>) will be interpreted as a <b><code>long</code></b>
	 * by default. This can be overriden by specifiying a numeric type with the json key <b><code>"nt"</code></b> specified at the point level. 
	 * The numeric types recognized are specified in {@link #ALLOWED_N_TYPES}.</p>.
	 * <p>A completion message will be returned to the caller for each submitted point as the TSDB callback is received. This can be replaced
	 * with a single callback indicating to the caller that the request was received 
	 * using a no-confirm at the request level as follows: <b><code>"noc":true</code></b>.</p>
	 * <p>The responses sent back to the caller with the above examples would appear as follows: (leading timestamp and "received:" generated by <a href="https://chrome.google.com/webstore/detail/old-websocket-terminal/cpopfplgicdljhakjpdochbbiodlgaoc?hl=en">Old WebSocketTerminal</a><ul>
	 * 		<li><b>Without a noc or noc=false</b>
	 * 		<pre>
	 * 			09:31:26	received:	{"id":189215650,"rerid":1,"t":"resp","msg":"sys.cpu:{host=webserver1}[L]t(Thu Dec 05 09:31:26 EST 2013)","op":"ok"}
	 * 			09:31:26	received:	{"id":276912657,"rerid":1,"t":"resp","msg":"sys.cpu:{host=webserver2}[L]t(Thu Dec 05 09:31:26 EST 2013)","op":"ok"} 
	 * 		</pre>
	 * 		</li>
	 * 		<li><b>With noc=true</b>
	 * 		<pre>
	 * 			09:33:44	received:	{"id":599311176,"rerid":1,"t":"resp","msg":{"points":2},"op":"ok"}
	 * 		</pre>
	 * 		</li>
	 * </ul></p>
	 * TODO: Handle errors and return error messages to caller.
	 * TODO: Implement hierarchical json tree of points for a smaller and more normalized payload
	 */
	
	// var a = '{"t":"req", "rid":1, "svc":"tsdb", "op":"points", 
	//  "points":[{"m":"sys.cpu", "ts":"' + Math.round(new Date().getTime()/1000) + '", "v":"32", "tags": {"host":"webserver1"}}, {"m":"sys.cpu", "ts":"' + Math.round(new Date().getTime()/1000) + '","v":"19","tags": {"host":"webserver2"}}]}';
	
	@JSONRequestHandler(name="points", description="Submits an array of datapoints to the TSDB")
	public void addPoint(final JSONRequest request) {
		if(request==null) throw new IllegalArgumentException("The passed request was null");
		final long defaultTimestamp = SystemClock.unixTime();
		try {
			long requestTimestamp = defaultTimestamp;
			if(request.getRequest().get("ts")!=null) {
				JsonNode tNode = request.getRequest().get("ts");
				if(tNode.isLong()) {
					requestTimestamp = tNode.asLong();
				}
			}
			final boolean sendConfirm;
			if(request.getRequest().get("noc")!=null) {
				
				JsonNode tNode = request.getRequest().get("noc");
				if(tNode.isBoolean()) {
					sendConfirm = !tNode.asBoolean();
				} else {
					sendConfirm = true;
				}
			} else {
				sendConfirm = true;
			}
			ArrayNode pointsArr = (ArrayNode)request.getRequest().get("points");
			int pointsProcessed = 0;
			for(int i = 0; i < pointsArr.size(); i++) {
				ObjectNode point = (ObjectNode)pointsArr.get(i);
				String metric = point.get("m").asText();
				long pointTimestamp = requestTimestamp;
				JsonNode tNode = point.get("ts");
				if(tNode!=null && tNode.isLong()) {
					pointTimestamp = tNode.asLong();
				}
				ObjectNode tagNode = (ObjectNode)point.get("tags");
				Map<String, String> tags = new TreeMap<String, String>();
				Iterator<String> titer = tagNode.fieldNames();
				while(titer.hasNext()) {
					String key = titer.next();
					tags.put(key, tagNode.get(key).asText());
				}
				String numericType = null;
				JsonNode numericTypeNode = point.get("nt");
				if(numericTypeNode==null) {
					numericType = "L";
				} else {
					numericType = numericTypeNode.asText().trim().toUpperCase();
					if(!ALLOWED_N_TYPES.contains(numericType)) {
						throw new Exception("Unrecognized numeric type code [" + numericType + "]");
					}
				}
				final String metricName = String.format("%s:%s[%s]t(%s)", metric, tags.toString(), numericType, new Date(pointTimestamp*1000));
				final Callback<Object, Object> completionCallback = new Callback<Object, Object>() {
					@Override
					public Object call(Object arg) throws Exception {							
						if(sendConfirm) {
							request.response().setContent(metricName).send();
						}
						return null;
					}					
				};
				if("L".equals(numericType)) {
					tsdb.addPoint(metric, pointTimestamp, point.get("v").asLong(), tags).addCallback(completionCallback);
					pointsProcessed++;
				} else if("F".equals(numericType)) {
					float f = (float)point.get("v").asDouble();
					tsdb.addPoint(metric, pointTimestamp, f, tags).addCallback(completionCallback);
					pointsProcessed++;
				} else if("D".equals(numericType)) {
					tsdb.addPoint(metric, pointTimestamp, point.get("v").asDouble(), tags).addCallback(completionCallback);
					pointsProcessed++;
				}
				log.debug("Submitted meric [{}]", metricName);
			}
			if(!sendConfirm) {
				request.response().setContent(nodeFactory.objectNode().set("points", nodeFactory.numberNode(pointsProcessed))).send();
			}
		} catch (Exception ex) {
			log.error("Failed to add points", ex);
			request.error("Failed to add points", ex).send();
		}
	}
	
	/**
	 * WebSocket invoker for OpenTSDB HTTP <a href="http://opentsdb.net/docs/build/html/api_http/query.html">api/query</a> API call
	 * @param request The JSONRequest
	 * <p>Example:<pre>
	 * 	var q = '{"t":"req", "rid":1, "svc":"tsdb", "op":"query", "args":{"start":"5m-ago", "m": [{"a":"avg","met":"jvm.ramfree"}]}}';
	 * 	ws.send(q);
	 * </pre></p>
	 */
	@JSONRequestHandler(name="query", description="Collects TSDB wide stats and returns them in JSON format to the caller")
	public void query(JSONRequest request) {
		try {
			
			request.allowDefaults(false);
			// Required Fields
			String startTime = request.get("start", "");
			
			
			request.allowDefaults(true);
			// Psuedo Required Fields
			ArrayNode mqueries = request.getArray("m");
			ArrayNode tqueries = request.getArray("tsuids");
			if(mqueries==null && tqueries==null) {
				throw new Exception("No parameter provided for 'm' or 'tsuids'");
			}
			ArrayNode queries = mqueries==null ? tqueries : mqueries;
			String flatQueries = flattenQueries(tqueries!=null, queries);
			
			
			// Optional Fields
			String endTime = request.get("end",  "" + SystemClock.unixTime());
			boolean noAnnotations = request.get("no_annotations", true);
			
			boolean globalAnnotations = request.get("global_annotations", false);
			boolean msResolution = request.get("ms", false);
			boolean showTSUIDs = request.get("show_tsuids", false);
			
			
			StringBuilder uri = new StringBuilder("json=true");
			uri.append("&start=").append(startTime);
			uri.append(flatQueries);
			uri.append("&no_annotations=").append(noAnnotations);
			uri.append("&end=").append(endTime);
			uri.append("&no_annotations=").append(globalAnnotations);
			uri.append("&ms=").append(msResolution);
			uri.append("&show_tsuids=").append(showTSUIDs);
			
			//HttpRequest httpRequest = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/api/query?" + URLEncoder.encode(uri.toString(), "UTF8"));
			HttpRequest httpRequest = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/api/query?" + uri.toString());
			invoke(false, httpRequest, request.response());
		} catch (Exception ex) {
			log.error("Failed to invoke query", ex);
			request.error("Failed to invoke query", ex).send();
		}
	}
	
	
	/**
	 * Flattens a json query node into a string matching the subquery format
	 * @param tsuid true if a tsuid based subquery, false if metric based
	 * @param qNodes The array of nodes to format
	 * @return a sub query string
	 */
	public String flattenQueries(boolean tsuid, ArrayNode qNodes) {
		if(qNodes==null) return "";
		StringBuilder b = new StringBuilder();
		for(JsonNode qNode: qNodes) {
			b.append("&m=");
			ObjectNode on = (ObjectNode)qNode;
			b.append(on.get("a").asText()).append(":");
			ObjectNode rateOptions = (ObjectNode)on.get("rate");
			if(rateOptions!=null) {
				JsonNode ctrNode = rateOptions.get("counter");
				JsonNode ctrMaxNode = rateOptions.get("counterMax");
				JsonNode ctrResetNode = rateOptions.get("resetValue");
				if(ctrNode!=null) {
					b.append("rate");
					if(ctrNode!=null) {
						b.append("counter");
					}
					if(ctrMaxNode!=null) {
						b.append(",").append(ctrMaxNode.asLong());
					}
					if(ctrResetNode!=null) {
						b.append(",").append(ctrResetNode.asLong());
					}
					b.append(":");
				}
			}
			
			if(on.has("ds")) {
				b.append(on.get("ds").asText()).append(":");  //opt
			}
			if(on.has("tags")) {
				b.append(on.get("tags").asText()).append(":"); //opt
			}
			if(!on.has("met")) {
				throw new RuntimeException("No metric specified. (key is \"met\")");
			}
			b.append(on.get("met").asText()); //req
			if(on.has("tags")) {
				ObjectNode tagsNode = (ObjectNode)on.get("tags");
				if(tagsNode.size()>0) {
					b.append("{");
					for(Iterator<String> is = tagsNode.fieldNames(); is.hasNext();) {
						String v = is.next();
						String k = tagsNode.get(v).asText();
						b.append(v).append("=").append(k).append(",");
					}
					b.deleteCharAt(b.length()-1);					
				}
			}
		}
		log.info("Query String [{}]", b.toString());
		return b.toString();
	}
	
	/**
	 * Executes the named groovy script passed in the JSONRequest.
	 * @param request The JSONRequest
	 * <p>Example:<pre>
	 * 	var q = '{"t":"req", "rid":1, "svc":"tsdb", "op":"groovy", "args":{"name":"printenv", "groovy" : "return System.getenv(args[0].toString());", "e" : "HOSTNAME"}}'
	 * 	ws.send(q);
	 * </pre></p>
	 */
	@JSONRequestHandler(name="groovy", description="Executes the named groovy script passed in the JSONRequest")
	public void groovy(JSONRequest request) {
		if(groovyService==null) {
			request.error("GroovyService not enabled").send();
			return;
		}
		try {
			request.allowDefaults(false);
			String name = request.get("name", "");
			String script = request.get("groovy", "");
			if(name==null) throw new Exception("Script name [name] was not provided");
			if(script==null) throw new Exception("Script source [groovy] was not provided");
			request.removeFields("name", "groovy", "__");
			groovyService.compile(name, script);
			Object[] gargs = request.asStringArray();
			Object response = groovyService.invokeScript(name, gargs);
			request.response().setContent(jsonMapper.writeValueAsString(response)).send();			
		} catch (Exception ex) {
			log.error("Failed to invoke groovy", ex);
			request.error("Failed to invoke groovy", ex).send();
		} finally {
			request.allowDefaults(true);
		}
	}
	
	
	/**
	 * Extracts the named value from the passed node
	 * @param node The node to extract from
	 * @param name The name of the value to extract
	 * @param defaultValue The default value 
	 * @param required true if required
	 * @return the value
	 */
	protected Object arg(Map<Object, Object> node, String name, Object defaultValue, boolean required) {
		Object v = node.get(name);
		if(v==null) {
			if(!required) return defaultValue;
			else throw new RuntimeException("Request was missing required attribute [" + name + "]");
		}
		return v;
	}
	
	protected JsonNode jarg(Map<Object, Object> node, String name, Object defaultValue, boolean required) {
		Object v = node.get(name);
		if(v==null) {
			if(defaultValue==null) return nodeFactory.nullNode();
			if(!required) return nodeFactory.POJONode(defaultValue);
			else throw new RuntimeException("Request was missing required attribute [" + name + "]");
		}
		if(v instanceof JsonNode) {
			return (JsonNode)v;
		}
		throw new RuntimeException("Item named [" + name + "] was of type [" + v.getClass().getName() + "] but was expected to be a JsonNode");
	}
	
	
	
	/**
	 * Loads and indexes an instance of the named HttpRpc class
	 * @param className HttpRpc class name
	 * @param key The key that the instance should be indexed by
	 */
	protected void loadInstanceOf(String className, String key, RpcHandler rpcHandler) {
		try {
			Class<? extends HttpRpc> rpcClazz = (Class<? extends HttpRpc>) Class.forName(className, true, TSDB.class.getClassLoader());

			Constructor<? extends HttpRpc> ctor = rpcHandler==null ? rpcClazz.getDeclaredConstructor() : rpcClazz.getDeclaredConstructor(rpcHandler.getClass());
			ctor.setAccessible(true);
			HttpRpc httpRpc = rpcHandler==null ? ctor.newInstance() : ctor.newInstance(rpcHandler);
			http_commands.put(key, httpRpc);
		} catch (Exception ex) {
			log.warn("Failed to load HttpRpc instance for [{}]", className, ex);
		}
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.service.IPluginContextResourceListener#onResourceRegistered(java.lang.String, java.lang.Object)
	 */
	@Override
	public void onResourceRegistered(String name, Object resource) {
		if(resource instanceof GroovyService) {
			groovyService = (GroovyService)resource;
			log.info("Set GroovyService");
		}
		
	}

}
