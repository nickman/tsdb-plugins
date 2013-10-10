/**
* Helios Development Group LLC, 2013. 
 *
 */
package org.helios.tsdb.plugins.util.jmx.threadinfo;

import java.lang.Thread.State;
import java.lang.management.LockInfo;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;

/**
 * <p>Title: ExtendedThreadInfo</p>
 * <p>Description: Wrapper adding additional functionality for standard {@link ThreadInfo}s </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.jmx.threadinfo.ExtendedThreadInfo</code></p>
 */

public class ExtendedThreadInfo implements ExtendedThreadInfoMBean {
	/** The wrapped thread info */
	private final ThreadInfo delegate;
	
	/**
	 * Wraps an array of {@link ThreadInfo}s.
	 * @param infos The array of {@link ThreadInfo}s to wrap
	 * @return an array of ExtendedThreadInfos
	 */
	public static ExtendedThreadInfo[] wrapThreadInfos(ThreadInfo...infos) {
		ExtendedThreadInfo[] xinfos = new ExtendedThreadInfo[infos.length];
		for(int i = 0; i < infos.length; i++) {
			xinfos[i] = new ExtendedThreadInfo(infos[i]);
		}
		return xinfos;
	}
	
	/**
	 * Creates a new ExtendedThreadInfo
	 * @param threadInfo the delegate thread info
	 */
	ExtendedThreadInfo(ThreadInfo threadInfo) {
		delegate = threadInfo;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.util.jmx.threadinfo.ExtendedThreadInfoMBean#hashCode()
	 */
	@Override
	public int hashCode() {
		return delegate.hashCode();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.util.jmx.threadinfo.ExtendedThreadInfoMBean#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		return delegate.equals(obj);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.util.jmx.threadinfo.ExtendedThreadInfoMBean#getThreadId()
	 */
	@Override
	public long getThreadId() {
		return delegate.getThreadId();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.util.jmx.threadinfo.ExtendedThreadInfoMBean#getThreadName()
	 */
	@Override
	public String getThreadName() {
		return delegate.getThreadName();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.util.jmx.threadinfo.ExtendedThreadInfoMBean#getThreadState()
	 */
	@Override
	public State getThreadState() {
		return delegate.getThreadState();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.util.jmx.threadinfo.ExtendedThreadInfoMBean#getBlockedTime()
	 */
	@Override
	public long getBlockedTime() {
		return delegate.getBlockedTime();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.util.jmx.threadinfo.ExtendedThreadInfoMBean#getBlockedCount()
	 */
	@Override
	public long getBlockedCount() {
		return delegate.getBlockedCount();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.util.jmx.threadinfo.ExtendedThreadInfoMBean#getWaitedTime()
	 */
	@Override
	public long getWaitedTime() {
		return delegate.getWaitedTime();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.util.jmx.threadinfo.ExtendedThreadInfoMBean#getWaitedCount()
	 */
	@Override
	public long getWaitedCount() {
		return delegate.getWaitedCount();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.util.jmx.threadinfo.ExtendedThreadInfoMBean#getLockInfo()
	 */
	@Override
	public LockInfo getLockInfo() {
		return delegate.getLockInfo();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.util.jmx.threadinfo.ExtendedThreadInfoMBean#getLockName()
	 */
	@Override
	public String getLockName() {
		return delegate.getLockName();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.util.jmx.threadinfo.ExtendedThreadInfoMBean#getLockOwnerId()
	 */
	@Override
	public long getLockOwnerId() {
		return delegate.getLockOwnerId();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.util.jmx.threadinfo.ExtendedThreadInfoMBean#getLockOwnerName()
	 */
	@Override
	public String getLockOwnerName() {
		return delegate.getLockOwnerName();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.util.jmx.threadinfo.ExtendedThreadInfoMBean#getStackTrace()
	 */
	@Override
	public StackTraceElement[] getStackTrace() {
		return delegate.getStackTrace();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.util.jmx.threadinfo.ExtendedThreadInfoMBean#isSuspended()
	 */
	@Override
	public boolean isSuspended() {
		return delegate.isSuspended();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.util.jmx.threadinfo.ExtendedThreadInfoMBean#isInNative()
	 */
	@Override
	public boolean isInNative() {
		return delegate.isInNative();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.util.jmx.threadinfo.ExtendedThreadInfoMBean#toString()
	 */
	@Override
	public String toString() {
		return delegate.toString();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.util.jmx.threadinfo.ExtendedThreadInfoMBean#getLockedMonitors()
	 */
	@Override
	public MonitorInfo[] getLockedMonitors() {
		return delegate.getLockedMonitors();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.util.jmx.threadinfo.ExtendedThreadInfoMBean#getLockedSynchronizers()
	 */
	@Override
	public LockInfo[] getLockedSynchronizers() {
		return delegate.getLockedSynchronizers();
	}
	

}
