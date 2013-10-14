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
package org.helios.tsdb.plugins.async;

import java.util.Map;

/**
 * <p>Title: IPublishEventDispatcher</p>
 * <p>Description: Defines a dispatcher that accepts parameterized callbacks from the {@link net.opentsdb.tsd.RTPublisher} shell plugins
 * through the {@link org.helios.tsdb.plugins.event.TSDBEventDispatcher}, converts them into {@link org.helios.tsdb.plugins.event.TSDBEvent}s 
 * and dispatches them asynchronously.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.async.IPublishEventDispatcher</code></p>
 */

public interface IPublishEventDispatcher {
	/**
	 * Called any time a new data point is published
	 * @param metric The name of the metric associated with the data point
	 * @param timestamp Timestamp as a Unix epoch in seconds or milliseconds (depending on the TSD's configuration)
	 * @param value Value for the data point
	 * @param tags The metric tags
	 * @param tsuid Time series UID for the value
	 * @see net.opentsdb.tsd.RTPublisher#publishDataPoint(java.lang.String, long, double, java.util.Map, byte[])
	 */
	public void publishDataPoint(String metric, long timestamp, double value, Map<String, String> tags, byte[] tsuid);

	/**
	 * Called any time a new data point is published
	 * @param metric The name of the metric associated with the data point
	 * @param timestamp Timestamp as a Unix epoch in seconds or milliseconds (depending on the TSD's configuration)
	 * @param value Value for the data point
	 * @param tags The metric tags
	 * @param tsuid Time series UID for the value
	 * @see net.opentsdb.tsd.RTPublisher#publishDataPoint(java.lang.String, long, long, java.util.Map, byte[])
	 */
	public void publishDataPoint(String metric, long timestamp, long value, Map<String, String> tags, byte[] tsuid);
		

}
