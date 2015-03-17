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
package test.com.heliosapm.tsdb.plugins.async;

import com.heliosapm.tsdb.plugins.async.TSDBEventType;

/**
 * <p>Title: EventCountingTSDBEventConsumerMBean</p>
 * <p>Description: MX MBean interface for {@link EventCountingTSDBEventConsumer}</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>test.com.heliosapm.tsdb.plugins.async.EventCountingTSDBEventConsumerMBean</code></p>
 */

public interface EventCountingTSDBEventConsumerMBean {
	/**
	 * Returns the number of consumed {@link TSDBEventType#DPOINT_LONG} events
	 * @return the number of consumed events
	 */
	public long getDPOINT_LONG();

	/**
	 * Returns the number of consumed {@link TSDBEventType#DPOINT_DOUBLE} events
	 * @return the number of consumed events
	 */
	public long getDPOINT_DOUBLE();

	/**
	 * Returns the number of consumed {@link TSDBEventType#ANNOTATION} events
	 * @return the number of consumed events
	 */
	public long getANNOTATION();

	/**
	 * Returns the number of consumed {@link TSDBEventType#ANNOTATION_INDEX} events
	 * @return the number of consumed events
	 */
	public long getANNOTATION_INDEX();

	/**
	 * Returns the number of consumed {@link TSDBEventType#ANNOTATION_DELETE} events
	 * @return the number of consumed events
	 */
	public long getANNOTATION_DELETE();

	/**
	 * Returns the number of consumed {@link TSDBEventType#TSMETA_INDEX} events
	 * @return the number of consumed events
	 */
	public long getTSMETA_INDEX();

	/**
	 * Returns the number of consumed {@link TSDBEventType#TSMETA_DELETE} events
	 * @return the number of consumed events
	 */
	public long getTSMETA_DELETE();

	/**
	 * Returns the number of consumed {@link TSDBEventType#UIDMETA_INDEX} events
	 * @return the number of consumed events
	 */
	public long getUIDMETA_INDEX();

	/**
	 * Returns the number of consumed {@link TSDBEventType#UIDMETA_DELETE} events
	 * @return the number of consumed events
	 */
	public long getUIDMETA_DELETE();

	/**
	 * Returns the number of consumed {@link TSDBEventType#SEARCH} events
	 * @return the number of consumed events
	 */
	public long getSEARCH();
	
	/**
	 * Returns the TSDB's UID Cache Size
	 * @return the TSDB's UID Cache Size
	 */
	public int getUIDCacheSize();
	
	/**
	 * Returns the TSDB's UID Cache Hit count
	 * @return the TSDB's UID Cache Hit count
	 */
	public int getUIDCacheHits();
	
	/**
	 * Returns the TSDB's UID Cache Miss count
	 * @return the TSDB's UID Cache Miss count
	 */
	public int getUIDCacheMisses();
	


}
