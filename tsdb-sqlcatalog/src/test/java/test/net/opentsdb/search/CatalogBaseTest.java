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
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.ObjectName;

import net.opentsdb.catalog.TSDBCatalogSearchEventHandler;
import net.opentsdb.catalog.datasource.ICatalogDataSource;
import net.opentsdb.meta.TSMeta;
import net.opentsdb.meta.UIDMeta;
import net.opentsdb.search.SearchQuery;
import net.opentsdb.search.SearchQuery.SearchType;
import net.opentsdb.uid.UniqueId;
import net.opentsdb.uid.UniqueId.UniqueIdType;
import net.opentsdb.utils.Config;

import org.helios.tsdb.plugins.test.BaseTest;
import org.helios.tsdb.plugins.util.FileHelper;
import org.helios.tsdb.plugins.util.SystemClock;
import org.helios.tsdb.plugins.util.SystemClock.ElapsedTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;

import test.net.opentsdb.core.EmptyTSDB;
import test.net.opentsdb.search.util.JDBCHelper;

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
	
	/** The JDBCHelper, initialized with a datasource from the handler */
	protected static JDBCHelper jdbcHelper = null;
	
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
	 * Initializes the environment for tests in this class
	 */
	@BeforeClass
	public static void initialize() {
		tearDownTSDBAfterTest = false;   // all tests in this class run against the same TSDB instance
		createServiceJar();
		configureTSDB();
		TSDBCatalogSearchEventHandler.waitForStart();
		jdbcHelper = new JDBCHelper(TSDBCatalogSearchEventHandler.getInstance().getDataSource());
	}	
	
	/**
	 * Configures the TSDB for all tests in this class.
	 */
	protected static void configureTSDB() {
		cleanH2FileDir("CatalogSearchConfig");
		tsdb = newTSDB("CatalogSearchConfig");
	}
	
	/**
	 * Waits for the event processing queue to complete processing on whatever has been submitted to this point.
	 * @param testPhase The name of the test phase
	 * @param timeout The time period to wait for the processing to complete
	 * @param unit the unit of the timeout
	 */
	protected void waitForProcessingQueue(String testPhase, long timeout, TimeUnit unit) {
		ElapsedTime et = SystemClock.startClock();
		if(!TSDBCatalogSearchEventHandler.getInstance().milestone().await(timeout, unit)) {
			Assert.fail("Timed out waiting for [" + testPhase + "] Milestone");
		} else {
			log("[%s] Milestone met after [%s] ms.", testPhase, et.elapsedMs());
		}
		
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
	
	/**
	 * Returns the UIDMeta uid as a byte array for the passed int
	 * @param key The int ot generate a byte array for
	 * @return the uid byte array
	 */
	public byte[] uidFor(int key) {
		StringBuilder b = new StringBuilder(Integer.toHexString(key));
		while(b.length()<6) {
			b.insert(0, "0");
		}
		return UniqueId.stringToUid(b.toString());
	}
	
	/** Synthetic UIDMeta counter for metrics */
	protected final AtomicInteger METRIC_COUNTER = new AtomicInteger();
	/** Synthetic UIDMeta counter for tag keys */
	protected final AtomicInteger TAGK_COUNTER = new AtomicInteger();
	/** Synthetic UIDMeta counter for tag values */
	protected final AtomicInteger TAGV_COUNTER = new AtomicInteger();
	
	/** The names for which metrics have been created */
	protected final Map<String, UIDMeta> createdMetricNames = new HashMap<String, UIDMeta>();
	/** The names for which tag keys have been created */
	protected final Map<String, UIDMeta> createdTagKeys = new HashMap<String, UIDMeta>();
	/** The names for which tag values have been created */
	protected final Map<String, UIDMeta> createdTagValues = new HashMap<String, UIDMeta>();

	/**
	 * Resets the UIDMeta counters
	 */
	@Before
	public void resetCounters() {
		METRIC_COUNTER.set(0);
		TAGK_COUNTER.set(0);
		TAGV_COUNTER.set(0);
		createdMetricNames.clear();
		createdTagKeys.clear();
		createdTagValues.clear();
		
	}
	
	
	/**
	 * Converts the passed ObjectName to a list of UIDMetas
	 * @param on The ObjectName to convert
	 * @return a list of UIDMetas
	 */
	public LinkedList<UIDMeta> objectNameToUIDMeta(ObjectName on) {
		LinkedList<UIDMeta> metas = new LinkedList<UIDMeta>();
		metas.add(newUIDMeta(UniqueIdType.METRIC, METRIC_COUNTER, on.getDomain()));
		TreeMap<String, String> props = new TreeMap<String, String>(on.getKeyPropertyList()); 
		for(Map.Entry<String, String> entry: props.entrySet()) {
			metas.add(newUIDMeta(UniqueIdType.TAGK, TAGK_COUNTER, entry.getKey()));
			metas.add(newUIDMeta(UniqueIdType.TAGV, TAGV_COUNTER, entry.getValue()));
		}
		return metas;
	}
	
	/**
	 * Creates a new UIDMeta
	 * @param type The meta type
	 * @param ctr An atomic counter providing the unique int identifier
	 * @param uname The uid meta name
	 * @return A UIDMeta
	 */
	public UIDMeta newUIDMeta(UniqueIdType type, AtomicInteger ctr, String uname) {
		UIDMeta meta = null;
		long unixTime = SystemClock.unixTime();
		switch(type) {
			case METRIC:
				meta = createdMetricNames.get(uname);				
				if(meta==null) {
					meta = new UIDMeta(type, uidFor(ctr.incrementAndGet()), uname);
					createdMetricNames.put(uname, meta);
				}
				meta.setCreated(unixTime);
				return meta;
			case TAGK:
				meta = createdTagKeys.get(uname);
				if(meta==null) {					
					meta = new UIDMeta(type, uidFor(ctr.incrementAndGet()), uname);
					createdTagKeys.put(uname, meta);
				}
				meta.setCreated(unixTime);
				return meta;
			case TAGV:
				meta = createdTagValues.get(uname);
				if(meta==null) {
					meta = new UIDMeta(type, uidFor(ctr.incrementAndGet()), uname);					
					createdTagValues.put(uname, meta);
				}
				meta.setCreated(unixTime);
				return meta;
			default:
				throw new RuntimeException("yeow. Unrecognized type [" + type + "]");							
		}
	}
	
	/**
	 * Creates a new TSMeta from the passed UIDMeta list
	 * @param uidMetas A list of UIDMetas
	 * @return the built TSMeta
	 */
	public TSMeta fromUids(LinkedList<UIDMeta> uidMetas) {
		UIDMeta metricMeta = uidMetas.removeFirst();
		StringBuilder strTsuid = new StringBuilder(metricMeta.getUID());
		for(UIDMeta umeta: uidMetas) {
			strTsuid.append(umeta.getUID());
		}
		byte[] tsuid = UniqueId.stringToUid(strTsuid.toString());
		TSMeta tsMeta = new TSMeta(tsuid, SystemClock.unixTime());
		setTags(tsMeta, new ArrayList<UIDMeta>(uidMetas));
		setMetric(tsMeta, metricMeta);
		return tsMeta;
	}
	
	/** The maximum number of sync queue loops when trying to flush the sync queue */
	public static final int MAX_SYNC_QUEUE_LOOPS = 10;
	
	/** The reflective access field for a TSMeta's tags ArrayList (since we don't have direct API access) */
	protected static final Field tsMetaTagsField;
	/** The reflective access field for a TSMeta's metric (since we don't have direct API access) */
	protected static final Field tsMetaMetricField;
	
	static {
		try {
			tsMetaTagsField = TSMeta.class.getDeclaredField("tags");
			tsMetaTagsField.setAccessible(true);
			tsMetaMetricField = TSMeta.class.getDeclaredField("metric");
			tsMetaMetricField.setAccessible(true);			
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	
	/**
	 * Reflectively sets the tags in a TSMeta instance
	 * @param tsMeta the TSMeta instance
	 * @param tags the tags
	 */
	protected static void setTags(TSMeta tsMeta, ArrayList<UIDMeta> tags) {
		try {
			tsMetaTagsField.set(tsMeta, tags);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	
	/**
	 * Reflectively sets the metric in a TSMeta instance
	 * @param tsMeta the TSMeta instance
	 * @param metric The metric UIDMeta
	 */
	protected static void setMetric(TSMeta tsMeta, UIDMeta metric) {
		try {
			tsMetaMetricField.set(tsMeta, metric);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	
	
	/** The TSD_TSMETA column id for FQNID */
	public static final int TSMETA_FQNID = 0;
	/** The TSD_TSMETA column id for VERSION */
	public static final int TSMETA_VERSION = 1;
	/** The TSD_TSMETA column id for METRIC_UID */
	public static final int TSMETA_METRIC_UID = 2;
	/** The TSD_TSMETA column id for FQN */
	public static final int TSMETA_FQN = 3;
	/** The TSD_TSMETA column id for TSUID */
	public static final int TSMETA_TSUID = 4;
	/** The TSD_TSMETA column id for CREATED */
	public static final int TSMETA_CREATED = 5;
	/** The TSD_TSMETA column id for LAST_UPDATE */
	public static final int TSMETA_LAST_UPDATE = 6;
	/** The TSD_TSMETA column id for MAX_VALUE */
	public static final int TSMETA_MAX_VALUE = 7;
	/** The TSD_TSMETA column id for MIN_VALUE */
	public static final int TSMETA_MIN_VALUE = 8;
	/** The TSD_TSMETA column id for DATA_TYPE */
	public static final int TSMETA_DATA_TYPE = 9;
	/** The TSD_TSMETA column id for DESCRIPTION */
	public static final int TSMETA_DESCRIPTION = 10;
	/** The TSD_TSMETA column id for DISPLAY_NAME */
	public static final int TSMETA_DISPLAY_NAME = 11;
	/** The TSD_TSMETA column id for NOTES */
	public static final int TSMETA_NOTES = 12;
	/** The TSD_TSMETA column id for UNITS */
	public static final int TSMETA_UNITS = 13;
	/** The TSD_TSMETA column id for RETENTION */
	public static final int TSMETA_RETENTION = 14;
	/** The TSD_TSMETA column id for CUSTOM */
	public static final int TSMETA_CUSTOM = 15;
	
	
	/** The TSD_TAG column id for XUID */
	public static final int TAG_XUID = 0;
	/** The TSD_TAG column id for VERSION */
	public static final int TAG_VERSION = 1;
	/** The TSD_TAG column id for NAME */
	public static final int TAG_NAME = 2;
	/** The TSD_TAG column id for CREATED */
	public static final int TAG_CREATED = 3;
	/** The TSD_TAG column id for LAST_UPDATE */
	public static final int TAG_LAST_UPDATE = 4;
	/** The TSD_TAG column id for DESCRIPTION */
	public static final int TAG_DESCRIPTION = 5;
	/** The TSD_TAG column id for DISPLAY_NAME */
	public static final int TAG_DISPLAY_NAME = 6;
	/** The TSD_TAG column id for NOTES */
	public static final int TAG_NOTES = 7;
	/** The TSD_TAG column id for CUSTOM */
	public static final int TAG_CUSTOM = 8;
	
	/** The TSD_METRIC column id for XUID */
	public static final int METRIC_XUID = 0;
	/** The TSD_METRIC column id for VERSION */
	public static final int METRIC_VERSION = 1;
	/** The TSD_METRIC column id for NAME */
	public static final int METRIC_NAME = 2;
	/** The TSD_METRIC column id for CREATED */
	public static final int METRIC_CREATED = 3;
	/** The TSD_METRIC column id for LAST_UPDATE */
	public static final int METRIC_LAST_UPDATE = 4;
	/** The TSD_METRIC column id for DESCRIPTION */
	public static final int METRIC_DESCRIPTION = 5;
	/** The TSD_METRIC column id for DISPLAY_NAME */
	public static final int METRIC_DISPLAY_NAME = 6;
	/** The TSD_METRIC column id for NOTES */
	public static final int METRIC_NOTES = 7;
	/** The TSD_METRIC column id for CUSTOM */
	public static final int METRIC_CUSTOM = 8;
	
	/** The TSD_ANNOTATION column id for ANNID */
	public static final int ANNOTATION_ANNID = 0;
	/** The TSD_ANNOTATION column id for VERSION */
	public static final int ANNOTATION_VERSION = 1;
	/** The TSD_ANNOTATION column id for START_TIME */
	public static final int ANNOTATION_START_TIME = 2;
	/** The TSD_ANNOTATION column id for LAST_UPDATE */
	public static final int ANNOTATION_LAST_UPDATE = 3;
	/** The TSD_ANNOTATION column id for DESCRIPTION */
	public static final int ANNOTATION_DESCRIPTION = 4;
	/** The TSD_ANNOTATION column id for NOTES */
	public static final int ANNOTATION_NOTES = 5;
	/** The TSD_ANNOTATION column id for FQNID */
	public static final int ANNOTATION_FQNID = 6;
	/** The TSD_ANNOTATION column id for END_TIME */
	public static final int ANNOTATION_END_TIME = 7;
	/** The TSD_ANNOTATION column id for CUSTOM */
	public static final int ANNOTATION_CUSTOM = 8;
	
	
	/**
	 * Converts a millisecond based timestamp to a unix seconds based timestamp
	 * @param time The millisecond timestamp to convert
	 * @return a unix timestamp
	 */
	public static long mstou(long time) {
		return TimeUnit.SECONDS.convert(time, TimeUnit.MILLISECONDS);
	}
	
	/**
	 * Converts a unix second based timestamp to a long millisecond based timestamp
	 * @param time The unix timestamp to convert
	 * @return a long millisecond timestamp
	 */
	public static long utoms(long time) {
		return TimeUnit.MILLISECONDS.convert(time, TimeUnit.SECONDS);
	}
	
	
}


//SELECT 
//'/** The TSD_ANNOTATION column id for ' || COLUMN_NAME || ' */' || CHAR(10) || 
//'public static final int ANNOTATION_' || COLUMN_NAME  || ' = ' || ORDINAL_POSITION-1  || ';' FROM INFORMATION_SCHEMA.COLUMNS  where table_name = 'TSD_ANNOTATION'
//order by ORDINAL_POSITION

