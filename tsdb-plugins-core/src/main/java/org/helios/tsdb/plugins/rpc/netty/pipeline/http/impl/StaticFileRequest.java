/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2014, Helios Development Group and individual contributors
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
package org.helios.tsdb.plugins.rpc.netty.pipeline.http.impl;

import static org.jboss.netty.handler.codec.http.HttpHeaders.setContentLength;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.OK;
import static org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;

import javax.activation.MimetypesFileTypeMap;

import net.opentsdb.core.TSDB;
import net.opentsdb.tsd.HttpQuery;
import net.opentsdb.tsd.StaticFileRpc;
import net.opentsdb.utils.Config;

import org.helios.tsdb.plugins.rpc.netty.pipeline.http.HttpRequestHandler;
import org.helios.tsdb.plugins.service.TSDBPluginServiceLoader;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelFutureProgressListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.DefaultFileRegion;
import org.jboss.netty.channel.FileRegion;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.ssl.SslHandler;
import org.jboss.netty.handler.stream.ChunkedFile;
import org.jboss.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * <p>Title: StaticFileRequest</p>
 * <p>Description: A fill-in static file request handler operating in the Netty RPC Plugin</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.rpc.netty.pipeline.http.impl.StaticFileRequest</code></p>
 */

public class StaticFileRequest implements HttpRequestHandler {
	/** Instance logger */
	protected final Logger log = LoggerFactory.getLogger(getClass());

	protected final String root;
	protected final File rootDir;
	
	protected final StaticFileRpc fileRpc;
	protected final TSDB tsdb;
	
    public static final String HTTP_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz";
    public static final String HTTP_DATE_GMT_TIMEZONE = "GMT";
    public static final int HTTP_CACHE_SECONDS = 60;
    
    /** A cache of loaded resources shared amongst file server handler instances */
    protected static final Map<String, HttpResponse> contentCache = new ConcurrentHashMap<String, HttpResponse>(1024);
    /** The Mime type assigner */
    protected static final MimetypesFileTypeMap mimeTypesMap = new MimetypesFileTypeMap();
    
    static {
    	mimeTypesMap.addMimeTypes("application/js");
    	mimeTypesMap.addMimeTypes("text/html");
    	
    }
    
    
	
	
	/**
	 * Creates a new StaticFileRequest
	 */
	public StaticFileRequest() {
		Config cfg = TSDBPluginServiceLoader.getInstance().getTSDB().getConfig();
		root = cfg.getString("tsd.http.staticroot");
		rootDir = new File(root);
		if(!rootDir.exists() || !rootDir.isDirectory()) {
			log.error("The configured root is invalid [{}]", root);
			throw new RuntimeException("The configured root is invalid [" + root + "]");
		}
		fileRpc = new StaticFileRpc();
		tsdb = TSDBPluginServiceLoader.getInstance().getTSDB();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.rpc.netty.pipeline.http.HttpRequestHandler#getUriPatterns()
	 */
	@Override
	public Set<String> getUriPatterns() {
		return new HashSet<String>(Arrays.asList("/s/*", "/s/", "/s"));
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.rpc.netty.pipeline.http.HttpRequestHandler#handle(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.MessageEvent, org.jboss.netty.handler.codec.http.HttpRequest, java.lang.String)
	 */
	@Override
	public void handle(ChannelHandlerContext ctx, MessageEvent e, HttpRequest request, String path) throws Exception {
		fileRpc.execute(tsdb, new HttpQuery(tsdb, request, ctx.getChannel()));
//		log.info("Static File Request: [{}], URI: [{}]", path, request.getUri());
//        if (request.getMethod() != GET) {
//            sendError(ctx, METHOD_NOT_ALLOWED);
//            return;
//        }
//        ChannelFuture writeFuture = writeResponseFromFile(ctx, e, request, path);
//
//        // Decide whether to close the connection or not.
//        if (writeFuture!=null && !isKeepAlive(request)) {
//            // Close the connection when the whole content is written out.
//            writeFuture.addListener(ChannelFutureListener.CLOSE);
//        }

	}
	
    /**
     * Sends an error back to the HTTP caller
     * @param ctx The channel handler context on which the request was receives
     * @param status the repsonse status
     * @param messages An optional array of messages added to the error
     */
    protected void sendError(ChannelHandlerContext ctx, HttpResponseStatus status, String...messages) {
        HttpResponse response = new DefaultHttpResponse(HTTP_1_1, status);

        response.setHeader(CONTENT_TYPE, "text/plain; charset=UTF-8");
        StringBuilder b = new StringBuilder();
        if(messages!=null) {
        	for(String m: messages) {
        		b.append(m).append("\r\n");
        	}
        }
        response.setContent(ChannelBuffers.copiedBuffer(
                "Failure: " + status.toString() + "\r\n" + b.toString(),
                CharsetUtil.UTF_8));

        // Close the connection as soon as the error message is sent.
        ctx.getChannel().write(response).addListener(ChannelFutureListener.CLOSE);
    }

	
	/**
	 * Writes the content request response from the file system
	 * @param ctx
	 * @param e
	 * @param request
	 * @param path
	 * @return    ChannelFuture
	 * @throws ParseException
	 * @throws IOException
	 */
	private ChannelFuture writeResponseFromFile(ChannelHandlerContext ctx,
			MessageEvent e, HttpRequest request, String path)
			throws ParseException, IOException {
		int qindex = path.indexOf('?');
		if(qindex!=-1) {
			path = path.substring(0, qindex);
		}
		File file = new File(root + File.separator + path.substring(2));
        if (file.isHidden() || !file.exists()) {
            sendError(ctx, NOT_FOUND);
            return null;
        }
        if (!file.isFile()) {
            sendError(ctx, FORBIDDEN);
            return null;
        }

        // Cache Validation
        String ifModifiedSince = request.getHeader(HttpHeaders.Names.IF_MODIFIED_SINCE);
        if (ifModifiedSince != null && !ifModifiedSince.equals("")) {
            SimpleDateFormat dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);
            Date ifModifiedSinceDate = dateFormatter.parse(ifModifiedSince);

            // Only compare up to the second because the datetime format we send to the client does not have milliseconds 
            long ifModifiedSinceDateSeconds = ifModifiedSinceDate.getTime() / 1000;
            long fileLastModifiedSeconds = file.lastModified() / 1000;
            if (ifModifiedSinceDateSeconds == fileLastModifiedSeconds) {
                sendNotModified(ctx);
                return null;
            }
        }
        
        RandomAccessFile raf;
        try {
            raf = new RandomAccessFile(file, "r");
        } catch (FileNotFoundException fnfe) {
            sendError(ctx, NOT_FOUND);
            return null;
        }
        long fileLength = raf.length();

        HttpResponse response = new DefaultHttpResponse(HTTP_1_1, OK);
        setContentLength(response, fileLength);
        setContentTypeHeader(response, file.getAbsolutePath());
        setDateAndCacheHeaders(response, file);
        
        Channel ch = e.getChannel();

        // Write the initial line and the header.
        ch.write(response);

        // Write the content.
        ChannelFuture writeFuture;
        if (ch.getPipeline().get(SslHandler.class) != null) {
            // Cannot use zero-copy with HTTPS.
            writeFuture = ch.write(new ChunkedFile(raf, 0, fileLength, 8192));
        } else {
            // No encryption - use zero-copy.
            final FileRegion region =
                new DefaultFileRegion(raf.getChannel(), 0, fileLength);
            writeFuture = ch.write(region);
            final String finalPath = path;
            writeFuture.addListener(new ChannelFutureProgressListener() {
            	@Override
                public void operationComplete(ChannelFuture future) {
                    region.releaseExternalResources();
                }
            	@Override
                public void operationProgressed(
                        ChannelFuture future, long amount, long current, long total) {
                    	log.info(String.format("%s: %d / %d (+%d)%n", finalPath, current, total, amount));
                }
            });
        }
		return writeFuture;
	}
	


    /**
     * When file timestamp is the same as what the browser is sending up, send a "304 Not Modified"
     * 
     * @param ctx
     *            Context
     */
    private void sendNotModified(ChannelHandlerContext ctx) {
        HttpResponse response = new DefaultHttpResponse(HTTP_1_1, HttpResponseStatus.NOT_MODIFIED);
        setDateHeader(response);

        // Close the connection as soon as the error message is sent.
        ctx.getChannel().write(response).addListener(ChannelFutureListener.CLOSE);
    }
    
    /**
     * Sets the Date header for the HTTP response
     * 
     * @param response
     *            HTTP response
     */
    private void setDateHeader(HttpResponse response) {
        SimpleDateFormat dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);
        dateFormatter.setTimeZone(TimeZone.getTimeZone(HTTP_DATE_GMT_TIMEZONE));

        Calendar time = new GregorianCalendar();
        response.setHeader(HttpHeaders.Names.DATE, dateFormatter.format(time.getTime()));
    }
    
    /**
     * Sets the Date and Cache headers for the HTTP Response
     * 
     * @param response
     *            HTTP response
     * @param fileToCache
     *            file to extract content type
     */
    private void setDateAndCacheHeaders(HttpResponse response, File fileToCache) {
        SimpleDateFormat dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);
        dateFormatter.setTimeZone(TimeZone.getTimeZone(HTTP_DATE_GMT_TIMEZONE));

        // Date header
        Calendar time = new GregorianCalendar();
        response.setHeader(HttpHeaders.Names.DATE, dateFormatter.format(time.getTime()));

        // Add cache headers
        time.add(Calendar.SECOND, HTTP_CACHE_SECONDS);
        response.setHeader(HttpHeaders.Names.EXPIRES, dateFormatter.format(time.getTime()));
        response.setHeader(HttpHeaders.Names.CACHE_CONTROL, "private, max-age=" + HTTP_CACHE_SECONDS);
        response.setHeader(HttpHeaders.Names.LAST_MODIFIED, dateFormatter.format(new Date(fileToCache.lastModified())));
    }
    
    /**
     * @param response
     * @param resourceName
     */
    private void setDateAndCacheHeaders(HttpResponse response, String resourceName) {
        SimpleDateFormat dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);
        dateFormatter.setTimeZone(TimeZone.getTimeZone(HTTP_DATE_GMT_TIMEZONE));

        // Date header
        Calendar time = new GregorianCalendar();
        response.setHeader(HttpHeaders.Names.DATE, dateFormatter.format(time.getTime()));

        // Add cache headers
        time.add(Calendar.SECOND, HTTP_CACHE_SECONDS);
        response.setHeader(HttpHeaders.Names.EXPIRES, dateFormatter.format(time.getTime()));
        response.setHeader(HttpHeaders.Names.CACHE_CONTROL, "private, max-age=" + HTTP_CACHE_SECONDS);
        response.setHeader(HttpHeaders.Names.LAST_MODIFIED, dateFormatter.format(new Date()));
    }
    

    /**
     * Sets the content type header for the HTTP Response
     * 
     * @param response
     *            HTTP response
     * @param resourceName
     *            file to extract content type
     */
    private void setContentTypeHeader(HttpResponse response, String resourceName) {
        response.setHeader(HttpHeaders.Names.CONTENT_TYPE, mimeTypesMap.getContentType(resourceName));
    }

	

	
}
