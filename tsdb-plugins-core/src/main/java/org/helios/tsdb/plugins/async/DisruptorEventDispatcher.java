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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Executor;

import net.opentsdb.meta.Annotation;
import net.opentsdb.meta.TSMeta;
import net.opentsdb.meta.UIDMeta;
import net.opentsdb.search.SearchQuery;
import net.opentsdb.stats.StatsCollector;

import org.helios.tsdb.plugins.Constants;
import org.helios.tsdb.plugins.event.TSDBEvent;
import org.helios.tsdb.plugins.event.TSDBSearchEvent;
import org.helios.tsdb.plugins.handlers.IEventHandler;
import org.helios.tsdb.plugins.util.ConfigurationHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lmax.disruptor.BatchEventProcessor;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.Sequence;
import com.lmax.disruptor.SequenceBarrier;
import com.lmax.disruptor.WaitStrategy;
import com.stumbleupon.async.Deferred;

/**
 * <p>Title: DisruptorEventDispatcher</p>
 * <p>Description: An async event dispatcher that uses a Disruptor RingBuffer to manage async dispatches.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.async.DisruptorEventDispatcher</code></p>
 */

public class DisruptorEventDispatcher implements AsyncEventDispatcher, EventHandler<TSDBEvent> {
	/** Instance logger */
	protected final Logger log = LoggerFactory.getLogger(getClass());
	/** The RingBuffer instance events are published to */
	protected RingBuffer<TSDBEvent> ringBuffer = null;
	/** The executor driving the async bus */
	protected Executor executor = null;
	/** The wait strategy */
	protected WaitStrategy waitStrategy = null;
	/** The ring buffer size */
	protected int ringBufferSize;
	/** The event handler sequence barrier */
	protected SequenceBarrier eventHandlerSequenceBarrier = null;
	/** The event handler batch processors */
	protected final Set<BatchEventProcessor<TSDBEvent>> eventHandlerBatchProcessors = new HashSet<BatchEventProcessor<TSDBEvent>>();
	/** The closer sequence barrier */
	protected SequenceBarrier closerSequenceBarrier = null;
	/** The closer batch processor */
	protected BatchEventProcessor<TSDBEvent> closerBatchProcessor;

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.async.AsyncEventDispatcher#initialize(java.util.Properties, java.util.concurrent.Executor, java.util.Collection)
	 */
	@Override
	public void initialize(Properties config, Executor executor, Collection<IEventHandler> handlers) {
		log.info("\n\t========================================\n\tStarting DisruptorEventDispatcher\n\t========================================\n");
		this.executor = executor;
		ringBufferSize = ConfigurationHelper.getIntSystemThenEnvProperty(Constants.RING_BUFFER_SIZE, Constants.DEFAULT_RING_BUFFER_SIZE, config);
		String[] waitStrategyArgs = ConfigurationHelper.getSystemThenEnvPropertyArray(Constants.RING_BUFFER_WAIT_STRAT_ARGS, Constants.DEFAULT_RING_BUFFER_WAIT_STRAT_ARGS, config);
		String waitStrategyClassName = ConfigurationHelper.getSystemThenEnvProperty(Constants.RING_BUFFER_WAIT_STRAT, Constants.DEFAULT_RING_BUFFER_WAIT_STRAT, config);
		log.info("Creating Dispruptor WaitStrategy [{}] with args {}....", waitStrategyClassName, Arrays.toString(waitStrategyArgs));
		waitStrategy = WaitStrategyFactory.newWaitStrategy(waitStrategyClassName, waitStrategyArgs);
		log.info("Dispruptor WaitStrategy Created");
		ringBuffer = RingBuffer.createMultiProducer(TSDBEvent.EVENT_FACTORY, ringBufferSize, waitStrategy);
		
		
		eventHandlerSequenceBarrier = ringBuffer.newBarrier();
		for(IEventHandler handler: handlers) {
			if(handler instanceof EventHandler) {
				EventHandler<TSDBEvent> eventHandler = (EventHandler<TSDBEvent>)handler;
				eventHandlerBatchProcessors.add(
						new BatchEventProcessor<TSDBEvent>(ringBuffer, eventHandlerSequenceBarrier, eventHandler)
				);
				log.info("Registered TSDBEventHandler [{}]", handler.getClass().getName());
			} else {
				log.warn("The handler [{}] does not implement [{}]. Not registered to handle events", handler.getClass().getName(), EventHandler.class.getName());
			}
		}
		if(eventHandlerBatchProcessors.isEmpty()) {
			// FIXME: Do some shutdown/cleanup here.
			throw new RuntimeException("No event handlers registered. Cannot continue.");
		}
		log.info("Registered [{}] AsyncEvent Handlers", eventHandlerBatchProcessors.size());
		Sequence[] eventHandlerSequences = new Sequence[eventHandlerBatchProcessors.size()];
		int index = 0;
		for(BatchEventProcessor<TSDBEvent> bep: eventHandlerBatchProcessors) {
			eventHandlerSequences[index] = bep.getSequence();
		}
		closerSequenceBarrier = ringBuffer.newBarrier(eventHandlerSequences);
		closerBatchProcessor = new BatchEventProcessor<TSDBEvent>(ringBuffer, closerSequenceBarrier, this);
		ringBuffer.addGatingSequences(closerBatchProcessor.getSequence());		
		log.info("Initialized Disruptor Closer.\n\tStarting RingBuffer Event Processing.....");
		for(BatchEventProcessor<TSDBEvent> bep: eventHandlerBatchProcessors) {
			executor.execute(bep);
		}
		executor.execute(closerBatchProcessor);
		
		log.info("\n\t========================================\n\tDisruptorEventDispatcher Started\n\t========================================\n");
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.async.AsyncEventDispatcher#shutdown()
	 */
	@Override
	public void shutdown() {
		log.info("\n\t========================================\n\tStopping DisruptorEventDispatcher\n\t========================================\n");
		for(BatchEventProcessor<TSDBEvent> bep: eventHandlerBatchProcessors) {
			bep.halt(); log.info("Stopped [{}] Handler", bep.getClass().getSimpleName());
		}
		closerBatchProcessor.halt();
		log.info("Stopped Closer Handler");
		log.info("\n\t========================================\n\tDisruptorEventDispatcher Stopped\n\t========================================\n");
	}
	

	/**
	 * Creates a new DisruptorEventDispatcher
	 */
	public DisruptorEventDispatcher() {
		// TODO Auto-generated constructor stub
	}
	

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.async.IPublishEventDispatcher#publishDataPoint(java.lang.String, long, double, java.util.Map, byte[])
	 */
	@Override
	public void publishDataPoint(String metric, long timestamp, double value, Map<String, String> tags, byte[] tsuid) {
        long sequence = ringBuffer.next();
        ringBuffer.get(sequence).publishDataPoint(metric, timestamp, value, tags, tsuid);
        ringBuffer.publish(sequence);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.async.IPublishEventDispatcher#publishDataPoint(java.lang.String, long, long, java.util.Map, byte[])
	 */
	@Override
	public void publishDataPoint(String metric, long timestamp, long value, Map<String, String> tags, byte[] tsuid) {
        long sequence = ringBuffer.next();
        ringBuffer.get(sequence).publishDataPoint(metric, timestamp, value, tags, tsuid);
        ringBuffer.publish(sequence);
	}




	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.async.AsyncEventDispatcher#collectStats(net.opentsdb.stats.StatsCollector)
	 */
	@Override
	public void collectStats(StatsCollector collector) {
		// TODO Auto-generated method stub

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.async.ISearchEventDispatcher#executeQuery(net.opentsdb.search.SearchQuery)
	 */
	@Override
	public Deferred<SearchQuery> executeQuery(SearchQuery searchQuery) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.async.ISearchEventDispatcher#indexAnnotation(net.opentsdb.meta.Annotation)
	 */
	@Override
	public void indexAnnotation(Annotation annotation) {
        long sequence = ringBuffer.next();
        ringBuffer.get(sequence).indexAnnotation(annotation);
        ringBuffer.publish(sequence);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.async.ISearchEventDispatcher#deleteAnnotation(net.opentsdb.meta.Annotation)
	 */
	@Override
	public void deleteAnnotation(Annotation annotation) {
        long sequence = ringBuffer.next();
        ringBuffer.get(sequence).deleteAnnotation(annotation);
        ringBuffer.publish(sequence);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.async.ISearchEventDispatcher#indexTSMeta(net.opentsdb.meta.TSMeta)
	 */
	@Override
	public void indexTSMeta(TSMeta tsMeta) {
        long sequence = ringBuffer.next();
        ringBuffer.get(sequence).indexTSMeta(tsMeta);
        ringBuffer.publish(sequence);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.async.ISearchEventDispatcher#deleteTSMeta(java.lang.String)
	 */
	@Override
	public void deleteTSMeta(String tsMeta) {
        long sequence = ringBuffer.next();
        ringBuffer.get(sequence).deleteTSMeta(tsMeta);
        ringBuffer.publish(sequence);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.async.ISearchEventDispatcher#indexUIDMeta(net.opentsdb.meta.UIDMeta)
	 */
	@Override
	public void indexUIDMeta(UIDMeta uidMeta) {
        long sequence = ringBuffer.next();
        ringBuffer.get(sequence).indexUIDMeta(uidMeta);
        ringBuffer.publish(sequence);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.async.ISearchEventDispatcher#deleteUIDMeta(net.opentsdb.meta.UIDMeta)
	 */
	@Override
	public void deleteUIDMeta(UIDMeta uidMeta) {
        long sequence = ringBuffer.next();
        ringBuffer.get(sequence).deleteUIDMeta(uidMeta);
        ringBuffer.publish(sequence);
	}




	/**
	 * <p>This is the default closer handler for the event</p> 
	 * {@inheritDoc}
	 * @see com.lmax.disruptor.EventHandler#onEvent(java.lang.Object, long, boolean)
	 */
	@Override
	public void onEvent(TSDBEvent event, long sequence, boolean endOfBatch) throws Exception {
		if(log.isTraceEnabled()) log.trace("Completed TSDBEvent type [{}], seq:{}, eob:{}", event.eventType, sequence, endOfBatch);
		//event.reset();
	}



}
