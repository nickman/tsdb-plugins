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
package net.opentsdb.catalog.cache;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.management.ObjectName;

import net.opentsdb.catalog.SQLWorker;
import net.opentsdb.catalog.SQLWorker.ResultSetHandler;

import org.helios.jmx.util.helpers.ConfigurationHelper;
import org.helios.tsdb.plugins.cache.CacheStatistics;
import org.helios.tsdb.plugins.util.JMXHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * <p>Title: TagPredicateCache</p>
 * <p>Description: Cache for tag query predicates keyed by the deep hash code of the raw tag predicates</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>net.opentsdb.catalog.cache.TagPredicateCache</code></p>
 */

public class TagPredicateCache {
	/** Instance logger */
	protected Logger log = LoggerFactory.getLogger(getClass()); 
	/** A SQLWorker to execute lookups */
	protected final SQLWorker sqlWorker;
	/** The underlying guava cache of UIDs keyed by the deep hash code of the raw predicates */
	protected final Cache<Integer, String[]> cache;
	/** The cache stats ObjectName if stats are enabled */
	protected final ObjectName objectName;
	/** The configuration property name for the maximum size of the cache */
	public static final String MAX_SIZE_PROP = "helios.search.catalog.predicatecache.maxsize";
	/** The configuration property name for the concurrency of the cache */
	public static final String CONCURRENCY_PROP = "helios.search.catalog.predicatecache.concurrency";
	/** The configuration property name for the stats enablement of the cache */
	public static final String STATS_ENABLED_PROP = "helios.search.catalog.predicatecache.stats";
	
	/** The default maximum size of the cache */
	public static final long DEFAULT_MAX_SIZE = 1000;
	/** The default concurrency of the cache */
	public static final int DEFAULT_CONCURRENCY = 4;
	/** The default stats enablement of the cache */
	public static final boolean DEFAULT_STATS_ENABLED = true;
	/** The configuration property with stats enablement of the cache */
	public static final String SPEC_TEMPLATE_WSTATS = "concurrencyLevel=%s,initialCapacity=%s,maximumSize=%s,recordStats";
	/** The configuration property without stats enablement of the cache */
	public static final String SPEC_TEMPLATE_NOSTATS = "concurrencyLevel=%s,initialCapacity=%s,maximumSize=%s";
	
	/** The load sql fragment to be UNION ALLed */
	public static final String LOAD_SQL = "SELECT DISTINCT P.XUID FROM TSD_TAGPAIR P, TSD_TAGK K, TSD_TAGV V WHERE P.TAGK = K.XUID AND P.TAGV = V.XUID AND ";
	// ((K.NAME = 'dc') AND (V.NAME = 'dc1'))
	/** The dynamic binding SQL block for tag keys */
	public static final String TAGK_SQL_BLOCK = "K.NAME %s ?"; 
	/** The dynamic binding SQL block for tag values */
	public static final String TAGV_SQL_BLOCK = "V.NAME %s ?";
	
	


	/**
	 * Creates a new TagPredicateCache
	 * @param sqlWorker A SQLWorker to execute lookups and inserts
	 */
	public TagPredicateCache(SQLWorker sqlWorker) {
		this.sqlWorker = sqlWorker;
		final long maxSize = ConfigurationHelper.getLongSystemThenEnvProperty(MAX_SIZE_PROP, DEFAULT_MAX_SIZE);
		final int concurrency = ConfigurationHelper.getIntSystemThenEnvProperty(CONCURRENCY_PROP, DEFAULT_CONCURRENCY);
		final boolean stats = ConfigurationHelper.getBooleanSystemThenEnvProperty(STATS_ENABLED_PROP, DEFAULT_STATS_ENABLED);
		final String spec = String.format(stats ? SPEC_TEMPLATE_WSTATS : SPEC_TEMPLATE_NOSTATS, concurrency, 100, maxSize);		
		cache = CacheBuilder.from(spec).build();
		if(stats) {
			objectName = JMXHelper.objectName(new StringBuilder(getClass().getPackage().getName()).append(":service=TagPredicateCache"));
			JMXHelper.registerMBean(objectName, new CacheStatistics(cache, objectName));
		} else {
			objectName = null;
		}		
	}
	
	public PredicateBuilder newPredicateBuilder() {
		return new PredicateBuilder();
	}
	
	private class PredicateRetriever implements Callable<String[]>, ResultSetHandler {
		final PredicateBuilder pb;
		final Set<String> results = new LinkedHashSet<String>();
		
		public PredicateRetriever(PredicateBuilder pb) {
			this.pb = pb;
		}



		@Override
		public String[] call() throws Exception {
			StringBuilder b = new StringBuilder();
			boolean first = true;
			List<Object> binds = new ArrayList<Object>();
			for(Map.Entry<String, String> entry: pb.pairPredicates.entrySet()) {
				
				if(first) {
					first = false;
				} else {
					b.append(" INTERSECT ");
				}
				b.append(LOAD_SQL);
				b.append("(")
					.append("(").append(expandPredicate(entry.getKey(), TAGK_SQL_BLOCK, binds)).append(")")
					.append(" AND ")
					.append("(").append(expandPredicate(entry.getValue(), TAGV_SQL_BLOCK, binds)).append(")")
					.append(") ");
			}
			log.info("Executing SQL [{}]", fillInSQL(b.toString(), binds));
			sqlWorker.executeQuery(b.toString(), this, binds.toArray(new Object[0]));			
			return results.toArray(new String[results.size()]);
		}



		@Override
		public boolean onRow(int rowId, ResultSet rset) {
			try {
				results.add(rset.getString(1));
			} catch (Exception ex) {
				ex.printStackTrace(System.err);
			}
			return true;
		}
		
	}
	
	private static final Pattern Q_PATTERN = Pattern.compile("\\?");
	
	public static String fillInSQL(String sql, List<Object> binds) {
		final int bindCnt = binds.size();
		Matcher m = Q_PATTERN.matcher(sql);
		for(int i = 0; i < bindCnt; i++) {
			Object bind = binds.get(i);
			if(bind instanceof CharSequence) {
				sql = m.replaceFirst("'" + bind.toString() + "'");
			} else {
				sql = m.replaceFirst(bind.toString());
			}		
			m = Q_PATTERN.matcher(sql);
		}
		return sql;
	}
	
	
	public class PredicateBuilder {
		final TreeSet<String> keyPredicates = new TreeSet<String>();
		final TreeSet<String> valuePredicates = new TreeSet<String>();
		final TreeMap<String, String> pairPredicates = new TreeMap<String, String>();		
		private volatile String[] preds = {};
		
		public PredicateBuilder appendKeys(String...keys) {
			for(String s: keys) {
				if(s!=null && !s.trim().isEmpty()) {
					if(keyPredicates.add(s.trim())) preds = null;
				}
			}
			return this;
		}
		
		public PredicateBuilder appendValues(String...values) {
			for(String s: values) {
				if(s!=null && !s.trim().isEmpty()) {
					if(valuePredicates.add(s.trim())) preds = null;
				}
			}
			return this;
		}
	
		public PredicateBuilder appendTags(Map<String, String> tags) {
			if(tags==null || tags.isEmpty()) return this;
			for(Map.Entry<String, String> e: tags.entrySet()) {
				pairPredicates.put(e.getKey().trim(), e.getValue().trim());
			}
			preds = null;
			return this;			
		}
		
		public int hashCode() {
			if(preds==null) {
				final int k = keyPredicates.size();
				final int v = valuePredicates.size();
				final int p = pairPredicates.size();
				preds = new String[p];
				System.arraycopy(valuePredicates.toArray(new String[p]), 0, preds, 0, p);
//				System.arraycopy(keyPredicates.toArray(new String[k]), 0, preds, 0, k);
//				System.arraycopy(valuePredicates.toArray(new String[v]), 0, preds, k-1, v);
			}
			return preds.length==0 ? 0 : Arrays.deepHashCode(preds);			
		}
		
		public String[] get() throws Exception {
			final int key = hashCode();
			return cache.get(hashCode(), new PredicateRetriever(this));
		}
		
		
		private PredicateBuilder() {}
	}
	
	/**
	 * Expands a SQL predicate for wildcards and multis
	 * @param value The value expression
	 * @param predicateBase The constant predicate base format
	 * @param binds The bind variable accumulator
	 * @return the expanded predicate
	 */
	public static final String expandPredicate(final String value, final String predicateBase, final List<Object> binds) {
		final StringTokenizer st = new StringTokenizer(value.replace(" ", ""), "|", false);
		final int segmentCount = st.countTokens();
		if(segmentCount<1) throw new RuntimeException("Failed to parse expression [" + value + "]. Segment count was 0");
		if(segmentCount==1) {
			String val = st.nextToken();
			binds.add(val.replace('*', '%'));
			return predicateBase.replace("%s", val.indexOf('*')==-1 ? "=" : "LIKE");
		}
		StringBuilder b = new StringBuilder();
		for(int i = 0; i < segmentCount; i++) {			
			if(i!=0) b.append(" OR ");
			String val = st.nextToken();
			binds.add(val.replace('*', '%'));
			b.append(predicateBase.replace("%s", val.indexOf('*')==-1 ? "=" : "LIKE"));			
		}
		return b.toString();				
	}
	

}
