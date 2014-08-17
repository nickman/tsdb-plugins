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

import java.io.File;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.opentsdb.catalog.cache.TagPredicateCache;
import net.opentsdb.catalog.datasource.CatalogDataSource;
import net.opentsdb.core.Const;
import net.opentsdb.core.TSDB;
import net.opentsdb.meta.TSMeta;
import net.opentsdb.meta.UIDMeta;
import net.opentsdb.meta.api.MetricsMetaAPI;
import net.opentsdb.meta.api.QueryContext;
import net.opentsdb.uid.UniqueId;
import net.opentsdb.utils.Config;
import net.opentsdb.utils.JSON;

import org.helios.tsdb.plugins.async.AsyncDispatcherExecutor;
import org.helios.tsdb.plugins.remoting.json.JSONRequest;
import org.helios.tsdb.plugins.remoting.json.annotations.JSONRequestHandler;
import org.helios.tsdb.plugins.remoting.json.annotations.JSONRequestService;
import org.helios.tsdb.plugins.service.PluginContext;
import org.helios.tsdb.plugins.service.PluginContextImpl;
import org.helios.tsdb.plugins.util.ConfigurationHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.stumbleupon.async.Callback;
import com.stumbleupon.async.Deferred;

/**
 * <p>Title: SQLCatalogMetricsMetaAPIImpl</p>
 * <p>Description: The {@link MetricsMetaAPI} implementation for the SQL Catalof</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>net.opentsdb.catalog.SQLCatalogMetricsMetaAPIImpl</code></p>
 */
@JSONRequestService(name="meta")
public class SQLCatalogMetricsMetaAPIImpl implements MetricsMetaAPI, UncaughtExceptionHandler {
	/** The MetricsMetaAPIService service executor */
	protected final AsyncDispatcherExecutor metaQueryExecutor;
	/** The SQLWorker to manage JDBC Ops */
	protected final SQLWorker sqlWorker;
	/** The TSDB instance */
	protected final TSDB tsdb;
	/** The meta-reader for returning pojos */
	protected final MetaReader metaReader;
	/** Instance logger */
	protected Logger log = LoggerFactory.getLogger(getClass());
	
	/** The tag predicate cache */
	protected final TagPredicateCache tagPredicateCache;
	
	/** The maximum TSUID in Hex String format */
	public static final String MAX_TSUID;
	/** The dynamic binding SQL block for tag keys */
	public static final String TAGK_SQL_BLOCK = "K.NAME %s ?"; 
	/** The dynamic binding SQL block for tag values */
	public static final String TAGV_SQL_BLOCK = "V.NAME %s ?"; 
	/** The dynamic binding SQL block for metric names */
	public static final String METRIC_SQL_BLOCK = "M.NAME %s ?"; 
	
	
	
	/** Empty string array const */
	public static final String[] EMPTY_STR_ARR = {};
	/** Pipe parser pattern */
	public static final Pattern SPLIT_PIPES = Pattern.compile("\\|");
	
	static {
		char[] fs = new char[4 + (2*4*Const.MAX_NUM_TAGS)];
		Arrays.fill(fs, 'F');
		MAX_TSUID = new String(fs);		
	}


	
	/** The UID Retrieval SQL template.
	 * Tokens are:
	 * 1. The target UID type
	 * 2. The correlation UID type
	 * 3. The bind symbols for #2
	 * 4. The start at XUID expression
	 *  */
	public static final String GET_KEY_TAGS_SQL = 
			"SELECT DISTINCT X.* FROM TSD_%s X, TSD_TSMETA F, TSD_FQN_TAGPAIR T, TSD_TAGPAIR P, TSD_%s K " + 
			"WHERE K.XUID = F.METRIC_UID " + 
			"AND F.FQNID = T.FQNID " + 
			"AND T.XUID = P.XUID " + 
			"AND P.TAGK = X.XUID " + 
			"AND (%s) " +   				//  K.NAME = ? et. al.
			"AND X.NAME NOT IN (%s) " +
			"AND %s " + 					// XUID_START_SQL goes here
			"ORDER BY X.XUID DESC " + 
			"LIMIT ?"; 
		
		/** The initial start range if no starting index is supplied. Token is the target table alias  */
		public static final String INITIAL_XUID_START_SQL = " X.XUID <= 'FFFFFF'";
		/** The initial start range if a starting index is supplied. Token is the target table alias */
		public static final String XUID_START_SQL = " X.XUID < ? " ;

	
	
	/**
	 * Creates a new SQLCatalogMetricsMetaAPIImpl
	 * @param sqlWorker The SQL execution interface 
	 * @param metaReader The meta-reader for converting ResultSets into OpenTSDB pojos.
	 * @param tsdb The TSDB instance
	 * @param ctx The plugin context
	 */
	public SQLCatalogMetricsMetaAPIImpl(SQLWorker sqlWorker, MetaReader metaReader, TSDB tsdb, PluginContext ctx) {
		metaQueryExecutor = new AsyncDispatcherExecutor("MetricsMetaAPIService", ctx.getExtracted());
		metaQueryExecutor.registerUncaughtExceptionHandler(this);
		this.sqlWorker = sqlWorker;
		this.tsdb = tsdb;
		this.metaReader = metaReader;
		tagPredicateCache = new TagPredicateCache(sqlWorker);
		ctx.setResource(getClass().getSimpleName(), this);				
	}
	
	public static void main(String[] args) {
		log("DB Only MetaInstance");
//		FileInputStream fis = null;
		try {
			ConfigurationHelper.createPluginJar(net.opentsdb.catalog.TSDBCatalogSearchEventHandler.class);
//			fis = new FileInputStream("c:/services/opentsdb/tsdb.conf");
			// tsd.core.plugin_path=c:/services/opentsdb/plugins
			File file = new File("./src/test/resources/configs/search-plugin.conf");
			Config cfg = new Config(file.getAbsolutePath());
			cfg.overrideConfig("tsd.core.plugin_path", ConfigurationHelper.TMP_PLUGIN_DIR);
			Properties config = new Properties();
			config.putAll(cfg.getMap());
			final PluginContext pc = new PluginContextImpl(null, cfg, config, Thread.currentThread().getContextClassLoader());
			log("PluginContext created.");
//			H2DBCatalog service = new H2DBCatalog();
//			service.initialize(pc);
//			log("H2DBCatalog Initialized");
			TSDBCatalogSearchEventHandler seh = TSDBCatalogSearchEventHandler.getInstance();
			seh.initialize(pc);
			log("TSDBCatalogSearchEventHandler Initialized");
			CatalogDataSource cds = CatalogDataSource.getInstance();
			cds.initialize(pc);
			log("Catalog Datasource Initialized");
			
		} catch (Exception ex) {
			loge("Start Failed", ex);
		} finally {
//			if(fis!=null) try { fis.close(); } catch (Exception x) {/* No Op */}
		}
	}
	
	public static void log(Object fmt, Object...args) {
		System.out.println(String.format(fmt.toString(), args));
	}
	public static void loge(Object fmt, Object...args) {
		System.err.println(String.format(fmt.toString(), args));
		if(args.length > 0 && args[args.length-1] instanceof Throwable) {
			System.err.println("Stack Trace Follows....");
			((Throwable)args[args.length-1]).printStackTrace(System.err);
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
	
	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.meta.api.MetricsMetaAPI#getTagKeys(net.opentsdb.meta.api.QueryContext, java.lang.String, java.lang.String[])
	 */
	@Override
	public Deferred<Set<UIDMeta>> getTagKeys(final QueryContext queryOptions, final String metric, final String...tagKeys) {
		return getUIDsFor(UniqueId.UniqueIdType.TAGK, UniqueId.UniqueIdType.METRIC, queryOptions, metric, tagKeys);
	}
	

	/**
	 * Shuts down this service
	 */
	public void shutdown() {
		metaQueryExecutor.shutdown();
	}
	
	/**
	 * Executes a single correlated UID query, where the target is the type we want to query and the filter is the correlation data.
	 * @param targetType The type we're looking up
	 * @param filterType The type we're using to filter
	 * @param queryOptions The query options 
	 * @param filterName The primary filter name driver
	 * @param excludes Values of the target name that we don't want
	 * @return A deferred result to a set of matching UIDMetas of the type defined in the target argument
	 */
	protected Deferred<Set<UIDMeta>> getUIDsFor(final UniqueId.UniqueIdType targetType, final UniqueId.UniqueIdType filterType, final QueryContext queryOptions, final String filterName, final String...excludes) {
		final Deferred<Set<UIDMeta>> def = new Deferred<Set<UIDMeta>>();
		this.metaQueryExecutor.execute(new Runnable() {
			public void run() {
				final List<Object> binds = new ArrayList<Object>();
				String sql = null;
				final String predicate = expandPredicate(filterName, TAGK_SQL_BLOCK, binds);
				try {
					
//					binds.add(filterName);
					StringBuilder keyBinds = new StringBuilder();
					for(String key: excludes) {
						keyBinds.append("?, ");
						binds.add(key);
					}			
					if(keyBinds.length()>0) {
						keyBinds.deleteCharAt(keyBinds.length()-1); 
						keyBinds.deleteCharAt(keyBinds.length()-1);
					}
//					 * 1. The target UID type
//					 * 2. The correlation UID type
//					 * 3. The bind symbols for #2
//					 * 4. The start at XUID expression
					
					if(queryOptions.getNextIndex()==null) {
						sql = String.format(GET_KEY_TAGS_SQL, targetType, filterType, predicate, keyBinds, INITIAL_XUID_START_SQL); 
					} else {
						sql = String.format(GET_KEY_TAGS_SQL, targetType, filterType, predicate, keyBinds, XUID_START_SQL);
						binds.add(queryOptions.getNextIndex());
					}
					final int modPageSize = queryOptions.getPageSize();
					binds.add(modPageSize);					
					log.info("Executing SQL [{}]", fillInSQL(sql, binds));
					final Set<UIDMeta> uidMetas = closeOutUIDMetaResult(modPageSize, queryOptions, 
							metaReader.readUIDMetas(sqlWorker.executeQuery(sql, true, binds.toArray(new Object[0])), targetType)
					);
					def.callback(uidMetas);
				} catch (Exception ex) {
					log.error("Failed to execute getTagKeysFor.\nSQL was [{}]", sql, ex);
					def.callback(ex);
				}
			}
		});
		
		return def;
	} 

	
	/** The Metric Name Retrieval SQL template when tag keys are provided */
	public static final String GET_METRIC_NAMES_WITH_KEYS_SQL =
			"SELECT DISTINCT X.* FROM TSD_METRIC X, TSD_TSMETA F, TSD_FQN_TAGPAIR T, TSD_TAGPAIR P, TSD_TAGK K  " +
			"WHERE X.XUID = F.METRIC_UID  " +
			"AND F.FQNID = T.FQNID  " +
			"AND T.XUID = P.XUID " +
			"AND P.TAGK = K.XUID " +
			"AND (%s) ";			// K.NAME = ? 	  --- > TAGK_SQL_BLOCK		

	/** The Metric Name Retrieval SQL template when no tag keys are provided */
	public static final String GET_METRIC_NAMES_SQL =
			"SELECT X.* FROM TSD_METRIC X WHERE %s ORDER BY X.XUID DESC LIMIT ?"; 
	
	
	// SAMPLE {"t":"req", "rid":1, "svc":"meta", "op":"metricnames", "q": { "pageSize" : 100 }, "keys" : ["host"] }
	
	
	
	class ResultCompleteCallback<T> implements Callback<Void, T> {
		/** The original JSON request */
		private final JSONRequest request;		
		/** The current query context */
		private final QueryContext ctx;
		/**
		 * Creates a new ResultCompleteCallback
		 * @param request The original JSON request
		 * @parma ctx The current query context
		 */
		public ResultCompleteCallback(JSONRequest request, QueryContext ctx) {
			this.request = request;
			this.ctx = ctx;
		}
		@Override
		public Void call(T result) throws Exception {
			try {
				ArrayNode an = JSON.getMapper().createArrayNode();
				an.insertPOJO(0, ctx);
				an.insertPOJO(0, result);
				JSON.getMapper().writeValue(request.response().getChannelOutputStream(), an);
			} catch (Exception e) {
				request.error("Failed to get metric names", e);
				log.error("Failed to get metric names", e);
			}			
			return null;
		}
	}

	/**
	 * HTTP and WebSocket exposed interface to {@link #getMetricNames(net.opentsdb.meta.api.QueryContext, java.lang.String[])} 
	 * @param request The JSON request
	 * <p>Sample request:<pre>
	 * 				{"t":"req", "rid":1, "svc":"meta", "op":"metricnames", "q": { "pageSize" : 10 }, "keys" : ["host", "type", "cpu"] }
	 * </pre></p>
	 */
	@JSONRequestHandler(name="metricnames", description="Returns the MetricNames that match the passed tag keys")
	public void jsonMetricNames(final JSONRequest request) {
		QueryContext q = JSON.parseToObject(request.getRequest().get("q").toString(), QueryContext.class);
		final ArrayNode keysArr = (ArrayNode)request.getRequest().get("keys");
		final String[] keys = new String[keysArr.size()];
		for(int i = 0; i < keysArr.size(); i++) {
			keys[i] = keysArr.get(i).asText();
		}
		log.info("Processing JSONMetricNames. q: [{}], keys: {}", q, Arrays.toString(keys));		
		getMetricNames(q, keys).addCallback(new ResultCompleteCallback<Set<UIDMeta>>(request, q));
	}
	
	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.meta.api.MetricsMetaAPI#getMetricNames(net.opentsdb.meta.api.QueryContext, java.lang.String[])
	 */
	@Override
	public Deferred<Set<UIDMeta>> getMetricNames(final QueryContext queryOptions, final String... tagKeys) {
		final Deferred<Set<UIDMeta>> def = new Deferred<Set<UIDMeta>>();
		this.metaQueryExecutor.execute(new Runnable() {
			public void run() {				
				final List<Object> binds = new ArrayList<Object>();
				
				String sql = null;
				try {
					if(tagKeys==null || tagKeys.length==0) {										
						if(queryOptions.getNextIndex()==null) {
							sql = String.format(GET_METRIC_NAMES_SQL,INITIAL_XUID_START_SQL); 
						} else {
							sql = String.format(GET_METRIC_NAMES_SQL, XUID_START_SQL);
							binds.add(queryOptions.getNextIndex());
						}
					} else {
						StringBuilder keySql = new StringBuilder("SELECT * FROM ( ").append(String.format(GET_METRIC_NAMES_WITH_KEYS_SQL, expandPredicate(tagKeys[0], TAGK_SQL_BLOCK, binds))); 
//						binds.add(tagKeys[0]);
						int tagCount = tagKeys.length;
						for(int i = 1; i < tagCount; i++) {
							keySql.append(" INTERSECT ").append(String.format(GET_METRIC_NAMES_WITH_KEYS_SQL, expandPredicate(tagKeys[i], TAGK_SQL_BLOCK, binds)));
//							binds.add(tagKeys[i]);
						}
						keySql.append(") X ");
						if(queryOptions.getNextIndex()==null) {
							keySql.append(" WHERE ").append(INITIAL_XUID_START_SQL);
						} else {
							keySql.append(" WHERE ").append(XUID_START_SQL);
							binds.add(queryOptions.getNextIndex());
						}
						keySql.append(" ORDER BY X.XUID DESC LIMIT ? ");
						sql = keySql.toString();
					}
					final int modPageSize = queryOptions.getPageSize() + 1;
					binds.add(modPageSize);
					log.info("Executing SQL [{}]", fillInSQL(sql, binds));
					final Set<UIDMeta> uidMetas = closeOutUIDMetaResult(modPageSize, queryOptions, 
							metaReader.readUIDMetas(sqlWorker.executeQuery(sql, true, binds.toArray(new Object[0])), UniqueId.UniqueIdType.METRIC)
					);
					def.callback(uidMetas);
				} catch (Exception ex) {
					log.error("Failed to execute getMetricNamesFor.\nSQL was [{}]", sql, ex);
					def.callback(ex);					
				}
			}
		});
		return def;
	}
	

	private static final Set<UIDMeta> EMPTY_UIDMETA_SET =  Collections.unmodifiableSet(new LinkedHashSet<UIDMeta>(0));
	private static final Set<TSMeta> EMPTY_TSMETA_SET =  Collections.unmodifiableSet(new LinkedHashSet<TSMeta>(0));
	
	/**
	 * Closes out a UIDMeta query result, updating the QueryContext to indicate if the results are exhausted,
	 * and if not, what the next page start index is
	 * @param modPageSize The modified page size (one more than what was requested)
	 * @param ctx The query context to update
	 * @param readMetas The materialized UIDMetas being returned
	 * @return The set of UIDMetas to return
	 */
	protected Set<UIDMeta> closeOutUIDMetaResult(final int modPageSize, final QueryContext ctx, final List<UIDMeta> readMetas) {
		UIDMeta lastUIDMeta = null;
		if(readMetas.isEmpty()) {
			ctx.setExhausted(true);
			ctx.setNextIndex(null);
			return EMPTY_UIDMETA_SET;
		} else {
			final int size = readMetas.size(); 
			if(size < modPageSize) {
				ctx.setExhausted(true);
				lastUIDMeta = readMetas.get(size-1);
			} else {
				lastUIDMeta = readMetas.get(size-2);
				readMetas.remove(size-1);							
			}						
		}
		if(lastUIDMeta!=null) {
			ctx.setNextIndex(lastUIDMeta.getUID());
		} else {
			ctx.setNextIndex(null);
		}		
		return readMetas.isEmpty() ? EMPTY_UIDMETA_SET : new LinkedHashSet<UIDMeta>(readMetas);
	}
	
	/**
	 * Closes out a TSMeta query result, updating the QueryContext to indicate if the results are exhausted,
	 * and if not, what the next page start index is
	 * @param modPageSize The modified page size (one more than what was requested)
	 * @param ctx The query context to update
	 * @param readMetas The materialized TSMetas being returned
	 * @return The set of TSMetas to return
	 */
	protected Set<TSMeta> closeOutTSMetaResult(final int modPageSize, final QueryContext ctx, final List<TSMeta> readMetas) {
		TSMeta lastTSMeta = null;
		if(readMetas.isEmpty()) {
			ctx.setExhausted(true);
			ctx.setNextIndex(null);
			return EMPTY_TSMETA_SET;
		} else {
			final int size = readMetas.size(); 
			if(size < modPageSize) {
				ctx.setExhausted(true);
				lastTSMeta = readMetas.get(size-1);
			} else {
				lastTSMeta = readMetas.get(size-2);
				readMetas.remove(size-1);							
			}						
		}
		if(lastTSMeta!=null) {
			ctx.setNextIndex(lastTSMeta.getTSUID());
		} else {
			ctx.setNextIndex(null);
		}		
		return readMetas.isEmpty() ? EMPTY_TSMETA_SET : new LinkedHashSet<TSMeta>(readMetas);
	}
	
	

	/** The Metric Name Retrieval SQL template when tag pairs are provided */
	public static final String GET_METRIC_NAMES_WITH_TAGS_SQL =
			"SELECT DISTINCT X.* FROM TSD_METRIC X, TSD_TSMETA F, TSD_FQN_TAGPAIR T, TSD_TAGPAIR P, TSD_TAGK K, TSD_TAGV V " +
			"WHERE X.XUID = F.METRIC_UID  " +
			"AND F.FQNID = T.FQNID  " +
			"AND T.XUID = P.XUID " +
			"AND P.TAGK = K.XUID " +
			"AND P.TAGV = V.XUID " +
			"AND (%s) " +					// K.NAME % ? 	  --- > TAGK_SQL_BLOCK		 			
			"AND (%s) ";					// V.NAME % ? 	  --- > TAGV_SQL_BLOCK
	

	
	/**
	 * HTTP and WebSocket exposed interface to {@link #getMetricNames(net.opentsdb.meta.api.QueryContext, java.lang.String[])} 
	 * @param request The JSON request
	 * <p>Sample request:<pre>
	 * 				{"t":"req", "rid":1, "svc":"meta", "op":"metricswtags", "q": { "pageSize" : 10 }, "tags" : {"host" : "*", "type" : "combined"}}
	 * </pre></p>
	 */
	@JSONRequestHandler(name="metricswtags", description="Returns the MetricNames that match the passed tag pairs")
	public void jsonMetricNamesWithTags(final JSONRequest request) {
		QueryContext q = JSON.parseToObject(request.getRequest().get("q").toString(), QueryContext.class);
		ObjectNode tagNode = (ObjectNode)request.getRequest().get("tags");
		final Map<String, String> tags = new TreeMap<String, String>();
		Iterator<String> titer = tagNode.fieldNames();
		while(titer.hasNext()) {
			String key = titer.next();
			tags.put(key, tagNode.get(key).asText());
		}
		log.info("Processing jsonMetricNamesWithTags. q: [{}], tags: {}", q, tags);		
		getMetricNames(q, tags).addCallback(new ResultCompleteCallback<Set<UIDMeta>>(request, q));
	}


	
	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.meta.api.MetricsMetaAPI#getMetricNames(net.opentsdb.meta.api.QueryContext, java.util.Map)
	 */
	@Override
	public Deferred<Set<UIDMeta>> getMetricNames(final QueryContext queryOptions, final Map<String, String> tags) {
		final Deferred<Set<UIDMeta>> def = new Deferred<Set<UIDMeta>>();
		this.metaQueryExecutor.execute(new Runnable() {
			public void run() {				
				final List<Object> binds = new ArrayList<Object>();
				final List<String> likeOrEquals = new ArrayList<String>();
				String sql = null;
				try {
					if(tags==null || tags.isEmpty()) {										
						if(queryOptions.getNextIndex()==null) {
							sql = String.format(GET_METRIC_NAMES_SQL,INITIAL_XUID_START_SQL); 
						} else {
							sql = String.format(GET_METRIC_NAMES_SQL, XUID_START_SQL);
							binds.add(queryOptions.getNextIndex());
						}
					} else {
//						String predicate = expandPredicate(tagKeys[0], TAGK_SQL_BLOCK, binds)
						Iterator<Map.Entry<String, String>> iter = tags.entrySet().iterator();
						Map.Entry<String, String> entry = iter.next();
								
						StringBuilder keySql = new StringBuilder("SELECT * FROM ( ").append(String.format(GET_METRIC_NAMES_WITH_TAGS_SQL, 
								expandPredicate(entry.getKey(), TAGK_SQL_BLOCK, binds),
								expandPredicate(entry.getValue(), TAGV_SQL_BLOCK, binds)								
						));
						
						//processBindsAndTokens(entry, binds, likeOrEquals);
						while(iter.hasNext()) {
//							processBindsAndTokens(iter.next(), binds, likeOrEquals);
							entry = iter.next();
							keySql.append(" INTERSECT ").append(String.format(GET_METRIC_NAMES_WITH_TAGS_SQL,
									expandPredicate(entry.getKey(), TAGK_SQL_BLOCK, binds),
									expandPredicate(entry.getValue(), TAGV_SQL_BLOCK, binds)																	
							));
						}
						keySql.append(") X ");
						if(queryOptions.getNextIndex()==null) {
							keySql.append(" WHERE ").append(INITIAL_XUID_START_SQL);
						} else {
							keySql.append(" WHERE ").append(XUID_START_SQL);
							binds.add(queryOptions.getNextIndex());
						}
						keySql.append(" ORDER BY X.XUID DESC LIMIT ? ");
						sql = String.format(keySql.toString(), likeOrEquals.toArray());
					}
					final int modPageSize = queryOptions.getPageSize(); 
					binds.add(modPageSize);
					log.info("Executing SQL [{}]", fillInSQL(sql, binds));
					final Set<UIDMeta> uidMetas = closeOutUIDMetaResult(modPageSize, queryOptions, 
							metaReader.readUIDMetas(sqlWorker.executeQuery(sql, true, binds.toArray(new Object[0])), UniqueId.UniqueIdType.METRIC)
					);
					def.callback(uidMetas);
				} catch (Exception ex) {
					log.error("Failed to execute getMetricNamesFor (with tags).\nSQL was [{}]", sql, ex);
					def.callback(ex);					
				}
			}
		});
		return def;
	}

	
	/** The Metric Name Retrieval SQL template when tag pairs are provided */
	public static final String GET_TSMETAS_SQL =
			"SELECT DISTINCT X.* FROM TSD_TSMETA X, TSD_METRIC M, TSD_FQN_TAGPAIR T, TSD_TAGPAIR P, TSD_TAGK K, TSD_TAGV V " +
			"WHERE M.XUID = X.METRIC_UID  " +
			"AND X.FQNID = T.FQNID  " +
			"AND T.XUID = P.XUID " +
			"AND P.TAGK = K.XUID " +
			"AND (%s) " + 						// M.NAME = ?    --> METRIC_SQL_BLOCK
			"AND P.TAGV = V.XUID " +
			"AND (%s) " +					// K.NAME % ? 	  --- > TAGK_SQL_BLOCK		 			
			"AND (%s) ";				// V.NAME % ? 	  --- > TAGV_SQL_BLOCK

	/** The TSMeta Retrieval SQL template when no tags or metric name are provided and overflow is true */
	public static final String GET_TSMETAS_NO_TAGS_NO_METRIC_NAME_SQL =
			"SELECT X.* FROM TSD_TSMETA X WHERE %s ORDER BY X.TSUID DESC LIMIT ?"; 

	/** The TSMeta Retrieval SQL template when no tags are provided and overflow is true */
	public static final String GET_TSMETAS_NO_TAGS_NAME_SQL =
			"SELECT X.* FROM TSD_TSMETA X, TSD_METRIC M WHERE M.XUID = X.METRIC_UID AND %s AND M.NAME = ? ORDER BY X.TSUID DESC LIMIT ?"; 
	
	/** The initial start range if no starting index is supplied. Token is the target table alias  */
	public static final String INITIAL_TSUID_START_SQL = " X.TSUID <= '" + MAX_TSUID + "'";
	/** The initial start range if a starting index is supplied. Token is the target table alias */
	public static final String TSUID_START_SQL = " X.TSUID < ? " ;

	
	/**
	 * HTTP and WebSocket exposed interface to {@link #getTSMetas(net.opentsdb.meta.api.QueryContext, boolean, java.lang.String, java.util.Map)} 
	 * @param request The JSON request
	 * <p>Sample request:<pre>
	 * 				{"t":"req", "rid":1, "svc":"meta", "op":"tsmetas", "q": { "pageSize" : 10 }, "m":"sys.cp*", "overflow":false,  "tags" : {"host" : "*NWHI*"}}
	 * </pre></p>
	 */
	@JSONRequestHandler(name="tsmetas", description="Returns the MetricNames that match the passed tag pairs")
	public void jsonTSMetas(final JSONRequest request) {
		final QueryContext q = JSON.parseToObject(request.getRequest().get("q").toString(), QueryContext.class);
		final String metricName = request.getRequest().get("m").textValue();
		final boolean overflow = request.getRequest().get("overflow").asBoolean(false);
		final ObjectNode tagNode = (ObjectNode)request.getRequest().get("tags");
		final Map<String, String> tags = new TreeMap<String, String>();
		Iterator<String> titer = tagNode.fieldNames();
		while(titer.hasNext()) {
			String key = titer.next();
			tags.put(key, tagNode.get(key).asText());
		}
		log.info("Processing jsonTSMetas. q: [{}], tags: {}", q, tags);		
		getTSMetas(q, overflow, metricName, tags).addCallback(new ResultCompleteCallback<Set<TSMeta>>(request, q));
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.meta.api.MetricsMetaAPI#getTSMetas(net.opentsdb.meta.api.QueryContext, boolean, java.lang.String, java.util.Map)
	 */
	@Override
	public Deferred<Set<TSMeta>> getTSMetas(final QueryContext queryOptions, final boolean overflow, final String metricName, final Map<String, String> tags) {
		final Deferred<Set<TSMeta>> def = new Deferred<Set<TSMeta>>();		
		this.metaQueryExecutor.execute(new Runnable() {
			public void run() {				
				final List<Object> binds = new ArrayList<Object>();
//				String sql = null;
				final StringBuilder sqlBuffer = new StringBuilder();
				try {
					if(tags==null || tags.isEmpty()) {	
						if(!overflow) {
							def.callback(Collections.EMPTY_SET);
							return;
						}
					}					
					generateTSMetaSQL(sqlBuffer, binds, queryOptions, metricName, tags);
					final int modPageSize = queryOptions.getPageSize();
					binds.add(modPageSize);
					log.info("Executing SQL [{}]", fillInSQL(sqlBuffer.toString(), binds));
					
					// =================================================================================================================================
					//    LOAD TSMETAS FROM TSDB/HBASE
					// =================================================================================================================================
//					ResultSet disconnected = sqlWorker.executeQuery(sql, true, binds.toArray(new Object[0]));
//					while(disconnected.next()) {
//						String tsuid = disconnected.getString(5);
//						tsMetas.add(TSMeta.getTSMeta(tsdb, tsuid).join(1000));  // FIXME:  Chain the callbacks, then add the timeout
//					}
					// =================================================================================================================================
					//	LOADS TSMETAS FROM THE SQL CATALOG DB
					// =================================================================================================================================
					
					final Set<TSMeta> tsMetas = closeOutTSMetaResult(modPageSize, queryOptions, 
							metaReader.readTSMetas(sqlWorker.executeQuery(sqlBuffer.toString(), true, binds.toArray(new Object[0])), true)							
					); 
					def.callback(tsMetas);
				} catch (Exception ex) {
					log.error("Failed to execute getTSMetas (with tags).\nSQL was [{}]", sqlBuffer, ex);
					def.callback(ex);					
				}
			}
		});
		return def;
	}
	
	public static final String TS_METAS_NO_OVERFLOW_SQL = "SELECT X.* FROM TSD_FQN_TAGPAIR T, TSD_TSMETA X WHERE X.FQNID = T.FQNID GROUP BY X.FQNID HAVING SUM(CASE WHEN T.XUID IN ( ? ) THEN 1 ELSE 0 END) = ? ORDER BY X.TSUID DESC LIMIT ?";
	
	protected void tsMetasNoOverflow(final QueryContext queryOptions, final String metricName, final Map<String, String> tags) throws Exception {
		final String[] xuids = tagPredicateCache.newPredicateBuilder().appendTags(tags).get();
		final List<Object> binds = new ArrayList<Object>();
		binds.add(xuids);
		binds.add(xuids.length);
		binds.add(queryOptions.getPageSize());
		//log.info("Executing SQL [{}]", fillInSQL(sqlBuffer.toString(), binds));
		
		
		// 
		
	}

	
	/**
	 * Generates a TSMeta query
	 * @param sqlBuffer The sql buffer to append generated SQL into
	 * @param binds The query bind values
	 * @param queryOptions The query options
	 * @param metricName The metric name
	 * @param tags The tag pairs
	 */
	protected void generateTSMetaSQL(final StringBuilder sqlBuffer, final List<Object> binds, final QueryContext queryOptions, final String metricName, final Map<String, String> tags) {
		final boolean hasMetricName = (metricName==null || metricName.trim().isEmpty());
		if(tags==null || tags.isEmpty()) {	
			if(queryOptions.getNextIndex()==null) {							
				sqlBuffer.append(String.format(hasMetricName ? GET_TSMETAS_NO_TAGS_NAME_SQL : GET_TSMETAS_NO_TAGS_NO_METRIC_NAME_SQL, INITIAL_XUID_START_SQL)); 
			} else {
				sqlBuffer.append(String.format(hasMetricName ? GET_TSMETAS_NO_TAGS_NAME_SQL : GET_TSMETAS_NO_TAGS_NO_METRIC_NAME_SQL, XUID_START_SQL));
				binds.add(queryOptions.getNextIndex());
			}
			if(hasMetricName) binds.add(metricName);
			
		} else {
			StringBuilder keySql = new StringBuilder("SELECT * FROM ( ");
			prepareGetTSMetasSQL(metricName, tags, binds, keySql);
			keySql.append(") X ");
			if(queryOptions.getNextIndex()==null) {
				keySql.append(" WHERE ").append(INITIAL_TSUID_START_SQL);
			} else {
				keySql.append(" WHERE ").append(TSUID_START_SQL);
				binds.add(queryOptions.getNextIndex());
			}
			keySql.append(" ORDER BY X.TSUID DESC LIMIT ? ");
			sqlBuffer.append(keySql.toString());
		}		
	}
		
	protected void doGetTSMetasSQLTagValues(final boolean firstEntry, final String metricName, Map.Entry<String, String> tag, final List<Object> binds, final StringBuilder sql) {
		// // expandPredicate(entry.getKey(), TAGK_SQL_BLOCK, binds),
		if(!firstEntry) {
			sql.append(" INTERSECT ");
		}
		
		sql.append(String.format(GET_TSMETAS_SQL,  
				expandPredicate(metricName, METRIC_SQL_BLOCK, binds),
				expandPredicate(tag.getKey(), TAGK_SQL_BLOCK, binds),
				expandPredicate(tag.getValue(), TAGV_SQL_BLOCK, binds)
		));
	}
	
	protected void prepareGetTSMetasSQL(final String metricName, final Map<String, String> tags, final List<Object> binds, final StringBuilder sql) {
		Iterator<Map.Entry<String, String>> iter = tags.entrySet().iterator();
		doGetTSMetasSQLTagValues(true, metricName, iter.next(), binds, sql);
		while(iter.hasNext()) {
			doGetTSMetasSQLTagValues(false, metricName, iter.next(), binds, sql);
		}
	}
	
	/** The Tag Values Retrieval SQL template when tag pairs are provided */
	
	/*
	 * 
	 * Outer select from TSD_TAGV where exists
	 * 	Same as TSDMeta query but:
	 * 		match provided tags exactly, but not (TAGK = target tag key)
	 *  
	 */
	
	public static final String GET_TAG_VALUES_SQL =
		"SELECT DISTINCT VA.* " + 
		"FROM TSD_TAGV VA, TSD_METRIC M, TSD_FQN_TAGPAIR T, TSD_TAGPAIR P, TSD_TAGK K, TSD_TSMETA XA " +
		"WHERE M.XUID = XA.METRIC_UID " +
		"AND XA.FQNID = T.FQNID " +
		"AND T.XUID = P.XUID " +
		"AND P.TAGK = K.XUID " +
		"AND P.TAGV = VA.XUID " +
		"AND EXISTS ( " +
			"SELECT * FROM ( " + 
				"SELECT DISTINCT X.FQNID, V.XUID, K.NAME FROM TSD_TSMETA X, TSD_METRIC M, TSD_FQN_TAGPAIR T, TSD_TAGPAIR P, TSD_TAGK K, TSD_TAGV V WHERE M.XUID = X.METRIC_UID  AND X.FQNID = T.FQNID  AND T.XUID = P.XUID AND P.TAGK = K.XUID  AND P.TAGV = V.XUID " + 
				"AND (M.NAME = 'sys.cpu') AND ( " +
					//  ====  APPEND TAG PAIR EXPRESSIONS ====
					"(K.NAME = 'host') AND (V.NAME = 'PP-WK-NWHI-01') " +
					"OR " +
					"(K.NAME = 'type') AND (V.NAME = 'combined') " +
			") " +
		") Q WHERE Q.FQNID = XA.FQNID AND K.NAME NOT IN ('host', 'type') " +
	   ")";
	
	/*
	 * No need to join to TSD_TSMETA if no metric name is provided.
	 * If metric name is not null, do an inner exists against TSD_TSMETA
	 */
	

	/** The TSMeta Retrieval SQL template when no tags or metric name are provided and overflow is true */
	public static final String GET_TAG_VALUES_NO_TAGS_NO_METRIC_NAME_SQL =
			"SELECT X.* FROM TSD_TSMETA X WHERE %s ORDER BY X.TSUID DESC LIMIT ?"; 

	/** The TSMeta Retrieval SQL template when no tags are provided and overflow is true */
	public static final String GET_TAG_VALUES_NO_TAGS_NAME_SQL =
			"SELECT X.* FROM TSD_TSMETA X, TSD_METRIC M WHERE M.XUID = X.METRIC_UID AND %s AND M.NAME = ? ORDER BY X.TSUID DESC LIMIT ?"; 
	
	
	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.meta.api.MetricsMetaAPI#getTagValues(net.opentsdb.meta.api.QueryContext, java.lang.String, java.util.Map, java.lang.String)
	 */
	@Override
	public Deferred<Set<UIDMeta>> getTagValues(final QueryContext queryOptions, final String metricName, final Map<String, String> tags, final String tagKey) {
		final Deferred<Set<UIDMeta>> def = new Deferred<Set<UIDMeta>>();		
		this.metaQueryExecutor.execute(new Runnable() {
			public void run() {				
				final List<Object> binds = new ArrayList<Object>();
				final StringBuilder sqlBuffer = new StringBuilder();
				try {
					generateTSMetaSQL(sqlBuffer, binds, queryOptions, metricName, tags);
					binds.add(queryOptions.getPageSize());
					log.info("Executing SQL [{}]", fillInSQL(sqlBuffer.toString(), binds));
					final Set<TSMeta> tsMetas = new LinkedHashSet<TSMeta>(queryOptions.getPageSize());
					// =================================================================================================================================
					//    LOAD TSMETAS FROM TSDB/HBASE
					// =================================================================================================================================
//					ResultSet disconnected = sqlWorker.executeQuery(sql, true, binds.toArray(new Object[0]));
//					while(disconnected.next()) {
//						String tsuid = disconnected.getString(5);
//						tsMetas.add(TSMeta.getTSMeta(tsdb, tsuid).join(1000));  // FIXME:  Chain the callbacks, then add the timeout
//					}
					// =================================================================================================================================
					//	LOADS TSMETAS FROM THE SQL CATALOG DB
					// =================================================================================================================================
					tsMetas.addAll(metaReader.readTSMetas(sqlWorker.executeQuery(sqlBuffer.toString(), true, binds.toArray(new Object[0])), true));					
					def.callback(tsMetas);
				} catch (Exception ex) {
					log.error("Failed to execute getTagValues (with tags).\nSQL was [{}]", sqlBuffer, ex);
					def.callback(ex);					
				}
			}
		});
		return def;
	}

	
	// BEST VALUE QUERY SO FAR
//	SELECT DISTINCT F.NODE, F.PORDER, K.NAME, V.NAME
//	FROM TSD_TAGV V, TSD_FQN_TAGPAIR F, TSD_TAGPAIR P, TSD_TAGK K
//	WHERE V.XUID = P.TAGV
//	AND K.XUID = P.TAGK
//	AND P.XUID = F.XUID
//	AND EXISTS (
//		SELECT 1
//		FROM TSD_TAGK XA, TSD_TSMETA FA, TSD_FQN_TAGPAIR TA, TSD_TAGPAIR PA
//		WHERE FA.FQNID = TA.FQNID
//		AND XA.XUID = K.XUID	
//		AND TA.XUID = PA.XUID
//		AND PA.TAGK = XA.XUID	
//		AND (XA.NAME != 'host')
//		AND (XA.NAME != 'type')
//		AND XA.XUID <= 'FFFFFF'
//		AND TA.FQN_TP_ID = F.FQN_TP_ID	
//		AND EXISTS (
//			SELECT 1 
//			FROM TSD_TSMETA TB, TSD_METRIC MB
//			WHERE TB.METRIC_UID = MB.XUID
//			AND TA.FQNID = TB.FQNID
//			AND MB.NAME = 'sys.cpu'
//		)
//	)
//	ORDER BY NODE,  V.NAME
	
		
	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.meta.api.MetricsMetaAPI#evaluate(net.opentsdb.meta.api.QueryContext, java.lang.String[])
	 */
	@Override
	public Deferred<Set<TSMeta>> evaluate(QueryContext queryOptions, String... expressions) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void uncaughtException(Thread t, Throwable e) {
		log.error("Caught exception on thread [{}]", t, e);
		
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




/*
POIFECT VALUE SQL:
==================
SELECT
*
FROM
(
   SELECT
   DISTINCT T.NODE, V.*
   FROM TSD_TSMETA X,
   TSD_METRIC M,
   TSD_FQN_TAGPAIR T,
   TSD_TAGPAIR P,
   TSD_TAGK K,
   TSD_TAGV V
   WHERE M.XUID = X.METRIC_UID
   AND X.FQNID = T.FQNID
   AND T.XUID = P.XUID
   AND P.TAGK = K.XUID
   AND (M.NAME = 'sys.cpu')
   AND P.TAGV = V.XUID
   AND (K.NAME = 'dc')
   AND (V.NAME = 'dc3' OR V.NAME = 'dc4') UNION
   SELECT
   DISTINCT T.NODE, V.*
   FROM TSD_TSMETA X,
   TSD_METRIC M,
   TSD_FQN_TAGPAIR T,
   TSD_TAGPAIR P,
   TSD_TAGK K,
   TSD_TAGV V
   WHERE M.XUID = X.METRIC_UID
   AND X.FQNID = T.FQNID
   AND T.XUID = P.XUID
   AND P.TAGK = K.XUID
   AND (M.NAME = 'sys.cpu')
   AND P.TAGV = V.XUID
   AND (K.NAME = 'host')
   AND (V.NAME LIKE 'Web%1') UNION
   SELECT
   DISTINCT T.NODE, V.*
   FROM TSD_TSMETA X,
   TSD_METRIC M,
   TSD_FQN_TAGPAIR T,
   TSD_TAGPAIR P,
   TSD_TAGK K,
   TSD_TAGV V
   WHERE M.XUID = X.METRIC_UID
   AND X.FQNID = T.FQNID
   AND T.XUID = P.XUID
   AND P.TAGK = K.XUID
   AND (M.NAME = 'sys.cpu')
   AND P.TAGV = V.XUID
   AND (K.NAME = 'type')
   AND (V.NAME = 'combined')
)
X
WHERE NODE = 'L'
--WHERE X.TSUID <= 'FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF'
--ORDER BY X.TSUID DESC LIMIT 300
ORDER BY X.XUID DESC LIMIT 300

====================================

NON OVERFLOWING TSMETAS:
========================

	SELECT X.*
	FROM TSD_FQN_TAGPAIR T, TSD_TSMETA X WHERE X.FQNID = T.FQNID
	GROUP BY X.FQNID
	HAVING SUM(CASE WHEN T.XUID IN (
	   SELECT DISTINCT P.XUID FROM TSD_TAGPAIR P, TSD_TAGK K, TSD_TAGV V WHERE P.TAGK = K.XUID AND P.TAGV = V.XUID AND ((K.NAME = 'dc') AND (V.NAME = 'dc1'))
	   UNION ALL
	   SELECT DISTINCT P.XUID FROM TSD_TAGPAIR P, TSD_TAGK K, TSD_TAGV V WHERE P.TAGK = K.XUID AND P.TAGV = V.XUID AND ((K.NAME = 'host') AND (V.NAME = 'WebServer1'))
	   UNION ALL
	   SELECT DISTINCT P.XUID FROM TSD_TAGPAIR P, TSD_TAGK K, TSD_TAGV V WHERE P.TAGK = K.XUID AND P.TAGV = V.XUID AND ((K.NAME = 'type') AND (V.NAME = 'combined'))	
	) THEN 1 ELSE 0 END) = 3


	SELECT X.* FROM TSD_FQN_TAGPAIR T, TSD_TSMETA X WHERE X.FQNID = T.FQNID GROUP BY X.FQNID HAVING SUM(CASE WHEN T.XUID IN ( ? ) THEN 1 ELSE 0 END) = ?  



*/