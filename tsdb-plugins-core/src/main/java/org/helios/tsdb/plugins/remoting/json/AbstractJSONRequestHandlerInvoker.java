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
package org.helios.tsdb.plugins.remoting.json;

import org.helios.tsdb.plugins.util.SystemClock;
import org.helios.tsdb.plugins.util.SystemClock.ElapsedTime;

/**
 * <p>Title: AbstractJSONRequestHandlerInvoker</p>
 * <p>Description: An abstract and instrumented wrapper for generated json invokers</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.remoting.json.AbstractJSONRequestHandlerInvoker</code></p>
 */

public abstract class AbstractJSONRequestHandlerInvoker  {
	/** The target service this invoker is invoking against */
	private final Object targetService;
	/** The target service name */
	private final String serviceName;
	/** The target op name */
	private final String opName;
	/** The target service description */
	private final String serviceDescription;
	/** The target op description */
	private final String opDescription;
	/** Indicates if this is a sub */
	private final boolean sub;
	/** The unsub op name if this is a sub */
	private final String unSubOp;
	/** The unsub service name if this is a sub */
	private final String unSubServiceName;
	
	/**
	 * Creates a new AbstractJSONRequestHandlerInvoker
	 * @param targetService The target service this invoker is invoking against
	 * @param serviceName The target service name
	 * @param serviceDescription The target service description
	 * @param opName The target op name
	 * @param opDescription The target op description
	 * @param sub true if this is a sub op
	 * @param unSubOp The unsub op name if this is a sub op
	 * @param unSubServiceName The unsub service name if this is a sub op
	 */
	public AbstractJSONRequestHandlerInvoker(Object targetService, String serviceName, String serviceDescription, String opName, String opDescription, boolean sub, String unSubOp, String unSubServiceName) {
		this.targetService = targetService;		
		this.serviceName = serviceName;
		this.serviceDescription = serviceDescription;
		this.opDescription = opDescription;
		this.opName = opName;
		this.sub = sub; 
		this.unSubOp = unSubOp==null||unSubOp.trim().isEmpty() ? null : unSubOp;
		this.unSubServiceName = unSubServiceName==null||unSubServiceName.trim().isEmpty() ? null : unSubServiceName;
	}
	
	/**
	 * Creates a new AbstractJSONRequestHandlerInvoker
	 * @param targetService The target service this invoker is invoking against
	 * @param serviceName The target service name
	 * @param serviceDescription The target service description
	 * @param opName The target op name
	 * @param opDescription The target op description
	 */
	public AbstractJSONRequestHandlerInvoker(Object targetService, String serviceName, String serviceDescription, String opName, String opDescription) {
		this(targetService, serviceName, serviceDescription, opName, opDescription, false, null, null);
	}
	


	/**
	 * Invokes the passed json request
	 * @param jsonRequest The json request to invoke
	 */
	public void invokeJSONRequest(JSONRequest jsonRequest) {
		ElapsedTime et = SystemClock.startClock();
		try {
			doInvoke(jsonRequest);
			long elpsd = et.elapsed();
		} catch (Exception ex) {
			
			throw new RuntimeException("Failed to invoke JSON Service [" + serviceName + "/" + opName + "]", ex);
		}
	}
	
	/**
	 * The byte-code generated json request invoker
	 * @param jsonRequest the request to invoke
	 */
	public abstract void doInvoke(JSONRequest jsonRequest);

	/**
	 * Returns the target service name
	 * @return the service name
	 */
	public String getServiceName() {
		return serviceName;
	}

	/**
	 * Returns the target operation name 
	 * @return the opName
	 */
	public String getOpName() {
		return opName;
	}

	/**
	 * Returns the target service description
	 * @return the serviceDescription
	 */
	public String getServiceDescription() {
		return serviceDescription;
	}

	/**
	 * Returns the target operation description
	 * @return the opDescription
	 */
	public String getOpDescription() {
		return opDescription;
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return String
				.format("JSONRequestHandlerInvoker [impl=%s, serviceName=%s, serviceDescription=%s, opName=%s, opDescription=%s]",
						targetService.getClass().getSimpleName(), serviceName, serviceDescription, opName, opDescription);
	}

	/**
	 * Returns 
	 * @return the sub
	 */
	public boolean isSub() {
		return sub;
	}

	/**
	 * Returns the unsub op for this sub, or null if this is not a sub
	 * @return the unsub op for this sub, or null if this is not a sub
	 */
	public String getUnSubOp() {
		return unSubOp;
	}

	/**
	 * Returns the unsub service name for this sub, or null if this is not a sub
	 * @return the unsub service name for this sub, or null if this is not a sub
	 */
	public String getUnSubServiceName() {
		return unSubServiceName;
	}
	
	

}
