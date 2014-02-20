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
package net.opentsdb.catalog.h2.triggers;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.helios.tsdb.plugins.util.SystemClock;

/**
 * <p>Title: LastUpdateTSTrigger</p>
 * <p>Description: Trigger to update the <b><code>LAST_UPDATE</code></b> timestamp on update statements.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>net.opentsdb.catalog.h2.triggers.LastUpdateTSTrigger</code></p>
 */

public class LastUpdateTSTrigger extends AbstractTrigger {
	/** The JDBC index of the <b><code>LAST_UPDATE</code></b> timestamp column */
	private int tsColumnId = -1;
	
	/**
	 * {@inheritDoc}
	 * @see org.h2.api.Trigger#fire(java.sql.Connection, java.lang.Object[], java.lang.Object[])
	 */
	@Override
	public void fire(Connection conn, Object[] oldRow, Object[] newRow) throws SQLException {
		newRow[tsColumnId] = SystemClock.getTimestamp();

	}

	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.h2.triggers.AbstractTrigger#init(java.sql.Connection, java.lang.String, java.lang.String, java.lang.String, boolean, int)
	 */
	public void init(Connection conn, String schemaName, String triggerName, String tableName, boolean before, int type) throws SQLException {
		super.init(conn, schemaName, triggerName, tableName, before, type);
		ResultSet rset = conn.getMetaData().getColumns(null, schemaName, tableName, "LAST_UPDATE");
		rset.next();
		tsColumnId = rset.getInt(17)-1;
	}
}
