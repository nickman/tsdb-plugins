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
package org.helios.tsdb.plugins.event;

import com.stumbleupon.async.Deferred;

import net.opentsdb.meta.Annotation;
import net.opentsdb.meta.TSMeta;
import net.opentsdb.meta.UIDMeta;
import net.opentsdb.search.SearchQuery;

/**
 * <p>Title: TSDBSearchEvent</p>
 * <p>Description: Type specific spoofing for strongly typed async dispatchers like Guava EventBus.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.event.TSDBEvent.TSDBSearchEvent</code></p>
 */
public class TSDBSearchEvent extends TSDBEvent {
	/**
	 * Creates a new TSDBSearchEvent
	 */
	public TSDBSearchEvent() {
		super();
	}
	
	
	/**
	 * Creates a new TSDBSearchEvent from an event
	 * @param event the event to copy from
	 */
	public TSDBSearchEvent(TSDBEvent event) {
		annotation = event.annotation;
		deferred = event.deferred;
		doubleValue = event.doubleValue;		
		eventType = event.eventType;
		longValue = event.longValue;		
		metric = event.metric;
		searchQuery = event.searchQuery;
		tags = event.tags;		
		timestamp = event.timestamp;
		tsMeta = event.tsMeta;
		tsuid = event.tsuid;
		tsuidBytes = event.tsuidBytes;
		uidMeta = event.uidMeta;
	}
	
	

	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.event.TSDBEvent#deleteAnnotation(net.opentsdb.meta.Annotation)
	 */
	@Override
	public TSDBSearchEvent deleteAnnotation(Annotation annotation) {
		super.deleteAnnotation(annotation);
		return this;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.event.TSDBEvent#indexAnnotation(net.opentsdb.meta.Annotation)
	 */
	@Override
	public TSDBSearchEvent indexAnnotation(Annotation annotation) {
		super.indexAnnotation(annotation);
		return this;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.event.TSDBEvent#deleteTSMeta(java.lang.String)
	 */
	@Override
	public TSDBSearchEvent deleteTSMeta(String tsuid) {
		super.deleteTSMeta(tsuid);
		return this;
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.event.TSDBEvent#indexTSMeta(net.opentsdb.meta.TSMeta)
	 */
	@Override
	public TSDBSearchEvent indexTSMeta(TSMeta tsMeta) {
		super.indexTSMeta(tsMeta);
		return this;
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.event.TSDBEvent#deleteUIDMeta(net.opentsdb.meta.UIDMeta)
	 */
	@Override
	public TSDBSearchEvent deleteUIDMeta(UIDMeta uidMeta) {
		super.deleteUIDMeta(uidMeta);
		return this;
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.event.TSDBEvent#indexUIDMeta(net.opentsdb.meta.UIDMeta)
	 */
	@Override
	public TSDBSearchEvent indexUIDMeta(UIDMeta uidMeta) {
		super.indexUIDMeta(uidMeta);
		return this;
	}
	
	/**
	 * Prepares and returns a search event
	 * @param searchQuery The query to create an event for
	 * @param toComplete The deferred to complete when the query execution completes (or errors out)
	 * @return the loaded event
	 */
	public TSDBSearchEvent executeQueryEvent(SearchQuery searchQuery, Deferred<SearchQuery> toComplete) {
		 super.executeQuery(searchQuery, toComplete);
		 return this;
    }

	

}