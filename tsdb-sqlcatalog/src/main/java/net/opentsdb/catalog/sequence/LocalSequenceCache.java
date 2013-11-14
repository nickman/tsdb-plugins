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
package net.opentsdb.catalog.sequence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import javax.sql.DataSource;

import org.helios.tsdb.plugins.util.SystemClock;
import org.helios.tsdb.plugins.util.SystemClock.ElapsedTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Title: LocalSequenceCache</p>
 * <p>Description: A local in-vm cache for DB sequence ranges</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>net.opentsdb.catalog.LocalSequenceCache</code></p>
 */

public class LocalSequenceCache {
	/** The local sequence increment */
	protected final int increment;
	/** The DB Sequence name, fully qualified if necessary  */
	protected final String sequenceName;
	/** The current local sequence value */
	protected final AtomicLong currentValue = new AtomicLong(0);
	/** The current ceiling on the local sequence value */
	protected final AtomicLong ceiling = new AtomicLong(0);
	/** Atomic switch indicating a refresh is required, i.e. if true, we're good to go, otherwise a refresh is needed */
	protected final AtomicBoolean rangeIsFresh = new AtomicBoolean(false);
	/** The datasource to provide connections to refresh the sequence cache */
	protected final DataSource dataSource;
	/** The SQL used to retrieve the next sequence value */
	protected String seqSql;
	/** Instance logger */
	protected final Logger log;
	
	
	/**
	 * Creates a new LocalSequenceCache
	 * @param increment The local sequence increment
	 * @param sequenceName The DB Sequence name, fully qualified if necessary
	 * @param dataSource The datasource to provide connections to refresh the sequence cache
	 */
	public LocalSequenceCache(int increment, String sequenceName, DataSource dataSource) {		
		log = LoggerFactory.getLogger(getClass().getName() + "." + sequenceName);
		this.increment = increment;
		this.sequenceName = sequenceName;
		this.dataSource = dataSource;
		init();
		refresh();
		log.info("Created LocalSequenceCache [{}]", sequenceName);
	}
	
	
	/**
	 * Initializes the SQL statement
	 */
	protected void init() {
		seqSql = "SELECT " + sequenceName + ".NEXTVAL FROM DUAL";
	}
	
	/**
	 * Returns the next value in the sequence, refreshing the sequence range if necessary
	 * @return the next value in the sequence
	 */
	public long next() {
		long next = -1;
		for(;;) {
			if(rangeIsFresh.compareAndSet(true, (next = currentValue.incrementAndGet())<=ceiling.get())) {
				return next;
			}
			rangeIsFresh.set(false);
			refresh();
		}
	}
	
	/**
	 * Refreshes the  sequence range
	 */
	protected void refresh() {
		log.info("Refreshing....");
		ElapsedTime et = SystemClock.startClock();
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rset = null;
		final long target = ceiling.get() + increment;
		long retrieved = 0;
		int loops = 0;
		try {
			conn = dataSource.getConnection();
			ps = conn.prepareStatement(seqSql);
			while(retrieved < target) {
				rset = ps.executeQuery();
				rset.next();
				retrieved += rset.getLong(1);
				rset.close(); rset = null;
				loops++;
				if(loops>increment) {
					throw new RuntimeException("Refresh loops exceeded increment [" + loops + "/" + increment + "]");
				}
			}
			ceiling.set(retrieved);
			currentValue.set(retrieved-increment);
			
			rangeIsFresh.set(true);
			log.info("Refreshed loops:{} current:{} ceiling:{}   Elapsed: {} ms.", loops, currentValue.get(), ceiling.get(), et.elapsedMs());
		} catch (Exception ex) {
			throw new RuntimeException("Failed to refresh sequence [" + sequenceName + "]", ex);
		} finally {
			if(rset!=null) try { rset.close(); } catch (Exception x) {/* No Op */}
			if(ps!=null) try { ps.close(); } catch (Exception x) {/* No Op */}
			if(conn!=null) try { conn.close(); } catch (Exception x) {/* No Op */}			
		}
	}
	
	
//	public static void main(String[] args) {
//		// jdbc:oracle:thin:@192.168.1.23:1521:ORCL
//		// oracle.jdbc.driver.OracleDriver
//		Properties p = new Properties();
//		p.setProperty(ICatalogDataSource.JDBC_POOL_JDBCDRIVER, "oracle.jdbc.driver.OracleDriver");
//		p.setProperty(ICatalogDataSource.JDBC_POOL_JDBCURL, "jdbc:oracle:thin:@192.168.1.23:1521:ORCL");
//		p.setProperty(ICatalogDataSource.JDBC_POOL_USERNAME, "TSDB");
//		p.setProperty(ICatalogDataSource.JDBC_POOL_PASSWORD, "tsdb");
//		CatalogDataSource cds = CatalogDataSource.getInstance();
//		cds.initialize(null, p);
//		System.out.println(p);
//		int loops = 1000;
//		LocalSequenceCache lsc = new OracleLocalSequenceCache(50, "TEST_SEQ", cds.getDataSource());
//		Set<Long> sequences = new HashSet<Long>(loops);
//		try {
//			for(int i = 0; i < loops; i++) {
//				long n = lsc.next();
//				if(!sequences.add(n)) {
//					throw new RuntimeException("Unexpected Dup:" + n);
//				}
//				
//			}
//		} finally {
//			cds.shutdown();
//		}
//	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return String.format(
				"LocalSequenceCache [sequenceName=%s, increment=%s]",
				sequenceName, increment);
	}
	
	
	


}
