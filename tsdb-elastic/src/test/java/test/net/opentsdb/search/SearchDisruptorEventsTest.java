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

import net.opentsdb.search.ElasticSearchEventHandler;

import org.junit.AfterClass;
import org.junit.BeforeClass;


/**
 * <p>Title: SearchDisruptorEventsTest</p>
 * <p>Description: Test cases for round-trip search events through ES using the disruptor async dispatcher</p> 
 * <p>For each of Annotation, TSMeta and UIDMeta:<ol>Class
 * 	<li>Async Publishing, validate via Percolate</li>
 * 	<li>Sync Publishing, validate via Percolate</li>
 * 	<li>Async Publishing, validate via Requery</li>
 * 	<li>TSDB SearchQuery</li>
 * 	<li>Vary index config ? i.e. seperate index per type</li>
 * </ol></p>
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>test.net.opentsdb.search.SearchEventsTest</code></p>
 */

public class SearchDisruptorEventsTest extends SearchEventsTest {
	/** The search config profile */
	public static final String SEARCH_CONFIG = "ESSearchDisruptorConfig";

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
		uidMetaType = ElasticSearchEventHandler.getInstance().getTsmeta_type();
		uidMetaIndex = ElasticSearchEventHandler.getInstance().getTsmeta_index();
		uidMetaUIndex = ioClient.getIndexForAlias(uidMetaIndex);
		tsMetaType = ElasticSearchEventHandler.getInstance().getUidmeta_type();
		tsMetaIndex = ElasticSearchEventHandler.getInstance().getUidmeta_index();	
		tsMetaUIndex = ioClient.getIndexForAlias(tsMetaIndex);
		log("\n\t=======================================\n\tSearchEventsTest Class Initalized\n\t=======================================");
	}
	

}
