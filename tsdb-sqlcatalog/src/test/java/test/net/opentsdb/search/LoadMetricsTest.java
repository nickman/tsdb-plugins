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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.MBeanInfo;
import javax.management.ObjectName;

import net.opentsdb.catalog.CatalogDBInterface;
import net.opentsdb.catalog.TSDBCatalogSearchEventHandler;
import net.opentsdb.core.TSDB;
import net.opentsdb.meta.Annotation;
import net.opentsdb.meta.TSMeta;
import net.opentsdb.meta.UIDMeta;
import net.opentsdb.search.SearchQuery;
import net.opentsdb.search.SearchQuery.SearchType;
import net.opentsdb.uid.UniqueId;
import net.opentsdb.uid.UniqueId.UniqueIdType;
import net.opentsdb.utils.Config;

import org.helios.tsdb.plugins.util.ConfigurationHelper;
import org.helios.tsdb.plugins.util.JMXHelper;
import org.helios.tsdb.plugins.util.SystemClock;
import org.helios.tsdb.plugins.util.SystemClock.ElapsedTime;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import test.net.opentsdb.core.MethodMocker;
import test.net.opentsdb.search.util.JDBCHelper;

import com.stumbleupon.async.Deferred;

/**
 * <p>Title: LoadMetricsTest</p>
 * <p>Description: Search event handling unit tests using the default in-mem H2 DB and the EventBus async driver.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>test.net.opentsdb.search.LoadMetricsTest</code></p>
 */
public class LoadMetricsTest extends CatalogBaseTest {
	
	
	
	/** The async wait time */
	public static final long ASYNC_WAIT_TIMEOUT = 2000;
	/** The number of events to publish */
	public static final int PUBLISH_COUNT = 1000;
	
	/** The maximum number of retrieval loops to allow */
	public static final int MAX_RLOOPS = PUBLISH_COUNT;
	
	


	
	
	
	/**
	 * Tests the indexing of a set of new UIDMetas.
	 * @throws Exception thrown on any error
	 */
	//@Test(timeout=60000)
	@Test
	public void testMetaIndexing() throws Exception {
		TSDBCatalogSearchEventHandler.getInstance().getDbInterface().purge();
		int fqnCount = jdbcHelper.queryForInt("SELECT COUNT(*) FROM TSD_TSMETA");
		Assert.assertEquals("Unexpected FQN RowCount After Purge", 0, fqnCount);

		Set<ObjectName> ons = ManagementFactory.getPlatformMBeanServer().queryNames(null, null);
		Set<TSMeta> createdTSMetas = new HashSet<TSMeta>(ons.size());
//		for(int i = 0; i < 1000; i++) {
//			ons.add(JMXHelper.objectName("%s:foo=%s,bar=%s,%s=%s", getRandomFragments()));
//		}
		/*
		 * Iterate through all the MBeans in the platform MBeanserver.
		 * For each, build a TSMeta from the MBean's ObjectName 
		 * where the domain is the metric name and the properties
		 * are tagk/tagv pairs. Index each created TSMeta 
		 */
		for(ObjectName on: ons) {
			LinkedList<UIDMeta> uidMetas = objectNameToUIDMeta(on);
			for(UIDMeta m : uidMetas) {
				if(m==null) continue;
				tsdb.indexUIDMeta(m);
			}
			TSMeta tsMeta = fromUids(uidMetas);
			tsMeta.setCreated(SystemClock.unixTime());
			tsdb.indexTSMeta(tsMeta);
			createdTSMetas.add(tsMeta);
			Annotation ann = new Annotation();
			ann.setTSUID(tsMeta.getTSUID());
			MBeanInfo minfo = JMXHelper.getMBeanInfo(on);
			ann.setDescription(minfo.getDescription());
			ann.setNotes(minfo.getClassName());
			tsdb.indexAnnotation(ann);
		} 
		waitForProcessingQueue("testMetaIndexing/Indexing", 30000, TimeUnit.MILLISECONDS);
		if(ConfigurationHelper.getBooleanSystemThenEnvProperty("debug.catalog.daemon", false)) {
			Thread.currentThread().join();
		}
		/*
		 * Now that we've indexed all the TSMetas, we need to validate
		 * that each one can be found in the database. 
		 */		
		for(ObjectName on: ons) {
//			log("Testing DB For [%s]", on);
			String metric = on.getDomain();
			int rowCount = jdbcHelper.queryForInt("SELECT COUNT(*) FROM TSD_METRIC WHERE NAME = '" + metric + "'");
			Assert.assertEquals("Unexpected Metric RowCount for [" + metric + "]", 1, rowCount);
			for(Map.Entry<String, String> entry: on.getKeyPropertyList().entrySet()) {
				String key = entry.getKey();
				String value = entry.getValue();
				String combined = key + "=" + value;						
				rowCount = jdbcHelper.queryForInt("SELECT COUNT(*) FROM TSD_TAGK WHERE NAME = '" + key + "'");
				Assert.assertEquals("Unexpected Tag Key RowCount for [" + key + "]", 1, rowCount);
				rowCount = jdbcHelper.queryForInt("SELECT COUNT(*) FROM TSD_TAGV WHERE NAME = '" + value + "'");
				Assert.assertEquals("Unexpected Tag Value RowCount for [" + value + "]", 1, rowCount);				
				String pairUid = jdbcHelper.query("SELECT XUID FROM TSD_TAGPAIR WHERE NAME = '" + combined + "'")[0][0].toString();				
				Assert.assertTrue("Unexpected Null or Empty Tag Pair UID for Combined [" + combined + "]", pairUid!=null && !pairUid.trim().isEmpty());
				rowCount = jdbcHelper.queryForInt("SELECT COUNT(*) FROM TSD_FQN_TAGPAIR WHERE XUID = '" + pairUid + "'");
				Assert.assertTrue("Unexpected FQN Tag Pair UID RowCount for [" + pairUid + "]", rowCount >= 1);
			}
			rowCount = jdbcHelper.queryForInt("SELECT COUNT(*) FROM TSD_TSMETA WHERE FQN = '" + JMXHelper.getPropSortedObjectName(on) + "'");
			Assert.assertTrue("Unexpected FQN  RowCount for [" + JMXHelper.getPropSortedObjectName(on) + "]", rowCount == 1);
		}
		Object[][] fqns = jdbcHelper.query("SELECT * FROM TSD_TSMETA");
		for(Object[] row: fqns) {
			ObjectName on = JMXHelper.objectName(row[3]);
			Assert.assertTrue("The ObjectName [" + on + "] was not registered", JMXHelper.isRegistered(on));
		}
		for(TSMeta tsMeta: createdTSMetas) {
			String tsUid = tsMeta.getTSUID();
			int tagCount = tsMeta.getTags().size()/2;
			int rowCount = jdbcHelper.queryForInt("SELECT COUNT(*) FROM TSD_TSMETA WHERE TSUID = '" + tsUid + "'");
			Assert.assertEquals("Unexpected TSUID RowCount for [" + tsUid + "]", 1, rowCount);
			rowCount = jdbcHelper.queryForInt("SELECT COUNT(*) FROM TSD_FQN_TAGPAIR WHERE FQNID IN " + 
					"(SELECT FQNID FROM TSD_TSMETA WHERE TSUID = '" + tsUid + "')");
			Assert.assertEquals("Unexpected TagPair RowCount for [" + tsUid + "]", tagCount, rowCount);			
			tsdb.deleteTSMeta(tsMeta.getTSUID());
		}
		waitForProcessingQueue("testMetaIndexing/TSUID Deletes", 30000, TimeUnit.MILLISECONDS);
		for(TSMeta tsMeta: createdTSMetas) {
			String tsUid = tsMeta.getTSUID();			
			int rowCount = jdbcHelper.queryForInt("SELECT COUNT(*) FROM TSD_TSMETA WHERE TSUID = '" + tsUid + "'");
			Assert.assertEquals("Unexpected TSUID RowCount for [" + tsUid + "]", 0, rowCount);
			rowCount = jdbcHelper.queryForInt("SELECT COUNT(*) FROM TSD_FQN_TAGPAIR WHERE FQNID IN " + 
					"(SELECT FQNID FROM TSD_TSMETA WHERE TSUID = '" + tsUid + "')");
			Assert.assertEquals("Unexpected TagPair RowCount for [" + tsUid + "]", 0, rowCount);
			SearchQuery searchQuery = new SearchQuery();
			searchQuery.setType(SearchType.TSMETA);
			searchQuery.setQuery("TSUID:" + tsUid);
			searchQuery.setLimit(0);
			searchQuery.setStartIndex(0);
			//TSDBCatalogSearchEventHandler.getInstance().getDbInterface().purge();
			final CountDownLatch latch = new CountDownLatch(1);
//			Deferred<SearchQuery> result = tsdb.executeSearch(searchQuery)
//					.addErrback(new )
//					
//					.addBoth(new Callback<SearchQuery, SearchQuery>() {
//						@Override
//						public SearchQuery call(SearchQuery arg)
//								throws Exception {
//							// TODO Auto-generated method stub
//							return null;
//						}
//					});
			
		}
	}		
	
	
	/**
	 * Tests the update of a set of UIDMetas.
	 * @throws Exception thrown on any error
	 */
//	@Test(timeout=60000)
	@Test
	public void testMetaUpdates() throws Exception {
		CatalogDBInterface dbInterface = TSDBCatalogSearchEventHandler.getInstance().getDbInterface();
		dbInterface.purge();
		int fqnCount = jdbcHelper.queryForInt("SELECT COUNT(*) FROM TSD_TSMETA");
		Assert.assertEquals("Unexpected FQN RowCount After Purge", 0, fqnCount);

		Set<ObjectName> ons = ManagementFactory.getPlatformMBeanServer().queryNames(null, null);
		Set<TSMeta> createdTSMetas = new HashSet<TSMeta>(ons.size());
		Set<UIDMeta> createdUIDMetas = new HashSet<UIDMeta>(ons.size()*3);
		Set<Annotation> createdAnnotations = new HashSet<Annotation>(ons.size());
		/*
		 * Iterate through all the MBeans in the platform MBeanserver.
		 * For each, build a TSMeta from the MBean's ObjectName 
		 * where the domain is the metric name and the properties
		 * are tagk/tagv pairs. Index each created TSMeta .
		 */
		for(ObjectName on: ons) {
			LinkedList<UIDMeta> uidMetas = objectNameToUIDMeta(on);
			for(UIDMeta m : uidMetas) {
				if(m==null) continue;
				tsdb.indexUIDMeta(m);
				createdUIDMetas.add(m);
			}
			TSMeta tsMeta = fromUids(uidMetas);
			tsMeta.setCreated(SystemClock.unixTime());
			tsdb.indexTSMeta(tsMeta);
			createdTSMetas.add(tsMeta);
			Annotation ann = new Annotation();
			ann.setTSUID(tsMeta.getTSUID());
			MBeanInfo minfo = JMXHelper.getMBeanInfo(on);
			ann.setDescription(minfo.getDescription());
			ann.setNotes(minfo.getClassName());
			tsdb.indexAnnotation(ann);
			createdAnnotations.add(ann);
		}
		waitForProcessingQueue("testMetaUpdates/Indexing", 30000, TimeUnit.MILLISECONDS);
		// Submit updates for each UIDMeta and TSMeta to trigger and update
		int cnt = 0;
		for(UIDMeta uidMeta: createdUIDMetas) {
			Assert.assertTrue(dbInterface.exists(uidMeta));
			uidMeta.setNotes(uidMeta.toString());			
			tsdb.indexUIDMeta(uidMeta);
			cnt++;
		}
		log("Updated [%s] UIDMetas", cnt);

		cnt=0;
		for(TSMeta tsMeta: createdTSMetas) {
			Assert.assertTrue(dbInterface.exists(tsMeta));
			tsMeta.setNotes(tsMeta.toString());
			tsdb.indexTSMeta(tsMeta);
			cnt++;
		}
		log("Updated [%s] TSMetas", cnt);
		cnt=0;
		for(Annotation ann: createdAnnotations) {
			Assert.assertTrue("Failed to find annotation [" + ann + "]", dbInterface.exists(ann));
			ann.setNotes(ann.getNotes() + "-UPDATED");
			tsdb.indexAnnotation(ann);
			cnt++;
		}
		log("Updated [%s] Annotations", cnt);		
		waitForProcessingQueue("testMetaUpdates/Updates", 30000, TimeUnit.MILLISECONDS);
		// Validate that all versions are 2 and the syncToStore mock object equals the original object
		Object[][] lookups = null;
		int index = 0;
//		for(UIDMeta uidMeta: createdUIDMetas) {
//			Assert.assertEquals("Version of UIDMeta [" + uidMeta + "]@[" + index + "] was not 2", "2", uidMeta.getCustom().get(CatalogDBInterface.VERSION_KEY));
//			lookups = jdbcHelper.query("SELECT VERSION FROM TSD_" + uidMeta.getType() + " WHERE XUID = '" + uidMeta.getUID() + "'");
//			Assert.assertEquals("Query lookup by " + uidMeta.getType() + " [" + uidMeta.getUID() + "] was not 1", 1, lookups.length);
//			Assert.assertEquals("Version from DB was not 2", 2, ((Number)lookups[0][0]).intValue());
//			index++;
//		}
//		for(TSMeta tsMeta: createdTSMetas) {
//			Assert.assertEquals("Version of TSMeta was not 2", "2", tsMeta.getCustom().get(CatalogDBInterface.VERSION_KEY));
//			lookups = jdbcHelper.query("SELECT VERSION FROM TSD_TSMETA WHERE TSUID = '" + tsMeta.getTSUID() + "'");
//			Assert.assertEquals("Query lookup by TSUID [" + tsMeta.getTSUID() + "] was not 1", 1, lookups.length);
//			Assert.assertEquals("Version of TSD_TSMETA.VERSION was not 2", 2, ((Number)lookups[0][0]).intValue());
//		}
//		for(Annotation ann: createdAnnotations) {
//			Assert.assertEquals("Version of Annotation was not 2", "2", ann.getCustom().get(CatalogDBInterface.VERSION_KEY));
//			long annId = Long.parseLong(ann.getCustom().get(CatalogDBInterface.PK_KEY));
//			lookups = jdbcHelper.query("SELECT VERSION FROM TSD_ANNOTATION WHERE ANNID = " + annId);
//			Assert.assertEquals("Query lookup for Annotation ANNID [" + annId + "] was not 1", 1, lookups.length);
//			Assert.assertEquals("Version of TSD_ANNOTATION.VERSION was not 2", 2, ((Number)lookups[0][0]).intValue());
//		}
		
		// Now the syncqueue has been flushed, loop through the objects again and find their flushed counter-part and compare
		for(UIDMeta uidMeta: createdUIDMetas) {
			UIDMeta storedUidMeta = (UIDMeta)modifiedUIDs.remove(uidMeta.toString());
			Assert.assertNotNull("The syncToStore UIDMeta was null", storedUidMeta);
			validate(storedUidMeta, uidMeta);
		}
		for(TSMeta tsMeta: createdTSMetas) {
			TSMeta storedTsMeta = (TSMeta)modifiedTSs.remove(tsMeta.toString());
			Assert.assertNotNull("The syncToStore TSMeta was null", storedTsMeta);
			validate(storedTsMeta, tsMeta);
		}
		for(Annotation annotation: createdAnnotations) {
			Annotation storedAnnotation = (Annotation)modifiedAnnotations.remove(getDeletedKey(annotation));
			Assert.assertNotNull("The syncToStore Annotation was null for key [" + annotation.toString() + "]", storedAnnotation);
			validate(storedAnnotation, annotation);
		}
		// Loop through the objects again and issue delete ops.
		cnt = 0;
		for(Annotation annotation: createdAnnotations) {
			tsdb.deleteAnnotation(annotation);
			cnt++;			
		}
		log("Deleted [%s] Annotations", cnt);
		cnt = 0;
		for(TSMeta tsMeta: createdTSMetas) {
			tsdb.deleteTSMeta(tsMeta.getTSUID());
			cnt++;
		}
		log("Deleted [%s] TSMetas", cnt);
		waitForProcessingQueue("testMetaUpdates/TSMeta Deletes", 30000, TimeUnit.MILLISECONDS);
		cnt = 0;		
		for(UIDMeta uidMeta: createdUIDMetas) {
			tsdb.deleteUIDMeta(uidMeta);
			cnt++;
		}
		log("Deleted [%s] UIDMetas", cnt);
		waitForProcessingQueue("testMetaUpdates/UIDMeta Deletes", 30000, TimeUnit.MILLISECONDS);
		log("Deleted UIDs:%s", deletedUIDs.size());
		log("Deleted TS:%s", deletedTSs.size());
		log("Deleted Annotations:%s", deletedAnnotations.size());
		
		// Validate that each of the deleted items are stored in the mock delete maps
		for(Annotation annotation: createdAnnotations) {
			Annotation deletedAnnotation = (Annotation)deletedAnnotations.remove(getDeletedKey(annotation));
			Assert.assertNotNull("The deleted Annotation was null for key [" + annotation.toString() + "]", deletedAnnotation);
			//validate(deletedAnnotation, annotation);
		}
		Assert.assertEquals("The DB Annotation Count was not zero", 0, jdbcHelper.queryForInt("SELECT COUNT(*) FROM TSD_ANNOTATION"));
		for(TSMeta tsMeta: createdTSMetas) {
			TSMeta deletedTsMeta = (TSMeta)deletedTSs.remove(getDeletedKey(tsMeta));
			Assert.assertNotNull("The deleted TSMeta [" + getDeletedKey(tsMeta) + "] was null", deletedTsMeta);
			//validate(deletedTsMeta, tsMeta);
		}
		Assert.assertEquals("The DB TSMeta Count was not zero", 0, jdbcHelper.queryForInt("SELECT COUNT(*) FROM TSD_TSMETA"));
		log("Deleted UIDMeta Keys %s", deletedUIDs.keySet());
		Set<String> uidDeleteKeys = new HashSet<String>();
		for(UIDMeta uidMeta: createdUIDMetas) {
			UIDMeta deletedUidMeta = (UIDMeta)deletedUIDs.get(getDeletedKey(uidMeta));
			Assert.assertNotNull("The deleted UIDMeta [" + getDeletedKey(uidMeta) + "] was null", deletedUidMeta);
			uidDeleteKeys.add(getDeletedKey(uidMeta));
			//validate(deletedUidMeta, uidMeta);
		}
		for(String s: uidDeleteKeys) deletedUIDs.remove(s);
		
		Assert.assertEquals("The DB TAGK Count was not zero", 0, jdbcHelper.queryForInt("SELECT COUNT(*) FROM TSD_TAGK"));
		Assert.assertEquals("The DB TAGV Count was not zero", 0, jdbcHelper.queryForInt("SELECT COUNT(*) FROM TSD_TAGV"));
		Assert.assertEquals("The DB METRIC Count was not zero", 0, jdbcHelper.queryForInt("SELECT COUNT(*) FROM TSD_METRIC"));
		
		log("Modified UIDs:%s", modifiedUIDs.size());
		log("Modified TS:%s", modifiedTSs.size());
		log("Modified Annotations:%s", modifiedAnnotations.size());
		log("Deleted UIDs:%s", deletedUIDs.size());
		log("Deleted TS:%s", deletedTSs.size());
		log("Deleted Annotations:%s", deletedAnnotations.size());
	}
	
	/**
	 * Tests the meta search facilities
	 * @throws Exception thrown on any error
	 */
	@Test
	public void testTextSearch() throws Exception {
		CatalogDBInterface dbInterface = TSDBCatalogSearchEventHandler.getInstance().getDbInterface();
		dbInterface.purge();
		int fqnCount = jdbcHelper.queryForInt("SELECT COUNT(*) FROM TSD_TSMETA");
		Assert.assertEquals("Unexpected FQN RowCount After Purge", 0, fqnCount);

		Set<ObjectName> ons = ManagementFactory.getPlatformMBeanServer().queryNames(null, null);
		Set<TSMeta> createdTSMetas = new HashSet<TSMeta>(ons.size());
		Set<UIDMeta> createdUIDMetas = new HashSet<UIDMeta>(ons.size()*3);
		Set<Annotation> createdAnnotations = new HashSet<Annotation>(ons.size());
		/*
		 * Iterate through all the MBeans in the platform MBeanserver.
		 * For each, build a TSMeta from the MBean's ObjectName 
		 * where the domain is the metric name and the properties
		 * are tagk/tagv pairs. Index each created TSMeta .
		 */
		for(ObjectName on: ons) {
			LinkedList<UIDMeta> uidMetas = objectNameToUIDMeta(on);
			for(UIDMeta m : uidMetas) {
				if(m==null) continue;
				tsdb.indexUIDMeta(m);
				createdUIDMetas.add(m);
			}
			TSMeta tsMeta = fromUids(uidMetas);
			tsMeta.setCreated(SystemClock.unixTime());
			tsdb.indexTSMeta(tsMeta);
			createdTSMetas.add(tsMeta);
			Annotation ann = new Annotation();
			ann.setTSUID(tsMeta.getTSUID());
			ann.setStartTime(SystemClock.unixTime());
			MBeanInfo minfo = JMXHelper.getMBeanInfo(on);		
			ann.setDescription(minfo.getDescription());
			ann.setNotes(minfo.getClassName());
			tsdb.indexAnnotation(ann);
			createdAnnotations.add(ann);
		}
		waitForProcessingQueue("testSearchMetaUpdates/Indexing", 30000, TimeUnit.MILLISECONDS);
		if(ConfigurationHelper.getBooleanSystemThenEnvProperty("debug.catalog.daemon", false)) {
			Thread.currentThread().join();
		}	
		// =============================================================================
		// Now match each known object name using the search api
		
		for(ObjectName on: ons) {
			// ===============================================
			// Search on ObjectName.DomainName
			// ===============================================
			List<Object> results = tsdb.executeSearch(
					new SearchQueryBuilder()
					.setType(SearchType.UIDMETA)
					.setQuery(on.getDomain()).get())
			.join().getResults();
			Assert.assertEquals("Unexpected number of search results", 1, results.size());
			for(Object result: results) {
				Assert.assertEquals("The result type was unexpected" , UIDMeta.class, result.getClass());
				UIDMeta uidMeta = (UIDMeta)result;
				Assert.assertEquals("The UIDMeta value was unexpected", on.getDomain(), uidMeta.getName());
			}
		}
	}	
	


	
	/** A map where the mock UIDMeta.delete op places deleted UIDMetas */
	public static final Map<String, Object> deletedUIDs = new ConcurrentHashMap<String, Object>();
	/** A map where the mock UIDMeta.syncToStore op places modified UIDMetas */
	public static final Map<String, Object> modifiedUIDs = new ConcurrentHashMap<String, Object>();
	/** A map where the mock TSMeta.delete op places deleted TSMetas */
	public static final Map<String, Object> deletedTSs = new ConcurrentHashMap<String, Object>();
	/** A map where the mock TSMeta.syncToStore op places modified TSMetas */
	public static final Map<String, Object> modifiedTSs = new ConcurrentHashMap<String, Object>();
	/** A map where the mock Annotation.delete op places deleted Annotations */
	public static final Map<String, Object> deletedAnnotations = new ConcurrentHashMap<String, Object>();
	/** A map where the mock Annotation.syncToStore op places updated Annotations */
	public static final Map<String, Object> modifiedAnnotations = new ConcurrentHashMap<String, Object>();
	
	@BeforeClass
	public static void mockClasses() {
		MethodMocker.getInstance().transform(Annotation.class, MockedAnnotation.class);
		MethodMocker.getInstance().transform(TSMeta.class, MockedTSMetaOps.class);
		MethodMocker.getInstance().transform(UIDMeta.class, MockedUIDMetaOps.class);
		MethodMocker.getInstance().transform(TSDB.class, MockedTSDB.class);
	}
	
	@AfterClass
	public static void restoreClasses() {
		MethodMocker.getInstance().restore(Annotation.class);
		MethodMocker.getInstance().restore(TSMeta.class);
		MethodMocker.getInstance().restore(UIDMeta.class);
		deletedUIDs.clear();
		modifiedUIDs.clear();
		deletedTSs.clear();
		modifiedTSs.clear();
		deletedAnnotations.clear();
		modifiedAnnotations.clear();
		
	}

	public static class MockedTSDB {
		public Deferred<Object> addPoint(final String metric, final long timestamp, final double value, final Map<String, String> tags) {			
			return Deferred.fromResult(null);
		}
		public Deferred<Object> addPoint(final String metric, final long timestamp, final long value, final Map<String, String> tags) {			
			return Deferred.fromResult(null);
		}
		public Deferred<Object> addPoint(final String metric, final long timestamp, final float value, final Map<String, String> tags) {			
			return Deferred.fromResult(null);
		}
		
		
	}
	
	public static class MockedTSMetaOps {
		public Deferred<Object> delete(final TSDB tsdb) {
			//log("MOCKED METHOD: %s.delete", getClass().getSimpleName());
			deletedTSs.put(getDeletedKey(this), this);
			return Deferred.fromResult(null);
		}
		
		public Deferred<Boolean> syncToStorage(TSDB tsdb, boolean overwrite) {
			//log("MOCKED METHOD: %s.syncToStorage", getClass().getSimpleName());
			modifiedTSs.put(this.toString(), this);
			return Deferred.fromResult(true);			
		}
	}

	public static class MockedUIDMetaOps {
		public Deferred<Object> delete(final TSDB tsdb) {
			//log("MOCKED METHOD: %s.delete", getClass().getSimpleName());
			deletedUIDs.put(getDeletedKey(this), this);
			return Deferred.fromResult(null);
		}
		
		public Deferred<Boolean> syncToStorage(TSDB tsdb, boolean overwrite) {
			//log("MOCKED METHOD: %s.syncToStorage", getClass().getSimpleName());
			modifiedUIDs.put(this.toString(), this);
			return Deferred.fromResult(true);			
		}
	}
	
	public static class MockedAnnotation {
		public Deferred<Boolean> syncToStorage(TSDB tsdb, Boolean overwrite) {
			//log("MOCKED METHOD: %s.syncToStorage", getClass().getSimpleName());
			modifiedAnnotations.put(getDeletedKey(this), this);
			return Deferred.fromResult(true);			
		}
		public Deferred<Object> delete(final TSDB tsdb) {
			//log("MOCKED METHOD: %s.delete", getClass().getSimpleName());
			deletedAnnotations.put(getDeletedKey(this), this);
			return Deferred.fromResult(null);
		}
	}
		
	
	public static String getDeletedKey(Object x) {
		if(x instanceof Annotation) {
			Annotation ann = (Annotation)x;
			String tsuid = ann.getTSUID();
			return String.format("%s-%s", ann.getStartTime(), tsuid==null ? "null" : tsuid);
		} else if(x instanceof UIDMeta) {
			return "UIDMeta-" + ((UIDMeta)x).getUID();
		} else if(x instanceof TSMeta) {
			return "TSMeta-" + ((TSMeta)x).getTSUID();
		} else {
			return x.getClass().getName() + "-" + x.toString(); 
		}
	}


}
