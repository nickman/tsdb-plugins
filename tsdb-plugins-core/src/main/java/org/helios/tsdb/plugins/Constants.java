package org.helios.tsdb.plugins;

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
	
	/** A null deferred response const */
	public static final Deferred<Object> NULL_DEFERED = Deferred.fromResult(null);
	
	private Constants() {
	}

}
