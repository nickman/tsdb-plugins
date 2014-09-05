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
package org.helios.tsdb.plugins.remoting.subpub;

import java.util.Collection;
import java.util.Set;

import org.cliffc.high_scale_lib.NonBlockingHashSet;
import org.helios.tsdb.plugins.event.TSDBEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Title: AbstractSubscriber</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.remoting.subpub.AbstractSubscriber</code></p>
 * @param <T> The types of events consumed by this subscriber
 */

public abstract class AbstractSubscriber<T> implements Subscriber<T> {
	/** The bitmask of the event types this subscriber is interested in */
	protected final int eventBitMask;
	/** The unique subscriber id */
	protected final String id;
	/** The subscriber logger */
	protected final Logger log;

	
	/** A set of registered listeners  */
	protected final Set<SubscriberEventListener> listeners = new NonBlockingHashSet<SubscriberEventListener>();
	
	
	/**
	 * Creates a new AbstractSubscriber
	 * @param id The subscriber id
	 * @param types the event types this subscriber is interested in
	 */
	protected AbstractSubscriber(final String id, final TSDBEventType...types) {
		this.id = id;
		eventBitMask = TSDBEventType.getMask(types);
		log = LoggerFactory.getLogger(getClass().getName() + "." + id);
	}
	
	@Override
	public abstract void accept(Collection<T> events);
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.remoting.subpub.Subscriber#getSubscriberId()
	 */
	@Override
	public String getSubscriberId() {
		return id;
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.remoting.subpub.Subscriber#getEventBitMask()
	 */
	@Override
	public int getEventBitMask() {
		return eventBitMask;
	}



	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.remoting.subpub.Subscriber#registerListener(org.helios.tsdb.plugins.remoting.subpub.SubscriberEventListener)
	 */
	@Override
	public void registerListener(SubscriberEventListener listener) {
		if(listener!=null) {
			listeners.add(listener);
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.remoting.subpub.Subscriber#removeListener(org.helios.tsdb.plugins.remoting.subpub.SubscriberEventListener)
	 */
	@Override
	public void removeListener(SubscriberEventListener listener) {
		if(listener!=null) {
			listeners.remove(listener);
		}		
	}
	
	
	/**
	 * Fires a disconnect event against all registered listeners
	 */
	protected void fireDisconnected() {
		for(SubscriberEventListener l: listeners) {
			l.onDisconnect(this);
		}
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
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
		if (!(obj instanceof AbstractSubscriber))
			return false;
		AbstractSubscriber other = (AbstractSubscriber) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Subscriber [");
		builder.append("id:").append(id).append(", ")
		.append("type:").append(getClass().getSimpleName());		
		builder.append("]");
		return builder.toString();
	}


}
