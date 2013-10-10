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
import net.opentsdb.stats.StatsCollector;

import com.lmax.disruptor.EventFactory;
import com.stumbleupon.async.Deferred;

/**
 * <p>Title: TSDBEvent</p>
 * <p>Description: Encapsulates an OpenTSDB callback to a plugin</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.asynch.TSDBEvent</code></p>
 */

public class TSDBEvent  {
	/** The event type */
	public TSDBEventType eventType;
	/** The stats collector for stats collection events */
	public StatsCollector collector;
	/** The metric name for data point publications */
	public String metric = null;
	/** The timestamp for data point publications */
	public long timestamp = -1;
	/** The value for long based data point publications */
	public long longValue = -1;
	/** The value for double based data point publications */
	public double doubleValue = -1;
	/** The value for raw data point publications */
	public byte[] rawValue = null;
	/** The value type indicator for raw data point publications */
	public short rawValueType = -1;
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
	private TSDBEvent() {

	}
	
	/**
	 * Nulls out all the fields.
	 */
	public void reset() {
		eventType = null;
		collector = null;
		metric = null;
		timestamp = -1;
		longValue = -1;
		doubleValue = -1;
		rawValue = null;
		rawValueType = -1;
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
		 public TSDBEvent newInstance() {
			 return new TSDBEvent();
		 }
	 };
	 
	/**
	 * Loads this event for a search event
	 * @param searchQuery The search query to translate
	 * @return a deferred search query
	 */
	public Deferred<SearchQuery> executeQuery(SearchQuery searchQuery) {
		 this.eventType = TSDBEventType.SEARCH;
		 this.searchQuery = searchQuery;
		 this.deferred = new Deferred<SearchQuery>();
		 return this.deferred;
	 }
	 
	/**
	 * Loads this event for a stats collection
	 * @param collector The collector stats should be written into
	 */
	public void collectStats(StatsCollector collector) {
		 this.eventType = TSDBEventType.STATS_COLLECT;
		 this.collector = collector;
	}
	
	/**
	 * Loads this event for an annotation deletion
	 * @param annotation The annotation to delete
	 */
	public void deleteAnnotation(Annotation annotation) {
		this.eventType = TSDBEventType.ANNOTATION_DELETE;
		this.annotation = annotation;
	}

	/**
	 * Loads this event for an annotation indexing
	 * @param annotation The annotation to index
	 */
	public void indexAnnotation(Annotation annotation) {
		this.eventType = TSDBEventType.ANNOTATION_INDEX;
		this.annotation = annotation;
	}

	/**
	 * Loads this event for a TSMeta deletion
	 * @param tsuid The tsuid name to delete
	 */
	public void deleteTSMeta(String tsuid) {
		this.eventType = TSDBEventType.TSMETA_DELETE;
		this.tsuid = tsuid;		
	}
	
	/**
	 * Loads this event for a TSMeta indexing
	 * @param tsMeta The tsuid to index
	 */
	public void indexTSMeta(TSMeta tsMeta) {
		this.eventType = TSDBEventType.TSMETA_INDEX;
		this.tsMeta = tsMeta;		
	}
	
	/**
	 * Loads this event for a UIDMeta deletion
	 * @param uidMeta The UIDMeta to delete
	 */
	public void deleteUIDMeta(UIDMeta uidMeta) {
		this.eventType = TSDBEventType.UIDMETA_DELETE;
		this.uidMeta = uidMeta;		
	}
	
	/**
	 * Loads this event for a UIDMeta indexing
	 * @param uidMeta The UIDMeta to index
	 */
	public void indexUIDMeta(UIDMeta uidMeta) {
		this.eventType = TSDBEventType.UIDMETA_INDEX;
		this.uidMeta = uidMeta;		
	}
	
	/**
	 * Loads this event for a double value data point publication
	 * @param metric The name of the metric associated with the data point
	 * @param timestamp Timestamp as a Unix epoch in seconds or milliseconds (depending on the TSD's configuration)
	 * @param value Value for the data point
	 * @param tags The metric tags
	 * @param tsuid Time series UID for the value
	 */
	public void publishDataPoint(String metric, long timestamp, double value, Map<String,String> tags, byte[] tsuid) {
		this.eventType = TSDBEventType.DPOINT_DOUBLE;
		this.metric = metric;
		this.timestamp = timestamp;
		this.doubleValue = value;
		this.tags = tags;
		this.tsuidBytes = tsuid;
	}
	
	/**
	 * Loads this event for a long value data point publication
	 * @param metric The name of the metric associated with the data point
	 * @param timestamp Timestamp as a Unix epoch in seconds or milliseconds (depending on the TSD's configuration)
	 * @param value Value for the data point
	 * @param tags The metric tags
	 * @param tsuid Time series UID for the value
	 */
	public void publishDataPoint(String metric, long timestamp, long value, Map<String,String> tags, byte[] tsuid) {
		this.eventType = TSDBEventType.DPOINT_LONG;
		this.metric = metric;
		this.timestamp = timestamp;
		this.longValue = value;
		this.tags = tags;
		this.tsuidBytes = tsuid;
	}
	
	/**
	 * Loads this event for a raw value data point publication
	 * @param metric The name of the metric associated with the data point
	 * @param timestamp Timestamp as a Unix epoch in seconds or milliseconds (depending on the TSD's configuration)
	 * @param value Value for the data point
	 * @param tags The metric tags
	 * @param tsuid Time series UID for the value
	 * @param flags Indicates if the byte array is an integer or floating point value 
	 */
	public void publishDataPoint(String metric, long timestamp, byte[] value, Map<String,String> tags, byte[] tsuid, short flags) {
		this.eventType = TSDBEventType.DPOINT_LONG;
		this.metric = metric;
		this.timestamp = timestamp;
		this.rawValue = value;
		this.tags = tags;
		this.tsuidBytes = tsuid;
		this.rawValueType = flags;
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
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
		case STATS_COLLECT:
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
