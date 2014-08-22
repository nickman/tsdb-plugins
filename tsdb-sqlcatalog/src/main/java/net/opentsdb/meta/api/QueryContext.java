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

import java.nio.charset.Charset;

import net.opentsdb.utils.JSON;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * <p>Title: QueryContext</p>
 * <p>Description: A container class to aggregate all the query options for a MetricMetaAPI query.</p>
 * <p>The default looks like this: <b><code>QueryContext [metaOption=[XUID, NAME], outputFormat=JSON, pageSize=100]</code></b></p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>net.opentsdb.meta.api.QueryContext</code></p>
 */

public class QueryContext {
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

	
	/** The starting index for the next chunk */
	protected Object nextIndex = null;
	/** Indicates if the full result set has exhausted */
	protected boolean exhausted = false;
	/** The maximum cummulative number of items to be returned within this context */
	protected int maxSize = 5000;
	/** The cummulative number of items retrieved within this context */
	protected int cummulative = 0;
	
	
	public static void main(String[] args) {
		System.out.println(new QueryContext());
		System.out.println("===");
		String s = new String(new QueryContext().toJSON(), Charset.defaultCharset());
		System.out.println(s);
		System.out.println("===");
		System.out.println(QueryContext.parse(s.getBytes()));
	}

		public byte[] toJSON() {
			return JSON.serializeToBytes(this);
		}
	
	  public static QueryContext parse(final byte[] bytes) {
		  return JSON.parseToObject(bytes, QueryContext.class);
	  }
	  
	  public static QueryContext parse(final JsonNode node) {
		  return JSON.parseToObject(JSON.serializeToBytes(node), QueryContext.class);
	  }



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
		builder.append(", elapsed=").append(elapsed);
		builder.append(", maxSize=").append(maxSize);
		builder.append(", exhausted=").append(exhausted);
		builder.append(", cummulative=").append(cummulative);
		if(nextIndex != null) {
			builder.append(", nextIndex=").append(nextIndex);
		}
		builder.append("]");
		return builder.toString();
	}
	
	/**
	 * Indicates if the query should be run again for continuity
	 * @return true to continue, false otherwise
	 */
	public boolean shouldContinue() {
		return continuous && nextIndex != null && !isExpired() && !isExhausted() && cummulative < maxSize;
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
		return Math.min(pageSize, maxSize - cummulative);
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
		this.cummulative = cummulative;
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
		final long now = System.currentTimeMillis();
		try {
			if(timeLimit!=-1L) {				
				elapsed = now - timeLimit + timeout;
				boolean exp =  now > timeLimit;
				timeLimit = -1L;
				return exp;
			}
			return false;
		} finally {
			if(continuous) {
				timeLimit = now + timeout;
			}
		}
	}
	
	@JsonIgnore
	protected final void setExpired(boolean exp) {
		
	}

	/**
	 * Starts the expiration timeout clock
	 * @return this QueryContext
	 */
	public QueryContext startExpiry() {
		this.timeLimit = System.currentTimeMillis() + timeout;
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




	
	

}
