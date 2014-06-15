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

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.concurrent.Callable;

import javassist.ByteArrayClassPath;
import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.LoaderClassPath;
import javassist.NotFoundException;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ConstPool;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.StringMemberValue;
import net.opentsdb.core.TSDB;
import net.opentsdb.meta.TSMeta;
import net.opentsdb.utils.Config;

import org.helios.vm.attach.agent.LocalAgentInstaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

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
	
	private static final Logger LOG = LoggerFactory.getLogger(MethodMocker.class);
	
	/** The JVM's instrumentation instance */
	private final Instrumentation instrumentation;

	/** The original byte code of transformed classes keyed by the class internal form name */
	protected final Cache<String, byte[]> originalByteCode = CacheBuilder.newBuilder()
			.maximumSize(1000)
			.build();
	
	/** The transformed byte code of transformed classes keyed by the class internal form name */
	protected final Cache<String, byte[]> transformedByteCode = CacheBuilder.newBuilder()
			.maximumSize(1000)
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
		return clazz.getAnnotation(MockedClass.class)!=null;
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
	 * Executes the transformation
	 * @param targetClass The target class to transform
	 * @param mockedClass The source of the mocked methods to inject into the target
	 */
	public synchronized void transform(final Class<?> targetClass, Class<?> mockedClass) {
		String internalFormName = internalForm(targetClass.getName());
		if(transformedByteCode.getIfPresent(internalFormName)!=null) {
			throw new RuntimeException("Class ["+ targetClass.getName() + "] already in mocked state. Restore first and then and retransform");
		}
		ClassFileTransformer transformer = null;
		try {
			originalByteCode.get(internalFormName, new Callable<byte[]>() {
				@Override
				public byte[] call() throws Exception {
					ClassPool cp = new ClassPool();
					cp.appendClassPath(new LoaderClassPath(TSDB.class.getClassLoader()));
					CtClass tsdbClazz = cp.get(TSDB.class.getName());
					return tsdbClazz.toBytecode();			
				}
			});		
			transformer = newClassFileTransformer(internalFormName, mockedClass);
			instrumentation.addTransformer(transformer, true);
			instrumentation.retransformClasses(targetClass);			
		} catch (Exception ex) {
			throw new RuntimeException("Failed to transform [" + targetClass.getName() + "]", ex);
		} finally {
			if(transformer!=null) {
				instrumentation.removeTransformer(transformer);
			}
		}
	}
	
	/**
	 * Restores a transformed class back to its original form 
	 * @param targetClass The class to restore
	 */
	public synchronized void restore(Class<?> targetClass) {
		final String internalFormName = internalForm(targetClass.getName());
		final byte[] restoreByteCode = originalByteCode.getIfPresent(internalFormName);
		if(restoreByteCode==null || transformedByteCode.getIfPresent(internalFormName)==null) {
			throw new RuntimeException("Class ["+ targetClass.getName() + "] is not in mocked state");
		}
		ClassFileTransformer ctf = new ClassFileTransformer() {
			public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
				if(internalFormName.equals(className)) {
					transformedByteCode.invalidate(internalFormName);
					return restoreByteCode;
				}
				return classfileBuffer;
			}
		};
		try {
			//instrumentation.addTransformer(ctf, true);
			instrumentation.retransformClasses(targetClass);
			transformedByteCode.invalidate(internalFormName);
		} catch (Throwable e) {
			throw new RuntimeException("Failed to restore class [" + internalFormName + "]", e);
		} finally {
			instrumentation.removeTransformer(ctf);
		}
	}	
	
	/**
	 * Indicates if the passed methods have the identical signature
	 * @param m1 The first method
	 * @param m2 The second method
	 * @return true if both methods have the same signature, false otherwise
	 */
	protected boolean areMethodsEqual(Method m1, Method m2) {
		if(!m1.getName().equals(m2.getName())) return false;
		if(!m1.getReturnType().equals(m2.getReturnType())) return false;
		return Arrays.deepEquals(m1.getParameterTypes(), m2.getParameterTypes());
	}

	/**
	 * Creates a new classfile transformer
	 * @param internalFormClassName The class name to transform
	 * @param mockedClass The class containing the mocked template methods to inject into the target class
	 * @return the transformer
	 */
	protected ClassFileTransformer newClassFileTransformer(final String internalFormClassName, final Class<?> mockedClass) {
		final String binaryName = binaryForm(internalFormClassName);
		return new ClassFileTransformer(){
			@Override
			public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
					if(internalFormClassName.equals(className)) {
						LOG.info("\n\t================\n\tTransforming [{}]\n\tUsing [{}]\n\t================", binaryForm(internalFormClassName), mockedClass.getName());
						try {
							ClassPool cp = new ClassPool();
							cp.appendClassPath(new ByteArrayClassPath(binaryName, classfileBuffer));
							cp.appendClassPath(new LoaderClassPath(mockedClass.getClassLoader()));
							cp.appendClassPath(new ClassClassPath(MockedClass.class));
							CtClass targetClazz = cp.get(binaryName);
							CtClass mockClazz = cp.get(mockedClass.getName());
//							 
							int methodCount = 0;
							for(CtMethod templateMethod: mockClazz.getDeclaredMethods()) {
								if(!templateMethod.getDeclaringClass().equals(mockClazz)) continue;
								CtMethod targetMethod = null;
								try {
									targetMethod = targetClazz.getDeclaredMethod(templateMethod.getName(), templateMethod.getParameterTypes());
									targetClazz.removeMethod(targetMethod);
									targetMethod.setBody(templateMethod, null);
									targetClazz.addMethod(targetMethod);
									methodCount++;
								} catch (NotFoundException nfe) {					
								}				
							}
							if(methodCount==0) {
								throw new RuntimeException("Failed to replace any methods");
							}
							ConstPool constpool = targetClazz.getClassFile().getConstPool();
							AnnotationsAttribute attr = new AnnotationsAttribute(constpool, AnnotationsAttribute.visibleTag);
							Annotation annot = new Annotation(MockedClass.class.getName(), constpool);
							StringMemberValue smv = new StringMemberValue(mockedClass.getName(), constpool);
							annot.addMemberValue("mockProvider", smv);
							attr.addAnnotation(annot);	
							targetClazz.getClassFile().addAttribute(attr);
							
							byte[] byteCode =  targetClazz.toBytecode();
							transformedByteCode.put(internalFormClassName, byteCode);
							return byteCode;
						} catch (Exception ex) {
							LOG.error("Transform for [{}] using [{}] failed", binaryName, mockedClass.getName(), ex);
							return classfileBuffer;
						}
					}
					return classfileBuffer;
			}
		}; 

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
	 * Converts the passed internal form class name to the binary name
	 * @param name The class name to convert
	 * @return the binary name of the class
	 */
	public static String binaryForm(CharSequence name) {
		return name.toString().replace('/', '.');
	}
	
	
	/**
	 * Converts the binary name of passed class to the internal form 
	 * @param clazz The class for which the name should be converted
	 * @return the internal form name of the class
	 */
	public static String internalForm(Class<?> clazz) {
		return internalForm(clazz.getName());
	}
	
	public static void main(String[] args) {
		LOG.info("Testing MethodMocker");
		MockedClass mc = TSDB.class.getAnnotation(MockedClass.class);
		LOG.info("TSDB Annotated:" + (mc!=null));
		MethodMocker mocker = getInstance();
		EmptyTSDB template = new EmptyTSDB() {
			@Override
			public void indexTSMeta(TSMeta meta) {
				meta.setDescription("Your mock rocks");				
			}
		};
		mocker.transform(TSDB.class, template.getClass());
		mc = TSDB.class.getAnnotation(MockedClass.class);
		LOG.info("TSDB Annotated:" + (mc!=null));
		if(mc!=null) {
			LOG.info("TSDB Mock Provider:" + mc.mockProvider());
		}
		TSMeta meta = new TSMeta();
		TSDB tsdb = null;
		try {
			tsdb = new TSDB(new Config(true));
			tsdb.indexTSMeta(meta);
			LOG.info("TSMeta Description: [{}]", meta.getDescription());
			
		} catch (Exception ex) {
			throw new RuntimeException("Failed to create TSDB", ex);
		} finally {
			mocker.restore(TSDB.class);
			mc = TSDB.class.getAnnotation(MockedClass.class);
			LOG.info("TSDB Annotated:" + (mc!=null));			
			if(tsdb!=null) try { tsdb.shutdown(); } catch (Exception ex) {}
		}
	}

	
}
