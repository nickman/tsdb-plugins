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

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.Properties;
import java.util.Set;

import javax.sql.DataSource;

import net.opentsdb.core.TSDB;
import net.opentsdb.meta.UIDMeta;

import org.helios.tsdb.plugins.event.TSDBSearchEvent;


/**
 * <p>Title: CatalogDBInterface</p>
 * <p>Description: Defines a SQL catalog DB initializer</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>net.opentsdb.catalog.CatalogDBInterface</code></p>
 */

public interface CatalogDBInterface {

	/** The config property name for the JDBC URL */
	public static final String DB_JDBC_URL = "helios.search.catalog.jdbc.url";
	/** The default JDBC URL */
	public static final String DEFAULT_DB_JDBC_URL = "jdbc:h2:mem:tsdb;JMX=TRUE;DB_CLOSE_DELAY=-1";
	/** The config property name for the JDBC Driver Name */
	public static final String DB_JDBC_DRIVER = "helios.search.catalog.jdbc.driver";
	/** The default JDBC Driver */
	public static final String DEFAULT_DB_JDBC_DRIVER = "org.h2.Driver";
	/** The config property name for the JDBC User Name */
	public static final String DB_JDBC_USER = "helios.search.catalog.jdbc.user";
	/** The default JDBC User */
	public static final String DEFAULT_DB_JDBC_USER = "sa";
	/** The config property name for the JDBC User Password */
	public static final String DB_JDBC_PW = "helios.search.catalog.jdbc.pw";
	/** The default JDBC User Password */
	public static final String DEFAULT_DB_JDBC_PW = "";
	
	/*
	 * Remote JDBC URL
	 * jdbc:h2:tcp://localhost:9092/mem:tsdb
	 */
	
	/**
	 * Returns the created data source
	 * @return a connection pool data source
	 */
	public DataSource getDataSource();
	
	/**
	 * Runs the initialization routine
	 * @param tsdb The parent TSDB instance
	 * @param extracted The extracted configuration
	 */
	public void initialize(TSDB tsdb, Properties extracted);
	
	/**
	 * Terminates the database resources
	 */
	public void shutdown();
	

	/**
	 * Processes a batch of events
	 * @param conn The connection to execute the events against
	 * @param events An ordered batch of events to process
	 */
	public void processEvents(Connection conn, Set<TSDBSearchEvent> events);
	
	/**
	 * Returns a collection of {@link UIDMeta}s read from the passed {@link ResultSet}.
	 * @param rset The result set to read from
	 * @return a [possibly empty] collection of UIDMetas
	 */
	public Collection<UIDMeta> readUIDMetas(ResultSet rset);
	
}
