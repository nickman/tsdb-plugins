/**
 * 
 */
package test.net.opentsdb.search;

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.management.ObjectName;

import net.opentsdb.catalog.AbstractDBCatalog;
import net.opentsdb.catalog.CatalogDBInterface;
import net.opentsdb.catalog.TSDBCatalogSearchEventHandler;
import net.opentsdb.core.TSDB;

import org.helios.tsdb.plugins.meta.MetaSynchronizer;
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
	/** The custom map key for the version number */
	public static final String VERSION_KEY = AbstractDBCatalog.VERSION_KEY;
	/** The custom map key for the internal pk id */
	public static final String PK_KEY = AbstractDBCatalog.PK_KEY;
	
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
	public void testNewTSMetaInsertCustom() throws Exception {
		Set<ObjectName> objectNames = new HashSet<ObjectName>();
		for(ObjectName on: ManagementFactory.getPlatformMBeanServer().queryNames(null, null)) {
			objectNames.add(on);
			final long timestamp = SystemClock.rtime();
			executeAsync(addPoint(on, tsdb, 1, timestamp), 2000);					
		}
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
		new MetaSynchronizer(tsdb).process(true);
		
		//TSMeta tsMeta = fromUids(objectNameToUIDMeta(JMXHelper.objectName(ManagementFactory.THREAD_MXBEAN_NAME)));
		
	}

}
