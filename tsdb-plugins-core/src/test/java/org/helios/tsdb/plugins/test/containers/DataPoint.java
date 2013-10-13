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
package org.helios.tsdb.plugins.test.containers;

import java.util.LinkedHashMap;
import java.util.Map;

import net.opentsdb.core.TSDB;

/**
 * <p>Title: DataPoint</p>
 * <p>Description: Encapsulates a syntehtic data point for submission to the TSDB.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.test.containers.DataPoint</code></p>
 */

public abstract class DataPoint {
	/** The metric name, a non-empty string. */	
	public final String metricName;
	/** The metric tags. Must be non empty */
	public final Map<String, String> tags;
	/** The timestamp associated with the value. */
	public final long timestamp;
	
	/**
	 * Creates a new DataPoint
	 * @param metricName The metric name, a non-empty string.
	 * @param tags The metric tags. Must be non empty
	 * @param timestamp  The timestamp associated with the value.
	 */
	public DataPoint(String metricName, Map<String, String> tags, long timestamp) {
		super();
		this.metricName = metricName;
		this.tags = tags;
		this.timestamp = timestamp;
	}
	
	/**
	 * Creates a new DataPoint for the current time
	 * @param metricName The metric name, a non-empty string.
	 * @param tags The metric tags. Must be non empty
	 */
	public DataPoint(String metricName, Map<String, String> tags) {
		this.metricName = metricName;
		this.tags = tags;
		this.timestamp = System.currentTimeMillis();
	}
	
	/**
	 * Creates a new DataPoint for the current time
	 * @param metricName The metric name, a non-empty string.
	 * @param tags The metric tags. Must be a non empty, even numbered number of non empty strings
	 */
	public DataPoint(String metricName, String...tags) {
		if(tags==null || tags.length==0) throw new IllegalArgumentException("Passed tags array was null or empty");
		if(tags.length%2!=0) throw new IllegalArgumentException("Passed tags array had an odd number of tags");
		
		this.metricName = metricName;
		this.timestamp = System.currentTimeMillis();
		Map<String, String> map = new LinkedHashMap<String, String>();
		for(int i = 0; i < tags.length;) {
			map.put(tags[i++], tags[i++]);
		}
		this.tags = map;
	}
	
	
	
	/**
	 * Creates a new DataPoint
	 * @param value The value for this datapoint
	 * @param metricName The metric name, a non-empty string.
	 * @param tags The metric tags. Must be non empty
	 * @param timestamp  The timestamp associated with the value.
	 * @return A correctly typed data point in accordance with the size of the value
	 */
	public static DataPoint newDataPoint(Number value, String metricName, Map<String, String> tags, long timestamp) {
		// Long < Float < Double
		double v = value.doubleValue();
		if(v <= Long.MAX_VALUE) {
			return new LongDataPoint(value.longValue(), metricName, tags, timestamp);
		} else if(v <= Float.MAX_VALUE) {
			return new FloatDataPoint(value.floatValue(), metricName, tags, timestamp);
		} else {
			return new DoubleDataPoint(v, metricName, tags, timestamp);
		}
	}
	
	/**
	 * Publishes this datapoint the passed TSDB
	 * @param tsdb the TSDB to publish to
	 */
	public abstract void publish(TSDB tsdb);

	
	/**
	 * Generates a unique key for this data point
	 * @return a unique key for this data point
	 */
	public String getKey() {
		StringBuilder b = new StringBuilder(metricName);
		b.append("/").append(timestamp).append(":");
		for(Map.Entry<String, String> entry: tags.entrySet()) {
			b.append("[").append(entry.getKey()).append("/").append(entry.getValue()).append("]");
		}
		return b.toString();
	}

	/**
	 * Creates a new DataPoint
	 * @param value The float value for this datapoint
	 * @param metricName The metric name, a non-empty string.
	 * @param tags The metric tags. Must be non empty
	 * @return A correctly typed data point in accordance with the size of the value
	 */
	public static DataPoint newDataPoint(Number value, String metricName, Map<String, String> tags) {
		double v = value.doubleValue();
		if(v <= Long.MAX_VALUE) {
			return new LongDataPoint(value.longValue(), metricName, tags);
		} else if(v <= Float.MAX_VALUE) {
			return new FloatDataPoint(value.floatValue(), metricName, tags);
		} else {
			return new DoubleDataPoint(v, metricName, tags);
		}
	}

	/**
	 * Creates a new DataPoint
	 * @param value The float value for this datapoint
	 * @param metricName The metric name, a non-empty string.
	 * @param tags The metric tags. Must be a non empty, even numbered number of non empty strings
	 * @return A correctly typed data point in accordance with the size of the value
	 */
	public static DataPoint newDataPoint(Number value, String metricName, String... tags) {
		double v = value.doubleValue();
		if(v <= Long.MAX_VALUE) {
			return new LongDataPoint(value.longValue(), metricName, tags);
		} else if(v <= Float.MAX_VALUE) {
			return new FloatDataPoint(value.floatValue(), metricName, tags);
		} else {
			return new DoubleDataPoint(v, metricName, tags);
		}
	}
	
	
	

}
