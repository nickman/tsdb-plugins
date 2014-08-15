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

/**
 * <p>Title: Column</p>
 * <p>Description: Enumerates the table columns in the SQL catalog</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>net.opentsdb.catalog.builder.Column</code></p>
 */

public enum Column {
	ANNID,
	CREATED,
	CUSTOM,
	DATA_TYPE,
	DESCRIPTION,
	DISPLAY_NAME,
	END_TIME,
	FQN,
	FQNID,
	FQN_TP_ID,
	LAST_UPDATE,
	MAX_VALUE,
	METRIC_UID,
	MIN_VALUE,
	NAME,
	NODE,
	NOTES,
	PORDER,
	RETENTION,
	START_TIME,
	TAGK,
	TAGV,
	TSUID,
	UNITS,
	VERSION,
	XUID;
	
	
	public String as(String alias) {
		return name() + " as \"" + alias + "\"";
	}
}
