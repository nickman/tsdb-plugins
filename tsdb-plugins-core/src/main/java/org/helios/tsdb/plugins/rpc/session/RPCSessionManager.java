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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.management.ObjectName;

import org.helios.tsdb.plugins.async.AsyncDispatcherExecutor;
import org.helios.tsdb.plugins.rpc.netty.NetyChannelSessionFactory;
import org.helios.tsdb.plugins.service.PluginContext;
import org.helios.tsdb.plugins.service.TSDBPluginServiceLoader;
import org.helios.tsdb.plugins.util.JMXHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Title: RPCSessionManager</p>
 * <p>Description: The manager for {@link IRPCSession} instances</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.rpc.session.RPCSessionManager</code></p>
 */

public class RPCSessionManager implements RPCSessionManagerMXBean {
	/** The singleton instance */
	private static volatile RPCSessionManager instance = null;
	/** The singleton instance ctor lock */
	private static final Object lock = new Object();
	/** Instance logger */
	private final Logger log = LoggerFactory.getLogger(getClass());
	/** A map of rpc sessions keyed by the session id */
	private final Map<String, IRPCSession> sessions = new ConcurrentHashMap<String, IRPCSession>();
	/** A map of registered ITransportSessionFactory instances keyed by the the supported transport type */
	private final Map<Class<?>, ITransportSessionFactory> lifeCycleFactories = new ConcurrentHashMap<Class<?>, ITransportSessionFactory>();
	/** A set of registered session lifecycle listeners */
	private final Set<IRPCSessionListener> sessionListeners = new CopyOnWriteArraySet<IRPCSessionListener>();
	/** A set of registered attribute binding listeners */
	private final Set<IAttributeBindingListener> attributeListeners = new CopyOnWriteArraySet<IAttributeBindingListener>();
	/** Async dispatcher for session events */
	private final AsyncDispatcherExecutor threadPool;
	/** The shared plugin context instance */
	private final PluginContext pluginContext;
	
	
	/** The JMX ObjectName for the RPCSessionManager MBean */
	public static final ObjectName OBJECT_NAME = JMXHelper.objectName(new StringBuilder(RPCSessionManager.class.getPackage().getName()).append(":service=").append(RPCSessionManager.class.getSimpleName()));
	
	
	/**
	 * Acquires and returns the singleton instance
	 * @return the singleton instance
	 */
	public static RPCSessionManager getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new RPCSessionManager();
				}
			}
		}
		return instance;
	}
	
	/**
	 * Creates a new RPCSessionManager
	 */
	private RPCSessionManager() {
		log.info("Created RPCSessionManager");		
		pluginContext = TSDBPluginServiceLoader.getLoaderInstance().getPluginContext();
		threadPool = new AsyncDispatcherExecutor("rpcsession", pluginContext.getExtracted());
		registerLifecycleSessionFactory(new NetyChannelSessionFactory());
		JMXHelper.registerMBean(this, OBJECT_NAME);
		
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.rpc.session.RPCSessionManagerMXBean#getSessions()
	 */
	@Override
	public Set<IRPCSessionMBean> getSessions() {
		return new HashSet<IRPCSessionMBean>(sessions.values());
	}
	
	/**
	 * Registers a lifecycle factory
	 * @param factory The lifecycle factory to register
	 */
	public void registerLifecycleSessionFactory(ITransportSessionFactory factory) {
		if(factory==null) throw new IllegalArgumentException("The passed factory was null");
		Class<?> transportType = factory.getTransportType();
		if(!lifeCycleFactories.containsKey(transportType)) {
			synchronized(lifeCycleFactories) {
				if(!lifeCycleFactories.containsKey(transportType)) {
					lifeCycleFactories.put(transportType, factory);
				}
			}
		}
	}
	
	/**
	 * Attempts to match a registered ITransportSessionFactory to the type of the passed opaque transport
	 * @param transport The transport
	 * @return the matched ITransportSessionFactory
	 */
	protected ITransportSessionFactory getFactoryFor(Object transport) {
		if(transport==null) throw new IllegalArgumentException("The passed transport was null");
		final Class<?> transportType = transport.getClass();
		ITransportSessionFactory factory = lifeCycleFactories.get(transportType);
		if(factory==null) {
			synchronized(lifeCycleFactories) {
				factory = lifeCycleFactories.get(transportType);
				if(factory==null) {
					// we did not get an exact match, so we need to iterate
					// all the keys of the map to see if this transport is a subclass
					for(Map.Entry<Class<?>, ITransportSessionFactory> entry: lifeCycleFactories.entrySet()) {
						if(entry.getKey().isInstance(transport)) {
							// cache the matching factory under this new subclass
							// so if we have more of them, we'll get an immediate match
							lifeCycleFactories.put(transportType, entry.getValue());
							factory = entry.getValue();
							break;
						}
					}
				}
			}
		}
		if(factory==null) {
			log.error("No ITransportSessionFactory found for transport type [{}]", transportType.getName());
			throw new RuntimeException("No ITransportSessionFactory found for transport type [" + transportType.getName() + "]");
		}
		return factory;
	}
	
	/**
	 * Processes an expired session
	 * @param expiredSession The expired session
	 */
	void expired(final IRPCSession expiredSession) {
		if(expiredSession!=null) {
			if(expiredSession.isExpired()) {
				if(!sessionListeners.isEmpty()) {
					threadPool.execute(new Runnable() {
						public void run() {
							sessions.remove(expiredSession.getSessionId());
							for(IRPCSessionListener listener: sessionListeners) {
								listener.onSessionExpired(expiredSession);
							}						
						}
					});
				}
				sessions.remove(expiredSession.getSessionId());
			}
		}
	}
	
	/**
	 * Processes an attribute binding event
	 * @param name The name of the attribute
	 * @param attribute The attribute value
	 */
	void attributeBound(final String name, final Object attribute) {
		if(!attributeListeners.isEmpty()) {
			threadPool.execute(new Runnable() {
				public void run() {
					for(IAttributeBindingListener listener: attributeListeners) {
						listener.onAttributeBound(name, attribute);
					}
				}
			});
		}
	}
	
	
	
	/**
	 * Processes an attribute removed event
	 * @param name The name of the attribute
	 * @param attribute The attribute value
	 */
	void attributeRemoved(final String name, final Object attribute) {
		if(!attributeListeners.isEmpty()) {
			threadPool.execute(new Runnable() {
				public void run() {
					for(IAttributeBindingListener listener: attributeListeners) {
						listener.onAttributeRemoved(name, attribute);
					}		
				}
			});
		}
	}
	
	/**
	 * Returns the IRPCSession for the passed transport object
	 * @param transport The transport object
	 * @return the IRPCSession for the transport object
	 */
	public IRPCSession getSession(Object transport) {
		IRPCSession tmpSession = getFactoryFor(transport).newRPCSession(transport);
		IRPCSession session = sessions.get(tmpSession.getSessionId());
		if(session==null) {
			synchronized(sessions) {
				session = sessions.get(tmpSession.getSessionId());
				if(session==null) {
					session = tmpSession;
					sessions.put(session.getSessionId(), session);
					if(!sessionListeners.isEmpty()) {
						final IRPCSession fsession = session;
						threadPool.execute(new Runnable() {
							public void run() {
								for(IRPCSessionListener listener: sessionListeners) {
									listener.onSessionCreated(fsession);
								}													
							}
						});
					}
				}
			}
		}
		return session;
	}
	
	/**
	 * Registers a new attribute binding listener that will be notified of session attribute binding and unbinding events.
	 * @param listener the listener to register
	 * @param filter An optional attribute event filter
	 */
	public void addAttributeBindingListener(final IAttributeBindingListener listener, final IAttributeBindingFilter filter) {
		if(listener!=null) {
			if(filter!=null) {
				attributeListeners.add(new IAttributeBindingListener(){
					@Override
					public void onAttributeBound(String name, Object attribute) {
						if(filter.include(name, attribute)) {
							listener.onAttributeBound(name, attribute);
						}
					}
					@Override
					public void onAttributeRemoved(String name, Object attribute) {
						if(filter.include(name, attribute)) {
							listener.onAttributeRemoved(name, attribute);
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
				});
			} else {
				attributeListeners.add(listener);
			}
		}		
	}
	
	/**
	 * Registers a new attribute binding listener that will be notified of session attribute binding and unbinding events.
	 * @param listener the listener to register
	 */
	public void addAttributeBindingListener(final IAttributeBindingListener listener) {
		addAttributeBindingListener(listener, null);
	}
	
	/**
	 * Removes a registered attribute listener
	 * @param listener The listener to remove
	 */
	public void removeAttributeBindingListener(final IAttributeBindingListener listener) {
		if(listener!=null) {
			attributeListeners.remove(listener);
		}
	}
	
	/**
	 * Registers a new IRPCSessionListener that will be notified of new and expired sessions
	 * @param listener The listener to register
	 */
	public void addSessionListener(IRPCSessionListener listener) {
		if(listener!=null) {
			sessionListeners.add(listener);
		}
	}
	
	/**
	 * Removes a IRPCSessionListener
	 * @param listener The listener to remove
	 */
	public void removeSessionListener(IRPCSessionListener listener) {
		if(listener!=null) {
			sessionListeners.remove(listener);
		}
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.rpc.session.RPCSessionManagerMXBean#getSessionCount()
	 */
	@Override
	public int getSessionCount() {
		return sessions.size();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.rpc.session.RPCSessionManagerMXBean#getSessionListenerCount()
	 */
	@Override
	public int getSessionListenerCount() {
		return sessionListeners.size();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.rpc.session.RPCSessionManagerMXBean#getAttributeListenerCount()
	 */
	@Override
	public int getAttributeListenerCount() {
		return attributeListeners.size();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.rpc.session.RPCSessionManagerMXBean#getTransportSessionFactories()
	 */
	@Override
	public Map<String, String> getTransportSessionFactories() {
		Map<String, String> factories = new HashMap<String, String>(lifeCycleFactories.size());
		for(Map.Entry<Class<?>, ITransportSessionFactory> entry: lifeCycleFactories.entrySet()) {
			factories.put(entry.getKey().getName(), entry.getValue().getClass().getName());
		}
		return factories;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.rpc.session.RPCSessionManagerMXBean#expireAll()
	 */
	@Override
	public void expireAll() {
		Set<IRPCSession> _sessions = new HashSet<IRPCSession>(sessions.values());
		for(IRPCSession session: _sessions) {
			session.expire();
		}
		_sessions.clear();		
	}


}
