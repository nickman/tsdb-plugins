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
package net.opentsdb.catalog.h2.json;

/**
 * <p>Title: JSONException</p>
 * <p>Description: JSON Operation runtime exception</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>net.opentsdb.catalog.h2.json.JSONException</code></p>
 */

public class JSONException extends RuntimeException {

	/**  */
	private static final long serialVersionUID = -723251312717375679L;

	/**
	 * Creates a new JSONException
	 */
	public JSONException() {
	}

	/**
	 * Creates a new JSONException
	 * @param format The message format
	 * @param args The message arguments
	 */
	public JSONException(String format, Object...args) {
		super(String.format(format, args));
	}

	/**
	 * Creates a new JSONException
	 * @param cause The underlying cause
	 */
	public JSONException(Throwable cause) {
		super(cause);
	}

	/**
	 * Creates a new JSONException
	 * @param message The exception error message
	 * @param cause The underlying cause
	 */
	public JSONException(String message, Throwable cause) {
		super(message, cause);
	}


}
