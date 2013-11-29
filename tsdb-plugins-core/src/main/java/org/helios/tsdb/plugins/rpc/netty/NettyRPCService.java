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
package org.helios.tsdb.plugins.rpc.netty;

import java.util.Properties;
import java.util.concurrent.ThreadPoolExecutor;

import net.opentsdb.core.TSDB;

import org.helios.tsdb.plugins.async.AsyncDispatcherExecutor;
import org.helios.tsdb.plugins.rpc.AbstractRPCService;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;

/**
 * <p>Title: NettyRPCService</p>
 * <p>Description: RPC service for extended Netty services such as WebSockets.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.rpc.netty.NettyRPCService</code></p>
 */

public class NettyRPCService extends AbstractRPCService {
	/** The netty boss pool */
	protected ThreadPoolExecutor bossPool = null;
	/** The netty worker pool */
	protected ThreadPoolExecutor workerPool = null;
	/** The tcp Server bootstrap */
	protected ServerBootstrap serverBootstrap = null;
	/** The nio channel factory */
	protected NioServerSocketChannelFactory nioServerChannelFactory = null;
	
	
	/**
	 * Creates a new NettyRPCService
	 * @param tsdb The parent TSDB
	 * @param config The extracted config
	 */
	public NettyRPCService(TSDB tsdb, Properties config) {
		super(tsdb, config);
	}
	
	
	/**
	 * <p>Starts the NettyRPCService and related sub-services</p>
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.rpc.AbstractRPCService#startImpl()
	 */
	@Override
	protected void startImpl() {
		bossPool = new AsyncDispatcherExecutor(getClass().getSimpleName() + "BossPool", config);
		workerPool = new AsyncDispatcherExecutor(getClass().getSimpleName() + "WorkerPool", config);
		
	}
	
	/**
	 * <p>Stops the NettyRPCService and related sub-services</p>
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.rpc.AbstractRPCService#stopImpl()
	 */
	@Override
	protected void stopImpl() {
		workerPool.shutdownNow();
		bossPool.shutdownNow();
	}

}
