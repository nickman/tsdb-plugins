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
package org.helios.tsdb.plugins.meta.query;

/**
 * <p>Title: MetaQueryTimeoutException</p>
 * <p>Description: Exception thrown when an {@link IMetaQuery} or its child {@link CancelableIterator} timeout.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.meta.query.MetaQueryTimeoutException</code></p>
 */

public class MetaQueryTimeoutException extends RuntimeException {

	/**  */
	private static final long serialVersionUID = -8572283512075716787L;

	/**
	 * Creates a new MetaQueryTimeoutException
	 */
	public MetaQueryTimeoutException() {

	}

	/**
	 * Creates a new MetaQueryTimeoutException
	 * @param message The exception message
	 */
	public MetaQueryTimeoutException(String message) {
		super(message);
	}

	/**
	 * Creates a new MetaQueryTimeoutException
	 * @param cause The exception underlying cause
	 */
	public MetaQueryTimeoutException(Throwable cause) {
		super(cause);
	}

	/**
	 * Creates a new MetaQueryTimeoutException
	 * @param message The exception message
	 * @param cause The exception underlying cause
	 */
	public MetaQueryTimeoutException(String message, Throwable cause) {
		super(message, cause);
	}


}
