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
package test.net.opentsdb.search;

import java.io.File;

import net.opentsdb.catalog.datasource.ICatalogDataSource;
import net.opentsdb.search.SearchQuery;
import net.opentsdb.search.SearchQuery.SearchType;
import net.opentsdb.utils.Config;

import org.helios.tsdb.plugins.test.BaseTest;

import test.net.opentsdb.core.EmptyTSDB;
import test.net.opentsdb.search.util.FileHelper;

/**
 * <p>Title: CatalogBaseTest</p>
 * <p>Description: Base test class for catalog search plugin</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>test.net.opentsdb.search.CatalogBaseTest</code></p>
 */

public class CatalogBaseTest extends BaseTest {
	/** The JDBC URL prefix of file based DB instances */
	public static final String H2_FILE_URL_PREFIX = "jdbc:h2:file:";
	/**
	 * Creates a new CatalogBaseTest
	 */
	public CatalogBaseTest() {
		
	}
	
	/**
	 * Determines if Oracle is available
	 * @param tsdbConfigName The TSDB environment configuration to build a classpath from
	 * @return true if Oracle is available, false otherwise
	 */
	public static boolean oracleAvailable(String tsdbConfigName) {
		try {
			ClassLoader cl = tsdbClassLoader(tsdbConfigName);
			Class.forName("oracle.jdbc.driver.OracleDriver", true, cl);
			return true;
		} catch (Exception ex) {
			return false;
		}
	}
	
	/**
	 * Determines if Postgres is available
	 * @param tsdbConfigName The TSDB environment configuration to build a classpath from
	 * @return true if Postgres is available, false otherwise
	 */
	public static boolean postgresAvailable(String tsdbConfigName) {
		try {
			ClassLoader cl = tsdbClassLoader(tsdbConfigName);
			Class.forName("org.postgresql.Driver", true, cl);
			return true;
		} catch (Exception ex) {
			return false;
		}
	}
	
	/**
	 * Configures the TSDB for all tests in this class.
	 */
	protected static void configureTSDB() {
		cleanH2FileDir("CatalogSearchConfig");
		tsdb = newTSDB("CatalogSearchConfig");
	}
	
	public static void cleanH2FileDir(String configName) {
		try {
			Config cfg = getConfig(configName);
			String jdbcUrl = cfg.getString(ICatalogDataSource.JDBC_POOL_JDBCURL);
			if(jdbcUrl==null) jdbcUrl = ICatalogDataSource.DEFAULT_JDBC_POOL_JDBCURL;
			if(jdbcUrl.startsWith(H2_FILE_URL_PREFIX)) {
				int pl = H2_FILE_URL_PREFIX.length();
		        int dindex = jdbcUrl.indexOf(';');
		        String fileDir  = jdbcUrl.substring(pl, dindex==-1 ? jdbcUrl.length() : dindex);
		        log("H2 FILE PREFIX: [%s]", fileDir);
		        fileDir = fileDir.replace("~", System.getProperty("user.home"));
		        File f = new File(fileDir);
		        if(f.exists()) {
//		        	if(f.isDirectory()) {
//		        		FileHelper.emptyDir(f);
//		        	} else {
//		        		FileHelper.emptyDir(f.getParentFile());
//		        	}
		        }
			}
			// H2_FILE_URL_PREFIX
			// jdbc:h2:file:~/sd-data/tsdb/tsdb;
		} catch (Exception e) {
			throw new RuntimeException("Failed to load CatalogSearchConfig", e);
		}
		
	}

	public static class FakeSyncToStore extends EmptyTSDB {
		
	}
	
	public static class SearchQueryBuilder  {
		private final SearchQuery sq = new SearchQuery();
		private boolean exact = false;
		
		public SearchQuery get() {
			if(exact) {
				if(!sq.getQuery().startsWith("\"")) {
					sq.setQuery("\"" + sq.getQuery());
				}
				if(!sq.getQuery().endsWith("\"")) {
					sq.setQuery(sq.getQuery() + "\"");
				}				
			}
			return sq;
		}

		/**
		 * @param type
		 * @see net.opentsdb.search.SearchQuery#setType(net.opentsdb.search.SearchQuery.SearchType)
		 */
		public SearchQueryBuilder setType(SearchType type) {
			sq.setType(type);
			return this;
		}
		
		public SearchQueryBuilder exact(boolean exact) {
			this.exact = exact;
			return this;
		}

		/**
		 * @param query
		 * @see net.opentsdb.search.SearchQuery#setQuery(java.lang.String)
		 */
		public SearchQueryBuilder setQuery(String query) {
			if(query.indexOf(' ')!=-1) {
				sq.setQuery("\"" + query + "\"");
			} else {
				sq.setQuery(query);
			}			
			return this;			
		}

		/**
		 * @param limit
		 * @see net.opentsdb.search.SearchQuery#setLimit(int)
		 */
		public SearchQueryBuilder setLimit(int limit) {
			sq.setLimit(limit);
			return this;
		}

		/**
		 * @param start_index
		 * @see net.opentsdb.search.SearchQuery#setStartIndex(int)
		 */
		public SearchQueryBuilder setStartIndex(int start_index) {
			sq.setStartIndex(start_index);
			return this;
		}


		/**
		 * @return
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			return sq.toString();
		}
		
	}

}
