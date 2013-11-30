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

import static net.opentsdb.catalog.CatalogDBInterface.EQ_CONN_FLAG;
import static net.opentsdb.catalog.CatalogDBInterface.SYNC_CONN_FLAG;
import static net.opentsdb.catalog.CatalogDBInterface.TSD_CONN_TYPE;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

import net.opentsdb.catalog.CatalogDBInterface;
import net.opentsdb.catalog.h2.json.JSONMapSupport;



/**
 * <p>Title: AbstractSyncQueueTrigger</p>
 * <p>Description: Abstract trigger impl as base class for triggers writing ops to the Sync Queue</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>net.opentsdb.catalog.h2.triggers.AbstractSyncQueueTrigger</code></p>
 */

public abstract class AbstractSyncQueueTrigger extends AbstractTrigger {
	

	/** The queue table insert template */
	public static final String QUEUE_SQL_TEMPLATE = "INSERT INTO SYNC_QUEUE (EVENT_TYPE, EVENT, OP_TYPE) VALUES (?,?,?)";
	
	
	/**
	 * Indicates if the connection is from the Event Queue Processor
	 * @param conn the connection to test 
	 * @return true if the connection is from the Event Queue Processor, false otherwise
	 */
	protected boolean isEQProcessor(Connection conn) {
		return EQ_CONN_FLAG.equals(getConnectionProperty(conn, TSD_CONN_TYPE, null));
	}
	
	/**
	 * Indicates if the connection is from the Sync Queue Processor
	 * @param conn the connection to test 
	 * @return true if the connection is from the Sync Queue Processor, false otherwise
	 */
	protected boolean isSQProcessor(Connection conn) {
		return SYNC_CONN_FLAG.equals(getConnectionProperty(conn, TSD_CONN_TYPE, null));
	}
	
	
	/**
	 * Writes a new SyncQueue event to the SyncQueue
	 * @param conn The connection to write with
	 * @param eventType The event type
	 * @param opType The operation type 
	 * @param pk A map of named values comprising the primary key
	 * @throws SQLException
	 */
	protected void addSyncQueueEvent(Connection conn, String eventType, String opType, String pk) throws SQLException {
		PreparedStatement ps = null;
		try {
			ps = conn.prepareStatement(QUEUE_SQL_TEMPLATE);
			ps.setString(1, eventType);
			ps.setString(2, pk);
			ps.setString(3, opType);
			ps.executeUpdate();
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
			throw new RuntimeException(ex);
		} finally {
			if(ps!=null) try { ps.close(); } catch (Exception x) {/* No Op */}
		}
	}
	
	/**
	 * Converts the passed PK map to a json string
	 * @param pk The pk to jsonize
	 * @return the JSON string representing the names values comprising the pk
	 */
	protected String pkToJSON(Map<String, Object> pk) {
		if(pk==null || pk.isEmpty()) throw new RuntimeException("PK Map was null or empty");
		StringBuilder b = new StringBuilder("{");
		for(Map.Entry<String, Object> entry: pk.entrySet()) {
			b.append("\"").append(entry.getKey()).append("\":").append("\"").append(entry.getValue()).append("\",");
		}
		b.deleteCharAt(b.length()-1);
		return b.append("}").toString();
	}

	/**
	 * Retrieves a connection property value from the passed connection
	 * @param conn The connection to read from
	 * @param key The property key
	 * @param defaultValue Thye value to return if the property is not defined or is set to null
	 * @return the read property value
	 */
	public String getConnectionProperty(Connection conn, String key, String defaultValue) {
		Statement st = null;
		ResultSet rset = null;
		try {
			st = conn.createStatement();
			rset = st.executeQuery("SELECT @" + key);
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
	 * Increments the numeric version column by 1
	 * @param newRow The new row trigger supplied object array
	 */
	protected void incrementVersion(Object[] newRow) {
		newRow[1] = ((Integer)newRow[1])+1;
	}
	
	/**
	 * Increments the custom JSON map version entry by 1
	 * @param col The column id of the custom JSON map 
	 * @param newRow The new row trigger supplied object array
	 */
	protected void incrementVersion(int col, Object[] newRow) {		
		Map<String, String> map = JSONMapSupport.read((String)newRow[col]);
		if(map.containsKey(CatalogDBInterface.VERSION_KEY)) {
			newRow[col] = JSONMapSupport.increment(map, 1, CatalogDBInterface.VERSION_KEY);
		} else {
			newRow[col] = JSONMapSupport.set(CatalogDBInterface.VERSION_KEY, 1, map);
		}
	}


}
