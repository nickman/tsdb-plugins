/**
 * 
 */
package org.helios.tsdb.plugins.remoting.json.services;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import net.opentsdb.core.TSDB;
import net.opentsdb.stats.StatsCollector;
import net.opentsdb.tsd.StatsRpc;

import org.helios.tsdb.plugins.remoting.json.JSONRequest;
import org.helios.tsdb.plugins.remoting.json.JSONResponse;
import org.helios.tsdb.plugins.remoting.json.annotations.JSONRequestHandler;
import org.helios.tsdb.plugins.remoting.json.annotations.JSONRequestService;
import org.helios.tsdb.plugins.service.PluginContext;
import org.helios.tsdb.plugins.service.TSDBPluginServiceLoader;
import org.helios.tsdb.plugins.util.SystemClock;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.handler.codec.http.DefaultHttpRequest;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.stumbleupon.async.Callback;

/**
 * <p>Title: TSDBJSONService</p>
 * <p>Description:  JSON service to expose TSDB based services.</p>
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><b><code>org.helios.tsdb.plugins.remoting.json.services.TSDBJSONService</code></b>
 */
@JSONRequestService(name="tsdb", description="TSDB JSON Remoting Services")
public class TSDBJSONService {
	/** The json node factory */
	private final JsonNodeFactory nodeFactory = JsonNodeFactory.instance; 
	/** Instance logger */
	protected final Logger log = LoggerFactory.getLogger(getClass());
	/** The tsdb instance */
	protected final TSDB tsdb;
	/** The plugin context */
	protected final PluginContext pluginContext;
	/** A map of http rpcs which we can piggy-back on */
	protected final Map<String, Object> http_commands = new HashMap<String, Object>(11);
	
	
	/** The connection manager collect stats method */
	protected static final Method connMgrCollectStats;
	/** The RPC Handler collect stats method */
	protected static final Method rpcMgrCollectStats;
	/** The HttpRpc exec method */
	protected static final Method httpRpcExec;
	/** The HttpQuery constructor */
	protected static final Constructor<?> httpQueryCtor;
	
	static {
		Method cmgr = null;
		Method rhand = null;
		Method hexec = null;
		Constructor<?> mctor = null; 
		
		try {
			cmgr = Class.forName("net.opentsdb.tsd.ConnectionManager", true, TSDB.class.getClassLoader()).getDeclaredMethod("collectStats", StatsCollector.class);
			cmgr.setAccessible(true);
		} catch (Exception ex) {
			LoggerFactory.getLogger(TSDBJSONService.class).error("Failed to get CollectStats method from ConnectionManager", ex);
		}
		try {			
			rhand = Class.forName("net.opentsdb.tsd.RpcHandler", true, TSDB.class.getClassLoader()).getDeclaredMethod("collectStats", StatsCollector.class);
			rhand.setAccessible(true);
		} catch (Exception ex) {
			LoggerFactory.getLogger(TSDBJSONService.class).error("Failed to get CollectStats method from RpcHandler", ex);
		}
		try {
			Class<?> httpQueryClazz = Class.forName("net.opentsdb.tsd.HttpQuery", true, TSDB.class.getClassLoader());
			mctor = httpQueryClazz.getDeclaredConstructor(TSDB.class, HttpRequest.class, Channel.class);
			mctor.setAccessible(true);
			hexec = Class.forName("net.opentsdb.tsd.HttpRpc", true, TSDB.class.getClassLoader()).getDeclaredMethod("execute", TSDB.class, httpQueryClazz);
			hexec.setAccessible(true);
		} catch (Exception ex) {
			LoggerFactory.getLogger(TSDBJSONService.class).error("Failed to get execute method from HttpQuery", ex);
		}
		httpQueryCtor = mctor;
		connMgrCollectStats = cmgr;
		rpcMgrCollectStats = rhand;
		httpRpcExec = hexec;
	}
	
	
	/**
	 * Creates a new TSDBJSONService and initializes references to the TSDB and plugin context
	 */
	public TSDBJSONService() {
		tsdb = TSDBPluginServiceLoader.getLoaderInstance().getTSDB();
		pluginContext = TSDBPluginServiceLoader.getLoaderInstance().getPluginContext();
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
	
	protected final StatsRpc statsRpc = new StatsRpc();
	
	@JSONRequestHandler(name="stats2", description="Collects TSDB wide stats and returns them in JSON format to the caller")
	public void stats2(JSONRequest request) {
		try {
			HttpRequest httpRequest = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "api/stats");
			JSONResponse response = request.response(); 
			response.writeHeader(false);
			InvocationChannel ichannel = new InvocationChannel(); 
			httpRpcExec.invoke(statsRpc, tsdb, httpQueryCtor.newInstance(tsdb, httpRequest, ichannel));
			HttpResponse resp = (HttpResponse)ichannel.getWrites().get(0);			
			ChannelBuffer content = resp.getContent();
			resp.getContent().readBytes(request.response().getChannelOutputStream(), content.readableBytes());
			response.closeGenerator();
		} catch (Exception ex) {
			log.error("Failed to invoke stats2", ex);
			request.error("Failed to invoke stats2", ex);
		}
	}
	
//	protected static final Method httpRpcExec;               -->  execute(final TSDB tsdb, final HttpQuery query)
//	protected static final Constructor<?> httpQueryCtor;	 --> HttpQuery(final TSDB tsdb, final HttpRequest request, final Channel chan) 	
	
	
	/**
	 * Collects TSDB wide stats and returns them in JSON format to the caller
	 * @param request The JSON request
	 * <p>JSON request to invoke:
	 * <pre>
	 * 		{"t":"req", "rid":1, "svc":"tsdb", "op":"stats" }
	 * </pre></p>
	 */
	@JSONRequestHandler(name="stats", description="Collects TSDB wide stats and returns them in JSON format to the caller")
	public void stats(JSONRequest request) {
		    final boolean canonical = tsdb.getConfig().getBoolean("tsd.stats.canonical");
		    
		    final JsonGenerator jsonGen = request.response().writeHeader(false);
		    final JSONCollector collector = new JSONCollector("tsd", jsonGen);
		    doCollectStats(tsdb, collector, canonical);
		    try {
				jsonGen.close();
			} catch (Exception ex) {
				throw new RuntimeException("Failed to close JSON Generator", ex);
			}
	  }
	  
	  /**
	   * Helper to record the statistics for the current TSD
	   * @param tsdb The TSDB to use for fetching stats
	   * @param collector The collector class to call for emitting stats
	   */
	  private void doCollectStats(final TSDB tsdb, final StatsCollector collector, final boolean canonical) {
		
	    collector.addHostTag(canonical);
	    try {
	    	connMgrCollectStats.invoke(null, collector);
	    } catch (Exception ex) {
	    	log.error("Failed to collect from ConnectionManager", ex);
	    }
	    try {
	    	rpcMgrCollectStats.invoke(null, collector);
	    } catch (Exception ex) {
	    	log.error("Failed to collect from RPCHandlder", ex);
	    }
	    tsdb.collectStats(collector);
	  }	  

	  /**
	 * <p>Title: JSONCollector</p>
	 * <p>Description: A stats collector implementation that writes each received stat to the calling channel in JSON format.</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.tsdb.plugins.remoting.json.services.TSDBJSONService.JSONCollector</code></p>
	 */
	class JSONCollector extends StatsCollector {
		  /** The json generator to write to */
		final JsonGenerator jsonGen;

		  /**
		   * Default constructor
		   * @param prefix The prefix to prepend to all statistics
		   * @param jsonGen The json generator to write to
		   */
		  public JSONCollector(final String prefix, JsonGenerator jsonGen) {
			  super(prefix);
			  this.jsonGen = jsonGen;
		  }
		  
		/**
		 * Splits a tag pair. Assumes a correct passed value.
		 * @param tagPair A tag ({@code name=value}) to splt
		 * @return an array containing the key and value
		 */
		private String[] splitTag(String tagPair) {
			  int index = tagPair.indexOf('=');
			  return new String[] {tagPair.substring(0, index), tagPair.substring(index+1)};
		  }
		  
		  /**
		   * Records a data point.
		   * @param name The name of the metric.
		   * @param value The current value for that metric.
		   * @param xtratag An extra tag ({@code name=value}) to add to this
		   * data point (ignored if {@code null}).
		   * @throws IllegalArgumentException if {@code xtratag != null} and it
		   * doesn't follow the {@code name=value} format.
		   */
		  public void record(final String name,long value, String xtratag) {
			  try {
			  
				jsonGen.writeStartObject(); 
				jsonGen.writeStringField("m", prefix + "." + name);
				jsonGen.writeNumberField("ts", System.currentTimeMillis() / 1000);
				jsonGen.writeNumberField("v", value);
				jsonGen.writeObjectFieldStart("tags");
	
				if (xtratag != null) {
					if (xtratag.indexOf('=') != xtratag.lastIndexOf('=')) {
						throw new IllegalArgumentException("invalid xtratag: " + xtratag
								+ " (multiple '=' signs), name=" + name + ", value=" + value);
					} else if (xtratag.indexOf('=') < 0) {
						throw new IllegalArgumentException("invalid xtratag: " + xtratag
								+ " (missing '=' signs), name=" + name + ", value=" + value);
					}
					String[] pair = splitTag(xtratag.trim());
					jsonGen.writeStringField(pair[0], pair[1]);
				}
	
			    if (extratags != null) {
			      for (final Map.Entry<String, String> entry : extratags.entrySet()) {
			    	  jsonGen.writeStringField(entry.getKey(), entry.getValue());
			      }
				}
				
				
				jsonGen.writeEndObject(); // closing tags
				jsonGen.writeEndObject(); // closing stat
				
			  } catch (Exception ex) {
				  throw new RuntimeException("Failed to record stat", ex);
			  }
		  }
	  }


}
