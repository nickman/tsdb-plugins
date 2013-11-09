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
package test.net.opentsdb.core;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.LoaderClassPath;
import javassist.NotFoundException;
import net.opentsdb.core.TSDB;
import net.opentsdb.meta.TSMeta;
import net.opentsdb.utils.Config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.heliosapm.shorthand.attach.vm.agent.LocalAgentInstaller;

/**
 * <p>Title: TSDBMocker</p>
 * <p>Description: Utility to runtime transform the TSDB class so we can mock some functionality without being 
 * connected to a full-blown OpenTSDB and HBase instance.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>test.net.opentsdb.core.TSDBMocker</code></p>
 */

public class TSDBMocker implements ClassFileTransformer {
	/** The singleton instance */
	private static volatile TSDBMocker instance = null;
	/** The singleton instance ctor lock */
	private static final Object lock = new Object();
	
	private static final Logger LOG = LoggerFactory.getLogger(TSDBMocker.class);
	
	/** The JVM's instrumentation instance */
	private final Instrumentation instrumentation;
	/** Indicates if the TSDB class is currently modified */
	private final AtomicBoolean inMockedState = new AtomicBoolean(false);
	/** The original unmodified TSDB class byte code */
	private final byte[] tsdbByteCode;
	/** The internal form class name of the TSDB class */
	private final String TSDB_INTERNAL_FORM = internalForm(TSDB.class);
	
	/**
	 * Acquires the TSDBMocker singleton instance
	 * @return the TSDBMocker singleton instance
	 */
	public static TSDBMocker getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new TSDBMocker();
					LOG.info("Created TSDBMocker");
				}
			}
		}
		return instance;
	}
	
	private TSDBMocker() {
		try {
			instrumentation = LocalAgentInstaller.getInstrumentation(5000);
			ClassPool cp = new ClassPool();
			cp.appendClassPath(new LoaderClassPath(TSDB.class.getClassLoader()));
			CtClass tsdbClazz = cp.get(TSDB.class.getName());
			tsdbByteCode = tsdbClazz.toBytecode();			
			cp = null;
		} catch (Exception ex) {
			throw new RuntimeException("Failed to initialize TSDBMocker", ex);
		}
	}
	
	/**
	 * Transforms the TSDB class to adopt the method bodies of the passed class where method signatures match
	 * @param mockedTSDBClass The class providing the method overrides
	 */
	public synchronized void transform(Class<? extends EmptyTSDB> mockedTSDBClass) {
		if(inMockedState.get()) {
			throw new RuntimeException("TSDB already in mocked state. Restore and retransform");
		}
		try {
			ClassPool cp = new ClassPool();
			cp.appendClassPath(new LoaderClassPath(TSDB.class.getClassLoader()));
			cp.appendClassPath(new LoaderClassPath(mockedTSDBClass.getClassLoader()));
			CtClass tsdbClazz = cp.get(TSDB.class.getName());
			CtClass mockClazz = cp.get(mockedTSDBClass.getName());
			int mockedMethods = 0;
			Set<String> replaced = new HashSet<String>();
			for(CtMethod templateMethod: mockClazz.getDeclaredMethods()) {
				if(!templateMethod.getDeclaringClass().equals(mockClazz)) continue;
				CtMethod tsdbMethod = null;
				try {
					tsdbMethod = tsdbClazz.getDeclaredMethod(templateMethod.getName(), templateMethod.getParameterTypes());
					tsdbClazz.removeMethod(tsdbMethod);
					tsdbMethod.setBody(templateMethod, null);
					tsdbClazz.addMethod(tsdbMethod);
					replaced.add(tsdbMethod.getLongName());
					mockedMethods++;
				} catch (NotFoundException nfe) {					
				}				
			}
			if(mockedMethods<1) {
				throw new RuntimeException("Failed to replace any methods");
			}
			LOG.info("Mocking [{}] methods in TSDB\n\t{}", replaced.size(), replaced.toString());
			final byte[] modifiedByteCode = tsdbClazz.toBytecode();
			ClassFileTransformer mockingClassFileTransformer = new ClassFileTransformer(){
				@Override
				public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
						if(TSDB_INTERNAL_FORM.equals(className)) {
							LOG.info("\n\t================\n\tTransforming TSDB\n\t================");
							return modifiedByteCode;
						}
						return classfileBuffer;
				}
			}; 
			try {
				instrumentation.addTransformer(mockingClassFileTransformer, true);
				instrumentation.retransformClasses(TSDB.class);
				inMockedState.set(true);
			} finally {
				instrumentation.removeTransformer(mockingClassFileTransformer);
			}
		} catch (Exception ex) {
			throw new RuntimeException("Failed to mock TSDB class from [" + mockedTSDBClass.getName() + "]" , ex);
		}
	}
	
	/**
	 * Restores the TSDB class back to its original form 
	 */
	public synchronized void restoreTSDB() {
		if(inMockedState.get()) {
			try {
				instrumentation.addTransformer(this, true);
				instrumentation.retransformClasses(TSDB.class);
				inMockedState.set(false);
			} catch (Exception ex) {
				throw new RuntimeException("Failed to restore TSDB class to original form" , ex);
			} finally {
				instrumentation.removeTransformer(this);
			}
		}
	}
	
	/**
	 * Indicates if the TSDB class is currently mocked
	 * @return false if the TSDB class has been modified, false if it is in its original form
	 */
	public boolean isTSDBMocked() {
		return inMockedState.get();
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

	/**
	 * <p>Retransforms the TSDB class back to its original form</p> 
	 * {@inheritDoc}
	 * @see java.lang.instrument.ClassFileTransformer#transform(java.lang.ClassLoader, java.lang.String, java.lang.Class, java.security.ProtectionDomain, byte[])
	 */
	@Override
	public byte[] transform(ClassLoader loader, String className,
			Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
			byte[] classfileBuffer) throws IllegalClassFormatException {
			if(TSDB_INTERNAL_FORM.equals(className)) {
				LOG.info("\n\t================\n\tRestoring TSDB\n\t================");
				return tsdbByteCode;
			}
			return classfileBuffer;
	}
	
	public static void main(String[] args) {
		LOG.info("Testing TSDBMocker");
		TSDBMocker mocker = getInstance();
		EmptyTSDB template = new EmptyTSDB() {
			@Override
			public void indexTSMeta(TSMeta meta) {
				meta.setDescription("Your mock rocks");				
			}
		};
		mocker.transform(template.getClass());
		TSMeta meta = new TSMeta();
		TSDB tsdb = null;
		try {
			tsdb = new TSDB(new Config(true));
			tsdb.indexTSMeta(meta);
			LOG.info("TSMeta Description: [{}]", meta.getDescription());
			
		} catch (Exception ex) {
			throw new RuntimeException("Failed to create TSDB", ex);
		} finally {
			if(tsdb!=null) try { tsdb.shutdown(); } catch (Exception ex) {}
		}
	}
	

}
