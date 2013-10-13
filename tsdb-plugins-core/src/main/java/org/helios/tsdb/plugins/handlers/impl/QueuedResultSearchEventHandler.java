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
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import net.opentsdb.core.TSDB;

import org.helios.tsdb.plugins.event.TSDBSearchEvent;
import org.helios.tsdb.plugins.handlers.EmptySearchEventHandler;
import org.helios.tsdb.plugins.util.ConfigurationHelper;

/**
 * <p>Title: QueuedResultSearchEventHandler</p>
 * <p>Description: A testing event handler that enqueues all events to a queue where they can be validated by a test validator</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.handlers.impl.QueuedResultSearchEventHandler</code></p>
 */

public class QueuedResultSearchEventHandler extends EmptySearchEventHandler {
	/** The singleton instance */
	private static volatile QueuedResultSearchEventHandler instance = null;
	/** The singleton instance ctor lock */
	private static final Object lock = new Object();
	
	/** The queue events will be enqueued to */
	private BlockingQueue<Object> resultQueue = null; 


	/** The maximum capacity of the queue */
	private int maxSize = -1;
	/** The fairness of the queue */
	private boolean fair = false;
	
	/**
	 * Acquires the singleton instancemaxsize
	 * @return the singleton instance
	 */
	public static QueuedResultSearchEventHandler getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new QueuedResultSearchEventHandler();
				}
			}
		}
		return instance;
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.handlers.EmptySearchEventHandler#initialize(net.opentsdb.core.TSDB, java.util.Properties)
	 */
	@Override
	public void initialize(TSDB tsdb, Properties extracted) {
		super.initialize(tsdb, extracted);
		maxSize = ConfigurationHelper.getIntSystemThenEnvProperty("org.helios.qhandler.search.maxsize", 1024, extracted);
		fair = ConfigurationHelper.getBooleanSystemThenEnvProperty("org.helios.qhandler.search.fair", true, extracted);
		resultQueue = new ArrayBlockingQueue<Object>(maxSize, fair);
		log.info("Created Queueing Handler with max size:{}  and fairness:{}", maxSize, fair);
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.handlers.EmptySearchEventHandler#onEvent(org.helios.tsdb.plugins.event.TSDBSearchEvent)
	 */
	@Override
	public void onEvent(TSDBSearchEvent event) throws Exception {
		resultQueue.add(event);
		if(log.isDebugEnabled()) log.debug("Queued event [{}]", event);
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
	 * Creates a new QueuedResultSearchEventHandler
	 */
	private QueuedResultSearchEventHandler() {
	}

}
