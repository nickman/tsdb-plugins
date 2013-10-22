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

import net.opentsdb.core.TSDB;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.AbstractService;

/**
 * <p>Title: AbstractRPCService</p>
 * <p>Description: Abstract base class for RPC service implementations.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.rpc.AbstractRPCService</code></p>
 */

public abstract class AbstractRPCService extends AbstractService implements IRPCService {
	/** Instance logger */
	protected final Logger log = LoggerFactory.getLogger(getClass());
	/** The parent TSDB instance */
	protected final TSDB tsdb;
	/** The extracted configuration */
	protected final Properties config;
	
	/**
	 * Creates a new AbstractRPCService
	 * @param tsdb The parent TSDB instance
	 * @param config The extracted configuration
	 */
	public AbstractRPCService(TSDB tsdb, Properties config) {
		this.tsdb = tsdb;
		this.config = config;
	}

	/**
	 * {@inheritDoc}
	 * @see com.google.common.util.concurrent.AbstractService#doStart()
	 */
	@Override
	protected void doStart() {
		log.info("\n\t======================================\n\tStarting RPC Service [{}]\n\t======================================\n", getClass().getSimpleName());
		startImpl();
		log.info("\n\t======================================\n\tRPC Service [{}] Started\n\t======================================\n", getClass().getSimpleName());
	}
	
	/**
	 * The concrete RPC service impl start
	 */
	protected abstract void startImpl();

	/**
	 * {@inheritDoc}
	 * @see com.google.common.util.concurrent.AbstractService#doStop()
	 */
	@Override
	protected void doStop() {
		log.info("\n\t======================================\n\tStopping RPC Service [{}]\n\t======================================\n", getClass().getSimpleName());
		stopImpl();
		log.info("\n\t======================================\n\tRPC Service [{}] Stopped\n\t======================================\n", getClass().getSimpleName());
	}

	/**
	 * The concrete RPC service impl stop
	 */
	protected abstract void stopImpl();
	
}
