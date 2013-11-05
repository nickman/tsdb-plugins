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
package net.opentsdb.catalog.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import javax.sql.DataSource;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterUtils;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

/**
 * <p>Title: JDBCHelper</p>
 * <p>Description: JDBC helper utility class</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>net.opentsdb.catalog.util.JDBCHelper</code></p>
 */

public class JDBCHelper {
	private final DataSource ds;
	
	
	public void killDb() {
		Connection conn = null;
		PreparedStatement ps = null;
		try {
			conn = ds.getConnection();
			ps = conn.prepareStatement("DROP ALL OBJECTS");			
			ps.executeUpdate();
			if(!conn.getAutoCommit()) {
				conn.commit();
			}
		} catch (Exception e) {
			System.err.println("Drop Schema [TESTDB] Failed:" + e);
		} finally {
			try { ps.close(); } catch (Exception e) {}
			try { conn.close(); } catch (Exception e) {}
		}

	}

	/**
	 * Creates a new JDBCHelper
	 * @param ds The underlyng data source
	 */
	public JDBCHelper(DataSource ds) {
		super();
		this.ds = ds;
	}
	
	/**
	 * Executes the SQL script in the passed file.
	 * @param fileName The sql script to run
	 */
	public void runSql(String fileName) {
		executeUpdate("RUNSCRIPT FROM '" + fileName + "'");
	}
	
	/**
	 * Executes the SQL script read from the named classpath resource
	 * @param resourceNames The names of classpath resources containing a SQL script
	 */
	public void runSqlFromResource(String...resourceNames) {
		for(String resourceName : resourceNames) {
			if(resourceName==null || resourceName.trim().isEmpty()) continue;
			InputStream is = null;
			FileOutputStream fos = null;
			File f = null;
			try {
				is = JDBCHelper.class.getClassLoader().getResourceAsStream(resourceName);
				f = File.createTempFile("tmp", ".sql");
				fos = new FileOutputStream(f);
				byte[] buff = new byte[8096];
				int bytesRead = -1;
				while((bytesRead = is.read(buff))!=-1) {
					fos.write(buff, 0, bytesRead);
				}
				is.close();
				is = null;
				fos.flush();
				fos.close();
				fos = null;
				runSql(f.getAbsolutePath());
			} catch (Exception ex) {
				throw new RuntimeException("Failed to execute SQL Script from resource [" + resourceName + "]", ex);
			} finally {
				if(is!=null) try { is.close(); } catch (Exception e) {}
				if(fos!=null) try { fos.close(); } catch (Exception e) {}
				if(f!=null) f.delete();
			}
		}
	}
	
	/**
	 * Executes the passed SQL as an update and returns the result code
	 * @param sql The update SQL
	 * @return the result code
	 */
	public int executeUpdate(CharSequence sql) {
		Connection conn = null;
		PreparedStatement ps = null;
		try {
			conn = ds.getConnection();
			ps = conn.prepareStatement(sql.toString());
			int x = ps.executeUpdate();
			if(!conn.getAutoCommit()) {
				conn.commit();
			}
			return x;
		} catch (Exception e) {
			throw new RuntimeException("Update for [" + sql + "] failed", e);
		} finally {
			try { ps.close(); } catch (Exception e) {}
			try { conn.close(); } catch (Exception e) {}
		}
	}
	
	/**
	 * Creates a new JDBCHelper and returns it.
	 * @param ds The data source to use
	 * @param initSql Optional init SQL. Ignored if null
	 * @return the created JDBCHelper
	 */
	public static JDBCHelper getInstance(DataSource ds, String initSql) {
		JDBCHelper helper = new JDBCHelper(ds);
		if(initSql!=null) {
			helper.executeUpdate(initSql);
		}
		return helper;
	}
	
	/**
	 * Creates a new JDBCHelper and returns it.
	 * @param ds The data source to use
	 * @return the created JDBCHelper
	 */
	public static JDBCHelper getInstance(DataSource ds) {
		return getInstance(ds, null);
	}
	
	
	/**
	 * Issues a query for an int
	 * @param sql The SQL
	 * @param binds The bind values
	 * @return an int value
	 */
	public int templateQueryForInt(CharSequence sql, Object...binds) {
		NamedParameterJdbcTemplate template = new NamedParameterJdbcTemplate(ds);
		return template.queryForInt(sql.toString(), getBinds(sql.toString().trim().toUpperCase(), binds));
	}
	
	/**
	 * Issues a query for an Object
	 * @param <T> The expected return type
	 * @param sql The SQL
	 * @param clazz The expected return type
	 * @param binds The bind values
	 * @return an Object
	 */
	public <T> T templateQueryForObject(CharSequence sql, Class<T> clazz, Object...binds) {
		NamedParameterJdbcTemplate template = new NamedParameterJdbcTemplate(ds);
		return template.queryForObject(sql.toString(), getBinds(sql.toString().trim().toUpperCase(), binds), clazz);
	}
	
	
	/**
	 * Issues a named parameter query using numerical binds, starting at 0.
	 * @param sql The SQL
	 * @param binds The bind values
	 * @return an Object array of the results
	 */
	public Object[][] templateQuery(CharSequence sql, Object...binds) {		
		NamedParameterJdbcTemplate template = new NamedParameterJdbcTemplate(ds);
		final List<Object[]> results = template.query(sql.toString(), getBinds(sql.toString().trim().toUpperCase(), binds), new RowMapper<Object[]>(){
			int columnCount = -1;
			public Object[] mapRow(ResultSet rs, int rowNum) throws SQLException {
				if(columnCount==-1) columnCount = rs.getMetaData().getColumnCount();
				Object[] row = new Object[columnCount];
				for(int i = 0; i < columnCount; i++) {
					row[i] = rs.getObject(i+1);
				}
				return row;
			}			
		});
		Object[][] ret = new Object[results.size()][];
		int cnt = 0;
		for(Object[] arr: results) {
			ret[cnt] = arr;
			cnt++;
		}
		return ret;
	}
	
	/**
	 * Executes the update defined in the passed sql 
	 * @param sql The sql
	 * @param binds The bind values
	 * @return the number of rows updated
	 */
	public int execute(CharSequence sql, Object...binds) {
		NamedParameterJdbcTemplate template = new NamedParameterJdbcTemplate(ds);
		return template.update(sql.toString(),getBinds(sql.toString().trim().toUpperCase(), binds));
	}
	
	/**
	 * Batch executes the update define in the passed sql 
	 * @param sql The sql
	 * @param bindSets An array of bind value arrays
	 * @return an array containing the numbers of rows affected by each update in the batch
	 */
	public int[] batchExecute(CharSequence sql, Object[]...bindSets) {
		NamedParameterJdbcTemplate template = new NamedParameterJdbcTemplate(ds);
		SqlParameterSource[] sps = new SqlParameterSource[bindSets.length];
		for(int i = 0; i < bindSets.length; i++) {
			sps[i] = getBinds(sql.toString().trim().toUpperCase(),bindSets[i]);
		}		
		return template.batchUpdate(sql.toString(), sps);
	}
	
	
	/** Cache of SQL statement parameter types */
	protected static final Map<String, int[]> TYPE_CACHE = new ConcurrentHashMap<String, int[]>();
	
	/**
	 * Generates a SqlParameterSource for the passed SQL text and supplied binds
	 * @param sql The SQL to bind to
	 * @param binds The supplied variables to bind
	 * @return a SqlParameterSource
	 */
	public SqlParameterSource getBinds(String sql, final Object...binds)  {
		final MapSqlParameterSource sqlParamSource = new MapSqlParameterSource();
		int[] parameterTypes = TYPE_CACHE.get(sql);
		if(parameterTypes==null) {
			synchronized(TYPE_CACHE) {
				parameterTypes = TYPE_CACHE.get(sql);
				if(parameterTypes==null) {
					Connection conn = null;
					PreparedStatement ps = null;
					try {
						conn = ds.getConnection();
						ps = conn.prepareStatement(NamedParameterUtils.parseSqlStatementIntoString(sql).toString());
						ParameterMetaData pmd = ps.getParameterMetaData();
						int paramCount = pmd.getParameterCount();						
						if(paramCount>0 && (binds==null || binds.length != paramCount)) {
							throw new RuntimeException("Bind Count [" + (binds==null ? 0 : binds.length) + "] was not equal to parameter count [" + paramCount + "]");
						}
						parameterTypes = new int[paramCount];
						for(int i = 0; i < paramCount; i++) {
							parameterTypes[i] = pmd.getParameterType(i+1);							
						}			
					} catch (RuntimeException re) {
						throw re;
					} catch (Exception e) {
						throw new RuntimeException("Failed to get binds for [" + sql + "]", e);
					} finally {
						try { ps.close(); } catch (Exception e) {}
						try { conn.close(); } catch (Exception e) {}
					}
					
				}
				TYPE_CACHE.put(sql, parameterTypes);
			}
		}
		for(int i = 0; i < parameterTypes.length; i++) {
			sqlParamSource.addValue("" + i, binds[i], parameterTypes[i]);
		}
		return sqlParamSource;
	}
	
	/**
	 * @param sql
	 * @return
	 */
	public int queryForInt(CharSequence sql) {
		Object[][] result = query(sql);
		if(result.length==1 && result[0].length==1) {
			return ((Number)result[0][0]).intValue();
		} else {
			throw new RuntimeException("Query did not return 1 row and 1 column");
		}
	}
	
	/**
	 * Executes the passed SQL and returns the resulting rows maps of values keyed by column name within a map keyed by rownumber (starting with zero)  
	 * @param sql The SQL to execute
	 * @return the results
	 */
	public Map<Integer, Map<String, Object>> result(CharSequence sql) {
		Map<Integer, Map<String, Object>> results = new TreeMap<Integer, Map<String, Object>>();
		Map<Integer, String> colNumToName;
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rset = null;
		try {
			conn = ds.getConnection();
			ps = conn.prepareStatement(sql.toString());
			rset = ps.executeQuery();
			int colCount = rset.getMetaData().getColumnCount();
			colNumToName  = new HashMap<Integer, String>(colCount);
			ResultSetMetaData rsmd = rset.getMetaData();
			for(int i = 1; i <= colCount; i++) {
				colNumToName.put(i, rsmd.getColumnLabel(i));
			}
			int rowNum = 0;
			while(rset.next()) {
				Map<String, Object> row = new HashMap<String, Object>(colCount);
				results.put(rowNum, row);
				for(int i = 1; i <= colCount; i++) {
					row.put(colNumToName.get(i), rset.getObject(i));
				}
				rowNum++;
			}
			return results;
		} catch (Exception e) {
			throw new RuntimeException("Query for [" + sql + "] failed", e);
		} finally {
			try { rset.close(); } catch (Exception e) {}
			try { ps.close(); } catch (Exception e) {}
			try { conn.close(); } catch (Exception e) {}
		}
	}
	
	/**
	 * Executes the passed SQL and returns the results in a 2D object array
	 * @param sql The SQL query to executer
	 * @return the results of the query
	 */
	public Object[][] query(CharSequence sql) {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rset = null;
		
		Vector<Object[]> rows = new Vector<Object[]>();
		try {
			conn = ds.getConnection();
			ps = conn.prepareStatement(sql.toString());
			rset = ps.executeQuery();
			int colCount = rset.getMetaData().getColumnCount();
			while(rset.next()) {
				Object[] row = new Object[colCount];
				for(int i = 1; i <= colCount; i++) {
					row[i-1] = rset.getObject(i);
				}
				rows.add(row);
			}
			Object[][] result = new Object[rows.size()][];
			int cnt = 0;
			for(Object[] row: rows) {
				result[cnt] = row;
				cnt++;
			}
			return result;
		} catch (Exception e) {
			throw new RuntimeException("Query for [" + sql + "] failed", e);
		} finally {
			try { rset.close(); } catch (Exception e) {}
			try { ps.close(); } catch (Exception e) {}
			try { conn.close(); } catch (Exception e) {}
		}
		
	}
}
