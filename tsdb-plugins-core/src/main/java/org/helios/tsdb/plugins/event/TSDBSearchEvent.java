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
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.event.TSDBEvent#deleteAnnotation(net.opentsdb.meta.Annotation)
	 */
	@Override
	public TSDBSearchEvent deleteAnnotation(Annotation annotation) {
		return new TSDBSearchEvent().deleteAnnotation(annotation);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.event.TSDBEvent#indexAnnotation(net.opentsdb.meta.Annotation)
	 */
	@Override
	public TSDBSearchEvent indexAnnotation(Annotation annotation) {
		return new TSDBSearchEvent().indexAnnotation(annotation);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.event.TSDBEvent#deleteTSMeta(java.lang.String)
	 */
	@Override
	public TSDBSearchEvent deleteTSMeta(String tsuid) {
		return new TSDBSearchEvent().deleteTSMeta(tsuid);
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.event.TSDBEvent#indexTSMeta(net.opentsdb.meta.TSMeta)
	 */
	@Override
	public TSDBSearchEvent indexTSMeta(TSMeta tsMeta) {
		return new TSDBSearchEvent().indexTSMeta(tsMeta);
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.event.TSDBEvent#deleteUIDMeta(net.opentsdb.meta.UIDMeta)
	 */
	@Override
	public TSDBSearchEvent deleteUIDMeta(UIDMeta uidMeta) {
		return new TSDBSearchEvent().deleteUIDMeta(uidMeta);
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.event.TSDBEvent#indexUIDMeta(net.opentsdb.meta.UIDMeta)
	 */
	@Override
	public TSDBSearchEvent indexUIDMeta(UIDMeta uidMeta) {
		return new TSDBSearchEvent().indexUIDMeta(uidMeta);
	}
	
	/**
	 * Prepares and returns a search event
	 * @param searchQuery The query to create an event for
	 * @return the loaded event
	 */
	public TSDBSearchEvent executeQueryEvent(SearchQuery searchQuery) {
		 this.deferred = super.executeQuery(searchQuery);
		 return this;
    }

	

}