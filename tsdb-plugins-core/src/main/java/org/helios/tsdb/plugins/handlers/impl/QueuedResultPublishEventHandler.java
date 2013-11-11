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
package org.helios.tsdb.plugins.handlers.impl;

import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import net.opentsdb.core.TSDB;

import org.helios.tsdb.plugins.event.TSDBEvent;
import org.helios.tsdb.plugins.event.TSDBPublishEvent;
import org.helios.tsdb.plugins.handlers.EmptyPublishEventHandler;
import org.helios.tsdb.plugins.util.ConfigurationHelper;

/**
 * <p>Title: QueuedResultPublishEventHandler</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.handlers.impl.QueuedResultPublishEventHandler</code></p>
 */

public class QueuedResultPublishEventHandler extends EmptyPublishEventHandler {
	/** The singleton instance */
	private static volatile QueuedResultPublishEventHandler instance = null;
	/** The singleton instance ctor lock */
	private static final Object lock = new Object();
	
	/** The queue events will be enqueued to */
	private BlockingQueue<Object> resultQueue = null; 


	/** The maximum capacity of the queue */
	private int maxSize = -1;
	/** The fairness of the queue */
	private boolean fair = false;
	
	/**
	 * Acquires the singleton instance
	 * @return the singleton instance
	 */
	public static QueuedResultPublishEventHandler getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new QueuedResultPublishEventHandler();
				}
			}
		}
		return instance;
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.handlers.AbstractTSDBEventHandler#initialize(net.opentsdb.core.TSDB, java.util.Properties, java.lang.ClassLoader)
	 */
	@Override
	public void initialize(TSDB tsdb, Properties extracted, ClassLoader supportClassLoader) {
		super.initialize(tsdb, extracted, supportClassLoader);
		maxSize = ConfigurationHelper.getIntSystemThenEnvProperty("org.helios.qhandler.publish.maxsize", 1024, extracted);
		fair = ConfigurationHelper.getBooleanSystemThenEnvProperty("org.helios.qhandler.publish.fair", true, extracted);
		resultQueue = new ArrayBlockingQueue<Object>(maxSize, fair);
		log.info("Created Queueing Handler with max size:{}  and fairness:{}", maxSize, fair);
	}
	
//	/**
//	 * {@inheritDoc}
//	 * @see org.helios.tsdb.plugins.handlers.EmptyPublishEventHandler#onEvent(org.helios.tsdb.plugins.event.TSDBPublishEvent)
//	 */
//	@Override
//	public void onEvent(TSDBPublishEvent event) throws Exception {
//		log.info("Queueing Event [{}]", event);
//		resultQueue.add(event);
//		if(log.isDebugEnabled()) log.debug("Queued event [{}]", event);
//	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.handlers.EmptyPublishEventHandler#onEvent(org.helios.tsdb.plugins.event.TSDBEvent, long, boolean)
	 */
	@Override
	public void onEvent(TSDBEvent event, long sequence, boolean endOfBatch) throws Exception {
		resultQueue.add(event);
		//if(log.isDebugEnabled()) log.debug("Queued event [{}]", event);		
	}
	
	/**
	 * Clears the queue if it has been initialized
	 */
	public void clearQueue() {
		if(resultQueue!=null) resultQueue.clear();
	}
	
	/**
	 * Returns the result queue
	 * @return the resultQueue
	 */
	public BlockingQueue<Object> getResultQueue() {
		return resultQueue;
	}
	
	
	/**
	 * Creates a new QueuedResultPublishEventHandler
	 */
	private QueuedResultPublishEventHandler() {
	}

}
