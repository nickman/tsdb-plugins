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
 * <p>Title: IRPCSession</p>
 * <p>Description: Defines an RPC session for retaining state on behalf of remote RPC clients</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.rpc.session.IRPCSession</code></p>
 */

public interface IRPCSession {
	/**
	 * Expires this session 
	 */
	public void expire();
	
	/**
	 * Returns this session's unique id
	 * @return this session's unique id
	 */
	public String getSessionId();
	
	/**
	 * Touches the session, resetting the time to expire
	 */
	public void touch();
	
	/**
	 * Returns the named untyped attribute from session state 
	 * @param name The name of the attribute
	 * @return the attribute value or null if not found
	 */
	public Object getAttribute(String name);
	
	/**
	 * Returns the named typed attribute from session state 
	 * @param name The name of the attribute
	 * @param type The expected type of the attribute
	 * @return the attribute value or null if not found
	 */
	public <T> T getAttribute(String name, Class<T> type);
	
	/**
	 * Sets a session attribute
	 * @param name The name of the attribute
	 * @param value The attribute value
	 * @return The value previously bound to the name, or null if the name was not bound
	 */
	public <T> T setAttribute(String name, T value);

	/**
	 * Clears a session attribute
	 * @param name The name of the attribute
	 * @return The value previously bound to the name, or null if the name was not bound
	 */
	public <T> T clearAttribute(String name);
	
	/**
	 * Returns the bound attribute names for this session
	 * @return an array of attribute names
	 */
	public String[] getAttributeNames();
	
	/**
	 * Returns the long UTC time of when this session was created
	 * @return a long UTC timestamp
	 */
	public long getCreationTime();
	
	/**
	 * Returns the last time the client sent a request associated with this session
	 * @return The last access time as a long UTC
	 */
	public long getLastAccessedTime();
	
	/**
	 * Returns the number of seconds until this session expires without access from the client
	 * @return seconds until expiry
	 */
	public int getTimeToExpiry();
	
	/**
	 * Returns the maximum time interval, in seconds, that this session will kept alive 
	 * without a request from the associated client before being expired.
	 * @return a time period in seconds
	 */
	public int getMaxInactiveInterval();
	
	
	
}
