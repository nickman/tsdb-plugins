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
package net.opentsdb.catalog;

import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentSkipListSet;

import net.opentsdb.core.TSDB;
import net.opentsdb.search.SearchQuery;

import org.helios.tsdb.plugins.event.TSDBEvent;
import org.helios.tsdb.plugins.event.TSDBEventType;
import org.helios.tsdb.plugins.event.TSDBSearchEvent;
import org.helios.tsdb.plugins.handlers.EmptySearchEventHandler;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import com.stumbleupon.async.Deferred;

/**
 * <p>Title: TSDBCatalogSearchEventHandler</p>
 * <p>Description: TSDB search event handler for populating a SQL based data dictionary of metrics.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>net.opentsdb.catalog.TSDBCatalogSearchEventHandler</code></p>
 */

public class TSDBCatalogSearchEventHandler extends EmptySearchEventHandler {
	/** Map of the relative priority ordering of events keyed by the event type enum */
	public static final Map<TSDBEventType, Integer> EVENT_ORDERING;
	
	/** The processing queue for catalog processed events */
	protected final ConcurrentSkipListSet<TSDBSearchEvent> processingQueue = new ConcurrentSkipListSet<TSDBSearchEvent>(new TSDBSearchEventComparator()); 
	
	static {		
		Map<TSDBEventType, Integer> tmp = new EnumMap<TSDBEventType, Integer>(TSDBEventType.class);
		tmp.put(TSDBEventType.TSMETA_DELETE, 1);
		tmp.put(TSDBEventType.UIDMETA_DELETE, 2);
		tmp.put(TSDBEventType.UIDMETA_INDEX, 3);
		tmp.put(TSDBEventType.TSMETA_INDEX, 4);
		tmp.put(TSDBEventType.SEARCH, 5);		
		tmp.put(TSDBEventType.ANNOTATION_DELETE, 6);
		tmp.put(TSDBEventType.ANNOTATION_INDEX, 7);
		EVENT_ORDERING = Collections.unmodifiableMap(tmp);
	}
	
	/**
	 * <p>Title: TSDBSearchEventComparator</p>
	 * <p>Description: Comparator for {@link TSDBSearchEvent}s to enforce priority ordering in the event submission queue</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>net.opentsdb.catalog.TSDBCatalogSearchEventHandler.TSDBSearchEventComparator</code></p>
	 */
	public static class TSDBSearchEventComparator implements Comparator<TSDBSearchEvent> {
		/**
		 * {@inheritDoc}
		 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
		 */
		@Override
		public int compare(TSDBSearchEvent t1, TSDBSearchEvent t2) {
			int i1 = EVENT_ORDERING.get(t1.eventType);
			int i2 = EVENT_ORDERING.get(t2.eventType);
			return i1 < i2 ? -1 : 1;
		}
	}
	
	
	
	/**
	 * Creates a new TSDBCatalogSearchEventHandler
	 */
	public TSDBCatalogSearchEventHandler() {
		super();
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.handlers.EmptySearchEventHandler#initialize(net.opentsdb.core.TSDB, java.util.Properties)
	 */
	@Override
	public void initialize(TSDB tsdb, Properties extracted) {		
		super.initialize(tsdb, extracted);
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.handlers.EmptySearchEventHandler#onEvent(org.helios.tsdb.plugins.event.TSDBSearchEvent)
	 */
	@Subscribe
	@AllowConcurrentEvents		
	@Override
	public void onEvent(TSDBSearchEvent event) throws Exception {
		this.onEvent(event, -1L, false);
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.handlers.EmptySearchEventHandler#onEvent(org.helios.tsdb.plugins.event.TSDBEvent, long, boolean)
	 */
	@Override
	public void onEvent(TSDBEvent event, long sequence, boolean endOfBatch) throws Exception {
		if(!EVENT_ORDERING.containsKey(event.eventType)) return;
		if(TSDBEventType.SEARCH==event.eventType) {
			executeQuery(event.searchQuery, event.deferred);
		} else {
			processingQueue.add(event.asSearchEvent());
		}
	}
	
	
	public void run() {
		log.info("Starting Catalog Processing Thread");
	}
	
	
	   /**
     * Executes a search query and returns the deferred for the results
     * @param query The query to execute
     * @param result The deferred to write the query results into
     * @return the deferred results
     */
    public Deferred<SearchQuery> executeQuery(final SearchQuery query, final Deferred<SearchQuery> result) {
    	
    	return result;
    }
}
