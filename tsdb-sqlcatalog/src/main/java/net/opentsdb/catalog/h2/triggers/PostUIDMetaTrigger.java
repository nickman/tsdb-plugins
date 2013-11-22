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

import static net.opentsdb.catalog.CatalogDBInterface.TSD_CONN_TYPE;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.helios.tsdb.plugins.util.SystemClock;
import org.helios.tsdb.plugins.util.SystemClock.ElapsedTime;

/**
 * <p>Title: PostUIDMetaTrigger</p>
 * <p>Description: Post update trigger to trigger updates to the FQN if a UIDMeta name was changed</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>net.opentsdb.catalog.h2.triggers.PostUIDMetaTrigger</code></p>
 */

public class PostUIDMetaTrigger extends UIDMetaTrigger {

	/**
	 * Creates a new PostUIDMetaTrigger
	 */
	public PostUIDMetaTrigger() {
		super();
	}
	
	protected String FIND_FQNS_SQL = null;
	protected String UPDATE_TAGPAIRS_SQL = null;
	
	/**
	 * {@inheritDoc}
	 * 
	 */
	
	@Override
	public void init(Connection conn, String schemaName, String triggerName,
			String tableName, boolean before, int type) throws SQLException {
		super.init(conn, schemaName, triggerName, tableName, before, type);
		if("TSD_METRIC".equals(tableName)) {
			FIND_FQNS_SQL = "SELECT FQNID FROM TSD_TSMETA WHERE METRIC_UID = ?";
		} else if("TSD_TAGK".equals(tableName)) {
			FIND_FQNS_SQL = "SELECT FQNID FROM TSD_TAGK M, TSD_FQN_TAGPAIR P, TSD_TAGPAIR T "
					+ "WHERE T.XUID = P.XUID AND T.TAGK = M.XUID AND M.XUID = ?";
			UPDATE_TAGPAIRS_SQL = "UPDATE TSD_TAGPAIR SET NAME=TPN(XUID) WHERE TAGK = ?";
		} else if("TSD_TAGV".equals(tableName)) {
			FIND_FQNS_SQL = "SELECT FQNID FROM TSD_TAGV M, TSD_FQN_TAGPAIR P, TSD_TAGPAIR T "
					+ "WHERE T.XUID = P.XUID AND T.TAGV = M.XUID AND M.XUID = ?";
			UPDATE_TAGPAIRS_SQL = "UPDATE TSD_TAGPAIR SET NAME=TPN(XUID) WHERE TAGV = ?";
		} else {
			throw new RuntimeException("This trigger does not belong on the table [" + tableName + "]");
		}
	}
	
	@Override
	public void fire(Connection conn, Object[] oldRow, Object[] newRow) throws SQLException {
		
		if(!oldRow[2].equals(newRow[2])) {
			ElapsedTime et = SystemClock.startClock();
			// the name has been updated, 
			// so we need to trigger an update of the FQN
			final String tsdConnType = getConnectionProperty(conn, TSD_CONN_TYPE, "");
			PreparedStatement ps = null;
			PreparedStatement updatePs = null;
			ResultSet rset = null;
			int updates = 0;
			try {
				setUserDefinedVar(conn, TSD_CONN_TYPE, "");
				if(UPDATE_TAGPAIRS_SQL!=null) {
					updatePs = conn.prepareStatement(UPDATE_TAGPAIRS_SQL);
					updatePs.setString(1, (String)newRow[0]);
					updatePs.executeUpdate();
					updatePs.close();
				}				
				updatePs = conn.prepareStatement("UPDATE TSD_TSMETA SET FQN = FQN(FQNID) WHERE FQNID = ?"); 
				ps = conn.prepareStatement(FIND_FQNS_SQL);
				ps.setString(1, (String)newRow[0]);
				rset = ps.executeQuery();
				while(rset.next()) {
					updatePs.setLong(1, rset.getLong(1));
					updatePs.addBatch();
					updates++;
				}
				updatePs.executeBatch();
				updatePs.close();
			} finally {
				setUserDefinedVar(conn, TSD_CONN_TYPE, tsdConnType);
				if(rset!=null) try { rset.close(); } catch (Exception x) {/* No Op */}
				if(ps!=null) try { ps.close(); } catch (Exception x) {/* No Op */}
				if(updatePs!=null) try { updatePs.close(); } catch (Exception x) {/* No Op */}
			}
			long elapsed = et.elapsed();
			elapsedTimes.insert(elapsed);
			log.info("Updated [{}] TSMetas in [{}] ms.", updates, et.elapsedMs());			
		}

	}

}
