/**
 * tsdb-plugins-core
 */
package org.helios.tsdb.plugins.asynch.handlers;

import org.helios.tsdb.plugins.event.TSDBEvent;

import net.opentsdb.core.TSDB;

/**
 * <p>Title: ITSDBEventHandler</p>
 * <p>Description: Generalized interface for asynch {@link TSDBEvent} handlers</p> 
 * @author Nicholas Whitehead
 * <p><code>org.helios.tsdb.plugins.asynch.ITSDBEventHandler</code></p>
 */
public interface ITSDBEventHandler {
	/**
	 * Starts the event handler
	 */
	public void start();
	
	/**
	 * Stops the event handler
	 */
	public void stop();
	
	/**
	 * Configures the event handler
	 * @param tsdb The passed TSDB instance containing the config
	 */
	public void configure(TSDB tsdb);
}
