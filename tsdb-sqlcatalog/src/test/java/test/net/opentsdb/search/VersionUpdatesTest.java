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
package test.net.opentsdb.search;

/**
 * <p>Title: VersionUpdatesTest</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>test.net.opentsdb.search.VersionUpdatesTest</code></p>
 */

public class VersionUpdatesTest extends CatalogBaseTest {
	/*
	 * New insert 
	 * 	has V1
	 * 	created = last_update
	 * 
	 * From TSDB Update
	 * 	if VERSION > V1, UPDATES V1, No change to LAST_UPDATE
	 * 
	 * 
	 * External Update
	 * 	VERSION = VERSON+1
	 * 	LAST_UPDATE ticks (and is > than UPDATE TRACKER)
	 * 	SYNCH is called to update TSDB and Updates comes back in as From TSDB Update
	 * 
	 */
	/**
	 * Creates a new VersionUpdatesTest
	 */
	public VersionUpdatesTest() {
		// TODO Auto-generated constructor stub
	}

}
