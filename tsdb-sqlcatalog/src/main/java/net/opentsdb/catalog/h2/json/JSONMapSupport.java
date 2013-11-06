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
package net.opentsdb.catalog.h2.json;

import java.util.Map;

import net.opentsdb.utils.JSON;

/**
 * <p>Title: JSONMapSupport</p>
 * <p>Description: Defines an adapter for creating SQL custom functions to support JSON stored maps.
 * Based largely on <b><code>org.json.JSONObject</code></b>.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>net.opentsdb.catalog.h2.json.JSONMapSupport</code></p>
 */

public class JSONMapSupport {
	/** A JSON representation of an empty map */
	public static final String EMPTY_MAP = "{}";

	/**
	 * Validates that the named value is not null, or if a {@link CharSequence} insance, 
	 * that it is not empty
	 * @param name The name of the value being validate
	 * @param t The object to validate
	 * @return The validated object
	 * @throws JSONException  thrown if the object fails validation
	 */
	protected static <T> T nvl(String name, T t) throws JSONException {
		if(
			t==null ||
			(t instanceof CharSequence && ((CharSequence)t).toString().trim().isEmpty())
		) throw new JSONException("The passed %s was null or empty", name);
		return t;
	}
	
	/**
	 * Validates that the named value is not null, or if a {@link CharSequence} insance, 
	 * that it is not empty and returns it as a string.
	 * @param name The name of the value being validate
	 * @param t The object to validate
	 * @return The validated object as a string
	 * @throws JSONException  thrown if the object fails validation
	 */
	protected static String nvls(String name, Object t) throws JSONException {
		return nvl(name, t).toString().trim();
	}
	
	
	/**
	 * Parses the passed JSON source and returns as a map
	 * @param source The JSON source to pass
	 * @return the parsed map
	 */
	public static Map<String, String> read(CharSequence source) {
		return JSON.parseToObject(nvl("JSON Source", source).toString().trim(), Map.class);
	}
	
	/**
	 * Serializes the passed map to a JSON string
	 * @param map The map to serialize
	 * @return the JSON representation of the passed map
	 */
	public static String toString(Map<String, String> map) {
		return JSON.serializeToString(nvl("Map", map));		
	}

	/**
	 * Serializes the passed map to a JSON string, 
	 * returning an empty JSON map if the map is null.
	 * @param map The map to serialize
	 * @return the JSON representation of the passed map
	 */
	public static String nokToString(Map<String, String> map) {
		return map==null ? EMPTY_MAP : JSON.serializeToString(nvl("Map", map));		
	}

	/**
	 * Serializes the passed map to JSON bytes
	 * @param map The map to serialize
	 * @return the JSON bytes representation of the passed map
	 */
	public static byte[] toBytes(Map<String, String> map) {
		return JSON.serializeToBytes(nvl("Map", map));		
	}
	
	
	/**
	 * Retrieves the value bound in the map for the passed key
	 * @param key The key to retrieve the bound value for
	 * @param source The JSON source to parse
	 * @return The bound value for the key
	 * @throws JSONException thrown if the key or source is empty or null 
	 * or if the key not bound into the map
	 */
	public static String get(CharSequence key, CharSequence source) {
		return read(source).get(nvls("Key", key));
	}
	
	/**
	 * Puts the passed key/value pair into the map represented by the passed JSON source
	 * @param key The map key
	 * @param value The map value
	 * @param source The JSON source to parse
	 * @return the JSON source of the modified map
	 */
	public static String set(CharSequence key, Object value, CharSequence source) {
		Map<String, String> map = read(source);
		map.put(nvls("Key", key), nvls("Value", value));
		return toString(map);
	}

	
}
