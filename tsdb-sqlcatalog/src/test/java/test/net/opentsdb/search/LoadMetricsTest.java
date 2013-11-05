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

import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.ObjectName;

import net.opentsdb.catalog.TSDBCatalogSearchEventHandler;
import net.opentsdb.meta.TSMeta;
import net.opentsdb.meta.UIDMeta;
import net.opentsdb.uid.UniqueId;
import net.opentsdb.uid.UniqueId.UniqueIdType;

import org.helios.tsdb.plugins.util.JMXHelper;
import org.helios.tsdb.plugins.util.SystemClock;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import test.net.opentsdb.search.util.JDBCHelper;

/**
 * <p>Title: LoadMetricsTest</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>test.net.opentsdb.search.LoadMetricsTest</code></p>
 */

public class LoadMetricsTest extends CatalogBaseTest {
	
	/** The JDBCHelper, initialized with a datasource from the handler */
	protected static JDBCHelper jdbcHelper = null;
	
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
	
	
	/** The async wait time */
	public static final long ASYNC_WAIT_TIMEOUT = 2000;
	/** The number of events to publish */
	public static final int PUBLISH_COUNT = 1000;
	
	/** The maximum number of retrieval loops to allow */
	public static final int MAX_RLOOPS = PUBLISH_COUNT;
	
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
	
	/**
	 * Converts the passed ObjectName to a list of UIDMetas
	 * @param on The ObjectName to convert
	 * @return a list of UIDMetas
	 */
	public LinkedList<UIDMeta> objectNameToUIDMeta(ObjectName on) {
		LinkedList<UIDMeta> metas = new LinkedList<UIDMeta>();
		metas.add(newUIDMeta(UniqueIdType.METRIC, METRIC_COUNTER, on.getDomain()));
		for(Map.Entry<String, String> entry: on.getKeyPropertyList().entrySet()) {
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
		switch(type) {
			case METRIC:
				meta = createdMetricNames.get(uname);
				if(meta==null) {
					meta = new UIDMeta(type, uidFor(ctr.incrementAndGet()), uname);
					createdMetricNames.put(uname, meta);
				}
				return meta;
			case TAGK:
				meta = createdTagKeys.get(uname);
				if(meta==null) {
					meta = new UIDMeta(type, uidFor(ctr.incrementAndGet()), uname);
					createdTagKeys.put(uname, meta);
				}
				return meta;
			case TAGV:
				meta = createdTagValues.get(uname);
				if(meta==null) {
					meta = new UIDMeta(type, uidFor(ctr.incrementAndGet()), uname);
					createdTagValues.put(uname, meta);
				}
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
	


	/**
	 * Configures the TSDB for all tests in this class.
	 */
	protected static void configureTSDB() {
		tsdb = newTSDB("CatalogSearchConfig");
	}
	
	/**
	 * Initializes the environment for tests in this class
	 */
	@BeforeClass
	public static void initialize() {
		tearDownTSDBAfterTest = false;   // all tests in this class run against the same TSDB instance
		createSearchShellJar();
		configureTSDB();
		TSDBCatalogSearchEventHandler.waitForStart();
		jdbcHelper = new JDBCHelper(TSDBCatalogSearchEventHandler.getInstance().getDataSource());
	}	
	
	/**
	 * Tests the indexing of a set of UIDMetas.
	 * @throws Exception thrown on any error
	 */
	@Test
	public void testUIDMetaIndexing() throws Exception {
		Set<ObjectName> ons = ManagementFactory.getPlatformMBeanServer().queryNames(null, null);
		for(int i = 0; i < 1000; i++) {
			//String[] frags = getRandomFragments();
			ons.add(JMXHelper.objectName("%s:foo=%s,bar=%s,%s=%s", getRandomFragments()));
		}
		for(ObjectName on: ons) {
			LinkedList<UIDMeta> uidMetas = objectNameToUIDMeta(on);
			for(UIDMeta m : uidMetas) {
				if(m==null) continue;
				tsdb.indexUIDMeta(m);
			}
			TSMeta tsMeta = fromUids(uidMetas);
			tsdb.indexTSMeta(tsMeta);
		}
		while(TSDBCatalogSearchEventHandler.getInstance().getProcessingQueueDepth()>0) {
			Thread.sleep(1000);
		}
		Thread.currentThread().join();
		//jdbcHelper.query(sql)
	}


}
