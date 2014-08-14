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
package net.opentsdb.catalog;


/**
 * <p>Title: TSDBCatalogSearchEventHandlerMBean</p>
 * <p>Description: JMX MBean interface for {@link TSDBCatalogSearchEventHandler}</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>net.opentsdb.catalog.TSDBCatalogSearchEventHandlerMBean</code></p>
 */

public interface TSDBCatalogSearchEventHandlerMBean {
	
	/**
	 * Returns the number of events pending in the processing queue
	 * @return the number of events pending in the processing queue
	 */
	public int getProcessingQueueDepth();

	/**
	 * Returns the configured JDBC batch size
	 * @return the configured JDBC batch size
	 */
	public int getBatchSize();
	
	/**
	 * Returns the configured event processing queue size
	 * @return the configured event processing queue size
	 */
	public int getQueueSize();
	
	/**
	 * Returns the time period allowed on shutdown to clear the processing queue in ms. 
	 * @return the time period allowed on shutdown to clear the processing queue in ms. 
	 */
	public long getShutdownFlushTimeout();
	

	
	
}
