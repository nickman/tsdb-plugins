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
package net.opentsdb.catalog.h2.triggers;

import java.sql.Connection;
import java.sql.SQLException;

import net.opentsdb.catalog.sequence.LocalSequenceCache;


/**
 * <p>Title: AbstractSyncQueueTrigger</p>
 * <p>Description: Abstract trigger impl as base class for triggers writing ops to the Sync Queue</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>net.opentsdb.catalog.h2.triggers.AbstractSyncQueueTrigger</code></p>
 */

public abstract class AbstractSyncQueueTrigger extends AbstractTrigger {
	/** The sequence cache providing the value for SYNC_QUEUE.QID */
	protected LocalSequenceCache sequenceCache = null;
	
	
	
    /** The key of the user defined var to flag a connection as the event queue processor */
    public static final String EQ_CONN_FLAG = "eqprocessor";
    /** The key of the user defined var to flag a connection as the sync queue processor */
    public static final String SYNC_CONN_FLAG = "syncprocessor";

	/** The queue table insert template */
	public static final String QUEUE_SQL_TEMPLATE = "INSERT INTO SYNC_QUEUE (QID, EVENT_TYPE, EVENT, OP_TYPE) VALUES (?,?,?,?)";

	/*
	CREATE TABLE IF NOT EXISTS SYNC_QUEUE (
	QID BIGINT NOT NULL COMMENT 'The synthetic identifier for this sync operation',
	EVENT_TYPE VARCHAR(20) NOT NULL 
		COMMENT 'The source of the update that triggered this sync operation'
		CHECK EVENT_TYPE IN ('TSD_ANNOTATION', 'TSD_FQN', 'TSD_METRIC', 'TSD_TAGK', 'TSD_TAGV'), 
	EVENT VARCHAR(120) NOT NULL COMMENT 'The event PK as JSON that triggered this Sync Operation',
	OP_TYPE CHAR(1) NOT NULL
		COMMENT 'The SQL Operation type that triggered this sync operation'
		CHECK OP_TYPE IN ('I', 'D', 'U'), 	
	EVENT_TIME TIMESTAMP AS NOW() NOT NULL COMMENT 'The timestamp when the sync event occured',
	LAST_SYNC_ATTEMPT TIMESTAMP COMMENT 'The last [failed] sync operation attempt timestamp',
	LAST_SYNC_ERROR CLOB COMMENT 'The exception trace of the last failed sync operation'
);

	QID_SEQ INCREMENT BY @QID_SEQ_SIZE;

	 */
	
	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.h2.triggers.AbstractTrigger#init(java.sql.Connection, java.lang.String, java.lang.String, java.lang.String, boolean, int)
	 */
	public void init(Connection conn, String schemaName, String triggerName, String tableName, boolean before, int type) throws SQLException {
		super.init(conn, schemaName, triggerName, tableName, before, type);
		sequenceCache = new LocalSequenceCache()
	}

}
