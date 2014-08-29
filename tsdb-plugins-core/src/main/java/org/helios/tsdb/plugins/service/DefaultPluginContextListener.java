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
package org.helios.tsdb.plugins.service;

import java.util.Arrays;

import net.opentsdb.meta.api.MetricsMetaAPI;

import org.helios.tsdb.plugins.remoting.subpub.SubscriptionManager;

/**
 * <p>Title: DefaultPluginContextListener</p>
 * <p>Description: Convenience listener for listening on a specific resource to be bound into the context</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.service.DefaultPluginContextListener</code></p>
 */

public class DefaultPluginContextListener implements IPluginContextResourceFilter, IPluginContextResourceListener, IPluginContextPendingRegistration {
	/** The type of the resource to listen for */
	private final Class<?> resourceType; 
	/** The names that the bound resource must match one of */
	private final String[] resourceNames;
	/** The resource to register */
	private Object resource = null;
	/** The resource binding name */
	private String bindingName = null;
	
	/**
	 * Creates a new DefaultPluginContextListener
	 * @param resourceType The type of the resource to listen for
	 * @param resourceNames The names that the bound resource must match one of
	 * @return the listener
	 */
	public static DefaultPluginContextListener newInstance(Class<?> resourceType, String...resourceNames) {
		return new DefaultPluginContextListener(resourceType, resourceNames);
	}
	
	/**
	 * Creates a new DefaultPluginContextListener
	 * @param resourceType The type of the resource to listen for
	 * @param resourceNames The names that the bound resource must match one of
	 */
	private DefaultPluginContextListener(Class<?> resourceType, String...resourceNames) {
		this.resourceType = resourceType;
		this.resourceNames = resourceNames;
	}
	
	
	/**
	 * Indicates that the passed resource should be registered in the plugin context when the listener is fired
	 * @param resource The resource to register
	 * @param bindingName The name to register with
	 * @return this listener
	 */
	public DefaultPluginContextListener registerOnHit(Object resource, String bindingName) {
		
		return this;
	}
	
//	ctx.addResourceListener(new IPluginContextResourceListener() {
//		public void onResourceRegistered(String name, Object resource) {
//			metricSvc = (MetricsMetaAPI)resource;
//			ctx.setResource(SubscriptionManager.class.getSimpleName(), this);
//		}
//	}, new IPluginContextResourceFilter() {
//		public boolean include(String name, Object resource) {				
//			return (resource!=null && (resource instanceof MetricsMetaAPI));
//		}
//	});


	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.service.IPluginContextResourceListener#onResourceRegistered(java.lang.String, java.lang.Object)
	 */
	@Override
	public void onResourceRegistered(String name, Object resource) {
		// TODO Auto-generated method stub

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.service.IPluginContextResourceFilter#include(java.lang.String, java.lang.Object)
	 */
	@Override
	public boolean include(String name, Object resource) {
		int filterSpec = 0;
		int comps = 0;
		if(resource!=null && resourceType!=null) {
			filterSpec += (resourceType.isInstance(resource) ? 1 : 0);
			comps++;
		} 
		if(name!=null && resourceNames!=null && resourceNames.length > 0) {
			filterSpec += (Arrays.binarySearch(resourceNames, name) >= 0 ? 1 : 0);
			comps++;
		}
		if(comps==0) return false;
		return (comps==1 && filterSpec==1) | (comps==2 && filterSpec==2); 
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.service.IPluginContextPendingRegistration#getResource()
	 */
	@Override
	public Object getResource() {
		return resource;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.service.IPluginContextPendingRegistration#getBindingName()
	 */
	@Override
	public String getBindingName() {
		return bindingName;
	}

}
