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
import java.util.Map;

import net.opentsdb.client.net.Protocol.PipelineBuilder;
import net.opentsdb.client.net.ws.WebSocketClientHandler;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.handler.codec.http.HttpRequestEncoder;
import org.jboss.netty.handler.codec.http.HttpResponseDecoder;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketVersion;

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
			ChannelPipeline pipeline = Channels.pipeline();		
			pipeline.addLast("decoder", new HttpResponseDecoder());
			pipeline.addLast("encoder", new HttpRequestEncoder());
			try {
				pipeline.addLast("ws-handler", new WebSocketClientHandler(getHandshaker(new URI("ws://" + host + ":" + port + "/" + path))));
			} catch (URISyntaxException e) {
				throw new RuntimeException(e);
			}
			return pipeline;		
		}
		
		protected WebSocketClientHandshaker getHandshaker(final URI uri) {
			return handshakerFactory.newHandshaker(uri, WebSocketVersion.V13, null, false, null);
		}
		
		
	}
}
