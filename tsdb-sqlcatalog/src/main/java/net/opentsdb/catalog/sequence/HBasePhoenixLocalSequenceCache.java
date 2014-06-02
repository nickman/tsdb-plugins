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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import javax.sql.DataSource;

import net.opentsdb.catalog.SQLWorker;

import org.helios.tsdb.plugins.util.SystemClock;
import org.helios.tsdb.plugins.util.SystemClock.ElapsedTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Title: HBasePhoenixLocalSequenceCache</p>
 * <p>Description: Local sequence cache for HBase.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>net.opentsdb.catalog.sequence.HBasePhoenixLocalSequenceCache</code></p>
 */

public class HBasePhoenixLocalSequenceCache implements ISequenceCache {
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
	/** The sql worker */
	protected final SQLWorker sqlWorker;
	/** The name of the sequence table */
	protected final String seqTableName;
	/** The upsert statement */
	protected final String upsertSql;
	/** The select statement */
	protected final String selectSql;
	
	/** The format of sequence tables */
	public static final String SEQ_TABLE_FORMAT = "CREATE TABLE IF NOT EXISTS %s (S BIGINT NOT NULL PRIMARY KEY, Q BIGINT NOT NULL)";
	
	
	/**
	 * Creates a new HBasePhoenixLocalSequenceCache
	 * @param increment The local sequence increment
	 * @param sequenceName The DB Sequence name, fully qualified if necessary
	 * @param dataSource The datasource to provide connections to refresh the sequence cache
	 */
	public HBasePhoenixLocalSequenceCache(int increment, String sequenceName, DataSource dataSource) {		
		log = LoggerFactory.getLogger(getClass().getName() + "." + sequenceName);
		this.increment = increment;
		this.sequenceName = sequenceName;
		seqTableName = String.format("%s_TAB_", sequenceName);
		upsertSql = String.format("UPSERT INTO %s VALUES (1, NEXT VALUE FOR FQN_SEQ)", seqTableName);
		selectSql = String.format("SELECT Q FROM TSD_SQ WHERE S = 1", seqTableName);
		this.dataSource = dataSource;
		sqlWorker = SQLWorker.getInstance(this.dataSource);
		init();
		refresh();
		log.info("Created HBasePhoenixLocalSequenceCache [{}]", sequenceName);
	}
	
	/**
	 * Resets this sequence cache (but not the underlying DB sequence)
	 */
	public void reset() {
		currentValue.set(0);
		ceiling.set(0);
		rangeIsFresh.set(false);
	}

	
	/**
	 * Initializes the SQL statement
	 */
	protected void init() {
		sqlWorker.execute(String.format(SEQ_TABLE_FORMAT, seqTableName));
		sqlWorker.execute(upsertSql);		
	}
	
	/**
	 * Returns the next value in the sequence, refreshing the sequence range if necessary
	 * @return the next value in the sequence
	 */
	public long next() {
		long next = -1;
		for(;;) {
			if(rangeIsFresh.compareAndSet(true, (next = currentValue.incrementAndGet())<ceiling.get())) {
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
		log.info("Refreshing....Current: [{}]", currentValue.get());
		ElapsedTime et = SystemClock.startClock();
		final long target = ceiling.get() + increment;
		long retrieved = 0;
		int loops = 0;
		try {
			while(retrieved < target) {
				sqlWorker.execute(upsertSql);
				retrieved += sqlWorker.sqlForLong(selectSql);
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
		}
	}
	
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

/*
 * CREATE TABLE IF NOT EXISTS TSD_SQ (S BIGINT NOT NULL PRIMARY KEY, Q BIGINT NOT NULL);
 * UPSERT INTO TSD_SQ VALUES (1, NEXT VALUE FOR FQN_SEQ);
 * SELECT Q FROM TSD_SQ WHERE S = 1;
 * 
 */ 
