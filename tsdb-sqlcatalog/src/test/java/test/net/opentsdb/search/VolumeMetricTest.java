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
package test.net.opentsdb.search;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.management.ObjectName;

import net.opentsdb.catalog.CatalogDBInterface;
import net.opentsdb.catalog.TSDBCatalogSearchEventHandler;
import net.opentsdb.meta.TSMeta;
import net.opentsdb.meta.UIDMeta;

import org.helios.tsdb.plugins.util.ConfigurationHelper;
import org.helios.tsdb.plugins.util.SystemClock;
import org.helios.tsdb.plugins.util.SystemClock.ElapsedTime;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import test.net.opentsdb.search.util.JDBCHelper;

/**
 * <p>Title: VolumeMetricTest</p>
 * <p>Description: A volume metric test for testing large volumes of metrics import and query performance</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>test.net.opentsdb.search.VolumeMetricTest</code></p>
 */

public class VolumeMetricTest extends LoadMetricsTest {
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
	 * Executes a volume load
	 * @throws Exception thrown on any error
	 */
	@Test
	public void testVolumeLoad() throws Exception {
		CatalogDBInterface dbInterface = TSDBCatalogSearchEventHandler.getInstance().getDbInterface();
		dbInterface.purge();
		int fqnCount = jdbcHelper.queryForInt("SELECT COUNT(*) FROM TSD_TSMETA");
		Assert.assertEquals("Unexpected FQN RowCount After Purge", 0, fqnCount);
		int MCOUNT = 20000;
		int KCOUNT = 10000;
		int VCOUNT = 127000;
		int TCOUNT = 2500000;
		
		Set<String> metrics = new HashSet<String>(MCOUNT);
		Set<String> tagKeys = new HashSet<String>(KCOUNT);
		Set<String> tagValues = new HashSet<String>(VCOUNT);
		Set<String> fqns = new HashSet<String>(TCOUNT);
		ElapsedTime et = SystemClock.startClock();
		
		log("Generating Syntehtic Metrics");
		while(metrics.size() < MCOUNT) {
			Collections.addAll(metrics, getRandomFragments());
		}
		log("Generating Syntehtic Tag Keys");
		while(tagKeys.size() < KCOUNT) {
			Collections.addAll(tagKeys, getRandomFragments());
		}
		log("Generating Syntehtic Tag Values");
		while(tagValues.size() < VCOUNT) {
			Collections.addAll(tagValues, getRandomFragments());
		}
		
		String[] M = metrics.toArray(new String[metrics.size()]); metrics.clear();
		String[] K = tagKeys.toArray(new String[tagKeys.size()]); tagKeys.clear();
		String[] V = tagValues.toArray(new String[tagValues.size()]); tagValues.clear();
		int ML = M.length-1;
		int KL = K.length-1;
		int VL = V.length-1;
		
		System.gc();
		int tagCount = -1;
		StringBuilder b = new StringBuilder(120);
		
		log("Generating TSMetas");
		while(fqns.size() < TCOUNT) {
			tagCount = nextPosInt(4); if(tagCount==0) tagCount=1;
			b.append(M[nextPosInt(ML)]).append(":");
			for(int i = 0; i < tagCount; i++) {
				b.append(K[nextPosInt(KL)]).append("=").append(V[nextPosInt(VL)]).append(",");
			}
			b.deleteCharAt(b.length()-1);
			fqns.add(b.toString());
			b.setLength(0);
		}
		log("Generated TSMetas: %s,  First Value: [%s]", fqns.size(), fqns.iterator().next());
		M = null; K = null; V = null;
		System.gc();
		log("Populated Raw Data in [%s] ms.", et.elapsedMs());
		int cnt = 0;
		for(String s: fqns) {
			try {
				ObjectName on = new ObjectName(s);
				TSMeta meta = new TSMeta();
				LinkedList<UIDMeta> uidMetas =  objectNameToUIDMeta(on);
				for(UIDMeta m : uidMetas) {
					if(m==null) continue;
					tsdb.indexUIDMeta(m);
				}
				meta = fromUids(uidMetas);
				tsdb.indexTSMeta(meta);
				cnt++;
				if(cnt%10000==0) {
					waitForProcessingQueue("testVolumeMetaUpdates/Indexing [" + cnt + "]", 300000, TimeUnit.MILLISECONDS);
				}
				if(cnt%100000==0) {
					log("==========================> Indexed [%s] TSMetas", cnt);
				}
			} catch (Exception ex) {
				/* No Op */
			}
		}
		
		log("All index requests submitted. Waiting for milestone");
		waitForProcessingQueue("testVolumeMetaUpdates/Indexing", 300000, TimeUnit.MILLISECONDS);
		if(ConfigurationHelper.getBooleanSystemThenEnvProperty("debug.catalog.daemon", false)) {
			Thread.currentThread().join();
		}	
		
	}

}
