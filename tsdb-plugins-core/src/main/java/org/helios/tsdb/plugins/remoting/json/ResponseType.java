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
package org.helios.tsdb.plugins.remoting.json;

/**
 * <p>Title: ResponseType</p>
 * <p>Description: Enumerates the JSON response types</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.remoting.json.ResponseType</code></p>
 */

public enum ResponseType {
	/** Response flag for an error message */
	ERR("err"),  // undefined continuation
	/** Response flag for a request response */
	RESP("resp"), // one time response
	/** Response flag for a request response with a finite number of responses */
	MRESP("mresp"), // n-times response ending with a final event
	/** Response flag for a request response with a finite number of responses */
	XMRESP("xmresp"), // final event on an MRESP sequence	
	/** Response flag for a subscription event delivery */
	SUB("sub"),  // infinite response
	/** Response flag for a subscription start confirm */
	SUB_STARTED("subst"),  // starts a sequence of infinite responses 
	/** Response flag for a subscription stop notification */
	SUB_STOPPED("xmsub"), // ends a sequence of infinite responses
	/** Response flag for a growl */
	GROWL("growl");  // infinite response, no starter, no ender
	
	
	private ResponseType(final String code) {
		this.code = code;
	}
	
	/** The code for the response type */
	public final String code;

}
