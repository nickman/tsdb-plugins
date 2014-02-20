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

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.opentsdb.meta.Annotation;
import net.opentsdb.meta.TSMeta;
import net.opentsdb.meta.UIDMeta;
import net.opentsdb.search.SearchQuery;

import org.h2.fulltext.FullTextLucene;
import org.h2.tools.Server;
import org.helios.tsdb.plugins.util.ConfigurationHelper;
import org.helios.tsdb.plugins.util.SystemClock;
import org.helios.tsdb.plugins.util.SystemClock.ElapsedTime;
import org.helios.tsdb.plugins.util.URLHelper;

import com.stumbleupon.async.Deferred;

/**
 * <p>Title: H2DBCatalog</p>
 * <p>Description: DB catalog interface for H2 DB</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>net.opentsdb.catalog.H2DBCatalog</code></p>
 */

public class H2DBCatalog extends AbstractDBCatalog {
	
	/** The H2 DDL resource */
	protected String[] ddlResources = null;	
	
	
	
	/** The H2 TCP Port */
	protected int tcpPort = -1;
	/** The H2 TCP Allow Others */
	protected boolean tcpAllowOthers = false;
	/** The H2 HTTP Port */
	protected int httpPort = -1;
	/** The H2 HTTP Allow Others */
	protected boolean httpAllowOthers = false;
	
	/** Indicates if lucene should be disabled */
	boolean disableLucene = false;
	
	/** The container/manager for the TCP Listener */
	protected Server tcpServer = null;
	/** The container/manager for the Web Listener */
	protected Server httpServer = null;
	
	
	/** The config property name for the H2 Init DDL resource */
	public static final String DB_H2_DDL = "helios.search.catalog.h2.ddl";
	/** The default H2 Init DDL resource */
	public static final String DEFAULT_DB_H2_DDL = "classpath:/ddl/h2/catalog.sql";
	
	
	/** The config property name for the H2 TCP Listener port */
	public static final String DB_H2_TCP_PORT = "helios.search.catalog.h2.port.tcp";
	/** The default H2 TCP Listener port*/
	public static final int DEFAULT_DB_H2_TCP_PORT = 9092;
	/** The config property name for the H2 TCP Listener accessibility from other hosts */
	public static final String DB_H2_TCP_ALLOW_OTHERS = "helios.search.catalog.h2.allowothers.tcp";
	/** The default H2 TCP Listener port*/
	public static final boolean DEFAULT_DB_H2_TCP_ALLOW_OTHERS = false;
	
	/** The config property name for the H2 HTTP Console Listener port */
	public static final String DB_H2_HTTP_PORT = "helios.search.catalog.h2.port.http";
	/** The default H2 HTTP Console Listener port*/
	public static final int DEFAULT_DB_H2_HTTP_PORT = 8082;
	/** The config property name for the H2 HTTP Console Listener accessibility from other hosts */
	public static final String DB_H2_HTTP_ALLOW_OTHERS = "helios.search.catalog.h2.allowothers.http";
	/** The default H2 HTTP Console Listener port*/
	public static final boolean DEFAULT_DB_H2_HTTP_ALLOW_OTHERS = false;
	
	
	/** The name of the H2 user defined variable specifying the increment size of the FQN sequence  */
	public static final String FQN_SEQ_SIZE = "FQN_SEQ_SIZE";
	/** The name of the H2 user defined variable specifying the increment size of the FQN TagPair sequence  */
	public static final String FQN_TP_SEQ_SIZE = "FQN_TP_SEQ_SIZE";
	/** The name of the H2 user defined variable specifying the increment size of the annotation sequence  */
	public static final String ANN_SEQ_SIZE = "ANN_SEQ_SIZE";
	/** The name of the H2 user defined variable specifying the increment size of the annotation sequence  */
	public static final String QID_SEQ_SIZE = "QID_SEQ_SIZE";

    /** The union all SQL clause */
    public static final String UNION_CLAUSE = " UNION ALL";
    /** The number of chars in the UNION_CLAUSE */
    public static final int UNION_LENGTH = UNION_CLAUSE.length();
    /** The select all SQL clause */
    public static final String SELECT_ALL = "SELECT * FROM ";

    /** The key of the user defined var to flag a connection as the event queue processor */
    public static final String EQ_CONN_FLAG = "eqprocessor";
    /** The key of the user defined var to flag a connection as the sync queue processor */
    public static final String SYNC_CONN_FLAG = "syncprocessor";
    
	/** The config property name to disable the lucene search engine in H2 */
	public static final String DB_H2_DISABLE_LUCENE = "helios.search.catalog.h2.lucene.disable";
	/** The default indicator for disabling the lucene search engine in H2 */
	public static final boolean DEFAULT_DB_H2_DISABLE_LUCENE = false;
	

    
    
	/**
	 * Runs the initialization routine
	 */
	@Override
	protected void doInitialize() {
		Map<String, Object> userDefinedVars = new HashMap<String, Object>();
		
		ddlResources = ConfigurationHelper.getSystemThenEnvPropertyArray(DB_H2_DDL, DEFAULT_DB_H2_DDL, extracted);
		tcpPort = ConfigurationHelper.getIntSystemThenEnvProperty(DB_H2_TCP_PORT, DEFAULT_DB_H2_TCP_PORT, extracted);
		tcpAllowOthers = ConfigurationHelper.getBooleanSystemThenEnvProperty(DB_H2_TCP_ALLOW_OTHERS, DEFAULT_DB_H2_TCP_ALLOW_OTHERS, extracted);
		httpPort = ConfigurationHelper.getIntSystemThenEnvProperty(DB_H2_HTTP_PORT, DEFAULT_DB_H2_HTTP_PORT, extracted);
		httpAllowOthers = ConfigurationHelper.getBooleanSystemThenEnvProperty(DB_H2_HTTP_ALLOW_OTHERS, DEFAULT_DB_H2_HTTP_ALLOW_OTHERS, extracted);
		disableLucene = ConfigurationHelper.getBooleanSystemThenEnvProperty(DB_H2_DISABLE_LUCENE, DEFAULT_DB_H2_DISABLE_LUCENE, extracted);
		long fqnSeqIncrement = ConfigurationHelper.getIntSystemThenEnvProperty(DB_FQN_SEQ_INCR, DEFAULT_DB_FQN_SEQ_INCR, extracted);
		long fqnTagPairSeqIncrement = ConfigurationHelper.getIntSystemThenEnvProperty(DB_TP_FQN_SEQ_INCR, DEFAULT_DB_TP_FQN_SEQ_INCR, extracted);
		long fqnAnnSeqIncrement = ConfigurationHelper.getIntSystemThenEnvProperty(DB_ANN_SEQ_INCR, DEFAULT_DB_ANN_SEQ_INCR, extracted);
		userDefinedVars.put(FQN_SEQ_SIZE, fqnSeqIncrement);
		userDefinedVars.put(FQN_TP_SEQ_SIZE, fqnTagPairSeqIncrement);
		userDefinedVars.put(ANN_SEQ_SIZE, fqnAnnSeqIncrement);
		log.info("Processing DDL Resources:{}", Arrays.toString(ddlResources));
		runDDLResources(userDefinedVars);
		log.info("DDL Resources Processed");
		if(ConfigurationHelper.getBooleanSystemThenEnvProperty("debug.catalog.daemon", false)) {
			tcpPort = DEFAULT_DB_H2_TCP_PORT;
			httpPort = DEFAULT_DB_H2_HTTP_PORT;
		}		
		if(tcpPort>0 || httpPort >0) {
			log.info("Starting H2 Listeners: TCP:{}, Web:{}", tcpPort>0, httpPort>0);
			startServers();
		} else {
			log.info("No H2 Listeners Configured");
		}
		Connection conn = null;
		if(!disableLucene) {
			try {
				conn = dataSource.getConnection();
				FullTextLucene.init(conn);
				FullTextLucene.createIndex(conn, "PUBLIC", "TSD_TSMETA", null);
				FullTextLucene.createIndex(conn, "PUBLIC", "TSD_METRIC", null);
				FullTextLucene.createIndex(conn, "PUBLIC", "TSD_TAGK", null);
				FullTextLucene.createIndex(conn, "PUBLIC", "TSD_TAGV", null);
				FullTextLucene.createIndex(conn, "PUBLIC", "TSD_ANNOTATION", null);
			} catch (SQLException e) {
				throw new RuntimeException("Failed to initialize Lucene Text Search for H2DBCatalog", e);
			} finally {
				if(conn!=null) try { conn.close(); } catch (Exception ex) {/* No Op */}
			}
		}
	}
	
	
	
	
	/**
	 * Terminates the database resources
	 */
	@Override
	protected void doShutdown() {
		if(tcpServer!=null) {
			log.info("Stopping TCP Server.....");
			try { 
				tcpServer.stop(); 
				tcpServer = null;
				log.info("TCP Server Stopped");
			} catch (Exception ex) {/* No Op */}  
		}
		if(httpServer!=null) {
			log.info("Stopping Web Server.....");
			try { 
				httpServer.stop(); 
				httpServer = null;
				log.info("Web Server Stopped");
			} catch (Exception ex) {/* No Op */}  
		}		
		Connection conn = null;
		try {
			conn = dataSource.getConnection();
			FullTextLucene.dropAll(conn);
		} catch (Exception ex) {
			log.warn("Failed to uninstall full-text search", ex);
		} finally {
			if(conn!=null) try { conn.close(); } catch (Exception x) {/* No Op */}
		}
	}
	
    
    /**
     * {@inheritDoc}
     * @see net.opentsdb.catalog.CatalogDBInterface#executeSearch(java.sql.Connection, net.opentsdb.search.SearchQuery)
     */
	@Override
    public List<?> executeSearch(Connection conn, SearchQuery query) {
		if(disableLucene) return Collections.EMPTY_LIST;
		PreparedStatement ps = null;
		ResultSet rset = null;
		List<?> results = null;
		ElapsedTime et = SystemClock.startClock();
		try {
			switch(query.getType()) {
			case TSMETA:
			case TSMETA_SUMMARY:
			case TSUIDS:
				ps = conn.prepareStatement("SELECT * FROM TSD_TSMETA T , (SELECT * FROM FTL_SEARCH_DATA(?, ?, ?) WHERE TABLE = ?) F  WHERE T.FQNID = F.KEYS[0]");
				ps.setString(1, query.getQuery());
				ps.setInt(2, query.getLimit());
				ps.setInt(3, query.getStartIndex());
				ps.setString(4, "TSD_TSMETA");
				rset = ps.executeQuery();				
				results =  readTSMetas(rset);	
				break;
			case ANNOTATION:
				ps = conn.prepareStatement("SELECT * FROM TSD_ANNOTATION T , (SELECT * FROM FTL_SEARCH_DATA(?, ?, ?) WHERE TABLE = ?) F  WHERE T.ANNID = F.KEYS[0]");
				ps.setString(1, query.getQuery());
				ps.setInt(2, query.getLimit());
				ps.setInt(3, query.getStartIndex());
				ps.setString(4, "TSD_ANNOTATION");
				rset = ps.executeQuery();
				results = readAnnotations(rset);
				break;
			case UIDMETA:
				List<UIDMeta> matches = new ArrayList<UIDMeta>();
				ps = conn.prepareStatement("SELECT * FROM TSD_TAGK T , (SELECT * FROM FTL_SEARCH_DATA(?, ?, ?) WHERE TABLE = ?) F  WHERE T.XUID = F.KEYS[0]");
				ps.setString(1, query.getQuery());
				ps.setInt(2, query.getLimit());
				ps.setInt(3, query.getStartIndex());
				ps.setString(4, "TSD_TAGK");
				rset = ps.executeQuery();
				matches.addAll(readUIDMetas(rset));
				rset.close(); ps.close();

				ps = conn.prepareStatement("SELECT * FROM TSD_TAGV T , (SELECT * FROM FTL_SEARCH_DATA(?, ?, ?) WHERE TABLE = ?) F  WHERE T.XUID = F.KEYS[0]");
				ps.setString(1, query.getQuery());
				ps.setInt(2, query.getLimit());
				ps.setInt(3, query.getStartIndex());
				ps.setString(4, "TSD_TAGV");
				rset = ps.executeQuery();
				matches.addAll(readUIDMetas(rset));
				rset.close(); ps.close();

				ps = conn.prepareStatement("SELECT * FROM TSD_METRIC T , (SELECT * FROM FTL_SEARCH_DATA(?, ?, ?) WHERE TABLE = ?) F  WHERE T.XUID = F.KEYS[0]");
				ps.setString(1, query.getQuery());
				ps.setInt(2, query.getLimit());
				ps.setInt(3, query.getStartIndex());
				ps.setString(4, "TSD_METRIC");
				rset = ps.executeQuery();
				matches.addAll(readUIDMetas(rset));
				rset.close(); ps.close();				
				results = matches;	
				break;
			default:
				throw new RuntimeException("yeow. Unrecognized type [" + query.getType() + "]");				
			}
			query.setTime(et.elapsedMs());
			query.setResults((List<Object>) results);
			return results;
		} catch (SQLException sex) {
			throw new RuntimeException("Failed to execute search on query [" + query + "]", sex);
		} finally {
			if(rset!=null) try { rset.close(); } catch (Exception x) {/* No Op */}
			if(ps!=null) try { ps.close(); } catch (Exception x) {/* No Op */}
		}
    }
    
	
	
	/**
	 * Starts the H2 Web and TCP listeners if configured
	 */
	protected void startServers() {
		List<String> args = new ArrayList<String>();
		if(tcpPort>0) {
			args.addAll(Arrays.asList("-tcp", "-tcpDaemon", "-tcpPort", "" + tcpPort));
			if(tcpAllowOthers) {
				args.add("-tcpAllowOthers");
			}
			log.info("Starting H2 TCP Listener on port [{}]. Allow Others:[{}]", tcpPort, tcpAllowOthers);
			tcpServer = new Server();
			try {
				tcpServer.runTool(args.toArray(new String[0]));
			} catch (Exception ex) {
				log.error("Failed to start TCP Server on port [" + tcpPort + "]", ex);
				throw new RuntimeException("Failed to start TCP Server on port [" + tcpPort + "]", ex);
				
			}
		}
		
		if(httpPort>0) {
			args.clear();
			args.addAll(Arrays.asList("-web", "-webDaemon", "-webPort", "" + httpPort));
			if(httpAllowOthers) {
				args.add("-httpAllowOthers");
			}
			log.info("Starting H2 Web Listener on port [{}]. Allow Others:[{}]", httpPort, httpAllowOthers);
			httpServer = new Server();
			try {
				httpServer.runTool(args.toArray(new String[0]));
			} catch (Exception ex) {
				log.error("Failed to start Web Server on port [" + tcpPort + "]", ex);
				throw new RuntimeException("Failed to start Web Server on port [" + tcpPort + "]", ex);				
			}
		}
	}
	
	/**
	 * Sets the passed user defined variable in the passed connection's session
	 * @param conn The connection to set the vars in
	 * @param key The variable key
	 * @param value The variable value
	 */
	protected void setUserDefinedVar(Connection conn, String key, Object value) {
		setUserDefinedVars(conn, Collections.singletonMap(key, value));
	}
	
	
	/**
	 * Sets the passed user defined variables in the passed connection's session
	 * @param conn The connection to set the vars in
	 * @param userDefinedVars A map of variables to set
	 */
	protected void setUserDefinedVars(Connection conn, Map<String, Object> userDefinedVars) {
		if(userDefinedVars==null) throw new IllegalArgumentException("User Vars was null");
		Statement st = null;
		try {
			st = conn.createStatement();
			String format = null;
			for(Map.Entry<String, Object> entry:  userDefinedVars.entrySet()) {
				if(entry.getValue() instanceof CharSequence) {
					format =  "SET @%s = '%s';";
				} else {
					format =  "SET @%s = %s;";
				}
				st.execute(String.format(format, entry.getKey(), entry.getValue()));
				log.debug("Set UDV [{}]=[{}]", entry.getKey(), entry.getValue());
			}						
		} catch (Exception ex) {
			throw new RuntimeException("Failed to set user defined vars [" + userDefinedVars + "]", ex);
		} finally {
			if(st!=null) try { st.close(); } catch (Exception x) { /* No Op */ }
		}
	}
	
	/**
	 * Executes the configured DDL resources against the created DB
	 * @param userDefinedVars A map of user defined variables to set
	 */
	protected void runDDLResources(Map<String, Object> userDefinedVars) {
		String pResource = null;
		Connection conn = null;
		Statement st = null;
		try {
			conn = dataSource.getConnection();
			conn.setAutoCommit(true);
			st = conn.createStatement();
			setUserDefinedVars(conn, userDefinedVars);
			Connection internal = ((com.jolbox.bonecp.ConnectionHandle)conn).getInternalConnection();
			log.info("\n\tConnected to [{}]. \n\tConnection Class [{}] \n\tLoaded from [{}]", conn.getMetaData().getURL(), internal.getClass().getName(), internal.getClass().getClassLoader().toString()); 
			for(String rez: ddlResources) {
				pResource = rez; 
				log.info("Processing DDL Resource [{}]", rez);
				String fileName = getDDLFileName(rez);
				st.execute("RUNSCRIPT FROM '" + fileName + "'");
				log.info("DDL Resource [{}] Processed", rez);
				new File(fileName).delete();
			}
		} catch (Exception ex) {
			log.error("Failed to run DDL Resource [" + pResource + "]", ex);
			throw new RuntimeException("Failed to run DDL Resource [" + pResource + "]", ex);
		} finally {
			if(st!=null) try { st.close(); } catch (Exception x) { /* No Op */ }
			if(conn!=null) try { conn.close(); } catch (Exception x) { /* No Op */ }
		}
	}
	
	/**
	 * Returns the readable file system file name for the passed DDL resource name
	 * @param ddlResource The resource path
	 * @return the readable DDL file
	 */
	protected String getDDLFileName(String ddlResource) {		
		InputStream is = null;
		FileOutputStream fos = null;
		File tmpFile = null;
		try {
			is = getDDLStream(ddlResource);
			tmpFile = File.createTempFile("tsdb-catalog", ".sql");
			tmpFile.deleteOnExit();
			fos = new FileOutputStream(tmpFile);
			byte[] buffer = new byte[8192];
			int bytesRead = -1;
			while((bytesRead=is.read(buffer))!=-1) {
				fos.write(buffer, 0, bytesRead);
			}
			fos.flush(); fos.close(); fos = null;
			is.close(); is = null;
			return tmpFile.getAbsolutePath();
		} catch (Exception ex) {
			log.error("Failed to get DDL file name", ex);
			throw new RuntimeException("Failed to get DDL file name", ex);
		} finally {
			if(is!=null) try { is.close(); } catch (Exception x) {/* No Op */}
			if(fos!=null) try { fos.close(); } catch (Exception x) {/* No Op */}
		}
	}
	
	/**
	 * Returns an inputstream for reading the H2 DDL
	 * @param ddlResource The resource path
	 * @return an inputstream for the H2 DDL
	 * @throws IOException thrown on errors reading from a file or URL
	 */
	protected InputStream getDDLStream(String ddlResource) throws IOException {
		if(ddlResource.startsWith("classpath:")) {
			String resource = ddlResource.substring("classpath:".length());		
			log.info("Looking Up DDL Resource [{}]", resource);
			InputStream is = getClass().getResourceAsStream(resource);
			if(is==null) {
				is = new FileInputStream("./src/main/resources" + resource);
			}
			return is;
		}
		if(URLHelper.isValidURL(ddlResource)) {
			return URLHelper.toURL(ddlResource).openStream();
		} else if(new File(ddlResource).canRead()) {
			return new FileInputStream(ddlResource);
		} else {
			throw new RuntimeException("Failed to locate DDL resource [" + ddlResource + "]");
		}
	}




	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.CatalogDBInterface#setConnectionProperty(java.sql.Connection, java.lang.String, java.lang.String)
	 */
	@Override
	public void setConnectionProperty(Connection conn, String key, String value) {
		setUserDefinedVar(conn, key, value);		
	}




	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.CatalogDBInterface#getConnectionProperty(java.sql.Connection, java.lang.String, java.lang.String)
	 */
	@Override
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

	
	
}
