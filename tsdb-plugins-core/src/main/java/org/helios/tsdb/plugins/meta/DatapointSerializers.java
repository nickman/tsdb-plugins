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
package org.helios.tsdb.plugins.meta;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import net.opentsdb.utils.JSON;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * <p>Title: DatapointSerializers</p>
 * <p>Description: JSON serializers for {@link Datapoint} instances</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.meta.DatapointSerializers</code></p>
 */

public class DatapointSerializers  {
	/** Sharable const instance */
	public static final DatapointSerializer DATAPOINT_SERIALIZER = new DatapointSerializer();
	/** Sharable const instance */
	public static final DatapointArraySerializer DATAPOINT_ARRAY_SERIALIZER = new DatapointArraySerializer();
	/** Sharable const instance */
	public static final DatapointCollectionSerializer DATAPOINT_COLLECTION_SERIALIZER = new DatapointCollectionSerializer();
	
	static {
		final SimpleModule sm = new SimpleModule();
		sm.addSerializer(DATAPOINT_ARRAY_SERIALIZER);
		sm.addSerializer(DATAPOINT_COLLECTION_SERIALIZER);
		JSON.getMapper().registerModule(sm);
	}
	
	/**
	 * <p>Title: DatapointSerializer</p>
	 * <p>Description: JSON serializer for {@link Datapoint} instances</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.tsdb.plugins.meta.DatapointSerializers.DatapointSerializer</code></p>
	 */
	public static class DatapointSerializer extends JsonSerializer<Datapoint> {
		@Override
		public void serialize(final Datapoint value, final JsonGenerator jgen, final SerializerProvider provider) throws IOException, JsonProcessingException {
			jgen.writeStartObject();
			jgen.writeStringField("fqn", value.fqn);
			jgen.writeStringField("tsuid", value.tsuid);
			jgen.writeStringField("metric", value.metric);
			jgen.writeObjectFieldStart("tags");
			for(Map.Entry<String, String> e: value.tags.entrySet()) {
				jgen.writeStringField(e.getKey(), e.getValue());
			}
			jgen.writeEndObject();
			value.values.serializeToJson(jgen);
			jgen.writeEndObject();		
		}
	}
	
	/**
	 * <p>Title: DatapointArraySerializer</p>
	 * <p>Description:JSON serializer for {@link Datapoint} arrays</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.tsdb.plugins.meta.DatapointSerializers.DatapointArraySerializer</code></p>
	 */
	public static class DatapointArraySerializer extends JsonSerializer<Datapoint[]> {
		@Override
		public void serialize(final Datapoint[] values, final JsonGenerator jgen, final SerializerProvider provider) throws IOException, JsonProcessingException {
			jgen.writeStartArray();
			for(Datapoint d: values) {
				DATAPOINT_SERIALIZER.serialize(d, jgen, provider);
			}
			jgen.writeEndArray();
		}
	}
	
	/**
	 * <p>Title: DatapointCollectionSerializer</p>
	 * <p>Description:JSON serializer for {@link Datapoint} collections</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.tsdb.plugins.meta.DatapointSerializers.DatapointCollectionSerializer</code></p>
	 */
	public static class DatapointCollectionSerializer extends JsonSerializer<Collection<Datapoint>> {
		@Override
		public void serialize(final Collection<Datapoint> values, final JsonGenerator jgen, final SerializerProvider provider) throws IOException, JsonProcessingException {
			jgen.writeStartArray();
			for(Datapoint d: values) {
				DATAPOINT_SERIALIZER.serialize(d, jgen, provider);
			}
			jgen.writeEndArray();
		}
	}

	
}
