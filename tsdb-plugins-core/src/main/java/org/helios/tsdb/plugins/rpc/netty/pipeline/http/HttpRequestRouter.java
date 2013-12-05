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
package org.helios.tsdb.plugins.rpc.netty.pipeline.http;

import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.helios.tsdb.plugins.rpc.netty.pipeline.websock.WebSocketServiceHandler;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.frame.TooLongFrameException;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.jboss.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Title: HttpRequestRouter</p>
 * <p>Description: Service to route incoming {@link HttpRequest}s to a {@link HttpRequestHandler} by the request URI</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.rpc.netty.pipeline.http.HttpRequestRouter</code></p>
 */

public class HttpRequestRouter implements ChannelUpstreamHandler {
	/** The singleton instance */
	protected static volatile HttpRequestRouter instance = null;
	/** The singleton instance ctor lock */
	protected static final Object lock = new Object();
	/** Instance logger */
	protected final Logger log = LoggerFactory.getLogger(getClass());
	/** A map of {@link HttpRequestHandler}s keyed by the URI pattern they respond to */
	protected ConcurrentHashMap<String, HttpRequestHandler> uriRoutes = new ConcurrentHashMap<String, HttpRequestHandler>();
	/** A map of {@link HttpRequestHandler}s keyed by the wildcarded URI pattern they respond to */
	protected ConcurrentHashMap<String, HttpRequestHandler> uriWildcardRoutes = new ConcurrentHashMap<String, HttpRequestHandler>();
	
	/** The websocket handler to be inserted into the pipeline if a request comes in with a URI suffix of {@link #WS_URI_SUFFIX} */
	protected WebSocketServiceHandler webSocketHandler = new WebSocketServiceHandler();
    /** Default page URI */
    public static final String DEFAULT_URI = "index.html";
    /** Root URI */
    public static final String ROOT_URI = "/";
    /** WebSocket URI Suffix */
    public static final String WS_URI_SUFFIX = "/ws";

	/**
	 * Acquires and returns the singleton instance
	 * @return the singleton instance
	 */
	public static HttpRequestRouter getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new HttpRequestRouter(); 
				}
			}
		}
		return instance;
	}	
	
	/**
	 * Creates a new HttpRequestRouter
	 */
	protected HttpRequestRouter() {
	}
	
	/**
	 * Registers an HTTPRequestHandler
	 * @param requestHandler the handler to register
	 */
	public void registerHandler(HttpRequestHandler requestHandler) {
		if(requestHandler==null) throw new IllegalArgumentException("The passed requestHandler was null");
		int added = 0;
		int wadded = 0;
		for(String key: requestHandler.getUriPatterns()) {
			if(key.endsWith("*")) {
				if(uriWildcardRoutes.putIfAbsent(key.substring(0, key.length()-1), requestHandler)==null) {
					wadded++;
				} else {
					log.warn("HttpRequestHandler [{}] attempted to register with wildcard URI [{}] which was already registered", requestHandler.getClass().getName(), key);
				}					
			} else {
				if(uriRoutes.putIfAbsent(key, requestHandler)==null) {
					added++;
				} else {
					log.warn("HttpRequestHandler [{}] attempted to register with URI [{}] which was already registered", requestHandler.getClass().getName(), key);
				}					
			}
		}
		log.info("Adding HttpRequestHandler [{}] with [{}] URI keys and [{}] wildcard keys", requestHandler.getClass().getName(), added, wadded);
	}
	
	/**
	 * Returns an unmodifiable set of URI patterns that this modifier is activated for 
	 * @return an unmodifiable set of URI patterns that this modifier is activated for
	 */
	public Map<String, String> getUriMappings() {
		Map<String, String> map = new HashMap<String, String>(uriRoutes.size());
		for(Map.Entry<String, HttpRequestHandler> entry: uriRoutes.entrySet()) {
			map.put(entry.getKey(), entry.getValue().getClass().getName());
		}
		return map;
	}
	
	/**
	 * Returns an unmodifiable set of wildcard URI patterns that this modifier is activated for 
	 * @return an unmodifiable set of wildcard URI patterns that this modifier is activated for
	 */
	public Map<String, String> getWildcardUriMappings() {
		Map<String, String> map = new HashMap<String, String>(uriWildcardRoutes.size());
		for(Map.Entry<String, HttpRequestHandler> entry: uriWildcardRoutes.entrySet()) {
			map.put(entry.getKey(), entry.getValue().getClass().getName());
		}
		return map;
	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.ChannelUpstreamHandler#handleUpstream(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ChannelEvent)
	 */
	@Override
	public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
		HttpRequest request = null;
		if(!(e instanceof MessageEvent)) {
			if(e instanceof ExceptionEvent) {
				log.error("Http Routing Exception ", ((ExceptionEvent)e).getCause());
				sendError(ctx, INTERNAL_SERVER_ERROR);
				return;
			}
			ctx.sendUpstream(e);
			return;
		}
		MessageEvent me = (MessageEvent)e;
		Object message = me.getMessage();
		if(message instanceof WebSocketFrame) {
			ctx.sendUpstream(e);
			return;
		}
		try {
			request = (HttpRequest)((MessageEvent)e).getMessage();
			if(request==null) {
				throw new Exception("HttpRequest was null", new Throwable());
			}
		} catch (Exception ex) {
			throw new Exception("Failed to extract a message event for assumed type HttpRequest", ex);
		}
		// now we have a request...
		String uri = request.getUri();
		int qindex = uri.indexOf("?");
		if(qindex!=-1) {
			uri = uri.substring(0, qindex);
					
		}
		if(uri.endsWith(WS_URI_SUFFIX)) {
			ctx.getPipeline().addLast("websocket", webSocketHandler);
			log.info("Added WebSocketHandler to Pipeline for channel [{}]", e.getChannel().getId());
			ctx.sendUpstream(e);
			return;
		}
        if(uri.isEmpty() || ROOT_URI.equals(uri)) {
        	uri = "index.html";
        }
		
        log.debug("Processing HTTP Request for URI [{}]", uri);
		// ======================================================================
		// ======================================================================
		//		The handler lookup
		// ======================================================================
		// ======================================================================
		
		HttpRequestHandler handler = uriRoutes.get(uri);
		if(handler==null) {
			handler = findWildcardMatch(uri);
		}
		if(handler==null) {
			handler = uriRoutes.get("");  // the default handler is the file server
		}
		// ======================================================================
		
		if(handler==null) {
			sendError(ctx, NOT_FOUND);
		} else {
			try {
				handler.handle(ctx, (MessageEvent)e, request, uri);
			} catch (Exception ex) {
		        Channel ch = e.getChannel();
		        log.error("Uncaught exception", ex);
		        if (ex instanceof TooLongFrameException) {
		            sendError(ctx, BAD_REQUEST);
		            return;
		        }
		        if (ch.isConnected()) {
		            sendError(ctx, INTERNAL_SERVER_ERROR);
		        }
				
			}
		}
	}
	
	   /**
     * Returns an HTTP error back to the caller
     * @param ctx The channel handler context
     * @param status The HTTP Status to send
     */
    private void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
        HttpResponse response = new DefaultHttpResponse(HTTP_1_1, status);
        response.setHeader(CONTENT_TYPE, "text/plain; charset=UTF-8");
        response.setContent(ChannelBuffers.copiedBuffer(
                "Failure: " + status.toString() + "\r\n",
                CharsetUtil.UTF_8));

        // Close the connection as soon as the error message is sent.
        if(ctx.getChannel().isOpen()) {
        	ctx.getChannel().write(response).addListener(ChannelFutureListener.CLOSE);
        }
    }
    
    /**
     * Attempts to match the passed URI to a wildcarded handler
     * @param uri The uri to match
     * @return the matched handler or null if one was not found
     */
    protected HttpRequestHandler findWildcardMatch(String uri) {
    	for(Map.Entry<String, HttpRequestHandler> wc : uriWildcardRoutes.entrySet()) {
    		if(uri.startsWith(wc.getKey())) {
    			return wc.getValue();
    		}
    	}
    	return null;
    }
    /**
     * Handles uncaught exceptions
     * @param ctx The channel handler context
     * @param e The exception event
     * @throws Exception the uncaught exception handling exception
     */
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        Channel ch = e.getChannel();
        Throwable cause = e.getCause();
        if(cause != null && cause instanceof TooLongFrameException) {
        	
        }
        log.error("Uncaught exception", cause);
        if (cause instanceof TooLongFrameException) {
            sendError(ctx, BAD_REQUEST);
            return;
        }
        if (ch.isConnected()) {
            sendError(ctx, INTERNAL_SERVER_ERROR);
        }
    }
    
	

}
