/**
 * 
 */
package net.opentsdb.client.net;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import net.opentsdb.client.net.ws.WebSocketClientHandler;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.handler.codec.http.HttpRequestEncoder;
import org.jboss.netty.handler.codec.http.HttpResponseDecoder;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketVersion;

/**
 * <p>Title: ClientPipelineFactory</p>
 * <p>Description: Pipeline factory for OpenTSDB client instances</p>
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><b><code>net.opentsdb.client.net.ClientPipelineFactory</code></b>
 */

public class ClientPipelineFactory implements ChannelPipelineFactory {
	/** The requested protocol */
	private final String protocol;
	/** The requested target host */
	private final String host;
	/** The requested target listening port */
	private final int port;
	/** The requested path */
	private final String path;
	
	/** The URI parameters */
	private final Map<String, String> parameters;
	
	/** Ampersand splitter */
	private static final Pattern AMP_SPLITTER = Pattern.compile("&");
	/** Equals splitter */
	private static final Pattern EQ_SPLITTER = Pattern.compile("=");
	
	/** The static entry point for external callers */
	public static final ChannelPipelineFactory DEFAULT = new ChannelPipelineFactory() {
		@Override
		public ChannelPipeline getPipeline() throws Exception {			
			return getClientPipelineFactory(currentURI.get()).getPipeline();
		}
	};
	
	
	/** The currently configured factory URI */
	private static final ThreadLocal<URI> currentURI = new ThreadLocal<URI>();
	/** A map of pipeline factories keyed by the requested URI */
	private static final Map<URI, ClientPipelineFactory> factories = new ConcurrentHashMap<URI, ClientPipelineFactory>();
	
	/** The handshaker factory */
	protected final WebSocketClientHandshakerFactory handshakerFactory = new WebSocketClientHandshakerFactory();


	public static void setURI(final URI uri) {
		currentURI.set(uri);
	}
	
	public static void clearURI() {
		currentURI.remove();
	}
	
	/**
	 * Acquires a ClientPipelineFactory configured for the requested URI
	 * @param uri The requested URI
	 * @return a ClientPipelineFactory configured for the requested URI
	 */
	public static ClientPipelineFactory getClientPipelineFactory(final URI uri) {
		if(uri==null) throw new IllegalArgumentException("Passed uri was null");
		ClientPipelineFactory factory = factories.get(uri);
		if(factory==null) {
			synchronized(factories) {
				factory = factories.get(uri);
				if(factory==null) {
					factory = new ClientPipelineFactory(uri);
					factories.put(uri, factory);
				}
			}
		}
		return factory;
	}
	
	
	/**
	 * Creates a new ClientPipelineFactory
	 * @param uri The requested URI
	 */
	private ClientPipelineFactory(final URI uri) {
		protocol = uri.getScheme();
		host = uri.getHost();
		port = uri.getPort();
		path = uri.getPath();
		String params = uri.getQuery();
		Map<String, String> p = new HashMap<String, String>();
		if(params!=null && !params.trim().isEmpty()) {
			String[] pairs = AMP_SPLITTER.split(params.trim());
			for(String pair: pairs) {
				if(pair.trim().isEmpty()) continue;
				String[] keyValue = EQ_SPLITTER.split(pair.trim());
				String key = keyValue[0];
				String value = keyValue[1];
				if(key==null || key.trim().isEmpty() || value==null || value.trim().isEmpty()) continue;
				p.put(key.trim(), value.trim());
			}
		}
		parameters = Collections.unmodifiableMap(p);
	}
	
	
	/**
	 * Creates a new WebSocketClientHandshaker
	 * @return the handshaker
	 */
	protected WebSocketClientHandshaker getHandshaker() {
		return handshakerFactory.newHandshaker(currentURI.get(), WebSocketVersion.V13, null, false, null);
	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.ChannelPipelineFactory#getPipeline()
	 */
	@Override
	public ChannelPipeline getPipeline() throws Exception {
		return Protocol.from(protocol).getPipeline(host, path, port, parameters);
	}

	/**
	 * Returns the URI protocol 
	 * @return the protocol
	 */
	public final String getProtocol() {
		return protocol;
	}

	/**
	 * Returns the URI host
	 * @return the host
	 */
	public final String getHost() {
		return host;
	}

	/**
	 * Returns the URI port
	 * @return the port
	 */
	public final int getPort() {
		return port;
	}

	/**
	 * Returns a read-only map of the URI parameters
	 * @return the parameters
	 */
	public final Map<String, String> getParameters() {
		return parameters;
	}

}
