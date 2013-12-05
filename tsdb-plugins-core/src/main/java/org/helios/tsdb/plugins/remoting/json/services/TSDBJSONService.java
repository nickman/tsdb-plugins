/**
 * 
 */
package org.helios.tsdb.plugins.remoting.json.services;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import net.opentsdb.core.TSDB;

import org.helios.tsdb.plugins.remoting.json.JSONRequest;
import org.helios.tsdb.plugins.remoting.json.annotations.JSONRequestHandler;
import org.helios.tsdb.plugins.service.PluginContext;
import org.helios.tsdb.plugins.service.TSDBPluginServiceLoader;
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
	 */
	@JSONRequestHandler(name="sysprops", description="Returns a json map of system properties")
	public void addPoint(final JSONRequest request) {
		//final long rid = request.requestId;
		try {
			ArrayNode pointsArr = (ArrayNode)request.getRequest().get("points");
			for(int i = 0; i < pointsArr.size(); i++) {
				ObjectNode point = (ObjectNode)pointsArr.get(i);
				String metric = point.get("m").asText();
				long timestamp = point.get("m").asLong();
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
				final String metricName = String.format("%s:%s", metric, tags.toString());
				if("L".equals(numericType)) {
					tsdb.addPoint(metric, timestamp, point.get("v").asLong(), tags).addCallback(new Callback<Object, Object>(){
						@Override
						public Object call(Object arg) throws Exception {							
							request.response().setContent(metricName).send();
							return null;
						}
					});
				} else if("F".equals(numericType)) {
					float f = (float)point.get("v").asDouble();
					tsdb.addPoint(metric, timestamp, f, tags).addCallback(new Callback<Object, Object>(){
						@Override
						public Object call(Object arg) throws Exception {							
							request.response().setContent(metricName).send();
							return null;
						}
					});
				} else if("D".equals(numericType)) {
					tsdb.addPoint(metric, timestamp, point.get("v").asDouble(), tags).addCallback(new Callback<Object, Object>(){
						@Override
						public Object call(Object arg) throws Exception {							
							request.response().setContent(metricName).send();
							return null;
						}
					});
				}
			}
		} catch (Exception ex) {
			log.error("Failed to add points", ex);
			request.error("Failed to add points", ex).send();
		}
	}

}
