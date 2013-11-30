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
package org.helios.tsdb.plugins.rpc.netty.pipeline;

import java.nio.ByteOrder;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferFactory;
import org.jboss.netty.buffer.DirectChannelBufferFactory;
import org.jboss.netty.buffer.DynamicChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelLocal;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.UpstreamMessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Title: ProtocolSwitch</p>
 * <p>Description: An upfront channel handler to determine the protocol handlers the pipeline should install for the incomng request</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.rpc.netty.pipeline.ProtocolSwitch</code></p>
 */

public class ProtocolSwitch implements ChannelUpstreamHandler {
	/** The singleton instance */
	protected static volatile ProtocolSwitch instance = null;
	/** The singleton instance ctor lock */
	protected static final Object lock = new Object();
	
	/** Instance logger */
	protected final Logger log = LoggerFactory.getLogger(getClass());

	/** A map of protocol initiators keyed by the protocol name they advertise */
	protected final Map<String, ProtocolInitiator> initiators = new ConcurrentHashMap<String, ProtocolInitiator>();
	
	/** A channel local to accumulate unclassified buffers */
	protected final ChannelLocal<DynamicChannelBuffer> preSwitchedBuffer = new ChannelLocal<DynamicChannelBuffer>(true);
	/** The channel buffer factory */
	protected final ChannelBufferFactory chanelBufferFactory = new DirectChannelBufferFactory(ByteOrder.nativeOrder(), 1024);

	
	/**
	 * Acquires and returns the singleton instance
	 * @return the singleton instance
	 */
	public static ProtocolSwitch getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new ProtocolSwitch(); 
				}
			}
		}
		return instance;
	}
	
	/**
	 * Registers a new protocol initiator
	 * @param initiator the protocol initiator to register
	 */
	public void registerProtocolInitiator(ProtocolInitiator initiator) {
		if(initiator==null) throw new IllegalArgumentException("The passed initiator was null");
		String key = initiator.getProtocol();
		if(key==null || key.trim().isEmpty()) throw new RuntimeException("The passed initiator had a null or empty protocol [" + initiator + "]");
		key = key.trim().toLowerCase();
		if(!initiators.containsKey(key)) {
			synchronized(initiators) {
				if(!initiators.containsKey(key)) {
					initiators.put(key, initiator);
					return;
				}
			}
		}
		throw new RuntimeException("An initiator for protocol [" + key + "] has already been registered");		
	}
	
	/**
	 * Creates a new ProtocolSwitch
	 */
	protected ProtocolSwitch() {
	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.ChannelUpstreamHandler#handleUpstream(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ChannelEvent)
	 */
	@Override
	public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
		if(e instanceof MessageEvent) {
			MessageEvent me = (MessageEvent) e;
			if(me.getMessage() instanceof ChannelBuffer) {
				ChannelBuffer postDetectBuffer = protocolSwitch(ctx, e.getChannel(), (ChannelBuffer)me.getMessage(), e);
				if(postDetectBuffer!=null) {	
					ctx.getPipeline().remove(this);
					ctx.sendUpstream(new UpstreamMessageEvent(e.getChannel(), postDetectBuffer, ((MessageEvent) e).getRemoteAddress()));					
				}
			}
		} else {
			ctx.sendUpstream(e);
		}
	}
	
	/**
	 * Examines the channel buffer and attempts to match the protocol of the request and invoke the matching {@link ProtocolInitiator}.
	 * @param ctx The channel handler context
	 * @param channel The channel
	 * @param bufferx The message buffer
	 * @param e The channel event
	 * @return The channel buffer to send upstream, or null if we need more bytes
	 */
	protected ChannelBuffer protocolSwitch(ChannelHandlerContext ctx, Channel channel, ChannelBuffer bufferx, ChannelEvent e)  {		
		ChannelBuffer cb = preSwitchedBuffer.get(channel);
		if(cb!=null) {
			cb.writeBytes(bufferx);
			cb.resetReaderIndex();
		} else {
			cb = bufferx;
		}
		// this guy will be set with a matching initiator
		ProtocolInitiator selectedInitiator = null;
		// this guy will be set to false if at least 1 initiator had insufficient bytes to match
		boolean sufficientBytes = true;
		// ths guy has the total bytes available in the buffer
		final int bytesAvailable = cb.readableBytes();
		
		for(ProtocolInitiator pi : initiators.values()) {
			if(pi.requiredBytes() < bytesAvailable) {
				sufficientBytes = false;
			} else {
				if(pi.match(cb)) {
					selectedInitiator = pi;
					break;
				}
			}			
		}
		
		if(selectedInitiator==null) {
			// we did not get a match
			if(!sufficientBytes) {
				// ok, we did not have enough bytes
				DynamicChannelBuffer dcb = preSwitchedBuffer.get(channel);
				if(dcb == null) {
					dcb = new DynamicChannelBuffer(cb.order(), 1024, chanelBufferFactory);
					preSwitchedBuffer.set(channel, dcb);
				}
				dcb.writeBytes(cb);
				dcb.resetReaderIndex();
				return null;
			}
			// darn, we have enough bytes for any of the inits,
			// but none matched
			throw new RuntimeException("Failed to match any protocol initiator");
		}
		preSwitchedBuffer.remove(channel);
		// we matched on an initiator, so have it modify the pipeline
		selectedInitiator.modifyPipeline(ctx, channel, cb);
		return cb;
		
		// if we get here, it means we did not find a protocol match
		// so pass to the default protocol initiator.
	}
	

}
