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
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.opentsdb.core.TSDB;

import org.helios.tsdb.plugins.remoting.json.JSONRequest;
import org.helios.tsdb.plugins.remoting.json.JSONResponse;
import org.helios.tsdb.plugins.remoting.json.annotations.JSONRequestHandler;
import org.helios.tsdb.plugins.remoting.json.annotations.JSONRequestService;
import org.helios.tsdb.plugins.remoting.json.services.InvocationChannel;
import org.helios.tsdb.plugins.service.PluginContext;
import org.helios.tsdb.plugins.service.TSDBPluginServiceLoader;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferInputStream;
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

/**
 * <p>Title: TSDBJSONService</p>
 * <p>Description: Websocket RPC service adapter for OpenTSDB's HTTP API</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>net.opentsdb.tsd.TSDBJSONService</code></p>
 */
@JSONRequestService(name="tsdb", description="TSDB JSON Remoting Services")
public class TSDBJSONService {
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

	/**
	 * Creates a new TSDBJSONService
	 */
	public TSDBJSONService() {
		
		tsdb = TSDBPluginServiceLoader.getLoaderInstance().getTSDB();
		pluginContext = TSDBPluginServiceLoader.getLoaderInstance().getPluginContext();		
		RpcHandler rpcHandler = new RpcHandler(tsdb);
		loadInstanceOf("net.opentsdb.tsd.RpcHandler$DieDieDie", "diediedie", rpcHandler);
		final StaticFileRpc staticfile = new StaticFileRpc();
	    http_commands.put("favicon.ico", staticfile);
	    http_commands.put("s", staticfile);
	    final StatsRpc stats = new StatsRpc();
	    http_commands.put("api/stats", stats);
	    loadInstanceOf("net.opentsdb.tsd.RpcHandler$Version", "api/version", null);
	    loadInstanceOf("net.opentsdb.tsd.RpcHandler$DropCaches", "api/dropcaches", null);
        final PutDataPointRpc put = new PutDataPointRpc();	    
	    http_commands.put("api/put", put);
	    loadInstanceOf("net.opentsdb.tsd.RpcHandler$ListAggregators", "api/aggregators", null);
	    http_commands.put("logs", new LogsRpc());
	    http_commands.put("q", new GraphHandler());
	    final SuggestRpc suggest_rpc = new SuggestRpc();
	    http_commands.put("api/suggest", suggest_rpc);
	    http_commands.put("api/uid", new UniqueIdRpc());
	    http_commands.put("api/query", new QueryRpc());
	    http_commands.put("api/tree", new TreeRpc());
	    http_commands.put("api/annotation", new AnnotationRpc());
	    http_commands.put("api/search", new SearchRpc());
	    loadInstanceOf("net.opentsdb.tsd.RpcHandler$Serializers", "api/serializers", null);
	    loadInstanceOf("net.opentsdb.tsd.RpcHandler$ShowConfig", "api/config", null);
	}
	
	/**
	 * Handles a prepared WebSocket API invocation to retrieve a static file
	 * @param asMap true if the JSON reponse is a map, false if it is an array
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
		List<Map.Entry<String, String>> responseHeaders = resp.getHeaders();
		for(Map.Entry<String, String> entry: responseHeaders) {
			generator.writeStringField(entry.getKey(), entry.getValue());
			log.info("Reponse Header: [{}] : [{}]", entry.getKey(), entry.getValue());
		}
		ChannelBuffer content = resp.getContent();
		String cType = resp.getHeader("Content-Type");
		if(cType!=null) {
			if(cType.startsWith("text")) {
				generator.writeStringField("content", content.toString(Charset.defaultCharset()));
			} else {
				ChannelBuffer b64 = Base64.encode(content);
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
		
		String cType = resp.getHeader("Content-Type");
		if(cType!=null) {
			if(cType.startsWith("text")) {
				
			} else {
				
			}
		}
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
			request.error("Failed to invoke die", ex);
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
			request.error("Failed to invoke stats", ex);
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
			request.error("Failed to invoke s/file", ex);
		}
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

}
