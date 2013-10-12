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
import org.helios.tsdb.plugins.event.TSDBEventType;
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

public class EmptySearchEventHandler  extends AbstractTSDBEventHandler implements EventHandler<TSDBEvent> {
	/** The shared TSDB instance */
	protected TSDB tsdb = null;
	
	/**
	 * Creates a new EmptySearchEventHandler
	 */
	public EmptySearchEventHandler() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void onAsynchStart() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onAsynchShutdown() {
		// TODO Auto-generated method stub
		
	}

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
	 * @see org.helios.tsdb.plugins.handlers.AbstractTSDBEventHandler#start()
	 */
	@Override
	public void start() {
		
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.handlers.AbstractTSDBEventHandler#stop()
	 */
	@Override
	public void stop() {
		
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.handlers.AbstractTSDBEventHandler#configure(net.opentsdb.core.TSDB)
	 */
	@Override
	public void configure(TSDB tsdb) {
		this.tsdb = tsdb;
	}

	public void initialize(TSDB tsdb) {
		
	}

	public void shutdown() {
		
	}

	public void collectStats(StatsCollector collector) {
		
	}

	public void executeQuery(SearchQuery searchQuery, Deferred<SearchQuery> deferredResult) {
		
	}

	public void indexAnnotation(Annotation annotation) {
		
	}

	public void deleteAnnotation(Annotation annotation) {
		
	}

	public void indexTSMeta(TSMeta tsMeta) {
		
	}

	public void deleteTSMeta(String tsMeta) {
		
	}

	public void indexUIDMeta(UIDMeta uidMeta) {
		
	}

	public void deleteUIDMeta(UIDMeta uidMeta) {
		
	}

}
