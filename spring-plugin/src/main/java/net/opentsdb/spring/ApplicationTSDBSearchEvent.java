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
package net.opentsdb.spring;

import java.util.Map;

import net.opentsdb.meta.Annotation;
import net.opentsdb.meta.TSMeta;
import net.opentsdb.meta.UIDMeta;
import net.opentsdb.search.SearchQuery;

import org.helios.tsdb.plugins.event.TSDBEvent;
import org.helios.tsdb.plugins.event.TSDBPublishEvent;
import org.helios.tsdb.plugins.event.TSDBSearchEvent;

import com.stumbleupon.async.Deferred;

/**
 * <p>Title: ApplicationTSDBSearchEvent</p>
 * <p>Description: A spring application event wrapper for {@link TSDBSearchEvent}s</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>net.opentsdb.spring.ApplicationTSDBSearchEvent</code></p>
 */

public class ApplicationTSDBSearchEvent extends ApplicationTSDBEvent {

	/**
	 * Creates a new ApplicationTSDBSearchEvent
	 * @param tsdbEvent The event to wrap
	 */
	public ApplicationTSDBSearchEvent(TSDBSearchEvent tsdbEvent) {
		super(tsdbEvent);
	}
	
	
	/**
	 * Deletes an annotation
	 * @param annotation the annotation to delete
	 * @return the evented invocation
	 */
	public static ApplicationTSDBSearchEvent deleteAnnotation(Annotation annotation) {
		return new ApplicationTSDBSearchEvent(new TSDBSearchEvent().deleteAnnotation(annotation));
	}
	
	/**
	 * Indexes an annotation
	 * @param annotation the annotation to index
	 * @return the evented invocation
	 */
	public static ApplicationTSDBSearchEvent indexAnnotation(Annotation annotation) {
		return new ApplicationTSDBSearchEvent(new TSDBSearchEvent().indexAnnotation(annotation));
	}

	/**
	 * Deletes a TSMeta
	 * @param tsuid the id of the TSMeta to delete
	 * @return the evented invocation
	 */
	public static ApplicationTSDBSearchEvent deleteTSMeta(String tsuid) {
		return new ApplicationTSDBSearchEvent(new TSDBSearchEvent().deleteTSMeta(tsuid));
	}
	
	/**
	 * Indexes a TSMeta
	 * @param tsMeta the TSMeta to index
	 * @return the evented invocation
	 */
	public static ApplicationTSDBSearchEvent indexTSMeta(TSMeta tsMeta) {
		return new ApplicationTSDBSearchEvent(new TSDBSearchEvent().indexTSMeta(tsMeta));
	}
	
	/**
	 * Deletes a UIDMeta
	 * @param uidMeta the UIDMeta to delete
	 * @return the evented invocation
	 */
	public static ApplicationTSDBSearchEvent deleteUIDMeta(UIDMeta uidMeta) {
		return new ApplicationTSDBSearchEvent(new TSDBSearchEvent().deleteUIDMeta(uidMeta));
	}
	
	/**
	 * Indexes a UIDMeta
	 * @param uidMeta the UIDMeta to index
	 * @return the evented invocation
	 */
	public static ApplicationTSDBSearchEvent indexUIDMeta(UIDMeta uidMeta) {
		return new ApplicationTSDBSearchEvent(new TSDBSearchEvent().indexUIDMeta(uidMeta));
	}
	
	/**
	 * Executes an asynchronous query
	 * @param searchQuery The query to execute
	 * @param toComplete The deferred asynchronous result
	 * @return the evented invocation
	 */
	public static ApplicationTSDBSearchEvent executeQueryEvent(SearchQuery searchQuery, Deferred<SearchQuery> toComplete) {
		return new ApplicationTSDBSearchEvent(new TSDBSearchEvent().executeQueryEvent(searchQuery, toComplete));
	}
	

}
