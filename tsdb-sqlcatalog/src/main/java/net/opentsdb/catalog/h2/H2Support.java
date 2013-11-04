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

import java.sql.*;

/**
 * <p>Title: H2Support</p>
 * <p>Description: Static support functions for H2 DB</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>net.opentsdb.catalog.h2.H2Support</code></p>
 */

public class H2Support {
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
	
	
}
