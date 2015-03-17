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
package com.heliosapm.tsdb.plugins.async;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.management.ObjectName;

import net.opentsdb.core.TSDB;
import net.opentsdb.meta.Annotation;
import net.opentsdb.meta.TSMeta;
import net.opentsdb.meta.UIDMeta;
import net.opentsdb.search.SearchPlugin;
import net.opentsdb.search.SearchQuery;
import net.opentsdb.stats.StatsCollector;
import net.opentsdb.tsd.RTPublisher;
import net.opentsdb.utils.Config;

import org.cliffc.high_scale_lib.Counter;
import org.cliffc.high_scale_lib.NonBlockingHashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.dispatch.processor.Operation;
import reactor.core.dispatch.processor.Processor;
import reactor.core.dispatch.processor.spec.ProcessorSpec;
import reactor.fn.Consumer;
import reactor.jarjar.com.lmax.disruptor.WaitStrategy;

import com.stumbleupon.async.Deferred;

/**
 * <p>Title: AsyncProcessor</p>
 * <p>Description: The main Async TSDB Event acceptor and router to registered event consumers.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.tsdb.plugins.async.AsyncProcessor</code></p>
 */

public class AsyncProcessor implements Consumer<TSDBEvent>, AsyncProcessorMBean {
	/** The singleton instance */
	private static volatile AsyncProcessor instance = null;
	/** The singleton instance ctor lock */
	private static final Object lock = new Object();
	/** Instance logger */
	protected final Logger log;
	/** The async event processor */
	protected final Processor<TSDBEvent> processor;
	/** The TSDB instance */
	protected final TSDB tsdb;
	/** The registered TSDBEvent consumers */
	protected final CopyOnWriteArrayList<TSDBEventConsumer> eventConsumers = new CopyOnWriteArrayList<TSDBEventConsumer>();
	/** The registered plugins */
	protected final CopyOnWriteArrayList<Object> plugins = new CopyOnWriteArrayList<Object>();
	
	/** Events received counter */
	protected final Map<TSDBEventType, Set<Counter>> eventsReceivedCounter = new EnumMap<TSDBEventType, Set<Counter>>(TSDBEventType.class);
	/** Events consumed counter */
	protected final Map<TSDBEventType, Counter> eventsConsumedCounter = new EnumMap<TSDBEventType, Counter>(TSDBEventType.class);
	/** The processor's executor which we need to acquire reflectively since it's private */
	protected final ThreadPoolExecutor executor;
	
	
	
	
	
	/**
	 * 
	 * Acquires the AsyncProcessor singleton instance
	 * @param tsdb The TSDB instance. Mandatory for the first caller. Ignored otherwise.
	 * @return the AsyncProcessor singleton instance
	 */
	public static AsyncProcessor getInstance(final TSDB tsdb) {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					if(tsdb==null) throw new IllegalArgumentException("The passed TSDB was null");
					instance = new AsyncProcessor(tsdb);
					try {
						ManagementFactory.getPlatformMBeanServer().registerMBean(instance, OBJECT_NAME);
					} catch (Exception ex) {
						instance.log.warn("Failed to register JMX interface for [{}]. Continuing without.", OBJECT_NAME, ex);
					}
				}
			}
		}
		return instance;
	}
	
	/**
	 * Creates a new AsyncProcessor
	 * @param dataBufferSize The size of the TSDBEvent buffer
	 * @param waitStrategy The wait strategy of the underlying RingBuffer
	 * @param consumer The consumer of the TSDBEvents
	 */
	private AsyncProcessor(final TSDB tsdb) {
		log = LoggerFactory.getLogger(getClass());
		this.tsdb = tsdb;
		final DefaultedConfig dconfig = new DefaultedConfig(tsdb.getConfig());
		eventConsumers.addAll(dconfig.getEventConsumers(this.tsdb));
		final int dataBufferSize = dconfig.getInt(CONFIG_DATA_BUFFER_SIZE, DEFAULT_DATA_BUFFER_SIZE);
		final WaitStrategy waitStrategy = dconfig.getWaitStrategy(CONFIG_WAIT_STRATEGY, DEFAULT_WAIT_STRATEGY);				
		for(TSDBEventType type: TSDBEventType.values()) {
			eventsReceivedCounter.put(type, new NonBlockingHashSet<Counter>());
			eventsConsumedCounter.put(type, new Counter());
		}
		processor = new ProcessorSpec<TSDBEvent>()
			.multiThreadedProducer()
			.dataBufferSize(dataBufferSize)
			.dataSupplier(TSDBEvent.EVENT_FACTORY)
			.consume(this)
			.waitStrategy(waitStrategy)
			.get();		
		executor = getExecutor();
	}
	
	private ThreadPoolExecutor getExecutor() {
		ThreadPoolExecutor tpe = null;
		try {
			Field f = processor.getClass().getDeclaredField("executor");
			f.setAccessible(true);
			tpe = (ThreadPoolExecutor)f.get(processor);
			f.setAccessible(false);
		} catch (Exception ex) {
			tpe = null;
		}
		return tpe;
	}
	
	/**
	 * Stops the async processor
	 */
	public void shutdown() {
		processor.shutdown();
		try { ManagementFactory.getPlatformMBeanServer().unregisterMBean(OBJECT_NAME); } catch (Exception x) {/* No Op */}
	}
	
	/**
	 * Stats collected by the least recently registered plugin since the TSDB is not aware of this component 
	 * @param collector The collector to write the stats to
	 */
	public void collectProcessorStats(final StatsCollector collector) {
		for(Map.Entry<TSDBEventType, Set<Counter>> entry: eventsReceivedCounter.entrySet()) {
			long total = 0;
			for(Counter c: entry.getValue()) {
				total += c.get();
			}
			collector.record("events.received." + entry.getKey().name(), total, "pluginimpl=AsyncProcessor");
		}
	}
	
	/**
	 * Returns the total number of events published through the AsyncProcessor
	 * @return the total number of events published through the AsyncProcessor
	 */
	@Override
	public long getTotalEventsPublished() {
		long total = 0;
		for(Map.Entry<TSDBEventType, Set<Counter>> entry: eventsReceivedCounter.entrySet()) {
			for(Counter c: entry.getValue()) {
				total += c.get();
			}			
		}
		return total;		
	}
	
	/**
	 * Returns the total number of events consumed from the AsyncProcessor
	 * @return the total number of events consumed from the AsyncProcessor
	 */
	@Override
	public long getTotalEventsConsumed() {
		long total = 0;
		for(Map.Entry<TSDBEventType, Counter> entry: eventsConsumedCounter.entrySet()) {			
				total += entry.getValue().get();
		}
		return total;		
	}
	
	
	/**
	 * Registers a new consumer of {@link TSDBEvent}s
	 * @param eventConsumer The event consumer. No op if null.
	 */
	public void registerEventConsumer(final TSDBEventConsumer eventConsumer) {
		if(eventConsumer!=null) {
			eventConsumers.add(eventConsumer); 
		}
	}
	
	/**
	 * Removes a registered consumer of {@link TSDBEvent}s
	 * @param eventConsumer The event consumer. No op if null.
	 */
	public void removeEventConsumer(final TSDBEventConsumer eventConsumer) {
		if(eventConsumer!=null) {
			eventConsumers.remove(eventConsumer); 
		}
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see reactor.fn.Consumer#accept(java.lang.Object)
	 */
	@Override
	public void accept(final TSDBEvent event) {
		if(event!=null && !eventConsumers.isEmpty()) {
			final TSDBEventType eventType = event.eventType;
			for(TSDBEventConsumer consumer: eventConsumers) {
				if(eventType.isEnabled(consumer.getSubscribedEventMask())) {
					consumer.onEvent(event);
					eventsConsumedCounter.get(event.eventType).increment();
				}
			}
		}
	}
	
	/**
	 * Returns an invoker for delegate search plugins to delegate to
	 * @param delegatePlugin The plugin delegating to this handler
	 * @return an async delegating search plugin
	 */
	public SearchPlugin getSearchPluginAsyncInvoker(final SearchPlugin delegatePlugin) {
		return new AsyncSearchPluginHandler(delegatePlugin);
	}
	
	/**
	 * Returns an invoker for delegate RTPublisher plugins to delegate to
	 * @param delegatePlugin The plugin delegating to this handler
	 * @return an async delegating RTPublisher plugin
	 */
	public RTPublisher getRTPublisherPluginAsyncInvoker(final RTPublisher delegatePlugin) {
		return new AsyncRTPublisherPluginHandler(delegatePlugin);
	}
	
	
	/**
	 * <p>Title: AsyncSearchPluginHandler</p>
	 * <p>Description: An invoker for delegate search plugins to delegate to the async processor</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>com.heliosapm.tsdb.plugins.async.AsyncProcessor.AsyncSearchPluginHandler</code></p>
	 */
	protected class AsyncSearchPluginHandler extends SearchPlugin {
		/** Events received by this plugin counter */
		protected final Map<TSDBEventType, Counter> pluginEventsReceivedCounter = new EnumMap<TSDBEventType, Counter>(TSDBEventType.class);
		/** The delegating plugin */
		protected final SearchPlugin delegatePlugin;
		/** The delegating plugin name */
		protected final String delegatePluginName;
		
		/**
		 * Creates a new AsyncSearchPluginHandler
		 * @param delegatePlugin The delegating plugin
		 */
		public AsyncSearchPluginHandler(final SearchPlugin delegatePlugin) {
			if(delegatePlugin==null) throw new IllegalArgumentException("The passed SearchPlugin was null");
			this.delegatePlugin = delegatePlugin;
			plugins.add(delegatePlugin);
			delegatePluginName = "pluginimpl=" + delegatePlugin.getClass().getSimpleName();
			for(TSDBEventType eventType: TSDBEventType.values()) {
				if(eventType.isForSearch()) {
					final Counter counter = new Counter();
					pluginEventsReceivedCounter.put(eventType, counter);
					eventsReceivedCounter.get(eventType).add(counter);
				}
			}
		}

		/**
		 * {@inheritDoc}
		 * @see net.opentsdb.search.SearchPlugin#initialize(net.opentsdb.core.TSDB)
		 */
		@Override
		public void initialize(final TSDB tsdb) {
			/* No Op */
		}
		
		/**
		 * {@inheritDoc}
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {			
			return AsyncSearchPluginHandler.class.getSimpleName() + "<--" + delegatePluginName;
		}

		/**
		 * {@inheritDoc}
		 * @see net.opentsdb.search.SearchPlugin#shutdown()
		 */
		@Override
		public Deferred<Object> shutdown() {
			plugins.remove(this);
			return Deferred.fromResult(null);
		}

		@Override
		public String version() {
			return "2.0";
		}

		@Override
		public void collectStats(final StatsCollector collector) {
			if(collector!=null) {
				for(Map.Entry<TSDBEventType, Counter> entry: pluginEventsReceivedCounter.entrySet()) {
					collector.record("events.received." + entry.getKey().name(), entry.getValue().get(), delegatePluginName);
				}
			}
			if(!plugins.isEmpty() && this.delegatePlugin==plugins.get(0)) {
				collectProcessorStats(collector);
			}			
		}
		
		

		/**
		 * {@inheritDoc}
		 * @see net.opentsdb.search.SearchPlugin#indexTSMeta(net.opentsdb.meta.TSMeta)
		 */
		@Override
		public Deferred<Object> indexTSMeta(final TSMeta tsMeta) {			
			if(tsMeta!=null) {
				Operation<TSDBEvent> op = processor.prepare();
				TSDBEvent event = op.get();
				event.indexTSMeta(tsMeta);				
				op.commit();				
				pluginEventsReceivedCounter.get(event.eventType).increment();
			}	        
			return Deferred.fromResult(null);
		}

		/**
		 * {@inheritDoc}
		 * @see net.opentsdb.search.SearchPlugin#deleteTSMeta(java.lang.String)
		 */
		@Override
		public Deferred<Object> deleteTSMeta(final String tsuid) {
			if(tsuid!=null) {
				Operation<TSDBEvent> op = processor.prepare();
				TSDBEvent event = op.get();
				event.deleteTSMeta(tsuid);				
				op.commit();				
				pluginEventsReceivedCounter.get(event.eventType).increment();
			}	        
			return Deferred.fromResult(null);
		}

		/**
		 * {@inheritDoc}
		 * @see net.opentsdb.search.SearchPlugin#indexUIDMeta(net.opentsdb.meta.UIDMeta)
		 */
		@Override
		public Deferred<Object> indexUIDMeta(final UIDMeta uidMeta) {
			if(uidMeta!=null) {
				Operation<TSDBEvent> op = processor.prepare();
				TSDBEvent event = op.get();
				event.indexUIDMeta(uidMeta);				
				op.commit();
				pluginEventsReceivedCounter.get(event.eventType).increment();
			}	        
			return Deferred.fromResult(null);
		}

		/**
		 * {@inheritDoc}
		 * @see net.opentsdb.search.SearchPlugin#deleteUIDMeta(net.opentsdb.meta.UIDMeta)
		 */
		@Override
		public Deferred<Object> deleteUIDMeta(final UIDMeta uidMeta) {
			if(uidMeta!=null) {
				Operation<TSDBEvent> op = processor.prepare();
				TSDBEvent event = op.get();
				event.deleteUIDMeta(uidMeta);				
				op.commit();
				pluginEventsReceivedCounter.get(event.eventType).increment();
			}	        
			return Deferred.fromResult(null);
		}

		/**
		 * {@inheritDoc}
		 * @see net.opentsdb.search.SearchPlugin#indexAnnotation(net.opentsdb.meta.Annotation)
		 */
		@Override
		public Deferred<Object> indexAnnotation(final Annotation annotation) {
			if(annotation!=null) {
				Operation<TSDBEvent> op = processor.prepare();
				TSDBEvent event = op.get();
				event.indexAnnotation(annotation);				
				op.commit();
				pluginEventsReceivedCounter.get(event.eventType).increment();
			}	        
			return Deferred.fromResult(null);			
		}

		/**
		 * {@inheritDoc}
		 * @see net.opentsdb.search.SearchPlugin#deleteAnnotation(net.opentsdb.meta.Annotation)
		 */
		@Override
		public Deferred<Object> deleteAnnotation(final Annotation annotation) {
			if(annotation!=null) {
				Operation<TSDBEvent> op = processor.prepare();
				TSDBEvent event = op.get();
				event.deleteAnnotation(annotation);				
				op.commit();
				pluginEventsReceivedCounter.get(event.eventType).increment();
			}	        
			return Deferred.fromResult(null);			
		}

		/**
		 * {@inheritDoc}
		 * @see net.opentsdb.search.SearchPlugin#executeQuery(net.opentsdb.search.SearchQuery)
		 */
		@Override
		public Deferred<SearchQuery> executeQuery(final SearchQuery searchQuery) {
			final Deferred<SearchQuery> def = new Deferred<SearchQuery>(); 
			if(searchQuery!=null) {
				Operation<TSDBEvent> op = processor.prepare();
				TSDBEvent event = op.get();
				event.executeQuery(searchQuery, def);				
				op.commit();
				pluginEventsReceivedCounter.get(event.eventType).increment();
			}	        
			return def;	
		}		
	}
	
	/**
	 * <p>Title: AsyncRTPublisherPluginHandler</p>
	 * <p>Description: An invoker for delegate rtPublisher plugins to delegate to the async processor</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>com.heliosapm.tsdb.plugins.async.AsyncProcessor.AsyncRTPublisherPluginHandler</code></p>
	 */
	protected class AsyncRTPublisherPluginHandler extends RTPublisher {
		/** Events received by this plugin counter */
		protected final Map<TSDBEventType, Counter> pluginEventsReceivedCounter = new EnumMap<TSDBEventType, Counter>(TSDBEventType.class);
		/** The delegating plugin */
		protected final RTPublisher delegatePlugin;
		/** The delegating plugin name */
		protected final String delegatePluginName;
		
		/**
		 * Creates a new AsyncRTPublisherPluginHandler
		 * @param delegatePlugin The delegating plugin
		 */
		public AsyncRTPublisherPluginHandler(final RTPublisher delegatePlugin) {
			if(delegatePlugin==null) throw new IllegalArgumentException("The passed RTPublisher was null");
			this.delegatePlugin = delegatePlugin;
			plugins.add(delegatePlugin);
			delegatePluginName = "pluginimpl=" + delegatePlugin.getClass().getSimpleName();
			for(TSDBEventType eventType: TSDBEventType.values()) {
				if(eventType.isForPublisher()) {
					final Counter counter = new Counter();
					pluginEventsReceivedCounter.put(eventType, counter);
					eventsReceivedCounter.get(eventType).add(counter);
				}
			}
		}
		
		/**
		 * {@inheritDoc}
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {			
			return AsyncSearchPluginHandler.class.getSimpleName() + "<--" + delegatePluginName;
		}


		/**
		 * {@inheritDoc}
		 * @see net.opentsdb.tsd.RTPublisher#initialize(net.opentsdb.core.TSDB)
		 */
		@Override
		public void initialize(final TSDB tsdb) {
			/* No Op */
		}

		/**
		 * {@inheritDoc}
		 * @see net.opentsdb.tsd.RTPublisher#shutdown()
		 */
		@Override
		public Deferred<Object> shutdown() {
			plugins.remove(this);
			return Deferred.fromResult(null);
		}

		/**
		 * {@inheritDoc}
		 * @see net.opentsdb.tsd.RTPublisher#version()
		 */
		@Override
		public String version() {
			return "2.0.1";
		}

		/**
		 * {@inheritDoc}
		 * @see net.opentsdb.tsd.RTPublisher#collectStats(net.opentsdb.stats.StatsCollector)
		 */
		@Override
		public void collectStats(final StatsCollector collector) {
			if(collector!=null) {
				for(Map.Entry<TSDBEventType, Counter> entry: pluginEventsReceivedCounter.entrySet()) {
					collector.record("events.received." + entry.getKey().name(), entry.getValue().get(), delegatePluginName);
				}
			}
			if(!plugins.isEmpty() && this.delegatePlugin==plugins.get(0)) {
				collectProcessorStats(collector);
			}
		}

		/**
		 * {@inheritDoc}
		 * @see net.opentsdb.tsd.RTPublisher#publishDataPoint(java.lang.String, long, long, java.util.Map, byte[])
		 */
		@Override
		public Deferred<Object> publishDataPoint(final String metric, final long timestamp,
				final long value, final Map<String, String> tags, final byte[] tsuid) {
			if(metric!=null && tags!=null && tsuid!=null) {
				Operation<TSDBEvent> op = processor.prepare();
				TSDBEvent event = op.get();
				event.publishDataPoint(metric, timestamp, value, tags, tsuid);			
				op.commit();
				pluginEventsReceivedCounter.get(event.eventType).increment();
			}	        
			return Deferred.fromResult(null);
		}

		/**
		 * {@inheritDoc}
		 * @see net.opentsdb.tsd.RTPublisher#publishDataPoint(java.lang.String, long, double, java.util.Map, byte[])
		 */
		@Override
		public Deferred<Object> publishDataPoint(final String metric, final long timestamp,
				final double value, final Map<String, String> tags, final byte[] tsuid) {
			if(metric!=null && tags!=null && tsuid!=null) {
				Operation<TSDBEvent> op = processor.prepare();
				TSDBEvent event = op.get();
				event.publishDataPoint(metric, timestamp, value, tags, tsuid);			
				op.commit();
				pluginEventsReceivedCounter.get(event.eventType).increment();
			}	        
			return Deferred.fromResult(null);
		}

		/**
		 * {@inheritDoc}
		 * @see net.opentsdb.tsd.RTPublisher#publishAnnotation(net.opentsdb.meta.Annotation)
		 */
		@Override
		public Deferred<Object> publishAnnotation(final Annotation annotation) {
			if(annotation!=null) {
				Operation<TSDBEvent> op = processor.prepare();
				TSDBEvent event = op.get();
				event.publishAnnotation(annotation);			
				op.commit();
				pluginEventsReceivedCounter.get(event.eventType).increment();
			}	        
			return Deferred.fromResult(null);
		}

	}
	
	
	/**
	 * <p>Title: DefaultedConfig</p>
	 * <p>Description: A wrapper for {@link Config} that extends the API to support defaults if a property is not defined.</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>com.heliosapm.tsdb.plugins.async.AsyncProcessor.DefaultedConfig</code></p>
	 */
	public static class DefaultedConfig {
		/** The underlying TSDB config */
		private final Config config;
		 
		/** Static class logger */
		static final Logger LOG = LoggerFactory.getLogger(DefaultedConfig.class);

		/**
		 * Creates a new DefaultedConfig
		 * @param config The underlying TSDB config
		 */
		public DefaultedConfig(Config config) {
			super();
			this.config = config;
		}

		/**
		 * Returns the configured or default value for the passed property
		 * @param property The config property name
		 * @param defaultValue The default value if the property is not defined
		 * @return the configured or default value
		 */
		public final String getString(final String property, final String defaultValue) {
			if(property==null || property.trim().isEmpty()) throw new IllegalArgumentException("The passed property name was null or empty");
			return config.hasProperty(property) ? config.getString(property) : defaultValue;			
		}
		
		/**
		 * Attempts to find a constructor accepting a {@link TSDB} as a parameter, 
		 * then looks for a parameterless constructor, otherwise returns null.
		 * @param clazz The class to find a constructor for
		 * @return the located constructor, or null if one was not found.
		 */
		@SuppressWarnings("unchecked")
		protected Constructor<? extends TSDBEventConsumer> getCtors(final Class<?> clazz) {
			Constructor<? extends TSDBEventConsumer> ctor = null;
			try {
				ctor = (Constructor<? extends TSDBEventConsumer>) clazz.getDeclaredConstructor(TSDB.class);
			} catch (Exception ex) {
				try {
					ctor = (Constructor<? extends TSDBEventConsumer>) clazz.getDeclaredConstructor();
				} catch (Exception ex2) {
					/* No Op */
				}
			}
			return ctor;
		}
		
		/**
		 * Returns the configuration defines event consumers
		 * @param tsdb The TSDB instance to initialize the consumer instances with
		 * @return a [possibly empty] set of TSDBEvent consumer instances
		 */
		public final Set<TSDBEventConsumer> getEventConsumers(final TSDB tsdb) {
			Set<TSDBEventConsumer> consumers = new HashSet<TSDBEventConsumer>();
			final boolean failOnLoad = getBoolean(CONFIG_FAIL_ON_CONSUMER_LOAD, DEFAULT_FAIL_ON_CONSUMER_LOAD);
			final String[] classNames = getString(CONFIG_CONSUMERS, DEFAULT_CONSUMERS).split(",");
			for(String className: classNames) {
				if(className==null || className.trim().isEmpty()) continue;
				className = className.trim();
				Class<?> clazz = null;
				try {
					clazz = Class.forName(className, true, Thread.currentThread().getContextClassLoader());
					if(!TSDBEventConsumer.class.isAssignableFrom(clazz)) {
						throw new Exception("The class [" + className + "] does not implement " + TSDBEventConsumer.class.getName());
					}
					Constructor<? extends TSDBEventConsumer> ctor = getCtors(clazz);
					if(ctor==null) throw new IllegalArgumentException("Failed to find a ctor for event consumer class [" + clazz.getName() + "]");
					TSDBEventConsumer consumer = ctor.getParameterTypes().length==0 ? ctor.newInstance() : ctor.newInstance(tsdb);
					consumers.add(consumer);
				} catch (Exception ex) {
					LOG.error("Failed to load TSDBEvent Consumer [{}]", className, ex);
					if(failOnLoad) {
						throw new IllegalArgumentException("Failed to load TSDBEvent Consumer [" + className + "]", ex);
					} else {
						LOG.error("Ignoring failed consumer load");
					}
				}
				
			}
			return consumers;
		}

		/**
		 * Returns the configured or default value for the passed property
		 * @param property The config property name
		 * @param defaultValue The default value if the property is not defined
		 * @return the configured or default value
		 */
		public final int getInt(final String property, final int defaultValue) {
			if(property==null || property.trim().isEmpty()) throw new IllegalArgumentException("The passed property name was null or empty");
			return config.hasProperty(property) ? Integer.parseInt(config.getString(property).trim()) : defaultValue;			
		}

		/**
		 * Returns the configured or default value for the passed property
		 * @param property The config property name
		 * @param defaultValue The default value if the property is not defined
		 * @return the configured or default value
		 */
		public final long getLong(final String property, final long defaultValue) {
			if(property==null || property.trim().isEmpty()) throw new IllegalArgumentException("The passed property name was null or empty");
			return config.hasProperty(property) ? Long.parseLong(config.getString(property).trim()) : defaultValue;
		}

		/**
		 * Returns the configured or default value for the passed property
		 * @param property The config property name
		 * @param defaultValue The default value if the property is not defined
		 * @return the configured or default value
		 */
		public final boolean getBoolean(final String property, final boolean defaultValue) {
			if(property==null || property.trim().isEmpty()) throw new IllegalArgumentException("The passed property name was null or empty");
			return config.hasProperty(property) ? Boolean.parseBoolean(config.getString(property).trim()) : defaultValue;
		}
		
		/**
		 * Returns the configured or default value for the passed property
		 * @param property The config property name
		 * @param defaultValue The default value if the property is not defined
		 * @return the configured or default value
		 */
		public final TimeUnit getTimeUnit(final String property, final TimeUnit defaultValue) {
			if(property==null || property.trim().isEmpty()) throw new IllegalArgumentException("The passed property name was null or empty");
			return config.hasProperty(property) ? TimeUnit.valueOf(config.getString(property).trim()) : defaultValue;
		}
		
		/**
		 * Returns the configured or default value for the passed property
		 * @param property The config property name
		 * @param defaultValue The default value if the property is not defined
		 * @return the configured or default value
		 */
		public final WaitStrategy getWaitStrategy(final String property, final WaitStrategy defaultValue) {
			if(property==null || property.trim().isEmpty()) throw new IllegalArgumentException("The passed property name was null or empty");
			if(config.hasProperty(property)) {
				WaitStrategyFactory wsf = WaitStrategyFactories.valueOf(config.getString(property).trim());
				String[] params = null;
				if(config.hasProperty(property + ".params")) {
					params = config.getString(property + ".params").replace(" ", "").split(",");
				} else {
					params = new String[0];
				}
				return wsf.create(params);
			} else {
				return defaultValue;
			}
		}
	}

	/**
	 * Indicates if the processor's thread pool is shutdown
	 * @return true if the processor's thread pool is shutdown, false otherwise
	 * @see java.util.concurrent.ThreadPoolExecutor#isShutdown()
	 */
	@Override
	public boolean isShutdown() {
		return executor.isShutdown();
	}

	/**
	 * Indicates if the processor's thread pool is terminating
	 * @return true if the processor's thread pool is terminating, false otherwise
	 * @see java.util.concurrent.ThreadPoolExecutor#isTerminating()
	 */
	@Override
	public boolean isTerminating() {
		return executor.isTerminating();
	}

	/**
	 * Indicates if the processor's thread pool is terminated
	 * @return true if the processor's thread pool, false otherwise
	 * @see java.util.concurrent.ThreadPoolExecutor#isTerminated()
	 */
	@Override
	public boolean isTerminated() {
		return executor.isTerminated();
	}

	/**
	 * Sets the processor's thread pool core size
	 * @param corePoolSize the new pool core size
	 * @see java.util.concurrent.ThreadPoolExecutor#setCorePoolSize(int)
	 */
	@Override
	public void setCorePoolSize(final int corePoolSize) {
		executor.setCorePoolSize(corePoolSize);
	}

	/**
	 * Returns the processor's thread pool core pool size
	 * @return the core pool size
	 * @see java.util.concurrent.ThreadPoolExecutor#getCorePoolSize()
	 */
	@Override
	public int getCorePoolSize() {
		return executor.getCorePoolSize();
	}

	/**
	 * Indicates if the processor's thread pool allows core thread timeout
	 * @return true if the processor's thread pool allows core thread timeout, false otherwise
	 * @see java.util.concurrent.ThreadPoolExecutor#allowsCoreThreadTimeOut()
	 */
	@Override
	public boolean isCoreThreadTimeOutAllowed() {
		return executor.allowsCoreThreadTimeOut();
	}

	/**
	 * Sets the processor's thread pool core thread timeout enablement
	 * @param allow true to allow core thread timeout, false for core threads that stay alive until the thread pool terminate
	 * @see java.util.concurrent.ThreadPoolExecutor#allowCoreThreadTimeOut(boolean)
	 */
	@Override
	public void setCoreThreadTimeOutAllowed(final boolean allow) {
		executor.allowCoreThreadTimeOut(allow);
	}

	/**
	 * Sets the processor's thread pool max pool size
	 * @param maximumPoolSize The max pool size
	 * @see java.util.concurrent.ThreadPoolExecutor#setMaximumPoolSize(int)
	 */
	@Override
	public void setMaximumPoolSize(final int maximumPoolSize) {
		executor.setMaximumPoolSize(maximumPoolSize);
	}

	/**
	 * Sets the processor's thread pool max pool size
	 * @return tha max pool size
	 * @see java.util.concurrent.ThreadPoolExecutor#getMaximumPoolSize()
	 */
	@Override
	public int getMaximumPoolSize() {
		return executor.getMaximumPoolSize();
	}

	/**
	 * Sets the time limit for which threads may remain idle in the processor's thread pool before being terminated
	 * @param time The time limit in ms.
	 * @see java.util.concurrent.ThreadPoolExecutor#setKeepAliveTime(long, java.util.concurrent.TimeUnit)
	 */
	@Override
	public void setKeepAliveTimeMs(final long time) {
		executor.setKeepAliveTime(time, TimeUnit.MILLISECONDS);
	}

	/**
	 * Returns the time limit for which threads may remain idle in the processor's thread pool before being terminated
	 * @return the time limit in ms.
	 * @see java.util.concurrent.ThreadPoolExecutor#getKeepAliveTime(java.util.concurrent.TimeUnit)
	 */
	@Override
	public long getKeepAliveTimeMs() {
		return executor.getKeepAliveTime(TimeUnit.MILLISECONDS);
	}

	/**
	 * Returns the number of tasks queued in the processor's thread pool
	 * @return the number of queued tasks
	 */
	@Override
	public int getQueueDepth() {
		return executor.getQueue().size();
	}
	
	/**
	 * Returns the number of free slots in the processor thread pool's task queue
	 * @return the number of queue free slots
	 */
	@Override
	public int getQueueFreeSlots() {
		return executor.getQueue().remainingCapacity();
	}
	
	/**
	 * Returns the processor thread pool's task queue class name
	 * @return the processor thread pool's task queue class name
	 */
	@Override
	public String getQueueType() {
		return executor.getQueue().getClass().getSimpleName();
	}
	

	/**
	 * Returns the processor's thread pool current pool size
	 * @return the processor's thread pool current pool size
	 * @see java.util.concurrent.ThreadPoolExecutor#getPoolSize()
	 */
	@Override
	public int getPoolSize() {
		return executor.getPoolSize();
	}

	/**
	 * Returns the number of active tasks in the processor's thread pool
	 * @return the number of active tasks
	 * @see java.util.concurrent.ThreadPoolExecutor#getActiveCount()
	 */
	@Override
	public int getActiveCount() {
		return executor.getActiveCount();
	}

	/**
	 * Returns the largest number of threads that have ever simultaneously been in the pool.
	 * @return the thread highwater
	 * @see java.util.concurrent.ThreadPoolExecutor#getLargestPoolSize()
	 */
	@Override
	public int getLargestPoolSize() {
		return executor.getLargestPoolSize();
	}

	/**
	 * Returns the approximate total number of tasks that have ever been scheduled for execution. 
	 * @return the number of tasks scheduled
	 * @see java.util.concurrent.ThreadPoolExecutor#getTaskCount()
	 */
	@Override
	public long getTaskCount() {
		return executor.getTaskCount();
	}

	/**
	 * Returns the approximate total number of tasks that have completed execution.
	 * @return the number of tasks completed
	 * @see java.util.concurrent.ThreadPoolExecutor#getCompletedTaskCount()
	 */
	@Override
	public long getCompletedTaskCount() {
		return executor.getCompletedTaskCount();
	}

	static ObjectName objectName(final CharSequence cs) {
		try {
			return new ObjectName(cs.toString().trim());
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	

}
