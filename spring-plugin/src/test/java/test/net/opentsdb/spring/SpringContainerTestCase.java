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
package test.net.opentsdb.spring;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import net.opentsdb.core.TSDB;
import net.opentsdb.meta.Annotation;
import net.opentsdb.spring.ApplicationTSDBEvent;
import net.opentsdb.spring.SpringContainerService;

import org.helios.tsdb.plugins.datapoints.DataPoint;
import org.helios.tsdb.plugins.datapoints.DoubleDataPoint;
import org.helios.tsdb.plugins.datapoints.FloatDataPoint;
import org.helios.tsdb.plugins.datapoints.LongDataPoint;
import org.helios.tsdb.plugins.event.TSDBEventType;
import org.helios.tsdb.plugins.event.TSDBPublishEvent;
import org.helios.tsdb.plugins.event.TSDBSearchEvent;
import org.helios.tsdb.plugins.handlers.impl.QueuedResultPublishEventHandler;
import org.helios.tsdb.plugins.handlers.impl.QueuedResultSearchEventHandler;
import org.helios.tsdb.plugins.test.BaseTest;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.support.GenericApplicationContext;

/**
 * <p>Title: SpringContainerTestCase</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>test.net.opentsdb.spring.SpringContainerTestCase</code></p>
 */

public class SpringContainerTestCase extends BaseTest {

	/**
	 * Validates that annotations submitted for indexing are delivered
	 * @throws Exception thrown on any error
	 */
	@Test(timeout=5000)
	//@Test
	public void testIndexAnnotationDelivery() throws Exception {
		createSearchShellJar();
//		try {
			TSDB tsdb = newTSDB("SpringSearchConfig");
			GenericApplicationContext appCtx = SpringContainerService.getInstance().getAppContext();
			QueuedResultSearchEventHandler handler = appCtx.getBean("EventHandler", QueuedResultSearchEventHandler.class);
			Assert.assertNotNull("The spring configured handler was null", handler);
			BlockingQueue<Object> events = handler.getResultQueue();
			Assert.assertNotNull("The spring configured event queue was null", events);
			int eventCount = 1000;
			int receivedEventCount = 0;
			Map<String, Annotation> annotations = startAnnotationStream(tsdb, eventCount, 2, 0);		
			for(int i = 0; i < eventCount; i++) {
				TSDBSearchEvent event = (TSDBSearchEvent)((ApplicationEvent)events.take()).getSource();			
				Annotation annot = annotations.get(event.annotation.getTSUID() + "/" + event.annotation.getStartTime());
				Assert.assertEquals("[" + i + "] Unexpected event type", TSDBEventType.ANNOTATION_INDEX, event.eventType);
				Assert.assertEquals("[" + i + "] Annotation mismatch: EndTime", annot.getEndTime(), event.annotation.getEndTime());
				Assert.assertEquals("[" + i + "] Annotation mismatch: StartTime", annot.getStartTime(), event.annotation.getStartTime());
				Assert.assertEquals("[" + i + "] Annotation mismatch: Custom", annot.getCustom().toString(), event.annotation.getCustom().toString());
				Assert.assertEquals("[" + i + "] Annotation mismatch: Description", annot.getDescription(), event.annotation.getDescription());
				Assert.assertEquals("[" + i + "] Annotation mismatch: Notes", annot.getNotes(), event.annotation.getNotes());
				Assert.assertEquals("[" + i + "] Annotation mismatch: TSUID", annot.getTSUID(), event.annotation.getTSUID());
				receivedEventCount++;
			}
			Assert.assertEquals("Unexpected received event count", eventCount, receivedEventCount);
			log("Processed [%s] Events", receivedEventCount);
			//try { Thread.sleep(3000000); } catch (Exception ex) {}
//		} catch (Exception ex) {
//			ex.printStackTrace(System.err);
//			throw ex;
//		}
	}
	
	/**
	 * Validates that data points submitted for publication are delivered
	 * @throws Exception thrown on any error
	 */
	//@Test(timeout=5000)
	@Test
	public void testDataPointDelivery() throws Exception {
		createPublishShellJar();
		TSDB tsdb = newTSDB("SpringPublishConfig");
		GenericApplicationContext appCtx = SpringContainerService.getInstance().getAppContext();
		QueuedResultSearchEventHandler handler = appCtx.getBean("EventHandler", QueuedResultSearchEventHandler.class);
		Assert.assertNotNull("The spring configured handler was null", handler);
		BlockingQueue<Object> events = handler.getResultQueue();
		Assert.assertNotNull("The spring configured event queue was null", events);
		int eventCount = 1000;
		int receivedEventCount = 0;
		Map<Class<? extends DataPoint>, int[]> typeCounts = new HashMap<Class<? extends DataPoint>, int[]>(3);
		typeCounts.put(LongDataPoint.class, new int[]{0}); typeCounts.put(FloatDataPoint.class, new int[]{0}); typeCounts.put(DoubleDataPoint.class, new int[]{0});
		Map<String, DataPoint> datapoints = startDataPointStream(tsdb, eventCount, 2, 0);		
		for(int i = 0; i < eventCount; i++) {
			TSDBPublishEvent event = (TSDBPublishEvent)((ApplicationEvent)events.take()).getSource();
			Assert.assertNotNull("Taken Publish Event Was Null", event);
			DataPoint sp1 = DataPoint.newDataPoint(event);			
			DataPoint sp2 = datapoints.get(sp1.getKey());
			Assert.assertNotNull("[" + i + "] Failed to find matching datapoint for [" + sp1.getKey() + "]", sp2);
			Assert.assertEquals("[" + i + "] DataPoints are not equal", sp1, sp2);
			receivedEventCount++;
			typeCounts.get(sp2.getClass())[0]++;
		}
		Assert.assertEquals("Unexpected received event count", eventCount, receivedEventCount);
		log("Processed [%s] Events", receivedEventCount);
		StringBuilder b = new StringBuilder("\nData Point Type Counts:");
		for(Map.Entry<Class<? extends DataPoint>, int[]> entry: typeCounts.entrySet()) {
			b.append("\n\t").append(entry.getKey().getSimpleName()).append(" :").append(entry.getValue()[0]);
		}
		b.append("\n");
		log(b.toString());
	}
	
	

}
