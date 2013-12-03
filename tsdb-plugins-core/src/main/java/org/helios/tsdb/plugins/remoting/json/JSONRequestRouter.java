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
package org.helios.tsdb.plugins.remoting.json;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.helios.tsdb.plugins.remoting.json.annotations.JSONRequestHandler;
import org.helios.tsdb.plugins.remoting.json.annotations.JSONRequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * <p>Title: JSONRequestRouter</p>
 * <p>Description: Examines JSON requests and routes them to the correct {@link JSONRequestService} annotated instance.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.remoting.json.JSONRequestRouter</code></p>
 */
@JSONRequestService(name="JSONRequestRouter", description="The main JSON request routing service")
public class JSONRequestRouter {
	/** The singleton instance */
	protected static volatile JSONRequestRouter instance = null;
	/** The singleton instance ctor lock */
	protected static final Object lock = new Object();	
	/** Instance logger */
	protected final Logger log = LoggerFactory.getLogger(getClass());
	/** The invoker map */
	protected final ConcurrentHashMap<String, Map<String, AbstractJSONRequestHandlerInvoker>> invokerMap = new ConcurrentHashMap<String, Map<String, AbstractJSONRequestHandlerInvoker>>();
	/** The json mapper */
	private static final ObjectMapper jsonMapper = new ObjectMapper();
	
	
	/**
	 * Acquires and returns the singleton instance
	 * @return the singleton instance
	 */
	public static JSONRequestRouter getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new JSONRequestRouter(); 
				}
			}
		}
		return instance;
	}

	/**
	 * Creates a new JSONRequestRouter
	 */
	private JSONRequestRouter() {
		registerJSONService(this);
	}
	
	/**
	 * Registers a new JSON Service which are pojos 
	 * @param service An object annotated with @JSONRequestService and @JSONRequestHandler annotations.
	 */
	public void registerJSONService(Object service) {
		if(service==null) throw new IllegalArgumentException("The passed JSON Service was null");
		for(Map.Entry<String, Map<String, AbstractJSONRequestHandlerInvoker>> entry: JSONRequestHandlerInvokerFactory.createInvokers(service).entrySet()) {
			invokerMap.putIfAbsent(entry.getKey(), entry.getValue());
			log.info("Added [{}] JSONRequest Operations for Service [{}] from impl [{}]", entry.getValue().size(), entry.getKey(), service.getClass().getName());
		}
	}
	
	/**
	 * Routes a json request to the intended request handler
	 * @param jsonRequest The request to route
	 */
	public void route(JSONRequest jsonRequest) {
		Map<String, AbstractJSONRequestHandlerInvoker> imap = invokerMap.get(jsonRequest.serviceName);
		if(imap==null) {
			jsonRequest.error("Failed to route to service name [" + jsonRequest.serviceName + "]").send();
			return;
		}
		AbstractJSONRequestHandlerInvoker invoker = imap.get(jsonRequest.opName);
		if(invoker==null) {
			jsonRequest.error("Failed to route to op [" + jsonRequest.serviceName + "/" + jsonRequest.opName + "]").send();
			return;
		}
		invoker.invokeJSONRequest(jsonRequest);		
	}
	
	/**
	 * Writes a JSON catalog of the available services
	 * @param jsonRequest The json request
	 */
	@JSONRequestHandler(name="services", description="Returns a catalog of available JSON services")
	public void services(JSONRequest jsonRequest) {
		Map<ObjectNode, Map<String, String>> serviceMap = new HashMap<ObjectNode, Map<String, String>>();
		for(Map.Entry<String, Map<String, AbstractJSONRequestHandlerInvoker>> entry: invokerMap.entrySet()) {
			Map<String, AbstractJSONRequestHandlerInvoker> opInvokerMap = entry.getValue();
			if(opInvokerMap.isEmpty()) continue;
			ObjectNode key = JsonNodeFactory.instance.objectNode();
			key.put("svc", entry.getKey());
			key.put("desc", opInvokerMap.values().iterator().next().getServiceDescription());
			Map<String, String> opMap = new HashMap<String, String>(opInvokerMap.size());
			for(AbstractJSONRequestHandlerInvoker invoker: opInvokerMap.values()) {
				opMap.put(invoker.getOpName(), invoker.getOpDescription());
			}
			serviceMap.put(key, opMap);
		}
		try {
			jsonRequest.response().setContent(jsonMapper.writeValueAsBytes(serviceMap)).send();
		} catch (Exception ex) {
			log.error("Failed to write service catalog", ex);
			jsonRequest.error("Failed to write service catalog", ex).send();
		}
	}

}
