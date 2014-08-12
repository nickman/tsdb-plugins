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
package net.opentsdb.meta;

import java.util.EnumSet;

import static net.opentsdb.meta.MetaOption.*;

/**
 * <p>Title: MetaType</p>
 * <p>Description: Enumerates the different meta-types. Similar to 
 * {@link net.opentsdb.uid.UniqueId.UniqueIdType} 
 * but intended to define the different data structure types.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>net.opentsdb.meta.MetaType</code></p>
 */

public enum MetaType implements BitMaskEnum {
	/** A metric or tag definition ({@link net.opentsdb.meta.UIDMeta}) */
	UID(XUID, NAME, DISPLAY_NAME, DESCRIPTION, NOTES, CUSTOM, CREATED, TYPE),
	/** An annotation definition ({@link net.opentsdb.meta.Annotation}) */
	ANNOTATION(XUID, NAME, DISPLAY_NAME, DESCRIPTION, NOTES, CUSTOM, CREATED, ENDTIME, STARTTIME),
	/** An TSMeta definition ({@link net.opentsdb.meta.TSMeta}) */
	TSUID(XUID, NAME, DISPLAY_NAME, DESCRIPTION, NOTES, CUSTOM, CREATED, DATATYPE, MAX, MIN, RETENTION, TAGS, UNITS);
	
	private MetaType(MetaOption...metaOptions) {
		bitmask = POW2INT[ordinal()];
		this.metaOptionMask = MetaOption.maskFor(metaOptions);
	}
	
	/** The member mask */
	public final int bitmask;
	/** The member's MetaOption association mask */
	public final int metaOptionMask;
	
	private static final MetaType[] _values = MetaType.values();	
	
	public static void main(String[] args) {
		for(MetaType mt: MetaType.values()) {
			System.out.println(mt.name() + ":" + mt.bitmask + "   [" + Integer.toBinaryString(mt.bitmask) + "]");
		}
	}
	
	/**
	 * Attempts to decode a general object to a MetaType
	 * @param obj An object representing a MetaType's name or ordinal
	 * @return the decoded MetaType
	 */
	public static MetaType decode(Object obj) {
		if(obj==null) throw new IllegalArgumentException("The passed object was null");
		try {
			if(obj instanceof Number) {
				return decode((Number)obj);
			}
			try {
				return valueOf(obj.toString().trim().toUpperCase());
			} catch (IllegalArgumentException ex) {
				return decode(new Double(obj.toString().trim()).intValue());
			}
		} catch (Exception ex) {
			throw new RuntimeException("Failed to decode the MetaType value [" + obj + "]");
		}
	}
	
	/**
	 * Decodes the passed number to a MetaType via their ordinals
	 * @param num The number to decode
	 * @return The decoded MetaType
	 */
	public static MetaType decode(Number num) {
		if(num==null) throw new IllegalArgumentException("The passed number was null");
		final int ord = num.intValue();
		try {
			return _values[ord];
		} catch (Exception x) {
			throw new IllegalArgumentException("The passed number was not a recognized MetaType Ordinal [" + ord + "]");
		}
	}

	
	/**
	 * Returns the member set of types included in the passed bitmask
	 * @param mask The bitmask membership filter
	 * @return a set of member types
	 */
	public static EnumSet<MetaType> members(final int mask) {
		EnumSet<MetaType> set = EnumSet.noneOf(MetaType.class);
		for(MetaType mt: values()) {
			if(mask == (mask | mt.bitmask)) set.add(mt); 
		}
		return set;
	}
	
	/**
	 * Returns this member's set of MetaOptions
	 * @return a set of member types
	 */
	public EnumSet<MetaOption> metaOptions() {
		return MetaOption.members(metaOptionMask);
	}
	
	
}
