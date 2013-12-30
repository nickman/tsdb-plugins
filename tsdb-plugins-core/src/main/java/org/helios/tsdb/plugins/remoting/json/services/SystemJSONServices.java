/**
 * 
 */
package org.helios.tsdb.plugins.remoting.json.services;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.helios.tsdb.plugins.remoting.json.JSONRequest;
import org.helios.tsdb.plugins.remoting.json.annotations.JSONRequestHandler;
import org.helios.tsdb.plugins.remoting.json.annotations.JSONRequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * <p>Title: SystemJSONServices</p>
 * <p>Description:  A JSON service to provide some generic system operations</p>
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><b><code>org.helios.tsdb.plugins.remoting.json.services.SystemJSONServices</code></b>
 */
@JSONRequestService(name="system", description="Some generic system services")
public class SystemJSONServices {
	/** The json node factory */
	private final JsonNodeFactory nodeFactory = JsonNodeFactory.instance;
	/** Scheduler */
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1, new ThreadFactory(){
		final AtomicInteger serial = new AtomicInteger();
		@Override
		public Thread newThread(Runnable r) {
			Thread t = new Thread(r, "SystemJSONServicesScheduler#" + serial.incrementAndGet());
			t.setDaemon(true);
			return t;
		}});
	/** Instance logger */
	protected final Logger log = LoggerFactory.getLogger(getClass());
	
	/**
	 * Helper service to sleep for a defined period
	 * @param request the JSON request
	 */
	@JSONRequestHandler(name="sleep", description="Sleeps for a defined period")
	public void sleep(final JSONRequest request) {
		request.allowDefaults(false);
		final long sleepMs = request.get("sleep", -1L);
		scheduler.schedule(new Runnable(){
			public void run() {
				request.response().send();
			}
		}, sleepMs, TimeUnit.MILLISECONDS);
	}
	
	/**
	 * Writes out the system properties as JSON to the caller
	 * @param request the request
	 */
	@JSONRequestHandler(name="sysprops", description="Returns a json map of system properties")
	public void sysProps(JSONRequest request) {
		ObjectNode sysPropsMap = nodeFactory.objectNode();
		ObjectNode pMap = nodeFactory.objectNode();
		sysPropsMap.put("sysprops", pMap);
		Properties props = System.getProperties();
		for(String key: props.stringPropertyNames()) {
			pMap.put(key, props.getProperty(key));
		}
		try {			
			request.response().setContent(sysPropsMap).send();
		} catch (Exception ex) {
			log.error("Failed to write sysprops", ex);
			request.error("Failed to write sysprops", ex).send();
		}
	}
	
	/**
	 * Writes out the JVM's environmental variables as JSON to the caller
	 * @param request the request
	 */
	@JSONRequestHandler(name="env", description="Returns a json map of the JMV environmental variables")
	public void envProps(JSONRequest request) {
		ObjectNode envPropsMap = nodeFactory.objectNode();
		ObjectNode eMap = nodeFactory.objectNode();
		envPropsMap.put("env", eMap);
		Map<String, String> env = System.getenv();
		for(Map.Entry<String, String> e: env.entrySet()) {
			eMap.put(e.getKey(), e.getValue());
		}
		try {			
			request.response().setContent(envPropsMap).send();
		} catch (Exception ex) {
			log.error("Failed to write environment variables", ex);
			request.error("Failed to write environment variables", ex).send();
		}
	}
	
	/**
	 * Echos back the <b><code>msg</code></b> keyed argument in the passed request
	 * @param request the request
	 */
	@JSONRequestHandler(name="echo", description="Echos back the 'msg' keyed argument in the passed request")
	public void echo(JSONRequest request) {
		try {			
			request.response().setContent(request.getArgument("msg")).send();
		} catch (Exception ex) {
			log.error("Failed to write echo", ex);
			request.error("Failed to write echo", ex).send();
		}		
	}
	
}
