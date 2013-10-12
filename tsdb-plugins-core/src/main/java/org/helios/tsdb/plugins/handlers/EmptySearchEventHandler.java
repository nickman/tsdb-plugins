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

import org.helios.tsdb.plugins.event.TSDBEvent;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import com.lmax.disruptor.EventHandler;

/**
 * <p>Title: EmptySearchEventHandler</p>
 * <p>Description: Base class for implementing OpenTSDB {@link net.opentsdb.search.SearchPlugin} event handlers.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.handlers.EmptySearchEventHandler</code></p>
 */

public class EmptySearchEventHandler  extends AbstractTSDBEventHandler implements EventHandler<TSDBEvent>  {

	/**
	 * Creates a new EmptySearchEventHandler
	 */
	public EmptySearchEventHandler() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void onAsynchStart() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onAsynchShutdown() {
		// TODO Auto-generated method stub
		
	}

	/**
	 * {@inheritDoc}
	 * @see com.lmax.disruptor.EventHandler#onEvent(java.lang.Object, long, boolean)
	 */
	@Override
	public void onEvent(TSDBEvent event, long sequence, boolean endOfBatch) throws Exception {
		// TODO Auto-generated method stub
		
	}
	
	/**
	 * @param event
	 * @throws Exception
	 */
	@Subscribe
	@AllowConcurrentEvents	
	public void onEvent(TSDBEvent event) throws Exception {
		
	}
	

	@Override
	public void start() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void configure(TSDB tsdb) {
		// TODO Auto-generated method stub
		
	}

}
