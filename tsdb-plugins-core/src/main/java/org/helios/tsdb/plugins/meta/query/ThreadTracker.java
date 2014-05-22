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
package org.helios.tsdb.plugins.meta.query;

import java.util.HashSet;
import java.util.Set;

import org.cliffc.high_scale_lib.NonBlockingHashMapLong;

/**
 * <p>Title: ThreadTracker</p>
 * <p>Description: Groups threads waiting on the same queue for tracking and group interrupt</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.meta.query.ThreadTracker</code></p>
 */

public class ThreadTracker {
	
	/** A map of threads keyed by thread id */
	protected final NonBlockingHashMapLong<Thread> threads = new NonBlockingHashMapLong<Thread>(true); 
	
	/**
	 * Removes and interrupts all threads in the tracker
	 */
	public void interrupt() {
		Set<Thread> _threads = new HashSet<Thread>(threads.values());
		threads.clear();
		for(Thread t: _threads) {
			t.interrupt();
		}		
	}

	/**
	 * Adds a thread to the tracker
	 * @param thread the thread to add
	 */
	public void add(Thread thread) {
		if(thread==null) throw new IllegalArgumentException("Passed thread was null");
		threads.put(thread.getId(), thread);
	}
	
	/**
	 * Adds the calling thread to the tracker
	 */
	public void add() {
		add(Thread.currentThread());
	}
	
	/**
	 * Removes a thread from the tracker
	 * @param thread the thread to remove
	 */
	public void remove(Thread thread) {
		if(thread==null) throw new IllegalArgumentException("Passed thread was null");
		threads.remove(thread.getId());
	}
	
	/**
	 * Removes the calling thread from the tracker
	 */
	public void remove() {
		remove(Thread.currentThread());
	}
	
	/**
	 * Indicates if there are any threads in this tracker
	 * @return true if there are any threads in this tracker, false if it is empty
	 */
	public boolean has() {
		return !threads.isEmpty();
	}
	
	/**
	 * Returns the number of threads in this tracker
	 * @return the number of threads in this tracker
	 */
	public int count() {
		return threads.size();
	}

}
