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
package org.helios.tsdb.plugins.handlers;

import net.opentsdb.core.TSDB;

/**
 * <p>Title: AbstractTSDBEventHandler</p>
 * <p>Description: Abstract base class for implementing TSDB event handlers </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.handlers.AbstractTSDBEventHandler</code></p>
 */

public class AbstractTSDBEventHandler implements ITSDBEventHandler {

	/**
	 * Creates a new AbstractTSDBEventHandler
	 */
	public AbstractTSDBEventHandler() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.handlers.ITSDBEventHandler#start()
	 */
	@Override
	public void start() {
		// TODO Auto-generated method stub

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.handlers.ITSDBEventHandler#stop()
	 */
	@Override
	public void stop() {
		// TODO Auto-generated method stub

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.handlers.ITSDBEventHandler#configure(net.opentsdb.core.TSDB)
	 */
	@Override
	public void configure(TSDB tsdb) {
		// TODO Auto-generated method stub

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.handlers.ITSDBEventHandler#onAsynchStart()
	 */
	@Override
	public void onAsynchStart() {
		// TODO Auto-generated method stub

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.handlers.ITSDBEventHandler#onAsynchShutdown()
	 */
	@Override
	public void onAsynchShutdown() {
		// TODO Auto-generated method stub

	}

}
