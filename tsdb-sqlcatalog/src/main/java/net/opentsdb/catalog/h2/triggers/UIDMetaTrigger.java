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
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;

import org.helios.tsdb.plugins.util.SystemClock;
import org.helios.tsdb.plugins.util.SystemClock.ElapsedTime;
import static net.opentsdb.catalog.CatalogDBInterface.*;


/**
 * <p>Title: UIDMetaTrigger</p>
 * <p>Description: Trigger for UIDMeta tables to capture deferred sync operations</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>net.opentsdb.catalog.h2.triggers.UIDMetaTrigger</code></p>
 */

public class UIDMetaTrigger extends AbstractSyncQueueTrigger {

	/**
	 * {@inheritDoc}
	 * @see org.h2.api.Trigger#fire(java.sql.Connection, java.lang.Object[], java.lang.Object[])
	 */
	@Override
	public void fire(Connection conn, Object[] oldRow, Object[] newRow)	throws SQLException {
		ElapsedTime et = SystemClock.startClock();
		try {
			if(isSQProcessor(conn)) return;
			callCount.incrementAndGet();
			final boolean EQ = isEQProcessor(conn);
			
			if(oldRow==null) {			
				// ======  INSERT  ======
				if(!EQ) {
					incrementVersion(newRow);
					incrementVersion(7, newRow);				
				}
				addSyncQueueEvent(conn, tableName, "I", newRow[0].toString());
			} else if(newRow==null) {
				// ======  DELETE  ======
				addSyncQueueEvent(conn, tableName, "D", oldRow[0].toString());
			} else {
				// ======  UPDATE  ======
				if(Arrays.deepEquals(oldRow, newRow)) return;			
				if(!EQ) {
					incrementVersion(newRow);
					incrementVersion(7, newRow);
				}
				addSyncQueueEvent(conn, tableName, "U", newRow[0].toString());
			}
		} finally {
			elapsedTimes.insert(et.elapsed());
		}
	}

	

}
