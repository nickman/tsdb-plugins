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
package net.opentsdb.catalog;

import java.sql.ResultSet;
import java.util.Iterator;
import java.util.List;

import net.opentsdb.meta.Annotation;
import net.opentsdb.meta.TSMeta;
import net.opentsdb.meta.UIDMeta;
import net.opentsdb.uid.UniqueId.UniqueIdType;

/**
 * <p>Title: MetaReader</p>
 * <p>Description: Defines a class that can instantiate OpenTSDB meta objects from a result set</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>net.opentsdb.catalog.MetaReader</code></p>
 */

public interface MetaReader {

	/**
	 * Returns a collection of {@link UIDMeta}s read from the passed {@link ResultSet}.
	 * @param rset The result set to read from
	 * @param uidType The UIDMeta type name we're reading
	 * @return a [possibly empty] collection of UIDMetas
	 */
	public List<UIDMeta> readUIDMetas(ResultSet rset, String uidType);
	
	
	/**
	 * Returns a collection of {@link UIDMeta}s read from the passed {@link ResultSet}.
	 * @param rset The result set to read from
	 * @param uidType The UIDMeta type we're reading
	 * @return a [possibly empty] collection of UIDMetas
	 */
	public List<UIDMeta> readUIDMetas(ResultSet rset, UniqueIdType uidType);
	
	/**
	 * Returns a collection of shallow (no UIDMetas for the metric or tags) {@link TSMeta}s read from the passed {@link ResultSet}.
	 * @param rset The result set to read from
	 * @return a [possibly empty] collection of TSMetas
	 */
	public List<TSMeta> readTSMetas(ResultSet rset);
	
	/**
	 * Returns a collection of {@link TSMeta}s read from the passed {@link ResultSet}.
	 * @param rset The result set to read from
	 * @param includeUIDs If true, the metric and tags will be loaded, otherwise shallow TSMetas will be returned
	 * @return a [possibly empty] collection of TSMetas
	 */
	public List<TSMeta> readTSMetas(ResultSet rset, boolean includeUIDs);
	
	/**
	 * Returns a TSMeta iterator for the passed result set (no UIDMetas for the metric or tags)
	 * @param rset The result set to read from
	 * @return a TSMeta iterator
	 */
	public IndexProvidingIterator<TSMeta> iterateTSMetas(ResultSet rset);
	
	/**
	 * Returns a TSMeta iterator for the passed result set 
	 * @param rset The result set to read from
	 * @param includeUIDs true to load UIDs, false otherwise
	 * @return the TSMeta iterator
	 */
	public IndexProvidingIterator<TSMeta> iterateTSMetas(ResultSet rset, boolean includeUIDs);
	
	
	/**
	 * Returns a UIDMeta iterator for the passed result set 
	 * @param rset The result set to read from
	 * @param uidType THe UID type to iterate 
	 * @return the UIDMeta iterator
	 */
	public IndexProvidingIterator<UIDMeta> iterateUIDMetas(ResultSet rset, UniqueIdType uidType);	
	
	
	/**
	 * Returns a collection of {@link Annotation}s read from the passed {@link ResultSet}.
	 * @param rset The result set to read from
	 * @return a [possibly empty] collection of Annotations
	 */
	public List<Annotation> readAnnotations(ResultSet rset);
	

}
