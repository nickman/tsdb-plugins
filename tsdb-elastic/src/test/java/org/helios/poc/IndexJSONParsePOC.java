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
package org.helios.poc;

import java.io.InputStream;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * <p>Title: IndexJSONParsePOC</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.poc.IndexJSONParsePOC</code></p>
 */

public class IndexJSONParsePOC {
	public static final String INDEX_CONFIG_ROOT = "tsdb-elastic-index-mapping";
	public static final String INDEX_CONFIG_RESOURCE = "scripts/index_definitions.json";
	/**
	 * Creates a new IndexJSONParsePOC
	 */
	public IndexJSONParsePOC() {
		log("IndexJSONParsePOC");
		InputStream is = null;
		try {
			is = IndexJSONParsePOC.class.getClassLoader().getResourceAsStream("scripts/index_definitions.json");
			JsonFactory jf = new JsonFactory(new ObjectMapper());
			JsonParser parser = jf.createParser(is);
			TreeNode tn = parser.readValueAsTree();
			for(JsonToken t: tn.asToken().values()) {
				log("TreeNode:%s", t.name());
			}
			
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		} finally {
			if(is!=null) try { is.close(); } catch (Exception ex) {}
		}
	}
	
	public static void main(String[] args) {
		new IndexJSONParsePOC();
	}
	
	public static void log(String format, Object...args) {
		System.out.println(String.format(format, args));
	}

}
