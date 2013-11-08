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
package net.opentsdb.catalog;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.helios.tsdb.plugins.event.TSDBSearchEvent;

/**
 * <p>Title: BatchMileStone</p>
 * <p>Description: Contrived search event that sorts high in the priority queue and counts down a latch when processed.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>net.opentsdb.catalog.BatchMileStone</code></p>
 */

public class BatchMileStone extends TSDBSearchEvent implements Comparable<BatchMileStone> {
	/** The latch that is counted down when processed */
	protected final CountDownLatch latch;
	/** The timestamp of the event creation */
	protected final long created = System.nanoTime();
	
	/**
	 * Creates a new BatchMileStone
	 */
	public BatchMileStone() {
		this(1);
	}
	
	/**
	 * Creates a new BatchMileStone
	 * @param count The latch count
	 */
	public BatchMileStone(int count) {
		latch = new CountDownLatch(count);
	}
	

	/**
	 * Waits for the latch to be dropped or the thread is interrupted
	 * @see java.util.concurrent.CountDownLatch#await()
	 */
	public void await() {
		try {
			latch.await();
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	/**
	 * Waits for the latch to be dropped or the thread is interrupted, or the specified timeout elapsed
	 * @param timeout The timeout
	 * @param unit The timeout unit
	 * @return true if the latch was dropped before the timeout, false otherwise
	 * @see java.util.concurrent.CountDownLatch#await(long, java.util.concurrent.TimeUnit)
	 */
	public boolean await(long timeout, TimeUnit unit) {
		try {
			return latch.await(timeout, unit);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	/**
	 * Counts down the latch 
	 * @see java.util.concurrent.CountDownLatch#countDown()
	 */
	public void countDown() {
		latch.countDown();
	}

	/**
	 * Returns the current count
	 * @return the current count
	 * @see java.util.concurrent.CountDownLatch#getCount()
	 */
	public long getCount() {
		return latch.getCount();
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(BatchMileStone other) {
		if(created < other.created) return -1;
		return 1;
	}
	
	
	

}
