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

import static net.opentsdb.catalog.builder.Column.ANNID;
import static net.opentsdb.catalog.builder.Column.CREATED;
import static net.opentsdb.catalog.builder.Column.CUSTOM;
import static net.opentsdb.catalog.builder.Column.DATA_TYPE;
import static net.opentsdb.catalog.builder.Column.DESCRIPTION;
import static net.opentsdb.catalog.builder.Column.DISPLAY_NAME;
import static net.opentsdb.catalog.builder.Column.END_TIME;
import static net.opentsdb.catalog.builder.Column.FQN;
import static net.opentsdb.catalog.builder.Column.FQNID;
import static net.opentsdb.catalog.builder.Column.FQN_TP_ID;
import static net.opentsdb.catalog.builder.Column.LAST_UPDATE;
import static net.opentsdb.catalog.builder.Column.MAX_VALUE;
import static net.opentsdb.catalog.builder.Column.METRIC_UID;
import static net.opentsdb.catalog.builder.Column.MIN_VALUE;
import static net.opentsdb.catalog.builder.Column.NAME;
import static net.opentsdb.catalog.builder.Column.NODE;
import static net.opentsdb.catalog.builder.Column.NOTES;
import static net.opentsdb.catalog.builder.Column.PORDER;
import static net.opentsdb.catalog.builder.Column.RETENTION;
import static net.opentsdb.catalog.builder.Column.START_TIME;
import static net.opentsdb.catalog.builder.Column.TAGK;
import static net.opentsdb.catalog.builder.Column.TAGV;
import static net.opentsdb.catalog.builder.Column.TSUID;
import static net.opentsdb.catalog.builder.Column.UNITS;
import static net.opentsdb.catalog.builder.Column.VERSION;
import static net.opentsdb.catalog.builder.Column.XUID;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * <p>Title: Table</p>
 * <p>Description: Enumerates the sql-catalog tables</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>net.opentsdb.catalog.builder.Table</code></p>
 */

public enum Table {
	/** Associative table between TSD_TSMETA and TSD_TAGPAIR, or the TSMeta and the Tag keys and values of the UIDMetas therein */
	TSD_FQN_TAGPAIR("F", FQN_TP_ID, FQNID, XUID, PORDER, NODE),
	/** Table storing the observed unique tag key and value pairs associated with a time-series/TSMeta */
	TSD_TAGPAIR("P", XUID, TAGK, TAGV, NAME),
	/** Table storing distinct time-series metric names */
	TSD_METRIC("M", XUID, VERSION, NAME, CREATED, LAST_UPDATE, DESCRIPTION, DISPLAY_NAME, NOTES, CUSTOM),
	/** Table storing created annotations */
	TSD_ANNOTATION("A", ANNID, VERSION, START_TIME, LAST_UPDATE, DESCRIPTION, NOTES, FQNID, END_TIME, CUSTOM),
	/** Table storing distinct time-series tag keys */
	TSD_TAGK("K", XUID, VERSION, NAME, CREATED, LAST_UPDATE, DESCRIPTION, DISPLAY_NAME, NOTES, CUSTOM),
	/** Table storing distinct time-series tag values */
	TSD_TAGV("V", XUID, VERSION, NAME, CREATED, LAST_UPDATE, DESCRIPTION, DISPLAY_NAME, NOTES, CUSTOM),
	/** Table storing each distinct time-series TSMeta and its attributes */
	TSD_TSMETA("T", FQNID, VERSION, METRIC_UID, FQN, TSUID, CREATED, LAST_UPDATE, MAX_VALUE, MIN_VALUE, DATA_TYPE, DESCRIPTION, DISPLAY_NAME, NOTES, UNITS, RETENTION, CUSTOM);

	public static class FK {
		public final Table table;
		public final Column column;
		/**
		 * Creates a new FK
		 * @param table
		 * @param column
		 */
		public FK(Table table, Column column) {
			this.table = table;
			this.column = column;
		}
	}
	
	public static final Map<Table, FK> JOINS;
	
	static {
		Map<Table, FK> tmp = new EnumMap<Table, FK>(Table.class);
		tmp.put(TSD_TAGPAIR, new FK(TSD_FQN_TAGPAIR,XUID));
		tmp.put(TSD_METRIC, new FK(TSD_TSMETA,METRIC_UID));
		tmp.put(TSD_TAGK, new FK(TSD_TAGPAIR,TAGK));
		tmp.put(TSD_TAGV, new FK(TSD_TAGPAIR,TAGV));
		tmp.put(TSD_TSMETA, new FK(TSD_FQN_TAGPAIR,FQNID));		
		JOINS = Collections.unmodifiableMap(tmp);
	}
	
	private Table(String alias, Column...columns) {
		this.alias = alias;
		this.alias2 = "X" + alias;
		this.columns = Collections.unmodifiableList(new ArrayList<Column>(Arrays.asList(columns)));
		this.pk = columns[0];
		StringBuilder a = new StringBuilder();
		StringBuilder b = new StringBuilder();
		StringBuilder b2 = new StringBuilder();
		for(Column c: columns) {
			a.append(c.name()).append(", ");
			b.append(alias).append(".").append(c.name()).append(", ");			
			b2.append(alias2).append(".").append(c.name()).append(", ");
		}
		columnNames = a.deleteCharAt(a.length()-1).deleteCharAt(a.length()-1).toString();
		allAliasedCsv = b.deleteCharAt(b.length()-1).deleteCharAt(b.length()-1).toString();
		allAliasedCsv2 = b2.deleteCharAt(b2.length()-1).deleteCharAt(b2.length()-1).toString();
		
		// allAliasedCsv
	}
	
	public final String alias;
	public final String alias2;
	public final List<Column> columns;
	public final Column pk;
	public final String allAliasedCsv;
	public final String allAliasedCsv2;
	public final String columnNames;
	
	
}
