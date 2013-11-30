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

import org.helios.tsdb.plugins.rpc.netty.pipeline.ProtocolInitiator;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.handler.codec.http.HttpChunkAggregator;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;
import org.jboss.netty.handler.logging.LoggingHandler;
import org.jboss.netty.handler.stream.ChunkedWriteHandler;
import org.jboss.netty.logging.InternalLogLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Title: HttpProtocolInitiator</p>
 * <p>Description: Protocol iniator for an HTTP connection</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.rpc.netty.pipeline.http.HttpProtocolInitiator</code></p>
 */

public class HttpProtocolInitiator implements ProtocolInitiator {
	/** Instance logger */
	protected final Logger log = LoggerFactory.getLogger(getClass());
	/** The pipeline logging handler */
	protected final LoggingHandler loggingHandler = new LoggingHandler(getClass(), InternalLogLevel.ERROR, true);

	/** The http request router */
	protected HttpRequestRouter router = HttpRequestRouter.getInstance();

	/**
	 * Creates a new HttpProtocolInitiator
	 */
	public HttpProtocolInitiator() {

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.rpc.netty.pipeline.ProtocolInitiator#requiredBytes()
	 */
	@Override
	public int requiredBytes() {
		return 2;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.rpc.netty.pipeline.ProtocolInitiator#match(org.jboss.netty.buffer.ChannelBuffer)
	 */
	@Override
	public boolean match(ChannelBuffer buff) {
		return isHttp(buff.getUnsignedByte(0), buff.getUnsignedByte(1));
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.rpc.netty.pipeline.ProtocolInitiator#modifyPipeline(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.Channel, org.jboss.netty.buffer.ChannelBuffer)
	 */
	@Override
	public void modifyPipeline(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer) {
		ChannelPipeline pipeline = ctx.getPipeline();
		pipeline.addLast("decoder", new HttpRequestDecoder());
		pipeline.addLast("aggregator", new HttpChunkAggregator(65536));
		pipeline.addLast("encoder", new HttpResponseEncoder());       
		pipeline.addLast("chunkedWriter", new ChunkedWriteHandler());
		pipeline.addLast("router", router);
		if(this.log.isDebugEnabled()) {
			pipeline.addFirst("logger", loggingHandler);
		}
		
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.rpc.netty.pipeline.ProtocolInitiator#getProtocol()
	 */
	@Override
	public String getProtocol() {
		return "http";
	}
	
	/**
	 * Determines if the channel is carrying an HTTP request
	 * @param magic1 The first byte of the incoming request
	 * @param magic2 The second byte of the incoming request
	 * @return true if the incoming is HTTP, false otherwise
	 */
	public boolean isHttp(int magic1, int magic2) {
		 return
		 magic1 == 'G' && magic2 == 'E' || // GET
		 magic1 == 'P' && magic2 == 'O' || // POST
		 magic1 == 'P' && magic2 == 'U' || // PUT
		 magic1 == 'H' && magic2 == 'E' || // HEAD
		 magic1 == 'O' && magic2 == 'P' || // OPTIONS
		 magic1 == 'P' && magic2 == 'A' || // PATCH
		 magic1 == 'D' && magic2 == 'E' || // DELETE
		 magic1 == 'T' && magic2 == 'R' || // TRACE
		 magic1 == 'C' && magic2 == 'O';   // CONNECT
	}	

}
