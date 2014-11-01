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

import java.io.InputStream;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferInputStream;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandler.Sharable;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelLocal;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.UpstreamMessageEvent;
import org.jboss.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.jboss.netty.handler.codec.oneone.OneToOneDecoder;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;
import org.jboss.netty.buffer.ChannelBufferInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * <p>Title: Codecs</p>
 * <p>Description: Enumerates the object encoding options for objects returned from the server</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>net.opentsdb.client.net.Codecs</code></p>
 */

public enum Codecs {
	
	/** Objects are encoded in JSON */
	JSON(new JSONCodecAdder());
	
	
	private static final Logger LOG = LoggerFactory.getLogger(Codecs.class);
	
	private Codecs(final CodecAdder codec) {
		this.codec = codec;
	}
	
	public void addCodecs(final ChannelPipeline pipeline) {
		this.codec.addCodecs(pipeline);
	}
	
	private final CodecAdder codec;
	
	public static interface CodecAdder {
		public void addCodecs(ChannelPipeline pipeline);
	}
	
	/**
	 * <p>Title: JSONCodecAdder</p>
	 * <p>Description: A codec for encoding/decoding JSON</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>net.opentsdb.client.net.Codecs.JSONCodecAdder</code></p>
	 */
	private static class JSONCodecAdder implements CodecAdder {
		/** The shared object mapper */
		private static final ObjectMapper om = new ObjectMapper();
		
		/** A reusable JSONEncoder instance */
		private static final JSONEncoder ENCODER_INSTANCE = new JSONEncoder();
		/** A reusable JSONDecoder instance */
		private static final JSONDecoder DECODER_INSTANCE = new JSONDecoder();
		
		/**
		 * <p>Title: JSONEncoder</p>
		 * <p>Description: The JSON encoder</p> 
		 * <p>Company: Helios Development Group LLC</p>
		 * @author Whitehead (nwhitehead AT heliosdev DOT org)
		 * <p><code>net.opentsdb.client.net.Codecs.JSONCodecAdder.JSONEncoder</code></p>
		 */
		@Sharable
		public static class JSONEncoder extends OneToOneEncoder {
			@Override
			protected Object encode(final ChannelHandlerContext ctx, final Channel channel, final Object msg) throws Exception {
				return new TextWebSocketFrame(ChannelBuffers.wrappedBuffer(om.writeValueAsBytes(msg)));
			}
		}

		/**
		 * <p>Title: JSONDecoder</p>
		 * <p>Description: The JSON decoder</p> 
		 * <p>Company: Helios Development Group LLC</p>
		 * @author Whitehead (nwhitehead AT heliosdev DOT org)
		 * <p><code>net.opentsdb.client.net.Codecs.JSONCodecAdder.JSONDecoder</code></p>
		 */
		@Sharable
		public static class JSONDecoder extends OneToOneDecoder {
			/** Accumulates channel buffers on incomplete json streams */
			static final ChannelLocal<ChannelBuffer> accumulatingChannelBuffer = new ChannelLocal<ChannelBuffer>();
			@Override
			protected Object decode(final ChannelHandlerContext ctx, final Channel channel, final Object msg) throws Exception {
				if(msg instanceof WebSocketFrame) {
					WebSocketFrame wsf = (WebSocketFrame)msg;
					InputStream is = null;
					try {
						is = new ChannelBufferInputStream(wsf.getBinaryData());
						return om.readTree(is);
					} finally {
						try { is.close(); } catch (Exception e) {}
					}
					
				} else if(msg instanceof ChannelBuffer) {
					final ChannelBuffer buff = accumulatingChannelBuffer.get(channel);
					if(buff!=null) {
						buff.writeBytes((ChannelBuffer)msg);
					}					
					final InputStream is = new ChannelBufferInputStream(buff);
					try {
						final JsonNode node = om.readTree(is);
						accumulatingChannelBuffer.remove(channel);
						return node;
					} catch (Exception x) {
						buff.resetReaderIndex();
						ChannelBuffer xbuff = accumulatingChannelBuffer.get(channel);
						if(xbuff==null) {
							xbuff = ChannelBuffers.dynamicBuffer();
							accumulatingChannelBuffer.set(channel, xbuff);
						}
						xbuff.writeBytes(buff);
						return null;
					} finally {
						try { is.close(); } catch (Exception x) {/* No Op */}
					}					
				} else {
					ctx.sendUpstream(new UpstreamMessageEvent(channel, msg, channel.getLocalAddress()));
					return null;
				}
			}
		}
		
		@Override
		public void addCodecs(final ChannelPipeline pipeline) {
			pipeline.addFirst("json-encoder", ENCODER_INSTANCE);
			pipeline.addFirst("json-decoder", DECODER_INSTANCE);
		}
	}
}
