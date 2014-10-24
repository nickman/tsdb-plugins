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
package net.opentsdb.client.net;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.opentsdb.client.net.ws.WebSocketClientHandler;

import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelState;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.UpstreamChannelStateEvent;
import org.jboss.netty.handler.codec.http.HttpRequestEncoder;
import org.jboss.netty.handler.codec.http.HttpResponseDecoder;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketVersion;
import org.jboss.netty.handler.logging.LoggingHandler;
import org.jboss.netty.logging.InternalLogLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Title: Protocol</p>
 * <p>Description: Functional enumeration for supported protocols in created clients</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>net.opentsdb.client.net.Protocol</code></p>
 */

public enum Protocol {
	WS(new WebSocketPipelineBuilder());
	
	private Protocol(final PipelineBuilder builder) {
		this.builder = builder;
	}
	
	private final PipelineBuilder builder;
	private static final LoggingHandler lh = new LoggingHandler(Protocol.class, InternalLogLevel.WARN, true);
	private static final Logger LOG = LoggerFactory.getLogger(Protocol.class);
	public ChannelPipeline getPipeline(final String host, final String path, final int port, final Map<String, String> parameters) {
		return builder.getPipeline(host, path, port, parameters);
	}
	
	/**
	 * Returns the protocol for the passed name. Passed value is trimmed and upercased.
	 * @param cs The name of the protocol
	 * @return the protocol member
	 */
	public static Protocol from(final CharSequence cs) {
		if(cs==null || cs.toString().trim().isEmpty()) throw new IllegalArgumentException("The passed protocol name was null or empty");
		final String name = cs.toString().trim().toUpperCase();
		return Protocol.valueOf(name);
	}
	
	public static interface PipelineBuilder {
		public ChannelPipeline getPipeline(final String host, final String path, final int port, final Map<String, String> parameters);
	}
	
	private static class WebSocketPipelineBuilder implements PipelineBuilder {
		/** The handshaker factory */
		protected final WebSocketClientHandshakerFactory handshakerFactory = new WebSocketClientHandshakerFactory();

		@Override
		public ChannelPipeline getPipeline(final String host, final String path, final int port, final Map<String, String> parameters) {
			final ChannelPipeline pipeline = Channels.pipeline();
			pipeline.addLast("logging", lh);
			Codecs.JSON.addCodecs(pipeline);
			pipeline.addLast("decoder", new HttpResponseDecoder());
			pipeline.addLast("encoder", new HttpRequestEncoder());
			final WebSocketClientHandshaker handshaker;
			try {
				handshaker = getHandshaker(new URI("ws://" + host + ":" + port + path));
				pipeline.addLast("ws-handler", new WebSocketClientHandler(handshaker));
			} catch (URISyntaxException e) {
				throw new RuntimeException(e);
			}
//			
//			pipeline.addLast("onconnect", new ChannelUpstreamHandler() {
//				@Override
//				public void handleUpstream(final ChannelHandlerContext ctx, final ChannelEvent e) throws Exception {
//					
//					if(e instanceof UpstreamChannelStateEvent) {
//						final UpstreamChannelStateEvent upEvent = (UpstreamChannelStateEvent)e;						
//						if(upEvent.getState()==ChannelState.CONNECTED && upEvent.getValue()!=null) {
//							LOG.info("Up Event ----> \n\t Value: {}\n\t State: {}", upEvent.getValue(), upEvent.getState().name());
//							final ChannelUpstreamHandler self = this;
//							handshaker.handshake(ctx.getChannel()).addListener(new ChannelFutureListener() {
//								@Override
//								public void operationComplete(ChannelFuture future) throws Exception {
//									if(future.isSuccess()) {
//										ctx.getPipeline().remove(self);
//										LOG.info("Handshake completed successfully");
//									}
//								}
//							});
//							
////							DefaultHttpRequest req = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "ws://" + host + ":" + port + "/" + path);
////							ctx.sendDownstream(new DownstreamMessageEvent(ctx.getChannel(), Channels.future(ctx.getChannel()), req,  ctx.getChannel().getRemoteAddress()));
//						}
//					}
//					ctx.sendUpstream(e);					
//				}
//			});
			
			 
			
			
			return pipeline;		
		}
		
		protected WebSocketClientHandshaker getHandshaker(final URI uri) {
			return handshakerFactory.newHandshaker(uri, WebSocketVersion.V13, null, false, new HashMap<String, String>(0));
		}
		
		
	}
}
