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
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.management.Notification;

import org.elasticsearch.action.index.IndexResponse;

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
	 * Creates a new PercolateEvent
	 * @param response The ES index operation response
	 */
	public PercolateEvent(IndexResponse response) {
		id = response.getId();
		index = response.getIndex();
		matchedQueryNames = Collections.unmodifiableSet(new HashSet<String>(response.getMatches()));
		type = response.getType();
		version = response.getVersion();
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

}
