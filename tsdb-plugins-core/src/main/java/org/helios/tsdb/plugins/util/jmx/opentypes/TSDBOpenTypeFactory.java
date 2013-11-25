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
package org.helios.tsdb.plugins.util.jmx.opentypes;

import javax.management.openmbean.OpenType;

import net.opentsdb.meta.Annotation;
import net.opentsdb.meta.TSMeta;
import net.opentsdb.meta.UIDMeta;

import com.sun.jmx.mbeanserver.DefaultMXBeanMappingFactory;
import com.sun.jmx.mbeanserver.MXBeanMapping;

/**
 * <p>Title: TSDBOpenTypeFactory</p>
 * <p>Description: Factory to generate open-types for OpenTSDB's meta classes</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.util.jmx.opentypes.TSDBOpenTypeFactory</code></p>
 */

@SuppressWarnings("restriction")
public class TSDBOpenTypeFactory {
	protected static final DefaultMXBeanMappingFactory factory = new DefaultMXBeanMappingFactory();
	protected static final OpenType<?> ANNOTATION_OPENTYPE;
	protected static final OpenType<?> TSMETA_OPENTYPE;
	protected static final OpenType<?> UIDMETA_OPENTYPE;
	
	static {
		try {
			
			ANNOTATION_OPENTYPE = DefaultMXBeanMappingFactory.DEFAULT.mappingForType(Annotation.class, DefaultMXBeanMappingFactory.DEFAULT).getOpenType();
			TSMETA_OPENTYPE = DefaultMXBeanMappingFactory.DEFAULT.mappingForType(TSMeta.class, DefaultMXBeanMappingFactory.DEFAULT).getOpenType();
			UIDMETA_OPENTYPE = DefaultMXBeanMappingFactory.DEFAULT.mappingForType(UIDMeta.class, DefaultMXBeanMappingFactory.DEFAULT).getOpenType();
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	
	public static void main(String[] args) {
		log("Annotation Keys:" + ANNOTATION_OPENTYPE.getClass().getName());
	}
	
	public static void log(Object obj) {
		System.out.println(obj);
	}
	
	
	/**
	 * Creates a new TSDBOpenTypeFactory
	 */
	public TSDBOpenTypeFactory() {
		// TODO Auto-generated constructor stub
	}

}
