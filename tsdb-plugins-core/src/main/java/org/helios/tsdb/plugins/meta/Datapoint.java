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
package org.helios.tsdb.plugins.meta;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import net.opentsdb.meta.api.NameUtil;

import org.helios.tsdb.plugins.event.TSDBEvent;
import org.helios.tsdb.plugins.event.TSDBEventType;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferFactory;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.buffer.DirectChannelBufferFactory;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;

/**
 * <p>Title: Datapoint</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.meta.Datapoint</code></p>
 */

public class Datapoint {
	/** The datapoint TSMeta metric name */
	protected final String metric;
	/** The datapoint fully qualified TSMeta name */
	protected final String fqn;
	/** The datapoint TSMeta tsuid */
	protected final String tsuid;
	/** The datapoint TSMeta tags */
	protected final Map<String, String> tags;
	/** The cummulative datapoint values */
	protected final Values values;
	/** The value type, true for double, false for long */
	protected final boolean doubleType;
	
	/** Initial size of the values buffer which is 5 pairs, 16 bytes each */
	public static final int DEFAULT_INITIAL_VSIZE = 5 * 8 * 2;
	/** The channel buffer factory */
	private static final ChannelBufferFactory factory = new DirectChannelBufferFactory(DEFAULT_INITIAL_VSIZE);
	/** The datapoint bitmask to validate the incoming TSDBEvent */
	public static final int DATAPOINT_BITMASK = TSDBEventType.getMask(TSDBEventType.DPOINT_DOUBLE, TSDBEventType.DPOINT_LONG);
	
	/**
	 * Creates a new Datapoint
	 * @param event The TSDBEvent to read the datapoint from 
	 */
	public Datapoint(final TSDBEvent event) {
		if(event==null) throw new IllegalArgumentException("The passed event was null");
		if(!event.eventType.isEnabled(DATAPOINT_BITMASK)) throw new IllegalArgumentException("Invalid TSDBEventType [" + event.eventType + "]");
		metric = event.metric;
		tsuid = event.tsuid;
		tags = new LinkedHashMap<String, String>(event.tags);
		if(event.eventType==TSDBEventType.DPOINT_DOUBLE) {
			doubleType = true;
			values = new Values(true).addValue(event.timestamp, event.doubleValue);
		} else {
			doubleType = false;
			values = new Values(false).addValue(event.timestamp, event.longValue);
		}
		fqn = NameUtil.buildObjectName(this.metric, this.tags).toString();
	}
	
	/**
	 * Appends the value and timestamp from the passed event to this Datapoint
	 * @param event the event to process
	 */
	public void apply(final TSDBEvent event) {
		if(event==null) return;
		if(event.tsuid.equals(this.tsuid)) {
			if(event.eventType==TSDBEventType.DPOINT_DOUBLE && this.doubleType) {
				values.addValue(event.timestamp, event.doubleValue);				
			} else if(event.eventType==TSDBEventType.DPOINT_LONG && !this.doubleType) {
				values.addValue(event.timestamp, event.longValue);
			}			
		}
	}
	
	
	/**
	 * Aggregates the values of another datapoint into this one
	 * @param d The other datapoint
	 */
	public void apply(final Datapoint d) {
		if(d==null) return;
		if(d.fqn.equals(this.fqn) && d.doubleType==doubleType) {
			d.values.apply(d.values);
		}
	}
	
	/**
	 * <p>Title: Values</p>
	 * <p>Description: A container for multiple datapoint values</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.tsdb.plugins.meta.Datapoint.Values</code></p>
	 */
	protected class Values {
		/** true for double values, false for long values */
		protected final boolean doubleType;
		/** The number of values (a timestamp and a value) */
		private int count = 0;
		/** The buffer containing the value pairs */
		protected final ChannelBuffer valuePairs = ChannelBuffers.dynamicBuffer(factory);
		
		
		
		/**
		 * Creates a new Values
		 * @param isDouble true for double values, false for long values
		 */
		Values(boolean isDouble) {
			doubleType = isDouble;
		}
		
		/**
		 * Returns the number of value pairs
		 * @return the number of value pairs
		 */
		synchronized int getCount() {
			return count;
		}
		
		/**
		 * Writes a new value pair into the value buffer and increments the count
		 * @param timestamp The timestamp of the value
		 * @param value The value
		 * @return this Values
		 */
		synchronized Values addValue(final long timestamp, final double value) {
			count++;
			valuePairs.writeLong(timestamp);
			valuePairs.writeDouble(value);
			return this;
		}
		
		synchronized Values apply(final Values otherValues) {
			if(otherValues==this) return this;			
			valuePairs.setIndex(0, count*2*8);
			valuePairs.writeBytes(otherValues.valuePairs, otherValues.count*2*8);
			count += otherValues.count;
			return this;
			
		}
		
		/**
		 * Writes a new value pair into the value buffer and increments the count
		 * @param timestamp The timestamp of the value
		 * @param value The value
		 * @return this Values
		 */
		synchronized Values addValue(final long timestamp, final long value) {
			count++;
			valuePairs.writeLong(timestamp);
			valuePairs.writeLong(value);
			return this;
		}
		
		/**
		 * JSON serialization routine for values
		 * @param jgen The json generator to write to
		 * @throws IOException  thrown on any IO error
		 * @throws JsonGenerationException thrown on any JSON error
		 */
		synchronized void serializeToJson(final JsonGenerator jgen) throws JsonGenerationException, IOException {
			jgen.writeObjectFieldStart("values");
			for(int i = 0; i < count; i++) {
				
				jgen.writeNumberField(
						"" + (valuePairs.getLong(i * 8)), 
						doubleType ? 
								valuePairs.getDouble(i * 8 + 8) :
								valuePairs.getLong(i * 8 + 8)
				);				
			}
			jgen.writeEndObject();
		}		
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((tsuid == null) ? 0 : tsuid.hashCode());
		return result;
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Datapoint other = (Datapoint) obj;
		if (tsuid == null) {
			if (other.tsuid != null)
				return false;
		} else if (!tsuid.equals(other.tsuid))
			return false;
		return true;
	}
	
	/**
	 * Returns the number of value pairs in this Datapoint
	 * @return the number of value pairs 
	 */
	public int getValueCount() {
		return values.getCount();
	}
	
	/**
	 * Returns the value timestamp at the specified index
	 * @param index The index of the timestamp to get
	 * @return the timestamp
	 */
	public long getTimestamp(final int index) {
		return values.valuePairs.getLong(index * 8);
	}
	
	/**
	 * Returns the long value at the specified index
	 * @param index The index of the long value to get
	 * @return the long value
	 */
	public long getLongValue(final int index) {
		if(doubleType) throw new RuntimeException("This Datapoint does not contain long values");
		return values.valuePairs.getLong(index * 8 + 8);
	}
	
	/**
	 * Returns the double value at the specified index
	 * @param index The index of the double value to get
	 * @return the double value
	 */
	public double getDoubleValue(final int index) {
		if(!doubleType) throw new RuntimeException("This Datapoint does not contain double values");
		return values.valuePairs.getDouble(index * 8 + 8);
	}
	
	/**
	 * Returns the value at the specified index
	 * @param index The index of the value to get
	 * @return the value
	 */
	public Number getValue(final int index) {
		return doubleType ? getDoubleValue(index) : getLongValue(index);
	}
	
	/**
	 * Returns all the values in a map keyed by the timestamp
	 * @return all the values in a map keyed by the timestamp
	 */
	public Map<Long, Number> getValues() {
		Map<Long, Number> map = new TreeMap<Long, Number>();
		final int cnt = values.count;
		for(int i = 0; i < cnt; i++) {
			map.put(getTimestamp(i), getValue(i));
		}
		return map;
	}

	/**
	 * Returns the TSMeta metric name
	 * @return the metric
	 */
	public String getMetric() {
		return metric;
	}

	/**
	 * Returns the TSMeta fully qualified name
	 * @return the fqn
	 */
	public String getFqn() {
		return fqn;
	}

	/**
	 * Returns the TSMeta TSUID
	 * @return the tsuid
	 */
	public String getTsuid() {
		return tsuid;
	}

	/**
	 * Returns a read only copy of the tags 
	 * @return the tags
	 */
	public Map<String, String> getTags() {
		return Collections.unmodifiableMap(tags);
	}

	/**
	 * Indicates if the values are doubles
	 * @return true if the values are doubles, false if they are longs
	 */
	public boolean isDoubleType() {
		return doubleType;
	}

	/**
	 * Indicates if the values are longs
	 * @return true if the values are longs, false if they are doubles
	 */
	public boolean isLongType() {
		return !doubleType;
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Datapoint [");
		builder.append("fqn:");
		builder.append(fqn);
		builder.append(", ");
		builder.append("tsuid:");
		builder.append(tsuid);
		builder.append(", ");
		builder.append("type: ");
		builder.append(doubleType ? "double" : "long");
		builder.append(", ");
		builder.append("vcount: ");
		builder.append(values.getCount());
		builder.append("]");
		return builder.toString();
	}
	
	

}
