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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import javax.management.Attribute;
import javax.management.MBeanAttributeInfo;
import javax.management.StandardMBean;
import javax.management.openmbean.OpenType;

/**
 * <p>Title: MXBeanMapper</p>
 * <p>Description: Factory for opentype wrappers of OpenTSDB meta classes</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.util.jmx.opentypes.MXBeanMapper</code></p>
 */

public class MXBeanMapper {
    private final StandardMBean mxbean;
    private final MXBeanInvocationHandler handler;

    public MXBeanMapper(Class<?> originalType) {
        InterfaceClassLoader loader =
                new InterfaceClassLoader(originalType.getClassLoader());
        Class<?> mxbeanInterface =
            loader.findOrBuildInterface("X", originalType);

        handler = new MXBeanInvocationHandler();
        mxbean = makeMXBean(mxbeanInterface, handler);
    }

    private static <T> StandardMBean makeMXBean(Class<T> intf,
                                                InvocationHandler handler) {
        Object proxy =
                Proxy.newProxyInstance(intf.getClassLoader(),
                                       new Class<?>[] {intf},
                                       handler);
        T impl = intf.cast(proxy);
        return new StandardMBean(impl, intf, true);
    }

    public OpenType<?> getOpenType() {
        MBeanAttributeInfo ai = mxbean.getMBeanInfo().getAttributes()[0];
        assert(ai.getName().equals("X"));
        return (OpenType<?>) ai.getDescriptor().getFieldValue("openType");
    }

    public synchronized Object toOpenValue(Object javaValue) {
        handler.javaValue = javaValue;
        try {
            return mxbean.getAttribute("X");
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    public synchronized Object fromOpenValue(Object openValue) {
        try {
            mxbean.setAttribute(new Attribute("X", openValue));
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
        return handler.javaValue;
    }

    private static class MXBeanInvocationHandler implements InvocationHandler {
        volatile Object javaValue;

        public Object invoke(Object proxy, Method method, Object[] args)
        throws Throwable {
            if (method.getName().equals("getX"))
                return javaValue;
            else if (method.getName().equals("setX")) {
                javaValue = args[0];
                return null;
            } else
                throw new AssertionError("Bad method name " + method.getName());
        }
    }
}
