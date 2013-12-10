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
package org.helios.tsdb.plugins.util.jmx.jmxmp;

import java.util.Properties;

import javax.management.ObjectName;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;

import org.helios.tsdb.plugins.Constants;
import org.helios.tsdb.plugins.util.ConfigurationHelper;
import org.helios.tsdb.plugins.util.JMXHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.AbstractService;

/**
 * <p>Title: JMXMPConnectionServer</p>
 * <p>Description: Bootstrap service for the purce tcp socket JMXMP JMX Connection Server</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.util.jmx.jmxmp.JMXMPConnectionServer</code></p>
 */

public class JMXMPConnectionServer extends AbstractService {
	/** Instance logger */
	protected final Logger log = LoggerFactory.getLogger(getClass());
	
	/** The listener port */
	protected int port = -1;
	/** The listener binding interface */
	protected String iface = null; 
	/** The JMXConnectorServer JMXServiceURL */
	protected JMXServiceURL serviceUrl = null;
	/** The server instance */
	protected JMXConnectorServer server = null;
	
	/** The JMX ObjectName used to register the server's management interface */
	public static final ObjectName JMXMP_SERVER_OBJECT_NAME = JMXHelper.objectName("jmx.remote:service=ConnectionServer,protocol=jmxmp");
	
	/**
	 * Creates a new JMXMPConnectionServer
	 * @param config The TSDB provided configuration properties
	 */
	public JMXMPConnectionServer(Properties config) {
		port = ConfigurationHelper.getIntSystemThenEnvProperty(Constants.JMXMP_PORT, Constants.DEFAULT_JMXMP_PORT, config);
		iface = ConfigurationHelper.getSystemThenEnvProperty(Constants.JMXMP_INTERFACE, Constants.DEFAULT_JMXMP_INTERFACE, config);
		log.info("JMXMP Listener Endpoint: [{}:{}]", iface, port);
	}

	/**
	 * {@inheritDoc}
	 * @see com.google.common.util.concurrent.AbstractService#doStart()
	 */
	@Override
	protected void doStart() {
		if(port==-1) {
			log.info("JMXMP Connection Server Disabled.");
			return;
		}
		log.info("\n\t==========================================\n\tStarting JMXMP Connector Server\n\t==========================================");
		
		
		try {
			serviceUrl = new JMXServiceURL(String.format("service:jmx:jmxmp://%s:%s", iface, port));
			log.info("Service URL:[{}]", serviceUrl);
			server = JMXConnectorServerFactory.newJMXConnectorServer(serviceUrl, null, JMXHelper.getHeliosMBeanServer());
			JMXHelper.registerMBean(server, JMXMP_SERVER_OBJECT_NAME);
			final JMXMPConnectionServer _this = this;
			Thread shutdown = new Thread("JMXServerShutdownHook") {
				public void run() {
					_this.stop();
				}
			};
			shutdown.setDaemon(true);
			Runtime.getRuntime().addShutdownHook(shutdown);
			server.start();
		} catch (Exception ex) {
			log.error("Failed to start JMXMP Connector Server", ex);
		}
		log.info("\n\t==========================================\n\tJMXMP Connector Server Started\n\t==========================================");

	}

	/**
	 * {@inheritDoc}
	 * @see com.google.common.util.concurrent.AbstractService#doStop()
	 */
	@Override
	protected void doStop() {
		log.info("\n\t==========================================\n\tStopping JMXMP Connector Server\n\t==========================================");
		
		log.info("\n\t==========================================\n\tJMXMP Connector Server Stopped\n\t==========================================");

	}

}
