/**
 * 
 */
package org.helios.tsdb.plugins.service;

import java.util.Map;
import java.util.Properties;

import javax.management.ObjectName;

import net.opentsdb.core.TSDB;
import net.opentsdb.utils.Config;

import org.helios.tsdb.plugins.util.JMXHelper;

/**
 * <p>Title: PluginContextImplMBean</p>
 * <p>Description: JMX Management interface for the plugin context </p>
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><b><code>org.helios.tsdb.plugins.service.PluginContextImplMBean</code></b>
 */

public interface PluginContextImplMBean {
	
	/** The JMX ObjectName for the PluginContextImpl */
	public static final ObjectName OBJECT_NAME = JMXHelper.objectName(new StringBuilder(PluginContext.class.getPackage().getName()).append(":service=").append(PluginContext.class.getSimpleName()));

	/**
	 * Returns the parent TSDB instance
	 * @return the tsdb
	 */
	public TSDB getTsdb();

	/**
	 * Returns the parent TSDB instance config
	 * @return the config
	 */
	public Config getConfig();

	/**
	 * Returns the extracted TSDB instance config properties
	 * @return the extracted
	 */
	public Properties getExtracted();

	/**
	 * Returns the plugin support classloader
	 * @return the supportClassLoader
	 */
	public ObjectName getSupportClassLoaderObjectName();
	
	/**
	 * Returns the resource bound to the passed name
	 * @param name The name of the bound resource
	 * @return The bound resource
	 */
	public Object getResource(String name);
	
	/**
	 * Returns a map of the bound resource class names keyed by the name of the bound resource
	 * @return a map of resource names and types
	 */
	public Map<String, String> getBoundResourceNames();
	
	/**
	 * Publishes a notification on behalf of a non-mbean service
	 * @param type The message type
	 * @param message The notification message
	 * @param userData The optional user data
	 * @param source The optional source of the notification
	 */
	public void publishNotification(String type, String message, Object userData, Object source);
	
	/**
	 * Publishes a notification on behalf of a non-mbean service
	 * @param type The message type
	 * @param message The notification message
	 */
	public void publishNotification(String type, String message);
	
	
	/**
	 * Returns the direct instance of this PluginContext
	 * @return the direct instance of this PluginContext
	 */
	public PluginContext getInstance();

}
