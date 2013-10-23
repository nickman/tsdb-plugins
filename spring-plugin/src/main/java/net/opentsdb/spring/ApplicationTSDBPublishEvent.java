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

import org.helios.tsdb.plugins.event.TSDBPublishEvent;

/**
 * <p>Title: ApplicationTSDBPublishEvent</p>
 * <p>Description: A spring application event wrapper for {@link TSDBPublishEvent}s.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>net.opentsdb.spring.ApplicationTSDBPublishEvent</code></p>
 */

public class ApplicationTSDBPublishEvent extends ApplicationTSDBEvent {

	/**  */
	private static final long serialVersionUID = -2122090464686721824L;

	/**
	 * Creates a new ApplicationTSDBPublishEvent
	 * @param tsdbPublishEvent The publish event
	 */
	public ApplicationTSDBPublishEvent(TSDBPublishEvent tsdbPublishEvent) {
		super(tsdbPublishEvent);
	}
	
	/**
	 * Converts the publish data point method invocation into a ApplicationTSDBPublishEvent to be published into the app context
	 * @param metric The name of the metric associated with the data point
	 * @param timestamp Timestamp as a Unix epoch in seconds or milliseconds (depending on the TSD's configuration)
	 * @param value Value for the data point
	 * @param tags The metric tags
	 * @param tsuid Time series UID for the value
	 * @return the created event
	 */
	public static ApplicationTSDBPublishEvent publishDataPoint(String metric, long timestamp, double value, Map<String,String> tags, byte[] tsuid) {
		return new ApplicationTSDBPublishEvent(new TSDBPublishEvent().publishDataPoint(metric, timestamp, value, tags, tsuid));
	}
	
	/**
	 * Converts the publish data point method invocation into a ApplicationTSDBPublishEvent to be published into the app context
	 * @param metric The name of the metric associated with the data point
	 * @param timestamp Timestamp as a Unix epoch in seconds or milliseconds (depending on the TSD's configuration)
	 * @param value Value for the data point
	 * @param tags The metric tags
	 * @param tsuid Time series UID for the value
	 * @return the created event
	 */
	public static ApplicationTSDBPublishEvent publishDataPoint(String metric, long timestamp, long value, Map<String,String> tags, byte[] tsuid) {
		return new ApplicationTSDBPublishEvent(new TSDBPublishEvent().publishDataPoint(metric, timestamp, value, tags, tsuid));
	}
	
	

}
