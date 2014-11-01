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
package javax.management.remote.tunnel;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.management.remote.JMXAddressable;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.generic.GenericConnector;
import javax.management.remote.generic.MessageConnection;

import com.sun.jmx.remote.opt.util.EnvHelp;
import com.sun.jmx.remote.socket.SocketConnection;

/**
 * <p>Title: TunnelConnector</p>
 * <p>Description: An SSH Local Port Forward tunneled JMXMP client that can connect to a remote JMXMP server.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>javax.management.remote.tunnel.TunnelConnector</code></p>
 */

public class TunnelConnector extends GenericConnector implements JMXAddressable {
    /** The requested tunnel JMX address */
    protected JMXServiceURL address;
    /** The internal connector environment map */
    protected transient Map<String, ?> tunnelEnv;

    /** The tunnel protocol name */
    public static final String protocolName = "jmxmp";

    /**
     * <p>Constructs a JMXMP Connector client tunneled through an SSH local port forwarder
     * that can make a connection to the connector server at the given address.  
     * This constructor is equivalent to {@link #TunnelConnector(JMXServiceURL, Map)
     * TunnelConnector(address, null)}.</p>
     *
     * @param address the address of the connector server to
     * connect to.
     *
     * @exception IllegalArgumentException if <code>address</code> is null.
     * @exception MalformedURLException if <code>address</code> is not
     * a valid URL for the tunnel connector.
     * @exception IOException if the connector cannot work for another reason.
     */
    public TunnelConnector(JMXServiceURL address) throws IOException {
    	this(address, null);
    }
    
    /**
     * <p>Constructs a tunneled Connector client that can make a
     * connection to the connector server at the given address.</p>
     *
     * @param address the address of the connector server to
     * connect to.
     *
     * @param env the environment parameters controlling the
     * connection.  This parameter can be null, which is equivalent to
     * an empty map.  The provided Map will not be
     * modified.
     *
     * @exception IllegalArgumentException if <code>address</code> is null.
     * @exception MalformedURLException if <code>address</code> is not
     * a valid URL for the tunnel connector.
     * @exception IOException if the connector cannot work for another reason.
     */
    public TunnelConnector(final JMXServiceURL address, final Map<String, ?> env) throws IOException {
    	super(env);
		tunnelEnv = new HashMap<String, Object>((env==null)?Collections.EMPTY_MAP:env);
    	this.address = address;
    	validateAddress();    	
    }
    
    private void validateAddress() throws IOException {
    	if (address == null) throw new IllegalArgumentException("JMXServiceURL must not be null");    	
    	if (!protocolName.equalsIgnoreCase(address.getProtocol())) throw new MalformedURLException("Unknown protocol: " + address.getProtocol());
    }    
    /**
     * {@inheritDoc}
     * @see javax.management.remote.generic.GenericConnector#connect(java.util.Map)
     */
    @Override
    public void connect(final Map env) throws IOException {
    	final Map<String, Object> newEnv = new HashMap<String, Object>();
    	validateAddress();
    	if(env!=null) {
    		newEnv.putAll(env);
    	}
    	final ClassLoader defaultClassLoader = EnvHelp.resolveClientClassLoader(newEnv);
    	newEnv.put(JMXConnectorFactory.DEFAULT_CLASS_LOADER, defaultClassLoader);
    	if (!newEnv.containsKey(MESSAGE_CONNECTION)) {
    		MessageConnection conn =
    			new SocketConnection(address.getHost(), address.getPort());
    		    newEnv.put(MESSAGE_CONNECTION, conn);
    	}
    	
    	super.connect(newEnv);
    }
    
    /**
     * {@inheritDoc}
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
    	return this.getClass().getName() + ": JMXServiceURL=" + address;
    }    

	/**
	 * {@inheritDoc}
	 * @see javax.management.remote.JMXAddressable#getAddress()
	 */
	@Override
	public JMXServiceURL getAddress() {
		return address;
	}



}
