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
package org.helios.tsdb.plugins.event;

import java.util.Arrays;

import gnu.trove.map.hash.TByteObjectHashMap;

/**
 * <p>Title: TSDBEventType</p>
 * <p>Description: Enumerates the event types that can be passed to a plugin from an OpenTSDB instance.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.asynch.TSDBEventType</code></p>
 */

public enum TSDBEventType {
//	/** Plugin Initialization */
//	PLUGIN_INIT,
//	/** Plugin Shutdown */
//	PLUGIN_STOP,	
	/** Stats Collection */
	STATS_COLLECT(PluginType.PUBLISH, PluginType.RPC, PluginType.SEARCH),
	/** A long value data point */
	DPOINT_LONG(PluginType.PUBLISH),
	/** A double value sdata point */
	DPOINT_DOUBLE(PluginType.PUBLISH),
	/** An annotation index */
	ANNOTATION_INDEX(PluginType.SEARCH),
	/** An annotation deletion */
	ANNOTATION_DELETE(PluginType.SEARCH),
	/** A TSMeta index */
	TSMETA_INDEX(PluginType.SEARCH),
	/** A TSMeta deletion */
	TSMETA_DELETE(PluginType.SEARCH),
	/** A UIDMeta index */
	UIDMETA_INDEX(PluginType.SEARCH),
	/** A UIDMeta deletion */
	UIDMETA_DELETE(PluginType.SEARCH),
	/** A search query event */
	SEARCH(PluginType.SEARCH),
	/** Search plugin configuration signal */
	CONFIGURE_SEARCH(PluginType.SEARCH),
	/** RPC plugin configuration signal */
	CONFIGURE_RPC(PluginType.RPC),
	/** Publisher plugin configuration signal */
	CONFIGURE_PUBLISHER(PluginType.PUBLISH);
	
	private TSDBEventType(PluginType...pluginTypes) {
		this.pluginTypes = pluginTypes;
	}
	
	/** The plugin types this event is targetted at */
	public final PluginType[] pluginTypes;
	
	/**
	 * Indicates if this event type is targetted at search plugins
	 * @return true if this event type is targetted at search plugins, false otherwise
	 */
	public boolean isForSearch() {
		if(pluginTypes.length==1) return pluginTypes[0]==PluginType.SEARCH;
		return Arrays.binarySearch(pluginTypes, PluginType.SEARCH) < 0;
	}
	
	/**
	 * Indicates if this event type is targetted at publisher plugins
	 * @return true if this event type is targetted at publisher plugins, false otherwise
	 */
	public boolean isForPulisher() {
		if(pluginTypes.length==1) return pluginTypes[0]==PluginType.PUBLISH;
		return Arrays.binarySearch(pluginTypes, PluginType.PUBLISH) < 0;
	}
	
	/**
	 * Indicates if this event type is targetted at RPC plugins
	 * @return true if this event type is targetted at RPC plugins, false otherwise
	 */
	public boolean isForRPC() {
		if(pluginTypes.length==1) return pluginTypes[0]==PluginType.RPC ;
		return Arrays.binarySearch(pluginTypes, PluginType.RPC) < 0;
	}
	
	
	private static final TByteObjectHashMap<TSDBEventType> ORD2ENUM;
	
	static {
		TSDBEventType[] values = values();
		ORD2ENUM = new TByteObjectHashMap<TSDBEventType>(values.length);
		for(TSDBEventType e: values) {
			ORD2ENUM.put((byte)e.ordinal(), e);
		}
	}
	
	/**
	 * Decodes the passed ordinal to the corresponding event
	 * @param ord The byte ordinal
	 * @return The corresponding event
	 */
	public static TSDBEventType ordinal(byte ord) {
		TSDBEventType t = ORD2ENUM.get(ord);
		if(t==null) throw new IllegalArgumentException("Invalid TSDBEventType Ordinal [" + ord + "]");
		return t;
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
