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
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import net.opentsdb.core.TSDB;
import net.opentsdb.meta.Annotation;

import org.helios.tsdb.plugins.event.TSDBEventType;
import org.helios.tsdb.plugins.event.TSDBSearchEvent;
import org.helios.tsdb.plugins.handlers.impl.QueuedResultSearchEventHandler;
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
		createSearchShellJar();
		TSDB tsdb = newTSDB("ESSearchConfig");
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
		tsdb.indexAnnotation(a);
		
		
		
		
	}

}
