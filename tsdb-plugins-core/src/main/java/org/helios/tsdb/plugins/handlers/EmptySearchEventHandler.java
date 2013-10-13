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

import net.opentsdb.core.TSDB;
import net.opentsdb.meta.Annotation;
import net.opentsdb.meta.TSMeta;
import net.opentsdb.meta.UIDMeta;
import net.opentsdb.search.SearchQuery;
import net.opentsdb.stats.StatsCollector;

import org.helios.tsdb.plugins.event.TSDBEvent;
import org.helios.tsdb.plugins.event.TSDBSearchEvent;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import com.lmax.disruptor.EventHandler;
import com.stumbleupon.async.Deferred;

/**
 * <p>Title: EmptySearchEventHandler</p>
 * <p>Description: Base class for implementing OpenTSDB {@link net.opentsdb.search.SearchPlugin} event handlers.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.handlers.EmptySearchEventHandler</code></p>
 */

public class EmptySearchEventHandler  extends AbstractTSDBEventHandler implements EventHandler<TSDBEvent>, ISearchEventHandler {
	

	/**
	 * {@inheritDoc}
	 * @see com.lmax.disruptor.EventHandler#onEvent(java.lang.Object, long, boolean)
	 */
	@Override
	public void onEvent(TSDBEvent event, long sequence, boolean endOfBatch) throws Exception {
		if(event.eventType==null || !event.eventType.isForSearch()) return;
		switch(event.eventType) {
		case ANNOTATION_DELETE:
			deleteAnnotation(event.annotation);
			break;
		case ANNOTATION_INDEX:
			indexAnnotation(event.annotation);
			break;
		case SEARCH:
			executeQuery(event.searchQuery, event.deferred);
			break;
		case TSMETA_DELETE:
			deleteTSMeta(event.tsuid);
			break;
		case TSMETA_INDEX:
			indexTSMeta(event.tsMeta);
			break;
		case UIDMETA_DELETE:
			deleteUIDMeta(event.uidMeta);
			break;
		case UIDMETA_INDEX:
			indexUIDMeta(event.uidMeta);
			break;
		default:
			//  ??  Programmer Error ?
			break;			
		}				
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
	 * @param searchQuery
	 * @param deferredResult
	 */
	public void executeQuery(SearchQuery searchQuery, Deferred<SearchQuery> deferredResult) {
		
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.handlers.ISearchEventHandler#indexAnnotation(net.opentsdb.meta.Annotation)
	 */
	@Override
	public void indexAnnotation(Annotation annotation) {
		
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.handlers.ISearchEventHandler#deleteAnnotation(net.opentsdb.meta.Annotation)
	 */
	@Override
	public void deleteAnnotation(Annotation annotation) {
		
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.handlers.ISearchEventHandler#indexTSMeta(net.opentsdb.meta.TSMeta)
	 */
	@Override
	public void indexTSMeta(TSMeta tsMeta) {
		
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.handlers.ISearchEventHandler#deleteTSMeta(java.lang.String)
	 */
	@Override
	public void deleteTSMeta(String tsMeta) {
		
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.handlers.ISearchEventHandler#indexUIDMeta(net.opentsdb.meta.UIDMeta)
	 */
	@Override
	public void indexUIDMeta(UIDMeta uidMeta) {
		
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.handlers.ISearchEventHandler#deleteUIDMeta(net.opentsdb.meta.UIDMeta)
	 */
	@Override
	public void deleteUIDMeta(UIDMeta uidMeta) {
		
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.handlers.ISearchEventHandler#executeQuery(net.opentsdb.search.SearchQuery)
	 */
	@Override
	public Deferred<SearchQuery> executeQuery(SearchQuery searchQuery) {
		return Deferred.fromResult(searchQuery);
	}

}
