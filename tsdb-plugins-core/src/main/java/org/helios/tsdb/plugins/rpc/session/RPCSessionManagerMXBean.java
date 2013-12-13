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

import java.util.Map;
import java.util.Set;

/**
 * <p>Title: RPCSessionManagerMXBean</p>
 * <p>Description: MX interface for the {@link RPCSessionManager}</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.rpc.session.RPCSessionManagerMXBean</code></p>
 */

public interface RPCSessionManagerMXBean {
	/**
	 * Returns the number of active registered sessions
	 * @return the number of active registered sessions
	 */
	public int getSessionCount();
	/**
	 * Returns the number of registered session listeners
	 * @return the number of registered session listeners
	 */
	public int getSessionListenerCount();
	/**
	 * Returns the number of registered attribute binding listeners
	 * @return the number of registered attribute binding listeners
	 */
	public int getAttributeListenerCount();
	
	/**
	 * Returns a set of the registered sessions
	 * @return a set of the registered sessions
	 */
	public Set<IRPCSessionMBean> getSessions();
	
	/**
	 * Returns a map of the transport session factories registered keyed by the name of the class they support
	 * @return a map of the transport session factories registered keyed by the name of the class they support
	 */
	public Map<String, String> getTransportSessionFactories();
}
