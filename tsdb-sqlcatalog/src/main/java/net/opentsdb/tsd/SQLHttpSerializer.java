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
package net.opentsdb.tsd;

import net.opentsdb.core.TSDB;

import com.stumbleupon.async.Deferred;

/**
 * <p>Title: SQLHttpSerializer</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>net.opentsdb.tsd.SQLHttpSerializer</code></p>
 */

public class SQLHttpSerializer extends HttpSerializer {

	/**
	 * Creates a new SQLHttpSerializer
	 */
	public SQLHttpSerializer() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * Creates a new SQLHttpSerializer
	 * @param query
	 */
	public SQLHttpSerializer(HttpQuery query) {
		super(query);
		// TODO Auto-generated constructor stub
	}

	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.tsd.HttpSerializer#initialize(net.opentsdb.core.TSDB)
	 */
	@Override
	public void initialize(TSDB tsdb) {
		// TODO Auto-generated method stub

	}

	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.tsd.HttpSerializer#shutdown()
	 */
	@Override
	public Deferred<Object> shutdown() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.tsd.HttpSerializer#version()
	 */
	@Override
	public String version() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.tsd.HttpSerializer#shortName()
	 */
	@Override
	public String shortName() {
		// TODO Auto-generated method stub
		return null;
	}

}
