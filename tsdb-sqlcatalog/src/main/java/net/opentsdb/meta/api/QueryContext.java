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
	/** The starting index for the next chunk */
	protected Object nextIndex = null;
	/** Indicates if the full result set has exhausted */
	protected boolean exhausted = false;
	
	
	
	
	
	
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
		builder.append(", exhausted=").append(exhausted);
		if(nextIndex != null) {
			builder.append(", nextIndex=").append(nextIndex);
		}
		builder.append("]");
		return builder.toString();
	}

	/**
	 * Indicates if the query's full result set is exausted
	 * @return true if the query's full result set is exausted, false if more data is available
	 */
	public boolean isExhausted() {
		return exhausted;
	}

	/**
	 * Indicates if the query should be for a count only
	 * @param exhausted true to set the full result set as exhausted, false to reset
	 */
	public void setExhausted(boolean exhausted) {
		this.exhausted = exhausted;
	}




	
	

}
