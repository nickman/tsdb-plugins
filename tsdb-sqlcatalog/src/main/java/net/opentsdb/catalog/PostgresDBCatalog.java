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
import java.sql.Statement;
import java.util.List;
import java.util.Set;

import javax.sql.DataSource;

import net.opentsdb.catalog.sequence.LocalSequenceCache;
import net.opentsdb.catalog.sequence.PostgresLocalSequenceCache;
import net.opentsdb.meta.Annotation;
import net.opentsdb.meta.TSMeta;
import net.opentsdb.meta.UIDMeta;
import net.opentsdb.search.SearchQuery;

/**
 * <p>Title: PostgresDBCatalog</p>
 * <p>Description: DB catalog implementation for PostgresSQL</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>net.opentsdb.catalog.PostgresDBCatalog</code></p>
 */

public class PostgresDBCatalog extends AbstractDBCatalog {


	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.AbstractDBCatalog#doInitialize()
	 */
	@Override
	protected void doInitialize() {
		/* No Op */
	}

	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.AbstractDBCatalog#doShutdown()
	 */
	@Override
	protected void doShutdown() {
		/* No Op */
	}

	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.CatalogDBInterface#executeSearch(java.sql.Connection, net.opentsdb.search.SearchQuery)
	 */
	@Override
	public List<?> executeSearch(Connection conn, SearchQuery query) {
		// TODO Auto-generated method stub
		return null;
	}	
	
	protected LocalSequenceCache createLocalSequenceCache(int increment, String sequenceName, DataSource dataSource) {
		return new PostgresLocalSequenceCache(increment, sequenceName, dataSource);
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
	 * @see net.opentsdb.catalog.CatalogDBInterface#setConnectionProperty(java.sql.Connection, java.lang.String, java.lang.String)
	 */
	@Override
	public void setConnectionProperty(Connection conn, String key, String value) {
		Statement st = null;
		try {
			st = conn.createStatement();
			st.executeQuery("SELECT set_env_var('" + key + "', '" + value + "')");
		} catch (Exception ex) {
			throw new RuntimeException("Failed to set connection property [" + key +  "/" + value + "]", ex);
		} finally {
			if(st!=null) try { st.close(); } catch (Exception x) {/* No Op */}			
		}		
	}




	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.CatalogDBInterface#getConnectionProperty(java.sql.Connection, java.lang.String, java.lang.String)
	 */
	@Override
	public String getConnectionProperty(Connection conn, String key, String defaultValue) {
		Statement st = null;
		ResultSet rset = null;
		try {
			st = conn.createStatement();
			rset = st.executeQuery("SELECT get_env_var('" + key + "')");
			rset.next();
			String val = rset.getString(1);
			return val==null ? defaultValue : val;
		} catch (Exception ex) {
			throw new RuntimeException("Failed to get connection property [" + key + "]", ex);
		} finally {
			if(rset!=null) try { rset.close(); } catch (Exception x) {/* No Op */}
			if(st!=null) try { st.close(); } catch (Exception x) {/* No Op */}			
		}
	}

	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.CatalogDBInterface#recordSyncQueueFailure(net.opentsdb.meta.UIDMeta, net.opentsdb.catalog.TSDBTable)
	 */
	@Override
	public void recordSyncQueueFailure(UIDMeta uidMeta, TSDBTable tsdbTable) {
		// TODO Auto-generated method stub
		
	}

	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.CatalogDBInterface#recordSyncQueueFailure(net.opentsdb.meta.TSMeta)
	 */
	@Override
	public void recordSyncQueueFailure(TSMeta tsMeta) {
		// TODO Auto-generated method stub
		
	}

	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.CatalogDBInterface#recordSyncQueueFailure(net.opentsdb.meta.Annotation)
	 */
	@Override
	public void recordSyncQueueFailure(Annotation ann) {
		// TODO Auto-generated method stub
		
	}	


}
