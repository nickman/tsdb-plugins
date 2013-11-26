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

/**
 * <p>Title: ICatalogDataSource</p>
 * <p>Description: Constants and defaults for data source creation</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>net.opentsdb.catalog.datasource.ICatalogDataSource</code></p>
 */

public interface ICatalogDataSource {

	/** The config property name for the pool acquireIncrement */
	public static final String JDBC_POOL_ACQUIREINCREMENT = "tsdb.jdbc.acquireIncrement";
	/** The default pool acquireIncrement */
	public static final String DEFAULT_JDBC_POOL_ACQUIREINCREMENT = "2";

	/** The config property name for the pool acquireRetryAttempts */
	public static final String JDBC_POOL_ACQUIRERETRYATTEMPTS = "tsdb.jdbc.acquireRetryAttempts";
	/** The default pool acquireRetryAttempts */
	public static final String DEFAULT_JDBC_POOL_ACQUIRERETRYATTEMPTS = "5";

	/** The config property name for the pool acquireRetryDelayInMs */
	public static final String JDBC_POOL_ACQUIRERETRYDELAYINMS = "tsdb.jdbc.acquireRetryDelayInMs";
	/** The default pool acquireRetryDelayInMs */
	public static final String DEFAULT_JDBC_POOL_ACQUIRERETRYDELAYINMS = "7000";

	/** The config property name for the pool classLoader */
	public static final String JDBC_POOL_CLASSLOADER = "tsdb.jdbc.classLoader";
	/** The default pool classLoader */
	public static final String DEFAULT_JDBC_POOL_CLASSLOADER = "";

	/** The config property name for the pool clientInfo */
	public static final String JDBC_POOL_CLIENTINFO = "tsdb.jdbc.clientInfo";
	/** The default pool clientInfo */
	public static final String DEFAULT_JDBC_POOL_CLIENTINFO = "";

	/** The config property name for the pool closeConnectionWatch */
	public static final String JDBC_POOL_CLOSECONNECTIONWATCH = "tsdb.jdbc.closeConnectionWatch";
	/** The default pool closeConnectionWatch */
	public static final String DEFAULT_JDBC_POOL_CLOSECONNECTIONWATCH = "false";

	/** The config property name for the pool closeConnectionWatchTimeout */
	public static final String JDBC_POOL_CLOSECONNECTIONWATCHTIMEOUTINMS = "tsdb.jdbc.closeConnectionWatchTimeoutInMs";
	/** The default pool closeConnectionWatchTimeout */
	public static final String DEFAULT_JDBC_POOL_CLOSECONNECTIONWATCHTIMEOUTINMS = "0";

	/** The config property name for the pool closeOpenStatements */
	public static final String JDBC_POOL_CLOSEOPENSTATEMENTS = "tsdb.jdbc.closeOpenStatements";
	/** The default pool closeOpenStatements */
	public static final String DEFAULT_JDBC_POOL_CLOSEOPENSTATEMENTS = "false";

	/** The config property name for the pool configFile */
	public static final String JDBC_POOL_CONFIGFILE = "tsdb.jdbc.configFile";
	/** The default pool configFile */
	public static final String DEFAULT_JDBC_POOL_CONFIGFILE = "";

	/** The config property name for the pool connectionHook */
	public static final String JDBC_POOL_CONNECTIONHOOK = "tsdb.jdbc.connectionHook";
	/** The default pool connectionHook */
	public static final String DEFAULT_JDBC_POOL_CONNECTIONHOOK = "";

	/** The config property name for the pool connectionHookClassName */
	public static final String JDBC_POOL_CONNECTIONHOOKCLASSNAME = "tsdb.jdbc.connectionHookClassName";
	/** The default pool connectionHookClassName */
	public static final String DEFAULT_JDBC_POOL_CONNECTIONHOOKCLASSNAME = "";

	/** The config property name for the pool connectionTestStatement */
	public static final String JDBC_POOL_CONNECTIONTESTSTATEMENT = "tsdb.jdbc.connectionTestStatement";
	/** The default pool connectionTestStatement */
	public static final String DEFAULT_JDBC_POOL_CONNECTIONTESTSTATEMENT = "";

	/** The config property name for the pool connectionTimeoutInMs */
	public static final String JDBC_POOL_CONNECTIONTIMEOUTINMS = "tsdb.jdbc.connectionTimeoutInMs";
	/** The default pool connectionTimeoutInMs */
	public static final String DEFAULT_JDBC_POOL_CONNECTIONTIMEOUTINMS = "0";

	/** The config property name for the pool datasourceBean */
	public static final String JDBC_POOL_DATASOURCEBEAN = "tsdb.jdbc.datasourceBean";
	/** The default pool datasourceBean */
	public static final String DEFAULT_JDBC_POOL_DATASOURCEBEAN = "";

	/** The config property name for the pool defaultAutoCommit */
	public static final String JDBC_POOL_DEFAULTAUTOCOMMIT = "tsdb.jdbc.defaultAutoCommit";
	/** The default pool defaultAutoCommit */
	public static final String DEFAULT_JDBC_POOL_DEFAULTAUTOCOMMIT = "true";

	/** The config property name for the pool defaultCatalog */
	public static final String JDBC_POOL_DEFAULTCATALOG = "tsdb.jdbc.defaultCatalog";
	/** The default pool defaultCatalog */
	public static final String DEFAULT_JDBC_POOL_DEFAULTCATALOG = "";

	/** The config property name for the pool defaultReadOnly */
	public static final String JDBC_POOL_DEFAULTREADONLY = "tsdb.jdbc.defaultReadOnly";
	/** The default pool defaultReadOnly */
	public static final String DEFAULT_JDBC_POOL_DEFAULTREADONLY = "false";

	/** The config property name for the pool defaultTransactionIsolation */
	public static final String JDBC_POOL_DEFAULTTRANSACTIONISOLATION = "tsdb.jdbc.defaultTransactionIsolation";
	/** The default pool defaultTransactionIsolation */
	public static final String DEFAULT_JDBC_POOL_DEFAULTTRANSACTIONISOLATION = "";

	/** The config property name for the pool deregisterDriverOnClose */
	public static final String JDBC_POOL_DEREGISTERDRIVERONCLOSE = "tsdb.jdbc.deregisterDriverOnClose";
	/** The default pool deregisterDriverOnClose */
	public static final String DEFAULT_JDBC_POOL_DEREGISTERDRIVERONCLOSE = "false";

	/** The config property name for the pool detectUnclosedStatements */
	public static final String JDBC_POOL_DETECTUNCLOSEDSTATEMENTS = "tsdb.jdbc.detectUnclosedStatements";
	/** The default pool detectUnclosedStatements */
	public static final String DEFAULT_JDBC_POOL_DETECTUNCLOSEDSTATEMENTS = "false";

	/** The config property name for the pool detectUnresolvedTransactions */
	public static final String JDBC_POOL_DETECTUNRESOLVEDTRANSACTIONS = "tsdb.jdbc.detectUnresolvedTransactions";
	/** The default pool detectUnresolvedTransactions */
	public static final String DEFAULT_JDBC_POOL_DETECTUNRESOLVEDTRANSACTIONS = "false";

	/** The config property name for the pool disableConnectionTracking */
	public static final String JDBC_POOL_DISABLECONNECTIONTRACKING = "tsdb.jdbc.disableConnectionTracking";
	/** The default pool disableConnectionTracking */
	public static final String DEFAULT_JDBC_POOL_DISABLECONNECTIONTRACKING = "false";

	/** The config property name for the pool disableJMX */
	public static final String JDBC_POOL_DISABLEJMX = "tsdb.jdbc.disableJMX";
	/** The default pool disableJMX */
	public static final String DEFAULT_JDBC_POOL_DISABLEJMX = "false";

	/** The config property name for the pool driverProperties */
	public static final String JDBC_POOL_DRIVERPROPERTIES = "tsdb.jdbc.driverProperties";
	/** The default pool driverProperties */
	public static final String DEFAULT_JDBC_POOL_DRIVERPROPERTIES = "";

	/** The config property name for the pool externalAuth */
	public static final String JDBC_POOL_EXTERNALAUTH = "tsdb.jdbc.externalAuth";
	/** The default pool externalAuth */
	public static final String DEFAULT_JDBC_POOL_EXTERNALAUTH = "false";

	/** The config property name for the pool idleConnectionTestPeriodInMinutes */
	public static final String JDBC_POOL_IDLECONNECTIONTESTPERIODINMINUTES = "tsdb.jdbc.idleConnectionTestPeriodInMinutes";
	/** The default pool idleConnectionTestPeriodInMinutes */
	public static final String DEFAULT_JDBC_POOL_IDLECONNECTIONTESTPERIODINMINUTES = "240";

	/** The config property name for the pool idleConnectionTestPeriodInSeconds */
	public static final String JDBC_POOL_IDLECONNECTIONTESTPERIODINSECONDS = "tsdb.jdbc.idleConnectionTestPeriodInSeconds";
	/** The default pool idleConnectionTestPeriodInSeconds */
	public static final String DEFAULT_JDBC_POOL_IDLECONNECTIONTESTPERIODINSECONDS = "14400";

	/** The config property name for the pool idleMaxAgeInMinutes */
	public static final String JDBC_POOL_IDLEMAXAGEINMINUTES = "tsdb.jdbc.idleMaxAgeInMinutes";
	/** The default pool idleMaxAgeInMinutes */
	public static final String DEFAULT_JDBC_POOL_IDLEMAXAGEINMINUTES = "60";

	/** The config property name for the pool idleMaxAgeInSeconds */
	public static final String JDBC_POOL_IDLEMAXAGEINSECONDS = "tsdb.jdbc.idleMaxAgeInSeconds";
	/** The default pool idleMaxAgeInSeconds */
	public static final String DEFAULT_JDBC_POOL_IDLEMAXAGEINSECONDS = "3600";

	/** The config property name for the pool initSQL */
	public static final String JDBC_POOL_INITSQL = "tsdb.jdbc.initSQL";
	/** The default pool initSQL */
	public static final String DEFAULT_JDBC_POOL_INITSQL = "";

	/** The config property name for the pool jdbcUrl */
	public static final String JDBC_POOL_JDBCDRIVER = "tsdb.jdbc.jdbcDriver";
	/** The default pool jdbcUrl */
	public static final String DEFAULT_JDBC_POOL_JDBCDRIVER = "org.h2.Driver";
	
	
	/** The config property name for the pool jdbcUrl */
	public static final String JDBC_POOL_JDBCURL = "tsdb.jdbc.jdbcUrl";
	/** The default pool jdbcUrl */
	public static final String DEFAULT_JDBC_POOL_JDBCURL = "jdbc:h2:mem:tsdb;JMX=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";

	/** The config property name for the pool lazyInit */
	public static final String JDBC_POOL_LAZYINIT = "tsdb.jdbc.lazyInit";
	/** The default pool lazyInit */
	public static final String DEFAULT_JDBC_POOL_LAZYINIT = "false";

	/** The config property name for the pool logStatementsEnabled */
	public static final String JDBC_POOL_LOGSTATEMENTSENABLED = "tsdb.jdbc.logStatementsEnabled";
	/** The default pool logStatementsEnabled */
	public static final String DEFAULT_JDBC_POOL_LOGSTATEMENTSENABLED = "false";

	/** The config property name for the pool maxConnectionAgeInSeconds */
	public static final String JDBC_POOL_MAXCONNECTIONAGEINSECONDS = "tsdb.jdbc.maxConnectionAgeInSeconds";
	/** The default pool maxConnectionAgeInSeconds */
	public static final String DEFAULT_JDBC_POOL_MAXCONNECTIONAGEINSECONDS = "0";

	/** The config property name for the pool maxConnectionsPerPartition */
	public static final String JDBC_POOL_MAXCONNECTIONSPERPARTITION = "tsdb.jdbc.maxConnectionsPerPartition";
	/** The default pool maxConnectionsPerPartition */
	public static final String DEFAULT_JDBC_POOL_MAXCONNECTIONSPERPARTITION = "10";

	/** The config property name for the pool minConnectionsPerPartition */
	public static final String JDBC_POOL_MINCONNECTIONSPERPARTITION = "tsdb.jdbc.minConnectionsPerPartition";
	/** The default pool minConnectionsPerPartition */
	public static final String DEFAULT_JDBC_POOL_MINCONNECTIONSPERPARTITION = "0";

	/** The config property name for the pool nullOnConnectionTimeout */
	public static final String JDBC_POOL_NULLONCONNECTIONTIMEOUT = "tsdb.jdbc.nullOnConnectionTimeout";
	/** The default pool nullOnConnectionTimeout */
	public static final String DEFAULT_JDBC_POOL_NULLONCONNECTIONTIMEOUT = "false";

	/** The config property name for the pool partitionCount */
	public static final String JDBC_POOL_PARTITIONCOUNT = "tsdb.jdbc.partitionCount";
	/** The default pool partitionCount */
	public static final String DEFAULT_JDBC_POOL_PARTITIONCOUNT = "1";

	/** The config property name for the pool password */
	public static final String JDBC_POOL_PASSWORD = "tsdb.jdbc.password";
	/** The default pool password */
	public static final String DEFAULT_JDBC_POOL_PASSWORD = "";

	/** The config property name for the pool poolAvailabilityThreshold */
	public static final String JDBC_POOL_POOLAVAILABILITYTHRESHOLD = "tsdb.jdbc.poolAvailabilityThreshold";
	/** The default pool poolAvailabilityThreshold */
	public static final String DEFAULT_JDBC_POOL_POOLAVAILABILITYTHRESHOLD = "0";

	/** The config property name for the pool poolName */
	public static final String JDBC_POOL_POOLNAME = "tsdb.jdbc.poolName";
	/** The default pool poolName */
	public static final String DEFAULT_JDBC_POOL_POOLNAME = "SQLCatalogDataSource";

	/** The config property name for the pool poolStrategy */
	public static final String JDBC_POOL_POOLSTRATEGY = "tsdb.jdbc.poolStrategy";
	/** The default pool poolStrategy */
	public static final String DEFAULT_JDBC_POOL_POOLSTRATEGY = "DEFAULT";

	/** The config property name for the pool queryExecuteTimeLimitInMs */
	public static final String JDBC_POOL_QUERYEXECUTETIMELIMITINMS = "tsdb.jdbc.queryExecuteTimeLimitInMs";
	/** The default pool queryExecuteTimeLimitInMs */
	public static final String DEFAULT_JDBC_POOL_QUERYEXECUTETIMELIMITINMS = "0";

	/** The config property name for the pool reConnectionOnClose */
	public static final String JDBC_POOL_RECONNECTIONONCLOSE = "tsdb.jdbc.reConnectionOnClose";
	/** The default pool reConnectionOnClose */
	public static final String DEFAULT_JDBC_POOL_RECONNECTIONONCLOSE = "";

	/** The config property name for the pool serviceOrder */
	public static final String JDBC_POOL_SERVICEORDER = "tsdb.jdbc.serviceOrder";
	/** The default pool serviceOrder */
	public static final String DEFAULT_JDBC_POOL_SERVICEORDER = "FIFO";

	/** The config property name for the pool statementCacheSize */
	public static final String JDBC_POOL_STATEMENTSCACHESIZE = "tsdb.jdbc.statementsCacheSize";
	/** The default pool statementCacheSize */
	public static final String DEFAULT_JDBC_POOL_STATEMENTSCACHESIZE = "100";

	/** The config property name for the pool statementsCachedPerConnection */
	public static final String JDBC_POOL_STATEMENTSCACHEDPERCONNECTION = "tsdb.jdbc.statementsCachedPerConnection";
	/** The default pool statementsCachedPerConnection */
	public static final String DEFAULT_JDBC_POOL_STATEMENTSCACHEDPERCONNECTION = "20";

	/** The config property name for the pool statisticsEnabled */
	public static final String JDBC_POOL_STATISTICSENABLED = "tsdb.jdbc.statisticsEnabled";
	/** The default pool statisticsEnabled */
	public static final String DEFAULT_JDBC_POOL_STATISTICSENABLED = "true";

	/** The config property name for the pool transactionRecoveryEnabled */
	public static final String JDBC_POOL_TRANSACTIONRECOVERYENABLED = "tsdb.jdbc.transactionRecoveryEnabled";
	/** The default pool transactionRecoveryEnabled */
	public static final String DEFAULT_JDBC_POOL_TRANSACTIONRECOVERYENABLED = "false";

	/** The config property name for the pool username */
	public static final String JDBC_POOL_USERNAME = "tsdb.jdbc.username";
	/** The default pool username */
	public static final String DEFAULT_JDBC_POOL_USERNAME = "sa";

	/**
	 * Returns the URL of the connected database
	 * @return the URL of the connected database
	 * @see com.jolbox.bonecp.BoneCPConfig#getJdbcUrl()
	 */
	public String getJdbcUrl();


	/**
	 * Returns the username  of the connected database user
	 * @return the username  of the connected database user
	 * @see com.jolbox.bonecp.BoneCPConfig#getUser()
	 */
	public String getUser();
	
	/**
	 * Returns the driver name and version used to connect to the database
	 * @return the driver name and version used to connect to the database
	 */
	public String getDriverName();
	

}
