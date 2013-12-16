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

import org.helios.tsdb.plugins.rpc.session.DefaultRPCSession;
import org.helios.tsdb.plugins.rpc.session.IRPCSession;
import org.helios.tsdb.plugins.rpc.session.ITransportSessionFactory;
import org.helios.tsdb.plugins.rpc.session.RPCSessionAttribute;
import org.jboss.netty.channel.Channel;

/**
 * <p>Title: NetyChannelSessionFactory</p>
 * <p>Description: An {@link ITransportSessionFactory} for Netty Channels</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.rpc.netty.NetyChannelSessionFactory</code></p>
 */

public class NetyChannelSessionFactory implements ITransportSessionFactory {
	/** The netty transport type */
	private static final Class<?> NETTY_CHANNEL = Channel.class;
	/**
	 * Creates a new NetyChannelSessionFactory
	 */
	public NetyChannelSessionFactory() {
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.rpc.session.ITransportSessionFactory#getTransportType()
	 */
	@Override
	public Class<?> getTransportType() {
		return NETTY_CHANNEL;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.rpc.session.ITransportSessionFactory#newRPCSession(java.lang.Object)
	 */
	@Override
	public IRPCSession newRPCSession(Object transport) {
		if(transport==null) throw new IllegalArgumentException("The passed transport was null");
		if(!NETTY_CHANNEL.isInstance(transport)) throw new IllegalArgumentException("The passed transport type [" + transport.getClass().getName() + "] is not a Netty Channel");
		Channel channel = (Channel)transport;
		IRPCSession session = new DefaultRPCSession(new NettyChannelSession((Channel)transport));
		session.addSessionAttribute(RPCSessionAttribute.Transport, "Netty");
		session.addSessionAttribute(RPCSessionAttribute.RemoteAddress, "Netty");
		return session;
	}

}
