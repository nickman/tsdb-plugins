/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2014, Helios Development Group and individual contributors
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
package org.helios.tsdb.plugins.async;

import reactor.core.Environment;
import reactor.core.Reactor;
import reactor.core.spec.Reactors;
import reactor.event.dispatch.Dispatcher;

/**
 * <p>Title: SingletonEnvironment</p>
 * <p>Description: Singleton for the reactor Env</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.async.SingletonEnvironment</code></p>
 */

public class SingletonEnvironment {
	/** The singleton instance */
	private static volatile SingletonEnvironment instance = null;
	/** The singleton instance ctor lock */
	private static final Object lock = new Object();	
	/** The environment */
	private final Environment env;
	/** The default reactor */
	private final Reactor defaultReactor;
	
	
	public static SingletonEnvironment getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new SingletonEnvironment();
				}
			}
		}
		return instance;
	}
	
	
	private SingletonEnvironment() {
		env = new Environment();
		defaultReactor = Reactors.reactor(env);
	}
	
	public Reactor getDefaultReactor() {
		return defaultReactor;
	}
	
	public Dispatcher getDefaultAsyncDispatcher() {
		return defaultReactor.getDispatcher();
	}


	/**
	 * Returns the 
	 * @return the env
	 */
	public Environment getEnv() {
		return env;
	}
}
