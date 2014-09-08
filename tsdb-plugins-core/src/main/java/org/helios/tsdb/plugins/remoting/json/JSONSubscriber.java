/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2014, Helios Development Group and individual contributors
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
package org.helios.tsdb.plugins.remoting.json;

import java.util.Collection;

import org.helios.tsdb.plugins.event.TSDBEventType;
import org.helios.tsdb.plugins.remoting.subpub.AbstractSubscriber;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * <p>Title: JSONSubscriber</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.remoting.json.JSONSubscriber</code></p>
 * @param <T> The assumed type of the object types that will be delivered to this subscriber
 */

public class JSONSubscriber<T> extends AbstractSubscriber<T> implements ChannelFutureListener {
	/** The JSONRequest initiating the subscription */
	protected final JSONRequest request;
	
	
	/**
	 * Creates a new JSONSubscriber
	 * @param request The JSONRequest initiating the subscription
	 * @param types The event types we're interested in
	 */
	public JSONSubscriber(final JSONRequest request, final TSDBEventType... types) {
		super(JSONSubscriber.class.getSimpleName() + ":" + request.channel.getId(), types);
		this.request = request;
		this.request.channel.getCloseFuture().addListener(this);
	}

	/**
	 * Determines what the Subscriber id would be (or is) for the passed JSONRequest
	 * @param request The request to get the Subscriber id for
	 * @return the Subscriber id
	 */
	public static String getId(final JSONRequest request) {
		return JSONSubscriber.class.getSimpleName() + ":" + request.channel.getId();
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.remoting.subpub.AbstractSubscriber#accept(java.util.Collection)
	 */
	@Override
	public void accept(Collection<T> events) {		
		final JSONResponse response = request.response(ResponseType.SUB);
		try {
			JsonGenerator jgen = response.writeHeader(true);
			jgen.writeFieldName("msg");
			jgen.writeStartArray();
			for(T t: events) {
				jgen.writeObject(t);
			}
			jgen.writeEndArray();
			response.closeGenerator();
		} catch (Exception ex) {
			log.error("Failed to write out accepted events", ex);
		}
	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.ChannelFutureListener#operationComplete(org.jboss.netty.channel.ChannelFuture)
	 */
	@Override
	public void operationComplete(ChannelFuture future) throws Exception {
		fireDisconnected();
	}

}
