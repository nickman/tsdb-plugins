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
package org.helios.tsdb.plugins.async;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;

import net.opentsdb.meta.Annotation;
import net.opentsdb.meta.TSMeta;
import net.opentsdb.meta.UIDMeta;
import net.opentsdb.search.SearchQuery;
import net.opentsdb.stats.StatsCollector;

import org.helios.tsdb.plugins.event.TSDBPublishEvent;
import org.helios.tsdb.plugins.event.TSDBSearchEvent;
import org.helios.tsdb.plugins.handlers.IEventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.DeadEvent;
import com.stumbleupon.async.Deferred;

/**
 * <p>Title: EventBusEventDispatcher</p>
 * <p>Description: An async event dispatcher that uses a Guava {@link AsyncEventBus} to manage async dispatches.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.async.EventBusEventDispatcher</code></p>
 */

public class EventBusEventDispatcher implements AsyncEventDispatcher {
	/** Instance logger */
	protected final Logger log = LoggerFactory.getLogger(getClass());
	/** The dispatching event bus */
	protected AsyncEventBus eventBus = null;
	/** The executor driving the async bus */
	protected Executor executor = null;
	/** The registered event handlers (since EventBus won't tell us what they are) */
	protected final Set<Object> registered = new CopyOnWriteArraySet<Object>();
	
	
	/**
	 * Creates a new EventBusEventDispatcher
	 */
	public EventBusEventDispatcher() {
		
	}
	
	/**
	 * Dead event handler. Prints a warning message on dead events.
	 * @param deadEvent The dead event
	 */
	public void onDeadEvent(DeadEvent deadEvent) {
		log.warn("\n\t**************************\n\tDEAD EVENT\n\t{}\n\t**************************\n");
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.async.AsyncEventDispatcher#shutdown()
	 */
	@Override
	public void shutdown() {
		for(Iterator<Object> iter = registered.iterator(); iter.hasNext();) {			
			eventBus.unregister(iter.next());			
		}
		registered.clear();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.async.AsyncEventDispatcher#collectStats(net.opentsdb.stats.StatsCollector)
	 */
	@Override
	public void collectStats(StatsCollector collector) {
		// TODO Auto-generated method stub

	}
	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.async.AsyncEventDispatcher#initialize(java.util.Properties, java.util.concurrent.Executor, java.util.Collection)
	 */
	@Override
	public void initialize(Properties config, Executor executor, Collection<IEventHandler> handlers) {
		eventBus = new AsyncEventBus("AsyncEventDispatcher", executor);
		this.executor = executor;		
		eventBus.register(this);
		if(handlers!=null && !handlers.isEmpty()) {
			for(IEventHandler handler: handlers) {
				if(handler==null) continue;
				eventBus.register(handler);
				registered.add(handler);
			}
		}
		
	}


	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.async.IPublishEventDispatcher#publishDataPoint(java.lang.String, long, double, java.util.Map, byte[])
	 */
	@Override
	public void publishDataPoint(String metric, long timestamp, double value, Map<String, String> tags, byte[] tsuid) {
		eventBus.post(new TSDBPublishEvent().publishDataPoint(metric, timestamp, value, tags, tsuid));
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.async.IPublishEventDispatcher#publishDataPoint(java.lang.String, long, long, java.util.Map, byte[])
	 */
	@Override
	public void publishDataPoint(String metric, long timestamp, long value, Map<String, String> tags, byte[] tsuid) {
		eventBus.post(new TSDBPublishEvent().publishDataPoint(metric, timestamp, value, tags, tsuid));
	}




	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.async.ISearchEventDispatcher#executeQuery(net.opentsdb.search.SearchQuery, com.stumbleupon.async.Deferred)
	 */
	@Override
	public void executeQuery(SearchQuery searchQuery, Deferred<SearchQuery> toComplete) {
		TSDBSearchEvent searchEvent = new TSDBSearchEvent().executeQueryEvent(searchQuery, toComplete); 
		eventBus.post(searchEvent);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.async.ISearchEventDispatcher#indexAnnotation(net.opentsdb.meta.Annotation)
	 */
	@Override
	public void indexAnnotation(Annotation annotation) {
		eventBus.post(new TSDBSearchEvent().indexAnnotation(annotation));
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.async.ISearchEventDispatcher#deleteAnnotation(net.opentsdb.meta.Annotation)
	 */
	@Override
	public void deleteAnnotation(Annotation annotation) {
		eventBus.post(new TSDBSearchEvent().deleteAnnotation(annotation));

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.async.ISearchEventDispatcher#indexTSMeta(net.opentsdb.meta.TSMeta)
	 */
	@Override
	public void indexTSMeta(TSMeta tsMeta) {
		eventBus.post(new TSDBSearchEvent().indexTSMeta(tsMeta));

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.async.ISearchEventDispatcher#deleteTSMeta(java.lang.String)
	 */
	@Override
	public void deleteTSMeta(String tsMeta) {
		eventBus.post(new TSDBSearchEvent().deleteTSMeta(tsMeta));

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.async.ISearchEventDispatcher#indexUIDMeta(net.opentsdb.meta.UIDMeta)
	 */
	@Override
	public void indexUIDMeta(UIDMeta uidMeta) {
		eventBus.post(new TSDBSearchEvent().indexUIDMeta(uidMeta));

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.async.ISearchEventDispatcher#deleteUIDMeta(net.opentsdb.meta.UIDMeta)
	 */
	@Override
	public void deleteUIDMeta(UIDMeta uidMeta) {
		eventBus.post(new TSDBSearchEvent().deleteUIDMeta(uidMeta));

	}

}
