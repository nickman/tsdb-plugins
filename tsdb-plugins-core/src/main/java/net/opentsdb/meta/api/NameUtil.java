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
package net.opentsdb.meta.api;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.node.ObjectNode;

import net.opentsdb.meta.UIDMeta;
import net.opentsdb.utils.JSON;

/**
 * <p>Title: NameUtil</p>
 * <p>Description: Name compilation utility functions</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>net.opentsdb.meta.api.NameUtil</code></p>
 */

public abstract class NameUtil {

	/** Fast string builder support */
	static final ThreadLocal<StringBuilder> stringBuilder = new ThreadLocal<StringBuilder>() {
		@Override
		protected StringBuilder initialValue() {
			return new StringBuilder(128);
		}
		@Override
		public StringBuilder get() {
			StringBuilder b = super.get();
			b.setLength(0);
			return b;
		}
	};
	
	
	
	/**
	 * Builds a stringy from the passed metric name and UID tags
	 * @param metric The metric name
	 * @param tags The UID tags
	 * @return a stringy
	 */
	public static final CharSequence buildObjectName(final String metric, final List<UIDMeta> tags) {
		if(tags==null || tags.isEmpty()) throw new IllegalArgumentException("The passed tags map was null or empty");
		String mname = metric==null || metric.isEmpty() ? "*" : metric;
		StringBuilder b = stringBuilder.get().append(mname).append(":");
		boolean k = true;		
		for(final UIDMeta meta: tags) {
			b.append(meta.getName());
			if(k) {
				b.append("=");
			} else {
				b.append(",");
			}
			k = !k;
		}
		b.deleteCharAt(b.length()-1);
		return b.toString();
		
	}
	
	public static final CharSequence buildObjectName(final String metric, final Map<String, String> tags) {
		if(tags==null || tags.isEmpty()) throw new IllegalArgumentException("The passed tags map was null or empty");
		String mname = metric==null || metric.isEmpty() ? "*" : metric;
		StringBuilder b = stringBuilder.get().append(mname).append(":");
		for(final Map.Entry<String, String> e: tags.entrySet()) {
			b.append(e.getKey()).append("=").append(e.getValue()).append(",");
		}
		b.deleteCharAt(b.length()-1);
		return b.toString();
	}

	/**
	 * Builds an ObjectNode to publish a double value datapoint to subscribers
	 * @param metric The data point metric name
	 * @param timestamp The data point timestamp
	 * @param value The data point value
	 * @param tags The data point tags
	 * @param tsuid The data point time series id
	 * @return the built object node to publish
	 */
	public static ObjectNode build(final String metric, final long timestamp, final double value, final Map<String,String> tags, final byte[] tsuid) {
		final ObjectNode on = JSON.getMapper().createObjectNode();
		on.put("metric", metric);
		on.put("ts", timestamp);
		on.put("value", value);
		on.put("type", "d");
		
		final ObjectNode tagMap = JSON.getMapper().createObjectNode();
		for(Map.Entry<String, String> e: tags.entrySet()) {
			tagMap.put(e.getKey(), e.getValue());
		}
		on.put("tags", tagMap);
		return on;
	}
	
	/**
	 * Builds an ObjectNode to publish a long value datapoint to subscribers
	 * @param metric The data point metric name
	 * @param timestamp The data point timestamp
	 * @param value The data point value
	 * @param tags The data point tags
	 * @param tsuid The data point time series id
	 * @return the built object node to publish
	 */
	public static ObjectNode build(final String metric, final long timestamp, final long value, final Map<String,String> tags, final byte[] tsuid) {
		final ObjectNode on = JSON.getMapper().createObjectNode();
		on.put("metric", metric);
		on.put("ts", timestamp);
		on.put("value", value);
		on.put("type", "l");		
		final ObjectNode tagMap = JSON.getMapper().createObjectNode();
		for(Map.Entry<String, String> e: tags.entrySet()) {
			tagMap.put(e.getKey(), e.getValue());
		}
		on.put("tags", tagMap);
		return on;
	}
	
	

}
