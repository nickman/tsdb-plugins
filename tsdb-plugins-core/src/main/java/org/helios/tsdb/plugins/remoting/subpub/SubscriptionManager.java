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

import java.util.Map;
import java.util.Properties;
import java.util.Set;

import net.opentsdb.core.TSDB;
import net.opentsdb.meta.api.MetricsMetaAPI;
import net.opentsdb.utils.JSON;

import org.cliffc.high_scale_lib.NonBlockingHashMap;
import org.cliffc.high_scale_lib.NonBlockingHashMapLong;
import org.helios.tsdb.plugins.rpc.AbstractRPCService;
import org.helios.tsdb.plugins.service.IPluginContextResourceFilter;
import org.helios.tsdb.plugins.service.IPluginContextResourceListener;
import org.helios.tsdb.plugins.service.PluginContext;
import org.helios.tsdb.plugins.util.JMXHelper;
import org.jboss.netty.channel.Channel;
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

public class SubscriptionManager extends AbstractRPCService {
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
		ctx.addResourceListener(new IPluginContextResourceListener() {
			public void onResourceRegistered(String name, Object resource) {
				metricSvc = (MetricsMetaAPI)resource;
				ctx.setResource(getClass().getSimpleName(), this);
			}
		}, new IPluginContextResourceFilter() {
			public boolean include(String name, Object resource) {				
				return (resource!=null && (resource instanceof MetricsMetaAPI));
			}
		});
	}
	
	public static void buildAndSend(final long subscriptionId, final String metric, final long timestamp, final double value, final Map<String,String> tags, final byte[] tsuid, final Set<Channel> channels) {
		final ObjectNode on = JSON.getMapper().createObjectNode();
		on.put("metric", metric);
		on.put("sub", subscriptionId);
		on.put("ts", timestamp);
		on.put("value", value);
		on.put("type", "d");
		
		final ObjectNode tagMap = JSON.getMapper().createObjectNode();
		for(Map.Entry<String, String> e: tags.entrySet()) {
			tagMap.put(e.getKey(), e.getValue());
		}
		on.put("tags", tagMap);
		
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
		try {
			final CharSequence cs = JMXHelper.objectName(metric, tags).toString();
			for(Subscription s: allSubscriptions.values()) {
				if(s.isMemberOf(cs)) {
					Set<Channel> channels = subscriptionChannels.get(s.getSubscriptionId());
					if(channels!=null && !channels.isEmpty()) {
						for(Channel ch: channels) {
							ch.write(message)
						}
					}
				}
			}
			
		} catch (Exception ex) {
			
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
		
	}
	


}
