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
package org.helios.tsdb.plugins.remoting.json.serialization;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.opentsdb.utils.JSON;

import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * <p>Title: TSDBTypeSerializer</p>
 * <p>Description: Enumerates the serialization methods for TSDB objects</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.remoting.json.serialization.TSDBTypeSerializer</code></p>
 */

public enum TSDBTypeSerializer {
	/** The default serialization */
	FULL,
	/** A slimmed down json payload */
	DEFAULT,
	/** Name only serializer */
	NAME,
	/** D3.js optimized serializer */
	D3;
	
	
	static {
		Serializers.register();
		for(TSDBTypeSerializer t: TSDBTypeSerializer.values()) {
			t.rebuildMapper();
		}
	}
	
	private final Map<Class<?>, ObjectMapper> serializers = new ConcurrentHashMap<Class<?>, ObjectMapper>();
	private volatile ObjectMapper allTypesMapper = null;
	private static final Map<TSDBTypeSerializer, Set<JsonSerializer<?>>> byTypeSerializers = new EnumMap<TSDBTypeSerializer, Set<JsonSerializer<?>>>(TSDBTypeSerializer.class);
	
	private void rebuildMapper() {
		ObjectMapper om = new ObjectMapper();
		Set<JsonSerializer<?>> set = byTypeSerializers.get(this);
		if(set!=null && !set.isEmpty()) {
			SimpleModule sm = new SimpleModule("All Serializers for [" + name() + "]");
			for(JsonSerializer<?> js: set) {
				sm.addSerializer(js);
			}
			om.registerModule(sm);
		}		
	}
	
	/**
	 * Registers a serializer for this enum member
	 * @param type The type the serializer is for
	 * @param serializer The serializer
	 */
	public synchronized <T> void registerSerializer(final Class<T> type, final JsonSerializer<T> serializer) {
		serializers.put(type, 
				new ObjectMapper().registerModule(
						new SimpleModule("[" + name() + " Serializer]:" + type.getName())
						.addSerializer(type, serializer)
				)
		);
		Set<JsonSerializer<?>> set = byTypeSerializers.get(this);
		if(set==null) {
			synchronized(byTypeSerializers) {
				set = byTypeSerializers.get(this);
				if(set==null) {
					set = new HashSet<JsonSerializer<?>>();
					byTypeSerializers.put(this, set);
				}
			}
		}
		if(set.add(serializer)) {
			rebuildMapper();
		}
	}
	
	/**
	 * Returns a mapper enabled for all OpenTSDB types for this enum member
	 * @return an ObjectMapper
	 */
	public ObjectMapper getMapper() {
		return allTypesMapper;
	}
	
	/**
	 * Returns the ObjectMapper to serialize objects of the passed type
	 * @param clazz The type to get an ObjectMapper for
	 * @return The registered ObjectMapper or the default if one was not found.
	 */
	public <T> ObjectMapper getSerializer(Class<T> clazz) {
		ObjectMapper mapper = serializers.get(clazz);
		return mapper != null ? mapper : JSON.getMapper();
	}
}
