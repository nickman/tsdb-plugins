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
package org.helios.tsdb.plugins.handlers;

import net.opentsdb.stats.StatsCollector;

import org.cliffc.high_scale_lib.Counter;
import org.cliffc.high_scale_lib.NonBlockingHashMap;
import org.helios.jmx.util.unsafe.collections.ConcurrentLongSlidingWindow;
import org.helios.tsdb.plugins.event.TSDBEvent;
import org.helios.tsdb.plugins.event.TSDBEventType;
import org.helios.tsdb.plugins.event.TSDBSearchEvent;
import org.helios.tsdb.plugins.service.PluginContext;
import org.helios.tsdb.plugins.util.ConfigurationHelper;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import com.lmax.disruptor.EventHandler;

/**
 * <p>Title: EmptySearchEventHandler</p>
 * <p>Description: Empty search event handler for implementing OpenTSDB {@link net.opentsdb.search.SearchPlugin} event handlers.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.handlers.EmptySearchEventHandler</code></p>
 */

public class EmptySearchEventHandler  extends AbstractTSDBEventHandler implements EventHandler<TSDBEvent>, ISearchEventHandler {
	/** A map of invocation counts for each operation handled by this handler */
	protected final NonBlockingHashMap<TSDBEventType, Counter> invocationCounts = new NonBlockingHashMap<TSDBEventType, Counter>(TSDBEventType.values().length);
	/** A map of elapsed time sliding windows for each operation handled by this handler */
	protected final NonBlockingHashMap<TSDBEventType, ConcurrentLongSlidingWindow> invocationTimes = new NonBlockingHashMap<TSDBEventType, ConcurrentLongSlidingWindow>(TSDBEventType.values().length);

	/** The config property name to specify the sliding window size for operation elapsed times */
	public static final String ES_SW_SIZE = "es.tsd.search.elasticsearch.sliding.size";
	/** The default sliding window size for operation elapsed times */
	public static final int DEFAULT_ES_SW_SIZE = 100;
	
	/** Indicates if this handler is intended to handle 
	 * {@link net.opentsdb.search.SearchPlugin#executeQuery(net.opentsdb.search.SearchQuery)} 
	 * sourced events. 
	 */
	protected boolean searchEnabled = false;


	/**
	 * Creates a new EmptySearchEventHandler
	 */
	protected EmptySearchEventHandler() {
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.handlers.AbstractTSDBEventHandler#initialize(org.helios.tsdb.plugins.service.PluginContext)
	 */
	@Override
	public void initialize(PluginContext pc) {
		super.initialize(pc);
		int swSize = ConfigurationHelper.getIntSystemThenEnvProperty(ES_SW_SIZE, DEFAULT_ES_SW_SIZE, pc.getExtracted());
		StringBuilder b = new StringBuilder("\nMetric Collection Keys:\n========================");
		for(TSDBEventType et: TSDBEventType.values()) {
			if(et.isForSearch()) {
				b.append("\n\t").append(et.name());
				invocationCounts.put(et, new Counter());
				invocationTimes.put(et, new ConcurrentLongSlidingWindow(swSize));
			}
		}
		log.info(b.toString());
	}
	
	/**
	 * Increments the invocation count for the passed event
	 * @param event the event received
	 */
	protected void incrCount(TSDBEvent event) {
		invocationCounts.get(event.eventType).increment();		
	}	
	
	/**
	 * Registers the elapsed time of an operation
	 * @param event The event processed
	 * @param elapsed The elapsed time of the operation in ms.
	 */
	protected void elapsedTime(TSDBEvent event, long elapsed) {
		invocationTimes.get(event.eventType).insert(elapsed);
	}

	/**
	 * {@inheritDoc}
	 * @see com.lmax.disruptor.EventHandler#onEvent(java.lang.Object, long, boolean)
	 */
	@Override
	public void onEvent(TSDBEvent event, long sequence, boolean endOfBatch) throws Exception {
//		if(event.eventType==null || !event.eventType.isForSearch()) return;
//		switch(event.eventType) {
//		case ANNOTATION_DELETE:
//			deleteAnnotation(event.annotation);
//			break;
//		case ANNOTATION_INDEX:
//			indexAnnotation(event.annotation);
//			break;
//		case SEARCH:
//			executeQuery(event.searchQuery, event.deferred);
//			break;
//		case TSMETA_DELETE:
//			deleteTSMeta(event.tsuid);
//			break;
//		case TSMETA_INDEX:
//			indexTSMeta(event.tsMeta);
//			break;
//		case UIDMETA_DELETE:
//			deleteUIDMeta(event.uidMeta);
//			break;
//		case UIDMETA_INDEX:
//			indexUIDMeta(event.uidMeta);
//			break;
//		default:
//			//  ??  Programmer Error ?
//			break;			
//		}				
	}
	
	/**
	 * Handles a search event from the TSDB through the event bus
	 * @param event The published event to dispatch
	 * @throws Exception thrown on failures in execution
	 */
	@Subscribe
	@AllowConcurrentEvents	
	public void onEvent(TSDBSearchEvent event) throws Exception {
		onEvent(event, -1, false);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.handlers.AbstractTSDBEventHandler#shutdown()
	 */
	@Override
	public void shutdown() {
		
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.handlers.AbstractTSDBEventHandler#collectStats(net.opentsdb.stats.StatsCollector)
	 */
	@Override
	public void collectStats(StatsCollector collector) {
		
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.handlers.ISearchEventHandler#setExecuteSearchEnabled(boolean)
	 */
	@Override
	public void setExecuteSearchEnabled(boolean enabled) {
		searchEnabled = enabled;		
	}


}
