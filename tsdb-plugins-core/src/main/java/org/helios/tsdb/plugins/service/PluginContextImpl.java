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

import java.net.URLClassLoader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.ListenerNotFoundException;
import javax.management.MBeanNotificationInfo;
import javax.management.Notification;
import javax.management.NotificationBroadcaster;
import javax.management.NotificationBroadcasterSupport;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.loading.MLet;

import net.opentsdb.core.TSDB;
import net.opentsdb.utils.Config;

import org.helios.tsdb.plugins.async.AsyncDispatcherExecutor;
import org.helios.tsdb.plugins.util.JMXHelper;
import org.helios.tsdb.plugins.util.SystemClock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Title: PluginContextImpl</p>
 * <p>Description: A common context shared among all handlers for distributing resources, internal events and configuration.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.service.PluginContextImpl</code></p>
 */

public class PluginContextImpl implements PluginContext, PluginContextImplMBean {
	/** The parent TSDB instance */
	protected final TSDB tsdb;
	/** The parent TSDB instance config */
	protected final Config config;
	/** The extracted TSDB instance config properties */
	protected final Properties extracted;
	/** The plugin support classloader */
	protected ClassLoader supportClassLoader;
	/** Miscellaneous named resources set for sharing across plugins */
	protected final Map<String, Object> namedResources = new ConcurrentHashMap<String, Object>();
	/** The registered resource listeners */
	protected final Set<IPluginContextResourceListener> listeners = new CopyOnWriteArraySet<IPluginContextResourceListener>();
	/** The sequence number factory for dispatched notificatons */
	protected final AtomicLong sequence = new AtomicLong();
	
	/** The JMX ObjectName of the MLet wrapped support class loader */
	protected ObjectName supportClassLoaderObjectName = null;
	/** The notification broadcaster thread pool */
	protected final ThreadPoolExecutor tpe = new AsyncDispatcherExecutor("PluginContext", 2, 5, 60000, 128);	
	/** The delegate NotificationBroadcaster */
	protected final NotificationBroadcasterSupport notificationBroadcaster;
	/** Instance logger */
	protected final Logger log = LoggerFactory.getLogger(getClass());
	
	

	/** The MBean notification descriptors */
	protected static final MBeanNotificationInfo[] MBEAN_NOTIF_INFOS = new MBeanNotificationInfo[]{};
	
	/**
	 * Creates a new PluginContextImpl
	 * @param tsdb The parent TSDB instance
	 * @param config The parent TSDB instance config
	 * @param extracted The extracted TSDB instance config properties
	 * @param supportClassLoader The plugin support classloader
	 */
	public PluginContextImpl(TSDB tsdb, Config config, Properties extracted, ClassLoader supportClassLoader) {
		if(JMXHelper.isRegistered(OBJECT_NAME)) throw new IllegalStateException("The PluginContext has already been created");
		this.tsdb = tsdb;
		this.config = config;
		this.extracted = extracted;
		this.supportClassLoader = supportClassLoader;
		this.notificationBroadcaster = new NotificationBroadcasterSupport(tpe, MBEAN_NOTIF_INFOS);
		JMXHelper.registerMBean(this, OBJECT_NAME);
		if(this.supportClassLoader instanceof URLClassLoader) {
			URLClassLoader urlClassLoader = (URLClassLoader)this.supportClassLoader;
			supportClassLoaderObjectName = JMXHelper.objectName(new StringBuilder(OBJECT_NAME.toString()).append(",ext=SupportClassLoader"));
			MLet mlet = new MLet(urlClassLoader.getURLs(), TSDB.class.getClassLoader());
			JMXHelper.registerMBean(mlet, supportClassLoaderObjectName);			
		}
	}
	
	public void setSupportClassLoader(ClassLoader loader) {
		this.supportClassLoader = loader;
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
	 * @see org.helios.tsdb.plugins.service.PluginContext#getResourceNames()
	 */
	@Override
	public Set<String> getResourceNames() {
		return Collections.unmodifiableSet(namedResources.keySet());
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
		if(listener instanceof IPluginContextResourceFilter) {
			addResourceListener(listener, (IPluginContextResourceFilter)listener);
		} else {
			addResourceListener(listener, null);
		}
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

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.service.PluginContextImplMBean#getSupportClassLoaderObjectName()
	 */
	@Override
	public ObjectName getSupportClassLoaderObjectName() {
		return supportClassLoaderObjectName;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.service.PluginContextImplMBean#getResource(java.lang.String)
	 */
	@Override
	public Object getResource(String name) {
		return namedResources.get(name);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.service.PluginContextImplMBean#getBoundResourceNames()
	 */
	@Override
	public Map<String, String> getBoundResourceNames() {
		Map<String, String> map = new HashMap<String, String>(namedResources.size());
		for(Map.Entry<String, Object> entry: namedResources.entrySet()) {
			map.put(entry.getKey(), entry.getValue().getClass().getName());
		}
		return map;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.service.PluginContext#publishNotification(java.lang.String, java.lang.String, java.lang.Object, java.lang.Object)
	 */
	public void publishNotification(String type, String message, Object userData, Object source) {
		Notification notification = new Notification(type, source==null ? OBJECT_NAME : source, sequence.incrementAndGet(), SystemClock.time(), message);
		if(userData!=null) {
			notification.setUserData(userData);
		}
//		log.info("\n\t@@@@@@@@@@@@@@@@\n\tNOTIF:\n\t{}\n\t@@@@@@@@@@@@@@@@\n", notification);
		sendNotification(notification);
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.service.PluginContextImplMBean#publishNotification(java.lang.String, java.lang.String)
	 */
	public void publishNotification(String type, String message) {
		publishNotification(type, message, null, null);
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return "PluginContext";
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.service.PluginContextImplMBean#getInstance()
	 */
	public PluginContext getInstance() {
		return this;
	}
	
	/**
	 * {@inheritDoc}
	 * @see javax.management.NotificationBroadcaster#addNotificationListener(javax.management.NotificationListener, javax.management.NotificationFilter, java.lang.Object)
	 */
	public void addNotificationListener(NotificationListener listener,
			NotificationFilter filter, Object handback) {
		notificationBroadcaster.addNotificationListener(listener, filter,
				handback);
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.NotificationBroadcaster#removeNotificationListener(javax.management.NotificationListener)
	 */
	public void removeNotificationListener(NotificationListener listener)
			throws ListenerNotFoundException {
		notificationBroadcaster.removeNotificationListener(listener);
	}

    /**
     * <p>Removes a listener from this MBean.  The MBean must have a
     * listener that exactly matches the given <code>listener</code>,
     * <code>filter</code>, and <code>handback</code> parameters.  If
     * there is more than one such listener, only one is removed.</p>
     *
     * <p>The <code>filter</code> and <code>handback</code> parameters
     * may be null if and only if they are null in a listener to be
     * removed.</p>
     *
     * @param listener A listener that was previously added to this
     * MBean.
     * @param filter The filter that was specified when the listener
     * was added.
     * @param handback The handback that was specified when the listener was
     * added.
     *
     * @exception ListenerNotFoundException The listener is not
     * registered with the MBean, or it is not registered with the
     * given filter and handback.
     */
	public void removeNotificationListener(NotificationListener listener,
			NotificationFilter filter, Object handback)
			throws ListenerNotFoundException {
		notificationBroadcaster.removeNotificationListener(listener, filter,
				handback);
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.NotificationBroadcaster#getNotificationInfo()
	 */
	public MBeanNotificationInfo[] getNotificationInfo() {
		return notificationBroadcaster.getNotificationInfo();
	}

    /**
     * Sends a notification.
     *
     * If an {@code Executor} was specified in the constructor, it will be given one
     * task per selected listener to deliver the notification to that listener.
     *
     * @param notification The notification to send.
     */
	public void sendNotification(Notification notification) {
		notificationBroadcaster.sendNotification(notification);
	}
	

}
