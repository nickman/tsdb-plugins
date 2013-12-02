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

import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;

import org.helios.tsdb.plugins.remoting.json.annotations.JSONRequestHandler;
import org.helios.tsdb.plugins.remoting.json.annotations.JSONRequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Title: JSONRequestHandlerInvokerFactory</p>
 * <p>Description: A factory for generating json request handler invokers.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.remoting.json.JSONRequestHandlerInvokerFactory</code></p>
 */

public class JSONRequestHandlerInvokerFactory {
	/** Static class logger */
	protected static final Logger LOG = LoggerFactory.getLogger(JSONRequestHandlerInvokerFactory.class);

	/**
	 * Creates a map of concrete json request handler invokers keyed by <b><code>&lt;service-name&gt;/&lt;op-name&gt;</code></b>.
	 * @param handlerInstance The request handler instance to generate invokers for
	 * @return the map of generated invokers
	 */
	public static Map<String, JSONRequestHandlerInvoker> createInvokers(Object handlerInstance) {
		if(handlerInstance==null) throw new IllegalArgumentException("The passed handlerInstance was null");
		Map<String, JSONRequestHandlerInvoker> invokerMap = new HashMap<String, JSONRequestHandlerInvoker>();
		LOG.info("Generating Invokers for [{}]", handlerInstance.getClass().getName());
		final String invokerServiceKey;
		JSONRequestService svc = handlerInstance.getClass().getAnnotation(JSONRequestService.class);
		if(svc==null) {
			invokerServiceKey = "";
		} else {
			invokerServiceKey  = svc.name();
		}
		//JSONRequestHandler handler = handlerInstance.getClass().getAnnotation(JSONRequestHandler.class);
		
		ClassPool cp = new ClassPool();
		cp.appendClassPath(new ClassClassPath(handlerInstance.getClass()));
		cp.appendClassPath(new ClassClassPath(JSONRequestHandler.class));
		CtClass invokerClass = cp.makeClass(handlerInstance.getClass().getName() + "." + invokerServiceKey + "ServiceInvoker"); 
		invokerClass.addInterface(cp.get(JSONRequestHandlerInvoker.class.getName()));
		
		
		return invokerMap;
	}
	
	private JSONRequestHandlerInvokerFactory() {
	}

}
