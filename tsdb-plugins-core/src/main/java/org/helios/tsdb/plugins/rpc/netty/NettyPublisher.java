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

import org.helios.tsdb.plugins.event.TSDBEvent;
import org.helios.tsdb.plugins.event.TSDBEventType;
import org.helios.tsdb.plugins.event.TSDBPublishEvent;
import org.helios.tsdb.plugins.handlers.EmptyPublishEventHandler;
import org.helios.tsdb.plugins.service.PluginContext;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

/**
 * <p>Title: NettyPublisher</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.rpc.netty.NettyPublisher</code></p>
 */

public class NettyPublisher extends EmptyPublishEventHandler {

	/**
	 * Creates a new NettyPublisher
	 */
	public NettyPublisher() {
		super();
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.handlers.AbstractTSDBEventHandler#initialize(org.helios.tsdb.plugins.service.PluginContext)
	 */
	@Override
	public void initialize(PluginContext pc) {		
		super.initialize(pc);
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.handlers.EmptyPublishEventHandler#onEvent(org.helios.tsdb.plugins.event.TSDBEvent, long, boolean)
	 */
	@Override
	public void onEvent(TSDBEvent event, long sequence, boolean endOfBatch) throws Exception {
		if(log.isTraceEnabled()) log.trace("Processing Sequence {} for Event [{}]", sequence, event);
		if(event.eventType==null || !event.eventType.isForPulisher()) return;
		// === Event Fields ===
//		this.eventType = TSDBEventType.DPOINT_DOUBLE; or DPOINT_LONG
//		this.metric = metric;
//		this.timestamp = timestamp;
//		this.doubleValue = value;  or this.longValue
//		this.tags = tags;
//		this.tsuidBytes = tsuid;
		
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.handlers.EmptyPublishEventHandler#onEvent(org.helios.tsdb.plugins.event.TSDBPublishEvent)
	 */
	@Override
	@Subscribe
	@AllowConcurrentEvents		
	public void onEvent(TSDBPublishEvent event) throws Exception {
		onEvent(event, -1L, false);		
	}

}
