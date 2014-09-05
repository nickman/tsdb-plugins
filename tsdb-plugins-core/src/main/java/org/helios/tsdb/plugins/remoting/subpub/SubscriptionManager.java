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

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.management.MXBean;

import net.opentsdb.core.TSDB;
import net.opentsdb.meta.TSMeta;
import net.opentsdb.meta.api.MetricsMetaAPI;
import net.opentsdb.meta.api.QueryContext;
import net.opentsdb.uid.UniqueId;
import net.opentsdb.utils.JSON;

import org.cliffc.high_scale_lib.NonBlockingHashMap;
import org.cliffc.high_scale_lib.NonBlockingHashSet;
import org.helios.tsdb.plugins.async.SingletonEnvironment;
import org.helios.tsdb.plugins.event.TSDBEventType;
import org.helios.tsdb.plugins.meta.Datapoint;
import org.helios.tsdb.plugins.remoting.json.JSONRequest;
import org.helios.tsdb.plugins.remoting.json.JSONRequestRouter;
import org.helios.tsdb.plugins.remoting.json.JSONResponse;
import org.helios.tsdb.plugins.remoting.json.JSONSubscriber;
import org.helios.tsdb.plugins.remoting.json.annotations.JSONRequestHandler;
import org.helios.tsdb.plugins.remoting.json.annotations.JSONRequestService;
import org.helios.tsdb.plugins.rpc.AbstractRPCService;
import org.helios.tsdb.plugins.service.IPluginContextResourceFilter;
import org.helios.tsdb.plugins.service.IPluginContextResourceListener;
import org.helios.tsdb.plugins.service.PluginContext;

import reactor.core.Reactor;
import reactor.core.composable.Promise;
import reactor.core.composable.spec.Promises;
import reactor.function.Consumer;

/**
 * <p>Title: SubscriptionManager</p>
 * <p>Description: Manages subscriptions on behalf of remote subscribers</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.remoting.subpub.SubscriptionManager</code></p>
 */
@JSONRequestService(name="pubsub", description="Manages subscriptions on behalf of remote subscribers")
@MXBean
public class SubscriptionManager extends AbstractRPCService implements SubscriptionManagerMXBean, SubscriberEventListener {
	
	/** All of the current subscriptions keyed by the pattern */
	protected final NonBlockingHashMap<String, Subscription> allSubscriptions = new NonBlockingHashMap<String, Subscription>(1024); 
	
	/** A map of sets of subscriptions keyed by the subscriber to those subscriptions */
	protected final NonBlockingHashMap<String, Map<Subscriber<Datapoint>, Set<Subscription>>> subscriberSubscriptions = new NonBlockingHashMap<String, Map<Subscriber<Datapoint>, Set<Subscription>>>(128);
	/** The event reactor */
	protected final Reactor reactor;
	/** The metric lookup service to prime subscription members */
	protected MetricsMetaAPI metricSvc = null;	

	
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
		reactor = SingletonEnvironment.getInstance().getDefaultReactor();
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
	 * Subscribes the calling client to a subscription of events matching the passed expression
	 * @param request The JSON request
	 * <p>Invoker:<b><code>sendRemoteRequest('ws://localhost:4243/ws', {svc:'pubsub', op:'sub', {x:'sys*:dc=dc*,host=WebServer1|WebServer5'}});</code></b>
	 */
	@JSONRequestHandler(name="sub", sub=true, unsub="unsub", description="Creates a new subscription on behalf of the calling client")
	public void subscribe(final JSONRequest request) {
		final String expression = request.getRequest().get("x").textValue();
		final String ID = JSONSubscriber.getId(request);
		Map<Subscriber<Datapoint>, Set<Subscription>> subMap = subscriberSubscriptions.get(ID);
		if(subMap==null) {
			synchronized(subscriberSubscriptions) {
				subMap = subscriberSubscriptions.get(ID);
				if(subMap==null) {
					Subscriber<Datapoint> subscriber = new JSONSubscriber<Datapoint>(request, TSDBEventType.DPOINT_DOUBLE, TSDBEventType.DPOINT_LONG);		
					Set<Subscription> set = new NonBlockingHashSet<Subscription>();
					subMap = Collections.unmodifiableMap(Collections.singletonMap(subscriber, set));
					subscriberSubscriptions.put(ID, subMap);
					subscriber.registerListener(this);
				}
			}
		}
		final Subscriber<Datapoint> finalSub = subMap.keySet().iterator().next();
		final Set<Subscription> subSet = subMap.values().iterator().next();
		subscribe(expression).onSuccess(new Consumer<Subscription>(){
			@Override
			public void accept(Subscription t) {				
				subSet.add(t);
				t.addSubscriber(finalSub);
				request.response().setOpCode(JSONResponse.RESP_TYPE_SUB_STARTED).setContent(JSON.getMapper().createObjectNode().put("subId", t.getSubscriptionId())).send();
			}
		}).onError(new Consumer<Throwable>(){
			@Override
			public void accept(Throwable t) {
				request.error("Failed to initiate subscription for [" + expression + "]", t);
			}
		});
	}
	

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.remoting.subpub.SubscriberEventListener#onDisconnect(org.helios.tsdb.plugins.remoting.subpub.Subscriber)
	 */
	@Override
	public void onDisconnect(Subscriber subscriber) {
		
		
	}	
	
	
	
	private <T> reactor.core.composable.Deferred<T, Promise<T>> getDeferred() {
		return Promises.<T>defer()				
				  .get();		
	}
	
	
	/**
	 * Acquires an existing or creates a new subscription for the passed pattern
	 * @param pattern The subscription pattern
	 * @return a promise of the created subscription
	 */
	protected Promise<Subscription> subscribe(final CharSequence pattern) {		
		log.info("Initiating Subscription for pattern [{}]", pattern);
		if(pattern==null) throw new IllegalArgumentException("The passed pattern was null");
		if(metricSvc==null) throw new IllegalStateException("The SubscriptionManager has not been initialized", new Throwable());
		final String _pattern = pattern.toString().trim();
		if(_pattern.isEmpty()) throw new IllegalArgumentException("The passed pattern was empty");
		final reactor.core.composable.Deferred<Subscription, Promise<Subscription>> def = getDeferred();
		final reactor.core.composable.Deferred<Long, Promise<Long>> defCount = getDeferred();
		final Promise<Subscription> promise = def.compose();
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
									final AtomicBoolean done = new AtomicBoolean(false);
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
										if(!q.shouldContinue() && done.compareAndSet(false, true)) {
											final long elapsedTime = System.currentTimeMillis()-startTime;
											log.info("Retrieved [{}] TSMetas in [{}] ms. to prime subscription [{}]", matchingTsuids.size(), elapsedTime, pattern);
											final int bloomFactor = Math.round(DEFAULT_BLOOM_FILTER_SPACE_FACTOR * matchingTsuids.size());
											final Subscription subx = new Subscription(reactor, metricSvc, _pattern, bloomFactor);
											int indexCnt = 0;
											for(final Iterator<byte[]> biter = matchingTsuids.iterator(); biter.hasNext();) {
												subx._internalIndex(biter.next());
												indexCnt++;
												biter.remove();
											}
											log.info("Created and initialized [{}] items in Subscription BloomFilter for [{}]", indexCnt, pattern);
											allSubscriptions.put(pattern.toString(), subx);
											def.accept(subx);
										}
									}
								});
							} catch (Exception ex) {
								def.accept(new Exception("Timed out on populating subscription for expression [" + pattern + "]"));
							}
						}  else {// subx not null
							def.accept(subx);
							return;							
						}
					} // synchr
				} else {// subx not null
					def.accept(subx);
					return;					
				}
			} // end of run
		}); // end of runnable
		return promise;
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.remoting.subpub.SubscriptionManagerMXBean#getSubscriptionCount()
	 */
	@Override
	public int getSubscriptionCount() {
		return allSubscriptions.size();
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.remoting.subpub.SubscriptionManagerMXBean#getSubscriberCount()
	 */
	@Override
	public int getSubscriberCount() {
		return subscriberSubscriptions.size();
	}

	


}




