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

import java.util.Map;
import java.util.concurrent.BlockingQueue;

import net.opentsdb.core.TSDB;
import net.opentsdb.meta.Annotation;
import net.opentsdb.spring.SpringContainerService;

import org.helios.tsdb.plugins.event.TSDBEventType;
import org.helios.tsdb.plugins.event.TSDBSearchEvent;
import org.helios.tsdb.plugins.handlers.impl.QueuedResultSearchEventHandler;
import org.helios.tsdb.plugins.test.BaseTest;
import org.junit.Assert;
import org.junit.Test;
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
	//@Test(timeout=5000)
	@Test
	public void testIndexAnnotationDelivery() throws Exception {
		createSearchShellJar();
//		try {
			TSDB tsdb = newTSDB("SpringSearchConfig");
			GenericApplicationContext appCtx = SpringContainerService.getInstance(null, null).getAppContext();
			QueuedResultSearchEventHandler handler = appCtx.getBean("SearchEventHandler", QueuedResultSearchEventHandler.class);
			Assert.assertNotNull("The spring configured handler was null", handler);
			BlockingQueue<Object> events = handler.getResultQueue();
			Assert.assertNotNull("The spring configured event queue was null", events);
			int eventCount = 1000;
			int receivedEventCount = 0;
			Map<String, Annotation> annotations = startAnnotationStream(tsdb, eventCount, 2, 0);		
			for(int i = 0; i < eventCount; i++) {
				TSDBSearchEvent event = (TSDBSearchEvent)events.take();			
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
//		} catch (Exception ex) {
//			ex.printStackTrace(System.err);
//			throw ex;
//		}
	}

}
