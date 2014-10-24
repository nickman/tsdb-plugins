/**
 * 
 */
package net.opentsdb.client.net;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketVersion;
import org.jboss.netty.logging.InternalLoggerFactory;
import org.jboss.netty.logging.Slf4JLoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Title: ClientChannelFactory</p>
 * <p>Description: OpenTSDB Client Netty Channel Factory</p>
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><b><code>net.opentsdb.client.net.ClientChannelFactory</code></b>
 */

public class ClientChannelFactory {
	/** The singleton instance */
	private static volatile ClientChannelFactory instance = null;
	/** The singleton instance ctor lock */
	private static final Object lock = new Object();
	
	/** The netty client bootstrap */
	protected final ClientBootstrap bootstrap;
	/** The netty client channel factory */
	protected final NioClientSocketChannelFactory channelFactory;
	/** The netty worker thread pool */
	protected final ExecutorService workerPool;
	/** The netty boss thread pool */
	protected final ExecutorService bossPool;
	/** A channel group for connected client instances */
	protected final ChannelGroup channelGroup = new DefaultChannelGroup("OpenTSDBClients");
	
	/** Static class logger */
	protected static final Logger log = LoggerFactory.getLogger(ClientChannelFactory.class);
	
	
	/**
	 * Returns the ClientChannelFactory singleton instance
	 * @return the ClientChannelFactory singleton instance
	 */
	public static ClientChannelFactory getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					InternalLoggerFactory.setDefaultFactory(new Slf4JLoggerFactory());
					instance = new ClientChannelFactory();
				}
			}
		}
		return instance;
	}
	
	public static void main(String[] args) {
		log.info("ClientChannelFactory Test");
		ClientChannelFactory f = getInstance();
		try {
			URI uri = new URI("ws://localhost:4242/ws?codec=json");
			Channel channel = f.connect(uri, 2000);
			log.info("Connected !");
			try { Thread.currentThread().join(); } catch (Exception x) {/* No Op */}
		} catch (Exception ex) {
			log.error("Client Test Fail", ex);
		}
		
		
	}
	
	/**
	 * Creates a new ClientChannelFactory
	 */
	private ClientChannelFactory() {
		workerPool = Executors.newCachedThreadPool(new ThreadFactory(){
			final AtomicInteger serial = new AtomicInteger();
			@Override
			public Thread newThread(Runnable r) {
				Thread t = new Thread(r, "OpenTSDBClientWorker#" + serial.incrementAndGet());
				t.setDaemon(true);
				return t;
			}
		});
		bossPool = Executors.newCachedThreadPool(new ThreadFactory(){
			final AtomicInteger serial = new AtomicInteger();
			@Override
			public Thread newThread(Runnable r) {
				Thread t = new Thread(r, "OpenTSDBClientBoss#" + serial.incrementAndGet());
				t.setDaemon(true);
				return t;
			}
		});
		channelFactory = new NioClientSocketChannelFactory(bossPool, workerPool);
		bootstrap = new ClientBootstrap(channelFactory);
		bootstrap.setPipelineFactory(ClientPipelineFactory.DEFAULT);
		log.info("OpenTSDB ClientFactory started.");
	}
	
	
	private ChannelFuture connect(final URI uri) {
		try {
			ClientPipelineFactory.setURI(uri);
			return bootstrap.connect(new InetSocketAddress(uri.getHost(), uri.getPort()));
		} finally {
			ClientPipelineFactory.clearURI();
		}
	}
	
	/** The handshaker factory */
	protected final WebSocketClientHandshakerFactory handshakerFactory = new WebSocketClientHandshakerFactory();
	protected WebSocketClientHandshaker getHandshaker(final URI uri) {
		return handshakerFactory.newHandshaker(uri, WebSocketVersion.V13, null, false, new HashMap<String, String>(0));
	}
	
	private Channel connect(final URI uri, final long timeout) throws TimeoutException {
		try {
			ClientPipelineFactory.setURI(uri);
			final ChannelFuture cf = bootstrap.connect(new InetSocketAddress(uri.getHost(), uri.getPort()));
			if(cf.awaitUninterruptibly(timeout)) {
				try {
					//getHandshaker(uri).handshake(cf.getChannel()).sync();
					
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace(System.err);
				} 
				return cf.getChannel();
			} else {
				cf.cancel();
				throw new TimeoutException("Failed to acquire connection within timeout of [" + timeout + "] ms");
			}
		} finally {
			ClientPipelineFactory.clearURI();
		}
	}
	

}
