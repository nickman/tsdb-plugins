/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2013, Helios Development Group and individual contributors
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
package org.helios.tsdb.plugins.remoting.json.services;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelConfig;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.Channels;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Title: InvocationChannel</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.remoting.json.services.InvocationChannel</code></p>
 */

public class InvocationChannel implements Channel {
	/** Instance logger */
	protected final Logger log = LoggerFactory.getLogger(getClass());
	
	protected final List<Object> channelWrites = new ArrayList<Object>();
	
	public List<Object> getWrites() {
		return channelWrites;
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.Channel#write(java.lang.Object)
	 */
	@Override
	public ChannelFuture write(Object message) {
		if(message!=null) {
			channelWrites.add(message);
			log.info("Received Channel Write [{}]  type:[{}]", message, message.getClass().getName());
		}
		
		return Channels.succeededFuture(this);
	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.Channel#write(java.lang.Object, java.net.SocketAddress)
	 */
	@Override
	public ChannelFuture write(Object message, SocketAddress remoteAddress) {
		return write(message);
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.Channel#isOpen()
	 */
	@Override
	public boolean isOpen() {
		return true;
	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.Channel#isBound()
	 */
	@Override
	public boolean isBound() {
		return true;
	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.Channel#isConnected()
	 */
	@Override
	public boolean isConnected() {
		return true;
	}

	
	
	// ===================================================================================================================
	//  Totally Fake Methods
	// ===================================================================================================================

	/**
	 * {@inheritDoc}
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(Channel o) {
		// TODO Auto-generated method stub
		return 0;
	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.Channel#getId()
	 */
	@Override
	public Integer getId() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.Channel#getFactory()
	 */
	@Override
	public ChannelFactory getFactory() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.Channel#getParent()
	 */
	@Override
	public Channel getParent() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.Channel#getConfig()
	 */
	@Override
	public ChannelConfig getConfig() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.Channel#getPipeline()
	 */
	@Override
	public ChannelPipeline getPipeline() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.Channel#getLocalAddress()
	 */
	@Override
	public SocketAddress getLocalAddress() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.Channel#getRemoteAddress()
	 */
	@Override
	public SocketAddress getRemoteAddress() {
		// TODO Auto-generated method stub
		return null;
	}


	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.Channel#bind(java.net.SocketAddress)
	 */
	@Override
	public ChannelFuture bind(SocketAddress localAddress) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.Channel#connect(java.net.SocketAddress)
	 */
	@Override
	public ChannelFuture connect(SocketAddress remoteAddress) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.Channel#disconnect()
	 */
	@Override
	public ChannelFuture disconnect() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.Channel#unbind()
	 */
	@Override
	public ChannelFuture unbind() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.Channel#close()
	 */
	@Override
	public ChannelFuture close() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.Channel#getCloseFuture()
	 */
	@Override
	public ChannelFuture getCloseFuture() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.Channel#getInterestOps()
	 */
	@Override
	public int getInterestOps() {
		// TODO Auto-generated method stub
		return 0;
	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.Channel#isReadable()
	 */
	@Override
	public boolean isReadable() {
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.Channel#isWritable()
	 */
	@Override
	public boolean isWritable() {
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.Channel#setInterestOps(int)
	 */
	@Override
	public ChannelFuture setInterestOps(int interestOps) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.Channel#setReadable(boolean)
	 */
	@Override
	public ChannelFuture setReadable(boolean readable) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.Channel#getAttachment()
	 */
	@Override
	public Object getAttachment() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.Channel#setAttachment(java.lang.Object)
	 */
	@Override
	public void setAttachment(Object attachment) {
		// TODO Auto-generated method stub

	}

}
