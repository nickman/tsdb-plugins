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
import java.sql.Timestamp;
import java.sql.Types;
import java.util.concurrent.TimeUnit;

import javax.management.ObjectName;

import net.opentsdb.catalog.h2.json.JSONMapSupport;

import org.h2.tools.SimpleResultSet;
import org.helios.tsdb.plugins.util.JMXHelper;

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
	 * Sets a value in the map
	 * @param jsonMap The JSON source of the map
	 * @param key The key to update the value for
	 * @param value The value to update to
	 * @return the updated json source
	 */
	public static String jsonSet(String jsonMap, String key, Object value) {
		return JSONMapSupport.set(key, value, jsonMap);
	}
	

	
	/**
	 * Returns the FNQID of the TSMeta with the passed TSUID
	 * @param conn The DB connection
	 * @param tsuid The TSUID of the TSMeta to get the id for
	 * @return the FNQID of the TSMeta or -1 if the passed tsuid was null or empty
	 */
	public static long fqnId(Connection conn, String tsuid) {
		if(tsuid==null || tsuid.trim().isEmpty()) return -1;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = conn.prepareStatement("SELECT FQNID FROM TSD_TSMETA WHERE TSUID = ?");
			ps.setString(1, tsuid);
			rs = ps.executeQuery();
			rs.next();
			return rs.getLong(1);
		} catch (Exception ex) {
			//throw new RuntimeException("Failed to find FQNID for TSUID [" + tsuid + "]", ex);
			return -1L;
		} finally {
			if(rs!=null) try { rs.close(); } catch (Exception x) {/* No Op */}
			if(ps!=null) try { ps.close(); } catch (Exception x) {/* No Op */}
		}
	}
	
	/**
	 * Returns the TSUID of the TSMeta with the passed FQNID
	 * @param conn The DB connection
	 * @param fqnId The FQNID of the TSMeta to get the tsuid for
	 * @return the TSUID of the TSMeta or null if one was not found
	 */
	public static String tsuid(Connection conn, long fqnId) {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = conn.prepareStatement("SELECT TSUID FROM TSD_TSMETA WHERE FQNID = ?");
			ps.setLong(1, fqnId);
			rs = ps.executeQuery();
			if(rs.next()) {
				return rs.getString(1);
			} else {
				return null;
			}
		} catch (Exception ex) {
			throw new RuntimeException("Failed to find TSUID for FQNID [" + fqnId + "]", ex);			
		} finally {
			if(rs!=null) try { rs.close(); } catch (Exception x) {/* No Op */}
			if(ps!=null) try { ps.close(); } catch (Exception x) {/* No Op */}
		}
	}	
	
	/**
	 * Looks up the name of a TAGV
	 * @param conn The DB connection
	 * @param uid The TAGV UID
	 * @return The TAGV name
	 */
	public static String tagvName(Connection conn, String uid) {
		PreparedStatement ps = null;
		ResultSet rs = null;		
		try {
			ps = conn.prepareStatement("SELECT NAME FROM TSD_TAGV WHERE XUID = ?");
			ps.setString(1, uid);
			rs = ps.executeQuery();
			if(rs.next()) {
				return rs.getString(1);
			}
			return null;
		} catch (SQLException sex) {
			return null;
		} finally {
			if(rs!=null) try { rs.close(); } catch (Exception x) {/* No Op */}
			if(ps!=null) try { ps.close(); } catch (Exception x) {/* No Op */}
		}
	}

	/**
	 * Looks up the UID of a TAGV
	 * @param conn The DB connection
	 * @param name The TAGV name 
	 * @return The TAGV name
	 */
	public static String tagvUid(Connection conn, String name) {
		PreparedStatement ps = null;
		ResultSet rs = null;				
		try {
			ps = conn.prepareStatement("SELECT XUID FROM TSD_TAGV WHERE NAME = ?");
			ps.setString(1, name);
			rs = ps.executeQuery();
			if(rs.next()) {
				return rs.getString(1);
			}
			return null;
		} catch (SQLException sex) {
			return null;
		} finally {
			if(rs!=null) try { rs.close(); } catch (Exception x) {/* No Op */}
			if(ps!=null) try { ps.close(); } catch (Exception x) {/* No Op */}
		}
	}
	
	/**
	 * Looks up the name of a TAGK
	 * @param conn The DB connection
	 * @param uid The TAGK UID
	 * @return The TAGK name
	 */
	public static String tagkName(Connection conn, String uid) {
		PreparedStatement ps = null;
		ResultSet rs = null;						
		try {
			ps = conn.prepareStatement("SELECT NAME FROM TSD_TAGK WHERE XUID = ?");
			ps.setString(1, uid);
			rs = ps.executeQuery();
			if(rs.next()) {
				return rs.getString(1);
			}
			return null;
		} catch (SQLException sex) {
			return null;
		} finally {
			if(rs!=null) try { rs.close(); } catch (Exception x) {/* No Op */}
			if(ps!=null) try { ps.close(); } catch (Exception x) {/* No Op */}
		}
	}
	
	

	/**
	 * Looks up the UID of a TAGK
	 * @param conn The DB connection
	 * @param name The TAGK name 
	 * @return The TAGK name
	 */
	public static String tagkUid(Connection conn, String name) {
		PreparedStatement ps = null;
		ResultSet rs = null;								
		try {
			ps = conn.prepareStatement("SELECT XUID FROM TSD_TAGK WHERE NAME = ?");
			ps.setString(1, name);
			rs = ps.executeQuery();
			if(rs.next()) {
				return rs.getString(1);
			}
			return null;
		} catch (SQLException sex) {
			return null;
		} finally {
			if(rs!=null) try { rs.close(); } catch (Exception x) {/* No Op */}
			if(ps!=null) try { ps.close(); } catch (Exception x) {/* No Op */}
		}
	}
	
	
	/**
	 * Looks up the name of a METRIC
	 * @param conn The DB connection
	 * @param uid The metric UID
	 * @return The METRIC name
	 */
	public static String metricName(Connection conn, String uid) {
		PreparedStatement ps = null;
		ResultSet rs = null;										
		try {
			ps = conn.prepareStatement("SELECT NAME FROM TSD_METRIC WHERE XUID = ?");
			ps.setString(1, uid);
			rs = ps.executeQuery();
			if(rs.next()) {
				return rs.getString(1);
			}
			return null;
		} catch (SQLException sex) {
			return null;
		} finally {
			if(rs!=null) try { rs.close(); } catch (Exception x) {/* No Op */}
			if(ps!=null) try { ps.close(); } catch (Exception x) {/* No Op */}
		}
	}

	/**
	 * Looks up the UID of a METRIC
	 * @param conn The DB connection
	 * @param name The metric name 
	 * @return The metric name
	 */
	public static String metricUid(Connection conn, String name) {
		PreparedStatement ps = null;
		ResultSet rset = null;
		try {
			ps = conn.prepareStatement("SELECT XUID FROM TSD_METRIC WHERE NAME = ?");
			ps.setString(1, name);
			rset = ps.executeQuery();
			if(rset.next()) {
				return rset.getString(1);
			}
			return null;
		} catch (SQLException sex) {
			return null;
		} finally {
			if(rset!=null) try { rset.close(); } catch (Exception x) {/* No Op */}
			if(ps!=null) try { ps.close(); } catch (Exception x) {/* No Op */}
		}
	}
	
	
	/**
	 * Looks up the ANNID of an Annotation
	 * @param conn The DB connection
	 * @param startTime The annotation start time in unix time 
	 * @param tsuid The optional TSUID of the associated TSMeta
	 * @return The annotation's ANNID
	 */
	public static Long annotationId(Connection conn, long startTime, String tsuid) {
		PreparedStatement ps = null;
		ResultSet rset = null;
		try {
			ps = conn.prepareStatement("SELECT ANNID FROM ANNOTATION WHERE START_TIME = ? AND (FQNID IS NULL OR FQNID = ?");
			ps.setTimestamp(1, new Timestamp(TimeUnit.MILLISECONDS.convert(startTime, TimeUnit.SECONDS)));
			if(tsuid==null) {
				ps.setNull(2, Types.BIGINT);
			} else {
				ps.setLong(2, fqnId(conn, tsuid));
			}
			rset = ps.executeQuery();
			if(rset.next()) {
				return rset.getLong(1);
			}
			return null;
		} catch (SQLException sex) {
			return null;
		} finally {
			if(rset!=null) try { rset.close(); } catch (Exception x) {/* No Op */}
			if(ps!=null) try { ps.close(); } catch (Exception x) {/* No Op */}
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
	
	/** The JMX ObjectName of the MBeanServer Delegate */
	public static final ObjectName JMI_IMPL_OBJECT_NAME = JMXHelper.objectName("JMImplementation:type=MBeanServerDelegate");
	/** The MBeanServer delegate attribute name for the server id */
	public static final String SERVER_ID_ATTR = "MBeanServerId";
	
	
	/**
	 * Returns the MBeanServerId of the current JVM's default MBeanServer.
	 * Used to determine if H2 is running in-jvm or remote
	 * @return the MBeanServerId of the current JVM's default MBeanServer.
	 */
	public static String getMBeanServerId() {
		return (String)JMXHelper.getAttribute(JMI_IMPL_OBJECT_NAME, SERVER_ID_ATTR);
	}
	
	/**
	 * Retrieves the value of the named user defined variable from the passed connection
	 * @param conn The connection to get the user defined variable from 
	 * @param key The key of the user defined variable
	 * @param type The expected type of the returned value
	 * @return The value of the variable or null if the name was not bound (or null was bound to the name)
	 */
	public static <T> T getUserDefinedVar(Connection conn, String key, Class<T> type) {
		return getUserDefinedVar(conn, key, null, type);
	}
	
	
	/**
	 * Retrieves the value of the named user defined variable from the passed connection
	 * @param conn The connection to get the user defined variable from 
	 * @param key The key of the user defined variable
	 * @param defaultValue The default value to return if the name is not bound or has a value of null
	 * @param type The expected type of the returned value
	 * @return The value of the variable or null if the name was not bound (or null was bound to the name)
	 */
	public static <T> T getUserDefinedVar(Connection conn, String key, T defaultValue, Class<T> type) {
		PreparedStatement ps = null;
		ResultSet rset = null;
		try {
			ps = conn.prepareStatement("SELECT @" + key);
			rset = ps.executeQuery();
			rset.next();
			Object obj = rset.getObject(1);
			if(obj==null) return defaultValue;
			return type.cast(obj);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to get UserDefinedVar [" + key + "] from session", ex);
		} finally {
			if(rset!=null) try { rset.close(); } catch (Exception x) {/* No Op */}
			if(ps!=null) try { ps.close(); } catch (Exception x) {/* No Op */}
		}
	}
	
	/**
	 * Builds and returns the fully qualified metric name for the passed TSMeta ID
	 * @param conn The connection to query on
	 * @param fqnId The TSMeta pk
	 * @return the fully qualified name
	 */
	public static String getMetricNameForFQN(Connection conn, long fqnId) {
		PreparedStatement ps = null;
		ResultSet rset = null;
		try {
			StringBuilder b = new StringBuilder();
			ps = conn.prepareStatement("SELECT -1, NAME FROM TSD_METRIC M, TSD_TSMETA  T " +  
					"WHERE T.METRIC_UID = M.XUID AND T.FQNID = ? " + 
					"UNION ALL " +
					"SELECT T.PORDER, P.NAME FROM TSD_TAGPAIR P, TSD_FQN_TAGPAIR T, TSD_TSMETA M " +  
					"WHERE P.XUID= T.XUID AND T.FQNID = M.FQNID AND M.FQNID = ? " +
					"ORDER BY 1");
			ps.setLong(1, fqnId);
			ps.setLong(2, fqnId);
			rset = ps.executeQuery();
			while(rset.next()) {
				b.append(rset.getString(2));
				if(rset.getInt(1)==-1) {
					b.append(":");
				} else {
					b.append(",");
				}
			}
			rset.close(); ps.close();
			return b.deleteCharAt(b.length()-1).toString();
		} catch (SQLException sex) {
			return null;
		} finally {
			if(rset!=null) try { rset.close(); } catch (Exception x) {/* No Op */}
			if(ps!=null) try { ps.close(); } catch (Exception x) {/* No Op */}
		}
	}
	
	
	/**
	 * Builds the name for a tagpair xuid
	 * @param conn The DB connection
	 * @param xuid The pk for the tag pair
	 * @return The built name
	 */
	public static String getNameForTagPair(Connection conn, String xuid) {
		PreparedStatement ps = null;
		ResultSet rset = null;
		try {
			StringBuilder b = new StringBuilder();
			ps = conn.prepareStatement(
					"SELECT 0, K.NAME FROM TSD_TAGPAIR T, TSD_TAGK K WHERE K.XUID = T.TAGK AND T.XUID = ? " + 
					"UNION ALL " + 
					"SELECT 1, V.NAME FROM TSD_TAGPAIR T, TSD_TAGV V WHERE V.XUID = T.TAGV AND T.XUID = ? " +
					"ORDER BY 1"					
			);
			ps.setMaxRows(2);
			ps.setString(1, xuid);
			ps.setString(2, xuid);
			rset = ps.executeQuery();
			while(rset.next()) {
				b.append(rset.getString(2));
				if(rset.getInt(1)==0) {
					b.append("=");
				}
			}
			rset.close(); ps.close();
			
			return b.toString();
		} catch (SQLException sex) {
			return null;
		} finally {
			if(rset!=null) try { rset.close(); } catch (Exception x) {/* No Op */}
			if(ps!=null) try { ps.close(); } catch (Exception x) {/* No Op */}
		}
	}
	
	/**
	 * Builds a result set displaying the TSDB synchronization status
	 * @param conn The connection to use
	 * @return A result set containing rows with each table name, the last sync update and the max last_update in the table.
	 */
	public static ResultSet getTSDBSyncStatus(Connection conn) {
		PreparedStatement outerPs = null, innerPs = null;
		ResultSet outerRs = null, innerRs = null;
		SimpleResultSet srs = new SimpleResultSet();
		srs.addColumn("TABLE_NAME", Types.VARCHAR, 0, 0);
		srs.addColumn("LAST_SYNC", Types.TIMESTAMP, 0, 0);
		srs.addColumn("MAX_LAST_UPDATED", Types.TIMESTAMP, 0, 0);
		
		try {
			outerPs = conn.prepareStatement("SELECT TABLE_NAME, LAST_SYNC FROM TSD_LASTSYNC ORDER BY ORDERING");
			
			outerRs = outerPs.executeQuery();
			while(outerRs.next()) {
				String tableName = outerRs.getString(1);
				Timestamp lastSync = outerRs.getTimestamp(2);
				innerPs = conn.prepareStatement("SELECT MAX(LAST_UPDATE) FROM " + tableName);
				innerRs = innerPs.executeQuery();
				innerRs.next();
				Timestamp maxSync = innerRs.getTimestamp(1);
				innerRs.close(); innerRs = null;
				innerPs.close(); innerPs = null;
				srs.addRow(tableName, lastSync, maxSync);
			}
			return srs;
		} catch (Exception ex) {
			throw new RuntimeException("Failed to get TSDBSyncStatus Summary", ex);
		} finally {
			if(outerRs!=null) try { outerRs.close(); } catch (Exception x) {/* No Op */}
			if(innerRs!=null) try { innerRs.close(); } catch (Exception x) {/* No Op */}
			if(outerPs!=null) try { outerPs.close(); } catch (Exception x) {/* No Op */}
			if(innerPs!=null) try { innerPs.close(); } catch (Exception x) {/* No Op */}
		}
		
	}

	
	
	
	
}
