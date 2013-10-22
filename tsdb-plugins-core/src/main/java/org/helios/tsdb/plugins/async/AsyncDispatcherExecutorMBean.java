package org.helios.tsdb.plugins.async;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * <p>Title: AsyncDispatcherExecutorMBean</p>
 * <p>Description: Management interface for {@link AsyncDispatcherExecutor} instances</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.async.AsyncDispatcherExecutorMBean</code></p>
 */
public interface AsyncDispatcherExecutorMBean {
	
	/** The ObjectName template with the pool name as the single token */
	public static final String OBJECT_NAME_TEMPLATE = "plugins.tsdb.async:service=Executor,name=%s";




	/**
	 * Indicates if core threads can timeout
	 * @return true if core threads can timeout
	 * @see java.util.concurrent.ThreadPoolExecutor#allowsCoreThreadTimeOut()
	 */
	public boolean getAllowsCoreThreadTimeOut();

	/**
	 * Returns the active thread count
	 * @return the active thread count
	 * @see java.util.concurrent.ThreadPoolExecutor#getActiveCount()
	 */
	public int getActiveCount();

	/**
	 * Returns the completed task count
	 * @return the completed task count
	 * @see java.util.concurrent.ThreadPoolExecutor#getCompletedTaskCount()
	 */
	public long getCompletedTaskCount();

	/**
	 * Returns the core pool size
	 * @return the core pool size
	 * @see java.util.concurrent.ThreadPoolExecutor#getCorePoolSize()
	 */
	public int getCorePoolSize();

	/**
	 * Returns the non-core thread keep-alive time in ms.
	 * @return the non-core thread keep-alive time in ms.
	 * @see java.util.concurrent.ThreadPoolExecutor#getKeepAliveTime(java.util.concurrent.TimeUnit)
	 */
	public long getKeepAliveTime();

	/**
	 * Returns the thread pool size highwater mark
	 * @return the thread pool size highwater mark
	 * @see java.util.concurrent.ThreadPoolExecutor#getLargestPoolSize()
	 */
	public int getLargestPoolSize();

	/**
	 * Returns the max pool size
	 * @return the max pool size
	 * @see java.util.concurrent.ThreadPoolExecutor#getMaximumPoolSize()
	 */
	public int getMaximumPoolSize();

	/**
	 * Returns the current pool size
	 * @return the current pool size
	 * @see java.util.concurrent.ThreadPoolExecutor#getPoolSize()
	 */
	public int getPoolSize();


	
	/**
	 * Returns the number of entries currently in the queue
	 * @return the number of entries currently in the queue
	 */
	public int getQueueDepth();
	
	/**
	 * Returns the queue capacity
	 * @return the queue capacity
	 */
	public int getQueueCapacity();

	/**
	 * Returns the number of rejected executions
	 * @return the number of rejected executions
	 */
	public long getRejectedExecutionCount();

	/**
	 * Returns the current task count
	 * @return the current task count
	 * @see java.util.concurrent.ThreadPoolExecutor#getTaskCount()
	 */
	public long getTaskCount();

	/**
	 * Indicates if this pool is shutdown
	 * @return true if this pool is shutdown, false otherwise
	 * @see java.util.concurrent.ThreadPoolExecutor#isShutdown()
	 */
	public boolean isShutdown();

	/**
	 * Indicates if this pool is terminated
	 * @return true if this pool is terminated, false otherwise
	 * @see java.util.concurrent.ThreadPoolExecutor#isTerminated()
	 */
	public boolean isTerminated();

	/**
	 * Indicates if this pool is terminating
	 * @return true if this pool is terminating, false otherwise
	 * @see java.util.concurrent.ThreadPoolExecutor#isTerminating()
	 */
	public boolean isTerminating();

	/**
	 * Purges the completed runnables
	 * @see java.util.concurrent.ThreadPoolExecutor#purge()
	 */
	public void purge();

	/**
	 * Executes a graceful shutdown on this pool
	 * @see java.util.concurrent.ThreadPoolExecutor#shutdown()
	 */
	public void shutdown();

	/**
	 * Executes an immediate shutdown on this pool
	 * @return the number of incomplete tasks
	 * @see java.util.concurrent.ThreadPoolExecutor#shutdownNow()
	 */
	public int shutdownImmediate();
	
	/**
	 * Returns the pool name
	 * @return the pool name
	 */
	public String getPoolName();

	/**
	 * Returns the uncaught exception count
	 * @return the uncaught exception count
	 */
	public long getUncaughtCount();	
}