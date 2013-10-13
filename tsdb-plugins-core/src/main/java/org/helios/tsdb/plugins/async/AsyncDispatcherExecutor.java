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

public class AsyncDispatcherExecutor extends ThreadPoolExecutor  {
	
	/** Static class logger */
	protected static final Logger LOG = LoggerFactory.getLogger(AsyncDispatcherExecutor.class);
	
	/** The async uncaught exception handler */
	protected static final Thread.UncaughtExceptionHandler ASYNC_EXCEPTION_HANDLER = new Thread.UncaughtExceptionHandler() {
		/** Exception count */
		protected final AtomicLong exceptionCount = new AtomicLong();		

		@Override
		public void uncaughtException(Thread t, Throwable e) {
			exceptionCount.incrementAndGet();
			LOG.warn("Async Event Handler Uncaught Exception on thread {}", t.getName(), e);			
		}
	};
	
	/** The executor's thread factory */
	protected static final ThreadFactory ASYNC_THREAD_FACTORY = new ThreadFactory() {
		/** Thread serial number */
		protected final AtomicInteger serial = new AtomicInteger();
		@Override
		public Thread newThread(Runnable r) {
			Thread t = new Thread(r, "TSDBPluginAsyncDispatcher#" + serial.incrementAndGet());
			t.setDaemon(true);
			return t;
		}		
	};
	
	/** The executor's RejectedExecutionHandler */
	protected static final RejectedExecutionHandler ASYNC_REJECTED_HANDLER = new RejectedExecutionHandler() {
		/** Rejection count */
		protected final AtomicLong rejectionCount = new AtomicLong();		
		@Override
		public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
			rejectionCount.incrementAndGet();
			
		}
	};

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
								Constants.ASYNC_QUEUE_SIZE, Constants.DEFAULT_ASYNC_QUEUE_SIZE, config), false),
				ASYNC_THREAD_FACTORY, ASYNC_REJECTED_HANDLER
				
		);
	}


}
