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
	public static Map<String, String> read(String source) {
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
	public static String get(CharSequence key, String source) {
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
	public static String getOrNull(CharSequence key, String source) {
		return read(source).get(nvls("Key", key));
	}
	
	/**
	 * Returns the keys of the map as a string array
	 * @param source The source of the json map
	 * @return an array of the keys in the map
	 */
	public static String[] keys(String source) {
		return read(source).keySet().toArray(new String[0]);
	}
	
	/**
	 * Returns the values of the map as a string array
	 * @param source The source of the json map
	 * @return an array of the values in the map
	 */
	public static String[] values(String source) {
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
	public static String[][] pairs(String source) {
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
	public static String set(CharSequence key, Object value, String source) {
		Map<String, String> map = read(source);
		map.put(nvls("Key", key), nvls("Value", value));
		return toString(map);
	}
	
	/**
	 * Puts the passed key/value pair into the map represented by the passed JSON source
	 * @param key The map key
	 * @param value The map value
	 * @param map The map
	 * @return the JSON source of the modified map
	 */
	public static String set(CharSequence key, Object value, Map<String, String> map) {
		map.put(nvls("Key", key), nvls("Value", value));
		return toString(map);
	}
	

	/**
	 * Determines if the JSON Map contains the passed key
	 * @param key The mapped key
	 * @param source The JSON source to parse
	 * @return true if the key was found, false otherwise
	 */
	public static boolean containsKey(CharSequence key, String source) {
		return read(source).containsKey(nvls("Key", key));
	}
	
	/**
	 * Determines if all the passed keys are present in the JSON Map.
	 * Will return false if no keys are passed or if any of the keys are null or empty
	 * @param source The JSON source to parse
	 * @param keys The keys to look for
	 * @return true if all keys were found, false otherwise
	 */
	public static boolean containsAllKeys(String source, String...keys) {
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
	public static boolean containsAnyKeys(String source, String...keys) {
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
	public static boolean containsAllValues(String source, String...values) {
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
	public static boolean containsAnyValues(String source, String...values) {
		if(values==null || values.length==0) return false;
		Map<String, String> map = read(source);
		if(map.isEmpty()) return false;
		for(String s: values) {
			if(s==null) return false;
			if(map.containsValue(s.trim())) return true;
		}
		return false;
	}
	
	/**
	 * Increments the int in the map bound to the passed key by the specified amount
	 * @param source The map JSON source
	 * @param incr The amount to increment by
	 * @param key The key that the int is bound to in the map
	 * @return the updated map JSON source
	 */
	public static String increment(String source, int incr, String key) {
		Map<String, String> map = read(source);
		int value = Integer.parseInt(map.get(nvl("Key", key))) + incr;
		map.put(key, Integer.toString(value));
		return JSON.serializeToString(map);	
	}
	
	/**
	 * Increments the int in the map bound to the passed key by the specified amount
	 * @param map The map to update
	 * @param incr The amount to increment by
	 * @param key The key that the int is bound to in the map
	 * @return the updated map JSON source
	 */
	public static String increment(Map<String, String> map, int incr, String key) {
		int value = Integer.parseInt(map.get(nvl("Key", key))) + incr;
		map.put(key, Integer.toString(value));
		return JSON.serializeToString(map);	
	}
	
	
	/**
	 * Increments the long in the map bound to the passed key by the specified amount
	 * @param source The map JSON source
	 * @param incr The amount to increment by
	 * @param key The key that the long is bound to in the map
	 * @return the updated map JSON source
	 */
	public static String increment(String source, long incr, String key) {
		Map<String, String> map = read(source);
		long value = Long.parseLong(map.get(nvl("Key", key))) + incr;
		map.put(key, Long.toString(value));
		return JSON.serializeToString(map);
	}
	
	/**
	 * Increments the double in the map bound to the passed key by the specified amount
	 * @param source The map JSON source
	 * @param incr The amount to increment by
	 * @param key The key that the double is bound to in the map
	 * @return the updated map JSON source
	 */
	public static String increment(String source, double incr, String key) {
		Map<String, String> map = read(source);
		double value = Double.parseDouble(map.get(nvl("Key", key))) + incr;
		map.put(key, Double.toString(value));
		return JSON.serializeToString(map);
	}
	
	/**
	 * Increments the float in the map bound to the passed key by the specified amount
	 * @param source The map JSON source
	 * @param incr The amount to increment by
	 * @param key The key that the float is bound to in the map
	 * @return the updated map JSON source
	 */
	public static String increment(String source, float incr, String key) {
		Map<String, String> map = read(source);
		float value = Float.parseFloat(map.get(nvl("Key", key))) + incr;
		map.put(key, Float.toString(value));
		return JSON.serializeToString(map);
	}
	
	
	/**
	 * Increments the int in the map bound to the passed key by 1
	 * @param source The map JSON source
	 * @param key The key that the int is bound to in the map
	 * @return the updated map JSON source
	 */
	public static String incrementInt(String source, String key) {
		return increment(source, 1, key);		
	}
	
	/**
	 * Increments the number in the map bound to the passed key by 1
	 * @param source The map JSON source
	 * @param key The key that the number is bound to in the map
	 * @return the updated map JSON source
	 */
	public static String incrementLong(String source, String key) {
		return increment(source, 1L, key);		
	}
	
	/**
	 * Increments the number in the map bound to the passed key by 1
	 * @param source The map JSON source
	 * @param key The key that the number is bound to in the map
	 * @return the updated map JSON source
	 */
	public static String incrementFloat(String source, String key) {
		return increment(source, 1F, key);		
	}

	/**
	 * Increments the number in the map bound to the passed key by 1
	 * @param source The map JSON source
	 * @param key The key that the number is bound to in the map
	 * @return the updated map JSON source
	 */
	public static String incrementDouble(String source, String key) {
		return increment(source, 1D, key);		
	}

	/**
	 * Decrements the number in the map bound to the passed key by 1
	 * @param source The map JSON source
	 * @param key The key that the number is bound to in the map
	 * @return the updated map JSON source
	 */
	public static String decrementInt(String source, String key) {
		return increment(source, -1, key);		
	}

	/**
	 * Decrements the number in the map bound to the passed key by 1
	 * @param source The map JSON source
	 * @param key The key that the number is bound to in the map
	 * @return the updated map JSON source
	 */
	public static String decrementLong(String source, String key) {
		return increment(source, -1L, key);		
	}
	
	/**
	 * Decrements the number in the map bound to the passed key by 1
	 * @param source The map JSON source
	 * @param key The key that the number is bound to in the map
	 * @return the updated map JSON source
	 */
	public static String decrementFloat(String source, String key) {
		return increment(source, -1F, key);		
	}

	/**
	 * Decrements the number in the map bound to the passed key by 1
	 * @param source The map JSON source
	 * @param key The key that the number is bound to in the map
	 * @return the updated map JSON source
	 */
	public static String decrementDouble(String source, String key) {
		return increment(source, -1D, key);		
	}

	
	/**
	 * Retrieves the value bound to the JSON map as a number
	 * @param source The map JSON source
	 * @param key The map key the number is bound to
	 * @return the bound number
	 */
	public static int getInt(String source, String key) {
		return Integer.parseInt(read(source).get(nvl("Key", key)));
	}
	
	/**
	 * Retrieves the value bound to the JSON map as a number
	 * @param source The map JSON source
	 * @param key The map key the number is bound to
	 * @return the bound number
	 */
	public static long getLong(String source, String key) {
		return Long.parseLong(read(source).get(nvl("Key", key)));
	}
	
	/**
	 * Retrieves the value bound to the JSON map as a number
	 * @param source The map JSON source
	 * @param key The map key the number is bound to
	 * @return the bound number
	 */
	public static float getFloat(String source, String key) {
		return Float.parseFloat(read(source).get(nvl("Key", key)));
	}
	
	/**
	 * Retrieves the value bound to the JSON map as a number
	 * @param source The map JSON source
	 * @param key The map key the number is bound to
	 * @return the bound number
	 */
	public static double getDouble(String source, String key) {
		return Double.parseDouble(read(source).get(nvl("Key", key)));
	}
	
}
