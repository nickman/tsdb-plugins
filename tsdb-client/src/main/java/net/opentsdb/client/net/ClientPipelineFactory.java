/**
 * 
 */
package net.opentsdb.client.net;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
	 /** The handshaker factory */
	protected final WebSocketClientHandshakerFactory handshakerFactory = new WebSocketClientHandshakerFactory();

	protected final URI uri;
	
	/**
	 * Creates a new 
	 */
	public ClientPipelineFactory(final URI uri) {
		this.uri = uri;
	}
	
	
	/**
	 * Creates a new WebSocketClientHandshaker
	 * @return the handshaker
	 */
	protected WebSocketClientHandshaker getHandshaker() {
		return handshakerFactory.newHandshaker(uri, WebSocketVersion.V13, null, false, null);
	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.ChannelPipelineFactory#getPipeline()
	 */
	@Override
	public ChannelPipeline getPipeline() throws Exception {
		ChannelPipeline pipeline = Channels.pipeline();
		pipeline.addLast("decoder", new HttpResponseDecoder());
		pipeline.addLast("encoder", new HttpRequestEncoder());
		pipeline.addLast("ws-handler", new WebSocketClientHandler(getHandshaker()));
		return pipeline;		
	}

}
