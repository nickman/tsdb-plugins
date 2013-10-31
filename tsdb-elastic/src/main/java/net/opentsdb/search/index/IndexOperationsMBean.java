package net.opentsdb.search.index;

import java.util.Map;

import javax.management.ObjectName;

import net.opentsdb.meta.Annotation;
import net.opentsdb.meta.TSMeta;
import net.opentsdb.meta.UIDMeta;
import net.opentsdb.search.SearchQuery;

import org.elasticsearch.index.query.QueryBuilder;
import org.helios.tsdb.plugins.util.JMXHelper;

import com.stumbleupon.async.Deferred;

/**
 * <p>Title: IndexOperationsMBean</p>
 * <p>Description: JMX MBean interface for {@link IndexOperations}</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>net.opentsdb.search.index.IndexOperationsMBean</code></p>
 */
public interface IndexOperationsMBean {
	/** The named thread pool for the notification broadcaster thread pool */
	public static final String NOTIF_THREAD_POOL_NAME_PREFIX = "indexops-notif";
	/** The IndexOperations JMX MBean Objectname */ 
	public static final ObjectName OBJECT_NAME = JMXHelper.objectName("net.tsdb.search:service=IndexOperations");

	/**
	 * Returns the current number of percolation event listeners
	 * @return the current number of percolation event listeners
	 */
	public int getListenerCount();
	
	/**
	 * Indexes a TSMeta object in ElasticSearch
	 * @param meta The meta data to publish
	 */
	public void indexTSMeta(TSMeta meta);

	/**
	 * Deletes a TSMeta object in ElasticSearch
	 * @param tsuid The id of the TSMeta doc to delete
	 */
	public void deleteTSMeta(String tsuid);

	/**
	 * Indexes a UIDMeta object in ElasticSearch
	 * @param meta The meta data to publish
	 */
	public void indexUIDMeta(UIDMeta meta);

	/**
	 * Deletes a UIDMeta object in ElasticSearch
	 * @param meta The UIDMeta to delete the doc for
	 */
	public void deleteUIDMeta(UIDMeta meta);

	/**
	 * Indexes an Annotation object in ElasticSearch
	 * @param note The annotation to index
	 */
	public void indexAnnotation(Annotation note);

	/**
	 * Returns the document ID for the passed annotation
	 * @param annotation the annotation to get the ID for
	 * @return the ID of the annotation
	 */
	public String getAnnotationId(Annotation annotation);

	/**
	 * Deletes an Annotation object in ElasticSearch
	 * @param note The annotation to delete the doc for
	 */
	public void deleteAnnotation(Annotation note);

	/**
	 * Executes a search query and returns the deferred for the results
	 * @param query The query to execute
	 * @param result The deferred to write the query results into
	 * @return the deferred results
	 */
	public Deferred<SearchQuery> executeQuery(SearchQuery query,
			Deferred<SearchQuery> result);

	/**
	 * Indicates if operations are being issued asynchronously
	 * @return true if operations are being issued asynchronously, false otherwise
	 */
	public boolean isAsync();
	
	/**
	 * Set operations to run asynchronously or not
	 * @param enabled true for async, false for sync
	 */
	public void setAsync(boolean enabled);
	
	/**
	 * Indicates if percolating is enabled
	 * @return true if percolating is enabled, false otherwise
	 */
	public boolean isPercolateEnabled();
	
	/**
	 * Set the percolating enabled state 
	 * @param enabled true for enabled, false for disabled
	 */
	public void setPercolateEnabled(boolean enabled);
	
    /**
     * Registers a query to listen for percolated events on
     * @param indexName The index name
     * @param queryName The query name
     * @param queryBuilder The query builder defining the query
     * @return the ID of the watched query
     */
    public String registerPecolate(String indexName, String queryName, QueryBuilder queryBuilder);
    
    /**
     * Registers a query to listen for percolated events on
     * @param indexName The index name
     * @param queryName The query name
     * @param queryJson The json text defining the query
     * @return the ID of the watched query
     */
    public String registerPecolate(String indexName, String queryName, String queryJson);
    
    /**
     * Registers a query to listen for percolated events on, using a generated query name which is returned.
     * @param indexName The index name
     * @param queryBuilder The query builder defining the query
     * @return the ID of the watched query
     */
    public String registerPecolate(String indexName, QueryBuilder queryBuilder);
    
    
    /**
     * Registers a query to listen for percolated events on, using a generated query name which is returned.
     * @param indexName The index name
     * @param queryJson The json text defining the query
     * @return the ID of the watched query
     */
    public String registerPecolate(String indexName, String queryJson);
    
    /**
     * Removes the named query from the percolation index, stopping callbacks on that query
     * @param queryName The name of the query to delete
     * @return true if the document was found and deleted, false otherwise
     */
    public boolean removePercolate(String queryName);
    
	/**
	 * Returns a map of percolated (watched) queries keyed by the percolated document id
	 * @return a map of the watchedQueries
	 */
	public Map<String, String> getWatchedQueries();
	
	/**
	 * Returns the number of watched queries
	 * @return the number of watched queries
	 */
	public int getWatchedQueryCount();
    
	/**
	 * Returns the underlying index for the passed alias.
	 * If the passed name is the underlying index name, it will be returned.
	 * @param name The alias name
	 * @return The underlying index
	 */
	public String getIndexForAlias(String name);
	
}