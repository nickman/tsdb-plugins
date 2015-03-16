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
package test.com.heliosapm.tsdb.plugins.async;

import com.heliosapm.tsdb.plugins.async.TSDBEvent;
import com.heliosapm.tsdb.plugins.async.TSDBEventConsumer;
import com.heliosapm.tsdb.plugins.async.TSDBEventType;
import com.stumbleupon.async.Deferred;

/**
 * <p>Title: NoopSearchEventConsumer</p>
 * <p>Description: No Op TSDBEvent Consumer for Search Events.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>test.com.heliosapm.tsdb.plugins.async.NoopSearchEventConsumer</code></p>
 */

public class NoopSearchEventConsumer implements TSDBEventConsumer {

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.tsdb.plugins.async.TSDBEventConsumer#getSubscribedEventMask()
	 */
	@Override
	public int getSubscribedEventMask() {
		return TSDBEventType.SEARCH_EVENT_MASK;
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.tsdb.plugins.async.TSDBEventConsumer#onEvent(com.heliosapm.tsdb.plugins.async.TSDBEvent)
	 */
	@Override
	public Deferred<?> onEvent(final TSDBEvent event) {
		return Deferred.fromResult(null);
	}

}
