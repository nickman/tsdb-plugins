/**
 * 
 */
package org.helios.tsdb.plugins.remoting.json.services;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import net.opentsdb.core.TSDB;

import org.helios.tsdb.plugins.remoting.json.JSONRequest;
import org.helios.tsdb.plugins.remoting.json.annotations.JSONRequestHandler;
import org.helios.tsdb.plugins.remoting.json.annotations.JSONRequestService;
import org.helios.tsdb.plugins.service.PluginContext;
import org.helios.tsdb.plugins.service.TSDBPluginServiceLoader;
import org.helios.tsdb.plugins.util.SystemClock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	 * using a no-confirm at the request level as follows: <b><code>"noc":"true"</code></b>.</p>
	 * TODO: Handle errors and return error messages to caller.
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
				sendConfirm = !(tNode.toString().trim().equalsIgnoreCase("true"));
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
				request.response().setContent(nodeFactory.objectNode().set("points", nodeFactory.numberNode(pointsProcessed)));
			}
		} catch (Exception ex) {
			log.error("Failed to add points", ex);
			request.error("Failed to add points", ex).send();
		}
	}

}
