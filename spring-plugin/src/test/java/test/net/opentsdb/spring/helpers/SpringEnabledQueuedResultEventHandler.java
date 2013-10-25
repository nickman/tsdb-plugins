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
package test.net.opentsdb.spring.helpers;

import java.util.Properties;

import net.opentsdb.core.TSDB;
import net.opentsdb.spring.ApplicationTSDBEvent;

import org.helios.tsdb.plugins.handlers.impl.QueuedResultSearchEventHandler;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;

/**
 * <p>Title: SpringEnabledQueuedResultEventHandler</p>
 * <p>Description: Test handler to pick up dispatched events to be validated</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>test.net.opentsdb.spring.SpringEnabledQueuedResultSearchEventHandler</code></p>
 */

public class SpringEnabledQueuedResultEventHandler extends QueuedResultSearchEventHandler implements InitializingBean, ApplicationListener<ApplicationTSDBEvent>{
	/**
	 * Creates a new SpringEnabledQueuedResultEventHandler
	 */
	public SpringEnabledQueuedResultEventHandler() {
		
	}
	
	@Override
	public void afterPropertiesSet() throws Exception {
		this.initialize(tsdb, config);
		log.info("Configuration Complete");
	}
	
	@Override
	public void onApplicationEvent(ApplicationTSDBEvent event) {
		resultQueue.add(event);
	}
	/**
	 * Sets the parent TSDB instance
	 * @param tsdb the tsdb to set
	 */
	@Autowired(required=true)
	public void setTsdb(TSDB tsdb) {
		this.tsdb = tsdb;
	}
	/**
	 * Sets the extracted config
	 * @param config the config to set
	 */
	@Autowired(required=true)
	public void setConfig(Properties config) {
		this.config = config;
	}

	
}
