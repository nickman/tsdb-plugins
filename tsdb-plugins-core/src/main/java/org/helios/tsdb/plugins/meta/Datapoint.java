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
import java.util.LinkedHashMap;
import java.util.Map;

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
		protected int count = 0;
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
	 * Returns 
	 * @return the metric
	 */
	public final String getMetric() {
		return metric;
	}

	/**
	 * Returns 
	 * @return the fqn
	 */
	public final String getFqn() {
		return fqn;
	}

	/**
	 * Returns 
	 * @return the tsuid
	 */
	public final String getTsuid() {
		return tsuid;
	}

	/**
	 * Returns 
	 * @return the tags
	 */
	public final Map<String, String> getTags() {
		return tags;
	}

	/**
	 * Returns 
	 * @return the doubleType
	 */
	public final boolean isDoubleType() {
		return doubleType;
	}

}
