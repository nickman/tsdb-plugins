/**
 * 
 */
package net.opentsdb.catalog.syncqueue;

import javax.management.ObjectName;

import org.helios.tsdb.plugins.util.JMXHelper;

/**
 * <p>Title: SyncQueueProcessorMXBean</p>
 * <p>Description: JMX MX interface for {@link SyncQueueProcessor}</p>
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><b><code>net.opentsdb.catalog.syncqueue.SyncQueueProcessorMXBean</code></b>
 */

public interface SyncQueueProcessorMXBean {
	/** The JMX ObjectName for the SyncQueueProcessor */
	public static final ObjectName OBJECT_NAME = JMXHelper.objectName(new StringBuilder(SyncQueueProcessor.class.getPackage().getName()).append(":service=").append(SyncQueueProcessor.class.getSimpleName()));
	
	
	/** The sync queue jmx notification prefix */
	public static final String JMX_NOTIF_PREFIX = "syncqueue.";
	/** The sync queue sync process jmx notification prefix */
	public static final String JMX_NOTIF_SYNC_EVENT = JMX_NOTIF_PREFIX + "sync.attempt."; 
	/** The sync queue sync attempt started jmx notification prefix */
	public static final String JMX_NOTIF_SYNC_STARTED = JMX_NOTIF_SYNC_EVENT + "start";
	/** The sync queue sync attempt ended jmx notification prefix */
	public static final String JMX_NOTIF_SYNC_ENDED = JMX_NOTIF_SYNC_EVENT + "end";	
	
	/**
	 * Returns the TSDB Sync period in seconds
	 * @return the TSDB Sync period in seconds
	 */
	public long getTSDBSyncPeriod();
	
	/**
	 * Indicates if all TSDB Synchronization operations have been disabled.
	 * @return true if all TSDB Synchronization operations have been disabled, false otherwise
	 */
	public boolean isTSDBSyncDisabled();
	
	/**
	 * Sets the TSDB Sync period in seconds. 
	 * If this op modifies the existing value, a schedule change will be triggered.
	 * This may stop a started schedule, or start a stopped schedule. 
	 * @param newPeriod the TSDB Sync period in seconds.
	 */
	public void setTSDBSyncPeriod(long newPeriod);
	
	/**
	 * Returns the number of pending Sync ops we're waiting on 
	 * @return the number of pending Sync ops we're waiting on
	 */
	public long getPendingSynchOps();


	/**
	 * Returns the maximum number of sync ops to execute per batch 
	 * @return the maxSyncOps
	 */
	public int getMaxSyncOps();


	/**
	 * Sets the maximum number of sync ops to execute per batch 
	 * @param maxSyncOps the maxSyncOps to set
	 */
	public void setMaxSyncOps(int maxSyncOps);	
	
	/**
	 * Indicates if the sync service is running (i.e. if it has been started)
	 * @return true if the sync service has been started, false otherwise
	 */
	public boolean isRunning();
	
	/**
	 * Returns the name of the current state
	 * @return the name of the current state
	 */
	public String getState();
	
	
}	
	
	
