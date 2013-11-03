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
package test.net.opentsdb.search.util;

import java.net.URL;

import org.helios.tsdb.plugins.util.URLHelper;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.RunListener;

import test.net.opentsdb.search.ESBaseTest;

/**
 * <p>Title: ESTestRunListener</p>
 * <p>Description: JUnit run listener to start an embedded ES instance if a server cannot be connected to.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>test.net.opentsdb.search.util.ESTestRunListener</code></p>
 */

public class ESTestRunListener extends RunListener { 
	private static boolean startedEmbedded = false;
	/**
	 * Creates a new ESTestRunListener
	 */
	public ESTestRunListener() {
		log("\n\t==========================================\n\tESTestRunListener\n\t==========================================\n");
	}
	
	@Override
	public void testRunFinished(Result result) throws Exception {
		super.testRunFinished(result);
		log("\n\t==========================================\n\tRun Finished.\n\t%s\n\t==========================================\n", result);
		if(startedEmbedded) {
			log("Stopping Embedded....");
			ESBaseTest.stopEs();
		}
	}
	
	@Override
	public void testRunStarted(Description description) throws Exception {
		super.testRunStarted(description);
		log("\n\t==========================================\n\tRun Started.\n\t%s\n\t==========================================\n", description);
		if(!isEsAvailable("http://localhost:9200/_status")) {
			log("No local ES available. Starting Embedded....");
			ESBaseTest.startEs();
			startedEmbedded = true;
		} else {
			
		}		
	}
	
	/**
	 * Determines if a local ES is available for testing
	 * @param esUrl The URL to test for an ES instance
	 * @return true if a local ES is available for testing, false otherwise
	 */
	public static boolean isEsAvailable(CharSequence esUrl) {
		URL url = URLHelper.toURL(esUrl);
		try {
			String status = URLHelper.getTextFromURL(url, 5000);
			return status.contains("{\"ok\":true");
		} catch (Exception ex) {
			return false;
		}		
	}
	
	
	/**
	 * Simple formatting logger
	 * @param format The format
	 * @param args The arguments
	 */
	public static void log(String format, Object...args) {
		System.out.println(String.format(format, args));
	}

}
