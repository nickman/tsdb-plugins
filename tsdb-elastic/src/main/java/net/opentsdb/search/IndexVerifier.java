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
package net.opentsdb.search;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;

/**
 * <p>Title: IndexVerifier</p>
 * <p>Description: HTTP client to validate or install the ES indexes and mapping for OpenTSDB indexed artifacts</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>net.opentsdb.search.IndexVerifier</code></p>
 */

public class IndexVerifier {
	/**  */
	//public static final Logger LOG = LoggerFactory.getLogger(IndexVerifier.class);
	/**
	 * Creates a new IndexVerifier
	 */
	public IndexVerifier() {
		
	}
	
	public static void log(String format, Object...args) {
		System.out.println(String.format(format, args));
	}

	public static void main(String[] args) {
		try {
			log("IndexVerifier Test");
			Request request = Request.Head("http://localhost");
			Response r = request.execute();
			
			HttpResponse response = r.returnResponse();
			
			log("Status:[%s]", response.getStatusLine().getStatusCode());
			for(Header h: response.getAllHeaders()) {
				log("\tHeader:[%s]--[%s]", h.getName(), h.getValue());
			}
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		}
	}
	
}
