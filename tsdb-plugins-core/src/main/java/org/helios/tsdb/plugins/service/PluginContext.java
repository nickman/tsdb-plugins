package org.helios.tsdb.plugins.service;

import java.util.Properties;
import java.util.Set;

import javax.management.NotificationEmitter;

import net.opentsdb.core.TSDB;
import net.opentsdb.utils.Config;

/**
 * <p>Title: PluginContext</p>
 * <p>Description: Defines the shared context used to share objects between plugin services</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.service.PluginContext</code></p>
 */
public interface PluginContext extends NotificationEmitter {

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
	public ClassLoader getSupportClassLoader();
	
	/**
	 * Returns a named resource in the context
	 * @param name The name of the resource
	 * @param type The type of the resource
	 * @return The named resource or null if the name was not bound 
	 */
	public <T> T getResource(String name, Class<?> type);
	
	/**
	 * Returns the names of all the registered resources
	 * @return the names of all the registered resources
	 */
	public Set<String> getResourceNames();

	/**
	 * Adds a resource to the named resource map
	 * @param name The name of the resource
	 * @param value The resource
	 */
	public void setResource(String name, Object value);

	/**
	 * Registers a resource listener. If a matching resource is already registered, 
	 * the listener will be invoked immediately.
	 * @param listener The listener to register
	 */
	public void addResourceListener(IPluginContextResourceListener listener);
	
	/**
	 * Registers a resource listener. If a matching resource is already registered, 
	 * the listener will be invoked immediately.
	 * @param listener The listener to register
	 * @param filter A filter to restrict the resources that trigger a callback on the passed listener
	 */
	public void addResourceListener(IPluginContextResourceListener listener, IPluginContextResourceFilter filter);

	/**
	 * Removes a registered resource listener
	 * @param listener The listener to remove
	 */
	public void removeResourceListener(IPluginContextResourceListener listener);
	
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
	


}