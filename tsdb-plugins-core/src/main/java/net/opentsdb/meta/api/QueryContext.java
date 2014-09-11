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
package net.opentsdb.meta.api;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import net.opentsdb.utils.JSON;

import org.helios.tsdb.plugins.remoting.json.serialization.TSDBTypeSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * <p>Title: QueryContext</p>
 * <p>Description: A container class to aggregate all the query options for a MetricMetaAPI query.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>net.opentsdb.meta.api.QueryContext</code></p>
 */

public class QueryContext {
	/** Static class logger */
	@JsonIgnore
	protected static final Logger log = LoggerFactory.getLogger(QueryContext.class);
	
	/** The maximum size of each page of results to be returned */
	protected int pageSize = 100;
	/** The timeout on each submitted request in ms. */
	protected long timeout = 3000L;
	/** The timestamp at which the running asynch request will be expired */
	@JsonIgnore
	private long timeLimit = -1L;
	/** The elapsed time of the last request in ms. */
	protected long elapsed = -1L;
	/** Indicates if the query will be continuous, self re-executing until are pages are returned */
	protected boolean continuous = false;
	/** The JSON seralization format to use for serializing the response objects for this query context */
	protected String format = "DEFAULT";
	/** Indicates hard expired */
	protected boolean expired = false;
	/** Recorded context events such as timings etc. */
	protected final Map<String, Object> ctx = new LinkedHashMap<String, Object>();

	
	/** The starting index for the next chunk */
	protected Object nextIndex = null;
	/** Indicates if the full result set has exhausted */
	protected boolean exhausted = false;
	/** The maximum cummulative number of items to be returned within this context */
	protected int maxSize = 5000;
	/** The cummulative number of items retrieved within this context */
	protected int cummulative = 0;
	


	/**
	 * Returns the page size set which is the maximum number of items returned in each call
	 * @return the pageSize the page size
	 */
	public final int getPageSize() {
		return pageSize;
	}


	/**
	 * Sets the page size set which is the maximum number of items returned in each call.
	 * The default is 100.
	 * @param pageSize the pageSize to set
	 * @return this Query Options
	 */
	public final QueryContext setPageSize(int pageSize) {
		if(pageSize < 1) throw new IllegalArgumentException("Invalid page size: [" + pageSize + "]");
		this.pageSize = pageSize;
		return this;
	}

	/**
	 * Returns the next index to start from
	 * @return the nextIndex
	 */
	public final Object getNextIndex() {
		return nextIndex;
	}


	/**
	 * Sets the next index to start from 
	 * @param nextIndex the nextIndex to set
	 * @return this Query Options
	 */
	public final QueryContext setNextIndex(Object nextIndex) {
		this.nextIndex = nextIndex;
		return this;
	}
	
	

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("QueryContext [pageSize=").append(pageSize);
		builder.append(", timeout=").append(timeout);
		builder.append(", continuous=").append(continuous);		
		builder.append(", timeLimit=").append(timeLimit);
		builder.append(", format=").append(format);
		builder.append(", elapsed=").append(elapsed);
		builder.append(", maxSize=").append(maxSize);
		builder.append(", exhausted=").append(exhausted);
		builder.append(", expired=").append(isExpired());
		builder.append(", cummulative=").append(cummulative);
		if(nextIndex != null) {
			builder.append(", nextIndex=").append(nextIndex);
		}
		if(!ctx.isEmpty()) {
			builder.append(", ctx:");
			for(Map.Entry<String, Object> e: ctx.entrySet()) {
				builder.append(e.getKey()).append(":").append(e.getValue()).append(", ");
			}
			builder.deleteCharAt(builder.length()-1); builder.deleteCharAt(builder.length()-1);
		}
		builder.append("]");
		return builder.toString();
	}
	
	/**
	 * Indicates if the query should be run again for continuity
	 * @return true to continue, false otherwise
	 */
	public boolean shouldContinue() {
		final boolean cont = continuous && nextIndex != null && !isExpired() && !isExhausted() && cummulative < maxSize;
		if(cont) this.timeLimit=-1L;
		return cont;
	}
	
	/**
	 * Adds an internal context value
	 * @param key The key
	 * @param value The value
	 * @return this QueryContext
	 */
	public QueryContext addCtx(final String key, final Object value) {
		ctx.put(key, value);
		return this;
	}
	
	/**
	 * Clears the internal context
	 * @return this QueryContext
	 */
	public QueryContext clearCtx() {
		ctx.clear();
		return this;
	}
	
	/**
	 * Returns a copy of the internal context
	 * @return a copy of the internal context
	 */
	public Map<String, Object> getCtx() {
		return new LinkedHashMap<String, Object>(ctx);
	}
	
	public String debugContinue() {
		return new StringBuilder("\nQueryContext Continue State [")
			.append("\n\tContinuous:").append(continuous)
			.append("\n\tHas Next Index:").append(nextIndex != null)
			.append("\n\tNot Expired:").append(!isExpired())
			.append("\n\tNot Exhausted:").append(!isExhausted())
			.append("\n\tNot At Max:").append((cummulative < maxSize))
			.append("\n\t===============================\n\tWill Continue:").append(
					continuous && nextIndex != null && !isExpired() && !isExhausted() && cummulative < maxSize
			)
			.append("\n]")
			.toString();
	}

	/**
	 * Indicates if the query's full result set is exausted
	 * @return true if the query's full result set is exausted, false if more data is available
	 */
	public boolean isExhausted() {
		return exhausted;
	}
	
	/**
	 * Calculates the item count limit on the next call which is the lesser of:<ol>
	 * 	<li>The configured page size</li>
	 *  <li>The maximum item count minus the number of cummulative items already retrieved</li>
	 * </ol>
	 * @return the item count limit on the next call
	 */
	public int getNextMaxLimit() {
		return Math.min((continuous ? maxSize : pageSize), maxSize - cummulative);
	}

	/**
	 * Indicates if the query should be for a count only
	 * @param exhausted true to set the full result set as exhausted, false to reset
	 * @return this QueryContext
	 */
	public QueryContext setExhausted(boolean exhausted) {
		this.exhausted = exhausted;
		return this;
	}

	/**
	 * Returns the configured maximum cummulative number of items to be returned within this context
	 * @return the maxSize the configured maximum cummulative number of items
	 */
	public final int getMaxSize() {
		return maxSize;
	}

	/**
	 * Sets the configured maximum cummulative number of items to be returned within this context
	 * @param maxSize the maximum cummulative number of items
	 * @return this QueryContext
	 */
	public final QueryContext setMaxSize(int maxSize) {
		this.maxSize = maxSize;
		return this;
	}

	/**
	 * Returns the cummulative number of items retrieved within this context
	 * @return the cummulative number of items
	 */
	public final int getCummulative() {
		return cummulative;
	}

	/**
	 * Increments the cummulative by the passed amount
	 * @param cummulative the amount to increment by
	 * @return this QueryContext
	 */
	public final QueryContext incrementCummulative(int cummulative) {
		this.cummulative += cummulative;
		return this;
	}

	/**
	 * Returns the timeout on each submitted request in ms. 
	 * @return the timeout in ms.
	 */
	public final long getTimeout() {
		return timeout;
	}

	/**
	 * Sets the timeout on each submitted request in ms.
	 * @param timeout the timeout in ms.
	 * @return this QueryContext
	 */
	public final QueryContext setTimeout(long timeout) {
		this.timeout = timeout;
		return this;
	}

	/**
	 * Returns the timestamp at which the running asynch request will be expired 
	 * @return the expired timestamp
	 */
	@JsonIgnore
	public final long getTimeLimit() {
		return timeLimit;
	}
	
	/**
	 * Indicates if the last submitted request in this context has expired
	 * according to the start time and timeout. Also updates the elapsed time.
	 * Should be called when the async response arrives.
	 * @return true if the request has expired, false otherwise.
	 */
	public final boolean isExpired() {
		if(expired) return true;
		final long now = System.currentTimeMillis();
		try {
			if(timeLimit!=-1L) {				
				elapsed = now - timeLimit + timeout;
				log.debug("\n\t***************\n\tTime Limit: {}\n\tNow: {}\n\tTimeout: {}\n\tElapsed: {}\n\t***************\n", timeLimit, now, timeout, elapsed);
				boolean exp =  now > timeLimit;
				timeLimit = -1L;
				expired = exp;
				return exp;
			} 
			return false;
		} finally {
			if(continuous) {
				timeLimit = now + timeout;
			}
		}
	}
	

	/**
	 * Starts the expiration timeout clock
	 * @return this QueryContext
	 */
	public QueryContext startExpiry() {
		if(this.timeLimit==-1L) {
			this.timeLimit = System.currentTimeMillis() + timeout;
			log.debug("\n\t**********************\n\tTimeLimit set with timeout [{}] to [{}]\n", timeout, timeLimit);
		}
		return this;
	}

	/**
	 * Returns the elapsed time of the last request in ms. 
	 * @return the elapsed time
	 */
	public final long getElapsed() {
		return elapsed;
	}

	/**
	 * Indicates if the query will be continuous, self re-executing until are pages are returned
	 * @return true if continuous, false if the caller will handle 
	 */
	public boolean isContinuous() {
		return continuous;
	}

	/**
	 * Sets the continuity of the query
	 * @param continuous true if continuous, false if the caller will handle 
	 * @return this QueryContext
	 */
	public QueryContext setContinuous(boolean continuous) {
		this.continuous = continuous;
		return this;
	}

	/**
	 * Returns the JSON seralization format name to use for serializing the response objects for this query context 
	 * @return the serializer format name
	 */
	public String getFormat() {
		return format;
	}

	/**
	 * Sets the JSON seralization format to use for serializing the response objects for this query context 
	 * @param serializer the serializer name to set
	 */
	public void setFormat(String serializer) {
		if(serializer==null || serializer.trim().isEmpty()) throw new IllegalArgumentException("The passed serialization name was null or empty");
		final String serName = serializer.trim().toUpperCase();
		try {
			TSDBTypeSerializer.valueOf(serName);
		} catch (Exception ex) {
			throw new IllegalArgumentException("The passed serialization name [" + serializer + "] was invalid. Valid names are:" + Arrays.toString(TSDBTypeSerializer.values()));
		}
		this.format = serName;
	}
	
	/**
	 * Returns the context's currently configured JSON ObjectMapper
	 * @return the context's currently configured JSON ObjectMapper
	 */
	@JsonIgnore
	public ObjectMapper getMapper() {
		try {
			return TSDBTypeSerializer.valueOf(format).getMapper();
		} catch (Exception ex) {
			return TSDBTypeSerializer.DEFAULT.getMapper();
		}
	}

	/**
	 * Serializes this QueryContext to JSON and returns the byte array
	 * @return the byte array containing the JSON
	 */
	public byte[] toJSON() {
		return JSON.serializeToBytes(this);
	}


}
