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
		String value =  read(source).get(nvls("Key", key));
		if(value==null) throw new JSONException("No value found for key [%s]", key);
		return value;
	}
	
	/**
	 * Retrieves the value bound in the map for the passed key, returning null if a bound value is not found
	 * @param key The key to retrieve the bound value for
	 * @param source The JSON source to parse
	 * @return The bound value for the key
	 * @throws JSONException thrown if the key or source is empty or null 
	 * or if the key not bound into the map
	 */
	public static String getOrNull(CharSequence key, CharSequence source) {
		return read(source).get(nvls("Key", key));
	}
	
	/**
	 * Returns the keys of the map as a string array
	 * @param source The source of the json map
	 * @return an array of the keys in the map
	 */
	public static String[] keys(CharSequence source) {
		return read(source).keySet().toArray(new String[0]);
	}
	
	/**
	 * Returns the values of the map as a string array
	 * @param source The source of the json map
	 * @return an array of the values in the map
	 */
	public static String[] values(CharSequence source) {
		return read(source).values().toArray(new String[0]);
	}
	
	/**
	 * Returns the keys and values of the map as a 2D string array
	 * where <b><code>arr[n][0]</code></b> is the key and <b><code>arr[n][1]</code></b> is the value.
	 * <pre>
           +--+--+--+--+..+--+
           |0 |k0|k1|k2|..|kn|
           +--+--+--+--+..+--+
           +--+--+--+--+..+--+
           |1 |v0|v1|v2|..|vn|
           +--+--+--+--+..+--+
	 * </pre>
	 * @param source The source of the json map
	 * @return a 2D array of the keys and values in the map.
	 */
	public static String[][] pairs(CharSequence source) {
		Map<String, String> map = read(source);
		String[][] arr = new String[map.size()][];
		int cnt = 0;
		for(Map.Entry<String, String> entry: map.entrySet()) {
			arr[cnt] = new String[]{entry.getKey(), entry.getValue()};
			cnt++;
		}
		return arr;
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

	/**
	 * Determines if the JSON Map contains the passed key
	 * @param key The mapped key
	 * @param source The JSON source to parse
	 * @return true if the key was found, false otherwise
	 */
	public static boolean containsKey(CharSequence key, CharSequence source) {
		return read(source).containsKey(nvls("Key", key));
	}
	
	/**
	 * Determines if all the passed keys are present in the JSON Map.
	 * Will return false if no keys are passed or if any of the keys are null or empty
	 * @param source The JSON source to parse
	 * @param keys The keys to look for
	 * @return true if all keys were found, false otherwise
	 */
	public static boolean containsAllKeys(CharSequence source, String...keys) {
		if(keys==null || keys.length==0) return false;
		Map<String, String> map = read(source);
		if(map.isEmpty()) return false;
		for(String s: keys) {
			if(s==null || s.trim().isEmpty()) return false;
			if(!map.containsKey(s.trim())) return false;
		}
		return true;
	}
	
	/**
	 * Determines if any of the passed keys are present in the JSON Map.
	 * Will return false if no keys are passed or if any of the keys are null or empty
	 * @param source The JSON source to parse
	 * @param keys The keys to look for
	 * @return true if any key is found, false otherwise
	 */
	public static boolean containsAnyKeys(CharSequence source, String...keys) {
		if(keys==null || keys.length==0) return false;
		Map<String, String> map = read(source);
		if(map.isEmpty()) return false;
		for(String s: keys) {
			if(s==null || s.trim().isEmpty()) continue;
			if(map.containsKey(s.trim())) return true;
		}
		return false;
	}
	
	
	/**
	 * Determines if all the passed values are present in the JSON Map.
	 * Will return false if no values are passed or if any of the values are null
	 * @param source The JSON source to parse
	 * @param values The values to look for
	 * @return true if all values were found, false otherwise 
	 */
	public static boolean containsAllValues(CharSequence source, String...values) {
		if(values==null || values.length==0) return false;
		Map<String, String> map = read(source);
		if(map.isEmpty()) return false;
		for(String s: values) {
			if(s==null) return false;
			if(!map.containsValue(s.trim())) return false;
		}
		return true;
	}
	
	/**
	 * Determines if any of the passed values are present in the JSON Map.
	 * Will return false if no values are passed or if any of the values are null
	 * @param source The JSON source to parse
	 * @param values The values to look for
	 * @return true if any value was found, false otherwise 
	 */
	public static boolean containsAnyValues(CharSequence source, String...values) {
		if(values==null || values.length==0) return false;
		Map<String, String> map = read(source);
		if(map.isEmpty()) return false;
		for(String s: values) {
			if(s==null) return false;
			if(map.containsValue(s.trim())) return true;
		}
		return false;
	}
	
	
	
}
