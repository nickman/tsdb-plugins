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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import net.opentsdb.meta.Annotation;
import net.opentsdb.meta.TSMeta;
import net.opentsdb.meta.UIDMeta;
import net.opentsdb.search.ElasticSearchEventHandler;
import net.opentsdb.search.SearchQuery;
import net.opentsdb.search.SearchQuery.SearchType;
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
	
	/** The maximum number of retrieval loops to allow */
	public static final int MAX_RLOOPS = PUBLISH_COUNT;

	
	/** he unique UID types */
	protected static final UniqueId.UniqueIdType[] UID_TYPES = UniqueId.UniqueIdType.values();
	
	/** A map of array with the type, aindex and index keyed by the type of the object whose types and indexes they are */
	protected static final Map<Class<?>, String[]> INDEX_AND_TYPE = new HashMap<Class<?>, String[]>(3);
	
	/**
	 * Configures the TSDB for all tests in this class.
	 */
	protected static void configureTSDB() {
		tsdb = newTSDB("ESSearchConfig");
	}
	
	/**
	 * Initializes the environment for tests in this class
	 */
	@BeforeClass
	public static void initialize() {
		tearDownTSDBAfterTest = false;
		createSearchShellJar();
		configureTSDB();
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
	 * @throws Exception thrown on any error
	 */
	@AfterClass
	public static void shutdown() throws Exception {
		tearDownAfterClass();
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
		test(Annotation.class, true, true, false, annotationType, annotationIndex, annotationUIndex);
	}
	
	/**
	 * Tests synchronous indexing of Annotations with percolation enabled
	 * @throws Exception thrown on any error
	 */
	@Test
	public void testSyncAnnotationIndexing() throws Exception {
		test(Annotation.class, false, true, false, annotationType, annotationIndex, annotationUIndex);
	}

	/**
	 * Tests asynchronous indexing of Annotations with percolation disabled
	 * @throws Exception thrown on any error
	 */
	@Test
	public void testAsyncAnnotationIndexingNoPerc() throws Exception {
		test(Annotation.class, true, false, false, annotationType, annotationIndex, annotationUIndex);
	}
	
	/**
	 * Tests asynchronous indexing of Annotations and retrieval through {@link net.opentsdb.core.TSDB#executeSearch(net.opentsdb.search.SearchQuery)} 
	 * @throws Exception thrown on any error
	 */
	@Test
	public void testAsyncAnnotationIndexingWithTSDBSearch() throws Exception {
		test(Annotation.class, true, false, true, annotationType, annotationIndex, annotationUIndex);
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
		test(UIDMeta.class, true, true, false, uidMetaType, uidMetaIndex, uidMetaUIndex);
	}
	
	/**
	 * Tests synchronous indexing of UIDMetas with percolation enabled
	 * @throws Exception thrown on any error
	 */
	@Test
	public void testSyncUIDMetaIndexing() throws Exception {
		test(UIDMeta.class, false, true, false, uidMetaType, uidMetaIndex, uidMetaUIndex);
	}

	/**
	 * Tests asynchronous indexing of UIDMetas with percolation disabled
	 * @throws Exception thrown on any error
	 */
	@Test
	public void testAsyncUIDMetaIndexingNoPerc() throws Exception {
		test(UIDMeta.class, true, false, false, uidMetaType, uidMetaIndex, uidMetaUIndex);
	}
	
	/**
	 * Tests asynchronous indexing of UIDMetas and retrieval through {@link net.opentsdb.core.TSDB#executeSearch(net.opentsdb.search.SearchQuery)} 
	 * @throws Exception thrown on any error
	 */
	@Test
	public void testAsyncUIDMetaIndexingWithTSDBSearch() throws Exception {
		test(UIDMeta.class, true, false, true, uidMetaType, uidMetaIndex, uidMetaUIndex);
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
		test(TSMeta.class, true, true, false, tsMetaType, tsMetaIndex, tsMetaUIndex);
	}
	
	/**
	 * Tests synchronous indexing of TSMetas with percolation enabled
	 * @throws Exception thrown on any error
	 */
	@Test
	public void testSyncTSMetaIndexing() throws Exception {
		test(TSMeta.class, false, true, false, tsMetaType, tsMetaIndex, tsMetaUIndex);
	}

	/**
	 * Tests asynchronous indexing of TSMetas with percolation disabled
	 * @throws Exception thrown on any error
	 */
	@Test
	public void testAsyncTSMetaIndexingNoPerc() throws Exception {
		test(TSMeta.class, true, false, false, tsMetaType, tsMetaIndex, tsMetaUIndex);
	}
	
	/**
	 * Tests asynchronous indexing of TSMetas and retrieval through {@link net.opentsdb.core.TSDB#executeSearch(net.opentsdb.search.SearchQuery)} 
	 * @throws Exception thrown on any error
	 */
	@Test
	public void testAsyncTSMetaIndexingWithTSDBSearch() throws Exception {
		test(TSMeta.class, true, false, true, tsMetaType, tsMetaIndex, tsMetaUIndex);
	}	
	
	// ==============================================================================================
	//	Indexing Test Impls
	// ==============================================================================================
	
	/**
	 * Parameterized indexing test
	 * @param indexableType The java type of the indexable object (Annotation, TSMeta or UIDMeta)
	 * @param async true for async, false for sync
	 * @param percolate true to enable percolation, false to disable
	 * @param tsdbSearch true to enable retrieval through {@link net.opentsdb.core.TSDB#executeSearch(net.opentsdb.search.SearchQuery)}, false otherwise
	 * @param mappingType The es type mapping for the indexable type
	 * @param aliasName The index alias name that the indexable type is indexed to
	 * @param indexName The index actual name that the indexable type is indexed to
	 * @throws Exception thrown on any error
	 */
	public <T> void test(Class<T> indexableType, boolean async, boolean percolate, boolean tsdbSearch, String mappingType, String aliasName, String indexName) throws Exception {	
		String queryName = null;
		Map<String, T> publishedEvents = null;
		DocEventWaiter waiter = null;
		int validatedEvents = 0;
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
					validatedEvents++;
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
					Collection<T> hits = tsdbSearch ? 
							findEventsByES(indexableType, indexName, mappingType) :
								findEventsByTSDB(indexableType, indexName, mappingType);
					for(T hit: hits) {
						String id = getId(hit);
						if(verified.contains(id)) continue;
						T t = publishedEvents.get(id);
						Assert.assertNotNull("Retrieved [" + className + "] (" + id + ") was null in loop [" + loops + "]", t);
						validate(t, hit);
						validatedEvents++;
						publishedEvents.remove(id);
						verified.add(id);
					}
				} catch (Exception ex) {
					loge("Verify Error:%s", ex);
					if(loops>MAX_RLOOPS) break;
					Thread.sleep(300);				
				}
				log("Loop #%s: Cleared:%s Remaining:%s", loops, verified.size(), publishedEvents.size());
				loops++;		
				if(loops>MAX_RLOOPS) break;
			}			
			Assert.assertTrue("There were [" + publishedEvents.size() + "] unverified [" + className + "] events", publishedEvents.isEmpty());
			// ================================================
			// All Tests Done Here
			// ================================================
			Assert.assertEquals("Unexpected number of validated events", PUBLISH_COUNT, validatedEvents);
		} finally {
			if(queryName!=null) try { ElasticSearchEventHandler.getInstance().getIndexOpsClient().removePercolate(queryName); } catch (Exception ex) {/* No Op */}			
		}
	}
	
	
	/**
	 * Uses the ES API to find all documents of the passed mapping type 
	 * in the named index and returns them unmarshalled into objects of the passed type
	 * @param indexableType The type to unmarshal documents into
	 * @param indexName The index name to search
	 * @param mappingType The mapping type to retreive documents for
	 * @return a collection of found objects
	 */
	protected <T> Collection<T> findEventsByES(Class<T> indexableType, String indexName, String mappingType) {
		SearchResponse sr = client.prepareSearch(indexName)
				.setQuery(QueryBuilders.fieldQuery("_type", mappingType))
				.setSize(PUBLISH_COUNT)
				.execute().actionGet(ASYNC_WAIT_TIMEOUT);
		SearchHits hits = sr.getHits();
		List<T> foundEvents = new ArrayList<T>((int)hits.getTotalHits());
		for(SearchHit hit: hits) {
			foundEvents.add(JSON.parseToObject(hit.source(), indexableType));
		}
		return foundEvents;
	}
	
	/**
	 * Uses {@link net.opentsdb.core.TSDB#executeSearch(net.opentsdb.search.SearchQuery)} to find all documents 
	 * of the passed mapping type in the named index and returns them unmarshalled into objects of the passed type
	 * @param indexableType The type to unmarshal documents into
	 * @param indexName The index name to search
	 * @param mappingType The mapping type to retreive documents for
	 * @return a collection of found objects
	 * @throws Exception thrown on the wait on the deferred results
	 */
	protected <T> Collection<T> findEventsByTSDB(Class<T> indexableType, String indexName, String mappingType) throws Exception {
		SearchType searchType = null;
		if(Annotation.class.equals(indexableType)) {
			searchType = SearchType.ANNOTATION;
		} else if(UIDMeta.class.equals(indexableType)) {
			searchType = SearchType.UIDMETA;
		} else if(TSMeta.class.equals(indexableType)) {
			searchType = SearchType.TSMETA;
		} else {
			throw new RuntimeException("Invalid Indexable Type [" + indexableType.getName() + "]");
		}
		SearchQuery sq = new SearchQuery();
		sq.setType(searchType);
		sq.setQuery("_type:" + mappingType);
		sq.setLimit(PUBLISH_COUNT);		
		return (Collection<T>) tsdb.executeSearch(sq).joinUninterruptibly().getResults();
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
