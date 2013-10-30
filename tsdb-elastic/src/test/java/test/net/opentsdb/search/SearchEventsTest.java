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

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.concurrent.TimeUnit;

import net.opentsdb.meta.Annotation;
import net.opentsdb.search.ElasticSearchEventHandler;
import net.opentsdb.search.index.PercolateEvent;

import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.Assert;
import org.junit.Test;

/**
 * <p>Title: SearchEventsTest</p>
 * <p>Description: Test cases for round-trip search events through ES.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>test.net.opentsdb.search.SearchEventsTest</code></p>
 */

public class SearchEventsTest extends ESBaseTest {

	
	
	/**
	 * Tests a round trip for an annotation 
	 * @throws Exception thrown on any error
	 */
	@Test
	public void testAnnotationIndex() throws Exception {
		TransportClient tc = null;
		try {
			createSearchShellJar();
			tsdb = newTSDB("ESSearchConfig");
			ElasticSearchEventHandler.waitForStart();
			String annotationType = ElasticSearchEventHandler.getInstance().getAnnotation_type();
			String annotationIndex = ElasticSearchEventHandler.getInstance().getAnnotation_index();			
						
			tc = ElasticSearchEventHandler.getClient();

			IndexRequestBuilder irb = tc.prepareIndex("_percolator", "opentsdb_1", "PingMe")
		    .setSource(jsonBuilder().startObject()
		    		.field("query", QueryBuilders.fieldQuery("_type", annotationType))
		            .endObject())
		        .setRefresh(true);
			
			irb.execute().actionGet();
			Annotation a = randomAnnotation(3);
			long start = System.currentTimeMillis();
			tsdb.indexAnnotation(a);
			Annotation b = waitOnDocEvent(PercolateEvent.typeMatcher(annotationType), 1, 2000, TimeUnit.MILLISECONDS)
					.iterator().next()
					.resolve(Annotation.class, tc);
			long elapsed = System.currentTimeMillis()-start;
			log("Retrieved PercolatedEvent for Annotation in [%s] ms. Value:[%s]", elapsed,  b);
			Assert.assertEquals("The annotation TSUID does not match", a.getTSUID(), b.getTSUID());
			Assert.assertEquals("The annotation Start Time does not match", a.getStartTime(), b.getStartTime());
			Assert.assertEquals("The annotation Start Time does not match", a.getDescription(), b.getDescription());
			Assert.assertEquals("The annotation Start Time does not match", a.getNotes(), b.getNotes());
		} finally {
			if(tc!=null) try { tc.close(); } catch (Exception ex) {}
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
				custs.put(frags[0], frags[1]);
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
