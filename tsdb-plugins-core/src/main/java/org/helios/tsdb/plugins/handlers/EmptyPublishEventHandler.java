/**
 * tsdb-plugins-core
 */
package org.helios.tsdb.plugins.handlers;

import org.helios.tsdb.plugins.event.TSDBEvent;
import org.helios.tsdb.plugins.event.TSDBPublishEvent;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import com.lmax.disruptor.EventHandler;

/**
 * <p>Title: EmptyPublishEventHandler</p>
 * <p>Description: Empty publish event handler for implementing OpenTSDB {@link net.opentsdb.tsd.RTPublisher} event handlers.</p> 
 * @author Nicholas Whitehead
 * <p><code>org.helios.tsdb.plugins.handlers.EmptyPublishEventHandler</code></p>
 */
public class EmptyPublishEventHandler extends AbstractTSDBEventHandler implements EventHandler<TSDBEvent>, IPublishEventHandler  {


	/**
	 * {@inheritDoc}
	 * @see com.lmax.disruptor.EventHandler#onEvent(java.lang.Object, long, boolean)
	 */
	@Override
	public void onEvent(TSDBEvent event, long sequence, boolean endOfBatch) throws Exception {
		log.info("Processing Sequence {} for Event [{}]", sequence, event);
//		if(event.eventType==null || !event.eventType.isForPulisher()) return;		
//		if (event.eventType == TSDBEventType.DPOINT_DOUBLE) {
//			publishDataPoint(event.metric, event.timestamp, event.doubleValue, event.tags, event.tsuidBytes);
//		} else if (event.eventType == TSDBEventType.DPOINT_LONG) {
//			publishDataPoint(event.metric, event.timestamp, event.longValue, event.tags, event.tsuidBytes);
//		} else if (event.eventType == TSDBEventType.STATS_COLLECT) {
//		
//		} else {
//			// Programmer Error ?
//		}		
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
	

	
	



}
