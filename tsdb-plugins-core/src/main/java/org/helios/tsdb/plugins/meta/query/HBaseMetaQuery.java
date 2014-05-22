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
package org.helios.tsdb.plugins.meta.query;

import java.util.Map;

import javax.management.ObjectName;

import org.hbase.async.HBaseClient;

/**
 * <p>Title: HBaseMetaQuery</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.meta.query.HBaseMetaQuery</code></p>
 * @param <R> The type to be returned from the query
 */

public class HBaseMetaQuery<R> implements IMetaQuery<R> {
	/** The HBase client to query with */
	protected HBaseClient client = null;
	/**
	 * Creates a new HBaseMetaQuery
	 * @param client The HBase client to query with
	 */
	public HBaseMetaQuery(HBaseClient client) {
		this.client = client;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.meta.query.IMetaQuery#query(java.lang.CharSequence, java.lang.Class)
	 */
	@Override
	public CancelableIterator<R> query(CharSequence objectName, Class<R> returnType) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.meta.query.IMetaQuery#query(javax.management.ObjectName, java.lang.Class)
	 */
	@Override
	public CancelableIterator<R> query(ObjectName objectName, Class<R> returnType) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.meta.query.IMetaQuery#query(java.lang.CharSequence, java.util.Map, java.lang.Class)
	 */
	@Override
	public CancelableIterator<R> query(CharSequence metric, Map<String, String> tags, Class<R> returnType) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IMetaQuery<R> timeout(long timeout) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getTimeout() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public IMetaQuery<R> maxWaitNext(long maxWait) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getMaxWaitNext() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public IMetaQuery<R> maxWaitFirst(long maxWait) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getMaxWaitFirst() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public IMetaQuery<R> maxResults(long maxResults) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getMaxResults() {
		// TODO Auto-generated method stub
		return 0;
	}

}
