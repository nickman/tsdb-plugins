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

import java.util.Map;

import net.opentsdb.meta.Annotation;
import net.opentsdb.meta.TSMeta;
import net.opentsdb.meta.UIDMeta;
import net.opentsdb.search.SearchQuery;

import com.lmax.disruptor.EventFactory;
import com.stumbleupon.async.Deferred;

/**
 * <p>Title: TSDBEvent</p>
 * <p>Description: Encapsulates an OpenTSDB callback to a plugin</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.event.TSDBEvent</code></p>
 */

public class TSDBEvent  {
	/** The event type */
	public TSDBEventType eventType;
	/** The metric name for data point publications */
	public String metric = null;
	/** The timestamp for data point publications */
	public long timestamp = -1;
	/** The value for long based data point publications */
	public long longValue = -1;
	/** The value for double based data point publications */
	public double doubleValue = -1;
	/** The annotation to index for annotation indexing or deletion events */
	public Annotation annotation = null;
	
	/** The datapoint tags for data point publications */
	public Map<String,String> tags = null;
	/** The tsuid in byte format for data point publications */
	public byte[] tsuidBytes = null;
	/** The tsuid in string format for tsuid deletion events */
	public String tsuid = null;
	/** The UIDMeta for UIDMeta indexing and deletion events */
	public UIDMeta uidMeta = null;
	/** The TSMeta for TSMeta indexing events */
	public TSMeta tsMeta = null;
	/** The search query for search events */
	public SearchQuery searchQuery = null;
	/** The deferred search query result for search events */
	public Deferred<SearchQuery> deferred;
	
	/**
	 * Creates a new TSDBEvent
	 */
	protected TSDBEvent() {

	}
	
	/**
	 * Returns this event instance as a search event
	 * @return this event instance as a search event
	 */
	public TSDBSearchEvent asSearchEvent() {
		if(!eventType.isForSearch()) {
			throw new RuntimeException("Cannot cast this event to Search. Event Type is [" + eventType + "]");
		}
		return (TSDBSearchEvent)this;
	}
	
	/**
	 * Nulls out all the fields.
	 */
	public void reset() {
		eventType = null;
		metric = null;
		timestamp = -1;
		longValue = -1;
		doubleValue = -1;
		annotation = null;
		tags = null;
		tsuidBytes = null;
		tsuid = null;
		uidMeta = null;
		tsMeta = null;
		searchQuery = null;
	}
	
	 /** The event factory for TSDBEvents */
	public final static EventFactory<TSDBEvent> EVENT_FACTORY = new EventFactory<TSDBEvent>() {
		 @Override
		public TSDBEvent newInstance() {
			 return new TSDBEvent();
		 }
	 };
	 
	/**
	 * Loads this event for a search event
	 * @param searchQuery The search query to translate
	 * @param toComplete The deferred to complete when the query execution completes (or errors out)
	 */
	public void executeQuery(SearchQuery searchQuery, Deferred<SearchQuery> toComplete) {
		 this.eventType = TSDBEventType.SEARCH;
		 this.searchQuery = searchQuery;
		 this.deferred = toComplete;
	 }
	 
	
	/**
	 * Loads this event for an annotation deletion
	 * @param annotation The annotation to delete
	 * @return the loaded event
	 */
	public TSDBEvent deleteAnnotation(Annotation annotation) {
		this.eventType = TSDBEventType.ANNOTATION_DELETE;
		this.annotation = annotation;
		return this;
	}

	/**
	 * Loads this event for an annotation indexing
	 * @param annotation The annotation to index
	 * @return the loaded event
	 */
	public TSDBEvent indexAnnotation(Annotation annotation) {
		this.eventType = TSDBEventType.ANNOTATION_INDEX;
		this.annotation = annotation;
		return this;
	}

	/**
	 * Loads this event for a TSMeta deletion
	 * @param tsuid The tsuid name to delete
	 * @return the loaded event
	 */
	public TSDBEvent deleteTSMeta(String tsuid) {
		this.eventType = TSDBEventType.TSMETA_DELETE;
		this.tsuid = tsuid;	
		return this;
	}
	
	/**
	 * Loads this event for a TSMeta indexing
	 * @param tsMeta The tsuid to index
	 * @return the loaded event
	 */
	public TSDBEvent indexTSMeta(TSMeta tsMeta) {
		this.eventType = TSDBEventType.TSMETA_INDEX;
		this.tsMeta = tsMeta;
		return this;
	}
	
	/**
	 * Loads this event for a UIDMeta deletion
	 * @param uidMeta The UIDMeta to delete
	 * @return the loaded event
	 */
	public TSDBEvent deleteUIDMeta(UIDMeta uidMeta) {
		this.eventType = TSDBEventType.UIDMETA_DELETE;
		this.uidMeta = uidMeta;	
		return this;
	}
	
	/**
	 * Loads this event for a UIDMeta indexing
	 * @param uidMeta The UIDMeta to index
	 * @return the loaded event
	 */
	public TSDBEvent indexUIDMeta(UIDMeta uidMeta) {
		this.eventType = TSDBEventType.UIDMETA_INDEX;
		this.uidMeta = uidMeta;	
		return this;
	}
	
	/**
	 * Loads this event for a double value data point publication
	 * @param metric The name of the metric associated with the data point
	 * @param timestamp Timestamp as a Unix epoch in seconds or milliseconds (depending on the TSD's configuration)
	 * @param value Value for the data point
	 * @param tags The metric tags
	 * @param tsuid Time series UID for the value
	 * @return the loaded event
	 */
	public TSDBEvent publishDataPoint(String metric, long timestamp, double value, Map<String,String> tags, byte[] tsuid) {
		this.eventType = TSDBEventType.DPOINT_DOUBLE;
		this.metric = metric;
		this.timestamp = timestamp;
		this.doubleValue = value;
		this.tags = tags;
		this.tsuidBytes = tsuid;
		return this;
	}
	
	/**
	 * Loads this event for a long value data point publication
	 * @param metric The name of the metric associated with the data point
	 * @param timestamp Timestamp as a Unix epoch in seconds or milliseconds (depending on the TSD's configuration)
	 * @param value Value for the data point
	 * @param tags The metric tags
	 * @param tsuid Time series UID for the value
	 * @return the loaded event
	 */
	public TSDBEvent publishDataPoint(String metric, long timestamp, long value, Map<String,String> tags, byte[] tsuid) {
		this.eventType = TSDBEventType.DPOINT_LONG;
		this.metric = metric;
		this.timestamp = timestamp;
		this.longValue = value;
		this.tags = tags;
		this.tsuidBytes = tsuid;
		return this;
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		if(eventType==null) return "Empty TSDBEvent";
		StringBuilder b = new StringBuilder("TSDBEvent ").append(eventType.name()).append("[");
		switch(eventType) {
		case ANNOTATION_DELETE: 
		case ANNOTATION_INDEX:
			b.append(annotation);
			break;
		case DPOINT_DOUBLE:
			b.append(doubleValue);
			break;
		case DPOINT_LONG:
			b.append(longValue);
			break;
		case SEARCH:
			b.append(searchQuery);
			break;
		case TSMETA_DELETE:
			b.append(tsuid);
			break;
		case TSMETA_INDEX:
			b.append(tsMeta);
			break;			
		case UIDMETA_DELETE:
		case UIDMETA_INDEX:
			b.append(uidMeta);
			break;
		default:
			break;			
		}
		return b.append("]").toString();
	}


}
