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

import net.opentsdb.meta.Annotation;
import net.opentsdb.search.ElasticSearchEventHandler;
import net.opentsdb.search.index.IndexOperations;
import net.opentsdb.search.index.PercolateEvent;
import net.opentsdb.utils.JSON;

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
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>test.net.opentsdb.search.SearchEventsTest</code></p>
 */

public class SearchEventsTest extends ESBaseTest {
	/** The ES mapping type name for OpenTSDB annotations */
	protected static String annotationType = null; 
	/** The ES index name for OpenTSDB annotations */
	protected static String annotationIndex = null;
	/** The ES mapping type name for OpenTSDB UIDMetas */
	protected static String uidMetaType = null; 
	/** The ES index name for OpenTSDB UIDMetas */
	protected static String uidMetaIndex = null;
	/** The ES mapping type name for OpenTSDB TSMetas */
	protected static String tsMetaType = null; 
	/** The ES index name for OpenTSDB TSMetas */
	protected static String tsMetaIndex = null;
	
	/** The elastic search client */
	protected static TransportClient client = null;
	/** The es handler index operations client */
	protected static IndexOperations ioClient = null;
	
	
	/**
	 * Initializes the environment for tests in this class
	 */
	@BeforeClass
	public static void initialize() {
		createSearchShellJar();
		tsdb = newTSDB("ESSearchConfig");
		ElasticSearchEventHandler.waitForStart();
		client = ElasticSearchEventHandler.getClient();
		ioClient = ElasticSearchEventHandler.getInstance().getIndexOpsClient();
		annotationType = ElasticSearchEventHandler.getInstance().getAnnotation_type();
		annotationIndex = ElasticSearchEventHandler.getInstance().getAnnotation_index();
		uidMetaType = ElasticSearchEventHandler.getInstance().getTsmeta_type();
		uidMetaIndex = ElasticSearchEventHandler.getInstance().getTsmeta_index();			
		tsMetaType = ElasticSearchEventHandler.getInstance().getUidmeta_type();
		tsMetaIndex = ElasticSearchEventHandler.getInstance().getUidmeta_index();	
		
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
	
	/**
	 * Resets the metrics after each test
	 */
	@After
	public void resetMetrics() {
		printMetrics();
		elapsedTimes.clear();
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
		b.append("\n");
		log(b.toString());
	}
	
	/**
	 * Tests a round trip for an annotation 
	 * @throws Exception thrown on any error
	 */
	@Test
	public void testAnnotationIndex() throws Exception {
		String queryName = null;
		try {
			queryName = ioClient.registerPecolate(annotationIndex, QueryBuilders.fieldQuery("_type", annotationType));
			for(int i = 0; i < 1000; i++) {
				Annotation a = randomAnnotation(3);
//				log("Annotation: [\n%s\n]", JSON.serializeToString(a));
				
//				{
//					"tsuid":"9c9441f8-e12e-4917-807e-33e1a05c7ee4",
//					"description":"3cf248a2-b89f-4b52-82c3-f94c686fe97a",
//					"notes":"",
//					"custom":{"2f8691f6":"625f","c2733ecf":"d8ac","c8e3df52":"f0e2"},
//					"startTime":6644408910471254705,
//					"endTime":6644408910471258705
//					
//				}				
				
				long start = System.currentTimeMillis();
				tsdb.indexAnnotation(a);
				Annotation b = waitOnDocEvent(PercolateEvent.typeMatcher(annotationType), 1, 10000, TimeUnit.MILLISECONDS)
						.iterator().next()
						.resolve(Annotation.class, client);
				elapsedTimes.insert(System.currentTimeMillis()-start);				
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
