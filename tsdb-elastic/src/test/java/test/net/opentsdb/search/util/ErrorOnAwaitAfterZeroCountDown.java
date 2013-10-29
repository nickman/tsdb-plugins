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
package test.net.opentsdb.search.util;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * <p>Title: ErrorOnAwaitAfterZeroCountDown</p>
 * <p>Description: An extension of {@link CountDownLatch} that throws an exception if the count was already zero when {@link CountDownLatch#await()} or {@link CountDownLatch#await(long, TimeUnit)} succeeds.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>test.net.opentsdb.search.util.ErrorOnAwaitAfterZeroCountDown</code></p>
 */

public class ErrorOnAwaitAfterZeroCountDown extends CountDownLatch {
	
	/**
	 * Creates a new ErrorOnAwaitAfterZeroCountDown with a count of one
	 */
	public ErrorOnAwaitAfterZeroCountDown() {
		this(1);
	}

	/**
	 * Creates a new ErrorOnAwaitAfterZeroCountDown
	 * @param count the count
	 */
	public ErrorOnAwaitAfterZeroCountDown(int count) {
		super(count);
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.util.concurrent.CountDownLatch#await()
	 */
	@Override
	public void await() throws InterruptedException {		
		if(getCount()==0) throw new RuntimeException("Last awaiter exception");
		super.await();
		
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.util.concurrent.CountDownLatch#await(long, java.util.concurrent.TimeUnit)
	 */
	@Override
	public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
		if(getCount()==0) throw new RuntimeException("Last awaiter exception");
		return super.await(timeout, unit);
	}

}
