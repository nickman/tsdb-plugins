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

import org.helios.tsdb.plugins.event.TSDBEvent;
import org.helios.tsdb.plugins.event.TSDBEventType;

/**
 * <p>Title: Subscriber</p>
 * <p>Description: Defines a class that subscribes to TSDBEvents</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.remoting.subpub.Subscriber</code></p>
 */

public interface Subscriber {
	/**
	 * Returns the selective bitmask for the types of events this subscriber is interested in
	 * @return the TSDBEvent selective bitmask
	 * @see  org.helios.tsdb.plugins.event.TSDBEventType#getMask(TSDBEventType...)
	 */
	public int getEventBitMask();
	
	/**
	 * Delivers a TSDBEvent to the subscriber
	 * @param event The event
	 */
	public void accept(TSDBEvent event);
	
	/**
	 * Registers a listener that should be notified of subscriber events
	 * @param listener The listener to register
	 */
	public void registerListener(SubscriberEventListener listener);
	
	/**
	 * Unregisters a listener
	 * @param listener The listener to unregister
	 */
	public void removeListener(SubscriberEventListener listener);
}
