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
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

import net.opentsdb.core.TSDB;
import net.opentsdb.meta.Annotation;
import net.opentsdb.meta.TSMeta;
import net.opentsdb.meta.UIDMeta;
import net.opentsdb.search.SearchQuery;
import net.opentsdb.stats.StatsCollector;

import org.helios.tsdb.plugins.event.TSDBEvent;
import org.helios.tsdb.plugins.event.TSDBSearchEvent;
import org.helios.tsdb.plugins.handlers.IEventHandler;

import com.google.common.eventbus.AsyncEventBus;
import com.stumbleupon.async.Deferred;

/**
 * <p>Title: EventBusEventDispatcher</p>
 * <p>Description: An async event dispatcher that uses a Guava {@link AsyncEventBus} to manage async dispatches.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.async.EventBusEventDispatcher</code></p>
 */

public class EventBusEventDispatcher implements AsyncEventDispatcher {
	/** The dispatching event bus */
	protected AsyncEventBus eventBus = null;
	/** The executor driving the async bus */
	protected Executor executor = null;
	/** The shared TSDB instance */
	protected TSDB tsdb = null;
	
	
	/**
	 * Creates a new EventBusEventDispatcher
	 */
	public EventBusEventDispatcher() {

	}
	
	/**	public static final String ASYNC_DISPATCHER = "helios.events
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.handlers.IEventHandler#shutdown()
	 */
	@Override
	public void shutdown() {

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.handlers.IEventHandler#collectStats(net.opentsdb.stats.StatsCollector)
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
		if(handlers!=null && !handlers.isEmpty()) {
			for(IEventHandler handler: handlers) {
				if(handler==null) continue;
				eventBus.register(handler);
			}
		}
		
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.handlers.IEventHandler#initialize(net.opentsdb.core.TSDB, java.util.Properties)
	 */
	@Override
	public void initialize(TSDB tsdb, Properties extracted) {
		this.tsdb = tsdb;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.handlers.IPublishEventHandler#publishDataPoint(java.lang.String, long, double, java.util.Map, byte[])
	 */
	@Override
	public void publishDataPoint(String metric, long timestamp, double value, Map<String, String> tags, byte[] tsuid) {
		eventBus.post(new TSDBSearchEvent().publishDataPoint(metric, timestamp, value, tags, tsuid));
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.handlers.IPublishEventHandler#publishDataPoint(java.lang.String, long, long, java.util.Map, byte[])
	 */
	@Override
	public void publishDataPoint(String metric, long timestamp, long value, Map<String, String> tags, byte[] tsuid) {
		eventBus.post(new TSDBSearchEvent().publishDataPoint(metric, timestamp, value, tags, tsuid));
	}




	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.handlers.ISearchEventHandler#executeQuery(net.opentsdb.search.SearchQuery)
	 */
	@Override
	public Deferred<SearchQuery> executeQuery(SearchQuery searchQuery) {
		TSDBSearchEvent searchEvent = new TSDBSearchEvent().executeQueryEvent(searchQuery); 
		eventBus.post(searchEvent);
		return searchEvent.deferred;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.handlers.ISearchEventHandler#indexAnnotation(net.opentsdb.meta.Annotation)
	 */
	@Override
	public void indexAnnotation(Annotation annotation) {
		eventBus.post(new TSDBSearchEvent().indexAnnotation(annotation));
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.handlers.ISearchEventHandler#deleteAnnotation(net.opentsdb.meta.Annotation)
	 */
	@Override
	public void deleteAnnotation(Annotation annotation) {
		eventBus.post(new TSDBSearchEvent().deleteAnnotation(annotation));

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.handlers.ISearchEventHandler#indexTSMeta(net.opentsdb.meta.TSMeta)
	 */
	@Override
	public void indexTSMeta(TSMeta tsMeta) {
		eventBus.post(new TSDBSearchEvent().indexTSMeta(tsMeta));

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.handlers.ISearchEventHandler#deleteTSMeta(java.lang.String)
	 */
	@Override
	public void deleteTSMeta(String tsMeta) {
		eventBus.post(new TSDBSearchEvent().deleteTSMeta(tsMeta));

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.handlers.ISearchEventHandler#indexUIDMeta(net.opentsdb.meta.UIDMeta)
	 */
	@Override
	public void indexUIDMeta(UIDMeta uidMeta) {
		eventBus.post(new TSDBSearchEvent().indexUIDMeta(uidMeta));

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.handlers.ISearchEventHandler#deleteUIDMeta(net.opentsdb.meta.UIDMeta)
	 */
	@Override
	public void deleteUIDMeta(UIDMeta uidMeta) {
		eventBus.post(new TSDBSearchEvent().deleteUIDMeta(uidMeta));

	}

}
