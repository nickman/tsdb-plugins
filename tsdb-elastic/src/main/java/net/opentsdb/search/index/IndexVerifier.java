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

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.elasticsearch.action.admin.indices.alias.get.IndicesGetAliasesRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.admin.indices.exists.types.TypesExistsRequest;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequest;
import org.elasticsearch.client.IndicesAdminClient;
import org.elasticsearch.client.Requests;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.xcontent.XContentBuilder;
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
		final String rootNodeName = rootNode.getNodeName();
		final Set<String> indexNames = new HashSet<String>();
		if(!ROOT_NODE.equalsIgnoreCase(rootNodeName)) {
			throw new RuntimeException("Could not verify XML doc root node name. Expected [" + ROOT_NODE + "] but got + [" + rootNodeName + "]");
		}
		for(Node indexNode: XMLHelper.getChildNodesByName(XMLHelper.getChildNodeByName(rootNode, "indexes", false), "index", false)) {
			String indexName = XMLHelper.getAttributeByName(indexNode, "name", null).trim();
			long indexSerial = getIndexSerial(indexName);
			String alias = XMLHelper.getAttributeByName(indexNode, "alias", null).trim();
			log.info("Index:[{}] Serial:[{}] Alias:[{}]", indexName, indexSerial, alias);
			Node settingsNode = XMLHelper.getChildNodeByName(indexNode, "settings", false);
			Map<String, Object> settings = new HashMap<String, Object>();
			if(settingsNode!=null) {
				List<Node> settingNodes =   XMLHelper.getChildNodesByName(settingsNode, "setting", false);
				if(!settingNodes.isEmpty()) {					
					StringBuilder b = new StringBuilder("\n\tIndex Settings for [").append(indexName).append("]:");
					for(Node settingNode: settingNodes) {
						String nm = XMLHelper.getAttributeByName(settingNode, "name", null);
						String val = XMLHelper.getAttributeByName(settingNode, "value", null);
						settings.put(nm, val);
						b.append("\n\t\t").append(nm).append(" : ").append(val);
					}
					log.info(b.toString());
				}
			}
			// check if index exists
			if(!indexClient.exists(new IndicesExistsRequest(indexName)).actionGet().isExists()) {
				log.info("Creating Index [{}]....", indexName);
				new CreateIndexRequestBuilder(indexClient, indexName).setSettings(settings).execute().actionGet();
				log.info("Index [{}] Created", indexName);
			} else {
				log.info("Index [{}] Exists", indexName);
			}
			// check if alias exists
			if(!indexClient.aliasesExist(new IndicesGetAliasesRequest(alias)).actionGet().isExists()) {
				log.info("Creating Alias [{}] for Index [{}]....", alias, indexName);				
				indexClient.aliases(Requests.indexAliasesRequest().addAlias(indexName, alias)).actionGet();
				log.info("Created Alias [{}] for Index [{}].", alias, indexName);
			} else {
				log.info("Alias [{}] for Index [{}] Exists", alias, indexName);
			}
			indexNames.add(indexName);
		}
		for(Node typeNode: XMLHelper.getChildNodesByName(XMLHelper.getChildNodeByName(rootNode, "objects", false), "object", false)) {
			String typeName = XMLHelper.getAttributeByName(typeNode, "name", null).trim();
			String indexName = XMLHelper.getAttributeByName(typeNode, "index-ref", null).trim();
			if(!indexNames.contains(indexName)) {
				throw new RuntimeException("The index [" + indexName + "] requested for type [" + typeName + "] does not exist");
			}
			if(!indexClient.typesExists(new TypesExistsRequest(new String[]{indexName}, typeName)).actionGet().isExists()) {
				try {
					log.info("Creating Type [\n{}\n]....", buildType(typeNode).string());
				} catch (IOException e) {
					e.printStackTrace(System.err);
				}
				//indexClient.putMapping(new PutMappingRequest(indexName).)
			} else {
				
			}
		}
	}
	
	protected XContentBuilder buildType(Node typeNode) {
		
		return null;
	}
	
	
	private Long getIndexSerial(String indexName) {
		if(indexName==null || indexName.trim().isEmpty()) return 1L;
		if(indexName.indexOf('_')==-1) return 1L;
		try {
			return Long.parseLong(indexName.split("-")[1]);
		} catch (Exception ex) {
			return 1L;
		}
		
	}
	
	
	/*

	<objects>
		<object name="uidmeta" index-ref="opentsdb_1">
				<property name="uid" type="string" index="not_analyzed" store="no" boost="1"/>
				<property name="name" type="string" index="not_analyzed" store="no" boost="1"/>
		</object>
		<object name="annotation" index-ref="opentsdb_1">
				<property name="tsuid" type="string" index="not_analyzed" store="no" boost="1"/>				
		</object>
		<object name="tsmeta" index-ref="opentsdb_1">
				<property name="tsuid" type="string" index="not_analyzed" store="no" boost="1"/>
				<object name="metric" index-ref="opentsdb_1">
					<property name="name" type="string" index="not_analyzed" store="no" boost="1"/>
				</object>
				<object name="tags" index-ref="opentsdb_1">
					<property name="uid" type="string" index="not_analyzed" store="no" boost="1"/>
					<property name="name" type="string" index="not_analyzed" store="no" boost="1"/>				
				</object>				
		</object>		
	</objects>	
	
	
	<indexes>
		<index name="opentsdb_1" alias="opentsdb">
			<settings>
				<setting name="compound_on_flush" value="true"/>
				<setting name="term_index_interval" value="128"/>
				<setting name="number_of_replicas" value="1"/>
				<setting name="term_index_divisor" value="1"/>
				<setting name="compound_format" value="false"/>
				<setting name="number_of_shards" value="5"/>
				<setting name="refresh_interval" value="1"/>
			</settings>
		</index>	
	</indexes>

	 */
	
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
