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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewConstructor;
import javassist.CtNewMethod;
import javassist.Modifier;

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
	/** Cache of created invoker maps keyed by target class */
	protected static final Map<Class<?>, Map<String, Map<String, AbstractJSONRequestHandlerInvoker>>> invokerCache = new ConcurrentHashMap<Class<?>, Map<String, Map<String, AbstractJSONRequestHandlerInvoker>>>();

	/**
	 * Creates a map of concrete json request handler invokers keyed by <b><code>&lt;service-name&gt;/&lt;op-name&gt;</code></b>.
	 * @param handlerInstance The request handler instance to generate invokers for
	 * @return the map of generated invokers
	 */
	public static Map<String, Map<String, AbstractJSONRequestHandlerInvoker>> createInvokers(Object handlerInstance) {
		if(handlerInstance==null) throw new IllegalArgumentException("The passed handlerInstance was null");
		Map<String, AbstractJSONRequestHandlerInvoker> subInvokerMap = new HashMap<String, AbstractJSONRequestHandlerInvoker>();
		Map<String, Map<String, AbstractJSONRequestHandlerInvoker>> invokerMap = invokerCache.get(handlerInstance.getClass());
		if(invokerMap!=null) {
			LOG.info("Found Cached Invokers for [{}]", handlerInstance.getClass().getName());
			return invokerMap;
		}
		invokerMap = new HashMap<String, Map<String, AbstractJSONRequestHandlerInvoker>>(1);
		
		LOG.info("Generating Invokers for [{}]", handlerInstance.getClass().getName());
		JSONRequestService svc = handlerInstance.getClass().getAnnotation(JSONRequestService.class);
		final String invokerServiceKey = svc.name();
		final String invokerServiceDescription = svc.description();
		
		
		invokerMap.put(invokerServiceKey, subInvokerMap);
		
		ClassPool cp = new ClassPool();
		cp.appendClassPath(new ClassClassPath(handlerInstance.getClass()));
		cp.appendClassPath(new ClassClassPath(AbstractJSONRequestHandlerInvoker.class));
		cp.importPackage(handlerInstance.getClass().getPackage().getName());
		
		try {
			final CtClass jsonRequestCtClass = cp.get(JSONRequest.class.getName());
			final CtClass parent = cp.get(AbstractJSONRequestHandlerInvoker.class.getName());
			CtClass targetClass = cp.get(handlerInstance.getClass().getName());
			Collection<Method> methods = getTargetMethods(handlerInstance.getClass());
			for(Method m: methods) {
				final JSONRequestHandler jsonHandler = m.getAnnotation(JSONRequestHandler.class);
				final String opName = jsonHandler.name();
				final String opDescription = jsonHandler.description();
				final String unSubOp = jsonHandler.unsub();
				final String unSubService = svc.name();
				final boolean sub = jsonHandler.sub();
				
				int targetMethodHashCode = m.toGenericString().hashCode(); 
				final String className = String.format("%s-%s%s-%s-%s", 
						handlerInstance.getClass().getName(), invokerServiceKey, opName, "ServiceInvoker", targetMethodHashCode);
				
				final CtClass invokerClass = cp.makeClass(className, parent);
				CtField ctf = new CtField(targetClass, "typedTarget", invokerClass);
				ctf.setModifiers(ctf.getModifiers() | Modifier.FINAL);
				invokerClass.addField(ctf);
				for(CtConstructor parentCtor: parent.getConstructors()) {
					CtConstructor invokerCtor = CtNewConstructor.copy(parentCtor, invokerClass, null);
					invokerCtor.setBody("{ super($$); typedTarget = (" + handlerInstance.getClass().getName() + ")$1; }");
					invokerClass.addConstructor(invokerCtor);					
				}
				CtMethod invokerMethod = CtNewMethod.copy(parent.getDeclaredMethod("doInvoke", new CtClass[] {jsonRequestCtClass}), invokerClass, null); 
						
				invokerMethod.setBody("{this.typedTarget." + m.getName() + "($1);}");
				invokerMethod.setModifiers(invokerMethod.getModifiers() & ~Modifier.ABSTRACT);
				invokerClass.addMethod(invokerMethod);
				Class<?> clazz = invokerClass.toClass();
				Constructor<?> ctor = null;
				AbstractJSONRequestHandlerInvoker invokerInstance = null;
				if(sub) {
					ctor = clazz.getDeclaredConstructor(Object.class, String.class, String.class, String.class, String.class, boolean.class, String.class, String.class);
					invokerInstance = (AbstractJSONRequestHandlerInvoker)ctor.newInstance(handlerInstance, invokerServiceKey, invokerServiceDescription, opName, opDescription, sub, unSubOp, unSubService);										
				} else {
					ctor = clazz.getDeclaredConstructor(Object.class, String.class, String.class, String.class, String.class);
					invokerInstance = (AbstractJSONRequestHandlerInvoker)ctor.newInstance(handlerInstance, invokerServiceKey, invokerServiceDescription, opName, opDescription);					
				}
				subInvokerMap.put(opName, invokerInstance);				
			}
			invokerCache.put(handlerInstance.getClass(), invokerMap);
			return invokerMap;
		} catch (Exception ex) {
			LOG.error("Failed to create RequestHandlerInvoker for [{}]", handlerInstance.getClass().getName(), ex);
			throw new RuntimeException("Failed to create RequestHandlerInvoker [" + handlerInstance.getClass().getName() + "]", ex);
		}
		
	}
	
	
	public static void main(String[] args) {
		createInvokers(new FooService());
		createInvokers(new FooService());
	}
	
	@JSONRequestService(name="foo")
	public static class FooService {
		@JSONRequestHandler(name="bar")
		public void bar(JSONRequest request) {
			
		}
	}
	
	/**
	 * Finds and returns the valid target {@link JSONRequestHandler} annotated methods in the passed class.
	 * @param clazz the class to inspect
	 * @return a collection of valid json request methods
	 */
	public static Collection<Method> getTargetMethods(Class<?> clazz) {
		Map<String, Method> mappedMethods = new HashMap<String, Method>();
		for(Method m: clazz.getMethods()) {
			JSONRequestHandler jsonHandler = m.getAnnotation(JSONRequestHandler.class);
			if(jsonHandler!=null) {
				Class<?>[] paramTypes = m.getParameterTypes();
				if(paramTypes.length!=1 || !JSONRequest.class.equals(paramTypes[0])) {
					LOG.warn("Invalid @JSONRequestHandler annotated method [{}]", m.toGenericString());
					continue;
				}
				mappedMethods.put(m.getName(), m);
			}
		}
		for(Method m: clazz.getDeclaredMethods()) {
			JSONRequestHandler jsonHandler = m.getAnnotation(JSONRequestHandler.class);
			if(jsonHandler!=null) {
				Class<?>[] paramTypes = m.getParameterTypes();
				if(paramTypes.length!=1 || !JSONRequest.class.equals(paramTypes[0])) {
					LOG.warn("Invalid @JSONRequestHandler annotated method [{}]", m.toGenericString());
					continue;
				}
				mappedMethods.put(m.getName(), m);
			}			
		}
		return mappedMethods.values();
		
	}
	
	private JSONRequestHandlerInvokerFactory() {
	}

}
