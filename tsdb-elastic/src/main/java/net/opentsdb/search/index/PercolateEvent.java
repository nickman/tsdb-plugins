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
package net.opentsdb.search.index;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.management.Notification;
import javax.management.ObjectName;

import net.opentsdb.utils.JSON;

import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.get.MultiGetItemResponse;
import org.elasticsearch.action.get.MultiGetRequestBuilder;
import org.elasticsearch.action.get.MultiGetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.helios.tsdb.plugins.util.JMXHelper;

import com.sun.jmx.mbeanserver.DefaultMXBeanMappingFactory;
import com.sun.jmx.mbeanserver.MXBeanMapping;

/**
 * <p>Title: PercolateEvent</p>
 * <p>Description: Represents a matched percolation event</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>net.opentsdb.search.index.PercolateEvent</code></p>
 */

@SuppressWarnings("restriction")
public class PercolateEvent implements Serializable {
	/**  */
	private static final long serialVersionUID = 6311439313121780015L;
	/** The document id */
	private final String id;
	/** The index the document was indexed against */
	private final String index;
	/** The names of the matched queries */
	private final Set<String> matchedQueryNames;
	/** The mapping type that was indexed */
	private final String type;
	/** The version of the document that was indexed */
	private final long version;
	/** The ObjectName used to match agains this event */
	private final ObjectName objectName;
	/** The object name template */
	public static final String ON_TEMPLATE = "event.percolate:id=%s,index=%s,type=%s";
	/** A '*' wildcard string */
	public static final String ON_WILDCARD = "*";
	/** A '?' wildcard string */
	public static final String ON_CHAR_WILDCARD = "?";

	/** The default event resolution timeout */
	public static long DEFAULT_RESOLVE_TIMEOUT = 500;
	/** The default multi event resolution timeout */
	public static long DEFAULT_MULTI_RESOLVE_TIMEOUT = 2000;
	
	/** The MXBean mapper for PercolateEvent */
	private static final MXBeanMapping mapping;
	
	static {
		try {			
			mapping = DefaultMXBeanMappingFactory.DEFAULT.mappingForType(PercolateEvent.class, DefaultMXBeanMappingFactory.DEFAULT);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to create DefaultMXBeanMappingFactory for PercolateEvent", ex);
		}
	}
	
	/**
	 * Creates a new PercolateEvent matcher ObjectName
	 * @param id The pattern matching the document id
	 * @param index The pattern matching the index name
	 * @param type The pattern matching the type name
	 * @return the ObjectName matcher
	 */
	public static ObjectName matcher(String id, String index, String type) {
		return JMXHelper.objectName(ON_TEMPLATE, id, index, type);
	}
	
	/**
	 * Creates a type matcher ObjectName
	 * @param type The pattern matching the type name
	 * @return the ObjectName type matcher
	 */
	public static ObjectName typeMatcher(String type) {
		return matcher(ON_WILDCARD, ON_WILDCARD, type);
	}

	/**
	 * Creates a new type and index PercolateEvent matcher ObjectName
	 * @param index The pattern matching the index name
	 * @param type The pattern matching the type name
	 * @return the ObjectName matcher
	 */
	public static ObjectName matcher(String index, String type) {
		return JMXHelper.objectName(ON_TEMPLATE, ON_WILDCARD, index, type);
	}
	
	
	
	/**
	 * Creates a new PercolateEvent
	 * @param response The ES index operation response
	 */
	public PercolateEvent(IndexResponse response) {
		id = response.getId();
		index = response.getIndex();
		matchedQueryNames = Collections.unmodifiableSet(new HashSet<String>(response.getMatches()));
		type = response.getType();
		version = response.getVersion();
		objectName = JMXHelper.objectName(String.format(ON_TEMPLATE, id, index, type));
	}
	
	/**
	 * Generates a notification to broadcast this event
	 * @param prefix The type prefix
	 * @param sequence The sequence id for this notification
	 * @return the built notification
	 */
	Notification getNotification(String prefix, long sequence) {
		Notification notif = new Notification(prefix + "." + type, IndexOperations.OBJECT_NAME, sequence, "Matched Percolation:" + toMessage());
		notif.setUserData(this);		
		return notif;
	}
	
	/**
	 * Synchronously resolves the PercolateRequest by fetching the indicated document and decoding it to an Object instance.
	 * @param decodeTo The class to which an instance of should be decoded to
	 * @param client A connected ES client
	 * @param timeout The timeout in ms.
	 * @return the resolved Object
	 */
	public <T> T resolve(Class<T> decodeTo, Client client, long timeout) {
		if(client==null) throw new IllegalArgumentException("The passed client was null");
		if(timeout<1) throw new IllegalArgumentException("Invalid timeout [" + timeout + "]");
		GetResponse gr = null;
		int cnt = 0;
		do {
			gr = client.prepareGet(index, type, id).execute().actionGet(timeout);
			cnt++;
			if(cnt>5) break;
		} while(!gr.isExists());
		
		
		if(!gr.isExists()) {
			throw new RuntimeException("Percolated Event Doc Instance Could Not Be Found:" + this);
		}
		byte[] source = gr.getSourceAsBytes();
		return JSON.parseToObject(source, decodeTo);		
	}
	
	/**
	 * Synchronously resolves the PercolateRequest by fetching the indicated document and decoding it to an Object instance,
	 * using the default resolution timeout {@link #DEFAULT_RESOLVE_TIMEOUT}.
	 * @param decodeTo The class to which an instance of should be decoded to
	 * @param client A connected ES client
	 * @return the resolved Object
	 */
	public <T> T resolve(Class<T> decodeTo, Client client) {
		return resolve(decodeTo, client, DEFAULT_RESOLVE_TIMEOUT);
	}

	/**
	 * Synchronously resolves a collection of PercolateRequests by fetching the indicated documents
	 * and decoding it to an Object instance of the specified class.
	 * @param decodeTo The class to which an instance of should be decoded to
	 * @param events A collection of events. If this is null or empty, an empty result will be returned.
	 * @param client A connected ES client
	 * @param timeout The timeout in ms.
	 * @return A list of decoded objects (in the same order as the corresponding percolate events)
	 */
	public static <T> List<T> resolve(Class<T> decodeTo, List<PercolateEvent> events, Client client, long timeout) {
		if(client==null) throw new IllegalArgumentException("The passed client was null");
		if(timeout<1) throw new IllegalArgumentException("Invalid timeout [" + timeout + "]");
		if(events==null || events.isEmpty()) return Collections.emptyList();		
		String index = null;
		String type = null;
		List<T> resolved = new ArrayList<T>(events.size());		
		Set<String> ids = new HashSet<String>(events.size());
		MultiGetRequestBuilder builder = client.prepareMultiGet();
		for(PercolateEvent event: events) {
			if(event==null) continue;
			if(index==null) {
				index = event.index;
				type = event.type;
			}
			ids.add(event.id);
		}
		builder.add(index, type, ids);
		MultiGetResponse mgr = builder.execute().actionGet(timeout);
		for(MultiGetItemResponse mr: mgr) {
			resolved.add(JSON.parseToObject(mr.getResponse().getSourceAsBytes(), decodeTo));
		}
		return resolved;
	}

	/**
	 * Synchronously resolves a collection of PercolateRequests by fetching the indicated documents
	 * and decoding it to an Object instance of the specified class using the default resolution timeout {@link #DEFAULT_MULTI_RESOLVE_TIMEOUT}.
	 * @param decodeTo The class to which an instance of should be decoded to
	 * @param events A collection of events. If this is null or empty, an empty result will be returned.
	 * @param client A connected ES client
	 * @return A list of decoded objects (in the same order as the corresponding percolate events)
	 */
	public static <T> List<T> resolve(Class<T> decodeTo, List<PercolateEvent> events, Client client) {
		return resolve(decodeTo, events, client, DEFAULT_MULTI_RESOLVE_TIMEOUT);
	}
		
	
	/**
	 * Returns the document id 
	 * @return the document id
	 */
	public String getId() {
		return id;
	}
	/**
	 * Returns the index that the document was indexed against
	 * @return the index name
	 */
	public String getIndex() {
		return index;
	}
	/**
	 * Returns a set of the names of the queries that the document matched
	 * @return the matched query names
	 */
	public Set<String> getMatchedQueryNames() {
		return matchedQueryNames;
	}
	/**
	 * Returns the type that was indexed
	 * @return the type name
	 */
	public String getType() {
		return type;
	}
	/**
	 * Returns the document version
	 * @return the document version
	 */
	public long getVersion() {
		return version;
	}
	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((index == null) ? 0 : index.hashCode());
		result = prime
				* result
				+ ((matchedQueryNames == null) ? 0 : matchedQueryNames
						.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		result = prime * result + (int) (version ^ (version >>> 32));
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
		PercolateEvent other = (PercolateEvent) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (index == null) {
			if (other.index != null)
				return false;
		} else if (!index.equals(other.index))
			return false;
		if (matchedQueryNames == null) {
			if (other.matchedQueryNames != null)
				return false;
		} else if (!matchedQueryNames.equals(other.matchedQueryNames))
			return false;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		if (version != other.version)
			return false;
		return true;
	}
	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return String
				.format("PercolateEvent [id=%s, index=%s, type=%s, version=%s, matchedQueryNames=%s]",
						id,
						index,
						type, 
						version,
						matchedQueryNames != null ? matchedQueryNames.toString() : "[]"
						);
	}
	
	/**
	 * Generates the message for a JMX notification
	 * @return the message for a JMX notification
	 */
	public String toMessage() {
		return String
				.format("PercolateEvent [id=%s, index=%s, type=%s, version=%s]",
						id,
						index,
						type, 
						version);
	}
	
	
	/**
	 * Replaces this object with an opentype when being serialized
	 * @return an open type data object representing this channel
	 * @throws ObjectStreamException
	 */
	private Object writeReplace() throws ObjectStreamException {
		try {
			return mapping.toOpenValue(this);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	/**
	 * Returns the ObjectName used to match agains this event
	 * @return the objectName
	 */
	public ObjectName getObjectName() {
		return objectName;
	}
	
	/**
	 * Determines if the passed object name is match to this one. 
	 * @param match The ObjectName to match against. It may be a pattern ObjectName.
	 * @return true if for a match, false otherwise.
	 */
	public boolean matches(ObjectName match) {
		if(match==null) return false;
		if(match.isPattern()) {
			return match.apply(objectName);
		}
		return match.equals(objectName);
	}

}
