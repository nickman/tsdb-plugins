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
package net.opentsdb.catalog;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.sql.DataSource;

import net.opentsdb.core.TSDB;

import org.h2.jdbcx.JdbcConnectionPool;
import org.helios.tsdb.plugins.util.ConfigurationHelper;
import org.helios.tsdb.plugins.util.URLHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Title: InitializeH2Database</p>
 * <p>Description: DB initializer for H2 DB</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>net.opentsdb.catalog.InitializeH2Database</code></p>
 */

public class InitializeH2Database implements IDBInitializer {
	/** Instance logger */
	protected Logger log = LoggerFactory.getLogger(getClass());
	
	/** The JDBC URL */
	protected String jdbcUrl = null;
	/** The JDBC Driver name */
	protected String jdbcDriver = null;
	/** The JDBC User name */
	protected String jdbcUser = null;
	/** The JDBC User password */
	protected String jdbcPassword = null;	
	/** The constructed datasource */
	protected JdbcConnectionPool dataSource = null;
	/** The H2 DDL resource */
	protected String ddlResource = null;	
	
	/** The config property name for the H2 Init DDL resource */
	public static final String DB_H2_DDL = "helios.search.catalog.h2.ddl";
	/** The default H2 Init DDL resource */
	public static final String DEFAULT_DB_H2_DDL = "classpath:/ddl/catalog.sql";
	
	
	/**
	 * Creates a new InitializeH2Database
	 */
	public InitializeH2Database() {
		log.info("Created DB Initializer");
	}
	
	/**
	 * Returns the created data source
	 * @return a connection pool data source
	 */
	public DataSource getDataSource() {
		return dataSource;
	}
	
	/**
	 * Runs the initialization routine
	 * @param tsdb The parent TSDB instance
	 * @param extracted The extracted configuration
	 */
	public void initialize(TSDB tsdb, Properties extracted) {
		log.info("\n\t================================================\n\tStarting DB Initializer\n\tName:{}\n\t================================================", getClass().getSimpleName());
		jdbcUrl = ConfigurationHelper.getSystemThenEnvProperty(DB_JDBC_URL, DEFAULT_DB_JDBC_URL, extracted);
		jdbcDriver = ConfigurationHelper.getSystemThenEnvProperty(DB_JDBC_DRIVER, DEFAULT_DB_JDBC_DRIVER, extracted);
		jdbcUser = ConfigurationHelper.getSystemThenEnvProperty(DB_JDBC_USER, DEFAULT_DB_JDBC_USER, extracted);
		jdbcPassword = ConfigurationHelper.getSystemThenEnvProperty(DB_JDBC_PW, DEFAULT_DB_JDBC_PW, extracted);
		ddlResource = ConfigurationHelper.getSystemThenEnvProperty(DB_H2_DDL, DEFAULT_DB_H2_DDL, extracted);
		dataSource = JdbcConnectionPool.create(jdbcUrl, jdbcUser, jdbcPassword);
		log.info("\n\t================================================\n\tDB Initializer Started\n\tName:{}\n\t================================================", getClass().getSimpleName());
	}
	
	/**
	 * Terminates the database resources
	 */
	public void shutdown() {
		log.info("\n\t================================================\n\tStopping DB Initializer\n\tName:{}\n\t================================================", getClass().getSimpleName());
		if(dataSource!=null) {
			dataSource.dispose();
		}
		log.info("\n\t================================================\n\tDB Initializer Stopped\n\tName:{}\n\t================================================", getClass().getSimpleName());
	}
	
	protected String getDDLFileName() {
		InputStream is = null;
		FileOutputStream fos = null;
		File tmpFile = null;
		try {
			is = getDDLStream();
			tmpFile = File.createTempFile("tsdb-catalog", ".sql");
			
		} catch (Exception ex) {
			log.error("Failed to get DDL file name", ex);
			throw new RuntimeException("Failed to get DDL file name", ex);
		} finally {
			
			if(is!=null) try { is.close(); } catch (Exception x) {/* No Op */}
			if(fos!=null) try { fos.close(); } catch (Exception x) {/* No Op */}
			if(tmpFile!=null) tmpFile.delete();
		}
	}
	
	/**
	 * Returns an inputstream for reading the H2 DDL
	 * @return an inputstream for the H2 DDL
	 * @throws IOException thrown on errors reading from a file or URL
	 */
	protected InputStream getDDLStream() throws IOException {
		if(ddlResource.startsWith("classpath:")) {
			String resource = ddlResource.substring("classpath:".length());		
			log.info("Looking Up DDL Resource [{}]", resource);
			InputStream is = getClass().getResourceAsStream(resource);
			if(is==null) {
				is = new FileInputStream("./src/main/resources" + resource);
			}
			return is;
		}
		if(URLHelper.isValidURL(ddlResource)) {
			return URLHelper.toURL(ddlResource).openStream();
		} else if(new File(ddlResource).canRead()) {
			return new FileInputStream(ddlResource);
		} else {
			throw new RuntimeException("Failed to locate DDL resource [" + ddlResource + "]");
		}
	}


}
