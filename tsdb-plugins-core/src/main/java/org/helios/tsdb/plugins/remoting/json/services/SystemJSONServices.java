/**
 * 
 */
package org.helios.tsdb.plugins.remoting.json.services;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.helios.tsdb.plugins.remoting.json.JSONRequest;
import org.helios.tsdb.plugins.remoting.json.JSONResponse;
import org.helios.tsdb.plugins.remoting.json.RequestType;
import org.helios.tsdb.plugins.remoting.json.ResponseType;
import org.helios.tsdb.plugins.remoting.json.annotations.JSONRequestHandler;
import org.helios.tsdb.plugins.remoting.json.annotations.JSONRequestService;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.node.ArrayNode;
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
	
	/** A channel group of system status subscribers */
	protected final ChannelGroup systemStatusChannelGroup = new DefaultChannelGroup("SystemStatusSubscribers");
	/** A map of request ids keyed by the channel that created them */
	protected final Map<Channel, Long> systemStatusRids = new ConcurrentHashMap<Channel, Long>();
	
	/** The system status collection and publishing task */
	protected final Runnable statusTask = new Runnable(){
		public void run() {
			ObjectNode stats = SystemStatusService.collect();
			for(Channel channel: systemStatusChannelGroup) {
				Long rerid = systemStatusRids.get(channel);
				if(rerid==null) continue;
				new JSONResponse(rerid, ResponseType.SUB, channel, null).setContent(stats).send();
			}
		}
	};

	/** The schedule handle for the system status task */
	protected volatile ScheduledFuture<?> systemStatusSchedule = scheduler.scheduleWithFixedDelay(statusTask, 1, 15, TimeUnit.SECONDS);

	
	/**
	 * Subscribes the calling channel to system status messages
	 * @param request The request from the subscribing channel
	 * <p>Invoker:<b><code>sendRemoteRequest('ws://localhost:4243/ws', {svc:'system', op:'subsysstat'});</code></b>
	 */
	@JSONRequestHandler(name="subsysstat", type=RequestType.SUBSCRIBE, description="Subscribes the calling channel to system status messages")
	public void subscribeSystemStatus(final JSONRequest request) {
		if(systemStatusChannelGroup.add(request.channel)) {
			request.channel.getCloseFuture().addListener(new ChannelFutureListener(){
				@Override
				public void operationComplete(ChannelFuture future)	throws Exception {
					systemStatusRids.remove(future.getChannel());
				}
			});
			systemStatusRids.put(request.channel, request.requestId);
			ArrayNode arrNode = nodeFactory.arrayNode();
			arrNode.add(SystemStatusService.getBacklog());
			arrNode.add(SystemStatusService.collect());
			request.response(ResponseType.SUB_STARTED).setContent(arrNode).send();
		} else {
			request.response(ResponseType.SUB_STARTED).setContent(SystemStatusService.getBacklog()).send();
		}		
	}

	/**
	 * Subscribes the calling channel to system status messages
	 * @param request The request from the subscribing channel
	 */
	@JSONRequestHandler(name="unsubsysstat", description="Unsubscribes the calling channel from system status messages")
	public void stopSystemStatus(final JSONRequest request) {
		systemStatusChannelGroup.remove(request.channel);
		systemStatusRids.remove(request.channel);
		request.response(ResponseType.SUB_STOPPED).send();
	}
	
	
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
				request.response(ResponseType.RESP).send();
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
			request.response(ResponseType.RESP).setContent(sysPropsMap).send();
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
			request.response(ResponseType.RESP).setContent(envPropsMap).send();
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
			request.response(ResponseType.RESP).setContent(request.getArgument("msg")).send();
		} catch (Exception ex) {
			log.error("Failed to write echo", ex);
			request.error("Failed to write echo", ex).send();
		}		
	}
	
}
