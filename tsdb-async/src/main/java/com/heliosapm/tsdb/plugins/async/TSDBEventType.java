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
package com.heliosapm.tsdb.plugins.async;

import java.util.Arrays;

/**
 * <p>Title: TSDBEventType</p>
 * <p>Description: Enumerates the possible TSDB event types published to the AsyncProcessor</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.tsdb.plugins.async.TSDBEventType</code></p>
 */

public enum TSDBEventType {
	/** A long value data point */
	DPOINT_LONG("publish-long", PluginType.PUBLISH),
	/** A double value sdata point */
	DPOINT_DOUBLE("publish-double", PluginType.PUBLISH),
	/** An annotation publication */
	ANNOTATION("publish-annotation", PluginType.PUBLISH),
	/** An annotation index */
	ANNOTATION_INDEX("index-annotation", PluginType.SEARCH),
	/** An annotation deletion */
	ANNOTATION_DELETE("delete-annotation", PluginType.SEARCH),
	/** A TSMeta index */
	TSMETA_INDEX("index-tsmeta", PluginType.SEARCH),
	/** A TSMeta deletion */
	TSMETA_DELETE("delete-tsmeta", PluginType.SEARCH),
	/** A UIDMeta index */
	UIDMETA_INDEX("index-uidmeta", PluginType.SEARCH),
	/** A UIDMeta deletion */
	UIDMETA_DELETE("delete-uidmeta", PluginType.SEARCH),
	/** A search query event */
	SEARCH("search", PluginType.SEARCH);
	
	private TSDBEventType(String shortName, PluginType...pluginTypes) {
		this.pluginTypes = pluginTypes;
		this.shortName = shortName; 
	}
	
	/** An all zeroes bit mask template */
	public static final String BITS = "0000000000000000000000000000000000000000000000000000000000000000";

	
	/** The plugin types this event is targetted at */
	public final PluginType[] pluginTypes;
	/** The short name of the event */
	public final String shortName;
	/** The bitmask for this event type */
	public final int mask = Integer.parseInt("1" + BITS.substring(0, ordinal()), 2);
	
	/** The event mask for all Search events */
	public static final int SEARCH_EVENT_MASK;
	/** The event mask for all Publisher events */
	public static final int PUBLISHER_EVENT_MASK;
	/** The event mask for all events */
	public static final int ALL_EVENT_MASK;
	
	static {
		final TSDBEventType[] types = TSDBEventType.values();
		int srchMask = 0, pubMask = 0, allMask = 0;
		for(TSDBEventType et: types) {
			if(et.isForSearch()) srchMask = (srchMask | et.mask);
			else if(et.isForPublisher()) pubMask = (pubMask | et.mask);
			allMask = (allMask | et.mask);
		}
		SEARCH_EVENT_MASK = srchMask;
		PUBLISHER_EVENT_MASK = pubMask;
		ALL_EVENT_MASK = allMask;				
	}
	
	public static void main(String[] args) {
		for(TSDBEventType t: TSDBEventType.values()) {
			System.out.println(t.name() + "[mask:" + t.mask + ", shortName:" + t.shortName + ", pluginTypes:" + Arrays.toString(t.pluginTypes) + "]" );
			for(TSDBEventType te: TSDBEventType.values()) {
				System.out.println("\t" + t.name() + " enabled for " + te.name() + ": " + t.isEnabled(te.mask));				
			}
			System.out.println("\t" + t.name() + " enabled for ALL: " + t.isEnabled(getMask(TSDBEventType.values())));
		}
	}
	
	
	
	
	/**
	 * Indicates if this event type is targetted at search plugins
	 * @return true if this event type is targetted at search plugins, false otherwise
	 */
	public boolean isForSearch() {
		return Arrays.binarySearch(pluginTypes, PluginType.SEARCH) >= 0;
	}
	
	/**
	 * Indicates if this event type is targetted at dispatcher plugins
	 * @return true if this event type is targetted at dispatcher plugins, false otherwise
	 */
	public boolean isForPublisher() {
		return Arrays.binarySearch(pluginTypes, PluginType.PUBLISH) >= 0;
		
	}
	
	/**
	 * Indicates if this event type is targetted at RPC plugins
	 * @return true if this event type is targetted at RPC plugins, false otherwise
	 */
	public boolean isForRPC() {
		return Arrays.binarySearch(pluginTypes, PluginType.RPC) >= 0;		
	}
	
	
	/**
	 * Generates a selective bitmask for the passed types
	 * @param types The types to create a bitmask for
	 * @return the selective mask
	 */
	public static final int getMask(final TSDBEventType...types) {
		if(types==null || types.length==0) return 0;
		int _mask = 0;
		for(TSDBEventType t: types) {
			if(t==null) continue;
			_mask = _mask | t.mask;
		}
		return _mask;
	}
	
	/**
	 * Indicates if the passed mask is enabled for this event type
	 * @param mask The mask to test
	 * @return true if enabled, false otherwise
	 */
	public final boolean isEnabled(final int mask) {
		return mask == (mask | this.mask);
	}
	
	
	private static final TSDBEventType[] values = values();
	
	
	/**
	 * Decodes the passed ordinal to the corresponding event
	 * @param ord The byte ordinal
	 * @return The corresponding event
	 */
	public static TSDBEventType ordinal(byte ord) {
		try { return values[ord]; } catch (Exception x) {/* No Op */}
		throw new IllegalArgumentException("Invalid TSDBEventType Ordinal [" + ord + "]");
	}
	
	/**
	 * Decodes the passed ordinal to the corresponding event
	 * @param ord The byte ordinal
	 * @return The corresponding event
	 */
	public static TSDBEventType ordinal(Number ord) {
		if(ord==null) throw new IllegalArgumentException("The passed TSDBEventType Ordinal was null");
		return ordinal(ord.byteValue());
	}
	
	/**
	 * Decodes the passed event name the corresponding event
	 * @param value The string value to decode which is trimmed and upper-cased
	 * @return The corresponding event
	 */
	public static TSDBEventType ordinal(CharSequence value) {
		if(value==null || value.toString().trim().isEmpty()) throw new IllegalArgumentException("The passed TSDBEventType name null or empty");
		try {
			return valueOf(value.toString().trim().toUpperCase());
		} catch (Exception ex) {
			throw new IllegalArgumentException("The passed TSDBEventType name [" + value + "] was invalid");
		}
	}

}
