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
package org.helios.tsdb.plugins.async;

import java.util.Collection;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

import net.opentsdb.core.TSDB;
import net.opentsdb.meta.Annotation;
import net.opentsdb.meta.TSMeta;
import net.opentsdb.meta.UIDMeta;
import net.opentsdb.search.SearchQuery;
import net.opentsdb.stats.StatsCollector;

import org.helios.tsdb.plugins.event.TSDBEvent;
import org.helios.tsdb.plugins.handlers.IEventHandler;

import com.lmax.disruptor.RingBuffer;
import com.stumbleupon.async.Deferred;

/**
 * <p>Title: DisruptorEventDispatcher</p>
 * <p>Description: An async event dispatcher that uses a Disruptor RingBuffer to manage async dispatches.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.async.DisruptorEventDispatcher</code></p>
 */

public class DisruptorEventDispatcher implements AsyncEventDispatcher {

	/** The RingBuffer instance events are published to */
	protected RingBuffer<TSDBEvent> ringBuffer = null;
	
	@Override
	public void initialize(Properties config, Executor executor, Collection<IEventHandler> handlers) {
		// TODO Auto-generated method stub
		
	}

	/**
	 * Creates a new DisruptorEventDispatcher
	 */
	public DisruptorEventDispatcher() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.handlers.IPublishEventHandler#publishDataPoint(java.lang.String, long, double, java.util.Map, byte[])
	 */
	@Override
	public void publishDataPoint(String metric, long timestamp, double value,
			Map<String, String> tags, byte[] tsuid) {
		// TODO Auto-generated method stub

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.handlers.IPublishEventHandler#publishDataPoint(java.lang.String, long, long, java.util.Map, byte[])
	 */
	@Override
	public void publishDataPoint(String metric, long timestamp, long value,
			Map<String, String> tags, byte[] tsuid) {
		// TODO Auto-generated method stub

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.handlers.IEventHandler#initialize(net.opentsdb.core.TSDB)
	 */
	@Override
	public void initialize(TSDB tsdb) {
		// TODO Auto-generated method stub

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.handlers.IEventHandler#shtdown()
	 */
	@Override
	public void shtdown() {
		// TODO Auto-generated method stub

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.handlers.IEventHandler#collectStats(net.opentsdb.stats.StatsCollector)
	 */
	@Override
	public void collectStats(StatsCollector collector) {
		// TODO Auto-generated method stub

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.handlers.ISearchEventHandler#executeQuery(net.opentsdb.search.SearchQuery)
	 */
	@Override
	public Deferred<SearchQuery> executeQuery(SearchQuery searchQuery) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.handlers.ISearchEventHandler#indexAnnotation(net.opentsdb.meta.Annotation)
	 */
	@Override
	public void indexAnnotation(Annotation annotation) {
		// TODO Auto-generated method stub

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.handlers.ISearchEventHandler#deleteAnnotation(net.opentsdb.meta.Annotation)
	 */
	@Override
	public void deleteAnnotation(Annotation annotation) {
		// TODO Auto-generated method stub

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.handlers.ISearchEventHandler#indexTSMeta(net.opentsdb.meta.TSMeta)
	 */
	@Override
	public void indexTSMeta(TSMeta tsMeta) {
		// TODO Auto-generated method stub

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.handlers.ISearchEventHandler#deleteTSMeta(java.lang.String)
	 */
	@Override
	public void deleteTSMeta(String tsMeta) {
		// TODO Auto-generated method stub

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.handlers.ISearchEventHandler#indexUIDMeta(net.opentsdb.meta.UIDMeta)
	 */
	@Override
	public void indexUIDMeta(UIDMeta uidMeta) {
		// TODO Auto-generated method stub

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.handlers.ISearchEventHandler#deleteUIDMeta(net.opentsdb.meta.UIDMeta)
	 */
	@Override
	public void deleteUIDMeta(UIDMeta uidMeta) {
		// TODO Auto-generated method stub

	}



}
