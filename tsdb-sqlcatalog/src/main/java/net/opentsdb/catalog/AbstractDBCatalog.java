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
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import net.opentsdb.core.TSDB;
import net.opentsdb.meta.Annotation;
import net.opentsdb.meta.TSMeta;
import net.opentsdb.meta.UIDMeta;
import net.opentsdb.search.SearchQuery;

import org.helios.tsdb.plugins.event.TSDBSearchEvent;

import com.stumbleupon.async.Deferred;

/**
 * <p>Title: AbstractDBCatalog</p>
 * <p>Description: Base abstract class for implementing concrete DB catalogs for different JDBC supporting databases.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>net.opentsdb.catalog.AbstractDBCatalog</code></p>
 */

public class AbstractDBCatalog implements CatalogDBInterface {

	/**
	 * Creates a new AbstractDBCatalog
	 */
	public AbstractDBCatalog() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.CatalogDBInterface#getDataSource()
	 */
	@Override
	public DataSource getDataSource() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.CatalogDBInterface#initialize(net.opentsdb.core.TSDB, java.util.Properties)
	 */
	@Override
	public void initialize(TSDB tsdb, Properties extracted) {
		// TODO Auto-generated method stub

	}

	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.CatalogDBInterface#shutdown()
	 */
	@Override
	public void shutdown() {
		// TODO Auto-generated method stub

	}

	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.CatalogDBInterface#processEvents(java.sql.Connection, java.util.Set)
	 */
	@Override
	public void processEvents(Connection conn, Set<TSDBSearchEvent> events) {
		// TODO Auto-generated method stub

	}

	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.CatalogDBInterface#readUIDMetas(java.sql.ResultSet)
	 */
	@Override
	public Collection<UIDMeta> readUIDMetas(ResultSet rset) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<TSMeta> readTSMetas(ResultSet rset) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<Annotation> readAnnotations(ResultSet rset) {
		// TODO Auto-generated method stub
		return null;
	}
	
	/**
	 * Converts a millisecond based timestamp to a unix seconds based timestamp
	 * @param time The millisecond timestamp to convert
	 * @return a unix timestamp
	 */
	public static long mstou(long time) {
		return TimeUnit.SECONDS.convert(time, TimeUnit.MILLISECONDS);
	}
	
	/**
	 * Converts a unix second based timestamp to a long millisecond based timestamp
	 * @param time The unix timestamp to convert
	 * @return a long millisecond timestamp
	 */
	public static long utoms(long time) {
		return TimeUnit.MILLISECONDS.convert(time, TimeUnit.SECONDS);
	}

	@Override
	public Deferred<SearchQuery> executeQuery(SearchQuery query,
			Deferred<SearchQuery> result) {
		// TODO Auto-generated method stub
		return null;
	}
		

}
