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
package org.helios.tsdb.plugins.util.jmx.opentypes;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.LoaderClassPath;
import javassist.Modifier;

import com.google.common.cache.CacheStats;

/**
 * <p>Title: InterfaceBuilder</p>
 * <p>Description: Builds an interface based on the bean properties of the passed class.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.util.jmx.opentypes.InterfaceBuilder</code></p>
 */

public class InterfaceBuilder {
	
	public static final CtClass[] EMPTY_CT_CLAZZ = {};
	
	public static Class<?> buildInterface(String name, Class<?> implClass) {
		try {
			ClassPool cp = new ClassPool();
			cp.appendClassPath(new LoaderClassPath(implClass.getClassLoader()));
			BeanInfo implBeanInfo = Introspector.getBeanInfo(implClass);
			PropertyDescriptor[] pds = implBeanInfo.getPropertyDescriptors();
			
			CtClass implCt = cp.get(implClass.getName());			
			CtClass iface = cp.makeInterface(name);
			
			
			for(PropertyDescriptor pd: pds) {
				Method m = pd.getReadMethod();
				if(!m.getDeclaringClass().equals(implClass)) continue;
				CtMethod ctm = implCt.getDeclaredMethod(m.getName(), EMPTY_CT_CLAZZ);
				ctm.setModifiers(ctm.getModifiers() | Modifier.ABSTRACT);				
				iface.addMethod(ctm);
				
				m = pd.getWriteMethod();
				ctm = implCt.getDeclaredMethod(m.getName(), new CtClass[]{cp.get(m.getParameterTypes()[0].getName())});
				ctm.setModifiers(ctm.getModifiers() | Modifier.ABSTRACT);				
				iface.addMethod(ctm);
				
			}
			
			return iface.toClass(implClass.getClassLoader(), implClass.getProtectionDomain());
		} catch (Exception ex) {
			throw new RuntimeException("Unexpected error creating interface [" + name + "] for class [" + implClass.getName() + "]", ex);
		}
	}
	
	
	public static void main(String[] args) {
		
		
		log("InterfaceBuilder Test");
		Class<?> cacheStatsMXInface = buildInterface("CacheStatsMXBean", CacheStats.class); 
		log("Built:" + cacheStatsMXInface.getName());
		for(Method m: cacheStatsMXInface.getDeclaredMethods()) {
			log(m.toGenericString());
		}
	}
	
	public static void log(Object msg) {
		System.out.println(msg);
	}
}
