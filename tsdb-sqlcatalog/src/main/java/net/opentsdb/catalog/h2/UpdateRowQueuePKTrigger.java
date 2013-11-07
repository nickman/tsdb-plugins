/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2013, Helios Development Group and individual contributors
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
package net.opentsdb.catalog.h2;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

import org.h2.api.Trigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * <p>Title: UpdateRowQueuePKTrigger</p>
 * <p>Description: H2 trigger to capture the PK of updated TSDB objects and queue them for synching back to the TSDB store.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>net.opentsdb.catalog.h2.UpdateRowQueuePKTrigger</code></p>
 */
public class UpdateRowQueuePKTrigger implements Trigger {
	/** The updated table name the update went to */
	protected String eventSource = null;
	/** The event type recorded to describe the type of the event */
	protected String eventType = null;		
	/** The index of the pk column for the event source */
	protected int pkIndex = -1;
	
	/** Instance logger */
	protected Logger log = null;
	
	/** The queue table insert template */
	public static final String QUEUE_SQL_TEMPLATE = "INSERT INTO SYNC_QUEUE (EVENT_TYPE, EVENT_ID) VALUES (?,?)";
	
	
	/*
CREATE TABLE SYNC_QUEUE (
QID BIGINT IDENTITY COMMENT 'The synthetic identifier for this sync operation',
EVENT_TYPE VARCHAR(20) NOT NULL 
	COMMENT 'The source of the update that triggered this sync operation'
	CHECK EVENT_TYPE IN ('TSD_ANNOTATION', 'TSD_FQN', 'TSD_METRIC', 'TSD_TAGK', 'TSD_TAGV'), 
EVENT_ID VARCHAR2(20) NOT NULL COMMENT 'The PK of the event that triggered this Sync Operation',
LAST_SYNC_ATTEMPT TIMESTAMP COMMENT 'The last [failed] sync operation attempt timestamp',
LAST_SYNC_ERROR CLOB COMMENT 'The exception trace of the last failed sync operation'
);
	 */
	
	/**
	 * {@inheritDoc}
	 * @see org.h2.api.Trigger#init(java.sql.Connection, java.lang.String, java.lang.String, java.lang.String, boolean, int)
	 */
	@Override
	public void init(Connection conn, String schemaName, String triggerName, String tableName, boolean before, int type) throws SQLException {
		log = LoggerFactory.getLogger(getClass().getName() + "." + triggerName);
		eventSource = tableName;
		ResultSet rset = conn.getMetaData().getPrimaryKeys(null, schemaName, tableName);
		rset.next();
		eventType = rset.getString(4);
		rset.close();
		rset = conn.getMetaData().getColumns(null, schemaName, tableName, eventType);
		rset.next();
		pkIndex = rset.getInt(17);
		rset.close();
		log.info("\n\t===============\n\tTrigger Name:{}\n\tTable Name:{}\n\tEvent Type:{}\n\tPK Index:{}\n\t===============", triggerName, tableName, eventType, pkIndex);
		
	}

	/**
	 * {@inheritDoc}
	 * @see org.h2.api.Trigger#fire(java.sql.Connection, java.lang.Object[], java.lang.Object[])
	 */
	@Override
	public void fire(Connection conn, Object[] oldRow, Object[] newRow) throws SQLException {
		if(!Arrays.deepEquals(oldRow, newRow)) {
			log.info("Detected Change In {}:[{}]", eventSource, oldRow[pkIndex]);
			PreparedStatement ps = null;
			try {
				ps = conn.prepareStatement(QUEUE_SQL_TEMPLATE);
				ps.setString(1, eventSource);
				ps.setString(2, oldRow[pkIndex].toString());
				ps.executeUpdate();
			} finally {
				if(ps!=null) try { ps.close(); } catch (Exception ex) {/* No Op */}
			}
		}
		
	}

	/**
	 * {@inheritDoc}
	 * @see org.h2.api.Trigger#close()
	 */
	@Override
	public void close() throws SQLException {
		// TODO Auto-generated method stub
		
	}

	/**
	 * {@inheritDoc}
	 * @see org.h2.api.Trigger#remove()
	 */
	@Override
	public void remove() throws SQLException {
		// TODO Auto-generated method stub
		
	}
	
}

