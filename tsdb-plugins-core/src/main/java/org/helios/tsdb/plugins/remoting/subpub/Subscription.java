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

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.ObjectName;

import net.opentsdb.meta.TSMeta;
import net.opentsdb.meta.api.MetricsMetaAPI;
import net.opentsdb.meta.api.QueryContext;
import net.opentsdb.uid.UniqueId;

import org.hbase.async.jsr166e.LongAdder;
import org.helios.jmx.metrics.ewma.ConcurrentDirectEWMA;
import org.helios.jmx.metrics.ewma.DirectEWMAMBean;
import org.helios.tsdb.plugins.util.JMXHelper;
import org.helios.tsdb.plugins.util.bloom.UnsafeBloomFilter;

import reactor.core.composable.Deferred;
import reactor.core.composable.Promise;
import reactor.core.composable.spec.Promises;
import reactor.event.dispatch.Dispatcher;
import reactor.event.dispatch.SynchronousDispatcher;
import reactor.function.Consumer;

import com.google.common.hash.Funnel;
import com.google.common.hash.Funnels;
import com.google.common.hash.PrimitiveSink;

/**
 * <p>Title: Subscription</p>
 * <p>Description: Represents a SubscriptionManager subscription</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.remoting.subpub.Subscription</code></p>
 */

/**
 * <p>Title: Subscription</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.remoting.subpub.Subscription</code></p>
 */
public class Subscription implements SubscriptionMBean {
	/** The filter to quickly determine if an incoming message matches this subscription */
	private UnsafeBloomFilter<byte[]> filter; 
	/** The charset of the incoming messages */
	public static final Charset DEFAULT_CHARSET = Charset.defaultCharset();
	/** The default number of insertions */
	public static final int DEFAULT_INSERTIONS = 10000;
	/** The ingestion funnel for incoming qualified messages */
	public enum SubFunnel implements Funnel<byte[]> {
	     /** The singleton funnel */
	    INSTANCE;	     
	     private final Funnel<byte[]> myfunnel = Funnels.byteArrayFunnel();

		@Override
		public void funnel(byte[] from, PrimitiveSink into) {
			myfunnel.funnel(from, into);
		}
	   }	
	
	/** A serial number sequence for Subscription instances */
	private static final AtomicLong serial = new AtomicLong();
	
	/** A EWMA for measuring the elapsed time of isMemberOf */
	protected final ConcurrentDirectEWMA ewma = new ConcurrentDirectEWMA(1024);
	/** The reactor async dispatcher */
	protected final Dispatcher dispatcher;
	/** The reactor sync dispatcher */
	protected final SynchronousDispatcher syncDispatcher = new SynchronousDispatcher();
	/** The metrics meta access service */
	final MetricsMetaAPI metricsMeta;
	/** The subscription pattern */
	protected final CharSequence pattern;
	/** The subscription pattern as an ObjectName */
	protected final ObjectName patternObjectName;
	/** The subscription id for this subscription */
	protected final long subscriptionId;
	
	/** The total number of matched incoming messages */
	protected final LongAdder totalMatched = new LongAdder();
	/** The total number of bloom filter "might" failures */
	protected final LongAdder mightDropped = new LongAdder();
	
	/** The current number of retained (inserted) patterns */
	protected final AtomicInteger retained = new AtomicInteger();
	/** The total number of subscribers interested in this subscription */
	protected final AtomicInteger subscribers = new AtomicInteger();
	
	/** The default false positive probability */
	public static final double DEFAULT_PROB = 0.3d;
	
	/**
	 * Creates a new Subscription
	 * @param dispatcher The reactor async dispatcher
	 * @param metricsMeta The metrics meta access service
	 * @param pattern The subscription pattern
	 * @param expectedInsertions The number of expected insertions
	 */
	public Subscription(final Dispatcher dispatcher, final MetricsMetaAPI metricsMeta, final CharSequence pattern, final int expectedInsertions) {
		filter = UnsafeBloomFilter.create(SubFunnel.INSTANCE, expectedInsertions, DEFAULT_PROB);
		this.pattern = pattern;
		this.dispatcher = dispatcher;
		this.metricsMeta = metricsMeta;
		this.patternObjectName = JMXHelper.objectName(pattern);
		subscriptionId = serial.incrementAndGet();		
	}
	
	public DirectEWMAMBean getEWMA() {
		return ewma;
	}
	
	/**
	 * Creates a new Subscription
	 * @param dispatcher The reactor async dispatcher
	 * @param metricsMeta The metrics meta access service
	 * @param pattern The subscription pattern
	 * @param expectedInsertions The number of expected insertions
	 */
	public Subscription(final Dispatcher dispatcher, final MetricsMetaAPI metricsMeta, final ObjectName pattern, final int expectedInsertions) {
		this(dispatcher, metricsMeta, pattern.toString(), expectedInsertions);
	}
	
	
	/**
	 * Increments the subscriber count and returns the new total
	 * @return the new total number of subscribers
	 */
	public int addSubscriber() {
		return subscribers.incrementAndGet();
	}
	
	/**
	 * Decrements the subscriber count and returns the new total
	 * @return the new total number of subscribers
	 */
	public int removeSubscriber() {
		return subscribers.decrementAndGet();
	}
	
	
	/**
	 * Creates a new Subscription with the default expected insertions
	 * @param dispatcher The reactor async dispatcher
	 * @param metricsMeta The metrics meta access service
	 * @param pattern The subscription pattern
	 */
	public Subscription(final Dispatcher dispatcher, final MetricsMetaAPI metricsMeta, ObjectName pattern) {
		this(dispatcher, metricsMeta, pattern, DEFAULT_INSERTIONS);
	}
	
	/**
	 * Creates a new Subscription with the default expected insertions
	 * @param dispatcher The reactor async dispatcher
	 * @param metricsMeta The metrics meta access service
	 * @param pattern The subscription pattern
	 */
	public Subscription(final Dispatcher dispatcher, final MetricsMetaAPI metricsMeta, CharSequence pattern) {
		this(dispatcher, metricsMeta, pattern, DEFAULT_INSERTIONS);
	}
	
	/**
	 * Indicates if the passed message is a member of this subscription
	 * @param bytes The message to determine the membership of
	 * @param metric The metric name
	 * @param tags The metric tags
	 * @return false if the message is definitely NOT a member of this subscription, true if it is a member.
	 */
	public Promise<Boolean> isMemberOf(final byte[] bytes, final String metric, final Map<String, String> tags) {
		if(bytes==null) return Promises.success(false).get();
		final Deferred<Boolean, Promise<Boolean>> def = Promises.<Boolean>defer().dispatcher(this.dispatcher).get();
		final Promise<Boolean> promise = def.compose();
		dispatcher.execute(new Runnable() {
			public void run() {
				final long start = System.nanoTime();
				if(filter.mightContain(bytes)) {
					try {
						if(metric!=null && tags!=null) {
							final String fqn = SubscriptionManager.buildObjectName(metric, tags).toString();
							metricsMeta.evaluate(new QueryContext().setMaxSize(1).setPageSize(1), fqn).consume(new Consumer<List<TSMeta>>() {
								@Override
								public void accept(final List<TSMeta> t) {
									final boolean b = (t!=null && !t.isEmpty());
									def.accept(b);
									if(b) {
										index(bytes);
										totalMatched.increment();
									} else {
										mightDropped.increment();
									}
									ewma.append(System.nanoTime() - start);
								}
							}).when(Throwable.class, new Consumer<Throwable>() {
								@Override
								public void accept(final Throwable t) {
									def.accept(false);
									ewma.append(System.nanoTime() - start);
								}
							});
						} else {
							def.accept(false);
						}						
					} catch (Exception ex) {
						/* No Op */
					}
					
				}	
				ewma.append(System.nanoTime() - start);
				def.accept(false);
			}
		});
		return promise;		
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.remoting.subpub.SubscriptionMBean#test(java.lang.String)
	 */
	@Override
	public boolean test(String message) {
		final CountDownLatch latch = new CountDownLatch(1);
		final AtomicBoolean result = new AtomicBoolean(false);
		isMemberOf(message.getBytes(Charset.defaultCharset()), null, null).consume(new Consumer<Boolean>() {
			@Override
			public void accept(Boolean t) {
				result.set(t);
				latch.countDown();
			}
		}).when(Throwable.class, new Consumer<Throwable>() {
			@Override
			public void accept(Throwable t) {
				result.set(false);
				latch.countDown();				
			}
		});
		try {
			latch.await(3, TimeUnit.SECONDS);
		} catch (Exception ex) {
			/* No Op */
		}
		return result.get();
		
	}
	
	/**
	 * Indexes the passed message after it has been determined to be a member of this subscription 
	 * @param bytes the message to index
	 */
	public void index(final byte[] bytes) {
		if(bytes!=null) {
			if(filter.put(bytes)) {
				retained.incrementAndGet();
			}
		}
	}

	/**
	 * Indexes a time series id
	 * @param tsMeta the time series
	 */
	public void index(final TSMeta tsMeta) {		
		if(tsMeta!=null) {
			if(filter.put(UniqueId.stringToUid(tsMeta.getTSUID()))) {
				retained.incrementAndGet();
			}
		}		
	}
	

	/**
	 * Indexes a time series id. Internal version that does not trigger stats or pubs.
	 * @param tsuid the time series TSUID bytes
	 */
	void _internalIndex(final byte[] tsuid) {		
		if(tsuid!=null) {
			if(filter.put(tsuid)) {
				retained.incrementAndGet();
			}
		}		
	}

	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.remoting.subpub.SubscriptionMBean#getMatches()
	 */
	@Override
	public long getMatches() {
		return totalMatched.longValue();
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.remoting.subpub.SubscriptionMBean#getDrops()
	 */
	@Override
	public long getDrops() {
		return mightDropped.longValue();
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.remoting.subpub.SubscriptionMBean#getSize()
	 */
	@Override
	public int getSize() {
		return retained.get();
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.remoting.subpub.SubscriptionMBean#getErrorProbability()
	 */
	public double getErrorProbability() {
		return filter.expectedFalsePositiveProbability();
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.remoting.subpub.SubscriptionMBean#getRelativeProbability()
	 */
	@Override
	public double getRelativeProbability() {
		return DEFAULT_PROB - filter.expectedFalsePositiveProbability();
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.remoting.subpub.SubscriptionMBean#getSubscriberCount()
	 */
	@Override
	public int getSubscriberCount() {
		return subscribers.get();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.remoting.subpub.SubscriptionMBean#getSubscriptionId()
	 */
	@Override
	public long getSubscriptionId() {
		return subscriptionId;
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.remoting.subpub.SubscriptionMBean#getDescription()
	 */
	@Override
	public String getDescription() {
		return toString();
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.remoting.subpub.SubscriptionMBean#getMeanMatch()
	 */
	@Override
	public double getMeanMatch() {
		return ewma.getMean();
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.remoting.subpub.SubscriptionMBean#getEWMAMatch()
	 */
	@Override
	public double getEWMAMatch() {
		return ewma.getAverage();
	}
	

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Subscription [Id:");
		builder.append(subscriptionId);
		builder.append(", pattern:");
		builder.append(pattern);
		builder.append(", size:");
		builder.append(retained.get());
		builder.append(", subscribers:");
		builder.append(subscribers.get());
		builder.append("]");
		return builder.toString();
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ (int) (subscriptionId ^ (subscriptionId >>> 32));
		return result;
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Subscription other = (Subscription) obj;
		if (subscriptionId != other.subscriptionId)
			return false;
		return true;
	}
	
	
	
	
	
}
