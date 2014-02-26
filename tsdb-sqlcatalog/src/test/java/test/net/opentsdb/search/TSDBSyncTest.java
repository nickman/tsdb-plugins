/**
 * 
 */
package test.net.opentsdb.search;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.management.ObjectName;

import net.opentsdb.catalog.CatalogDBInterface;
import net.opentsdb.catalog.TSDBCatalogSearchEventHandler;
import net.opentsdb.core.TSDB;
import net.opentsdb.core.UniqueIdRegistry;
import net.opentsdb.meta.TSMeta;
import net.opentsdb.meta.UIDMeta;
import net.opentsdb.uid.UniqueId;

import org.helios.tsdb.plugins.util.JMXHelper;
import org.helios.tsdb.plugins.util.SystemClock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.stumbleupon.async.Deferred;

/**
 * <p>Title: TSDBSyncTest</p>
 * <p>Description: DB to TSDB synchronization test cases./p>
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><b><code>test.net.opentsdb.search.TSDBSyncTest</code></b>
 */

public class TSDBSyncTest extends CatalogBaseTest {
	
	/** The installed catalog service */
	protected CatalogDBInterface dbInterface = null;
	
	/**
	 * Purges the database before each test 
	 */
	@Before
	public void purgeDb() {
		dbInterface = TSDBCatalogSearchEventHandler.getInstance().getDbInterface();
		dbInterface.purge();
		dbInterface.setTSDBSyncPeriod(-1L);
		int fqnCount = jdbcHelper.queryForInt("SELECT COUNT(*) FROM TSD_TSMETA");
		Assert.assertEquals("Unexpected FQN RowCount After Purge", 0, fqnCount);
	}
	
	protected Deferred<Object> addPoint(ObjectName on, TSDB tsdb, long value, long timestamp) {
		String mn = on.getDomain().replace(" ", "");
		Map<String, String> tags = new HashMap<String, String>();
		for(Map.Entry<String, String> tag: on.getKeyPropertyList().entrySet()) {
			tags.put(tag.getKey(), tag.getValue().replace(" ", ""));
		}
		return tsdb.addPoint(mn, timestamp, value, tags);
	}
	
	/**
	 * Tests an inserted TSMeta's DB saved custom map values and last update times.
	 * @throws Exception thrown on any error.
	 */
	@Test
	public <T> void testNewTSMetaInsertCustom() throws Exception {
		Set<ObjectName> objectNames = new HashSet<ObjectName>();
		List<Deferred<T>> defs = new ArrayList<Deferred<T>>();
//		dbInterface.setTSDBSyncPeriodAndHighwater(10);
		for(ObjectName on: ManagementFactory.getPlatformMBeanServer().queryNames(null, null)) {
			objectNames.add(on);
			final long timestamp = SystemClock.rtime();
			//addPoint(on, tsdb, 1, timestamp);
			TSMeta tsMeta = fromUids(objectNameToUIDMeta(on));
			final long createTimeMs = SystemClock.rtime();
			final long createTimeSec = mstou(createTimeMs);
			
			tsMeta.setCreated(createTimeSec);
			//tsdb.indexTSMeta(tsMeta);
			addPoint(on, tsdb, 1, timestamp);
			
		}
		waitForProcessingQueue(name.getMethodName(), 3000000, TimeUnit.MILLISECONDS);
		dbInterface.setTSDBSyncPeriodAndHighwater(10);
//		UniqueIdRegistry.getInstance().purgeAllCaches();
		Thread.currentThread().join();
		
		
		
//		ObjectName on = JMXHelper.objectName(ManagementFactory.THREAD_MXBEAN_NAME);
//		//TSMeta tsMeta = fromUids(objectNameToUIDMeta(JMXHelper.objectName(ManagementFactory.THREAD_MXBEAN_NAME)));
//		final long timestamp = SystemClock.rtime();
//		executeAsync(tsdb.addPoint(on.getDomain(), timestamp, 1, on.getKeyPropertyList()), 2000);
//		TSMeta tsMeta = fromUids(objectNameToUIDMeta(JMXHelper.objectName(ManagementFactory.THREAD_MXBEAN_NAME)));
//		TSMeta tsMeta2 = (TSMeta)executeAsync(TSMeta.getTSMeta(tsdb, tsMeta.getTSUID()), 2000); 
//	 	//getTSMeta(TSDB tsdb, String tsuid)
////		TSMeta tsMeta = fromUids(objectNameToUIDMeta(JMXHelper.objectName(ManagementFactory.THREAD_MXBEAN_NAME)));
//		//log("Data Point Added: [%s]", tsMeta2);
//		
		//new MetaSynchronizer(tsdb).process(true);
		
		for(ObjectName on: objectNames) {
			String metric = on.getDomain().replace(" ", "");
			byte[] mkey = tagMunik.getId(metric);
			UIDMeta uidMeta = (UIDMeta)executeAsync(UIDMeta.getUIDMeta(tsdb, UniqueId.UniqueIdType.METRIC, mkey));
			log("Metric UIDMeta:[%s]", uidMeta);
			for(Map.Entry<String, String> entry: on.getKeyPropertyList().entrySet()) {
				mkey = tagKunik.getId(entry.getKey().replace(" ", ""));
				uidMeta = (UIDMeta)executeAsync(UIDMeta.getUIDMeta(tsdb, UniqueId.UniqueIdType.TAGK, mkey));
				log("TAGK UIDMeta:[%s]", uidMeta);
				mkey = tagVunik.getId(entry.getValue().replace(" ", ""));
				uidMeta = (UIDMeta)executeAsync(UIDMeta.getUIDMeta(tsdb, UniqueId.UniqueIdType.TAGV, mkey));
				log("TAGV UIDMeta:[%s]", uidMeta);								
			}
			String tsuid = fromUids(objectNameToUIDMeta(JMXHelper.objectName(ManagementFactory.THREAD_MXBEAN_NAME))).getTSUID();
			TSMeta tsMeta = (TSMeta)executeAsync(TSMeta.getTSMeta(tsdb, tsuid));
			log("TSMeta:[%s]", tsMeta);
		}
		log("Complete");
		Thread.currentThread().join();
		//TSMeta tsMeta = fromUids(objectNameToUIDMeta(JMXHelper.objectName(ManagementFactory.THREAD_MXBEAN_NAME)));
		
	}

}
