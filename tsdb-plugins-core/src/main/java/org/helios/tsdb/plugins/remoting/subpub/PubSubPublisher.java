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
package org.helios.tsdb.plugins.remoting.subpub;

import javax.management.ObjectName;

import org.hbase.async.jsr166e.LongAdder;
import org.helios.tsdb.plugins.event.TSDBEvent;
import org.helios.tsdb.plugins.event.TSDBEventType;
import org.helios.tsdb.plugins.handlers.EmptyPublishEventHandler;
import org.helios.tsdb.plugins.service.IPluginContextResourceFilter;
import org.helios.tsdb.plugins.service.IPluginContextResourceListener;
import org.helios.tsdb.plugins.service.PluginContext;


/**
 * <p>Title: PubSubPublisher</p>
 * <p>Description: RT Publisher event handler to stream incoming events to the SubscriptionManager.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.remoting.subpub.PubSubPublisher</code></p>
 */

public class PubSubPublisher extends EmptyPublishEventHandler implements PubSubPublisherMXBean {
	/** The sub manager to push events to */
	protected SubscriptionManager subManager = null;
	
	/** Counter for received messages */
	protected final LongAdder events = new LongAdder();
	/** Counter for exceptions processing events */
	protected final LongAdder errors = new LongAdder();
	
	
	/**
	 * Creates a new PubSubPublisher
	 */
	public PubSubPublisher() {
		log.info("Created PubSubPublisher Instance");
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.handlers.AbstractTSDBEventHandler#initialize(org.helios.tsdb.plugins.service.PluginContext)
	 */
	@Override
	public void initialize(final PluginContext pc) {
		final PubSubPublisher rez = this;
		pc.addResourceListener(new IPluginContextResourceListener() {
			public void onResourceRegistered(String name, Object resource) {
				subManager = (SubscriptionManager)resource;
				pc.setResource(PubSubPublisher.class.getSimpleName(), rez);
				log.info("PubSubPublisher ready for business");
			}
		}, new IPluginContextResourceFilter() {
			public boolean include(String name, Object resource) {				
				return (resource!=null && (resource instanceof SubscriptionManager));
			}
		});
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
		try {
			if(event.eventType==TSDBEventType.DPOINT_DOUBLE) {
				subManager.onDataPoint(event.metric, event.timestamp, event.doubleValue, event.tags, event.tsuidBytes);
			} else {
				subManager.onDataPoint(event.metric, event.timestamp, event.longValue, event.tags, event.tsuidBytes);
			}
			events.increment();
		} catch (Exception ex) {
			errors.increment();
		}
	}	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.remoting.subpub.PubSubPublisherMXBean#resetCounters()
	 */
	@Override
	public void resetCounters() {
		events.add(events.longValue()*-1L);
		errors.add(errors.longValue()*-1L);
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.remoting.subpub.PubSubPublisherMXBean#getEventCount()
	 */
	@Override
	public long getEventCount() {
		return events.longValue();
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.remoting.subpub.PubSubPublisherMXBean#getErrorCount()
	 */
	@Override
	public long getErrorCount() {
		return errors.longValue();
	}

}
