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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.ObjectName;

import net.opentsdb.catalog.TSDBCatalogSearchEventHandler;
import net.opentsdb.catalog.util.JDBCHelper;
import net.opentsdb.meta.UIDMeta;
import net.opentsdb.uid.UniqueId;
import net.opentsdb.uid.UniqueId.UniqueIdType;

import org.helios.tsdb.plugins.util.SystemClock;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

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
	protected final Set<String> createdMetricNames = new HashSet<String>();
	/** The names for which tag keys have been created */
	protected final Set<String> createdTagKeys = new HashSet<String>();
	/** The names for which tag values have been created */
	protected final Set<String> createdTagValues = new HashSet<String>();

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
	public List<UIDMeta> objectNameToUIDMeta(ObjectName on) {
		List<UIDMeta> metas = new ArrayList<UIDMeta>();
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
		switch(type) {
			case METRIC:
				if(!createdMetricNames.add(uname)) return null;
				break;
			case TAGK:
				if(!createdTagKeys.add(uname)) return null;
				break;
			case TAGV:
				if(!createdTagValues.add(uname)) return null;
				break;
		
		}
		UIDMeta meta = new UIDMeta(type, uidFor(ctr.incrementAndGet()), uname);
		meta.setCreated(SystemClock.time());
		return meta;
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
		for(ObjectName on: ons) {
			for(UIDMeta m : objectNameToUIDMeta(on)) {
				if(m==null) continue;
				tsdb.indexUIDMeta(m);
			}
		}
		//jdbcHelper.query(sql)
	}


}
