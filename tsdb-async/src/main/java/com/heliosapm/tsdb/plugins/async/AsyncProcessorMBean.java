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
package com.heliosapm.tsdb.plugins.async;

import javax.management.ObjectName;

import reactor.jarjar.com.lmax.disruptor.WaitStrategy;

/**
 * <p>Title: AsyncProcessorMBean</p>
 * <p>Description: Simple MBean interface for the {@link AsyncProcessor}</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.tsdb.plugins.async.AsyncProcessorMBean</code></p>
 */

public interface AsyncProcessorMBean extends ThreadPoolExecutorMXBean {
	/** The AsyncProcessor's management interface ObjectName */
	public static final ObjectName OBJECT_NAME = AsyncProcessor.objectName("com.heliosapm.tsdb.plugins:service=AsyncProcessor");
	
	/** The config property for the processor's data buffer size */
	public static final String CONFIG_DATA_BUFFER_SIZE = "tsdb.asyncprocessor.databuffersize";
	/** The default processor's data buffer size */
	public static final int DEFAULT_DATA_BUFFER_SIZE = 1024;
	/** The config property for the processor's wait strategy name */
	public static final String CONFIG_WAIT_STRATEGY = "tsdb.asyncprocessor.waitstrategy";
	/** The default processor's wait strategy name */
	public static final WaitStrategy DEFAULT_WAIT_STRATEGY = WaitStrategyFactories.BLOCK.create();
	/** The config property for the processor's wait strategy parameters as comma separated strings */
	public static final String CONFIG_WAIT_STRATEGY_PARAM = "tsdb.asyncprocessor.waitstrategy.params";
	/** The default processor's wait strategy name */
	public static final String DEFAULT_WAIT_STRATEGY_PARAM = "";
	/** The config property for the processor's pre-defined event consumers as consumer class names in comma separated strings */
	public static final String CONFIG_CONSUMERS = "tsdb.asyncprocessor.consumers";
	/** The default processor's consumers as consumer class names in comma separated strings */
	public static final String DEFAULT_CONSUMERS = "";
	/** The config property name indicating if a halting exception should be thrown if configured consumers cannot be loaded */
	public static final String CONFIG_FAIL_ON_CONSUMER_LOAD = "tsdb.asyncprocessor.consumers.failenabled";
	/** The default processor's fail on consumer load */
	public static final boolean DEFAULT_FAIL_ON_CONSUMER_LOAD = true;
	
	
	/**
	 * Returns the total number of events published through the AsyncProcessor
	 * @return the total number of events published through the AsyncProcessor
	 */
	public long getTotalEventsPublished();
	
	/**
	 * Returns the total number of events consumed from the AsyncProcessor
	 * @return the total number of events consumed from the AsyncProcessor
	 */
	public long getTotalEventsConsumed();	
}
