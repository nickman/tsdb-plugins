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
package com.heliosapm.shorthand.attach.vm.agent;

import java.lang.instrument.ClassDefinition;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.management.ManagementFactory;
import java.util.jar.JarFile;

import javax.management.MBeanServer;
import javax.management.NotificationBroadcasterSupport;
import javax.management.ObjectName;

/**
 * <p>Title: AgentInstrumentation</p>
 * <p>Description: A JMX exposed java agent and instrumentation instance wrapper</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.attach.vm.agent.AgentInstrumentation</code></p>
 */
public class AgentInstrumentation extends NotificationBroadcasterSupport implements AgentInstrumentationMBean {
	/** The instrumentation delegate */
	protected final Instrumentation instrumentation;
	
	/** The Agent instrumentation JMX ObjectName */
	public static final ObjectName AGENT_INSTR_ON; 
	
	static {
		try {
			AGENT_INSTR_ON = new ObjectName("org.helios.vm.agent:service=AgentInstrumentation");
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	
	/**
	 * The agent bootstrap entry point
	 * @param agentArgs The agent initialization arguments
	 * @param inst The instrumentation instance
	 */	
	public static void agentmain(String agentArgs, Instrumentation inst) {
		premain(agentArgs, inst);
	}
	
	/**
	 * The agent bootstrap entry point
	 * @param agentArgs The agent initialization arguments
	 * @param inst The instrumentation instance
	 */
	public static void premain(String agentArgs, Instrumentation inst) {
		if(inst==null) {
			System.err.println("Agent install failed. Instrumentation was null. Stack trace follows:");
			new Throwable().fillInStackTrace().printStackTrace(System.err);
			return;
		}
		System.out.println("Loading AgentInstrumentation MBean");
		AgentInstrumentation ai = new AgentInstrumentation(inst);
		try {
			MBeanServer server = ManagementFactory.getPlatformMBeanServer();
			if(server.isRegistered(AGENT_INSTR_ON)) {
				server.unregisterMBean(AGENT_INSTR_ON);
			}
			server.registerMBean(ai, AGENT_INSTR_ON);
			System.out.println("AgentInstrumentation MBean Loaded");
		} catch (Exception e) {
			System.err.println("Agent install failed. AgentInstrumentation MBean could not be registered. Stack trace follows:");
			e.printStackTrace(System.err);
		}
	}
	
	/**
	 * The agent bootstrap entry point which fails the install since there is no instrumentation
	 * @param agentArgs The agent initialization arguments
	 */	
	public static void agentmain(String agentArgs) {
		System.err.println("Agent install failed. Instrumentation was null. Stack trace follows:");
	}
	
	/**
	 * The agent bootstrap entry point which fails the install since there is no instrumentation
	 * @param agentArgs The agent initialization arguments
	 */	
	public static void premain(String agentArgs) {
		System.err.println("Agent install failed. Instrumentation was null. Stack trace follows:");
	}
	

	/**
	 * Creates a new AgentInstrumentation
	 * @param instrumentation The acquired instrumentation instance
	 */
	public AgentInstrumentation(Instrumentation instrumentation) {
		super();
		this.instrumentation = instrumentation;		
	}
	
//	/**
//	 * Returns the byte code for the passed class
//	 * @param clazz The class to get the byte code for
//	 * @return the class bytecode or null if not found.
//	 */
//	@Override
//	public byte[] getByteCode(Class<?> clazz) {
//		System.out.println("Retransformable [" + clazz.getName() + "]:" + this.isModifiableClass(clazz));
//		byte[] bytecode = collector.getByteCode(clazz);
//		if(bytecode==null) {
//			try {
//				this.retransformClasses(clazz);
//			} catch (UnmodifiableClassException e) {
//				e.printStackTrace();
//				return null;
//			}
//		}
//		bytecode = collector.getByteCode(clazz); 
//		if(bytecode==null) {
//			System.out.println("Failed to Generate ByteCode for class [" + clazz.getName() + "]");
//		} else {
//			System.out.println("Generated ByteCode for class [" + clazz.getName() + "] --> [" + bytecode.length + "] Bytes.");
//		}
//		
//		return bytecode;
//	}
	
	/**
	 * Returns the instrumentation agent
	 * @return the instrumentation agent
	 */
	@Override
	public Instrumentation getInstrumentation() {
		return instrumentation;
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.instrument.Instrumentation#addTransformer(java.lang.instrument.ClassFileTransformer, boolean)
	 */
	@Override
	public void addTransformer(ClassFileTransformer transformer, boolean canRetransform) {
		instrumentation.addTransformer(transformer, canRetransform);
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.instrument.Instrumentation#addTransformer(java.lang.instrument.ClassFileTransformer)
	 */
	@Override
	public void addTransformer(ClassFileTransformer transformer) {
		instrumentation.addTransformer(transformer);
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.instrument.Instrumentation#removeTransformer(java.lang.instrument.ClassFileTransformer)
	 */
	@Override
	public boolean removeTransformer(ClassFileTransformer transformer) {
		return instrumentation.removeTransformer(transformer);
	}


	/**
	 * {@inheritDoc}
	 * @see java.lang.instrument.Instrumentation#isRetransformClassesSupported()
	 */
	@Override
	public boolean isRetransformClassesSupported() {
		return instrumentation.isRetransformClassesSupported();
	}


	/**
	 * {@inheritDoc}
	 * @see java.lang.instrument.Instrumentation#retransformClasses(java.lang.Class[])
	 */
	@Override
	public void retransformClasses(Class<?>... classes) throws UnmodifiableClassException {
		System.out.println("Retransforming [" + classes[0].getName() + "]");
		instrumentation.retransformClasses(classes);
	}


	/**
	 * {@inheritDoc}
	 * @see java.lang.instrument.Instrumentation#isRedefineClassesSupported()
	 */
	@Override
	public boolean isRedefineClassesSupported() {
		return instrumentation.isRedefineClassesSupported();
	}


	/**
	 * {@inheritDoc}
	 * @see java.lang.instrument.Instrumentation#redefineClasses(java.lang.instrument.ClassDefinition[])
	 */
	@Override
	public void redefineClasses(ClassDefinition... definitions) throws ClassNotFoundException, UnmodifiableClassException {
		instrumentation.redefineClasses(definitions);
	}


	/**
	 * {@inheritDoc}
	 * @see java.lang.instrument.Instrumentation#isModifiableClass(java.lang.Class)
	 */
	@Override
	public boolean isModifiableClass(Class<?> theClass) {
		return instrumentation.isModifiableClass(theClass);
	}


	/**
	 * {@inheritDoc}
	 * @see java.lang.instrument.Instrumentation#getAllLoadedClasses()
	 */
	@Override
	public Class<?>[] getAllLoadedClasses() {
		return instrumentation.getAllLoadedClasses();
	}


	/**
	 * {@inheritDoc}
	 * @see java.lang.instrument.Instrumentation#getInitiatedClasses(java.lang.ClassLoader)
	 */
	@Override
	public Class<?>[] getInitiatedClasses(ClassLoader loader) {
		return instrumentation.getInitiatedClasses(loader);
	}


	/**
	 * {@inheritDoc}
	 * @see java.lang.instrument.Instrumentation#getObjectSize(java.lang.Object)
	 */
	@Override
	public long getObjectSize(Object objectToSize) {
		return instrumentation.getObjectSize(objectToSize);
	}


	/**
	 * {@inheritDoc}
	 * @see java.lang.instrument.Instrumentation#appendToBootstrapClassLoaderSearch(java.util.jar.JarFile)
	 */
	@Override
	public void appendToBootstrapClassLoaderSearch(JarFile jarfile) {
		instrumentation.appendToBootstrapClassLoaderSearch(jarfile);
	}


	/**
	 * {@inheritDoc}
	 * @see java.lang.instrument.Instrumentation#appendToSystemClassLoaderSearch(java.util.jar.JarFile)
	 */
	@Override
	public void appendToSystemClassLoaderSearch(JarFile jarfile) {
		instrumentation.appendToSystemClassLoaderSearch(jarfile);
	}


	/**
	 * {@inheritDoc}
	 * @see java.lang.instrument.Instrumentation#isNativeMethodPrefixSupported()
	 */
	@Override
	public boolean isNativeMethodPrefixSupported() {
		return instrumentation.isNativeMethodPrefixSupported();
	}


	/**
	 * {@inheritDoc}
	 * @see java.lang.instrument.Instrumentation#setNativeMethodPrefix(java.lang.instrument.ClassFileTransformer, java.lang.String)
	 */
	@Override
	public void setNativeMethodPrefix(ClassFileTransformer transformer, String prefix) {
		instrumentation.setNativeMethodPrefix(transformer, prefix);
	}

}
