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

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import net.opentsdb.meta.Annotation;
import net.opentsdb.meta.TSMeta;
import net.opentsdb.meta.UIDMeta;
import net.opentsdb.search.ElasticSearchEventHandler;
import net.opentsdb.search.index.IndexOperations;
import net.opentsdb.search.index.PercolateEvent;
import net.opentsdb.uid.UniqueId;
import net.opentsdb.utils.JSON;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.indices.IndexMissingException;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.helios.tsdb.plugins.util.unsafe.collections.ConcurrentLongSlidingWindow;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * <p>Title: ParameterizedSearchTest</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>test.net.opentsdb.search.ParameterizedSearchTest</code></p>
 */

public class ParameterizedSearchTest extends ESBaseTest {
	/** The ES mapping type name for OpenTSDB annotations */
	protected static String annotationType = null; 
	/** The ES index name for OpenTSDB annotations */
	protected static String annotationIndex = null;
	/** The ES underlying index name for OpenTSDB annotations */
	protected static String annotationUIndex = null;

	/** The ES mapping type name for OpenTSDB UIDMetas */
	protected static String uidMetaType = null; 
	/** The ES index name for OpenTSDB UIDMetas */
	protected static String uidMetaIndex = null;
	/** The ES underlying index name for OpenTSDB UIDMetas */
	protected static String uidMetaUIndex = null;
	
	/** The ES mapping type name for OpenTSDB TSMetas */
	protected static String tsMetaType = null; 
	/** The ES index name for OpenTSDB TSMetas */
	protected static String tsMetaIndex = null;
	/** The ES underlying index name for OpenTSDB TSMetas */
	protected static String tsMetaUIndex = null;
	
	/** The elastic search client */
	protected static TransportClient client = null;
	/** The es handler index operations client */
	protected static IndexOperations ioClient = null;

	/** The async wait time */
	public static final long ASYNC_WAIT_TIMEOUT = 2000;
	/** The number of events to publish */
	public static final int PUBLISH_COUNT = 1000;
	
	/** The search config profile */
	public static final String SEARCH_CONFIG = "ESSearchConfig";
	
	/** he unique UID types */
	protected static final UniqueId.UniqueIdType[] UID_TYPES = UniqueId.UniqueIdType.values();
	
	/** A map of array with the type, aindex and index keyed by the type of the object whose types and indexes they are */
	protected static final Map<Class<?>, String[]> INDEX_AND_TYPE = new HashMap<Class<?>, String[]>(3);
	
	/**
	 * Initializes the environment for tests in this class
	 */
	@BeforeClass
	public static void initialize() {
		tearDownTSDBAfterTest = false;
		createSearchShellJar();
		tsdb = newTSDB(SEARCH_CONFIG);
		ElasticSearchEventHandler.waitForStart();
		client = ElasticSearchEventHandler.getClient();
		ioClient = ElasticSearchEventHandler.getInstance().getIndexOpsClient();
		annotationType = ElasticSearchEventHandler.getInstance().getAnnotation_type();
		annotationIndex = ElasticSearchEventHandler.getInstance().getAnnotation_index();
		annotationUIndex = ioClient.getIndexForAlias(annotationIndex);
		INDEX_AND_TYPE.put(Annotation.class, new String[]{annotationType, annotationIndex, annotationUIndex});
		tsMetaType = ElasticSearchEventHandler.getInstance().getTsmeta_type();
		tsMetaIndex = ElasticSearchEventHandler.getInstance().getTsmeta_index();
		tsMetaUIndex = ioClient.getIndexForAlias(tsMetaIndex);
		INDEX_AND_TYPE.put(TSMeta.class, new String[]{tsMetaType, tsMetaIndex, tsMetaUIndex});
		uidMetaType = ElasticSearchEventHandler.getInstance().getUidmeta_type();
		uidMetaIndex = ElasticSearchEventHandler.getInstance().getUidmeta_index();	
		uidMetaUIndex = ioClient.getIndexForAlias(tsMetaIndex);
		INDEX_AND_TYPE.put(UIDMeta.class, new String[]{uidMetaType, uidMetaIndex, uidMetaUIndex});
		log("\n\t=======================================\n\tSearchEventsTest Class Initalized\n\t=======================================");
	}

    /**
     * Wipes all documents from the named indexes 
     * @param client The client to pass delete commands through
     * @param names The index names to wipe
     */
    public static void wipeIndices(Client client, String... names) {
        try {
            client.prepareDeleteByQuery(names).setQuery(QueryBuilders.matchAllQuery()).execute().actionGet(5000);
        } catch (IndexMissingException e) {
            /* Ignore */
        }
    }	
	
	/**
	 * Cleans the environment after tests in this class
	 */
	@AfterClass
	public static void shutdown() {
		
		if(client!=null) try { client.close(); } catch (Exception ex) {/* No Op */}
		log("\n\t=======================================\n\tSearchEventsTest Class Torn Down\n\t=======================================");
	}
	
	/** Elapsed times  */
	protected final ConcurrentLongSlidingWindow elapsedTimes = new ConcurrentLongSlidingWindow(1000); 
	/** Published Events */
	protected final AtomicLong publicationCount = new AtomicLong();
	
	/**
	 * Resets the metrics after each test
	 */
	@After
	public void resetMetrics() {
		
		printMetrics();
		FILTER_FAILS.set(0);
		elapsedTimes.clear();
		publicationCount.set(0);
	}
	
	/**
	 * Wipes all indexes before each test
	 */
	@Before
	public void wipeIndxs() {
		wipeIndices(client, annotationIndex, tsMetaIndex, uidMetaIndex, "_percolator");
	}
	
	/**
	 * Prints summary metrics at the end of each test
	 */
	protected void printMetrics() {
		StringBuilder b = new StringBuilder("\nTest Metrics\n============");
		b.append("\n\tAverage Elapsed:").append(elapsedTimes.avg()).append(" ms.");
		b.append("\n\tMax Elapsed:").append(elapsedTimes.max()).append(" ms.");
		b.append("\n\tMin Elapsed:").append(elapsedTimes.min()).append(" ms.");
		b.append("\n\tPublished Events:").append(publicationCount.get()).append(" events.");
		b.append("\n\tFilter Fails:").append(FILTER_FAILS.get()).append(" fails.");
		b.append("\n");
		log(b.toString());
	}
	
	// ==============================================================================================
	//	Annotation Indexing Tests
	// ==============================================================================================
	
	/**
	 * Tests asynchronous indexing of Annotations with percolation enabled
	 * @throws Exception thrown on any error
	 */
	@Test
	public void testAsyncAnnotationIndexing() throws Exception {
		test(Annotation.class, true, true, annotationType, annotationIndex, annotationUIndex);
	}
	
	/**
	 * Tests synchronous indexing of Annotations with percolation enabled
	 * @throws Exception thrown on any error
	 */
	@Test
	public void testSyncAnnotationIndexing() throws Exception {
		test(Annotation.class, false, true, annotationType, annotationIndex, annotationUIndex);
	}

	/**
	 * Tests asynchronous indexing of Annotations with percolation disabled
	 * @throws Exception thrown on any error
	 */
	@Test
	public void testAsyncAnnotationIndexingNoPerc() throws Exception {
		test(Annotation.class, true, false, annotationType, annotationIndex, annotationUIndex);
	}
	
	// ==============================================================================================
	//	UIDMeta Indexing Tests
	// ==============================================================================================
	
	/**
	 * Tests asynchronous indexing of UIDMetas with percolation enabled
	 * @throws Exception thrown on any error
	 */
	@Test
	public void testAsyncUIDMetaIndexing() throws Exception {
		test(UIDMeta.class, true, true, uidMetaType, uidMetaIndex, uidMetaUIndex);
	}
	
	/**
	 * Tests synchronous indexing of UIDMetas with percolation enabled
	 * @throws Exception thrown on any error
	 */
	@Test
	public void testSyncUIDMetaIndexing() throws Exception {
		test(UIDMeta.class, false, true, uidMetaType, uidMetaIndex, uidMetaUIndex);
	}

	/**
	 * Tests asynchronous indexing of UIDMetas with percolation disabled
	 * @throws Exception thrown on any error
	 */
	@Test
	public void testAsyncUIDMetaIndexingNoPerc() throws Exception {
		test(UIDMeta.class, true, false, uidMetaType, uidMetaIndex, uidMetaUIndex);
	}
	
	
	// ==============================================================================================
	//	TSMeta Indexing Tests
	// ==============================================================================================

	/**
	 * Tests asynchronous indexing of TSMetas with percolation enabled
	 * @throws Exception thrown on any error
	 */
	@Test
	public void testAsyncTSMetaIndexing() throws Exception {
		test(TSMeta.class, true, true, tsMetaType, tsMetaIndex, tsMetaUIndex);
	}
	
	/**
	 * Tests synchronous indexing of TSMetas with percolation enabled
	 * @throws Exception thrown on any error
	 */
	@Test
	public void testSyncTSMetaIndexing() throws Exception {
		test(TSMeta.class, false, true, tsMetaType, tsMetaIndex, tsMetaUIndex);
	}

	/**
	 * Tests asynchronous indexing of TSMetas with percolation disabled
	 * @throws Exception thrown on any error
	 */
	@Test
	public void testAsyncTSMetaIndexingNoPerc() throws Exception {
		test(TSMeta.class, true, false, tsMetaType, tsMetaIndex, tsMetaUIndex);
	}
	
	// ==============================================================================================
	//	Indexing Test Impls
	// ==============================================================================================
	
	/**
	 * Parameterized indexing test
	 * @param indexableType The java type of the indexable object (Annotation, TSMeta or UIDMeta)
	 * @param async true for async, false for sync
	 * @param percolate true to enable percolation, false to disable
	 * @param mappingType The es type mapping for the indexable type
	 * @param aliasName The index alias name that the indexable type is indexed to
	 * @param indexName The index actual name that the indexable type is indexed to
	 * @throws Exception thrown on any error
	 */
	public <T> void test(Class<T> indexableType, boolean async, boolean percolate, String mappingType, String aliasName, String indexName) throws Exception {	
		String queryName = null;
		Map<String, T> publishedEvents = null;
		DocEventWaiter waiter = null;
		final String className = indexableType.getSimpleName();
		ioClient.setAsync(async);
		ioClient.setPercolateEnabled(percolate);

		try {
			if(percolate) {
				queryName = ioClient.registerPecolate(indexName, QueryBuilders.fieldQuery("_type", mappingType));
			} else {
				publishedEvents = new HashMap<String, T>(PUBLISH_COUNT);
			}
			for(int i = 0; i < PUBLISH_COUNT; i++) {
				T indexable = randomIndexable(3, indexableType);
				long start = System.currentTimeMillis();
				String aId = getId(indexable);
				if(percolate) {
					waiter = new DocEventWaiter(PercolateEvent.matcher(aId, indexName, mappingType), 1, ASYNC_WAIT_TIMEOUT, TimeUnit.MILLISECONDS);
				} else {
					publishedEvents.put(aId, indexable);
				}				
				index(indexable);
				elapsedTimes.insert(System.currentTimeMillis()-start);
				publicationCount.incrementAndGet();
				
				if(percolate) {
					T retrievedIndexable = waiter.waitForEvent().iterator().next().resolve(indexableType, client, ASYNC_WAIT_TIMEOUT);								
					waiter.cleanup();
					validate(indexable, retrievedIndexable);					
				}				
			}
			if(percolate) return;
			// ================================================
			// No Perc Test Continues Here
			// ================================================
			Assert.assertEquals("Unexpected number of [" + className + "] events in prep-map", PUBLISH_COUNT, publishedEvents.size());
			int loops = 0;
			Set<String> verified = new HashSet<String>(PUBLISH_COUNT);
			while(!publishedEvents.isEmpty()) {
				try {
					SearchResponse sr = client.prepareSearch(indexName)
							.setQuery(QueryBuilders.fieldQuery("_type", mappingType))
							.setSize(PUBLISH_COUNT)
							.execute().actionGet(ASYNC_WAIT_TIMEOUT);
					SearchHits hits = sr.getHits();
					for(SearchHit hit: hits) {
						String id = hit.getId();
						if(verified.contains(id)) continue;
						T t = publishedEvents.get(id);
						Assert.assertNotNull("Retrieved [" + className + "] (" + id + ") was null in loop [" + loops + "]", t);
						T t2 = JSON.parseToObject(hit.source(), indexableType);
						validate(t, t2);
						publishedEvents.remove(id);
						verified.add(id);
					}
				} catch (Exception ex) {
					loge("Verify Error:%s", ex);
					if(loops>10) break;
					Thread.sleep(300);				
				}
				log("Loop #%s: Cleared:%s Remaining:%s", loops, verified.size(), publishedEvents.size());
				loops++;							
			}
			Assert.assertTrue("There were [" + publishedEvents.size() + "] unverified [" + className + "] events", publishedEvents.isEmpty());
			// ================================================
			// All Tests Done Here
			// ================================================

		} finally {
			if(queryName!=null) try { ElasticSearchEventHandler.getInstance().getIndexOpsClient().removePercolate(queryName); } catch (Exception ex) {/* No Op */}			
		}
	}
	
	/**
	 * Asserts the equality of the passed indexable objects
	 * @param c the created indexable object
	 * @param r the retrieved indexable object
	 */
	protected void validate(Object c, Object r) {
		Assert.assertNotNull("The created indexable was null", c);
		Assert.assertNotNull("The retrieved indexable was null", r);
		Assert.assertEquals("The retrieved indexable is not the same type as the created", c.getClass(), r.getClass());
		if(c instanceof Annotation) {
			Annotation a = (Annotation)c, a2 = (Annotation)r;
			Assert.assertEquals("The annotation TSUIDs do not match", a.getTSUID(), a2.getTSUID());
			Assert.assertEquals("The annotation Start Times do not match", a.getStartTime(), a2.getStartTime());
			Assert.assertEquals("The annotation Descriptions do not match", a.getDescription(), a2.getDescription());
			Assert.assertEquals("The annotation Notes do not match", a.getNotes(), a2.getNotes());
			Assert.assertEquals("The annotation Customs do not match", a.getCustom(), a2.getCustom());						
		} else if(c instanceof TSMeta) {
			TSMeta t = (TSMeta)c, t2 = (TSMeta)r;
			Assert.assertEquals("The TSMeta TSUIDs do not match", t.getTSUID(), t2.getTSUID());
			Assert.assertEquals("The TSMeta Create Times do not match", t.getCreated(), t2.getCreated());
			Assert.assertEquals("The TSMeta descriptions do not match", t.getDescription(), t2.getDescription());
			Assert.assertEquals("The TSMeta notes do not match", t.getNotes(), t2.getNotes());
			Assert.assertEquals("The TSMeta customs do not match", t.getCustom(), t2.getCustom());
		} else if(c instanceof UIDMeta) {
			UIDMeta u = (UIDMeta)c, u2 = (UIDMeta)r;
			Assert.assertEquals("The UIDMeta UIDs do not match", u.getUID(), u2.getUID());
			Assert.assertEquals("The UIDMeta Create Times do not match", u.getCreated(), u2.getCreated());
			Assert.assertEquals("The UIDMeta descriptions do not match", u.getDescription(), u2.getDescription());
			Assert.assertEquals("The UIDMeta notes do not match", u.getNotes(), u2.getNotes());
			Assert.assertEquals("The UIDMeta customs do not match", u.getCustom(), u2.getCustom());
		} else {
			throw new RuntimeException("Invalid Indexable Type [" + c.getClass().getName() + "]");
		}
	}
	
	
	/**
	 * Indexes the passed indexable object
	 * @param obj the indexable object to index
	 */
	protected void index(Object obj) {
		if(obj instanceof Annotation) {
			tsdb.indexAnnotation((Annotation)obj);
		} else if(obj instanceof TSMeta) {
			tsdb.indexTSMeta((TSMeta)obj);
		} else if(obj instanceof UIDMeta) {
			tsdb.indexUIDMeta((UIDMeta)obj);
		} else {
			throw new RuntimeException("Invalid Indexable Type [" + obj.getClass().getName() + "]");
		}
	}

	
	/**
	 * Returns the ID of the passed indexable object
	 * @param obj The indexable object
	 * @return the id
	 */
	protected String getId(Object obj) {
		if(obj instanceof Annotation) {
			Annotation a = (Annotation)obj;
			return String.format("%s%s%s", INDEX_AND_TYPE.get(obj.getClass())[0], a.getStartTime(), (a.getTSUID()==null ? "" : a.getTSUID()));
		} else if(obj instanceof TSMeta) {
			return ((TSMeta)obj).getTSUID();
		} else if(obj instanceof UIDMeta) {
			return ((UIDMeta)obj).getUID() + uidMetaType;
		} else {
			throw new RuntimeException("Invalid Indexable Type [" + obj.getClass().getName() + "]");
		}
	}
	
	/**
	 * Generates an indexable object loaded with random data (i.e. an Annotation, TSMeta or UIDMeta)
	 * @param customs The number of custom tags
	 * @param type The type (Annotation, TSMeta or UIDMeta)
	 * @return the created instance
	 */
	protected <T> T randomIndexable(int customs, Class<T> type) {
		if(Annotation.class.equals(type)) {
			Annotation a = new Annotation();
			if(customs>0) {
				HashMap<String, String> custs = new LinkedHashMap<String, String>(customs);
				for(int c = 0; c < customs; c++) {
					String[] frags = getRandomFragments();
					custs.put("afield#" + c, frags[1]);
				}
				a.setCustom(custs);
			}
			a.setDescription(getRandomFragment());
			long start = nextPosLong();
			a.setStartTime(start);
			a.setEndTime(start + nextPosInt(10000));
			a.setTSUID(getRandomFragment());
			return (T)a;			
		} else if(TSMeta.class.equals(type)) {
			TSMeta t = new TSMeta(getRandomFragment());
			if(customs>0) {
				HashMap<String, String> custs = new LinkedHashMap<String, String>(customs);
				for(int c = 0; c < customs; c++) {
					String[] frags = getRandomFragments();
					custs.put("tfield#" + c, frags[1]);
				}
				t.setCustom(custs);
			}
			t.setNotes(getRandomFragment());
			t.setDescription(getRandomFragment());
			t.setDisplayName(getRandomFragment());
			t.setCreated(nextPosLong());
			return (T)t;			
		} else if(UIDMeta.class.equals(type)) {
			
			UIDMeta u = new UIDMeta(UID_TYPES[nextPosInt(3)], getRandomFragment().getBytes(), getRandomFragment());
			if(customs>0) {
				HashMap<String, String> custs = new LinkedHashMap<String, String>(customs);
				for(int c = 0; c < customs; c++) {
					String[] frags = getRandomFragments();
					custs.put("ufield#" + c, frags[1]);
				}
				u.setCustom(custs);
			}
			u.setNotes(getRandomFragment());
			u.setDescription(getRandomFragment());
			u.setDisplayName(getRandomFragment());
			u.setCreated(nextPosLong());
			return (T)u;						
		} else {
			throw new RuntimeException("Invalid Indexable Type [" + type.getName() + "]");
		}
	}

}
