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

import net.opentsdb.core.TSDB;
import net.opentsdb.meta.Annotation;
import net.opentsdb.search.ElasticSearchEventHandler;
import net.opentsdb.utils.JSON;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.percolate.PercolateResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.Priority;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.junit.Assert;
import org.junit.Test;

/**
 * <p>Title: AnnotationTest</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>test.net.opentsdb.search.AnnotationTest</code></p>
 */

public class AnnotationTest extends ESBaseTest {

	/**
	 * Tests a round trip for an annotation 
	 * @throws Exception thrown on any error
	 */
	@Test
	public void testAnnotationIndex() throws Exception {
		TransportClient tc = null;
		try {
			createSearchShellJar();
			TSDB tsdb = newTSDB("ESSearchConfig");
			ElasticSearchEventHandler.waitForStart();
			String annotationType = ElasticSearchEventHandler.getInstance().getAnnotation_type();
			String annotationIndex = ElasticSearchEventHandler.getInstance().getAnnotation_index();
			Annotation a = new Annotation();
			int customs = 3;
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
			
			tc = ElasticSearchEventHandler.getClient();
			String aId = getAnnotationId(annotationType, a);
			QueryBuilder qb = QueryBuilders.termQuery("description", a.getDescription());
//			
			IndexRequestBuilder irb = tc.prepareIndex("_percolator", "opentsdb_1", "PingMe")
		    .setSource(jsonBuilder().startObject()
		            //.field("query", qb)
		    		.field("query", qb)
		            .endObject())
//				.setType("annotation")
		        .setRefresh(true);
			
//			log("Prep Index for Perc:[\n%s\n]", JSON.serializeToString(irb));
			IndexResponse ir = irb.execute().actionGet();
			//tc.admin().cluster().prepareHealth().setWaitForEvents(Priority.LANGUID).setWaitForGreenStatus().execute().actionGet();
			log("\n\t#################\n\tGreen Light\n\t#################");
			
			tsdb.indexAnnotation(a);
			SearchRequestBuilder sbr = tc.prepareSearch(annotationIndex)
					.setQuery(qb);
//			log("Query Built:[\n%s\n]", JSON.serializeToString(qb));
			boolean matched = false;
			for(int i = 0; i < 10; i++) {
				SearchResponse response = sbr.execute().actionGet();
				if(response.getHits().getHits().length>0) {
					SearchHit hit = response.getHits().getHits()[0];
					Annotation b = JSON.parseToObject(hit.source(), Annotation.class);
					log("Annotation:[%s]", b);
					Assert.assertEquals("The annotation TSUID does not match", a.getTSUID(), b.getTSUID());
					Assert.assertEquals("The annotation Start Time does not match", a.getStartTime(), b.getStartTime());
					Assert.assertEquals("The annotation Start Time does not match", a.getDescription(), b.getDescription());
					Assert.assertEquals("The annotation Start Time does not match", a.getNotes(), b.getNotes());
					matched = true;
					break;
				}
				
				Thread.sleep(300);
			}
			if(!matched) Assert.fail("No annotation match");
		} catch (Exception ex) {
			loge("FAIL", ex);
			throw new RuntimeException(ex);
		} finally {
			if(tc!=null) try { tc.close(); } catch (Exception ex) {}
		}
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
