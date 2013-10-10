/**
* Helios Development Group LLC, 2013. 
 *
 */
package org.helios.tsdb.plugins.util.jmx.threadinfo;

import java.lang.management.ThreadMXBean;

/**
 * <p>Title: ExtendedThreadManagerMXBean</p>
 * <p>Description: JMX MXBean interface for {@link ExtendedThreadManager}</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.jmx.threadinfo.ExtendedThreadManagerMXBean</code></p>
 */

public interface ExtendedThreadManagerMXBean extends ThreadMXBean {
	/**
	 * Returns the max depth used for getting thread infos
	 * @return the max depth used for getting thread infos
	 */
	public int getMaxDepth();

	/**
	 * Sets the max depth used for getting thread infos
	 * @param maxDepth the max depth used for getting thread infos
	 */
	public void setMaxDepth(int maxDepth);
	
	/**
	 * Returns an array ExtendedThreadInfos for all threads in the VM
	 * @return an array ExtendedThreadInfos for all threads in the VM
	 */
	public ExtendedThreadInfo[] getThreadInfo();
	
	/**
	 * Returns the number of non-daemon threads
	 * @return the number of non-daemon threads
	 */
	public int getNonDaemonThreadCount();
	
	/**
	 * Returns the names of the non-daemon threads
	 * @return the names of the non-daemon threads
	 */
	public String[] getNonDaemonThreadNames();
	
	/**
	 * Generates a list of thread names ordered in cpu utilization descending
	 * @param sampleTime The time to sample for
	 * @return a list of thread names with the cpu time appended
	 */
	public String[] getBusyThreads(long sampleTime);

}
