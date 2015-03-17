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

import java.util.concurrent.TimeUnit;

/**
 * <p>Title: ThreadPoolExecutorMXBean</p>
 * <p>Description: MXBean interface for managed thread pools</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.tsdb.plugins.async.ThreadPoolExecutorMXBean</code></p>
 */

public interface ThreadPoolExecutorMXBean {

	/**
	 * Indicates if the processor's thread pool is shutdown
	 * @return true if the processor's thread pool is shutdown, false otherwise
	 * @see java.util.concurrent.ThreadPoolExecutor#isShutdown()
	 */
	public boolean isShutdown();

	/**
	 * Indicates if the processor's thread pool is terminating
	 * @return true if the processor's thread pool is terminating, false otherwise
	 * @see java.util.concurrent.ThreadPoolExecutor#isTerminating()
	 */
	public boolean isTerminating();

	/**
	 * Indicates if the processor's thread pool is terminated
	 * @return true if the processor's thread pool, false otherwise
	 * @see java.util.concurrent.ThreadPoolExecutor#isTerminated()
	 */
	public boolean isTerminated();

	/**
	 * Sets the processor's thread pool core size
	 * @param corePoolSize the new pool core size
	 * @see java.util.concurrent.ThreadPoolExecutor#setCorePoolSize(int)
	 */
	public void setCorePoolSize(final int corePoolSize);

	/**
	 * Returns the processor's thread pool core pool size
	 * @return the core pool size
	 * @see java.util.concurrent.ThreadPoolExecutor#getCorePoolSize()
	 */
	public int getCorePoolSize();

	/**
	 * Indicates if the processor's thread pool allows core thread timeout
	 * @return true if the processor's thread pool allows core thread timeout, false otherwise
	 * @see java.util.concurrent.ThreadPoolExecutor#allowsCoreThreadTimeOut()
	 */
	public boolean isCoreThreadTimeOutAllowed();

	/**
	 * Sets the processor's thread pool core thread timeout enablement
	 * @param allow true to allow core thread timeout, false for core threads that stay alive until the thread pool terminate
	 * @see java.util.concurrent.ThreadPoolExecutor#allowCoreThreadTimeOut(boolean)
	 */
	public void setCoreThreadTimeOutAllowed(final boolean allow);

	/**
	 * Sets the processor's thread pool max pool size
	 * @param maximumPoolSize The max pool size
	 * @see java.util.concurrent.ThreadPoolExecutor#setMaximumPoolSize(int)
	 */
	public void setMaximumPoolSize(final int maximumPoolSize);

	/**
	 * Sets the processor's thread pool max pool size
	 * @return tha max pool size
	 * @see java.util.concurrent.ThreadPoolExecutor#getMaximumPoolSize()
	 */
	public int getMaximumPoolSize();

	/**
	 * Sets the time limit for which threads may remain idle in the processor's thread pool before being terminated
	 * @param time The time limit in ms.
	 * @see java.util.concurrent.ThreadPoolExecutor#setKeepAliveTime(long, java.util.concurrent.TimeUnit)
	 */
	public void setKeepAliveTimeMs(final long time);

	/**
	 * Returns the time limit for which threads may remain idle in the processor's thread pool before being terminated
	 * @return the time limit in ms.
	 * @see java.util.concurrent.ThreadPoolExecutor#getKeepAliveTime(java.util.concurrent.TimeUnit)
	 */
	public long getKeepAliveTimeMs();

	/**
	 * Returns the number of tasks queued in the processor's thread pool
	 * @return the number of queued tasks
	 */
	public int getQueueDepth();
	
	/**
	 * Returns the number of free slots in the processor thread pool's task queue
	 * @return the number of queue free slots
	 */
	public int getQueueFreeSlots();
	
	/**
	 * Returns the processor thread pool's task queue class name
	 * @return the processor thread pool's task queue class name
	 */
	public String getQueueType();
	

	/**
	 * Returns the processor's thread pool current pool size
	 * @return the processor's thread pool current pool size
	 * @see java.util.concurrent.ThreadPoolExecutor#getPoolSize()
	 */
	public int getPoolSize();

	/**
	 * Returns the number of active tasks in the processor's thread pool
	 * @return the number of active tasks
	 * @see java.util.concurrent.ThreadPoolExecutor#getActiveCount()
	 */
	public int getActiveCount();

	/**
	 * Returns the largest number of threads that have ever simultaneously been in the pool.
	 * @return the thread highwater
	 * @see java.util.concurrent.ThreadPoolExecutor#getLargestPoolSize()
	 */
	public int getLargestPoolSize();

	/**
	 * Returns the approximate total number of tasks that have ever been scheduled for execution. 
	 * @return the number of tasks scheduled
	 * @see java.util.concurrent.ThreadPoolExecutor#getTaskCount()
	 */
	public long getTaskCount();

	/**
	 * Returns the approximate total number of tasks that have completed execution.
	 * @return the number of tasks completed
	 * @see java.util.concurrent.ThreadPoolExecutor#getCompletedTaskCount()
	 */
	public long getCompletedTaskCount();

}
