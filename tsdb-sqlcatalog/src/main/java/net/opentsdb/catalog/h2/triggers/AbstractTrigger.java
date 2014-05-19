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
import java.sql.Statement;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.ObjectName;

import org.h2.api.Trigger;
import org.helios.jmx.util.unsafe.collections.ConcurrentLongSlidingWindow;
import org.helios.tsdb.plugins.util.ConfigurationHelper;
import org.helios.tsdb.plugins.util.JMXHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Title: AbstractTrigger</p>
 * <p>Description: Base JMX enabled H2 trigger impl.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>net.opentsdb.catalog.h2.triggers.AbstractTrigger</code></p>
 */

public abstract class AbstractTrigger implements Trigger, AbstractTriggerMBean {
	/** The trigger's JMX ObjectName */
	protected ObjectName on;
	/** Instance logger */
	protected final Logger log = LoggerFactory.getLogger(getClass());
	/** A counter of the number of calls to this trigger */
	protected final AtomicLong callCount = new AtomicLong(0L);
	/** A sliding window of elapsed times in ns. for calls to this trigger */
	protected ConcurrentLongSlidingWindow elapsedTimes;
	
	/** The schema name where the trigger resides */
	protected String schemaName = null;
	/** The H2 trigger name */
	protected String triggerName = null; 
	/** The table name that the trigger is attached to  */
	protected String tableName = null;
	/** Indicates if the trigger is fired before the op, or after  */
	protected boolean before = false;
	/** The operation types that this trigger is fired on */
	protected int type = -1;
	
	/**
	 * Creates a new AbstractTrigger
	 */
	protected AbstractTrigger() {
		log.info("Created Trigger [" + getClass().getSimpleName() + "]");
	}
	
	
	/**
	 * Returns the number of calls to this trigger
	 * @return the number of calls to this trigger
	 */
	@Override
	public long getCallCount() {
		return callCount.get();
	}
	
	/**
	 * Returns the rolling average elapsed time in ns. of trigger ops
	 * @return the rolling average elapsed time in ns. of trigger ops
	 */
	public long getAverageElapsedTimeNs() {
		return elapsedTimes.avg();
	}
	
	/**
	 * Returns the last elapsed time in ns. of trigger ops
	 * @return the last elapsed time in ns. of trigger ops
	 */
	public long getLastElapsedTimeNs() {
		return elapsedTimes.getFirst();
	}
	
	/**
	 * Returns the rolling average elapsed time in ms. of trigger ops
	 * @return the rolling average elapsed time in ms. of trigger ops
	 */
	public long getAverageElapsedTimeMs() {
		return TimeUnit.MILLISECONDS.convert(getAverageElapsedTimeNs(), TimeUnit.NANOSECONDS);
	}
	
	/**
	 * Returns the last elapsed time in ms. of trigger ops
	 * @return the last elapsed time in ms. of trigger ops
	 */
	public long getLastElapsedTimeMs() {
		return TimeUnit.MILLISECONDS.convert(getLastElapsedTimeNs(), TimeUnit.NANOSECONDS);
	}
	
	/**
	 * Returns the rolling average elapsed time in us. of trigger ops
	 * @return the rolling average elapsed time in us. of trigger ops
	 */
	public long getAverageElapsedTimeUs() {
		return TimeUnit.MICROSECONDS.convert(getAverageElapsedTimeNs(), TimeUnit.NANOSECONDS);
	}
	
	/**
	 * Returns the last elapsed time in us. of trigger ops
	 * @return the last elapsed time in us. of trigger ops
	 */
	public long getLastElapsedTimeUs() {
		return TimeUnit.MICROSECONDS.convert(getLastElapsedTimeNs(), TimeUnit.NANOSECONDS);
	}
	
	/**
	 * Returns the rolling 90th percentile elapsed time in ns.
	 * @return the rolling 90th percentile elapsed time in ns.
	 */
	public long get90PercentileElapsedTimeNs() {
		return elapsedTimes.percentile(90);
	}

	/**
	 * {@inheritDoc}
	 * @see org.h2.api.Trigger#init(java.sql.Connection, java.lang.String, java.lang.String, java.lang.String, boolean, int)
	 */
	@Override
	public void init(Connection conn, String schemaName, String triggerName, String tableName, boolean before, int type) throws SQLException {
		this.schemaName = schemaName;
		this.triggerName = triggerName;
		this.tableName = tableName;
		this.before = before;
		this.type = type;
		on = JMXHelper.objectName(new StringBuilder(getClass().getPackage().getName()).append(":table=").append(tableName).append(",type=").append(TriggerOp.getEnabledStatesName(type)));
		if(JMXHelper.getHeliosMBeanServer().isRegistered(on)) {
			try { JMXHelper.getHeliosMBeanServer().unregisterMBean(on); } catch (Exception ex) {/* No Op */}
		}
		try {
			JMXHelper.getHeliosMBeanServer().registerMBean(this, on);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to register H2 Trigger [" + on + "]", ex);
		}
		int slidingWindowSize = ConfigurationHelper.getIntSystemThenEnvProperty("h2.trigger.elpased.slidingwindowsize", 150);
		elapsedTimes = new ConcurrentLongSlidingWindow(slidingWindowSize);
		log.info("Initialized Trigger [" + getClass().getSimpleName() + "]  Type [" + TriggerOp.getEnabledStatesName(type) + "]");
		
	}
	
	/**
	 * Sets the passed user defined variable in the passed connection's session
	 * @param conn The connection to set the vars in
	 * @param key The variable key
	 * @param value The variable value
	 */
	protected void setUserDefinedVar(Connection conn, String key, Object value) {
		setUserDefinedVars(conn, Collections.singletonMap(key, value));
	}
	
	
	/**
	 * Sets the passed user defined variables in the passed connection's session
	 * @param conn The connection to set the vars in
	 * @param userDefinedVars A map of variables to set
	 */
	protected void setUserDefinedVars(Connection conn, Map<String, Object> userDefinedVars) {
		if(userDefinedVars==null) throw new IllegalArgumentException("User Vars was null");
		Statement st = null;
		try {
			st = conn.createStatement();
			String format = null;
			for(Map.Entry<String, Object> entry:  userDefinedVars.entrySet()) {
				if(entry.getValue() instanceof CharSequence) {
					format =  "SET @%s = '%s';";
				} else {
					format =  "SET @%s = %s;";
				}
				st.execute(String.format(format, entry.getKey(), entry.getValue()));
				log.debug("Set UDV [{}]=[{}]", entry.getKey(), entry.getValue());
			}						
		} catch (Exception ex) {
			throw new RuntimeException("Failed to set user defined vars [" + userDefinedVars + "]", ex);
		} finally {
			if(st!=null) try { st.close(); } catch (Exception x) { /* No Op */ }
		}
	}
	
	
	/**
	 * Returns this trigger's JMX {@link ObjectName}
	 * @return this trigger's JMX {@link ObjectName}
	 */
	@Override
	public ObjectName getOn() {
		return on;
	}

	/**
	 * Returns the schema that this trigger is installed in
	 * @return the schema that this trigger is installed in
	 */
	@Override
	public String getSchemaName() {
		return schemaName;
	}

	/**
	 * Returns the name of the trigger
	 * @return the name of the trigger
	 */
	@Override
	public String getTriggerName() {
		return triggerName;
	}

	/**
	 * Returns the table that this trigger is attached to
	 * @return the table that this trigger is attached to
	 */
	@Override
	public String getTableName() {
		return tableName;
	}

	/**
	 * Indicates if this trigger is fired before the operation, or after
	 * @return true if this trigger is fired before the operation, false if after
	 */
	@Override
	public boolean isBefore() {
		return before;
	}

	/**
	 * Returns the bitmask of the operations that this trigger fires on
	 * @return the bitmask of the operations that this trigger fires on
	 */
	@Override
	public int getType() {
		return type;
	}
	
	/**
	 * Returns the names of the operations that this trigger fires on
	 * @return the names of the operations that this trigger fires on
	 */
	@Override
	public String getTypeNames() {
		return TriggerOp.getEnabledStatesName(type);
	}

	/**
	 * {@inheritDoc}
	 * @see org.h2.api.Trigger#close()
	 */
	@Override
	public void close() throws SQLException {
		/* No Op */			
	}

	/**
	 * {@inheritDoc}
	 * @see org.h2.api.Trigger#remove()
	 */
	@Override
	public void remove() throws SQLException {
		/* No Op */			
	}		
}
