package net.opentsdb.search.index;

import java.util.Set;

/**
 * <p>Title: PercolateEventMBean</p>
 * <p>Description: MBean interface for {@link PercolateEvent}</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>net.opentsdb.search.index.PercolateEventMBean</code></p>
 */
public interface PercolateEventMBean {

	/**
	 * Returns the document id 
	 * @return the document id
	 */
	public String getId();

	/**
	 * Returns the index that the document was indexed against
	 * @return the index name
	 */
	public String getIndex();

	/**
	 * Returns a set of the names of the queries that the document matched
	 * @return the matched query names
	 */
	public Set<String> getMatchedQueryNames();

	/**
	 * Returns the type that was indexed
	 * @return the type name
	 */
	public String getType();

	/**
	 * Returns the document version
	 * @return the document version
	 */
	public long getVersion();

}