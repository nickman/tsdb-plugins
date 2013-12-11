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

import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import net.opentsdb.core.TSDB;
import net.opentsdb.utils.Config;

/**
 * <p>Title: PluginContextImpl</p>
 * <p>Description: A common context shared among all handlers for distributing resources, internal events and configuration.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.service.PluginContextImpl</code></p>
 */

public class PluginContextImpl implements PluginContext {
	/** The parent TSDB instance */
	protected final TSDB tsdb;
	/** The parent TSDB instance config */
	protected final Config config;
	/** The extracted TSDB instance config properties */
	protected final Properties extracted;
	/** The plugin support classloader */
	protected final ClassLoader supportClassLoader;
	/** Miscellaneous named resources set for sharing across plugins */
	protected final Map<String, Object> namedResources = new ConcurrentHashMap<String, Object>();
	/** The registered resource listeners */
	protected final Set<IPluginContextResourceListener> listeners = new CopyOnWriteArraySet<IPluginContextResourceListener>();
	
	/**
	 * Creates a new PluginContextImpl
	 * @param tsdb The parent TSDB instance
	 * @param config The parent TSDB instance config
	 * @param extracted The extracted TSDB instance config properties
	 * @param supportClassLoader The plugin support classloader
	 */
	PluginContextImpl(TSDB tsdb, Config config, Properties extracted, ClassLoader supportClassLoader) {
		this.tsdb = tsdb;
		this.config = config;
		this.extracted = extracted;
		this.supportClassLoader = supportClassLoader;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.service.PluginContext#getTsdb()
	 */
	@Override
	public TSDB getTsdb() {
		return tsdb;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.service.PluginContext#getConfig()
	 */
	@Override
	public Config getConfig() {
		return config;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.service.PluginContext#getExtracted()
	 */
	@Override
	public Properties getExtracted() {
		return extracted;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.service.PluginContext#getSupportClassLoader()
	 */
	@Override
	public ClassLoader getSupportClassLoader() {
		return supportClassLoader;
	}


	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.service.PluginContext#getResource(java.lang.String, java.lang.Class)
	 */
	@Override
	public <T> T getResource(String name, Class<?> type) {
		return (T)namedResources.get(name);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.service.PluginContext#setResource(java.lang.String, java.lang.Object)
	 */
	@Override
	public void setResource(String name, Object value) {
		if(name==null) throw new IllegalArgumentException("The passed name was null");
		if(value==null) throw new IllegalArgumentException("The passed value was null");
		namedResources.put(name, value);
		for(IPluginContextResourceListener listener: listeners) {
			listener.onResourceRegistered(name, value);
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.service.PluginContext#addResourceListener(org.helios.tsdb.plugins.service.IPluginContextResourceListener, org.helios.tsdb.plugins.service.IPluginContextResourceFilter)
	 */
	@Override
	public void addResourceListener(final IPluginContextResourceListener listener, final IPluginContextResourceFilter filter) {
		if(listener!=null) {
			IPluginContextResourceListener _listener = filter==null ? listener : new IPluginContextResourceListener() {
				@Override
				public void onResourceRegistered(String name, Object resource) {
					if(filter.include(name, resource)) {
						listener.onResourceRegistered(name, resource);
					}						
				}
				@Override
				public boolean equals(Object obj) {						
					return listener.equals(obj);
				}
				@Override
				public int hashCode() {
					return listener.hashCode();
				}
			}; 			
			for(Map.Entry<String, Object> entry: namedResources.entrySet()) {
				_listener.onResourceRegistered(entry.getKey(), entry.getValue());
			}
			listeners.add(_listener);
		}		
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.service.PluginContext#addResourceListener(org.helios.tsdb.plugins.service.IPluginContextResourceListener)
	 */
	@Override
	public void addResourceListener(IPluginContextResourceListener listener) {
		addResourceListener(listener, null);
	}
	

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.service.PluginContext#removeResourceListener(org.helios.tsdb.plugins.service.IPluginContextResourceListener)
	 */
	@Override
	public void removeResourceListener(IPluginContextResourceListener listener) {
		if(listener!=null) {
			if(!listeners.remove(listener)) {
				for(Iterator<IPluginContextResourceListener> iter = listeners.iterator(); iter.hasNext();) {
					IPluginContextResourceListener _listener = iter.next();
					if(_listener.equals(listener) && _listener.hashCode()==listener.hashCode()) {
						iter.remove();
						break;
					}
				}
			}
		}
	}

}
