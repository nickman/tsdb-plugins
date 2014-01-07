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
package org.helios.tsdb.plugins.rpc;

import java.util.Properties;
import java.util.concurrent.Executor;

import net.opentsdb.core.TSDB;
import net.opentsdb.stats.StatsCollector;

import org.helios.tsdb.plugins.async.AsyncDispatcherExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.AbstractService;
import com.google.common.util.concurrent.ListenableFuture;

/**
 * <p>Title: AbstractRPCService</p>
 * <p>Description: Abstract base class for RPC service implementations.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.rpc.AbstractRPCService</code></p>
 */

public abstract class AbstractRPCService  implements IRPCService {
	/** The RPC service shared executor */
	protected static volatile AsyncDispatcherExecutor rpcExecutor = null;
	/** The initialization lock for the rpc executor */
	protected static final Object lock = new Object();
	/** Instance logger */
	protected final Logger log = LoggerFactory.getLogger(getClass());
	/** The parent TSDB instance */
	protected final TSDB tsdb;
	/** The extracted configuration */
	protected final Properties config;
	/** The delegate abstract service */
	protected final AbstractService abstractService;
	
	/**
	 * Creates a new AbstractRPCService
	 * @param tsdb The parent TSDB instance
	 * @param config The extracted configuration
	 */
	public AbstractRPCService(TSDB tsdb, Properties config) {
		this.tsdb = tsdb;
		this.config = config;
		if(rpcExecutor==null) {
			synchronized(lock) {
				if(rpcExecutor==null) {
					rpcExecutor = new AsyncDispatcherExecutor("RPCService", config);
				}
			}
		}
		final AbstractRPCService thisAbstractService = this;
		abstractService = new AbstractService(){
			@Override
			protected void doStart() {
				try {
					thisAbstractService.doStart();
					notifyStarted();
				} catch (Exception ex) {
					log.error("Failed to start RPC Service", ex);
					notifyFailed(ex);
				}								
			}
			@Override
			protected void doStop() {
				thisAbstractService.doStop();
				notifyStopped();
			}
		};
	}


	/**
	 * The service start hook
	 */
	protected void doStart() {
		log.info("\n\t======================================\n\tStarting RPC Service [{}]\n\t======================================\n", getClass().getSimpleName());
		startImpl();
		log.info("\n\t======================================\n\tRPC Service [{}] Started\n\t======================================\n", getClass().getSimpleName());
	}
	
	/**
	 * The concrete RPC service impl start
	 */
	protected void startImpl() {
		/* Re-implement me */		
	}

	/**
	 * The service stop hook
	 */
	protected void doStop() {
		log.info("\n\t======================================\n\tStopping RPC Service [{}]\n\t======================================\n", getClass().getSimpleName());
		stopImpl();
		log.info("\n\t======================================\n\tRPC Service [{}] Stopped\n\t======================================\n", getClass().getSimpleName());
	}

	/**
	 * The concrete RPC service impl stop
	 */
	protected void stopImpl() {
		/* Re-implement me */
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.rpc.IRPCService#collectStats(net.opentsdb.stats.StatsCollector)
	 */
	@Override
	public void collectStats(StatsCollector collector) {
		/* Re-implement me */
	}

	/**
	 * Starts the service and waits for the started state to be acquired
	 * @return The service state
	 * @see com.google.common.util.concurrent.AbstractService#startAndWait()
	 */
	public State startAndWait() {
		return abstractService.startAndWait();
	}

	/**
	 * Stops the service and waits for the stopped state to be acquired
	 * @return The service state
	 * @see com.google.common.util.concurrent.AbstractService#stopAndWait()
	 */
	public State stopAndWait() {
		return abstractService.stopAndWait();
	}

	/**
	 * Returns a string rendering of this service's state
	 * @return a string rendering of this object
	 * @see com.google.common.util.concurrent.AbstractService#toString()
	 */
	public String toString() {
		return abstractService.toString();
	}

	/**
	 * {@inheritDoc}
	 * @see com.google.common.util.concurrent.Service#start()
	 */
	@Override
	public ListenableFuture<State> start() {
		return abstractService.start();
	}

	/**
	 * {@inheritDoc}
	 * @see com.google.common.util.concurrent.Service#isRunning()
	 */
	@Override
	public boolean isRunning() {
		return abstractService.isRunning();
	}

	/**
	 * {@inheritDoc}
	 * @see com.google.common.util.concurrent.Service#state()
	 */
	@Override
	public State state() {
		return abstractService.state();
	}

	/**
	 * {@inheritDoc}
	 * @see com.google.common.util.concurrent.Service#stop()
	 */
	@Override
	public ListenableFuture<State> stop() {
		return abstractService.stop();
	}

	/**
	 * {@inheritDoc}
	 * @see com.google.common.util.concurrent.Service#addListener(com.google.common.util.concurrent.Service.Listener, java.util.concurrent.Executor)
	 */
	@Override
	public void addListener(Listener listener, Executor executor) {
		abstractService.addListener(listener, executor);
	}
	
	/**
	 * Adds a listener using the rpc executor
	 * @param listener The listener to add
	 */
	public void addListener(Listener listener) {
		abstractService.addListener(listener, rpcExecutor);
	}
	
}
