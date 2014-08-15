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
package net.opentsdb.catalog.builder;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * <p>Title: SQLBuilder</p>
 * <p>Description: Fluent style SQL builder</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>net.opentsdb.catalog.builder.SQLBuilder</code></p>
 */

public class SQLBuilder {
	protected final StringBuilder sqlBuffer = new StringBuilder();
	protected final Stack<Ops> opStack = new Stack<Ops>();
	protected final List<Object> binds = new ArrayList<Object>();
	
	
	public SQLBuilder selectColumns() {
		
		return this;
	}
	
	
	
	/**
	 * Creates and returns a new SQLBuilder
	 * @return a new SQLBuilder
	 */
	public static SQLBuilder newBuilder() {
		return new SQLBuilder();
	}
	
	/**
	 * Creates a new SQLBuilder
	 */
	private SQLBuilder() {
		
	}

}
