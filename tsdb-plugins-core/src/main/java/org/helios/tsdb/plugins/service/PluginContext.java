package org.helios.tsdb.plugins.service;

import java.util.Properties;

import javax.sql.DataSource;

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
	
	/**
	 * Returns a reference to the configured catalog datasource
	 * @return the configured catalog datasource
	 */
	public DataSource getDataSource();

}