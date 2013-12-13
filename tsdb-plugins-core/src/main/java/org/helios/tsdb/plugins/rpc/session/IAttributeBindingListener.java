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
 * <p>Title: IAttributeBindingListener</p>
 * <p>Description: Defines a listener that will be notified of attributes bound into an {@link IRPCSession}, optionally when the name value pair matches a specified filter.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.rpc.session.IAttributeBindingListener</code></p>
 */

public interface IAttributeBindingListener {
	/**
	 * Callback from an {@link IRPCSession} when an attribute is bound to the session
	 * @param name The name under which the attribute was bound
	 * @param resource The attribute value that was bound
	 */
	public void onAttributeBound(String name, Object resource);
	
	/**
	 * Callback from an {@link IRPCSession} when an attribute is removed from the session
	 * @param name The name under which the removed attribute was bound
	 * @param resource The attribute value that was removed
	 */
	public void onAttributeRemoved(String name, Object resource);

}
