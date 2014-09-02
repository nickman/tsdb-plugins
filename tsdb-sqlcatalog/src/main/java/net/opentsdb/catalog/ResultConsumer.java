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

import org.helios.tsdb.plugins.remoting.json.JSONRequest;
import org.helios.tsdb.plugins.remoting.json.JSONResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.function.Consumer;

import com.fasterxml.jackson.core.JsonGenerator;

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
	/** The current query context */
	private final QueryContext ctx;
	/** The current response we're writing results to */
	private JSONResponse response = null;
	
	/**
	 * Creates a new ResultConsumerX
	 * @param request The original JSON request
	 * @param ctx The current query context
	 */
	public ResultConsumer(JSONRequest request, QueryContext ctx) {
		this.ctx = ctx;
		response = request.response();
		response.setOpCode("results");	
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see reactor.function.Consumer#accept(java.lang.Object)
	 */
	@Override
	public void accept(final T t) {
		try {
			
			JsonGenerator jgen = response.writeHeader(true);
			jgen.setCodec(ctx.getMapper());
			jgen.writeObjectField("results", t);	
			jgen.writeObjectField("q", ctx);	
			response.closeGenerator();
		} catch (Exception ex) {
			log.error("Failed to write result batch", ex);
			throw new RuntimeException("Failed to write result batch", ex);
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