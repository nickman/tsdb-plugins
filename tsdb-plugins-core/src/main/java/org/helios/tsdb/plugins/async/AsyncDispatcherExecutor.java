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

import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.helios.tsdb.plugins.Constants;
import org.helios.tsdb.plugins.util.ConfigurationHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Title: AsyncDispatcherExecutor</p>
 * <p>Description: The {@link java.util.concurrent.Executor} that powers the {@link AsyncEventDispatcher}.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.async.AsyncDispatcherExecutor</code></p>
 */

public class AsyncDispatcherExecutor extends ThreadPoolExecutor implements ThreadFactory, RejectedExecutionHandler, Thread.UncaughtExceptionHandler, AsyncDispatcherExecutorMBean  {
	
	/** An arbitrary pool name */
	protected final String poolName;
	/** Instance logger */
	protected final Logger log;   
	/** Thread serial number */
	protected final AtomicInteger threadSerial = new AtomicInteger();
	/** Rejection count */
	protected final AtomicLong rejectionCount = new AtomicLong();		
	/** Uncaught exception count */
	protected final AtomicLong uncaughtCount = new AtomicLong();		
	
	
	
	
	
	
	/**
	 * Creates a new AsyncDispatcherExecutor
	 * @param poolName The pool name
	 * @param corePoolSize The thread pool core size
	 * @param maximumPoolSize The thread pool maximum size
	 * @param keepAliveTime The thread pool non-core thread keep alive time in ms.
	 * @param workQueueSize The size of the work queue
	 */
	public AsyncDispatcherExecutor(String poolName, int corePoolSize, int maximumPoolSize,
			long keepAliveTime, int workQueueSize) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, TimeUnit.MILLISECONDS, workQueueSize<2 ? new SynchronousQueue<Runnable>() : new ArrayBlockingQueue<Runnable>(workQueueSize));				
		this.poolName =  poolName;
		log = LoggerFactory.getLogger(getClass().getName() + "." + poolName);
		this.setThreadFactory(this);
		this.setRejectedExecutionHandler(this);
	}


	/**
	 * Creates a new AsyncDispatcherExecutor configured from the passed prefixed properties.
	 * This allows a new executor to be created using the same properties names, 
	 * except prefixed by some significant name.
	 * @param prefix The prefix of the properties to extract. (e.g. <b><code>"my.async.plugin"</code></b>).
	 * @param config The source of the prefixed properties.
	 */
	public AsyncDispatcherExecutor(String prefix, Properties config) {
		super(
				ConfigurationHelper.getIntSystemThenEnvProperty(prefix + "." + Constants.ASYNC_CORE_SIZE, Constants.DEFAULT_ASYNC_CORE_SIZE, config),
				ConfigurationHelper.getIntSystemThenEnvProperty(prefix + "." + Constants.ASYNC_MAX_SIZE, Constants.DEFAULT_ASYNC_MAX_SIZE, config),
				ConfigurationHelper.getLongSystemThenEnvProperty(prefix + "." + Constants.ASYNC_KEEPALIVE_TIME, Constants.DEFAULT_ASYNC_KEEPALIVE_TIME, config),
				TimeUnit.MILLISECONDS,
				new ArrayBlockingQueue<Runnable>(
						ConfigurationHelper.getIntSystemThenEnvProperty(
								prefix + "." + Constants.ASYNC_QUEUE_SIZE, Constants.DEFAULT_ASYNC_QUEUE_SIZE, config), false)
		);
		poolName = ConfigurationHelper.getSystemThenEnvProperty(prefix + "." + Constants.ASYNC_EXECUTOR_NAME, prefix + "ThreadPool", config); 
		log = LoggerFactory.getLogger(getClass().getName() + "." + poolName);
		this.setThreadFactory(this);
		this.setRejectedExecutionHandler(this);
	}
	

	/**
	 * Creates a new AsyncDispatcherExecutor
	 * @param config The TSDB configuration properties
	 */
	public AsyncDispatcherExecutor(Properties config) {
		super(
				ConfigurationHelper.getIntSystemThenEnvProperty(Constants.ASYNC_CORE_SIZE, Constants.DEFAULT_ASYNC_CORE_SIZE, config),
				ConfigurationHelper.getIntSystemThenEnvProperty(Constants.ASYNC_MAX_SIZE, Constants.DEFAULT_ASYNC_MAX_SIZE, config),
				ConfigurationHelper.getLongSystemThenEnvProperty(Constants.ASYNC_KEEPALIVE_TIME, Constants.DEFAULT_ASYNC_KEEPALIVE_TIME, config),
				TimeUnit.MILLISECONDS,
				new ArrayBlockingQueue<Runnable>(
						ConfigurationHelper.getIntSystemThenEnvProperty(
								Constants.ASYNC_QUEUE_SIZE, Constants.DEFAULT_ASYNC_QUEUE_SIZE, config), false)
		);
		poolName = ConfigurationHelper.getSystemThenEnvProperty(Constants.ASYNC_EXECUTOR_NAME, Constants.DEFAULT_ASYNC_EXECUTOR_NAME, config); 
		log = LoggerFactory.getLogger(getClass().getName() + "." + poolName);
		this.setThreadFactory(this);
		this.setRejectedExecutionHandler(this);
		
	}


	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.async.AsyncDispatcherExecutorMBean#getAllowsCoreThreadTimeOut()
	 */
	@Override
	public boolean getAllowsCoreThreadTimeOut() {
		return allowsCoreThreadTimeOut();
	}


	@Override
	public long getKeepAliveTime() {
		return getKeepAliveTime(TimeUnit.MILLISECONDS);
	}



	@Override
	public int getQueueDepth() {
		return getQueue().size();
	}


	@Override
	public int getQueueCapacity() {
		return getQueue().remainingCapacity();
	}


	@Override
	public long getRejectedExecutionCount() {
		return rejectionCount.get();
	}


	@Override
	public String getPoolName() {
		return poolName;
	}

	@Override
	public int shutdownImmediate() {
		return shutdownNow().size();
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Thread.UncaughtExceptionHandler#uncaughtException(java.lang.Thread, java.lang.Throwable)
	 */
	@Override
	public void uncaughtException(Thread thread, Throwable t) {
		long cnt = uncaughtCount.incrementAndGet();
		log.warn("Uncaught exception. Total uncaught: {}", cnt);		
	}


	/**
	 * {@inheritDoc}
	 * @see java.util.concurrent.RejectedExecutionHandler#rejectedExecution(java.lang.Runnable, java.util.concurrent.ThreadPoolExecutor)
	 */
	@Override
	public void rejectedExecution(Runnable r, ThreadPoolExecutor tpe) {
		long cnt = rejectionCount.incrementAndGet();
		log.warn("Execution Rejected. Total rejected: {}", cnt);		
	}


	/**
	 * {@inheritDoc}
	 * @see java.util.concurrent.ThreadFactory#newThread(java.lang.Runnable)
	 */
	@Override
	public Thread newThread(Runnable r) {
		Thread t = new Thread(r, poolName + "#" + threadSerial.incrementAndGet());
		t.setDaemon(true);
		t.setUncaughtExceptionHandler(this);
		return t;
	}


	/**
	 * Returns the uncaught exception count
	 * @return the uncaught exception count
	 */
	public long getUncaughtCount() {
		return uncaughtCount.get();
	}	

}
