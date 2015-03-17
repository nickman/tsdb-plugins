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
package test.com.heliosapm.tsdb.plugins.async;

import java.lang.management.ManagementFactory;
import java.util.EnumMap;
import java.util.Map;

import javax.management.ObjectName;

import net.opentsdb.core.TSDB;

import org.cliffc.high_scale_lib.Counter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.heliosapm.tsdb.plugins.async.TSDBEvent;
import com.heliosapm.tsdb.plugins.async.TSDBEventConsumer;
import com.heliosapm.tsdb.plugins.async.TSDBEventType;
import com.stumbleupon.async.Deferred;

/**
 * <p>Title: EventCountingTSDBEventConsumer</p>
 * <p>Description: JMX exposed event counting event consumer</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>test.com.heliosapm.tsdb.plugins.async.EventCountingTSDBEventConsumer</code></p>
 */

public class EventCountingTSDBEventConsumer implements TSDBEventConsumer, EventCountingTSDBEventConsumerMBean {
	/** Events consumed counter */
	protected final Map<TSDBEventType, Counter> eventsConsumedCounter = new EnumMap<TSDBEventType, Counter>(TSDBEventType.class);
	/** The TSDB instance */
	protected final TSDB tsdb;
	/** Instance logger */
	protected final Logger log = LoggerFactory.getLogger(getClass());
	
	/**
	 * Creates a new EventCountingTSDBEventConsumer
	 * @param tsdb The main TSDB instance
	 */
	public EventCountingTSDBEventConsumer(final TSDB tsdb) {
		this.tsdb = tsdb;
		
		final int mask = getSubscribedEventMask();
		for(TSDBEventType et: TSDBEventType.values()) {
			if(et.isEnabled(mask))
			eventsConsumedCounter.put(et, new Counter());
		}
		try {
			ManagementFactory.getPlatformMBeanServer().registerMBean(this, new ObjectName("com.heliosapm.tsdb.plugins:service=EventCountingTSDBEventConsumer"));
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		}
		log.info("Created new EventCountingTSDBEventConsumer. TSDB: {}", tsdb);
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.tsdb.plugins.async.TSDBEventConsumer#getSubscribedEventMask()
	 */
	@Override
	public int getSubscribedEventMask() {
		return TSDBEventType.ALL_EVENT_MASK;
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.tsdb.plugins.async.TSDBEventConsumer#onEvent(com.heliosapm.tsdb.plugins.async.TSDBEvent)
	 */
	@Override
	public Deferred<?> onEvent(final TSDBEvent event) {
		eventsConsumedCounter.get(event.eventType).increment();
		return Deferred.fromResult(null);
	}
	
	/**
	 * {@inheritDoc}
	 * @see test.com.heliosapm.tsdb.plugins.async.EventCountingTSDBEventConsumerMBean#getUIDCacheSize()
	 */
	@Override
	public int getUIDCacheSize() {
		return tsdb.uidCacheSize();
	}
	
	/**
	 * {@inheritDoc}
	 * @see test.com.heliosapm.tsdb.plugins.async.EventCountingTSDBEventConsumerMBean#getUIDCacheHits()
	 */
	@Override
	public int getUIDCacheHits() {
		return tsdb.uidCacheHits();
	}
	
	/**
	 * {@inheritDoc}
	 * @see test.com.heliosapm.tsdb.plugins.async.EventCountingTSDBEventConsumerMBean#getUIDCacheMisses()
	 */
	@Override
	public int getUIDCacheMisses() {
		return tsdb.uidCacheMisses();
	}
	
	
	/**
	 * Returns the number of consumed {@link TSDBEventType#DPOINT_LONG} events
	 * @return the number of consumed events
	 */
	@Override
	public long getDPOINT_LONG() {
		return eventsConsumedCounter.get(TSDBEventType.DPOINT_LONG).get();
	}

	/**
	 * Returns the number of consumed {@link TSDBEventType#DPOINT_DOUBLE} events
	 * @return the number of consumed events
	 */
	@Override
	public long getDPOINT_DOUBLE() {
		return eventsConsumedCounter.get(TSDBEventType.DPOINT_DOUBLE).get();
	}

	/**
	 * Returns the number of consumed {@link TSDBEventType#ANNOTATION} events
	 * @return the number of consumed events
	 */
	@Override
	public long getANNOTATION() {
		return eventsConsumedCounter.get(TSDBEventType.ANNOTATION).get();
	}

	/**
	 * Returns the number of consumed {@link TSDBEventType#ANNOTATION_INDEX} events
	 * @return the number of consumed events
	 */
	@Override
	public long getANNOTATION_INDEX() {
		return eventsConsumedCounter.get(TSDBEventType.ANNOTATION_INDEX).get();
	}

	/**
	 * Returns the number of consumed {@link TSDBEventType#ANNOTATION_DELETE} events
	 * @return the number of consumed events
	 */
	@Override
	public long getANNOTATION_DELETE() {
		return eventsConsumedCounter.get(TSDBEventType.ANNOTATION_DELETE).get();
	}

	/**
	 * Returns the number of consumed {@link TSDBEventType#TSMETA_INDEX} events
	 * @return the number of consumed events
	 */
	@Override
	public long getTSMETA_INDEX() {
		return eventsConsumedCounter.get(TSDBEventType.TSMETA_INDEX).get();
	}

	/**
	 * Returns the number of consumed {@link TSDBEventType#TSMETA_DELETE} events
	 * @return the number of consumed events
	 */
	@Override
	public long getTSMETA_DELETE() {
		return eventsConsumedCounter.get(TSDBEventType.TSMETA_DELETE).get();
	}

	/**
	 * Returns the number of consumed {@link TSDBEventType#UIDMETA_INDEX} events
	 * @return the number of consumed events
	 */
	@Override
	public long getUIDMETA_INDEX() {
		return eventsConsumedCounter.get(TSDBEventType.UIDMETA_INDEX).get();
	}

	/**
	 * Returns the number of consumed {@link TSDBEventType#UIDMETA_DELETE} events
	 * @return the number of consumed events
	 */
	@Override
	public long getUIDMETA_DELETE() {
		return eventsConsumedCounter.get(TSDBEventType.UIDMETA_DELETE).get();
	}

	/**
	 * Returns the number of consumed {@link TSDBEventType#SEARCH} events
	 * @return the number of consumed events
	 */
	@Override
	public long getSEARCH() {
		return eventsConsumedCounter.get(TSDBEventType.SEARCH).get();
	}

	

}
