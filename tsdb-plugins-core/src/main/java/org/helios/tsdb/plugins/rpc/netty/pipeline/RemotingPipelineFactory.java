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

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;

/**
 * <p>Title: RemotingPipelineFactory</p>
 * <p>Description: Creates the protocol unified switching pipeline factory for the netty based TSDB RPC plugin services.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.rpc.netty.pipeline.RemotingPipelineFactory</code></p>
 */

public class RemotingPipelineFactory implements ChannelPipelineFactory {
	/** The singleton instance */
	protected static volatile RemotingPipelineFactory instance = null;
	/** The singleton instance ctor lock */
	protected static final Object lock = new Object();
	
	/** The protocol detection handler */
	protected final ProtocolSwitch protocolSwitch = new ProtocolSwitch();
	
	/**
	 * Acquires and returns the singleton instance
	 * @return the singleton instance
	 */
	public static RemotingPipelineFactory getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new RemotingPipelineFactory(); 
				}
			}
		}
		return instance;
	}
	/**
	 * Creates a new RemotingPipelineFactory
	 */
	protected RemotingPipelineFactory() {

	}
	
	

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.ChannelPipelineFactory#getPipeline()
	 */
	@Override
	public ChannelPipeline getPipeline() throws Exception {
		ChannelPipeline pipeline = Channels.pipeline();
		pipeline.addLast("protocolSwitch", protocolSwitch);
		return pipeline;
	}

}
