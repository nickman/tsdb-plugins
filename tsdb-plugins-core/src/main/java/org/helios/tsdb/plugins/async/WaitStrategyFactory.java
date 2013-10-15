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
package org.helios.tsdb.plugins.async;

import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;
import java.lang.reflect.Constructor;
import java.util.Arrays;

import com.lmax.disruptor.WaitStrategy;

/**
 * <p>Title: WaitStrategyFactory</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.async.WaitStrategyFactory</code></p>
 */

public class WaitStrategyFactory {
    /** The default package where these strategies are located */
    public static final String DEFAULT_PACKAGE = "com.lmax.disruptor.";

    /**
     * Creates a new {@link WaitStrategy} for the disruptor async handler.
     * @param className The wait strategy class name
     * @param args The constructor arguments in string form
     * @return the wait strategy instance
     */
    public static WaitStrategy newWaitStrategy(String className, String...args) {
    	WaitStrategy ws = null;
    	Class<?>_clazz = null;    	
    	Class<WaitStrategy> clazz = null;
    	if(!className.startsWith(DEFAULT_PACKAGE)) {
    		if(!className.contains(".")) {
    			className = DEFAULT_PACKAGE + className;
    		}
    	}
    	try {
    		_clazz = Class.forName(className);
    	} catch (Exception ex) {
    		throw new RuntimeException("Failed to load WaitStrategy [" + className + "]");    		
    	}
    	if(!WaitStrategy.class.isAssignableFrom(_clazz)) {
    		throw new IllegalArgumentException("The class [" + className + "] does not implement " + WaitStrategy.class.getName());
    	}
    	clazz = (Class<WaitStrategy>)_clazz;
    	Constructor<WaitStrategy> ctor = (Constructor<WaitStrategy>) clazz.getDeclaredConstructors()[0];
    	Class<?>[] paramTypes = ctor.getParameterTypes();
    	if(args.length!=paramTypes.length) {
    		StringBuilder b = new StringBuilder("[");
    		for(Class<?> pt: paramTypes) {
    			b.append(pt.getName()).append(",");
    		}
    		if(b.length()>1) b.deleteCharAt(b.length()-1).append("]");
    		throw new IllegalArgumentException("Invalid number of arguments. [" + className + "] has [" + paramTypes.length + "] but found [" + args.length + "]" + 
    				"Param Types Are:" + b.toString()
    		);
    	}
    	Object[] pArgs = new Object[paramTypes.length];    	
	    	for(int i = 0; i < paramTypes.length; i++) {
	    		try {
		    		PropertyEditor pe = PropertyEditorManager.findEditor(paramTypes[i]);
		    		pe.setAsText(args[i]);
		    		pArgs[i] = pe.getValue();
	    		} catch (Exception ex) {
	    			throw new IllegalArgumentException("The value [" + args[i] + "] for parameter [" + i + "] could not be converted to a [" + paramTypes[i] + "] in class [" + className + "]", ex);
	    		}
	    	}
	    try {
	    	return ctor.newInstance(pArgs);
	    } catch (Exception ex) {
	    	throw new RuntimeException("Failed to create instance of [" + className + "] with arguments " + Arrays.toString(args), ex);
	    }
    }
}
