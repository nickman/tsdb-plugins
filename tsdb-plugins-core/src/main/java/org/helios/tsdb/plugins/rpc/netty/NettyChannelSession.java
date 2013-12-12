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
package org.helios.tsdb.plugins.rpc.netty;

import java.util.concurrent.atomic.AtomicBoolean;

import org.helios.tsdb.plugins.rpc.session.IRPCSession;
import org.helios.tsdb.plugins.rpc.session.ISessionLifecycle;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Title: NettyChannelSession</p>
 * <p>Description: RPC Session lifecycle for Netty channels</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.rpc.netty.NettyChannelSession</code></p>
 */

public class NettyChannelSession implements ISessionLifecycle, ChannelFutureListener {
	/** The netty channel to associate to the session */
	protected final Channel channel;
	/** The session to associate with the Netty channel */
	protected final IRPCSession session;
	/** The session id */
	protected final String sessionId;
	/** The expired indicator flag */
	protected final AtomicBoolean expired = new AtomicBoolean(false);
	/** Static class logger */
	protected static final Logger log = LoggerFactory.getLogger(NettyChannelSession.class);
	
	/**
	 * Creates a new NettyChannelSession
	 * @param channel The netty channel to associate to the session
	 * @param session The session to associate with the Netty channel 
	 */
	public NettyChannelSession(Channel channel, IRPCSession session) {
		if(channel==null) throw new IllegalArgumentException("The passed channel was null");
		if(!channel.isOpen()) throw new IllegalStateException("The channel is not open");
		if(session==null) throw new IllegalArgumentException("The passed session was null");
		this.channel = channel;
		this.session = session;
		sessionId = Integer.toString(channel.getId());
		log.info("Created Session [{}] with remote [{}]", sessionId, channel.getRemoteAddress());
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.rpc.session.ISessionLifecycle#getSessionId()
	 */
	@Override
	public String getSessionId() {
		return sessionId;
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.rpc.session.ISessionLifecycle#isExpired()
	 */
	@Override
	public boolean isExpired() {
		return expired.get();
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.rpc.session.ISessionLifecycle#onSessionExpire()
	 */
	@Override
	public void onSessionExpire() {
		if(expired.compareAndSet(false, true)) {
			log.info("Closing Channel [{}]", sessionId);
			channel.close();
		}
	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.ChannelFutureListener#operationComplete(org.jboss.netty.channel.ChannelFuture)
	 */
	@Override
	public void operationComplete(ChannelFuture future) throws Exception {
		Channel ch = future.getChannel();
		if(ch!=null && !ch.isOpen()) {
			if(expired.compareAndSet(false, true)) {
				log.info("Expiring Session [{}]", sessionId);
				session.expire();
			}
		}
	}

}
