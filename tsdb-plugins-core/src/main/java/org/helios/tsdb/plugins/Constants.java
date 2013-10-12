package org.helios.tsdb.plugins;

import java.lang.management.ManagementFactory;

import org.helios.tsdb.plugins.async.EventBusEventDispatcher;

import com.stumbleupon.async.Deferred;

/**
 * <p>Title: Constants</p>
 * <p>Description: TSDB-Plugins Common Constants</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.Constants</code></p>
 */
public class Constants {

	/** Config property name for unsafe mem tracking */
	public static final String TRACK_MEM_PROP = "org.helios.tsdb.trackmem";
	/** The default unsafe mem tracking */
	public static final boolean DEFAULT_TRACK_MEM = false;
	
	/** The default plugin version to publish */
	public static final String PLUGIN_VERSION = "2.0.1";
	
	/** The number of processors available to the JVM */
	public static final int CORES = ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors();
	
	/** A null deferred response const */
	public static final Deferred<Object> NULL_DEFERED = Deferred.fromResult(null);
	
	/** The config property name for the class names of event handlers to register */
	public static final String EVENT_HANDLERS = "net.opentsdb.events.handlers";
	/** The config property name for the class name of the event publisher asynch dispatcher */
	public static final String ASYNC_DISPATCHER = "net.opentsdb.events.async.dispatcher";
	/** The default name for the class name of the event publisher asynch dispatcher */
	public static final String DEFAULT_ASYNC_DISPATCHER = EventBusEventDispatcher.class.getName();

	
	private Constants() {
	}

}
