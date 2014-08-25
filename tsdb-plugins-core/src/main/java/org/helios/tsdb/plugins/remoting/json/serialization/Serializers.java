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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.ObjectName;

import net.opentsdb.core.TSDB;
import net.opentsdb.meta.Annotation;
import net.opentsdb.meta.TSMeta;
import net.opentsdb.meta.UIDMeta;
import net.opentsdb.uid.UniqueId;
import net.opentsdb.uid.UniqueId.UniqueIdType;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.google.common.cache.Cache;

/**
 * <p>Title: Serializers</p>
 * <p>Description: Standard serializers for OpenTSDB types</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.remoting.json.serialization.Serializers</code></p>
 */

public class Serializers {
	
	/** TSDB used to retrieve TSMetas associated to annotations */
	private static volatile TSDB tsdb = null;
	/** TSMeta cache as an alternative to going to TSDB */
	private static volatile Cache<String, TSMeta> tsMetaCache = null;
	
	/**
	 * Sets the TSDB for the annotation serializers
	 * @param tsdb TSDB used to retrieve TSMetas associated to annotations
	 */
	public static void setTSDB(TSDB tsdb) {
		Serializers.tsdb = tsdb;
	}

	/**
	 * Sets the TSMeta cache for the annotation serializers
	 * @param tsMetaCache cache used to retrieve TSMetas associated to annotations
	 */
	public static void setCache(Cache<String, TSMeta> tsMetaCache) {
		Serializers.tsMetaCache = tsMetaCache;
	}

	
	/**
	 * Retrieves the TSMeta for the passed tsuid
	 * @param tsuid The tsuid of the TSMeta to get
	 * @return the TSMeta or null if the tsdb was null, or the retrieval timed out
	 */
	public static TSMeta getTSMeta(String tsuid) {
//		if(tsMetaCache!=null) {
//			try {
//				TSMeta t = tsMetaCache.get(tsuid, valueLoader)
//			} catch (Exception x) {/* No Op */}
//		}
		if(tsdb==null) return null;
		try {
			return TSMeta.getTSMeta(tsdb, tsuid).joinUninterruptibly(500);
		} catch (Exception ex) {
			return null;
		}
	}
	
	/**
	 * <p>Title: TSMetaFullSerializer</p>
	 * <p>Description: The {@link TSDBTypeSerializer#FULL} serializer for {@link TSMeta} instances </p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.tsdb.plugins.remoting.json.serialization.Serializers.TSMetaFullSerializer</code></p>
	 */
	public static class TSMetaFullSerializer extends StdSerializer<TSMeta> {
		public TSMetaFullSerializer() {
			super(TSMeta.class);
		}
		
		@Override
		public void serialize(TSMeta t, JsonGenerator json,
				SerializerProvider provider) throws IOException,
				JsonProcessingException {
		      json.writeStartObject();
		      json.writeStringField("tsuid", t.getTSUID());
		      json.writeStringField("displayName", t.getDisplayName());
		      json.writeStringField("description", t.getDescription());
		      json.writeStringField("notes", t.getNotes());
		      json.writeNumberField("created", t.getCreated());
		      Map<String, String> custom = t.getCustom();
		      if (custom == null) {
		        json.writeNullField("custom");
		      } else {
		        json.writeObjectFieldStart("custom");
		        for (Map.Entry<String, String> entry : custom.entrySet()) {
		          json.writeStringField(entry.getKey(), entry.getValue());
		        }
		        json.writeEndObject();
		      }
		      json.writeStringField("units", t.getUnits());
		      json.writeStringField("dataType", t.getDataType());
		      json.writeNumberField("retention", t.getRetention());
		      json.writeNumberField("max", t.getMax());
		      json.writeNumberField("min", t.getMin());
		      json.writeEndObject(); 
		}
	}
	
	/**
	 * <p>Title: UIDMetaFullSerializer</p>
	 * <p>Description: The {@link TSDBTypeSerializer#FULL} serializer for {@link UIDMeta} instances </p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.tsdb.plugins.remoting.json.serialization.Serializers.UIDMetaFullSerializer</code></p>
	 */
	public static class UIDMetaFullSerializer extends StdSerializer<UIDMeta> {
		public UIDMetaFullSerializer() {
			super(UIDMeta.class);
		}
		
		@Override
		public void serialize(UIDMeta u, JsonGenerator json,
				SerializerProvider provider) throws IOException,
				JsonProcessingException {
		      json.writeStartObject();
		      json.writeStringField("uid", u.getUID());
		      json.writeStringField("type", u.getType().name());
		      json.writeStringField("name", u.getName());
		      json.writeStringField("displayName", u.getDisplayName());
		      json.writeStringField("description", u.getDescription());
		      json.writeStringField("notes", u.getNotes());
		      json.writeNumberField("created", u.getCreated());
		      Map<String, String> custom = u.getCustom();
		      if (custom == null) {
		        json.writeNullField("custom");
		      } else {
		        json.writeObjectFieldStart("custom");
		        for (Map.Entry<String, String> entry : custom.entrySet()) {
		          json.writeStringField(entry.getKey(), entry.getValue());
		        }
		        json.writeEndObject();
		      }		      
		      json.writeEndObject(); 			
		}
	}
	
	/**
	 * <p>Title: AnnotationFullSerializer</p>
	 * <p>Description: The {@link TSDBTypeSerializer#FULL} serializer for {@link Annotation} instances </p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.tsdb.plugins.remoting.json.serialization.Serializers.AnnotationFullSerializer</code></p>
	 */	
	public static class AnnotationFullSerializer extends StdSerializer<Annotation> {
		public AnnotationFullSerializer() {
			super(Annotation.class);
		}
		@Override
		public void serialize(Annotation a, JsonGenerator json,
				SerializerProvider provider) throws IOException,
				JsonProcessingException {
		      json.writeStartObject();
		      String tsuid = a.getTSUID();
		      if (tsuid != null && !tsuid.isEmpty()) {
		        json.writeStringField("tsuid", tsuid);  
		      } else {
		    	  json.writeStringField("id", "Global");
		      }
		      json.writeNumberField("startTime", a.getStartTime());
		      json.writeNumberField("endTime", a.getEndTime());
		      json.writeStringField("description", a.getDescription());
		      json.writeStringField("notes", a.getDescription());
		      Map<String, String> custom = a.getCustom();
		      if (custom == null) {
		        json.writeNullField("custom");
		      } else {
		        json.writeObjectFieldStart("custom");
		        for (Map.Entry<String, String> entry : custom.entrySet()) {
		          json.writeStringField(entry.getKey(), entry.getValue());
		        }
		        json.writeEndObject();
		      }
		      
		      json.writeEndObject(); 
		}
	}

	/**
	 * <p>Title: AnnotationDefaultSerializer</p>
	 * <p>Description: The {@link TSDBTypeSerializer#DEFAULT} serializer for {@link Annotation} instances </p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.tsdb.plugins.remoting.json.serialization.Serializers.AnnotationDefaultSerializer</code></p>
	 */	
	public static class AnnotationDefaultSerializer extends StdSerializer<Annotation> {
		public AnnotationDefaultSerializer() {
			super(Annotation.class);
		}

		@Override
		public void serialize(Annotation a, JsonGenerator json,
				SerializerProvider provider) throws IOException,
				JsonProcessingException {
		      json.writeStartObject();
		      String tsuid = a.getTSUID();
		      if (tsuid != null && !tsuid.isEmpty()) {
		        json.writeStringField("id", tsuid);  
		        TSMeta tsMeta = getTSMeta(tsuid);
		        if(tsMeta!=null) {
		        	json.writeStringField("name", render(tsMeta));
		        }
		      } else {
		    	  json.writeStringField("id", "Global");
		      }
		      json.writeNumberField("startTime", a.getStartTime());
		      json.writeNumberField("endTime", a.getEndTime());
		      json.writeStringField("description", a.getDescription());
		      json.writeStringField("notes", a.getDescription());
		      Map<String, String> custom = a.getCustom();
		      if (custom == null) {
		        json.writeNullField("custom");
		      } else {
		        json.writeObjectFieldStart("custom");
		        for (Map.Entry<String, String> entry : custom.entrySet()) {
		          json.writeStringField(entry.getKey(), entry.getValue());
		        }
		        json.writeEndObject();
		      }		      
		      json.writeEndObject(); 
		}
	}
	
	/**
	 * <p>Title: AnnotationDefaultSerializer</p>
	 * <p>Description: The {@link TSDBTypeSerializer#DEFAULT} serializer for {@link Annotation} instances </p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.tsdb.plugins.remoting.json.serialization.Serializers.AnnotationDefaultSerializer</code></p>
	 */	
	public static class AnnotationNameSerializer extends StdSerializer<Annotation> {
		public AnnotationNameSerializer() {
			super(Annotation.class);
		}
		
		@Override
		public void serialize(Annotation a, JsonGenerator json,
				SerializerProvider provider) throws IOException,
				JsonProcessingException {
			json.writeStartObject();
			String tsuid = a.getTSUID();
			if (tsuid != null && !tsuid.isEmpty()) {
				json.writeStringField("id", tsuid);  
				TSMeta tsMeta = getTSMeta(tsuid);
				if(tsMeta!=null) {
					json.writeStringField("name", render(tsMeta));
				}
			} else {
				json.writeStringField("id", "Global");
			}
			json.writeNumberField("startTime", a.getStartTime());
			json.writeNumberField("endTime", a.getEndTime());
			
			json.writeEndObject();
		}
	}
	
	/**
	 * <p>Title: TSMetaDefaultSerializer</p>
	 * <p>Description: The {@link TSDBTypeSerializer#DEFAULT} serializer for {@link TSMeta} instances </p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.tsdb.plugins.remoting.json.serialization.Serializers.TSMetaDefaultSerializer</code></p>
	 */
	public static class TSMetaDefaultSerializer extends StdSerializer<TSMeta> {
		public TSMetaDefaultSerializer() {
			super(TSMeta.class);
		}		
		@Override
		public void serialize(TSMeta t, JsonGenerator json,
				SerializerProvider provider) throws IOException,
				JsonProcessingException {
		      json.writeStartObject();
		      json.writeStringField("tsuid", t.getTSUID());
		      json.writeStringField("name", render(t));
		      json.writeStringField("displayName", t.getDisplayName());
		      json.writeStringField("description", t.getDescription());
		      json.writeStringField("metric", t.getMetric().getName());
		      json.writeFieldName("tags");
		      json.writeStartArray();
		      for(UIDMeta u: t.getTags()) {
		    	  json.writeString(u.getName());
		      }
			  json.writeEndArray();
		      
		      json.writeStringField("notes", t.getNotes());
		      json.writeNumberField("created", t.getCreated());
		      Map<String, String> custom = t.getCustom();
		      if (custom == null) {
		        json.writeNullField("custom");
		      } else {
		        json.writeObjectFieldStart("custom");
		        for (Map.Entry<String, String> entry : custom.entrySet()) {
		          json.writeStringField(entry.getKey(), entry.getValue());
		        }
		        json.writeEndObject();
		      }
		      json.writeStringField("units", t.getUnits());
		      json.writeStringField("dataType", t.getDataType());
		      json.writeNumberField("retention", t.getRetention());
		      json.writeNumberField("max", t.getMax());
		      json.writeNumberField("min", t.getMin());
		      json.writeEndObject(); 
		}
	}
	
	/**
	 * Renders the full TSMeta name
	 * @param t The TSMeta to render
	 * @return the fully qualified name
	 */
	public static String render(TSMeta t) {
		StringBuilder b = new StringBuilder(t.getMetric().getName()).append(":");
		for(UIDMeta u: t.getTags()) {
			b.append(u.getName());
			if(u.getType()==UniqueIdType.TAGK) {
				b.append("=");
			} else {
				b.append(",");
			}
		}
		b.deleteCharAt(b.length()-1);
		return b.toString();
	}

	/**
	 * <p>Title: TSMetaNameSerializer</p>
	 * <p>Description: The {@link TSDBTypeSerializer#NAME} serializer for {@link TSMeta} instances </p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.tsdb.plugins.remoting.json.serialization.Serializers.TSMetaNameSerializer</code></p>
	 */
	public static class TSMetaNameSerializer extends StdSerializer<TSMeta> {
		public TSMetaNameSerializer() {
			super(TSMeta.class);
		}		
		
		@Override
		public void serialize(TSMeta t, JsonGenerator json,
				SerializerProvider provider) throws IOException,
				JsonProcessingException {
		      json.writeStartObject();		     
		      json.writeStringField("name", render(t));
		      json.writeStringField("type", "TS");
		      json.writeEndObject(); 
		}
	}
	
	/**
	 * <p>Title: TSMetaArrayD3Serializer</p>
	 * <p>Description: The {@link TSDBTypeSerializer#D3} serializer for {@link TSMeta} instances </p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.tsdb.plugins.remoting.json.serialization.Serializers.TSMetaArrayD3Serializer</code></p>
	 */
	public static class TSMetaArrayD3Serializer extends StdSerializer<TSMeta[]> {
		public TSMetaArrayD3Serializer() {
			super(TSMeta[].class);
		}
		@Override
		public void serialize(TSMeta[] t, JsonGenerator json,
				SerializerProvider provider) throws IOException,
				JsonProcessingException {
			Set<TSMeta> tsMetas = new LinkedHashSet<TSMeta>(t.length);
			Collections.addAll(tsMetas, t);
			TSMetaTree tm = TSMetaTree.build("org", tsMetas);
			json.writeObject(tm);
		}
	}
	
	/**
	 * <p>Title: TSMetaCollectionD3Serializer</p>
	 * <p>Description: The {@link TSDBTypeSerializer#D3} serializer for {@link TSMeta} instances </p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.tsdb.plugins.remoting.json.serialization.Serializers.TSMetaCollectionD3Serializer</code></p>
	 */
	public static class TSMetaCollectionD3Serializer extends StdSerializer<LinkedHashSet<TSMeta>> {
		private static final Class<?> c = new LinkedHashSet<TSMeta>(0).getClass();
		
		public TSMetaCollectionD3Serializer() {
			super((Class<LinkedHashSet<TSMeta>>) c);
		}
		
		@Override
		public void serialize(LinkedHashSet<TSMeta> t, JsonGenerator json,
				SerializerProvider provider) throws IOException,
				JsonProcessingException {
			TSMetaTree tm = TSMetaTree.build("org", t);
			json.writeObject(tm);
		}
	}
	
	/**
	 * <p>Title: TSMetaD3Serializer</p>
	 * <p>Description: The {@link TSDBTypeSerializer#D3} serializer for {@link TSMeta} instances </p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.tsdb.plugins.remoting.json.serialization.Serializers.TSMetaD3Serializer</code></p>
	 */
	public static class TSMetaD3Serializer extends StdSerializer<TSMeta> {
		
		public TSMetaD3Serializer() {
			super(TSMeta.class);
		}

		
		@Override
		public void serialize(TSMeta t, JsonGenerator json,
				SerializerProvider provider) throws IOException,
				JsonProcessingException {
			TSMetaTree tm = TSMetaTree.build("org", new HashSet<TSMeta>(Arrays.asList(t)));
			json.writeObject(tm);
		}
	}
	
	
	/**
	 * <p>Title: UIDMetaNameSerializer</p>
	 * <p>Description: The {@link TSDBTypeSerializer#NAME} serializer for {@link UIDMeta} instances </p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.tsdb.plugins.remoting.json.serialization.Serializers.UIDMetaNameSerializer</code></p>
	 */
	public static class UIDMetaNameSerializer extends StdSerializer<UIDMeta> {
		public UIDMetaNameSerializer() {
			super(UIDMeta.class);
		}

		@Override
		public void serialize(UIDMeta u, JsonGenerator json,
				SerializerProvider provider) throws IOException,
				JsonProcessingException {
		      json.writeStartObject();
		      json.writeStringField("name", u.getName());
		      json.writeStringField("type", u.getType().name());
		      json.writeEndObject(); 
		}
	}
	
	public static String tagPath(TSMeta tsMeta) {
		StringBuilder b = new StringBuilder();
		for(Map.Entry<String, String> tag: tagMap(tsMeta).entrySet()) {
			b.append(tag.getKey()).append("/").append(tag.getValue()).append("/");
		}
		if(b.length()>0) b.deleteCharAt(b.length()-1);
		return b.toString();
	}
	
	public static Map<String, String> tagMap(TSMeta tsMeta) {
		List<UIDMeta> metas = tsMeta.getTags();
		final int size = metas.size()/2;		
		List<String> keys = new ArrayList<String>(size);
		List<String> values = new ArrayList<String>(size);
		Map<String, String> map = new LinkedHashMap<String, String>(size);
		for(UIDMeta u: metas) {
			if(u.getType()==UniqueId.UniqueIdType.TAGK) {
				keys.add(u.getName());
			} else {
				values.add(u.getName());
			}
		}
		for(int i = 0; i < size; i++) {
			map.put(keys.get(i), values.get(i));			
		}
		return map;
	}
	
	public static Collection<Map<String, String>> tagMap(Collection<TSMeta> tsMetas) {
		LinkedHashSet<Map<String, String>> set = new LinkedHashSet<Map<String, String>>(tsMetas.size());
		for(TSMeta t: tsMetas) {
			set.add(tagMap(t));
		}
		return set;
	}
	
	
	
	@JsonSerialize(using=TSMetaTreeSerializer.class)
	public static class TSMetaTree {
		public final String name;							// the key, eg. dc
		public final String path; 
		protected Set<String> metrics = null;
		public final Map<String, TSMetaTree> children = new LinkedHashMap<String, TSMetaTree>();		
		
		private TSMetaTree tag(final TSMetaTree parent, final String name) {
			TSMetaTree tm = children.get(name);
			if(tm==null) {
				tm = new TSMetaTree(name, parent.path);
				children.put(name, tm);
			}
			return tm;
		}
		
		public void addMetric(String...metrics) {
			if(this.metrics==null) {
				this.metrics = new LinkedHashSet<String>();
			}
			Collections.addAll(this.metrics, metrics);
		}
		
		public TSMetaTree getByPath(final String path) {
			if(this.path.equals(path)) return this;
			for(TSMetaTree t: children.values()) {
				if(path.equals(t.path)) return t;
			}
			for(TSMetaTree t: children.values()) {
				TSMetaTree tt = t.getByPath(path);
				if(tt!=null) return tt;
			}
			return null;
		}
		
		public static TSMetaTree build(final String rootName, final Set<TSMeta> tsMetas) {
			TSMetaTree root = new TSMetaTree(rootName);
			load(root, tagMap(tsMetas));
			for(TSMeta t: tsMetas) {
				TSMetaTree tree = root.getByPath(rootName + "/" + tagPath(t));
				if(tree!=null) {
					tree.addMetric(t.getMetric().getName());
				}
			}
			return root;				
		}
		
		public static TSMetaTree buildFromObjectNames(final String rootName, final Set<ObjectName> objectNames) {
			TSMetaTree root = new TSMetaTree(rootName);
			LinkedHashSet<Map<String, String>> set = new LinkedHashSet<Map<String, String>>(objectNames.size());
			for(ObjectName on: objectNames) {
				set.add(on.getKeyPropertyList());
			}
			load(root, set);
			return root;
		}
		
		
		private static void load(final TSMetaTree root, final Collection<Map<String, String>> tags) {
			TSMetaTree current = root;			
			for(Map<String, String> tagMap: tags) {
				for(Map.Entry<String, String> tag: tagMap.entrySet()) {
					String key = tag.getKey(), value = tag.getValue();
					current = current.tag(current, key);
					//log(current);
					current = current.tag(current, value);
					//log(current);
				}	
				current = root;
			}
		}
		
		
		private TSMetaTree(final String name) {
			this.name = name;
			this.path = name;
		}
		
		private TSMetaTree(final String name, final String parentPath) {
			this.name = name;
			path = parentPath + "/" + name;	
		}
		
		
		public String toString() {
			return name + "[ path: [" + path + "], children:"  + children.size() + ": " + children.keySet() + "]";
		}
		private static final ThreadLocal<StringBuilder> indent = new ThreadLocal<StringBuilder>();
		
		public String deepToString() {
			final boolean root = indent.get()==null;
			if(root) indent.set(new StringBuilder());
			try {
				StringBuilder b = new StringBuilder();
				if(!root) indent.get().append("\t");
				b.append(name).append("\n");
				final String state = indent.get().toString();
				//indent.get().append("\t");
				for(TSMetaTree t: this.children.values()) {
					b.append(indent.get()).append(t.deepToString());					
				}
				indent.get().setLength(0);
				indent.get().append(state);
				return b.toString();
			} finally {
				if(root) indent.remove();
			}			
		}

		/**
		 * {@inheritDoc}
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			return result;
		}

		/**
		 * {@inheritDoc}
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			TSMetaTree other = (TSMetaTree) obj;
			if (name == null) {
				if (other.name != null)
					return false;
			} else if (!name.equals(other.name))
				return false;
			return true;
		}
		
	}

	public static class TSMetaTreeSerializer extends StdSerializer<TSMetaTree> {
		public TSMetaTreeSerializer() {
			super(TSMetaTree.class);
		}
		@Override
		public void serialize(TSMetaTree value, JsonGenerator jgen,
				SerializerProvider provider) throws IOException,
				JsonProcessingException {
			jgen.writeStartObject();
			jgen.writeStringField("name", value.name);
			jgen.writeStringField("type", "branch");
			jgen.writeStringField("path", value.path);
//			if(value.metrics!=null) {
//				jgen.writeFieldName("metrics");			
//				jgen.writeStartArray();
//				for(String m: value.metrics) {
//					jgen.writeString(m);
//				}
//				jgen.writeEndArray();				
//			}
//			jgen.writeFieldName("path");			
//			jgen.writeStartArray();
//			for(String p: value.path) {
//				jgen.writeString(p);
//			}
//			jgen.writeEndArray();
			if(value.children!=null && !value.children.isEmpty()) {
				jgen.writeFieldName("children");			
				jgen.writeStartArray();
				for(TSMetaTree t: value.children.values()) {
					provider.defaultSerializeValue(t, jgen);
				}
				jgen.writeEndArray();
			} else {
				if(value.metrics!=null && !value.metrics.isEmpty()) {
					jgen.writeFieldName("children");			
					jgen.writeStartArray();
					for(String m: value.metrics) {
						jgen.writeStartObject();
						jgen.writeStringField("name", m);
						jgen.writeStringField("type", "leaf");
						jgen.writeStringField("path", value.path + "/" + m);
						jgen.writeFieldName("children");			
						jgen.writeStartArray();
						jgen.writeEndArray();
						jgen.writeEndObject();
					}
					jgen.writeEndArray();				
				}
			}
			jgen.writeEndObject();
		}
		
	}
	
	
	/**
	 * Registers all the standard serializers
	 */
	static void register() {
		TSDBTypeSerializer.FULL.registerSerializer(TSMeta.class, new TSMetaFullSerializer());
		TSDBTypeSerializer.FULL.registerSerializer(UIDMeta.class, new UIDMetaFullSerializer());
		TSDBTypeSerializer.FULL.registerSerializer(Annotation.class, new AnnotationFullSerializer());
		
		TSDBTypeSerializer.DEFAULT.registerSerializer(TSMeta.class, new TSMetaDefaultSerializer());
//		TSDBTypeSerializer.DEFAULT.registerSerializer(UIDMeta.class, new UIDMetaFullSerializer());
//		TSDBTypeSerializer.DEFAULT.registerSerializer(Annotation.class, new AnnotationDefaultSerializer());

		TSDBTypeSerializer.NAME.registerSerializer(TSMeta.class, new TSMetaNameSerializer());
//		TSDBTypeSerializer.NAME.registerSerializer(UIDMeta.class, new UIDMetaNameSerializer());
//		TSDBTypeSerializer.NAME.registerSerializer(Annotation.class, new AnnotationNameSerializer());
		
		TSDBTypeSerializer.D3.registerSerializer(TSMeta.class, new TSMetaD3Serializer());
		TSDBTypeSerializer.D3.registerSerializer(TSMeta[].class, new TSMetaArrayD3Serializer());
		TSDBTypeSerializer.D3.registerSerializer(new LinkedHashSet<TSMeta>(0).getClass(), new TSMetaCollectionD3Serializer());
		
	}

	private Serializers() {}

}
