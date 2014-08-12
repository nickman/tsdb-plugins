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
package net.opentsdb.meta;

import java.nio.charset.Charset;

import net.opentsdb.utils.JSON;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * <p>Title: QueryOptions</p>
 * <p>Description: A container class to aggregate all the query options for a MetricMetaAPI query.</p>
 * <p>The default looks like this: <b><code>QueryOptions [metaOption=[XUID, NAME], outputFormat=JSON, pageSize=100]</code></b></p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>net.opentsdb.meta.QueryOptions</code></p>
 */

public class QueryOptions {
	/** The bit-mask representing the meta options to be included in the result */
	protected int meta = MetaOption.defaultBitmask;
	/** The ordinal of the format format defining the format of the result */
	protected int format = OutputFormat.JSON.ordinal();
	/** The maximum size of each page of results to be returned */
	protected int pageSize = 100;
	/** The starting index for the next chunk */
	protected Object nextIndex = null;
	/** An arbitrary state provided by the MetricsMetaAPI implementation which should be
	 submitted back in each subsequent call after the first during one logical query excution */
	protected byte[] context = null;
	
	
	
	public static void main(String[] args) {
		System.out.println(new QueryOptions());
		System.out.println("===");
		String s = new String(new QueryOptions().toJSON(), Charset.defaultCharset());
		System.out.println(s);
		System.out.println("===");
		System.out.println(QueryOptions.parse(s.getBytes()));
	}

		public byte[] toJSON() {
			return JSON.serializeToBytes(this);
		}
	
	  public static QueryOptions parse(final byte[] bytes) {
		  return JSON.parseToObject(bytes, QueryOptions.class);
	  }
	  
	  public static QueryOptions parse(final JsonNode node) {
		  return JSON.parseToObject(JSON.serializeToBytes(node), QueryOptions.class);
	  }

	/**
	 * Returns the meta option bit mask
	 * @return the metaOption bit mask
	 */
	public final int getMeta() {
		return meta;
	}


	/**
	 * Sets the meta option bit mask
	 * @param metaOption the metaOption to set
	 * @return this QueryOptions
	 */
	public final QueryOptions setMeta(int metaOption) {
		this.meta = metaOption;
		return this;
	}


	/**
	 * Returns the ordinal of the format format
	 * @return the format format
	 */
	public final int getFormat() {
		return format;
	}


	/**
	 * Sets the ordinal of the OutputFormat the result should be returned as
	 * @param outputFormat the OutputFormat ordinal to set
	 * @return this Query Options
	 */
	public final QueryOptions setFormat(int outputFormat) {
		this.format = outputFormat;
		return this;
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
	public final QueryOptions setPageSize(int pageSize) {
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
	public final QueryOptions setNextIndex(Object nextIndex) {
		this.nextIndex = nextIndex;
		return this;
	}
	
	/**
	 * Returns the query context
	 * @return the context
	 */
	public final byte[] getContext() {
		return context;
	}


	/**
	 * Sets the query context
	 * @param context the context to set
	 * @return this Query Options
	 */
	public final QueryOptions setContext(byte[] context) {
		this.context = context;
		return this;
	}
	

	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + meta;
		result = prime * result + format;
		result = prime * result + pageSize;
		return result;
	}


	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		QueryOptions other = (QueryOptions) obj;
		if (meta != other.meta)
			return false;
		if (format != other.format)
			return false;
		if (pageSize != other.pageSize)
			return false;
		return true;
	}


	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("QueryOptions [metaOption=");
		builder.append(MetaOption.members(meta));
		builder.append(", outputFormat=");
		builder.append(OutputFormat.decode(format));
		builder.append(", pageSize=").append(pageSize);
		if(nextIndex != null) {
			builder.append(", nextIndex=").append(nextIndex);
		}
		if(context!=null && context.length>0) {
			builder.append(", context=").append(context.length).append(" bytes");
		}
		builder.append("]");
		return builder.toString();
	}




	
	

}
