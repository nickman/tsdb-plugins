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
package net.opentsdb.search;

import java.io.InputStream;

import org.elasticsearch.client.IndicesAdminClient;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.helios.tsdb.plugins.util.XMLHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

/**
 * <p>Title: IndexVerifier</p>
 * <p>Description: Client to validate or install the ES indexes and mapping for OpenTSDB indexed artifacts</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>net.opentsdb.search.IndexVerifier</code></p>
 */

public class IndexVerifier {
	/** Instance logger */
	protected final Logger log = LoggerFactory.getLogger(getClass());
	/** The ES index client */
	protected final IndicesAdminClient indexClient;
	
	/**
	 * Creates a new IndexVerifier
	 * @param indexClient The ES index client 
	 */
	public IndexVerifier(IndicesAdminClient indexClient) {
		this.indexClient = indexClient;
		log(this.indexClient.toString());
	}
	
	/** The XML config root node */
	public static final String ROOT_NODE = "tsdb-elastic-index-mapping";
	
	public void processIndexConfig(InputStream xmlDoc) {
		Node rootNode = XMLHelper.parseXML(xmlDoc).getDocumentElement();
		String rootNodeName = rootNode.getNodeName();
		if(!ROOT_NODE.equalsIgnoreCase(rootNodeName)) {
			throw new RuntimeException("Could not verify XML doc root node name. Expected [" + ROOT_NODE + "] but got + [" + rootNodeName + "]");
		}
		log("Root Node Name:%s", rootNode.getNodeName());
	}
	
	public static void log(String format, Object...args) {
		System.out.println(String.format(format, args));
	}

	public static void main(String[] args) {
		TransportClient client  = null;
		try {
			log("IndexVerifier Test");
			client = new TransportClient().addTransportAddress(new InetSocketTransportAddress("localhost", 9300));
			IndexVerifier iv = new IndexVerifier(client.admin().indices());
			iv.processIndexConfig(IndexVerifier.class.getClassLoader().getResourceAsStream("scripts/index-definitions.xml"));
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		} finally {
			try { client.close(); } catch (Exception ex) {}
		}
	}
	
}
