/**
 * tsdb-plugins-core
 */
package org.helios.tsdb.plugins.handlers;

import java.util.Map;

import net.opentsdb.core.TSDB;

import org.helios.tsdb.plugins.event.TSDBEvent;
import org.helios.tsdb.plugins.event.TSDBEventType;
import org.helios.tsdb.plugins.event.TSDBPublishEvent;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import com.lmax.disruptor.EventHandler;

/**
 * <p>Title: EmptyPublishEventHandler</p>
 * <p>Description: Base class for implementing OpenTSDB {@link net.opentsdb.tsd.RTPublisher} event handlers.</p> 
 * @author Nicholas Whitehead
 * <p><code>org.helios.tsdb.plugins.handlers.EmptyPublishEventHandler</code></p>
 */
public class EmptyPublishEventHandler extends AbstractTSDBEventHandler implements EventHandler<TSDBEvent>  {

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.handlers.ITSDBEventHandler#start()
	 */
	@Override
	public void start() {
		// TODO Auto-generated method stub

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.handlers.ITSDBEventHandler#stop()
	 */
	@Override
	public void stop() {
		// TODO Auto-generated method stub

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.handlers.ITSDBEventHandler#configure(net.opentsdb.core.TSDB)
	 */
	@Override
	public void configure(TSDB tsdb) {
		// TODO Auto-generated method stub

	}

	/**
	 * {@inheritDoc}
	 * @see com.lmax.disruptor.EventHandler#onEvent(java.lang.Object, long, boolean)
	 */
	@Override
	@Subscribe
	@AllowConcurrentEvents
	public void onEvent(TSDBEvent event, long sequence, boolean endOfBatch) throws Exception {
		if(event.eventType==null || !event.eventType.isForPulisher()) return;		
		if (event.eventType == TSDBEventType.DPOINT_DOUBLE) {
			publishDataPoint(event.metric, event.timestamp, event.doubleValue, event.tags, event.tsuidBytes);
		} else if (event.eventType == TSDBEventType.DPOINT_LONG) {
			publishDataPoint(event.metric, event.timestamp, event.longValue, event.tags, event.tsuidBytes);
		} else if (event.eventType == TSDBEventType.STATS_COLLECT) {
		
		} else {
			// Programmer Error ?
		}		
	}
	
	/**
	 * Handles a publish event from the TSDB through the event bus
	 * @param event The published event to dispatch
	 * @throws Exception thrown on failures in execution
	 */
	@Subscribe
	@AllowConcurrentEvents	
	public void onEvent(TSDBPublishEvent event) throws Exception {
		onEvent(event, -1L, false);
	}
	
	
	/**
	 * Called any time a new data point is published
	 * @param metric The name of the metric associated with the data point
	 * @param timestamp Timestamp as a Unix epoch in seconds or milliseconds (depending on the TSD's configuration)
	 * @param value Value for the data point
	 * @param tags The metric tags
	 * @param tsuid Time series UID for the value
	 * @see net.opentsdb.tsd.RTPublisher#publishDataPoint(java.lang.String, long, double, java.util.Map, byte[])
	 * <b>Note:</b>Extend me ! I don't do anything.
	 */
	protected void publishDataPoint(String metric, long timestamp, double value, Map<String, String> tags, byte[] tsuid) {
		
	}

	/**
	 * Called any time a new data point is published
	 * @param metric The name of the metric associated with the data point
	 * @param timestamp Timestamp as a Unix epoch in seconds or milliseconds (depending on the TSD's configuration)
	 * @param value Value for the data point
	 * @param tags The metric tags
	 * @param tsuid Time series UID for the value
	 * @see net.opentsdb.tsd.RTPublisher#publishDataPoint(java.lang.String, long, long, java.util.Map, byte[])
	 * <b>Note:</b>Extend me ! I don't do anything.
	 */
	protected void publishDataPoint(String metric, long timestamp, long value, Map<String, String> tags, byte[] tsuid) {
		
	}	
	


}
