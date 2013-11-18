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
package net.opentsdb.catalog.datasource;

import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;

import javassist.ClassPool;
import javassist.LoaderClassPath;

import javax.sql.DataSource;

import org.helios.tsdb.plugins.service.PluginContext;
import org.helios.tsdb.plugins.util.ConfigurationHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jolbox.bonecp.BoneCP;
import com.jolbox.bonecp.BoneCPConfig;
import com.jolbox.bonecp.BoneCPDataSource;

/**
 * <p>Title: CatalogDataSource</p>
 * <p>Description: A configurable data source to provide JDBC connections for the Catalog event handler</p> 
 * <p>For the full documentation on these parameters, please consult the <a href="http://jolbox.com/bonecp/downloads/site/apidocs/com/jolbox/bonecp/BoneCPConfig.html">BoneCPConfig</a> JavaDoc.</p>
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>net.opentsdb.catalog.datasource.CatalogDataSource</code></p>
 */

public class CatalogDataSource implements ICatalogDataSource {
	/** The singleton instance */
	private static volatile CatalogDataSource instance = null;
	/** The singleton instance ctor lock */
	private static final Object lock = new Object();

	/** The built datasource configuration */
	protected BoneCPConfig config = null;
	/** The built datasource */
	protected BoneCPDataSource connectionPool = null;
	/** Instance logger */
	protected final Logger log = LoggerFactory.getLogger(getClass());
	
	/** The driver wrapper */
	protected DelegatingDriver delegatingDriver = null;

	/** DataSource log writer */
	protected final PrintWriter dmLog = new PrintWriter(System.err);
	
	/** The config property name to enable DriverManager logging to system err */
	public static final String JDBC_POOL_DMLOGGING = "tsdb.jdbc.drivermanager.enablelogging";
	/** The default DriverManager logging enablement  */
	public static final boolean DEFAULT_JDBC_POOL_DMLOGGING = false;
	
	
	/**
	 * Acquires the CatalogDataSource singleton instance
	 * @return the CatalogDataSource singleton instance
	 */
	public static CatalogDataSource getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new CatalogDataSource();
				}
			}
		}
		return instance;
	}

	
	/**
	 * Creates a new CatalogDataSource
	 */
	private CatalogDataSource() {
		
	}
	
	/**
	 * Returns the configured data source
	 * @return the configured data source
	 */
	public DataSource getDataSource() {
		if(connectionPool==null) {
			throw new RuntimeException("The datasource is null. Did you call initialize ?");
		}
		return connectionPool;
	}
	
	/**
	 * Initializes the data source
	 * @param pc The plugin context
	 */
	public void initialize(PluginContext pc) {
		try {			
			log.info("Initializing CatalogDataSource. CL:[{}]  ThreadCL:[{}]", getClass().getClassLoader(), Thread.currentThread().getContextClassLoader());
			Properties dsProps = configure(pc.getExtracted());
			if(ConfigurationHelper.getBooleanSystemThenEnvProperty(JDBC_POOL_DMLOGGING, DEFAULT_JDBC_POOL_DMLOGGING, pc.getExtracted())) {
				DriverManager.setLogWriter(dmLog);
			}
			delegatingDriver = new DelegatingDriver(pc);
			DriverManager.registerDriver(delegatingDriver);
			config = new BoneCPConfig(dsProps);		
			config.setDefaultAutoCommit(false);
			config.sanitize();
			connectionPool = new BoneCPDataSource(config);
//			log.info("Loading JDBC Driver [{}] with classloader [{}]", driver, pc.getSupportClassLoader());
//			config.setClassLoader(pc.getSupportClassLoader());
//			checkDriverClasspath(driver, Thread.currentThread().getContextClassLoader());
//			checkDriverClasspath(driver, pc.getSupportClassLoader());
			pc.setResource(getClass().getSimpleName(), connectionPool);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to create datasource", ex);
		}
	}
	
	/**
	 * Prints some diagnostics about a loaded driver
	 * @param driver The driver
	 * @param classLoader The driver classloader
	 * @return true if the driver was found on the classpath
	 */
	protected boolean checkDriverClasspath(String driver, ClassLoader classLoader) {
		try {
			log.info("Testing for Driver [{}] with classloader [{}]", driver, classLoader);
			ClassPool cp = new ClassPool();
			cp.appendClassPath(new LoaderClassPath(classLoader));
			cp.get(driver);
			log.info("Found Driver [{}] with classloader [{}]", driver, classLoader);
			return true;
		} catch (Exception ex) {
			log.info("FAILED to find Driver [{}] with classloader [{}]", driver, classLoader);
			return false;
		}
	}
	
	/**
	 * Reads the JDBC datasource configuration from the TSDB passed config and returns a BoneCP properties config.
	 * @param config The TSDB provided configuration
	 * @return the BoneCP configuration properties
	 */
	public Properties configure(Properties config) {
		Properties p = new Properties();
		setIfNotEmpty(p, "acquireIncrement", ConfigurationHelper.getSystemThenEnvProperty(JDBC_POOL_ACQUIREINCREMENT, DEFAULT_JDBC_POOL_ACQUIREINCREMENT, config));
		setIfNotEmpty(p, "acquireRetryAttempts", ConfigurationHelper.getSystemThenEnvProperty(JDBC_POOL_ACQUIRERETRYATTEMPTS, DEFAULT_JDBC_POOL_ACQUIRERETRYATTEMPTS, config));
		setIfNotEmpty(p, "acquireRetryDelayInMs", ConfigurationHelper.getSystemThenEnvProperty(JDBC_POOL_ACQUIRERETRYDELAYINMS, DEFAULT_JDBC_POOL_ACQUIRERETRYDELAYINMS, config));
		setIfNotEmpty(p, "classLoader", ConfigurationHelper.getSystemThenEnvProperty(JDBC_POOL_CLASSLOADER, DEFAULT_JDBC_POOL_CLASSLOADER, config));
		setIfNotEmpty(p, "clientInfo", ConfigurationHelper.getSystemThenEnvProperty(JDBC_POOL_CLIENTINFO, DEFAULT_JDBC_POOL_CLIENTINFO, config));
		setIfNotEmpty(p, "closeConnectionWatch", ConfigurationHelper.getSystemThenEnvProperty(JDBC_POOL_CLOSECONNECTIONWATCH, DEFAULT_JDBC_POOL_CLOSECONNECTIONWATCH, config));
		setIfNotEmpty(p, "closeConnectionWatchTimeoutInMs", ConfigurationHelper.getSystemThenEnvProperty(JDBC_POOL_CLOSECONNECTIONWATCHTIMEOUTINMS, DEFAULT_JDBC_POOL_CLOSECONNECTIONWATCHTIMEOUTINMS, config));
		setIfNotEmpty(p, "closeOpenStatements", ConfigurationHelper.getSystemThenEnvProperty(JDBC_POOL_CLOSEOPENSTATEMENTS, DEFAULT_JDBC_POOL_CLOSEOPENSTATEMENTS, config));
		setIfNotEmpty(p, "configFile", ConfigurationHelper.getSystemThenEnvProperty(JDBC_POOL_CONFIGFILE, DEFAULT_JDBC_POOL_CONFIGFILE, config));
		setIfNotEmpty(p, "connectionHook", ConfigurationHelper.getSystemThenEnvProperty(JDBC_POOL_CONNECTIONHOOK, DEFAULT_JDBC_POOL_CONNECTIONHOOK, config));
		setIfNotEmpty(p, "connectionHookClassName", ConfigurationHelper.getSystemThenEnvProperty(JDBC_POOL_CONNECTIONHOOKCLASSNAME, DEFAULT_JDBC_POOL_CONNECTIONHOOKCLASSNAME, config));
		setIfNotEmpty(p, "connectionTestStatement", ConfigurationHelper.getSystemThenEnvProperty(JDBC_POOL_CONNECTIONTESTSTATEMENT, DEFAULT_JDBC_POOL_CONNECTIONTESTSTATEMENT, config));
		setIfNotEmpty(p, "connectionTimeoutInMs", ConfigurationHelper.getSystemThenEnvProperty(JDBC_POOL_CONNECTIONTIMEOUTINMS, DEFAULT_JDBC_POOL_CONNECTIONTIMEOUTINMS, config));
		setIfNotEmpty(p, "datasourceBean", ConfigurationHelper.getSystemThenEnvProperty(JDBC_POOL_DATASOURCEBEAN, DEFAULT_JDBC_POOL_DATASOURCEBEAN, config));
		setIfNotEmpty(p, "defaultAutoCommit", ConfigurationHelper.getSystemThenEnvProperty(JDBC_POOL_DEFAULTAUTOCOMMIT, DEFAULT_JDBC_POOL_DEFAULTAUTOCOMMIT, config));
		setIfNotEmpty(p, "defaultCatalog", ConfigurationHelper.getSystemThenEnvProperty(JDBC_POOL_DEFAULTCATALOG, DEFAULT_JDBC_POOL_DEFAULTCATALOG, config));
		setIfNotEmpty(p, "defaultReadOnly", ConfigurationHelper.getSystemThenEnvProperty(JDBC_POOL_DEFAULTREADONLY, DEFAULT_JDBC_POOL_DEFAULTREADONLY, config));
		setIfNotEmpty(p, "defaultTransactionIsolation", ConfigurationHelper.getSystemThenEnvProperty(JDBC_POOL_DEFAULTTRANSACTIONISOLATION, DEFAULT_JDBC_POOL_DEFAULTTRANSACTIONISOLATION, config));
		setIfNotEmpty(p, "deregisterDriverOnClose", ConfigurationHelper.getSystemThenEnvProperty(JDBC_POOL_DEREGISTERDRIVERONCLOSE, DEFAULT_JDBC_POOL_DEREGISTERDRIVERONCLOSE, config));
		setIfNotEmpty(p, "detectUnclosedStatements", ConfigurationHelper.getSystemThenEnvProperty(JDBC_POOL_DETECTUNCLOSEDSTATEMENTS, DEFAULT_JDBC_POOL_DETECTUNCLOSEDSTATEMENTS, config));
		setIfNotEmpty(p, "detectUnresolvedTransactions", ConfigurationHelper.getSystemThenEnvProperty(JDBC_POOL_DETECTUNRESOLVEDTRANSACTIONS, DEFAULT_JDBC_POOL_DETECTUNRESOLVEDTRANSACTIONS, config));
		setIfNotEmpty(p, "disableConnectionTracking", ConfigurationHelper.getSystemThenEnvProperty(JDBC_POOL_DISABLECONNECTIONTRACKING, DEFAULT_JDBC_POOL_DISABLECONNECTIONTRACKING, config));
		setIfNotEmpty(p, "disableJMX", ConfigurationHelper.getSystemThenEnvProperty(JDBC_POOL_DISABLEJMX, DEFAULT_JDBC_POOL_DISABLEJMX, config));
		setIfNotEmpty(p, "driverProperties", ConfigurationHelper.getSystemThenEnvProperty(JDBC_POOL_DRIVERPROPERTIES, DEFAULT_JDBC_POOL_DRIVERPROPERTIES, config));
		setIfNotEmpty(p, "externalAuth", ConfigurationHelper.getSystemThenEnvProperty(JDBC_POOL_EXTERNALAUTH, DEFAULT_JDBC_POOL_EXTERNALAUTH, config));
		setIfNotEmpty(p, "idleConnectionTestPeriodInMinutes", ConfigurationHelper.getSystemThenEnvProperty(JDBC_POOL_IDLECONNECTIONTESTPERIODINMINUTES, DEFAULT_JDBC_POOL_IDLECONNECTIONTESTPERIODINMINUTES, config));
		setIfNotEmpty(p, "idleConnectionTestPeriodInSeconds", ConfigurationHelper.getSystemThenEnvProperty(JDBC_POOL_IDLECONNECTIONTESTPERIODINSECONDS, DEFAULT_JDBC_POOL_IDLECONNECTIONTESTPERIODINSECONDS, config));
		setIfNotEmpty(p, "idleMaxAgeInMinutes", ConfigurationHelper.getSystemThenEnvProperty(JDBC_POOL_IDLEMAXAGEINMINUTES, DEFAULT_JDBC_POOL_IDLEMAXAGEINMINUTES, config));
		setIfNotEmpty(p, "idleMaxAgeInSeconds", ConfigurationHelper.getSystemThenEnvProperty(JDBC_POOL_IDLEMAXAGEINSECONDS, DEFAULT_JDBC_POOL_IDLEMAXAGEINSECONDS, config));
		setIfNotEmpty(p, "initSQL", ConfigurationHelper.getSystemThenEnvProperty(JDBC_POOL_INITSQL, DEFAULT_JDBC_POOL_INITSQL, config));
		setIfNotEmpty(p, "jdbcUrl", ConfigurationHelper.getSystemThenEnvProperty(JDBC_POOL_JDBCURL, DEFAULT_JDBC_POOL_JDBCURL, config));
		setIfNotEmpty(p, "lazyInit", ConfigurationHelper.getSystemThenEnvProperty(JDBC_POOL_LAZYINIT, DEFAULT_JDBC_POOL_LAZYINIT, config));
		setIfNotEmpty(p, "logStatementsEnabled", ConfigurationHelper.getSystemThenEnvProperty(JDBC_POOL_LOGSTATEMENTSENABLED, DEFAULT_JDBC_POOL_LOGSTATEMENTSENABLED, config));
		setIfNotEmpty(p, "maxConnectionAgeInSeconds", ConfigurationHelper.getSystemThenEnvProperty(JDBC_POOL_MAXCONNECTIONAGEINSECONDS, DEFAULT_JDBC_POOL_MAXCONNECTIONAGEINSECONDS, config));
		setIfNotEmpty(p, "maxConnectionsPerPartition", ConfigurationHelper.getSystemThenEnvProperty(JDBC_POOL_MAXCONNECTIONSPERPARTITION, DEFAULT_JDBC_POOL_MAXCONNECTIONSPERPARTITION, config));
		setIfNotEmpty(p, "minConnectionsPerPartition", ConfigurationHelper.getSystemThenEnvProperty(JDBC_POOL_MINCONNECTIONSPERPARTITION, DEFAULT_JDBC_POOL_MINCONNECTIONSPERPARTITION, config));
		setIfNotEmpty(p, "nullOnConnectionTimeout", ConfigurationHelper.getSystemThenEnvProperty(JDBC_POOL_NULLONCONNECTIONTIMEOUT, DEFAULT_JDBC_POOL_NULLONCONNECTIONTIMEOUT, config));
		setIfNotEmpty(p, "partitionCount", ConfigurationHelper.getSystemThenEnvProperty(JDBC_POOL_PARTITIONCOUNT, DEFAULT_JDBC_POOL_PARTITIONCOUNT, config));
		setIfNotEmpty(p, "password", ConfigurationHelper.getSystemThenEnvProperty(JDBC_POOL_PASSWORD, DEFAULT_JDBC_POOL_PASSWORD, config));
		setIfNotEmpty(p, "poolAvailabilityThreshold", ConfigurationHelper.getSystemThenEnvProperty(JDBC_POOL_POOLAVAILABILITYTHRESHOLD, DEFAULT_JDBC_POOL_POOLAVAILABILITYTHRESHOLD, config));
		setIfNotEmpty(p, "poolName", ConfigurationHelper.getSystemThenEnvProperty(JDBC_POOL_POOLNAME, DEFAULT_JDBC_POOL_POOLNAME, config));
		setIfNotEmpty(p, "poolStrategy", ConfigurationHelper.getSystemThenEnvProperty(JDBC_POOL_POOLSTRATEGY, DEFAULT_JDBC_POOL_POOLSTRATEGY, config));
		setIfNotEmpty(p, "queryExecuteTimeLimitInMs", ConfigurationHelper.getSystemThenEnvProperty(JDBC_POOL_QUERYEXECUTETIMELIMITINMS, DEFAULT_JDBC_POOL_QUERYEXECUTETIMELIMITINMS, config));
		setIfNotEmpty(p, "reConnectionOnClose", ConfigurationHelper.getSystemThenEnvProperty(JDBC_POOL_RECONNECTIONONCLOSE, DEFAULT_JDBC_POOL_RECONNECTIONONCLOSE, config));
		setIfNotEmpty(p, "serviceOrder", ConfigurationHelper.getSystemThenEnvProperty(JDBC_POOL_SERVICEORDER, DEFAULT_JDBC_POOL_SERVICEORDER, config));
		setIfNotEmpty(p, "statementsCacheSize", ConfigurationHelper.getSystemThenEnvProperty(JDBC_POOL_STATEMENTSCACHESIZE, DEFAULT_JDBC_POOL_STATEMENTSCACHESIZE, config));
		setIfNotEmpty(p, "statementsCachedPerConnection", ConfigurationHelper.getSystemThenEnvProperty(JDBC_POOL_STATEMENTSCACHEDPERCONNECTION, DEFAULT_JDBC_POOL_STATEMENTSCACHEDPERCONNECTION, config));
		setIfNotEmpty(p, "statisticsEnabled", ConfigurationHelper.getSystemThenEnvProperty(JDBC_POOL_STATISTICSENABLED, DEFAULT_JDBC_POOL_STATISTICSENABLED, config));
		setIfNotEmpty(p, "transactionRecoveryEnabled", ConfigurationHelper.getSystemThenEnvProperty(JDBC_POOL_TRANSACTIONRECOVERYENABLED, DEFAULT_JDBC_POOL_TRANSACTIONRECOVERYENABLED, config));
		setIfNotEmpty(p, "username", ConfigurationHelper.getSystemThenEnvProperty(JDBC_POOL_USERNAME, DEFAULT_JDBC_POOL_USERNAME, config));
		return p;
	}
	
	/**
	 * Sets a property if the value is not null or empty
	 * @param p The properties instance to set the property in
	 * @param key The property key
	 * @param value The property value
	 */
	protected void setIfNotEmpty(Properties p, String key, String value) {
		if(value!=null && !value.trim().isEmpty()) {
			p.setProperty(key, value.trim());
		}
	}

	/**
	 * Closes out the datasource
	 */
	public void shutdown() {
		if(connectionPool!=null) {
			preShutdownWorkAround();
			connectionPool.close();
			connectionPool = null;			
		}
		config = null;
		instance = null;		
		try { DriverManager.deregisterDriver(delegatingDriver); } catch (Exception ex) {/* No Op */}
		try { DriverManager.deregisterDriver(delegatingDriver.driver); } catch (Exception ex) {/* No Op */}
	}


	/**
	 * Returns 
	 * @return the config
	 */
	public BoneCPConfig getConfig() {
		return config;
	}
	
	
	//============================================================================================
	//  All the following stuff is a temporary work around for a lib version mismatch
	//  between versions of Guava for OpenTSDB (13.x) and BoneCP (15).
	//  BoneCP attempts to call com.google.common.base.FinalizableReferenceQueue.close()
	//	which does not exist in 13.x.
	//  This work around reflectively cleans up the 13.x version FinalizableReferenceQueue
	// 	and then sets it to null so close is never called.
	//  Should be fixed when OpenTSDB code set upgrades to Guava 15+
	//	or we implement isolated class loaders for plugins.
	//============================================================================================
	
	private void preShutdownWorkAround() {
		try {
			BoneCP bcp = connectionPool.getPool();
			Object obj = refQueueField.get(bcp);
			if(obj!=null) {
				refQueueCleanup.invoke(obj);
				refQueueField.set(bcp, null);
			}
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		}
	}
	
	
	/** The ref queue field in the BoneCP class */
	private static final Field refQueueField;
	/** The ref queue cleanup method */
	private static final Method refQueueCleanup;
	
	static {
		try {
			Class<?> refQueueClazz = Class.forName("com.google.common.base.FinalizableReferenceQueue", true, BoneCP.class.getClassLoader());
			refQueueField = BoneCP.class.getDeclaredField("finalizableRefQueue");
			refQueueField.setAccessible(true);
			refQueueCleanup = refQueueClazz.getDeclaredMethod("cleanUp");
			refQueueCleanup.setAccessible(true);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.datasource.ICatalogDataSource#getJdbcUrl()
	 */
	@Override
	public String getJdbcUrl() {
		return config.getJdbcUrl();
	}


	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.datasource.ICatalogDataSource#getUser()
	 */
	@Override
	public String getUser() {
		return config.getUser();
	}

	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.datasource.ICatalogDataSource#getDriverName()
	 */
	@Override
	public String getDriverName() {
		return delegatingDriver.getName();
	}
	

	/**
	 * <p>Title: DelegatingDriver</p>
	 * <p>Description: Delegating driver to persuade the DriverManager to use this driver</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>net.opentsdb.catalog.datasource.CatalogDataSource.DelegatingDriver</code></p>
	 */
	public static class DelegatingDriver implements Driver {
		/** The wrapped driver */
		private final Driver driver;

		/**
		 * Creates a new DelegatingDriver
		 * @param driver the driver to wrap
		 */
		public DelegatingDriver(Driver driver) {
			this.driver = driver;
		}
		
		/**
		 * {@inheritDoc}
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			return driver.toString();
		}
		
		/**
		 * Returns the driver name and version
		 * @return the driver name and version
		 */
		public String getName() {
			return String.format("%s version %s.%s", driver.getClass().getName(), driver.getMajorVersion(), driver.getMinorVersion());
		}
		
		/**
		 * Creates a new DelegatingDriver
		 * @param pc The plugin context
		 */
		public DelegatingDriver(PluginContext pc) {
			try {
				String driverName = ConfigurationHelper.getSystemThenEnvProperty(JDBC_POOL_JDBCDRIVER, DEFAULT_JDBC_POOL_JDBCDRIVER, pc.getExtracted()).trim();
				Class<Driver> clazz = (Class<Driver>) Class.forName(driverName, true, pc.getSupportClassLoader());
				driver = clazz.newInstance();
			} catch (Exception ex) {
				throw new RuntimeException("Failed to create delegating driver", ex);
			}
		}

		/**
		 * {@inheritDoc}
		 * @see java.sql.Driver#connect(java.lang.String, java.util.Properties)
		 */
		@Override
		public Connection connect(String url, Properties info)
				throws SQLException {
			return driver.connect(url, info);
		}


		/**
		 * {@inheritDoc}
		 * @see java.sql.Driver#acceptsURL(java.lang.String)
		 */
		@Override
		public boolean acceptsURL(String url) throws SQLException {
			return driver.acceptsURL(url);
		}

		/**
		 * {@inheritDoc}
		 * @see java.sql.Driver#getPropertyInfo(java.lang.String, java.util.Properties)
		 */
		@Override
		public DriverPropertyInfo[] getPropertyInfo(String url, Properties info)
				throws SQLException {
			return driver.getPropertyInfo(url, info);
		}

		/**
		 * {@inheritDoc}
		 * @see java.sql.Driver#getMajorVersion()
		 */
		@Override
		public int getMajorVersion() {
			return driver.getMajorVersion();
		}

		/**
		 * {@inheritDoc}
		 * @see java.sql.Driver#getMinorVersion()
		 */
		@Override
		public int getMinorVersion() {
			return driver.getMinorVersion();
		}

		/**
		 * {@inheritDoc}
		 * @see java.sql.Driver#jdbcCompliant()
		 */
		@Override
		public boolean jdbcCompliant() {
			return driver.jdbcCompliant();
		}

		/**
		 * {@inheritDoc}
		 * @see java.sql.Driver#getParentLogger()
		 */
		@Override
		public java.util.logging.Logger getParentLogger()
				throws SQLFeatureNotSupportedException {
			return driver.getParentLogger();
		}
	}



}
