/**
 * 
 */
package net.opentsdb.client.net;

import org.jboss.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Title: Client</p>
 * <p>Description: </p>
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><b><code>net.opentsdb.client.net.Client</code></b>
 */

public class Client {
	/** Instance logger */
	protected static final Logger log = LoggerFactory.getLogger(Client.class);

	/** The netty channel for this client */
	protected Channel channel = null;
	
	/**
	 * Creates a new client 
	 * @param channel The netty channel for this client
	 */
	Client(final Channel channel) {
		this.channel = channel;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
