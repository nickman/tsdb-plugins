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

import java.util.concurrent.TimeUnit;

import org.helios.tsdb.plugins.async.AsyncDispatcherExecutor;

import reactor.event.dispatch.AbstractMultiThreadDispatcher;

/**
 * <p>Title: SuppliedTPEMultiThreadDispatcher</p>
 * <p>Description: Creates a reactor multi-threaded dispatcher using an existing thread pool.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>net.opentsdb.catalog.SuppliedTPEMultiThreadDispatcher</code></p>
 */

public class SuppliedTPEMultiThreadDispatcher extends AbstractMultiThreadDispatcher {
	/** The underlying supplied executor */
	protected final AsyncDispatcherExecutor executor;
	
	/**
	 * Creates a new SuppliedTPEMultiThreadDispatcher
	 * @param executor The underlying supplied executor
	 */
	public SuppliedTPEMultiThreadDispatcher(AsyncDispatcherExecutor executor) {
		super(executor.getMaximumPoolSize(), executor.getQueue().remainingCapacity());
		this.executor = executor;
	}

	/**
	 * {@inheritDoc}
	 * @see reactor.event.dispatch.Dispatcher#awaitAndShutdown(long, java.util.concurrent.TimeUnit)
	 */
	@Override
	public boolean awaitAndShutdown(long timeout, TimeUnit timeUnit) {
		executor.shutdown();
		try {
			if(!executor.awaitTermination(timeout, timeUnit)) {
				return false;
			}
		} catch(InterruptedException e) {
			Thread.currentThread().interrupt();
			return false;
		}
		return true;
	}

	/**
	 * {@inheritDoc}
	 * @see reactor.event.dispatch.AbstractLifecycleDispatcher#execute(reactor.event.dispatch.AbstractLifecycleDispatcher.Task)
	 */
	@Override
	protected void execute(Task task) {
		executor.execute(task);
	}

}
