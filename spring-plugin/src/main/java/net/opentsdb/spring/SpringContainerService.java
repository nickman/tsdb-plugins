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
package net.opentsdb.spring;

import java.util.Properties;

import net.opentsdb.core.TSDB;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.GenericXmlApplicationContext;
import org.springframework.core.io.DefaultResourceLoader;

/**
 * <p>Title: SpringContainerService</p>
 * <p>Description: A bootstrap for a spring container triggered for loading as an OpenTSDB plugin.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>net.opentsdb.spring.SpringContainerService</code></p>
 */

public class SpringContainerService {
	/** Singleton instance */
	private static volatile SpringContainerService instance = null;
	/** Singleton instance ctor lock */
	private static final Object lock = new Object();

	/** Instance logger */
	protected final Logger log = LoggerFactory.getLogger(getClass());
	
	/** The service root context */
	protected final GenericXmlApplicationContext appContext = new GenericXmlApplicationContext();
	/** The bootstrap XML config resource loader */
	protected final DefaultResourceLoader resourceLoader;
	/** The core TSDB insance */
	protected final TSDB tsdb;
	
	/** The config property name for the resource path of the spring bootstrap xml */
	public static final String SPRING_ROOT_XML = "spr.tsd.config";
	/** The default resource path of the spring bootstrap xml */
	public static final String DEFAULT_SPRING_ROOT_XML = "classpath:spring/appCtx.xml";
	
	// CONFIG_PLUGIN_SUPPORT_PATH
	
	/**
	 * Acquires the SpringContainerService singleton instance
	 * @return the SpringContainerService singleton instance
	 */
	public static SpringContainerService getInstance(TSDB tsdb) {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new SpringContainerService(tsdb);
				}
			}
		}
		return instance;
	}
	
	/**
	 * Creates a new SpringContainerService
	 * @param tsdb the core TSDB instance
	 */
	private SpringContainerService(TSDB tsdb) {
		this.tsdb = tsdb;
		Properties p = new Properties();
		p.putAll(tsdb.getConfig().getMap());
		
	}

	/**
	 * CL Test
	 * @param args None
	 */
	public static void main(String[] args) {

	}

}
