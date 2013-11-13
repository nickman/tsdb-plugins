/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2013, Helios Development Group and individual contributors
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
package test.net.opentsdb.core;

import java.lang.instrument.Instrumentation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.heliosapm.shorthand.attach.vm.agent.LocalAgentInstaller;

/**
 * <p>Title: MethodMocker</p>
 * <p>Description: Utility to runtime transform target classes to mock functionality and then restore.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>test.net.opentsdb.core.MethodMocker</code></p>
 */

public class MethodMocker {
	/** The singleton instance */
	private static volatile MethodMocker instance = null;
	/** The singleton instance ctor lock */
	private static final Object lock = new Object();
	
	private static final Logger LOG = LoggerFactory.getLogger(TSDBMocker.class);
	
	/** The JVM's instrumentation instance */
	private final Instrumentation instrumentation;

	/** The original byte code of transformed classes keyed by the class internal form name */
	protected final Cache<String, byte[]> originalByteCode = CacheBuilder.newBuilder()
			.maximumSize(1000)
			.weakKeys()
			.build();
	
	/** The transformed byte code of transformed classes keyed by the class internal form name */
	protected final Cache<String, byte[]> transformedByteCode = CacheBuilder.newBuilder()
			.maximumSize(1000)
			.weakKeys()
			.build();
	
	
	/**
	 * Acquires the MethodMocker singleton instance
	 * @return the MethodMocker singleton instance
	 */
	public static MethodMocker getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new MethodMocker();
					LOG.info("Created MethodMocker");
				}
			}
		}
		return instance;
	}
	
	private MethodMocker() {
		try {
			instrumentation = LocalAgentInstaller.getInstrumentation(5000);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to initialize TSDBMocker", ex);
		}
	}
	
	/**
	 * Indicates if the passed class is currently mocked
	 * @param clazz The class to test for
	 * @return true if the passed class is currently mocked, false otherwise
	 */
	public boolean isClassMocked(Class<?> clazz) {
		if(clazz==null) throw new IllegalArgumentException("Passed class was null");
		return transformedByteCode.getIfPresent(internalForm(clazz))!=null;
	}
	
	/**
	 * Indicates if the class with the passed internal form name is currently mocked
	 * @param clazzName The class name to test for
	 * @return true if the class is currently mocked, false otherwise
	 */
	public boolean isClassMocked(String clazzName) {
		if(clazzName==null) throw new IllegalArgumentException("Passed class was null");
		return transformedByteCode.getIfPresent(internalForm(clazzName.trim()))!=null;
	}

	
	/**
	 * Converts the passed binary class name to the internal form 
	 * @param name The class name to convert
	 * @return the internal form name of the class
	 */
	public static String internalForm(CharSequence name) {
		return name.toString().replace('.', '/');
	}
	
	/**
	 * Converts the binary name of passed class to the internal form 
	 * @param clazz The class for which the name should be converted
	 * @return the internal form name of the class
	 */
	public static String internalForm(Class<?> clazz) {
		return internalForm(clazz.getName());
	}
	
	

	
}
