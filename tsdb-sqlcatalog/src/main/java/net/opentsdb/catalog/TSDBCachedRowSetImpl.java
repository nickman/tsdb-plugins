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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Hashtable;

import com.sun.rowset.CachedRowSetImpl;

/**
 * <p>Title: TSDBCachedRowSetImpl</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>net.opentsdb.catalog.TSDBCachedRowSetImpl</code></p>
 */

public class TSDBCachedRowSetImpl extends CachedRowSetImpl {
	protected boolean closed = true;
	protected Statement statement = null;
	

	/**
	 * Creates a new TSDBCachedRowSetImpl
	 * @throws SQLException
	 */
	public TSDBCachedRowSetImpl() throws SQLException {
		super();
	}


	/**
	 * Creates a new TSDBCachedRowSetImpl
	 * @param tab
	 * @throws SQLException
	 */
	public TSDBCachedRowSetImpl(Hashtable tab) throws SQLException {
		super(tab);
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see com.sun.rowset.CachedRowSetImpl#size()
	 */
	@SuppressWarnings("restriction")
	public int size() {
		return super.size();
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.sun.rowset.CachedRowSetImpl#populate(java.sql.ResultSet)
	 */
	@Override
	public void populate(ResultSet rset) throws SQLException {
		closed = false;
		statement = rset.getStatement();
		super.populate(rset);
	}
	
	@Override
	public Statement getStatement() throws SQLException {		
		return statement;
	}
	
	@Override
	public boolean isClosed() throws SQLException {
		return closed;
	}
	
	@Override
	public void populate(ResultSet rset, int i) throws SQLException {
		closed = false;
		statement = rset.getStatement();
		super.populate(rset, i);
	}
	
	@Override
	public void close() throws SQLException {
		closed = true;
		
		super.close();
	}

}
