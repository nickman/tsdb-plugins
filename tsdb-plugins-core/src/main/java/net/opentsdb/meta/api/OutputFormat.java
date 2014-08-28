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

/**
 * <p>Title: OutputFormat</p>
 * <p>Description: The format of the format data</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>net.opentsdb.meta.api.OutputFormat</code></p>
 */

public enum OutputFormat {
	/** A serialized Java list of maps of each result in the format */
	LIST,
	/** A JSON representation of each result in the format */
	JSON,
	/** The native OpenTSDB POJOS */
	POJO;
	
	private static final OutputFormat[] _values = OutputFormat.values();
	
	/**
	 * Decodes the passed number to a OutputFormat via their ordinals
	 * @param num The number to decode
	 * @return The decoded OutputFormat
	 */
	public static OutputFormat decode(Number num) {
		if(num==null) throw new IllegalArgumentException("The passed number was null");
		final int ord = num.intValue();
		try {
			return _values[ord];
		} catch (Exception x) {
			throw new IllegalArgumentException("The passed number was not a recognized OutputFormat Ordinal [" + ord + "]");
		}
	}
	
}
