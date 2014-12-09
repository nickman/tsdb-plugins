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
package net.opentsdb.catalog;

import java.io.IOException;

import net.opentsdb.core.TSDB;
import net.opentsdb.tsd.BadRequestException;
import net.opentsdb.tsd.HttpQuery;
import net.opentsdb.tsd.HttpRpc;
import net.opentsdb.tsd.RPC;

/**
 * <p>Title: MetricUIHandler</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>net.opentsdb.catalog.MetricUIHandler</code></p>
 */

@RPC(httpKeys={"$s{catalog.metricuihandler.uri}:metricapi-ui"})
public class MetricUIHandler implements HttpRpc {
	final String contentDir;
	final String staticDir;
	
	  /**
	   * Constructor.
	   */
	  public MetricUIHandler(final TSDB tsdb) {
		  staticDir = tsdb.getConfig().getDirectoryName("tsd.http.staticroot");
		  contentDir = System.getProperty("metricui.staticroot", staticDir);
		  //RpcHandler.getInstance().registerHandler("metricapi-ui", this);
	  }
	  
	  

	  public void execute(final TSDB tsdb, final HttpQuery query)
	    throws IOException {
		final String baseURI = query.request().getUri();
		if("/metricapi-ui".equals(baseURI)) {
			query.sendFile(contentDir + "/index.html", 0);
			return;
		}
	    final String uri = baseURI.replace("metricapi-ui/", "");
	    if ("/favicon.ico".equals(uri)) {
	      query.sendFile(staticDir 
	          + "/favicon.ico", 31536000 /*=1yr*/);
	      return;
	    }
	    if (uri.length() < 3) {  // Must be at least 3 because of the "/s/".
	      throw new BadRequestException("URI too short <code>" + uri + "</code>");
	    }
	    // Cheap security check to avoid directory traversal attacks.
	    // TODO(tsuna): This is certainly not sufficient.
	    if (uri.indexOf("..", 3) > 0) {
	      throw new BadRequestException("Malformed URI <code>" + uri + "</code>");
	    }
	    final int questionmark = uri.indexOf('?', 3);
	    final int pathend = questionmark > 0 ? questionmark : uri.length();
	    query.sendFile(contentDir + "/" 
	                 + uri.substring(1, pathend),  // Drop the "/s"
	                   uri.contains("nocache") ? 0 : 31536000 /*=1yr*/);
	  }		
}

