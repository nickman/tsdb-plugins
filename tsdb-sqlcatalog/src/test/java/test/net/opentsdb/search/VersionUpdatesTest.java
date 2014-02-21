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

import java.lang.management.ManagementFactory;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import net.opentsdb.catalog.AbstractDBCatalog;
import net.opentsdb.catalog.TSDBCatalogSearchEventHandler;
import net.opentsdb.catalog.h2.json.JSONMapSupport;
import net.opentsdb.meta.TSMeta;

import org.helios.tsdb.plugins.util.JMXHelper;
import org.helios.tsdb.plugins.util.SystemClock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

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
	
	/** The custom map key for the version number */
	public static final String VERSION_KEY = AbstractDBCatalog.VERSION_KEY;
	/** The custom map key for the internal pk id */
	public static final String PK_KEY = AbstractDBCatalog.PK_KEY;
	
	/**
	 * Purges the database before each test 
	 */
	@Before
	public void purgeDb() {
		TSDBCatalogSearchEventHandler.getInstance().getDbInterface().purge();
		int fqnCount = jdbcHelper.queryForInt("SELECT COUNT(*) FROM TSD_TSMETA");
		Assert.assertEquals("Unexpected FQN RowCount After Purge", 0, fqnCount);
	}
	
	
	/**
	 * Tests an inserted TSMeta's DB saved custom map values and last update times.
	 * @throws Exception thrown on any error.
	 */
	@Test
	public void testNewTSMetaInsertCustom() throws Exception {
		TSMeta tsMeta = fromUids(objectNameToUIDMeta(JMXHelper.objectName(ManagementFactory.THREAD_MXBEAN_NAME)));
		final long createTimeMs = SystemClock.rtime();
		final long createTimeSec = mstou(createTimeMs);
		
		tsMeta.setCreated(createTimeSec);
		tsdb.indexTSMeta(tsMeta);
		waitForProcessingQueue(name.getMethodName(), 3000, TimeUnit.MILLISECONDS);
		Object[][] rows = jdbcHelper.query("SELECT * FROM TSD_TSMETA WHERE TSUID = '" + tsMeta.getTSUID() + "'");
		Map<String, String> customMap = JSONMapSupport.read(rows[0][TSMETA_CUSTOM].toString()); 
		Assert.assertEquals("The VERSION value", 1,  rows[0][TSMETA_VERSION]);
		Assert.assertEquals("The custom map V value", "1",  customMap.get(VERSION_KEY));
		Assert.assertEquals("The custom map PK value", "" + rows[0][TSMETA_FQNID],  customMap.get(PK_KEY));
		Assert.assertEquals("The create time vs last update", rows[0][TSMETA_CREATED],  rows[0][TSMETA_LAST_UPDATE]);		
		Assert.assertEquals("The create time vs the time we set", createTimeMs, ((Timestamp)rows[0][TSMETA_CREATED]).getTime());
	}
	

}
