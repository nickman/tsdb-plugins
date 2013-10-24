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
package test.net.opentsdb.spring;

import java.util.Properties;

import net.opentsdb.core.TSDB;
import net.opentsdb.spring.ApplicationTSDBEvent;

import org.helios.tsdb.plugins.handlers.impl.QueuedResultSearchEventHandler;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;

/**
 * <p>Title: SpringEnabledQueuedResultSearchEventHandler</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>test.net.opentsdb.spring.SpringEnabledQueuedResultSearchEventHandler</code></p>
 */

public class SpringEnabledQueuedResultSearchEventHandler extends QueuedResultSearchEventHandler implements InitializingBean, ApplicationListener<ApplicationTSDBEvent>{
	/** The injected TSDB */
	@Autowired(required=true)
	protected TSDB tsdb = null;
	/** The injected config */
	@Autowired(required=true)
	protected Properties config = null;

	@Override
	public void afterPropertiesSet() throws Exception {
		this.initialize(tsdb, config);
		log.info("Configuration Complete");
	}
	@Override
	public void onApplicationEvent(ApplicationTSDBEvent event) {
		resultQueue.add(event);
	}

	
}
