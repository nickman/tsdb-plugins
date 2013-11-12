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

import java.io.Closeable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.Set;

import net.opentsdb.search.SearchQuery;

/**
 * <p>Title: OracleDBCatalog</p>
 * <p>Description: DB catalog implementation for Oracle</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>net.opentsdb.catalog.OracleDBCatalog</code></p>
 */

public class OracleDBCatalog extends AbstractDBCatalog {


	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.AbstractDBCatalog#doInitialize()
	 */
	@Override
	protected void doInitialize() {

	}
	
	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.CatalogDBInterface#isNaNToNull()
	 */
	@Override
	public boolean isNaNToNull() {
		return true;
	}

	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.AbstractDBCatalog#doShutdown()
	 */
	@Override
	protected void doShutdown() {

	}

	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.AbstractDBCatalog#executeSearch(java.sql.Connection, net.opentsdb.search.SearchQuery, java.util.Set)
	 */
	@Override
	public ResultSet executeSearch(Connection conn, SearchQuery query, Set<Closeable> closeables) {
		return null;
	}

}
