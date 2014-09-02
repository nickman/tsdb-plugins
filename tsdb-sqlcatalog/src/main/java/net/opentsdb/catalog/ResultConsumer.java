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
package net.opentsdb.catalog;

import net.opentsdb.meta.api.QueryContext;

import org.helios.jmx.util.helpers.StringHelper;
import org.helios.tsdb.plugins.remoting.json.JSONRequest;
import org.helios.tsdb.plugins.remoting.json.JSONResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonGenerator;

import reactor.function.Consumer;

/**
 * <p>Title: ResultConsumer</p>
 * <p>Description: Reactor consumer function to write results from a MetricMetaAPI call back to the caller.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>net.opentsdb.catalog.ResultConsumer</code></p>
 * @param <T> The type of the results being returned to the caller
 */

public class ResultConsumer<T> implements Consumer<T> {
	/** static class logger */
	protected static final Logger log = LoggerFactory.getLogger(ResultConsumer.class);

	/** The original JSON request */
	private final JSONRequest request;		
	/** The current query context */
	private final QueryContext ctx;
	/** The streaming json generator */
	private JsonGenerator jgen;	
	/** The current response we're writing results to */
	private JSONResponse response = null;
	/** If true, we're still waiting on the first non-null/non-exception {@link #accept(Object)} call */
	private boolean firstResult = true;
	
	/**
	 * Creates a new ResultConsumerX
	 * @param request The original JSON request
	 * @param ctx The current query context
	 */
	public ResultConsumer(JSONRequest request, QueryContext ctx) {
		this.request = request;
		this.ctx = ctx;
		response = request.response();
		reset();			
	}
	
	/**
	 * Resets the consumer once a result package has been dispatched to the caller
	 */
	protected void reset() {
		try {
			firstResult = true;
			jgen = response.writeHeader(false);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to create JSON output streamer", ex);
		}			
	}
	

	/**
	 * Handles an exception accepted by the reactor's deferred
	 * @param t The exception accepted
	 */
	protected void handleError(final Throwable t) {
		ctx.setExhausted(true);
		try {
			response.resetChannelOutputStream();
			response.setOpCode("error");
			jgen = response.writeHeader(false);							
			String message = t.getMessage();
			if(message==null || message.trim().isEmpty()) {
				message = t.getClass().getSimpleName();
			}
			jgen.writeObject(message);
			jgen.writeObject(ctx);
			//jgen.writeObject(StringHelper.formatStackTrace(t));
//			jgen.writeString("The request timed out");
			response = response.closeGenerator();
			log.warn("Timeout message dispatched");
			//throw new RuntimeException("Request Expired");
		} catch (Exception ex) {
			throw new RuntimeException("Failed to write timeout response to JSON output streamer", ex);
		}							
	}
	
	/**
	 * {@inheritDoc}
	 * @see reactor.function.Consumer#accept(java.lang.Object)
	 */
	@Override
	public void accept(final T t) {
		
//		log.info("Accepting [{}]", t);
		if(t!=null && t instanceof Throwable) {
			synchronized(this) {
				handleError((Throwable)t);
			}
			return;
		}
		if(t==null) {
			log.info("Calling complete on thread [{}]", Thread.currentThread().getName());
			synchronized(this) {
				complete();
			}
			return;
		}
		try {
			if(firstResult) {
				jgen.writeStartArray();
				jgen.setCodec(ctx.getMapper());													
				firstResult = false;
			}
			jgen.writeObject(t);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to write accepted instance to JSON output streamer", ex);
		}
	}
	
	/**
	 * Completes a consumed set of objects or accepted exception
	 */
	private void complete() {
		try {
			jgen.writeEndArray();
			jgen.writeObject(ctx);				
			response = response.closeGenerator();
			log.info("\n\t**********************\n\tElapsed:{} ms.\n\t**********************", ctx.getElapsed());
			reset();				
		} catch (Exception ex) {
			throw new RuntimeException("Failed to close array in JSON output stream", ex);
		} finally {
			
		}
	}
	

}



/*
	Output Spec
	===========
	
Object
id: 1991992677
msg: Array[2]
0: Array[10]
0: Object
1: Object
2: Object
3: Object
4: Object
5: Object
6: Object
7: Object
8: Object
9: Object
length: 10
__proto__: Array[0]
1: Object
continuous: false
cummulative: 10
elapsed: 1939
elapsedTime: 1997.7770000696182
exhausted: false
expired: false
format: "DEFAULT"
maxSize: 2000
nextIndex: "000004000001000055000002000002000004000008000005000009"
nextMaxLimit: 10
pageSize: 10
timeout: 5000
__proto__: Object
length: 2
__proto__: Array[0]
op: null
rerid: 5
t: "resp"
__proto__: Object	


*/