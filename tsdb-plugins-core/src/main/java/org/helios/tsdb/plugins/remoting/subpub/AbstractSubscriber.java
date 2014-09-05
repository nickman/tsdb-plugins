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

import java.util.List;
import java.util.Set;

import org.cliffc.high_scale_lib.NonBlockingHashSet;
import org.helios.tsdb.plugins.event.TSDBEvent;
import org.helios.tsdb.plugins.event.TSDBEventType;

/**
 * <p>Title: AbstractSubscriber</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.remoting.subpub.AbstractSubscriber</code></p>
 */

public class AbstractSubscriber implements Subscriber {
	/** The bitmask of the event types this subscriber is interested in */
	protected final int eventBitMask;
	
	/** A set of registered listeners  */
	protected final Set<SubscriberEventListener> listeners = new NonBlockingHashSet<SubscriberEventListener>();
	
	
	/**
	 * Creates a new AbstractSubscriber
	 * @param types the event types this subscriber is interested in
	 */
	protected AbstractSubscriber(TSDBEventType...types) {
		eventBitMask = TSDBEventType.getMask(types);
	}
	
	@Override
	public void accept(List<TSDBEvent> events) {
		// TODO Auto-generated method stub
		
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


}
