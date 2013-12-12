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
package org.helios.tsdb.plugins.rpc.session;

/**
 * <p>Title: ISessionLifecycle</p>
 * <p>Description: Defines the lifecycle triggers for an RPC session that retains state on behalf of a remote client.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.rpc.session.ISessionLifecycle</code></p>
 */

public interface ISessionLifecycle {
	/**
	 * Returns this session's unique id
	 * @return this session's unique id
	 */
	public String getSessionId();
	
	/**
	 * Fired when the session expires
	 */
	public void onSessionExpire();
	
	/**
	 * Indicates if the session is expired
	 * @return true if the session is expired, false otherwise
	 */
	public boolean isExpired();

}
