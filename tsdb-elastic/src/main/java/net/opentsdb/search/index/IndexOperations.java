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

import static net.opentsdb.search.ElasticSearchEventHandler.DEFAULT_ES_ANNOT_TYPE;
import static net.opentsdb.search.ElasticSearchEventHandler.DEFAULT_ES_TSMETA_TYPE;
import static net.opentsdb.search.ElasticSearchEventHandler.DEFAULT_ES_UIDMETA_TYPE;

import java.io.UnsupportedEncodingException;
import java.util.Map;

import net.opentsdb.meta.TSMeta;
import net.opentsdb.search.ElasticSearchEventHandler;
import net.opentsdb.utils.JSON;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.internal.InternalClient;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * <p>Title: IndexOperations</p>
 * <p>Description: Handles all the search event callacks</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>net.opentsdb.search.index.IndexOperations</code></p>
 */

public class IndexOperations implements ActionListener<IndexResponse> {
	/** Instance logger */
	protected final Logger log = LoggerFactory.getLogger(getClass());
	/** The ES client */
	protected final InternalClient client;
	/** The timeout in ms. for index factory operations */
	protected long indexOpsTimeout;
	
	/** The configured annotation type name */
	protected final String annotationTypeName;
	/** The configured annotation index name */
	protected final String annotationIndexName;
	
	/** The configured TSMeta name */
	protected final String tsMetaTypeName;
	/** The configured TSMeta index name */
	protected final String tsMetaIndexName;
	
	/** The configured UIDMeta name */
	protected final String uidMetaTypeName;
	/** The configured UIDMeta index name */
	protected final String uidMetaIndexName;
	

	/**
	 * Creates a new IndexOperations
	 * @param client The ES index client 
	 * @param indexOpsTimeout The timeout in ms. for index factory operations
	 * @param typeIndexNames A map keyed by the default type names (e.g. {@link ElasticSearchEventHandler#DEFAULT_ES_ANNOT_TYPE}
	 * and where the values are string arrays where index 0 is the configured type name and index 1 is the index name.
	 */
	public IndexOperations(InternalClient client, long indexOpsTimeout, Map<String, String[]> typeIndexNames) {
		this.client = client;
		this.indexOpsTimeout = indexOpsTimeout;
		annotationTypeName = typeIndexNames.get(DEFAULT_ES_ANNOT_TYPE)[0];
		annotationIndexName = typeIndexNames.get(DEFAULT_ES_ANNOT_TYPE)[1];
		tsMetaTypeName = typeIndexNames.get(DEFAULT_ES_TSMETA_TYPE)[0];
		tsMetaIndexName = typeIndexNames.get(DEFAULT_ES_TSMETA_TYPE)[1];
		uidMetaTypeName = typeIndexNames.get(DEFAULT_ES_UIDMETA_TYPE)[0];
		uidMetaIndexName = typeIndexNames.get(DEFAULT_ES_UIDMETA_TYPE)[1];
		
		
		log.info("Created IndexOperations with timeout [{}]", indexOpsTimeout);
	}
	
	
	
    /**
     * Pushes a TSMeta object to the Elastic Search boxes over HTTP
     * @param meta The meta data to publish
     */
    public void indexTSUID(final TSMeta meta) { 
    	log.debug("Indexing TSMeta [{}]", meta);    	
    	client.index(new IndexRequest(tsMetaIndexName, tsMetaTypeName).source(JSON.serializeToString(meta)), this);
    }
	
	
	


	/**
	 * Quickie standalone test
	 * @param args None
	 */
	public static void main(String[] args) {
		InternalClient client  = null;
		try {
			log("IndexVerifier Test");
			client = new TransportClient().addTransportAddress(new InetSocketTransportAddress("localhost", 9300));
			
			IndexOperations iOps = new IndexOperations(client, 2000);
			
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		} finally {
			try { client.close(); } catch (Exception ex) {}
		}
	}
	
	public static void log(String format, Object...args) {
		System.out.println(String.format(format, args));
	}



	@Override
	public void onResponse(IndexResponse response) {
		// TODO Auto-generated method stub
		
	}



	@Override
	public void onFailure(Throwable e) {
		// TODO Auto-generated method stub
		
	}	

}
