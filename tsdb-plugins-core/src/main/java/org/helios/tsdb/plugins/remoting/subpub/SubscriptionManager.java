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
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import net.opentsdb.core.TSDB;
import net.opentsdb.meta.api.MetricsMetaAPI;
import net.opentsdb.utils.JSON;

import org.cliffc.high_scale_lib.NonBlockingHashMap;
import org.cliffc.high_scale_lib.NonBlockingHashMapLong;
import org.cliffc.high_scale_lib.NonBlockingHashSet;
import org.helios.tsdb.plugins.remoting.json.JSONRequest;
import org.helios.tsdb.plugins.remoting.json.annotations.JSONRequestService;
import org.helios.tsdb.plugins.rpc.AbstractRPCService;
import org.helios.tsdb.plugins.service.IPluginContextResourceFilter;
import org.helios.tsdb.plugins.service.IPluginContextResourceListener;
import org.helios.tsdb.plugins.service.PluginContext;
import org.helios.tsdb.plugins.util.JMXHelper;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * <p>Title: SubscriptionManager</p>
 * <p>Description: Manages subscriptions on behalf of remote subscribers</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.remoting.subpub.SubscriptionManager</code></p>
 */
@JSONRequestService(name="pubsub", description="Manages subscriptions on behalf of remote subscribers")
public class SubscriptionManager extends AbstractRPCService implements ChannelFutureListener, SubscriptionManagerMXBean {
	/** A channel group of channels with subscriptions */
	protected final ChannelGroup subscribedChannels = new DefaultChannelGroup(getClass().getSimpleName());
	
	/** All of the current subscriptions keyed by the pattern */
	protected final NonBlockingHashMap<String, Subscription> allSubscriptions = new NonBlockingHashMap<String, Subscription>(1024); 
	
	/** A map of sets of subscriptions keyed by the id of the channel they were initiated by */
	protected final NonBlockingHashMapLong<Set<Subscription>> channelSubscriptions = new NonBlockingHashMapLong<Set<Subscription>>(128);
	/** A map of sets of channels subscribed to the same subscription keyed by the id of the subscription */
	protected final NonBlockingHashMapLong<Set<Channel>> subscriptionChannels = new NonBlockingHashMapLong<Set<Channel>>(128); 
	
	
	/** The metric lookup service to prime subscription members */
	protected MetricsMetaAPI metricSvc = null;
	
	/**
	 * Creates a new SubscriptionManager
	 * @param tsdb The parent TSDB instance
	 * @param config The extracted configuration
	 */
	public SubscriptionManager(TSDB tsdb, Properties config) {
		super(tsdb, config);
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
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.remoting.subpub.SubscriptionManagerMXBean#getSubscriptions()
	 */
	@Override
	public SubscriptionMBean[] getSubscriptions() {
		final Set<SubscriptionMBean> copy = new HashSet<SubscriptionMBean>(allSubscriptions.values());		
		return copy.toArray(new SubscriptionMBean[copy.size()]);
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
					allSubscriptions.remove(s);
					subscriptionChannels.remove(s.getSubscriptionId());
					if(!subChannels.isEmpty()) {
						log.info("Subscription [{}] indicated zero subscribers, but [{}] channels remain subscribed", s, subChannels.size());
						// notify the orphan channels
						subChannels.clear();
					} 
				}
			}
		}		
	}
	
	@JSON
	public void subscribe(JSONRequest request, String expression) {
		
	}
	
	/**
	 * Subscribes the passed channel to a subscription for the passed channel
	 * @param channel The channel to subscribe
	 * @param pattern The pattern of the subscription
	 */
	protected void subscribe(final Channel channel, final CharSequence pattern) {
		if(channel==null) throw new IllegalArgumentException("The passed channel was null");
		if(pattern==null) throw new IllegalArgumentException("The passed pattern was null");
		if(metricSvc==null) throw new IllegalStateException("The SubscriptionManager has not been initialized", new Throwable());
		final String _pattern = pattern.toString().trim();
		if(_pattern.isEmpty()) throw new IllegalArgumentException("The passed pattern was empty");
		final long channelId = channel.getId().longValue();
		Subscription sub = allSubscriptions.get(_pattern);
		if(sub==null) {
			synchronized(allSubscriptions) {
				sub = allSubscriptions.get(_pattern);
				if(sub==null) {
					sub = new Subscription(_pattern);
					allSubscriptions.put(_pattern, sub);
				}
			}
		}
		Set<Channel> subChannels = subscriptionChannels.get(sub.getSubscriptionId());
		if(subChannels==null) {
			synchronized(subscriptionChannels) {
				subChannels = subscriptionChannels.get(sub.getSubscriptionId());
				if(subChannels==null) {
					subChannels = new NonBlockingHashSet<Channel>();
					subscriptionChannels.put(sub.getSubscriptionId(), subChannels);
				}
			}
		}
		subChannels.add(channel);
		
		
		sub.addSubscriber();
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
		channelSubs.add(sub);
		if(subscribedChannels.add(channel)) {
			channel.getCloseFuture().addListener(this);
		}
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
	 * @param tags Tagk/v pairs
	 * @param tsuid The timeseries uid
	 */
	public void onDataPoint(final String metric, final long timestamp, final double value, final Map<String,String> tags, final byte[] tsuid) {
		if(allSubscriptions.isEmpty()) return;
		try {
			ObjectNode node = null;
			final CharSequence cs = JMXHelper.objectName(metric, tags).toString();
			for(Subscription s: allSubscriptions.values()) {
				if(s.isMemberOf(cs)) {
					Set<Channel> channels = subscriptionChannels.get(s.getSubscriptionId());
					if(channels!=null && !channels.isEmpty()) {						
						if(node==null) node = build(metric, timestamp, value, tags, tsuid);
						node.put("subid", s.getSubscriptionId());
						for(Channel ch: channels) {
							ch.write(node);
						}
					}
				}
			}			
		} catch (Exception ex) {
			log.error("Failed to process double data point", ex);
		}
	}
	
	/**
	 * Evaluates the incoming datapoint and tests each subscription to see if it is a member, then pushes the datapoint
	 * to the channels that have matching subscriptions 
	 * @param metric The name of the metric associated with the data point
	 * @param timestamp Timestamp as a Unix epoch in seconds or milliseconds (depending on the TSD's configuration)
	 * @param value Value for the data point
	 * @param tags Tagk/v pairs
	 * @param tsuid The timeseries uid
	 */
	public void onDataPoint(final String metric, final long timestamp, final long value, final Map<String,String> tags, final byte[] tsuid) {
		if(allSubscriptions.isEmpty()) return;
		try {
			ObjectNode node = null;
			final CharSequence cs = JMXHelper.objectName(metric, tags).toString();
			for(Subscription s: allSubscriptions.values()) {
				if(s.isMemberOf(cs)) {
					Set<Channel> channels = subscriptionChannels.get(s.getSubscriptionId());
					if(channels!=null && !channels.isEmpty()) {
						if(node==null) node = build(metric, timestamp, value, tags, tsuid);
						node.put("subid", s.getSubscriptionId());
						for(Channel ch: channels) {
							ch.write(node);
						}
					}
				}
			}			
		} catch (Exception ex) {
			log.error("Failed to process long data point", ex);
		}		
	}


	


}
