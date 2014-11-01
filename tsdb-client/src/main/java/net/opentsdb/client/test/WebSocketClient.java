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
package net.opentsdb.client.test;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import net.opentsdb.client.net.Codecs;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.http.HttpRequestEncoder;
import org.jboss.netty.handler.codec.http.HttpResponseDecoder;
import org.jboss.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketVersion;
import org.jboss.netty.handler.logging.LoggingHandler;
import org.jboss.netty.logging.InternalLogLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * <p>Title: WebSocketClient</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>net.opentsdb.client.test.WebSocketClient</code></p>
 */

public class WebSocketClient {

	private static final LoggingHandler top = new LoggingHandler("WebSocketClient-TOP", InternalLogLevel.WARN, true);
	private static final LoggingHandler bottom = new LoggingHandler("WebSocketClient-BOTTOM", InternalLogLevel.WARN, true);
	private static final Logger LOG = LoggerFactory.getLogger(WebSocketClient.class);

    private final URI uri;

    public WebSocketClient(URI uri) {
        this.uri = uri;
    }

    public void run() throws Exception {
        ClientBootstrap bootstrap =
                new ClientBootstrap(
                        new NioClientSocketChannelFactory(
                                Executors.newCachedThreadPool(new ThreadFactory() {
                                	@Override
                                	public Thread newThread(Runnable r) {
                                		Thread t = new Thread(r);
//                                		t.setDaemon(true);
                                		return t;
                                	}
                                }),
                                Executors.newCachedThreadPool(new ThreadFactory() {
                                	@Override
                                	public Thread newThread(Runnable r) {
                                		Thread t = new Thread(r);
//                                		t.setDaemon(true);
                                		return t;
                                	}
                                })));

        Channel ch = null;

        try {
            String protocol = uri.getScheme();
            if (!protocol.equals("ws")) {
                throw new IllegalArgumentException("Unsupported protocol: " + protocol);
            }

            HashMap<String, String> customHeaders = new HashMap<String, String>();
            customHeaders.put("MyHeader", "MyValue");

            // Connect with V13 (RFC 6455 aka HyBi-17). You can change it to V08 or V00.
            // If you change it to V00, ping is not supported and remember to change
            // HttpResponseDecoder to WebSocketHttpResponseDecoder in the pipeline.
            final WebSocketClientHandshaker handshaker =
                    new WebSocketClientHandshakerFactory().newHandshaker(
                            uri, WebSocketVersion.V13, null, false, customHeaders);

            bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
                public ChannelPipeline getPipeline() throws Exception {
                    ChannelPipeline pipeline = Channels.pipeline();
                    pipeline.addLast("tlogger", top);
                    pipeline.addLast("decoder", new HttpResponseDecoder());
                    pipeline.addLast("encoder", new HttpRequestEncoder());
                    //pipeline.addLast("logging", lh);
                    pipeline.addLast("ws-handler", new WebSocketClientHandler(handshaker));
//                    Codecs.JSON.addCodecs(pipeline);
                    pipeline.addLast("blogger", bottom);
                    return pipeline;
                }
            });

            // Connect
            System.out.println("WebSocket Client connecting");
            ChannelFuture future =
                    bootstrap.connect(
                            new InetSocketAddress(uri.getHost(), uri.getPort()));
            future.syncUninterruptibly();

            ch = future.getChannel();

            ObjectMapper om = new ObjectMapper();
            handshaker.handshake(ch).syncUninterruptibly();

            Map<String, Object> jreq = new HashMap<String, Object>();
            jreq.put("t", "req");
            jreq.put("rid", 1);
            jreq.put("svc", "system");
            jreq.put("op", "env");
            ch.write(new TextWebSocketFrame(om.writeValueAsString(jreq))).addListener(new ChannelFutureListener() {
				public void operationComplete(final ChannelFuture f) throws Exception {
					if(f.isSuccess()) {
						LOG.info("req successfully sent");
					} else {
						LOG.error("Failed to send req", f.getCause());						
					}
					f.getChannel().getCloseFuture().addListener(new ChannelFutureListener() {
						public void operationComplete(final ChannelFuture fx) throws Exception {
							if(fx.isSuccess()) {
								LOG.info("Channel Closed");
							} else {
								LOG.error("Failed to Close", fx.getCause());						
							}							
						}
					});
					
//					f.getChannel().close();
				}
			});
            
//            
//            
//            // Send 10 messages and wait for responses
//            System.out.println("WebSocket Client sending message");
//            for (int i = 0; i < 1000; i++) {
//                ch.write(new TextWebSocketFrame("Message #" + i));
//            }
//
//            // Ping
//            System.out.println("WebSocket Client sending ping");
//            ch.write(new PingWebSocketFrame(ChannelBuffers.copiedBuffer(new byte[]{1, 2, 3, 4, 5, 6})));
//
//            // Close
//            System.out.println("WebSocket Client sending close");
//            ch.write(new CloseWebSocketFrame());
//
//            // WebSocketClientHandler will close the connection when the server
//            // responds to the CloseWebSocketFrame.
            
        } finally {
        	try { Thread.sleep(5000); } catch (Exception x) {/* No Op */}
            if (ch != null) {
                ch.close();
            }
            bootstrap.releaseExternalResources();
        }
    }

    public static void main(String[] args) throws Exception {
        URI uri;
        if (args.length > 0) {
            uri = new URI(args[0]);
        } else {
            uri = new URI("ws://localhost:4242/ws");
        }
        new WebSocketClient(uri).run();
    }

}
