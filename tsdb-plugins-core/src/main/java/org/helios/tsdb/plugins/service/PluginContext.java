package org.helios.tsdb.plugins.service;

import java.util.Properties;

import net.opentsdb.core.TSDB;
import net.opentsdb.utils.Config;

public interface PluginContext {

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

}