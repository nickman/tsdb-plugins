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
package net.opentsdb.meta.api;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

/**
 * <p>Title: MetaOption</p>
 * <p>Description: Defines the levels of meta data that can be requested when issuing a meta-api request</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>net.opentsdb.meta.api.MetaOption</code></p>
 */

public enum MetaOption implements BitMaskEnum {
	// ============================
	//  All types
	// ============================
	/** The TSMeta TSUID or UIDMeta UID */
	XUID,
	/** The name of the meta-object */
	NAME,
	/** The display name of the meta-object */
	DISPLAY_NAME,
	/** The description of the meta-object */
	DESCRIPTION,
	/** The recorded notes about the meta-object */
	NOTES,
	/** The custom content of the meta-object */
	CUSTOM,
	/** The created timestamp of the meta-object */
	CREATED,
	
	// ============================
	// TSMeta
	// ============================
	/** The designated data type definition of a TSMeta */
	DATATYPE,
	/** The maximum value of a TSMeta */
	MAX,
	/** The minimum value of a TSMeta */
	MIN,
	/** The designated retion time of a TSMeta */
	RETENTION,
	/** The associated tags (name/value pairs) of a a TSMeta  */
	TAGS,
	/** The unit for values recorded for a TSMeta */
	UNITS,
	// ============================
	// UIDMeta
	// ============================
	/** The type of a UIDMeta ({@link net.opentsdb.uid.UniqueId.UniqueIdType}) */
	TYPE,	
	// ============================
	// Annotation
	// ============================
	/** The end timestamp of an annotation */
	ENDTIME,
	/** The starting timestamp of an annotation */
	STARTTIME;
	
	private static final MetaOption[] _values = MetaOption.values();
	
	private MetaOption() {
		bitmask = POW2INT[ordinal()];
	}
	
	/** The member mask */
	public final int bitmask;
	
	private static final Set<MetaOption> defaults = new HashSet<MetaOption>(Arrays.asList(
			XUID, NAME
	));
	
	/** The membership bitmask for the default MetaOptions */
	public static final int defaultBitmask = maskFor(defaults);

	
	public static void main(String[] args) {
		for(MetaOption mt: MetaOption.values()) {
			System.out.println(mt.name() + ":" + mt.bitmask + "   [" + Integer.toBinaryString(mt.bitmask) + "]");
		}
	}
	
	/**
	 * Returns the default MetaOptions
	 * @return the default MetaOptions
	 */
	public static EnumSet<MetaOption> defaultOptions() {
		return EnumSet.copyOf(defaults);
	}
	
	/**
	 * Returns a bitmask int representing all the passed MetaOptions.
	 * @param metaOptions The MetaOptions to build a mask for
	 * @return the bitmask
	 */
	public static int maskFor(MetaOption...metaOptions) {
		if(metaOptions==null || metaOptions.length==0) return 0;
		return maskFor(Arrays.asList(metaOptions));
	}
	
	/**
	 * Returns a bitmask int representing all the passed MetaOptions.
	 * @param metaOptions The MetaOptions to build a mask for
	 * @return the bitmask
	 */
	public static int maskFor(Collection<MetaOption> metaOptions) {
		if(metaOptions==null || metaOptions.isEmpty()) return 0;
		int mask = 0;
		for(MetaOption mo: metaOptions) {
			if(mo==null) continue;
			mask = (mask | mo.bitmask);
		}
		return mask;
	}

	/**
	 * Returns a bitmask int representing all the passed MetaOptions, 
	 * or the default membership bitmask if the evaluated is zero.
	 * @param metaOptions The MetaOptions to build a mask for
	 * @return the bitmask
	 */
	public static int maskOrDefaultFor(Collection<MetaOption> metaOptions) {
		int mask = maskFor(metaOptions);
		return mask==0 ? defaultBitmask : mask;
	}
	
	/**
	 * Returns a bitmask int representing all the passed MetaOptions, 
	 * or the default membership bitmask if the evaluated is zero.
	 * @param metaOptions The MetaOptions to build a mask for
	 * @return the bitmask
	 */
	public static int maskOrDefaultFor(MetaOption...metaOptions) {
		int mask = maskFor(metaOptions);
		return mask==0 ? defaultBitmask : mask;
	}
	
	
	/**
	 * Returns the member set of types included in the passed bitmask
	 * @param mask The bitmask membership filter
	 * @return a set of member types
	 */
	public static EnumSet<MetaOption> members(final int mask) {
		EnumSet<MetaOption> set = EnumSet.noneOf(MetaOption.class);
		for(MetaOption mt: _values) {
			if(mask == (mask | mt.bitmask)) set.add(mt); 
		}
		return set;
	}
	
	public static String list(final int mask) {
		return members(mask).toString().replace("[", "").replace("]", "");
	}
	
	/**
	 * Attempts to decode a general object to a MetaOption
	 * @param obj An object representing a MetaOption's name or ordinal
	 * @return the decoded MetaOption
	 */
	public static MetaOption decode(Object obj) {
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
			throw new RuntimeException("Failed to decode the MetaOption value [" + obj + "]");
		}
	}
	
	/**
	 * Decodes the passed number to a MetaOption via their ordinals
	 * @param num The number to decode
	 * @return The decoded MetaOption
	 */
	public static MetaOption decode(Number num) {
		if(num==null) throw new IllegalArgumentException("The passed number was null");
		final int ord = num.intValue();
		try {
			return _values[ord];
		} catch (Exception x) {
			throw new IllegalArgumentException("The passed number was not a recognized MetaOption Ordinal [" + ord + "]");
		}
	}

}
