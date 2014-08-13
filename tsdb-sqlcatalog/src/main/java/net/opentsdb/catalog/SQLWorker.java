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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.LoaderClassPath;
import javassist.Modifier;

import javax.sql.DataSource;

import org.h2.jdbcx.JdbcConnectionPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;

/**
 * <p>Title: SQLWorker</p>
 * <p>Description: A functional wrapper class for JDBC operations to handle all the messy stuff.
 * No magic here. Just raw, low level overloading.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>net.opentsdb.catalog.SQLWorker</code></p>
 */

@SuppressWarnings("restriction")
public class SQLWorker {
	/** The data source this SQLWorker will use */
	protected final DataSource dataSource;
	/** The binder factory for the data source */
	protected final BinderFactory binderFactory;
	/** The JDBC URL of the data source's database */
	protected final String dbUrl;
	
	/** Static class Logger */
	protected static final Logger log = LoggerFactory.getLogger(SQLWorker.class);
	
	/** A map of SQLWorkers keyed by their data source */
	protected static final Map<String, SQLWorker> workers = new ConcurrentHashMap<String, SQLWorker>();

	/** A class array of a ResultSet */
	protected static final Class<?>[] RSET_IFACE = {ResultSet.class};
	
	/**
	 * Acquires a SQLWorker for the passed DataSource
	 * @param dataSource The data source this SQLWorker will use
	 * @return The acquired SQLWorker
	 */
	public static SQLWorker getInstance(DataSource dataSource) {
		Connection conn = null;
		String url = null;
		try {
			conn = dataSource.getConnection();
			url = conn.getMetaData().getURL();
			SQLWorker worker = workers.get(url);
			if(worker==null) {
				synchronized(workers) {
					worker = workers.get(url);
					if(worker==null) {
						worker = new SQLWorker(url, dataSource);
						workers.put(url, worker);
						log.info("\n\t=======================================\n\tCreated SQLWorker for [{}]\n\t=======================================\n", url);
					}
				}
			}
			return worker;
		} catch (Exception ex) {
			throw new RuntimeException("Failed to get connection from datasource [" + dataSource + "]", ex);
		} finally {
			if(conn!=null) try { conn.close(); } catch (Exception x) { /* No Op */ }
		}
	}

	/**
	 * Creates a new SQLWorker
	 * @param dbUrl The URL of the DB
	 * @param dataSource The data source this SQLWorker will use
	 */
	private SQLWorker(String dbUrl, DataSource dataSource) {
		this.dataSource = dataSource;
		this.dbUrl = dbUrl;
		binderFactory = new BinderFactory(this.dataSource);
	}
	
	/**
	 * <p>Title: ResultSetHandler</p>
	 * <p>Description: Defines an object that can be passed into a SQLWorker query and handle the result row.</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>net.opentsdb.catalog.SQLWorker.ResultSetHandler</code></p>
	 */
	public static interface ResultSetHandler {
		/**
		 * Callback on the next row. Implementations should not call {@link ResultSet#next()} unless 
		 * it is intended to skip rows.
		 * @param rowId The row sequence id, starting at zero.
		 * @param rset The result set at the next logical row
		 * @return true to contine processing, false otherwise
		 */
		public boolean onRow(int rowId, ResultSet rset);
	}
	
	/**
	 * Creates a ResultSet proxy that will close the parent statement and connection when the result set is closed.
	 * @param rset The resultset to proxy
	 * @param st The parent statement
	 * @param conn The parent connection
	 * @return the resultset proxy
	 */
	protected static ResultSet getAutoCloseResultSet(final ResultSet rset, final Statement st, final Connection conn) {
		return (ResultSet) Proxy.newProxyInstance(st.getClass().getClassLoader(), RSET_IFACE, new InvocationHandler() {
			@Override
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				try {
					return method.invoke(rset, args);
				} finally {
					if("close".equals(method.getName())) {
						try { st.close(); } catch (Exception x) { /* No Op */ }
						if(conn!=null) try { conn.close(); } catch (Exception x) { /* No Op */ }
					}
				}
			}
		});
	}
	
	/**
	 * Executes the passed query and returns the actual connected result set, 
	 * meaning the caller should close the statemnt and connection via {@link #close(ResultSet)}.
	 * @param sqlText The SQL query
	 * @param args The query bind arguments
	 * @return A result set for the query
	 */	
	public ResultSet executeRawQuery(String sqlText, Object...args) {
		Connection conn = null;
		PreparedStatement ps = null;
		try {
			conn = dataSource.getConnection();
			ps = conn.prepareStatement(sqlText);
			binderFactory.getBinder(sqlText).bind(ps, args);
			return ps.executeQuery();
		} catch (Exception ex) {
			throw new RuntimeException("SQL Query Failure [" + sqlText + "]", ex);
		}				
	}
	
	/**
	 * Closes the passed resultset and the associated statement and connection
	 * @param rset The result set to close
	 */
	public void close(ResultSet rset) {
		try {
			Statement st = rset.getStatement();
			Connection conn = st.getConnection();
			try { rset.close(); } catch (Exception x) { /* No Op */ }
			try { st.close(); } catch (Exception x) { /* No Op */ }
			try { conn.close(); } catch (Exception x) { /* No Op */ }				
			
		} catch (Exception ex) {
			throw new RuntimeException("Failed to close result set resources", ex);
		}
	}
	
	/**
	 * Executes the passed query and returns a result set
	 * @param conn An optional connection. If not supplied, a new connection will be acquired, and closed when used.
	 * @param sqlText The SQL query
	 * @param disconnected true to read all rows and return a disconnected resultset, false to return the connected result set
	 * @param args The query bind arguments
	 * @return A result set for the query
	 */
	public ResultSet executeQuery(Connection conn, String sqlText, boolean disconnected, Object...args) {		
		PreparedStatement ps = null;
		ResultSet rset = null;
		final boolean newConn = conn==null;
		try {
			if(newConn) {
				conn = dataSource.getConnection();
			}
			ps = conn.prepareStatement(sqlText);
			binderFactory.getBinder(sqlText).bind(ps, args);
			rset = ps.executeQuery();
			if(disconnected) {
				TSDBCachedRowSetImpl crs = new TSDBCachedRowSetImpl();
				crs.populate(rset);
				return crs;
			}
			return getAutoCloseResultSet(rset, ps, newConn ? conn : null);
		} catch (Exception ex) {
			throw new RuntimeException("SQL Query Failure [" + sqlText + "]", ex);
		} finally {
			if(disconnected) {				
				if(rset!=null) try { rset.close(); } catch (Exception x) { /* No Op */ }
				if(ps!=null) try { ps.close(); } catch (Exception x) { /* No Op */ }
				if(newConn && conn!=null) try { conn.close(); } catch (Exception x) { /* No Op */ }				
			} 
		}		
	}
	
	/**
	 * Executes a query with a {@link ResultSetHandler} that handles the returned rows.
	 * @param conn An optional connection. If not supplied, a new connection will be acquired, and closed when used.
	 * @param sqlText The SQL query
	 * @param rowHandler The row handler to handle the rows returned
	 * @param args The bind variables
	 * @return the number of rows retrieved
	 */
	public int executeQuery(Connection conn, String sqlText, ResultSetHandler rowHandler, Object...args) {
		PreparedStatement ps = null;
		ResultSet rset = null;
		final boolean newConn = conn==null;
		try {
			if(newConn) {
				conn = dataSource.getConnection();
			}
			ps = conn.prepareStatement(sqlText);
			binderFactory.getBinder(sqlText).bind(ps, args);
			int rowId = 0;
			rset = ps.executeQuery();
			while(rset.next()) {
				if(!rowHandler.onRow(rowId, rset)) break;
			}
			return rowId+1;
		} catch (Exception ex) {
			throw new RuntimeException("SQL Query Failure [" + sqlText + "]", ex);
		} finally {
			if(rset!=null) try { rset.close(); } catch (Exception x) { /* No Op */ }
			if(ps!=null) try { ps.close(); } catch (Exception x) { /* No Op */ }
			if(newConn && conn!=null) try { conn.close(); } catch (Exception x) { /* No Op */ }				
		}		
	}
	
	/**
	 * Executes a query using a new connection with a {@link ResultSetHandler} that handles the returned rows.
	 * @param sqlText The SQL query
	 * @param rowHandler The row handler to handle the rows returned
	 * @param args The bind variables
	 * @return the number of rows retrieved
	 */
	public int executeQuery(String sqlText, ResultSetHandler rowHandler, Object...args) {
		return executeQuery(sqlText, rowHandler, args);
	}
	
	
	/**
	 * Executes the passed query and returns a result set
	 * @param sqlText The SQL query
	 * @param disconnected true to read all rows and return a disconnected resultset, false to return the connected result set
	 * @param args The query bind arguments
	 * @return A result set for the query
	 */
	public ResultSet executeQuery(String sqlText, boolean disconnected, Object...args) {
		return executeQuery(null, sqlText, disconnected, args);
	}
	
	/**
	 * Executes the passed update statement
	 * @param conn An optional connection. If not supplied, a new connection will be acquired, and closed when used.
	 * @param sqlText The update SQL text
	 * @param args The bind arguments
	 * @return either (1) the row count for SQL Data Manipulation Language (DML) statements or (2) 0 for SQL statements that return nothing 
	 */
	public int executeUpdate(Connection conn, String sqlText, Object...args) {
		boolean newConn = conn==null;
		PreparedStatement ps = null;
		try {
			if(newConn) {
				conn = dataSource.getConnection();
			}
			ps = conn.prepareStatement(sqlText);
			binderFactory.getBinder(sqlText).bind(ps, args);
			int updateResult = ps.executeUpdate();
			if(newConn) conn.commit();
			return updateResult;
		} catch (Exception ex) {
			throw new RuntimeException("SQL Update Failure [" + sqlText + "]", ex);
		} finally {
			if(ps!=null) try { ps.close(); } catch (Exception x) { /* No Op */ }
			if(newConn && conn!=null) try { conn.close(); } catch (Exception x) { /* No Op */ }
		}		
	}

	/**
	 * Executes the passed update statement
	 * @param sqlText The update SQL text
	 * @param args The bind arguments
	 * @return either (1) the row count for SQL Data Manipulation Language (DML) statements or (2) 0 for SQL statements that return nothing 
	 */
	public int executeUpdate(String sqlText, Object...args) {
		return executeUpdate(null, sqlText, args);
	}
	
	/**
	 * Executes the passed statement
	 * @param conn An optional connection. If not supplied, a new connection will be acquired, and closed when used.
	 * @param sqlText The update SQL text
	 * @param args The bind arguments
	 */
	public void execute(Connection conn, String sqlText, Object...args) {		
		PreparedStatement ps = null;
		boolean newConn = conn==null;
		try {
			if(newConn) {
				conn = dataSource.getConnection();
			}
			ps = conn.prepareStatement(sqlText);
			binderFactory.getBinder(sqlText).bind(ps, args);
			ps.execute();
			if(newConn) conn.commit();
		} catch (Exception ex) {
			throw new RuntimeException("SQL Update Failure [" + sqlText + "]", ex);
		} finally {
			if(ps!=null) try { ps.close(); } catch (Exception x) { /* No Op */ }
			if(newConn && conn!=null) try { conn.close(); } catch (Exception x) { /* No Op */ }
		}		
	}
	
	/**
	 * Executes the passed statement
	 * @param sqlText The update SQL text
	 * @param args The bind arguments
	 */
	public void execute(String sqlText, Object...args) {
		execute(null, sqlText, args);
	}
	
	

	/**
	 * Adds a batch to a prepared statement
	 * @param conn The current connection
	 * @param ps The prepared statement. If null, it will be prepared
	 * @param sqlText The SQL batch statement
	 * @param args The bind arguments
	 * @return the batched prepared statement
	 */
	public PreparedStatement batch(Connection conn, PreparedStatement ps, String sqlText, Object...args) {
		try {
			if(ps==null) {
				ps = conn.prepareStatement(sqlText);
			}
			binderFactory.getBinder(sqlText).bind(ps, args);
			ps.addBatch();
			return ps;
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
			throw new RuntimeException("SQL Batch Failure [" + sqlText + "]", ex);
		}				
	}

	/**
	 * Executes the passed query statement and returns the first column of the first row as a boolean
	 * @param conn An optional connection. If not supplied, a new connection will be acquired, and closed when used.
	 * @param sqlText The query SQL text
	 * @param args The bind arguments
	 * @return true if the first int from the first column of the first row is > 0, otherwise false
	 */
	public boolean sqlForBool(Connection conn, String sqlText, Object...args) {
		int x = sqlForInt(conn, sqlText, args);
		log.debug("sqlForBool [{}] --> [{}]", sqlText, x>0);
		return x > 0;
	}

	/**
	 * Executes the passed query statement and returns the first column of the first row as a boolean
	 * @param sqlText The query SQL text
	 * @param args The bind arguments
	 * @return true if the first int from the first column of the first row is > 0, otherwise false
	 */
	public boolean sqlForBool(String sqlText, Object...args) {
		return sqlForBool(null, sqlText, args);
	}
	
	
	/**
	 * Executes the passed query statement and returns the first column of the first row as an int
	 * @param sqlText The query SQL text
	 * @param args The bind argumentsclazz
	 * @return The int from the first column of the first row
	 */
	public int sqlForInt(String sqlText, Object...args) {
		return sqlForInt(null, sqlText, args);
	}
	
	/**
	 * Executes the passed query statement and returns the first column of the first row as a string
	 * @param conn An optional connection. If not supplied, a new connection will be acquired, and closed when used.
	 * @param sqlText The query SQL text
	 * @param args The bind arguments
	 * @return The string from the first column of the first row
	 */
	public String sqlForString(Connection conn, String sqlText, Object...args) {		
		PreparedStatement ps = null;
		ResultSet rset = null;
		boolean newConn = conn==null;
		try {
			if(newConn) {
				conn = dataSource.getConnection();
			}
			ps = conn.prepareStatement(sqlText);
			binderFactory.getBinder(sqlText).bind(ps, args);
			rset = ps.executeQuery();
			if(!rset.next()) return null;
			return rset.getString(1);
		} catch (Exception ex) {
			throw new RuntimeException("SQL Query Failure [" + sqlText + "]", ex);
		} finally {
			if(rset!=null) try { rset.close(); } catch (Exception x) { /* No Op */ }
			if(ps!=null) try { ps.close(); } catch (Exception x) { /* No Op */ }
			if(newConn && conn!=null) try { conn.close(); } catch (Exception x) { /* No Op */ }
		}
	}
	
	/**
	 * Executes the passed query statement and returns the first column of the first row as a string
	 * @param sqlText The query SQL text
	 * @param args The bind arguments
	 * @return The string from the first column of the first row
	 */
	public String sqlForString(String sqlText, Object...args) {		
		return sqlForString(null, sqlText, args);
	}
	/**
	 * Executes the passed query statement and returns the first column of the first row as an int
	 * @param conn An optional connection. If not supplied, a new connection will be acquired, and closed when used.
	 * @param sqlText The query SQL text
	 * @param args The bind arguments
	 * @return The int from the first column of the first row
	 */
	public int sqlForInt(Connection conn, String sqlText, Object...args) {		
		PreparedStatement ps = null;
		ResultSet rset = null;
		boolean newConn = conn==null;
		try {
			if(newConn) {
				conn = dataSource.getConnection();			
			}
			ps = conn.prepareStatement(sqlText);
			binderFactory.getBinder(sqlText).bind(ps, args);
			rset = ps.executeQuery();
			rset.next();
			return rset.getInt(1);
		} catch (Exception ex) {
			throw new RuntimeException("SQL Query Failure [" + sqlText + "]", ex);
		} finally {
			if(rset!=null) try { rset.close(); } catch (Exception x) { /* No Op */ }
			if(ps!=null) try { ps.close(); } catch (Exception x) { /* No Op */ }
			if(newConn && conn!=null) try { conn.close(); } catch (Exception x) { /* No Op */ }
		}
	}
	
	/**
	 * Executes the passed query statement and returns the first column of the first row as a long
	 * @param conn An optional connection. If not supplied, a new connection will be acquired, and closed when used.
	 * @param sqlText The query SQL text
	 * @param args The bind arguments
	 * @return The long from the first column of the first row
	 */
	public long sqlForLong(Connection conn, String sqlText, Object...args) {
		PreparedStatement ps = null;
		ResultSet rset = null;
		boolean newConn = conn==null;
		try {
			if(newConn) {
				conn = dataSource.getConnection();			
			}
			ps = conn.prepareStatement(sqlText);
			binderFactory.getBinder(sqlText).bind(ps, args);
			rset = ps.executeQuery();
			rset.next();
			return rset.getLong(1);
		} catch (Exception ex) {
			throw new RuntimeException("SQL Query Failure [" + sqlText + "]", ex);
		} finally {
			if(rset!=null) try { rset.close(); } catch (Exception x) { /* No Op */ }
			if(ps!=null) try { ps.close(); } catch (Exception x) { /* No Op */ }
			if(newConn && conn!=null) try { conn.close(); } catch (Exception x) { /* No Op */ }
		}
	}
	
	/**
	 * Executes the passed query statement and returns the first column of the first row as a long
	 * @param sqlText The query SQL text
	 * @param args The bind arguments
	 * @return The long from the first column of the first row
	 */
	public long sqlForLong(String sqlText, Object...args) {
		return sqlForLong(null, sqlText, args);
	}
	
	

	
	
	public static void main(String[] args) {
		// SELECT * FROM INFORMATION_SCHEMA.TYPE_INFO WHERE AUTO_INCREMENT = ?
		try {
			log("BinderTest");
			log("LF:%s", LoggerFactory.getILoggerFactory().getClass().getName());
			LoggerContext lc = (LoggerContext)LoggerFactory.getILoggerFactory();
			lc.getLogger(SQLWorker.class).setLevel(Level.DEBUG);
			JdbcConnectionPool pool = JdbcConnectionPool.create("jdbc:h2:mem:test", "sa", "sa");
			BinderFactory factory = new BinderFactory(pool);
			PreparedStatementBinder psb = factory.getBinder("SELECT * FROM INFORMATION_SCHEMA.TYPE_INFO WHERE AUTO_INCREMENT = ?");
			log("Binder: [%s]", psb);
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		}
	}
	
	public static void log(String format, Object...args) {
		System.out.println(String.format(format, args));
	}
	
	
	/**
	 * <p>Title: PreparedStatementBinder</p>
	 * <p>Description: Defines a class that binds typed arguments against a prepared statement</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>net.opentsdb.catalog.SQLWorker.PreparedStatementBinder</code></p>
	 */
	public static interface PreparedStatementBinder {
		/**
		 * Binds the passed arguments against the passed PreparedStatement
		 * @param ps The PreparedStatement to bind against
		 * @param args The arguments to bind
		 */
		public void bind(PreparedStatement ps, Object...args);
		
		/**
		 * Returns the SQL statement for this binder
		 * @return the SQL statement for this binder
		 */
		public String getSQL();
		
		/**
		 * Returns the number of times this binder has been executed
		 * @return the binder's execution count
		 */
		public long getExecutionCount();
	}
	
	/**
	 * <p>Title: BinderFactory</p>
	 * <p>Description: Factory class for generating PreparedStatementBinders for a given SQL statement</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>net.opentsdb.catalog.SQLWorker.BinderFactory</code></p>
	 */
	public static class BinderFactory {
		/** The Javassist classpool */
		protected final ClassPool classPool = new ClassPool();
		/** A cache of binders keyed by the SQL statement */
		protected final Map<String, PreparedStatementBinder> binders = new ConcurrentHashMap<String, PreparedStatementBinder>();
		/** A class naming key */
		protected final AtomicLong serial = new AtomicLong(0L);
		/** The datasource providing connections for this binder factory */
		protected final DataSource ds;
		/** The PreparedStatementBinder Ct Interface */
		protected final CtClass binderIface;
		/** The SQLException ctclass */
		protected final CtClass[] sqlEx;
		/** The String ctclass */
		protected final CtClass str;
		/** The AtomicLong ctclass */
		protected final CtClass atomicL;
		
		/** The Object ctclass */
		protected final CtClass obj;
		/** The Object toString ctmethod */
		protected final CtMethod toStr;
		
		
		
		
		/** The PreparedStatementBinder bind CtMethod */
		protected final CtMethod bindMethod;
		/** The PreparedStatementBinder getSQL CtMethod */
		protected final CtMethod getSqlMethod;
		/** The PreparedStatementBinder getExec CtMethod */
		protected final CtMethod getExecMethod;
		
		
		/**
		 * Creates a new BinderFactory
		 * @param ds The datasource providing connections for this binder factory
		 */
		public BinderFactory(DataSource ds) {
			this.ds = ds;
			Connection conn = null;
			Statement st = null;
			try {
				classPool.appendClassPath(new LoaderClassPath(PreparedStatementBinder.class.getClassLoader()));
				conn = this.ds.getConnection();
				st = conn.createStatement();
				classPool.appendClassPath(new LoaderClassPath(st.getClass().getClassLoader()));
				classPool.importPackage("java.util.concurrent.locks");
				binderIface = classPool.get(PreparedStatementBinder.class.getName());
				bindMethod = binderIface.getDeclaredMethod("bind");
				getExecMethod = binderIface.getDeclaredMethod("getExecutionCount");
				getSqlMethod = binderIface.getDeclaredMethod("getSQL");
				sqlEx = new CtClass[] {classPool.get(SQLException.class.getName())};
				str = classPool.get(String.class.getName());
				obj = classPool.get(Object.class.getName());
				atomicL = classPool.get(AtomicLong.class.getName());
				toStr = obj.getDeclaredMethod("toString");
			} catch (Exception ex) {
				throw new RuntimeException("Failed to create PreparedStatementBinder CtClass IFace", ex);
			} finally {
				if(st!=null) try { st.close(); } catch (Exception x) { /* No Op */ }
				if(conn!=null) try { conn.close(); } catch (Exception x) { /* No Op */ }
			}
		}
		
		/**
		 * Returns a PreparedStatementBinder for the passed SQL statement
		 * @param sqlText The SQL statement to create a binder for
		 * @return the built PreparedStatementBinder instance
		 */
		public PreparedStatementBinder getBinder(String sqlText) {
			PreparedStatementBinder psb = binders.get(sqlText);
			if(psb==null) {
				synchronized(binders) {
					psb = binders.get(sqlText);
					if(psb==null) {
						psb = buildBinder(sqlText);
						binders.put(sqlText, psb);
					}
				}
			}
			return psb;
		}
		
		/**
		 * Builds a PreparedStatementBinder
		 * @param sqlText The SQL statement to create a binder for
		 * @return the built PreparedStatementBinder instance
		 */
		protected PreparedStatementBinder buildBinder(String sqlText) {
			Connection conn = null;
			PreparedStatement ps = null;
			PreparedStatementBinder psb = null;
			try {
				final String className = "PreparedStatementBinder" + serial.incrementAndGet();
				conn = ds.getConnection();				
				ps = conn.prepareStatement(sqlText);
				ParameterMetaData pmd = ps.getParameterMetaData();
				
				
				
				CtClass binderClazz = classPool.makeClass(className);
				binderClazz.addInterface(binderIface);
				
				CtField sqlField = new CtField(str, "sqltext", binderClazz);
				sqlField.setModifiers(sqlField.getModifiers() | Modifier.FINAL);
				binderClazz.addField(sqlField, CtField.Initializer.constant(sqlText));
				
				CtField counterField = new CtField(atomicL, "execCounter", binderClazz);
				counterField.setModifiers(counterField.getModifiers() | Modifier.FINAL);
				binderClazz.addField(counterField, CtField.Initializer.byExpr("new java.util.concurrent.atomic.AtomicLong(0L)"));

				CtMethod execm = CtNewMethod.copy(getExecMethod, binderClazz, null);
				execm.setBody("{ return execCounter.get(); }");				
				execm.setModifiers(execm.getModifiers() & ~Modifier.ABSTRACT);
				binderClazz.addMethod(execm);

				
				CtMethod tosm = CtNewMethod.copy(toStr, binderClazz, null);
				tosm.setBody("{ return \"PreparedStatementBinder:\" + sqltext; }");				
				tosm.setModifiers(tosm.getModifiers() & ~Modifier.ABSTRACT);
				binderClazz.addMethod(tosm);
				
				CtMethod getm = CtNewMethod.copy(getSqlMethod, binderClazz, null);
				getm.setBody("{ return sqltext; }");
				getm.setModifiers(getm.getModifiers() & ~Modifier.ABSTRACT);
				binderClazz.addMethod(getm);
				
				CtMethod bindm = CtNewMethod.copy(bindMethod, binderClazz, null);
				bindm.setExceptionTypes(sqlEx);
				StringBuilder b = new StringBuilder("{ execCounter.incrementAndGet(); ");
				for(int i = 0; i < pmd.getParameterCount(); i++) {
					int sqlType = pmd.getParameterType(i+1);
					String objRef = new StringBuilder("$2[").append(i).append("]").toString(); 
					b.append("if($2[").append(i).append("]==null) {");
					b.append("$1.setNull(").append(i+1).append(", ").append(sqlType).append(");");
					b.append("} else {");
					b.append("$1.setObject(").append(i+1).append(", ").append(objRef).append(", ").append(sqlType).append(");");
//					if(log.isDebugEnabled()) {
//						String bindClass = pmd.getParameterClassName(i+1);
//						String dbType = pmd.getParameterTypeName(i+1);
//						b.append("\n\nSystem.out.println(\"debugbind: index:[").append((i+1)).append("] value:[\" + ").append(objRef).append(" + \"] bc: [").append(bindClass)
//						.append("] db: [").append(dbType).append("] SQL: [").append(sqlText)
//						.append("]\");");
//						
//					}
					b.append("}");
				}
				b.append("}");
				//log(b.toString());
				bindm.setBody(b.toString());
				bindm.setModifiers(bindm.getModifiers() & ~Modifier.ABSTRACT);
				binderClazz.addMethod(bindm);
				binderClazz.writeFile("/tmp/sqlworker/");
				@SuppressWarnings("unchecked")
				Class<PreparedStatementBinder> javaClazz = binderClazz.toClass(SQLWorker.class.getClassLoader(), SQLWorker.class.getProtectionDomain());
				psb = javaClazz.newInstance();
				binderClazz.detach();
				return psb;
			} catch (Exception ex) {
				throw new RuntimeException("Failed to build PreparedStatementBinder for statement [" + sqlText + "]", ex);
			} finally {
				if(ps!=null) try { ps.close(); } catch (Exception x) { /* No Op */ }
				if(conn!=null) try { conn.close(); } catch (Exception x) { /* No Op */ }				
			}
		}
		
	}
	
}
