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
import java.util.LinkedHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import net.opentsdb.meta.Annotation;
import net.opentsdb.search.ElasticSearchEventHandler;
import net.opentsdb.search.index.IndexOperations;
import net.opentsdb.search.index.PercolateEvent;

import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.indices.IndexMissingException;
import org.helios.tsdb.plugins.util.unsafe.collections.ConcurrentLongSlidingWindow;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * <p>Title: SearchEventsTest</p>
 * <p>Description: Test cases for round-trip search events through ES.</p> 
 * <p>For each of Annotation, TSMeta and UIDMeta:<ol>
 * 	<li>Async Publishing, validate via Percolate</li>
 * 	<li>Sync Publishing, validate via Percolate</li>
 * 	<li>Sync Publishing, validate response</li>
 * 	<li>TSDB SearchQuery</li>
 * 	<li>Vary index config ? i.e. seperate index per type</li>
 * </ol></p>
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>test.net.opentsdb.search.SearchEventsTest</code></p>
 */

public class SearchEventsTest extends ESBaseTest {
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
	public static final long ASYNC_WAIT_TIMEOUT = 1000;
	
	/**
	 * Initializes the environment for tests in this class
	 */
	@BeforeClass
	public static void initialize() {
		tearDownTSDBAfterTest = false;
		createSearchShellJar();
		tsdb = newTSDB("ESSearchConfig");
		ElasticSearchEventHandler.waitForStart();
		client = ElasticSearchEventHandler.getClient();
		ioClient = ElasticSearchEventHandler.getInstance().getIndexOpsClient();
		annotationType = ElasticSearchEventHandler.getInstance().getAnnotation_type();
		annotationIndex = ElasticSearchEventHandler.getInstance().getAnnotation_index();
		annotationUIndex = ioClient.getIndexForAlias(annotationIndex);
		uidMetaType = ElasticSearchEventHandler.getInstance().getTsmeta_type();
		uidMetaIndex = ElasticSearchEventHandler.getInstance().getTsmeta_index();
		uidMetaUIndex = ioClient.getIndexForAlias(uidMetaIndex);
		tsMetaType = ElasticSearchEventHandler.getInstance().getUidmeta_type();
		tsMetaIndex = ElasticSearchEventHandler.getInstance().getUidmeta_index();	
		tsMetaUIndex = ioClient.getIndexForAlias(tsMetaIndex);
		log("\n\t=======================================\n\tSearchEventsTest Class Initalized\n\t=======================================");
	}
	
    /**
     * @param client
     * @param names
     */
    public static void wipeIndices(Client client, String... names) {
        try {
            client.prepareDeleteByQuery(names).setQuery(QueryBuilders.matchAllQuery()).execute().actionGet(5000);
        } catch (IndexMissingException e) {
            // ignore
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
	
	/**
	 * Asynchronous Annotation indexing 
	 * @throws Exception thrown on any error
	 */
	@Test
	public void testAsynchronousAnnotationIndexing() throws Exception {		
		String queryName = null;
		ioClient.setAsync(true);
		ioClient.setPercolateEnabled(true);
		try {
			queryName = ioClient.registerPecolate(annotationIndex, QueryBuilders.fieldQuery("_type", annotationType));
			for(int i = 0; i < 1000; i++) {
				Annotation a = randomAnnotation(3);
				long start = System.currentTimeMillis();
				String aId = getAnnotationId(annotationType, a);
				DocEventWaiter waiter = new DocEventWaiter(PercolateEvent.matcher(aId, annotationUIndex, annotationType), 1, ASYNC_WAIT_TIMEOUT, TimeUnit.MILLISECONDS);
				tsdb.indexAnnotation(a);
				publicationCount.incrementAndGet();
				Annotation b = waiter.waitForEvent().iterator().next().resolve(Annotation.class, client, ASYNC_WAIT_TIMEOUT);
				elapsedTimes.insert(System.currentTimeMillis()-start);				
				waiter.cleanup();
				Assert.assertEquals("The annotation TSUID does not match", a.getTSUID(), b.getTSUID());
				Assert.assertEquals("The annotation Start Time does not match", a.getStartTime(), b.getStartTime());
				Assert.assertEquals("The annotation Start Time does not match", a.getDescription(), b.getDescription());
				Assert.assertEquals("The annotation Start Time does not match", a.getNotes(), b.getNotes());
			}
		} finally {
			if(queryName!=null) try { ElasticSearchEventHandler.getInstance().getIndexOpsClient().removePercolate(queryName); } catch (Exception ex) {/* No Op */}
			
		}
	}
	
	/**
	 * Ssynchronous Annotation indexing 
	 * @throws Exception thrown on any error
	 */
	@Test
	public void testSynchronousAnnotationIndexing() throws Exception {		
		String queryName = null;
		ioClient.setAsync(false);
		ioClient.setPercolateEnabled(true);
		try {
			queryName = ioClient.registerPecolate(annotationIndex, QueryBuilders.fieldQuery("_type", annotationType));
			for(int i = 0; i < 1000; i++) {
				Annotation a = randomAnnotation(3);
				long start = System.currentTimeMillis();
				String aId = getAnnotationId(annotationType, a);
				DocEventWaiter waiter = new DocEventWaiter(PercolateEvent.matcher(aId, annotationUIndex, annotationType), 1, ASYNC_WAIT_TIMEOUT, TimeUnit.MILLISECONDS);
				tsdb.indexAnnotation(a);
				publicationCount.incrementAndGet();
				Annotation b = waiter.waitForEvent().iterator().next().resolve(Annotation.class, client, ASYNC_WAIT_TIMEOUT);
				elapsedTimes.insert(System.currentTimeMillis()-start);				
				waiter.cleanup();
				Assert.assertEquals("The annotation TSUID does not match", a.getTSUID(), b.getTSUID());
				Assert.assertEquals("The annotation Start Time does not match", a.getStartTime(), b.getStartTime());
				Assert.assertEquals("The annotation Start Time does not match", a.getDescription(), b.getDescription());
				Assert.assertEquals("The annotation Start Time does not match", a.getNotes(), b.getNotes());
			}
		} finally {
			if(queryName!=null) try { ElasticSearchEventHandler.getInstance().getIndexOpsClient().removePercolate(queryName); } catch (Exception ex) {/* No Op */}
			
		}
	}
	
	
	

	/**
	 * Creates an annotation with random content
	 * @param customs The number of custom values
	 * @return an annotation
	 */
	protected Annotation randomAnnotation(int customs) {
		Annotation a = new Annotation();
		
		if(customs>0) {
			HashMap<String, String> custs = new LinkedHashMap<String, String>(customs);
			for(int c = 0; c < customs; c++) {
				String[] frags = getRandomFragments();
				custs.put("field#" + c, frags[1]);
			}
			a.setCustom(custs);
		}
		a.setDescription(getRandomFragment());
		long start = nextPosLong();
		a.setStartTime(start);
		a.setEndTime(start + nextPosInt(10000));
		a.setTSUID(getRandomFragment());
		return a;
	}
	
    /**
     * Returns the document ID for the passed annotation
     * @param annotationTypeName The ES annotation type name
     * @param annotation the annotation to get the ID for
     * @return the ID of the annotation
     */
    public String getAnnotationId(String annotationTypeName, Annotation annotation) {
    	return String.format("%s%s%s", annotationTypeName, annotation.getStartTime(), (annotation.getTSUID()==null ? "" : annotation.getTSUID()));
    }
    

}
