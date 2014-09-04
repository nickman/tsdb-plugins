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

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.management.MXBean;

import net.opentsdb.core.TSDB;
import net.opentsdb.meta.TSMeta;
import net.opentsdb.meta.UIDMeta;
import net.opentsdb.meta.api.MetricsMetaAPI;
import net.opentsdb.meta.api.QueryContext;
import net.opentsdb.uid.UniqueId;
import net.opentsdb.utils.JSON;

import org.cliffc.high_scale_lib.NonBlockingHashMap;
import org.cliffc.high_scale_lib.NonBlockingHashMapLong;
import org.cliffc.high_scale_lib.NonBlockingHashSet;
import org.hbase.async.jsr166e.LongAdder;
import org.helios.jmx.metrics.ewma.ConcurrentDirectEWMA;
import org.helios.tsdb.plugins.async.SingletonEnvironment;
import org.helios.tsdb.plugins.remoting.json.JSONRequest;
import org.helios.tsdb.plugins.remoting.json.JSONRequestRouter;
import org.helios.tsdb.plugins.remoting.json.JSONResponse;
import org.helios.tsdb.plugins.remoting.json.annotations.JSONRequestHandler;
import org.helios.tsdb.plugins.remoting.json.annotations.JSONRequestService;
import org.helios.tsdb.plugins.rpc.AbstractRPCService;
import org.helios.tsdb.plugins.service.IPluginContextResourceFilter;
import org.helios.tsdb.plugins.service.IPluginContextResourceListener;
import org.helios.tsdb.plugins.service.PluginContext;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;

import reactor.core.Reactor;
import reactor.core.composable.Deferred;
import reactor.core.composable.Promise;
import reactor.core.composable.spec.Promises;
import reactor.event.selector.Selector;
import reactor.function.Consumer;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * <p>Title: SubscriptionManager</p>
 * <p>Description: Manages subscriptions on behalf of remote subscribers</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.remoting.subpub.SubscriptionManager</code></p>
 */
@JSONRequestService(name="pubsub", description="Manages subscriptions on behalf of remote subscribers")
@MXBean
public class SubscriptionManager extends AbstractRPCService implements ChannelFutureListener, SubscriptionManagerMXBean {
	/** A channel group of channels with subscriptions */
	protected final ChannelGroup subscribedChannels = new DefaultChannelGroup(getClass().getSimpleName());
	
	/** All of the current subscriptions keyed by the pattern */
	protected final NonBlockingHashMap<String, Subscription> allSubscriptions = new NonBlockingHashMap<String, Subscription>(1024); 
	
	/** A map of sets of subscriptions keyed by the id of the channel they were initiated by */
	protected final NonBlockingHashMapLong<Set<Subscription>> channelSubscriptions = new NonBlockingHashMapLong<Set<Subscription>>(128);
	/** A map of sets of channels subscribed to the same subscription keyed by the id of the subscription */
	protected final NonBlockingHashMapLong<Set<Channel>> subscriptionChannels = new NonBlockingHashMapLong<Set<Channel>>(128);
	
		
	
	/** The event reactor */
	protected final Reactor reactor;
	
	
	/** The metric lookup service to prime subscription members */
	protected MetricsMetaAPI metricSvc = null;	
	/** Counter for propagated event messages */
	protected final LongAdder events = new LongAdder();
	/** A EWMA for measuring the elapsed time of processing an event */
	protected final ConcurrentDirectEWMA ewma = new ConcurrentDirectEWMA(1024);

	
	/** The default multiplier factor where a subscription's bloom filter will be the size of the initial load
	 * multiplied by this value */  // FIXME: this should be configurable
	public static final float DEFAULT_BLOOM_FILTER_SPACE_FACTOR = 2.1f;
	
	/**
	 * Creates a new SubscriptionManager
	 * @param tsdb The parent TSDB instance
	 * @param config The extracted configuration
	 */
	public SubscriptionManager(TSDB tsdb, Properties config) {
		super(tsdb, config);
		reactor = SingletonEnvironment.getEnvironment().getRootReactor();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.rpc.AbstractRPCService#setPluginContext(org.helios.tsdb.plugins.service.PluginContext)
	 */
	@Override
	public void setPluginContext(final PluginContext ctx) {
		super.setPluginContext(ctx);		
		final SubscriptionManager rez = this;
		ctx.addResourceListener(new IPluginContextResourceListener() {
			public void onResourceRegistered(String name, Object resource) {
				metricSvc = (MetricsMetaAPI)resource;
				ctx.setResource(SubscriptionManager.class.getSimpleName(), rez);
			}
		}, new IPluginContextResourceFilter() {
			public boolean include(String name, Object resource) {				
				return (resource!=null && (resource instanceof MetricsMetaAPI));
			}
		});
		pluginContext.addResourceListener(
				new IPluginContextResourceListener() {
					@Override
					public void onResourceRegistered(String name, Object resource) {
						((JSONRequestRouter)resource).registerJSONService(rez);						
					}
				},
				new IPluginContextResourceFilter() {
					@Override
					public boolean include(String name, Object resource) {						
						return name.equals(JSONRequestRouter.class.getSimpleName());
					}
				}
		);		
		
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.remoting.subpub.SubscriptionManagerMXBean#getEventCount()
	 */
	public long getEventCount() {
		return events.longValue();
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.remoting.subpub.SubscriptionManagerMXBean#getSubscriptions()
	 */
	@Override
	public SubscriptionMBean[] getSubscriptions() {
		try {
			final Set<SubscriptionMBean> copy = new HashSet<SubscriptionMBean>(allSubscriptions.values());		
			return copy.toArray(new SubscriptionMBean[copy.size()]);
		} catch (Exception ex) {
			log.error("Failed to convert Subscriptions to CompositeData", ex);
			return new SubscriptionMBean[0];
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.ChannelFutureListener#operationComplete(org.jboss.netty.channel.ChannelFuture)
	 */
	@Override
	public void operationComplete(final ChannelFuture future) throws Exception {
		cleanupForChannel(future.getChannel());
	}
	
	/**
	 * Cleans up resources allocated for the specified channel
	 * @param channel the channel to clean up for
	 */
	protected void cleanupForChannel(final Channel channel) {
		if(channel==null) return;
		final long channelId = channel.getId().longValue();
		Set<Subscription> channelSubs = channelSubscriptions.remove(channelId);
		if(channelSubs!=null && !channelSubs.isEmpty()) {
			for(Subscription s: channelSubs) {
				final int remaining = s.removeSubscriber();
				Set<Channel> subChannels = subscriptionChannels.get(s.getSubscriptionId());
				if(subChannels!=null && !subChannels.isEmpty()) {
					subChannels.remove(channel);
				}
				if(remaining==0) {
					allSubscriptions.remove(s.pattern.toString());
					subscriptionChannels.remove(s.getSubscriptionId());
					if(subChannels!=null && !subChannels.isEmpty()) {
						log.info("Subscription [{}] indicated zero subscribers, but [{}] channels remain subscribed", s, subChannels.size());
						// notify the orphan channels
						subChannels.clear();
					} 
				}
			}
		}		
	}
	
	/**
	 * Subscribes the calling client to a subscription of events matching the passed expression
	 * @param request The JSON request
	 * <p>Invoker:<b><code>sendRemoteRequest('ws://localhost:4243/ws', {svc:'pubsub', op:'sub', {x:'sys*:dc=dc*,host=WebServer1|WebServer5'}});</code></b>
	 */
	@JSONRequestHandler(name="sub", sub=true, unsub="unsub", description="Creates a new subscription on behalf of the calling client")
	public void subscribe(final JSONRequest request) {
		final String expression = request.getRequest().get("x").textValue();
		final Channel channel = request.channel;
		subscribe(channel, expression).onSuccess(new Consumer<Subscription>() {
			@Override
			public void accept(final Subscription sub) {
				request.response().setOpCode(JSONResponse.RESP_TYPE_SUB_STARTED).setContent(JSON.getMapper().createObjectNode().put("subId", sub.getSubscriptionId())).send();
			}
		}).onError(new Consumer<Throwable>() {
			@Override
			public void accept(Throwable t) {
				request.error(new StringBuilder("Failed to subscribe to pattern [").append(expression).append("]"), t);
			}
		});		
	}
	
	private <T> reactor.core.composable.Deferred<T, Promise<T>> getDeferred() {
		return Promises.<T>defer()				
				  .get();		
	}
	
	
	
	/**
	 * Subscribes the passed channel to a subscription for the passed channel
	 * @param channel The channel to subscribe
	 * @param pattern The pattern of the subscription
	 * @return The new or already existing subscription
	 */
	protected Promise<Subscription> subscribe(final Channel channel, final CharSequence pattern) {
		if(channel==null) throw new IllegalArgumentException("The passed channel was null");
		if(pattern==null) throw new IllegalArgumentException("The passed pattern was null");
		if(metricSvc==null) throw new IllegalStateException("The SubscriptionManager has not been initialized", new Throwable());
		final String _pattern = pattern.toString().trim();
		if(_pattern.isEmpty()) throw new IllegalArgumentException("The passed pattern was empty");
		final long channelId = channel.getId().longValue();				
		final reactor.core.composable.Deferred<Subscription, Promise<Subscription>> def = getDeferred();
		final Promise<Subscription> promise = def.compose();
		final SubscriptionManager finalSm = this;
		reactor.getDispatcher().execute(new Runnable() {			
			public void run() {
				Subscription subx = allSubscriptions.get(_pattern);
				if(subx==null) {
					synchronized(allSubscriptions) {
						subx = allSubscriptions.get(_pattern);						
						if(subx==null) {							
							try {
								final QueryContext q = new QueryContext().setContinuous(true).setMaxSize(10240);
								final long startTime = System.currentTimeMillis();								
								final Set<byte[]> matchingTsuids = new HashSet<byte[]>(128);
								log.info("Executing search for TSMetas with pattern [{}]", pattern);
								metricSvc.evaluate(q, pattern.toString()).consume(new Consumer<List<TSMeta>>() {
									@Override
									public void accept(List<TSMeta> t) {
										int cnt = 0;
										for(TSMeta tsm: t) {
											if(matchingTsuids.add(UniqueId.stringToUid(tsm.getTSUID()))) {
												cnt++;
											}
											
										}
										log.info("Accepted [{}] TSMeta TSUIDs", cnt);
										t.clear();
										if(!q.shouldContinue()) {
											final long elapsedTime = System.currentTimeMillis()-startTime;
											log.info("Retrieved [{}] TSMetas in [{}] ms. to prime subscription [{}]", matchingTsuids.size(), elapsedTime, pattern);
											final int bloomFactor = Math.round(DEFAULT_BLOOM_FILTER_SPACE_FACTOR * matchingTsuids.size());
											final Subscription subx = new Subscription(reactor.getDispatcher(), metricSvc, _pattern, bloomFactor);
											int indexCnt = 0;
											for(final Iterator<byte[]> biter = matchingTsuids.iterator(); biter.hasNext();) {
												subx._internalIndex(biter.next());
												indexCnt++;
												biter.remove();
											}
											log.info("Created and initialized [{}] items in Subscription BloomFilter for [{}]", indexCnt, pattern);
											Set<Channel> subChannels = subscriptionChannels.get(subx.getSubscriptionId());
											if(subChannels==null) {
												synchronized(subscriptionChannels) {
													subChannels = subscriptionChannels.get(subx.getSubscriptionId());
													if(subChannels==null) {
														subChannels = new NonBlockingHashSet<Channel>();
														subscriptionChannels.put(subx.getSubscriptionId(), subChannels);
													}
												}
											}
											subChannels.add(channel);
											subx.addSubscriber();
											Set<Subscription> channelSubs = channelSubscriptions.get(channelId);
											if(channelSubs==null) {
												synchronized(channelSubscriptions) {
													channelSubs = channelSubscriptions.get(channelId);
													if(channelSubs==null) {
														channelSubs = new NonBlockingHashSet<Subscription>();
														channelSubscriptions.put(channelId, channelSubs);
													}
												}
											}
											channelSubs.add(subx);
											if(subscribedChannels.add(channel)) {
												channel.getCloseFuture().addListener(finalSm);
											}						
											allSubscriptions.put(_pattern, subx);	
											log.info("Completed Subscription [{}] in [{}] ms.", pattern, System.currentTimeMillis()-startTime);
											def.accept(subx);											
										}
									}
								});
							} catch (Exception ex) {
								throw new RuntimeException("Timed out on populating subscription for expression [" + pattern + "]");
							}					
						} else {
							def.accept(subx);
							return;
						}
					}				
				} else {
					def.accept(subx);
					return;									
				}
			}
		});
		
		return promise;
	}
	
	/**
	 * Builds an ObjectNode to publish a double value datapoint to subscribers
	 * @param metric The data point metric name
	 * @param timestamp The data point timestamp
	 * @param value The data point value
	 * @param tags The data point tags
	 * @param tsuid The data point time series id
	 * @return the built object node to publish
	 */
	public static ObjectNode build(final String metric, final long timestamp, final double value, final Map<String,String> tags, final byte[] tsuid) {
		final ObjectNode on = JSON.getMapper().createObjectNode();
		on.put("metric", metric);
		on.put("ts", timestamp);
		on.put("value", value);
		on.put("type", "d");
		
		final ObjectNode tagMap = JSON.getMapper().createObjectNode();
		for(Map.Entry<String, String> e: tags.entrySet()) {
			tagMap.put(e.getKey(), e.getValue());
		}
		on.put("tags", tagMap);
		return on;
	}
	
	/**
	 * Builds an ObjectNode to publish a long value datapoint to subscribers
	 * @param metric The data point metric name
	 * @param timestamp The data point timestamp
	 * @param value The data point value
	 * @param tags The data point tags
	 * @param tsuid The data point time series id
	 * @return the built object node to publish
	 */
	public static ObjectNode build(final String metric, final long timestamp, final long value, final Map<String,String> tags, final byte[] tsuid) {
		final ObjectNode on = JSON.getMapper().createObjectNode();
		on.put("metric", metric);
		on.put("ts", timestamp);
		on.put("value", value);
		on.put("type", "l");		
		final ObjectNode tagMap = JSON.getMapper().createObjectNode();
		for(Map.Entry<String, String> e: tags.entrySet()) {
			tagMap.put(e.getKey(), e.getValue());
		}
		on.put("tags", tagMap);
		return on;
	}
	

	
	/**
	 * Evaluates the incoming datapoint and tests each subscription to see if it is a member, then pushes the datapoint
	 * to the channels that have matching subscriptions 
	 * @param metric The name of the metric associated with the data point
	 * @param timestamp Timestamp as a Unix epoch in seconds or milliseconds (depending on the TSD's configuration)
	 * @param value Value for the data point
	 * @param isLong true if the value is a long, false if it is a double
	 * @param tags Tagk/v pairs
	 * @param tsuid The timeseries uid
	 * @return A completion promise
	 */
	public Promise<Void> onDataPoint(final String metric, final long timestamp, final Number value, final boolean isLong,  final Map<String,String> tags, final byte[] tsuid) {
		final Deferred<Void, Promise<Void>> def = Promises.<Void>defer().get();
		final Promise<Void> promise = def.compose();

		if(!allSubscriptions.isEmpty()) {
			final long startTime = System.nanoTime();
			try {
				ObjectNode node = null;
				for(Subscription s: allSubscriptions.values()) {
					attachEventHandlers(
							s.isMemberOf(tsuid, metric, tags), 
							s, node, metric, timestamp, value, 
							isLong, tags, tsuid, startTime
						).onComplete(new Consumer<Promise<Boolean>>() {
							@Override
							public void accept(Promise<Boolean> t) {
								def.accept((Void)null);
							}
						});
				}			
			} catch (Exception ex) {
				ewma.error();
				log.error("Failed to process double data point", ex);
			}
		}		
		return promise;
	}
	
	
	
	/**
	 * Attaches an event handler on the 
	 * @param promise The result promise from the metric meta service
	 * @param s The subscription 
	 * @param innode A possibly null packaged json node (which might have already been created)
	 * @param metric The data point metric name
	 * @param timestamp The data point timestamp 
	 * @param value The data point value
	 * @param isLong true if the data point is a long, false if it is a double 
	 * @param tags The data point tags
	 * @param tsuid The data point TSMeta UID
	 * @param startTime The start time in nanos so we can compute the total elapsed
	 * @return The passed promise so we can chain
	 */
	protected Promise<Boolean> attachEventHandlers(final Promise<Boolean> promise, final Subscription s, final ObjectNode innode, final String metric, final long timestamp, final Number value, final boolean isLong, final Map<String,String> tags, final byte[] tsuid, final long startTime) {
		promise.consume(new Consumer<Boolean>() {
			@Override
			public void accept(Boolean t) {
				if(t) {
					ObjectNode node = innode; 
					Set<Channel> channels = subscriptionChannels.get(s.getSubscriptionId());
					if(channels!=null && !channels.isEmpty()) {
						if(node==null) node = build(metric, timestamp, isLong ? value.longValue() : value.doubleValue(), tags, tsuid);
						node.put("subid", s.getSubscriptionId());
						for(Channel ch: channels) {
							ch.write(node);
						}
						events.increment();
					}
				}
				ewma.append(System.nanoTime()-startTime);				
			}
		}).when(Throwable.class, new Consumer<Throwable>() {
			@Override
			public void accept(Throwable t) {
				log.error("Failed to process subscription callback", t);
				ewma.error();
			}
		});
		return promise;
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.remoting.subpub.SubscriptionManagerMXBean#getChannelCount()
	 */
	@Override
	public int getChannelCount() {
		return subscribedChannels.size();
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.remoting.subpub.SubscriptionManagerMXBean#getSubscriptionCount()
	 */
	@Override
	public int getSubscriptionCount() {
		return allSubscriptions.size();
	}
	

	
	
	/** Fast string builder support */
	static final ThreadLocal<StringBuilder> stringBuilder = new ThreadLocal<StringBuilder>() {
		@Override
		protected StringBuilder initialValue() {
			return new StringBuilder(128);
		}
		@Override
		public StringBuilder get() {
			StringBuilder b = super.get();
			b.setLength(0);
			return b;
		}
	};
	
	
	
	/**
	 * Builds a stringy from the passed metric name and UID tags
	 * @param metric The metric name
	 * @param tags The UID tags
	 * @return a stringy
	 */
	static final CharSequence buildObjectName(final String metric, final List<UIDMeta> tags) {
		if(tags==null || tags.isEmpty()) throw new IllegalArgumentException("The passed tags map was null or empty");
		String mname = metric==null || metric.isEmpty() ? "*" : metric;
		StringBuilder b = stringBuilder.get().append(mname).append(":");
		boolean k = true;		
		for(final UIDMeta meta: tags) {
			b.append(meta.getName());
			if(k) {
				b.append("=");
			} else {
				b.append(",");
			}
			k = !k;
		}
		b.deleteCharAt(b.length()-1);
		return b.toString();
		
	}
	
	static final CharSequence buildObjectName(final String metric, final Map<String, String> tags) {
		if(tags==null || tags.isEmpty()) throw new IllegalArgumentException("The passed tags map was null or empty");
		String mname = metric==null || metric.isEmpty() ? "*" : metric;
		StringBuilder b = stringBuilder.get().append(mname).append(":");
		for(final Map.Entry<String, String> e: tags.entrySet()) {
			b.append(e.getKey()).append("=").append(e.getValue()).append(",");
		}
		b.deleteCharAt(b.length()-1);
		return b.toString();
	}

	/**
	 * Returns the timestamp of the last sample
	 * @return the timestamp of the last sample
	 * @see org.helios.jmx.metrics.ewma.ConcurrentDirectEWMA#getLastSample()
	 */
	public long getLastSample() {
		return ewma.getLastSample();
	}

	/**
	 * Returns the value of the last sampled time (ns) in the ewma
	 * @return the value of the last sampled time (ns) in the ewma
	 * @see org.helios.jmx.metrics.ewma.ConcurrentDirectEWMA#getLastValue()
	 */
	public double getLastValue() {
		return ewma.getLastValue();
	}

	/**
	 * Resets the EWMA
	 * @see org.helios.jmx.metrics.ewma.ConcurrentDirectEWMA#reset()
	 */
	public void resetEWMA() {
		ewma.reset();
	}

	/**
	 * Returns the number of errors recorded since the last reset
	 * @return the number of errors recorded
	 * @see org.helios.jmx.metrics.ewma.ConcurrentDirectEWMA#getErrors()
	 */
	public long getErrors() {
		return ewma.getErrors();
	}

	/**
	 * Returns the exponentially weighted moving average time in ns.
	 * @return the exponentially weighted moving average time in ns.
	 * @see org.helios.jmx.metrics.ewma.ConcurrentDirectEWMA#getAverage()
	 */
	public double getAverage() {
		return ewma.getAverage();
	}

	/**
	 * Returns the number of samples recorded since the last reset
	 * @return the number of samples recorded
	 * @see org.helios.jmx.metrics.ewma.ConcurrentDirectEWMA#getCount()
	 */
	public long getCount() {
		return ewma.getCount();
	}

	/**
	 * Returns the maximum sampled elapsed time
	 * @return the maximum sampled elapsed time
	 * @see org.helios.jmx.metrics.ewma.ConcurrentDirectEWMA#getMaximum()
	 */
	public double getMaximum() {
		return ewma.getMaximum();
	}

	/**
	 * Returns the mean sampled elapsed time
	 * @return the mean sampled elapsed time
	 * @see org.helios.jmx.metrics.ewma.ConcurrentDirectEWMA#getMean()
	 */
	public double getMean() {
		return ewma.getMean();
	}

	/**
	 * Returns the minimum sampled elapsed time
	 * @return the minimum sampled elapsed time
	 * @see org.helios.jmx.metrics.ewma.ConcurrentDirectEWMA#getMinimum()
	 */
	public double getMinimum() {
		return ewma.getMinimum();
	}

	/**
	 * Returns the EWMA window size
	 * @return the EWMA window size
	 * @see org.helios.jmx.metrics.ewma.DirectEWMA#getWindow()
	 */
	public long getWindow() {
		return ewma.getWindow();
	}

	


}




/*
//.addCallbacks(new Callback<Subscription, Set<TSMeta>>() {
//
//	int count = 0;
//	List<byte[]> tsuids = new ArrayList<byte[]>(128);
//	@Override
//	public Subscription call(final Set<TSMeta> tsMetas) throws Exception {
//		try {
//			for(final Iterator<TSMeta> titer = tsMetas.iterator(); titer.hasNext();) {
//				tsuids.add(UniqueId.stringToUid(titer.next().getTSUID()));
//				titer.remove();
//			}
//			count += tsMetas.size();
//			log.info("Q: {}", q);
//			
//			if(!q.shouldContinue()) {
//				long elapsed = System.currentTimeMillis() - startTime;
//				log.info("Loaded [{}] TSMetas into Subscription Bloom Filter for pattern [{}] in [{}] ms.", count, _pattern, elapsed);
//				final int bloomFactor = Math.round(DEFAULT_BLOOM_FILTER_SPACE_FACTOR * count);
//				final Subscription poppedSub = new Subscription(_pattern, bloomFactor);
//				for(final Iterator<byte[]> biter = tsuids.iterator(); biter.hasNext();) {
//					poppedSub._internalIndex(biter.next());
//					biter.remove();
//				}
//				return poppedSub;
//			}									
//		} catch (Exception ex) {
//			log.error("Failed to process load callback", ex);
//		}
//		return null;								
//	}
//}, new Callback<Void, Throwable>() {
//	@Override
//	public Void call(Throwable t) throws Exception {
//		log.info("TSMeta Bloom Filter Load Failed for pattern [{}]", _pattern, t);
//		deferredSub.callback(t);
//		return null;
//	}
//}).addCallback(new Callback<Void, Subscription>() {
//	@Override
//	public Void call(final Subscription subscription) throws Exception { 
//		if(subscription==null) return null;
//		Set<Channel> subChannels = subscriptionChannels.get(subscription.getSubscriptionId());
//		if(subChannels==null) {
//			synchronized(subscriptionChannels) {
//				subChannels = subscriptionChannels.get(subscription.getSubscriptionId());
//				if(subChannels==null) {
//					subChannels = new NonBlockingHashSet<Channel>();
//					subscriptionChannels.put(subscription.getSubscriptionId(), subChannels);
//				}
//			}
//		}
//		subChannels.add(channel);
//		subscription.addSubscriber();
//		Set<Subscription> channelSubs = channelSubscriptions.get(channelId);
//		if(channelSubs==null) {
//			synchronized(channelSubscriptions) {
//				channelSubs = channelSubscriptions.get(channelId);
//				if(channelSubs==null) {
//					channelSubs = new NonBlockingHashSet<Subscription>();
//					channelSubscriptions.put(channelId, channelSubs);
//				}
//			}
//		}
//		channelSubs.add(subscription);
//		if(subscribedChannels.add(channel)) {
//			channel.getCloseFuture().addListener(finalSm);
//		}
//		deferredSub.callback(subscription);
//		allSubscriptions.put(_pattern, subscription);								
//		return null;
//	}
//});						
*/