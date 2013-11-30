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
package org.helios.tsdb.plugins.rpc.netty.pipeline.http;

import org.helios.tsdb.plugins.rpc.netty.pipeline.RemotingPipelineFactory;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelUpstreamHandler;

/**
 * <p>Title: HttpRequestRouter</p>
 * <p>Description: Service to route incoming {@link HttpRequest}s to a {@link HttpRequestHandler} by the request URI</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.rpc.netty.pipeline.http.HttpRequestRouter</code></p>
 */

public class HttpRequestRouter implements ChannelUpstreamHandler {
	/** The singleton instance */
	protected static volatile HttpRequestRouter instance = null;
	/** The singleton instance ctor lock */
	protected static final Object lock = new Object();

	/**
	 * Acquires and returns the singleton instance
	 * @return the singleton instance
	 */
	public static HttpRequestRouter getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new HttpRequestRouter(); 
				}
			}
		}
		return instance;
	}	
	
	/**
	 * Creates a new HttpRequestRouter
	 */
	protected HttpRequestRouter() {
	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.ChannelUpstreamHandler#handleUpstream(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ChannelEvent)
	 */
	@Override
	public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
		// TODO Auto-generated method stub

	}

}
