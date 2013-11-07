/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2013, Helios Development Group and individual contributors
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
package net.opentsdb.catalog.h2;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import net.opentsdb.catalog.h2.json.JSONMapSupport;

import org.h2.api.Trigger;

/**
 * <p>Title: H2Support</p>
 * <p>Description: Static support functions for H2 DB</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>net.opentsdb.catalog.h2.H2Support</code></p>
 */

public class H2Support {
	
	/**
	 * Returns the named value from the map
	 * @param key The key to retrieve the value for
	 * @param jsonMap The JSON map source
	 * @return the bound value or null if one was not found
	 */
	public static String jsonGet(String key, String jsonMap) {
		return JSONMapSupport.getOrNull(key, jsonMap);
	}
	
	/**
	 * Returns the keys of the map as a string array
	 * @param jsonMap The JSON map source
	 * @return an array of the map's keys
	 */
	public static String[] jsonKeys(String jsonMap) {
		return JSONMapSupport.keys(jsonMap);
	}
	
	/**
	 * Returns the values of the map as a string array
	 * @param jsonMap The JSON map source
	 * @return an array of the map's values
	 */
	public static String[] jsonValues(String jsonMap) {
		return JSONMapSupport.keys(jsonMap);
	}
	
	/**
	 * Returns the keys and values of the map as a 2D string array
	 * @param jsonMap The JSON map source
	 * @return a 2D array of the map's keys and values
	 */
	public static String[][] jsonPairs(String jsonMap) {
		return JSONMapSupport.pairs(jsonMap);
	}
	
	/**
	 * Returns the FNQID of the TSMeta with the passed TSUID
	 * @param conn The DB connection
	 * @param tsuid The TSUID of the TSMeta to get the id for
	 * @return the FNQID of the TSMeta or -1 if the passed tsuid was null or empty
	 */
	public static long fqnId(Connection conn, String tsuid) {
		if(tsuid==null || tsuid.trim().isEmpty()) return -1;
		try {
			PreparedStatement ps = conn.prepareStatement("SELECT FQNID FROM TSD_FQN WHERE TSUID = ?");
			ps.setString(1, tsuid);
			ResultSet rs = ps.executeQuery();
			rs.next();
			return rs.getLong(1);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to find FQNID for TSUID [" + tsuid + "]", ex);
		}
	}
	
	/**
	 * Looks up the name of a TAGV
	 * @param conn The DB connection
	 * @param uid The TAGV UID
	 * @return The TAGV name
	 */
	public static String tagvName(Connection conn, String uid) {
		try {
			PreparedStatement ps = conn.prepareStatement("SELECT NAME FROM TSD_TAGV WHERE UID = ?");
			ps.setString(1, uid);
			ResultSet rs = ps.executeQuery();
			if(rs.next()) {
				return rs.getString(1);
			}
			return null;
		} catch (SQLException sex) {
			return null;
		}
	}

	/**
	 * Looks up the UID of a TAGV
	 * @param conn The DB connection
	 * @param name The TAGV name 
	 * @return The TAGV name
	 */
	public static String tagvUid(Connection conn, String name) {
		try {
			PreparedStatement ps = conn.prepareStatement("SELECT UID FROM TSD_TAGV WHERE NAME = ?");
			ps.setString(1, name);
			ResultSet rs = ps.executeQuery();
			if(rs.next()) {
				return rs.getString(1);
			}
			return null;
		} catch (SQLException sex) {
			return null;
		}
	}
	
	/**
	 * Looks up the name of a TAGK
	 * @param conn The DB connection
	 * @param uid The TAGK UID
	 * @return The TAGK name
	 */
	public static String tagkName(Connection conn, String uid) {
		try {
			PreparedStatement ps = conn.prepareStatement("SELECT NAME FROM TSD_TAGK WHERE UID = ?");
			ps.setString(1, uid);
			ResultSet rs = ps.executeQuery();
			if(rs.next()) {
				return rs.getString(1);
			}
			return null;
		} catch (SQLException sex) {
			return null;
		}
	}
	
	

	/**
	 * Looks up the UID of a TAGK
	 * @param conn The DB connection
	 * @param name The TAGK name 
	 * @return The TAGK name
	 */
	public static String tagkUid(Connection conn, String name) {
		try {
			PreparedStatement ps = conn.prepareStatement("SELECT UID FROM TSD_TAGK WHERE NAME = ?");
			ps.setString(1, name);
			ResultSet rs = ps.executeQuery();
			if(rs.next()) {
				return rs.getString(1);
			}
			return null;
		} catch (SQLException sex) {
			return null;
		}
	}
	
	
	/**
	 * Looks up the name of a METRIC
	 * @param conn The DB connection
	 * @param uid The metric UID
	 * @return The METRIC name
	 */
	public static String metricName(Connection conn, String uid) {
		try {
			PreparedStatement ps = conn.prepareStatement("SELECT NAME FROM TSD_METRIC WHERE UID = ?");
			ps.setString(1, uid);
			ResultSet rs = ps.executeQuery();
			if(rs.next()) {
				return rs.getString(1);
			}
			return null;
		} catch (SQLException sex) {
			return null;
		}
	}

	/**
	 * Looks up the UID of a METRIC
	 * @param conn The DB connection
	 * @param name The metric name 
	 * @return The metric name
	 */
	public static String metricUid(Connection conn, String name) {
		try {
			PreparedStatement ps = conn.prepareStatement("SELECT UID FROM TSD_METRIC WHERE NAME = ?");
			ps.setString(1, name);
			ResultSet rs = ps.executeQuery();
			if(rs.next()) {
				return rs.getString(1);
			}
			return null;
		} catch (SQLException sex) {
			return null;
		}
	}
	
	/**
	 * Retrieves the UID of the tagpair with the passed name, adding a new tagpair if not present.
	 * @param conn The JDBC connection
	 * @param name The name-value pair of the tag pair
	 * @return the id of the tag pair
	 */
	public static String tagPairUidByName(Connection conn, String name) {
		String[] nvp = splitPair(name.trim());
		return tagPairUidByNames(conn, nvp[0], nvp[1]);
	}
	
	/**
	 * Retrieves the UID of the tagpair with the passed name, adding a new tagpair if not present.
	 * @param conn The JDBC connection
	 * @param key The key the tag pair
	 * @param value The value the tag pair
	 * @return the id of the tag pair
	 */
	public static String tagPairUidByNames(Connection conn, String key, String value) {
		try {
			String name = String.format("%s=%s", key, value);
			PreparedStatement ps = conn.prepareStatement("SELECT UID FROM TSD_TAGPAIR WHERE NAME = ?");
			ps.setString(1, name);
			ResultSet rs = ps.executeQuery();
			if(rs.next()) {
				return rs.getString(1);
			}
			ps.close();
			
			String tagk = tagkUid(conn, key);
			String tagv = tagvUid(conn, value);
			String id = tagk + tagv;
			ps = conn.prepareStatement("INSERT INTO TSD_TAGPAIR (UID, TAGK, TAGV, NAME) VALUES (?,?,?,?)");

			ps.setString(1, id);
			ps.setString(2, tagk);
			ps.setString(3, tagv);
			ps.setString(4, name);
			ps.executeUpdate();
			return id;
		} catch (SQLException sex) {
			throw new RuntimeException("TAGPAIR Lookup failed", sex);
		}
	}
	
	/**
	 * Returns the tagk value for the tagpair with the passed UID
	 * @param conn The JDBC connection
	 * @param uid The UID of the target tagpair
	 * @return the tagk value
	 */
	public static String tagPairKeyNameByUid(Connection conn, String uid) {
		try {
			PreparedStatement ps = conn.prepareStatement("SELECT TAGKNAME(TAGK) FROM TSD_TAGPAIR WHERE UID = ?");
			ps.setString(1, uid);
			ResultSet rs = ps.executeQuery();
			if(rs.next()) {
				return rs.getString(1);
			}
			return null;
		} catch (SQLException sex) {
			throw new RuntimeException("TAGPAIR Key value Lookup failed", sex);
		}
	}
	
	/**
	 * Returns the tagv value for the tagpair with the passed UID
	 * @param conn The JDBC connection
	 * @param uid The UID of the target tagpair
	 * @return the tagv value
	 */
	public static String tagPairValueNameByUid(Connection conn, String uid) {
		try {
			PreparedStatement ps = conn.prepareStatement("SELECT TAGVNAME(TAGV) FROM TSD_TAGPAIR WHERE UID = ?");
			ps.setString(1, uid);
			ResultSet rs = ps.executeQuery();
			if(rs.next()) {
				return rs.getString(1);
			}
			return null;
		} catch (SQLException sex) {
			throw new RuntimeException("TAGPAIR Key value Lookup failed", sex);
		}
	}
	
	
	/**
	 * Splits a name-value pair into nae and value
	 * @param pair The name value pair, delimited by a "="
	 * @return an array with the name and value
	 */
	public static String[] splitPair(String pair) {
		int index = pair.indexOf('=');
		if(index==-1) throw new RuntimeException("The string [" + pair + "] is not a name value pair");
		String[] p = new String[2];
		p[0] = pair.substring(0, index);
		p[1] = pair.substring(index+1);
		return p;
	}
	
	public static class UpdateRowQueuePKTrigger implements Trigger {
		/** The updated table name the update went to */
		protected String eventSource = null;
		
		/** The queue table insert template */
		public static final String QUEUE_SQL_TEMPLATE = "INSERT INTO SYNC_QUEUE (EVENT_TYPE, EVENT_ID) VALUES (?,?)";
		
		
		/*
CREATE TABLE SYNC_QUEUE (
	QID BIGINT IDENTITY COMMENT 'The synthetic identifier for this sync operation',
	EVENT_TYPE VARCHAR(20) NOT NULL 
		COMMENT 'The source of the update that triggered this sync operation'
		CHECK EVENT_TYPE IN ('TSD_ANNOTATION', 'TSD_FQN', 'TSD_METRIC', 'TSD_TAGK', 'TSD_TAGV'), 
	EVENT_ID VARCHAR2(20) NOT NULL COMMENT 'The PK of the event that triggered this Sync Operation',
	LAST_SYNC_ATTEMPT TIMESTAMP COMMENT 'The last [failed] sync operation attempt timestamp',
	LAST_SYNC_ERROR CLOB COMMENT 'The exception trace of the last failed sync operation'
);
		 */
		
		/**
		 * {@inheritDoc}
		 * @see org.h2.api.Trigger#init(java.sql.Connection, java.lang.String, java.lang.String, java.lang.String, boolean, int)
		 */
		@Override
		public void init(Connection conn, String schemaName, String triggerName, String tableName, boolean before, int type) 				throws SQLException {
			this.eventSource = tableName;
			PreparedStatement ps = null;
			try {
				ps = conn.prepareStatement("SELECT COLUMN_LIST FROM ");
			} catch (Exception ex) {
				throw new RuntimeException("Failed to get PK for [" + tableName + "]", ex);
			} finally {
				if(ps!=null) try { ps.close(); } catch (Exception x) {/* No Op */}
			}
		}

		/**
		 * {@inheritDoc}
		 * @see org.h2.api.Trigger#fire(java.sql.Connection, java.lang.Object[], java.lang.Object[])
		 */
		@Override
		public void fire(Connection conn, Object[] oldRow, Object[] newRow)
				throws SQLException {
			// TODO Auto-generated method stub
			
		}

		/**
		 * {@inheritDoc}
		 * @see org.h2.api.Trigger#close()
		 */
		@Override
		public void close() throws SQLException {
			// TODO Auto-generated method stub
			
		}

		/**
		 * {@inheritDoc}
		 * @see org.h2.api.Trigger#remove()
		 */
		@Override
		public void remove() throws SQLException {
			// TODO Auto-generated method stub
			
		}
		
	}
	
	
}
