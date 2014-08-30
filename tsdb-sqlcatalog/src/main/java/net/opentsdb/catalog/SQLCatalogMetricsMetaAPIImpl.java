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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.management.ObjectName;

import net.opentsdb.catalog.cache.TagPredicateCache;
import net.opentsdb.catalog.datasource.CatalogDataSource;
import net.opentsdb.core.Const;
import net.opentsdb.core.TSDB;
import net.opentsdb.meta.Annotation;
import net.opentsdb.meta.TSMeta;
import net.opentsdb.meta.UIDMeta;
import net.opentsdb.meta.api.MetricsMetaAPI;
import net.opentsdb.meta.api.QueryContext;
import net.opentsdb.tsd.BadRequestException;
import net.opentsdb.tsd.HttpQuery;
import net.opentsdb.tsd.HttpRpc;
import net.opentsdb.tsd.RpcHandler;
import net.opentsdb.uid.UniqueId;
import net.opentsdb.uid.UniqueId.UniqueIdType;
import net.opentsdb.utils.Config;

import org.helios.tsdb.plugins.async.AsyncDispatcherExecutor;
import org.helios.tsdb.plugins.remoting.json.serialization.Serializers;
import org.helios.tsdb.plugins.service.PluginContext;
import org.helios.tsdb.plugins.service.PluginContextImpl;
import org.helios.tsdb.plugins.util.ConfigurationHelper;
import org.helios.tsdb.plugins.util.JMXHelper;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stumbleupon.async.Callback;
import com.stumbleupon.async.Deferred;


/**
 * <p>Title: SQLCatalogMetricsMetaAPIImpl</p>
 * <p>Description: The {@link MetricsMetaAPI} implementation for the SQL Catalof</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>net.opentsdb.catalog.SQLCatalogMetricsMetaAPIImpl</code></p>
 */

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
	
	/** Empty UIDMeta set const */
	private static final Set<UIDMeta> EMPTY_UIDMETA_SET =  Collections.unmodifiableSet(new LinkedHashSet<UIDMeta>(0));
	/** Empty TSMeta set const */
	private static final Set<TSMeta> EMPTY_TSMETA_SET =  Collections.unmodifiableSet(new LinkedHashSet<TSMeta>(0));
	/** Empty Annotation set const */
	private static final Set<Annotation> EMPTY_ANNOTATION_SET =  Collections.unmodifiableSet(new LinkedHashSet<Annotation>(0));
	
	
	/** Empty string array const */
	public static final String[] EMPTY_STR_ARR = {};
	/** Pipe parser pattern */
	public static final Pattern SPLIT_PIPES = Pattern.compile("\\|");
	
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

		/** The FQNID Retrieval SQL template for matching annotations */
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
		
		
		/** The Metric Name Retrieval SQL template when tag pairs are provided */
		public static final String GET_ANN_TSMETAS_SQL =
				"SELECT DISTINCT X.FQNID FROM TSD_TSMETA X, TSD_METRIC M, TSD_FQN_TAGPAIR T, TSD_TAGPAIR P, TSD_TAGK K, TSD_TAGV V " +
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
		
	
	static {
		char[] fs = new char[4 + (2*4*Const.MAX_NUM_TAGS)];
		Arrays.fill(fs, 'F');
		MAX_TSUID = new String(fs);		
	}

	/** The initial start range if no starting index is supplied. Token is the target table alias  */
	public static final String INITIAL_TSUID_START_SQL = " X.TSUID <= '" + MAX_TSUID + "'";
	/** The initial start range if a starting index is supplied. Token is the target table alias */
	public static final String TSUID_START_SQL = " X.TSUID < ? " ;

	/*
	 * 
	 * Outer select from TSD_TAGV where exists
	 * 	Same as TSDMeta query but:
	 * 		match provided tags exactly, but not (TAGK = target tag key)
	 *  
	 */
	
	public static final String GET_TAG_VALUES_SQL =
			"SELECT " +
			"DISTINCT X.* " +
			"FROM TSD_TAGV X, TSD_TSMETA F, TSD_FQN_TAGPAIR T, TSD_TAGPAIR P, TSD_METRIC M, TSD_TAGK K " +
			"WHERE M.XUID = F.METRIC_UID " +
			"AND F.FQNID = T.FQNID " +
			"AND T.XUID = P.XUID " +
			"AND P.TAGV = X.XUID " +
			"AND P.TAGK = K.XUID " +
			"AND (%s) " +   // M.NAME = 'sys.cpu'  --> METRIC_SQL_BLOCK
			"AND (%s) ";	// K.NAME = 'cpu'   -->  TAGK_SQL_BLOCK

//	public static final String INITIAL_XUID_START_SQL = " X.XUID <= 'FFFFFF'";
//	public static final String XUID_START_SQL = " X.XUID < ? " ;
	
	public static final String GET_TAG_FILTER_SQL = 
		      "SELECT 1 " +
		      "FROM TSD_FQN_TAGPAIR TA, TSD_TAGPAIR PA, TSD_TAGK KA, TSD_TAGV VA " +
		      "WHERE TA.FQNID = F.FQNID " +
		      "AND TA.XUID = PA.XUID " +
		      "AND PA.TAGK = KA.XUID " +
		      "AND PA.TAGV = VA.XUID " +
		      "AND ( " +
			  " %s " + 		// ((KA.NAME = 'dc') AND (VA.NAME = 'dc4'))
		      " )";
			
	

	
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
		Serializers.setTSDB(tsdb);
		this.metaReader = metaReader;
		tagPredicateCache = new TagPredicateCache(sqlWorker);
		loadContent();
		ctx.setResource(getClass().getSimpleName(), this);	
		new MetricUIHandler();
	}
	
	private class MetricUIHandler implements HttpRpc {
		final String contentDir;
		final String staticDir;
		
		  /**
		   * Constructor.
		   */
		  public MetricUIHandler() {
			  staticDir = tsdb.getConfig().getDirectoryName("tsd.http.staticroot");
			  contentDir = System.getProperty("metricui.staticroot", staticDir);
			  RpcHandler.getInstance().registerHandler("metricapi-ui", this);
		  }

		  public void execute(final TSDB tsdb, final HttpQuery query)
		    throws IOException {
		    final String uri = query.request().getUri();
		    if ("/favicon.ico".equals(uri)) {
		      query.sendFile(staticDir 
		          + "/favicon.ico", 31536000 /*=1yr*/);
		      return;
		    }
		    if (uri.length() < 3) {  // Must be at least 3 because of the "/s/".
		      throw new BadRequestException("URI too short <code>" + uri + "</code>");
		    }
		    // Cheap security check to avoid directory traversal attacks.
		    // TODO(tsuna): This is certainly not sufficient.
		    if (uri.indexOf("..", 3) > 0) {
		      throw new BadRequestException("Malformed URI <code>" + uri + "</code>");
		    }
		    final int questionmark = uri.indexOf('?', 3);
		    final int pathend = questionmark > 0 ? questionmark : uri.length();
		    query.sendFile(contentDir
		                 + uri.substring(1, pathend),  // Drop the "/s"
		                   uri.contains("nocache") ? 0 : 31536000 /*=1yr*/);
		  }		
	}
	
	public static void mainx(String[] args) {
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
			} else if (bind.getClass().isArray()) {
				sql = m.replaceFirst(renderArray(bind));
			} else {
				sql = m.replaceFirst(bind.toString());
			}		
			m = Q_PATTERN.matcher(sql);
		}
		return sql;
	}
	
	public static String renderArray(final Object array) {
		if(array==null) return "";
		final int length = java.lang.reflect.Array.getLength(array);
		if(length < 1) return "";
		final boolean quote = CharSequence.class.isAssignableFrom(array.getClass().getComponentType());
		final StringBuilder b = new StringBuilder();
		for(int i = 0; i < length; i++) {
			if(quote) b.append("'");
			b.append(java.lang.reflect.Array.get(array, i));
			if(quote) b.append("'");
			b.append(", ");
		}
		return b.deleteCharAt(b.length()-1).deleteCharAt(b.length()-1).toString();
		
	}
	
	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.meta.api.MetricsMetaAPI#getTagKeys(net.opentsdb.meta.api.QueryContext, java.lang.String, java.lang.String[])
	 */
	@Override
	public Deferred<Set<UIDMeta>> getTagKeys(final QueryContext queryContext, final String metric, final String...tagKeys) {
		return getUIDsFor(null, queryContext, UniqueId.UniqueIdType.TAGK, UniqueId.UniqueIdType.METRIC, metric, tagKeys);
	}
	
	
	

	/**
	 * Shuts down this service
	 */
	public void shutdown() {
		metaQueryExecutor.shutdown();
	}
	
	/**
	 * Executes a single correlated UID query, where the target is the type we want to query and the filter is the correlation data.
	 * @param priorDeferred An optional deferred result from a prior continuous call. If null, this is assumed
	 * to be the first call and a new deferred will be created.
	 * @param queryContext The query options 
	 * @param targetType The type we're looking up
	 * @param filterType The type we're using to filter
	 * @param filterName The primary filter name driver
	 * @param excludes Values of the target name that we don't want
	 * @return A deferred result to a set of matching UIDMetas of the type defined in the target argument
	 */
	protected Deferred<Set<UIDMeta>> getUIDsFor(final Deferred<Set<UIDMeta>> priorDeferred, final QueryContext queryContext, final UniqueId.UniqueIdType targetType, final UniqueId.UniqueIdType filterType,  final String filterName, final String...excludes) {
		final Deferred<Set<UIDMeta>> def = priorDeferred==null ? new Deferred<Set<UIDMeta>>() : priorDeferred;
		final String _filterName = (filterName==null || filterName.trim().isEmpty()) ? "*" : filterName.trim();
		this.metaQueryExecutor.execute(new Runnable() {
			@SuppressWarnings("boxing")
			public void run() {
				final List<Object> binds = new ArrayList<Object>();
				String sql = null;
				final String predicate = expandPredicate(_filterName, TAGK_SQL_BLOCK, binds);
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
					
					if(queryContext.getNextIndex()==null) {
						sql = String.format(GET_KEY_TAGS_SQL, targetType, filterType, predicate, keyBinds, INITIAL_XUID_START_SQL); 
					} else {
						sql = String.format(GET_KEY_TAGS_SQL, targetType, filterType, predicate, keyBinds, XUID_START_SQL);
						binds.add(queryContext.getNextIndex());
					}
					final int modPageSize = queryContext.getNextMaxLimit() + 1;
					binds.add(modPageSize);					
					log.info("Executing SQL [{}]", fillInSQL(sql, binds));
					final Set<UIDMeta> uidMetas = closeOutUIDMetaResult(modPageSize, queryContext, 
							metaReader.readUIDMetas(sqlWorker.executeQuery(sql, true, binds.toArray(new Object[0])), targetType)
					);
					def.callback(uidMetas);
					if(queryContext.shouldContinue()) {
						getUIDsFor(def, queryContext, targetType, filterType, _filterName, excludes);
					}					
				} catch (Exception ex) {
					log.error("Failed to execute getTagKeysFor.\nSQL was [{}]", sql, ex);
					def.callback(ex);
				}
			}
		});
		
		return def;
	} 

	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.meta.api.MetricsMetaAPI#getMetricNames(net.opentsdb.meta.api.QueryContext, java.lang.String[])
	 */
	@Override
	public Deferred<Set<UIDMeta>> getMetricNames(final QueryContext queryContext, final String... tagKeys) {
		return getMetricNames(null, queryContext, tagKeys);
	}
	
	/**
	 * Returns the associated metric names (metric UIDs) for the passed tag keys.
	 * Wildcards will be honoured on metric names and tag keys.
	 * @param priorDeferred An optional deferred result from a prior continuous call. If null, this is assumed
	 * to be the first call and a new deferred will be created.
	 * @param queryContext The query context 
	 * @param tagKeys an array of tag keys to exclude
	 * @return the deferred result
	 */
	protected Deferred<Set<UIDMeta>> getMetricNames(final Deferred<Set<UIDMeta>> priorDeferred, final QueryContext queryContext, final String... tagKeys) {
		final Deferred<Set<UIDMeta>> def = priorDeferred==null ? new Deferred<Set<UIDMeta>>() : priorDeferred;		
		this.metaQueryExecutor.execute(new Runnable() {
			@SuppressWarnings("boxing")
			public void run() {				
				final List<Object> binds = new ArrayList<Object>();
				
				String sql = null;
				try {
					if(tagKeys==null || tagKeys.length==0) {										
						if(queryContext.getNextIndex()==null) {
							sql = String.format(GET_METRIC_NAMES_SQL,INITIAL_XUID_START_SQL); 
						} else {
							sql = String.format(GET_METRIC_NAMES_SQL, XUID_START_SQL);
							binds.add(queryContext.getNextIndex());
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
						if(queryContext.getNextIndex()==null) {
							keySql.append(" WHERE ").append(INITIAL_XUID_START_SQL);
						} else {
							keySql.append(" WHERE ").append(XUID_START_SQL);
							binds.add(queryContext.getNextIndex());
						}
						keySql.append(" ORDER BY X.XUID DESC LIMIT ? ");
						sql = keySql.toString();
					}
					final int modPageSize = queryContext.getNextMaxLimit() + 1;
					binds.add(modPageSize);
					log.info("Executing SQL [{}]", fillInSQL(sql, binds));
					final Set<UIDMeta> uidMetas = closeOutUIDMetaResult(modPageSize, queryContext, 
							metaReader.readUIDMetas(sqlWorker.executeQuery(sql, true, binds.toArray(new Object[0])), UniqueId.UniqueIdType.METRIC)
					);
					def.callback(uidMetas);
					if(queryContext.shouldContinue()) {
						getMetricNames(def, queryContext, tagKeys);
					}					
				} catch (Exception ex) {
					log.error("Failed to execute getMetricNamesFor.\nSQL was [{}]", sql, ex);
					def.callback(ex);					
				}
			}
		});
		return def;
	}
	

	
	/**
	 * Closes out a UIDMeta query result, updating the QueryContext to indicate if the results are exhausted,
	 * and if not, what the next page start index is
	 * @param modPageSize The modified page size (one more than what was requested)
	 * @param ctx The query context to update
	 * @param readMetas The materialized UIDMetas being returned
	 * @return The set of UIDMetas to return
	 */
	protected static Set<UIDMeta> closeOutUIDMetaResult(final int modPageSize, final QueryContext ctx, final List<UIDMeta> readMetas) {
		if(readMetas.isEmpty()) {
			ctx.setExhausted(true).setNextIndex(null);			
			return EMPTY_UIDMETA_SET;
		}
		final int size = readMetas.size(); 
		if(size < modPageSize) {
			// the result set size was less than actually requested
			// so the query is exhausted
			ctx.setExhausted(true).setNextIndex(null).incrementCummulative(size);			
		} else {
			// the result set size was equal to what was actually requested
			// so the result set is still live.
			// the result list has +1 over the caller specified, so:
			//	1. The last item should be removed (it is excess)
			//	2. The nextIndex is the index of the last item						
			ctx.setExhausted(false).setNextIndex(readMetas.get(size-2).getUID()).incrementCummulative(size-1);
			readMetas.remove(size-1);
		}
		return readMetas.isEmpty() ? EMPTY_UIDMETA_SET : new LinkedHashSet<UIDMeta>(readMetas);
	}
	
	/**
	 * Closes out an Annotation query result, updating the QueryContext to indicate if the results are exhausted,
	 * and if not, what the next page start index is
	 * @param modPageSize The modified page size (one more than what was requested)
	 * @param ctx The query context to update
	 * @param readAnnotations The materialized Annotations being returned
	 * @return The set of Annotations to return
	 */
	protected static Set<Annotation> closeOutAnnotationResult(final int modPageSize, final QueryContext ctx, final List<Annotation> readAnnotations) {
		if(readAnnotations.isEmpty()) {
			ctx.setExhausted(true).setNextIndex(null);			
			return EMPTY_ANNOTATION_SET;
		}
		final int size = readAnnotations.size(); 
		if(size < modPageSize) {
			// the result set size was less than actually requested
			// so the query is exhausted
			ctx.setExhausted(true).setNextIndex(null).incrementCummulative(size);			
		} else {
			// the result set size was equal to what was actually requested
			// so the result set is still live.
			// the result list has +1 over the caller specified, so:
			//	1. The last item should be removed (it is excess)
			//	2. The nextIndex is the index of the last item						
			ctx.setExhausted(false).setNextIndex(readAnnotations.get(size-2).getStartTime()).incrementCummulative(size-1);
			readAnnotations.remove(size-1);
		}
		return readAnnotations.isEmpty() ? EMPTY_ANNOTATION_SET : new LinkedHashSet<Annotation>(readAnnotations);
	}
	
	
	/**
	 * Closes out a TSMeta query result, updating the QueryContext to indicate if the results are exhausted,
	 * and if not, what the next page start index is
	 * @param modPageSize The modified page size (one more than what was requested)
	 * @param ctx The query context to update
	 * @param readMetas The materialized TSMetas being returned
	 * @return The set of TSMetas to return
	 */
	protected static Set<TSMeta> closeOutTSMetaResult(final int modPageSize, final QueryContext ctx, final List<TSMeta> readMetas) {		
		if(readMetas.isEmpty()) {
			ctx.setExhausted(true).setNextIndex(null);			
			return EMPTY_TSMETA_SET;
		}
		final int size = readMetas.size(); 		
		if(size < modPageSize) {
			// the result set size was less than actually requested
			// so the query is exhausted
			ctx.setExhausted(true).setNextIndex(null).incrementCummulative(size);			
		} else {
			// the result set size was equal to what was actually requested
			// so the result set is still live.
			// the result list has +1 over the caller specified, so:
			//	1. The last item should be removed (it is excess)
			//	2. The nextIndex is the index of the last item						
			ctx.setExhausted(false).setNextIndex(readMetas.get(size-2).getTSUID()).incrementCummulative(size-1);
			readMetas.remove(size-1);
		}
		return readMetas.isEmpty() ? EMPTY_TSMETA_SET : new LinkedHashSet<TSMeta>(readMetas);
	}
	

	
	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.meta.api.MetricsMetaAPI#getMetricNames(net.opentsdb.meta.api.QueryContext, java.util.Map)
	 */
	@Override
	public Deferred<Set<UIDMeta>> getMetricNames(final QueryContext queryContext, final Map<String, String> tags) {
		return getMetricNames(null, queryContext, tags);
	}
	
	
	/**
	 * Returns the associated metric names (metric UIDs) for the passed tag pairs.
	 * Wildcards will be honoured on metric names and tag keys.
	 * @param priorDeferred An optional deferred result from a prior continuous call. If null, this is assumed
	 * to be the first call and a new deferred will be created.
	 * @param queryContext The query context 
	 * @param tags The TSMeta tags to match
	 * @return the deferred result
	 */
	protected Deferred<Set<UIDMeta>> getMetricNames(final Deferred<Set<UIDMeta>> priorDeferred, final QueryContext queryContext, final Map<String, String> tags) {
		final Deferred<Set<UIDMeta>> def = priorDeferred==null ? new Deferred<Set<UIDMeta>>() : priorDeferred;
		final Map<String, String> _tags = (tags==null) ? EMPTY_TAGS : tags;
		this.metaQueryExecutor.execute(new Runnable() {
			@SuppressWarnings("boxing")
			public void run() {				
				final List<Object> binds = new ArrayList<Object>();
				final List<String> likeOrEquals = new ArrayList<String>();
				String sql = null;
				try {
					if(_tags==null || _tags.isEmpty()) {										
						if(queryContext.getNextIndex()==null) {
							sql = String.format(GET_METRIC_NAMES_SQL,INITIAL_XUID_START_SQL); 
						} else {
							sql = String.format(GET_METRIC_NAMES_SQL, XUID_START_SQL);
							binds.add(queryContext.getNextIndex());
						}
					} else {
//						String predicate = expandPredicate(tagKeys[0], TAGK_SQL_BLOCK, binds)
						Iterator<Map.Entry<String, String>> iter = _tags.entrySet().iterator();
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
						if(queryContext.getNextIndex()==null) {
							keySql.append(" WHERE ").append(INITIAL_XUID_START_SQL);
						} else {
							keySql.append(" WHERE ").append(XUID_START_SQL);
							binds.add(queryContext.getNextIndex());
						}
						keySql.append(" ORDER BY X.XUID DESC LIMIT ? ");
						sql = String.format(keySql.toString(), likeOrEquals.toArray());
					}
					final int modPageSize = queryContext.getNextMaxLimit() + 1; 
					binds.add(modPageSize);
					log.info("Executing SQL [{}]", fillInSQL(sql, binds));
					final Set<UIDMeta> uidMetas = closeOutUIDMetaResult(modPageSize, queryContext, 
							metaReader.readUIDMetas(sqlWorker.executeQuery(sql, true, binds.toArray(new Object[0])), UniqueId.UniqueIdType.METRIC)
					);
					def.callback(uidMetas);
					if(queryContext.shouldContinue()) {
						getMetricNames(def, queryContext, _tags);
					}
				} catch (Exception ex) {
					log.error("Failed to execute getMetricNamesFor (with tags).\nSQL was [{}]", sql, ex);
					def.callback(ex);					
				}
			}
		});
		return def;
	}

	/** An empty tags map const */
	public static final Map<String, String> EMPTY_TAGS = Collections.unmodifiableMap(new HashMap<String, String>(0));
	
	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.meta.api.MetricsMetaAPI#getTSMetas(net.opentsdb.meta.api.QueryContext, java.lang.String, java.util.Map)
	 */
	@Override
	public Deferred<Set<TSMeta>> getTSMetas(final QueryContext queryContext, final String metricName, final Map<String, String> tags) {
		return getTSMetas(null, queryContext, metricName, tags);
	}
		
	
	/**
	 * Executes a TSMetas query
	 * @param priorDeferred An optional deferred result from a prior continuous call. If null, this is assumed
	 * to be the first call and a new deferred will be created.
	 * @param queryContext The query context
	 * @param metricName The optional TSMeta metric name or expression. Will be substituted with <b><code>*</code></b> if null or empty
	 * @param tags The optional TSMeta tags. Will be substituted with an empty map if null.
	 * @return the deferred result
	 */
	protected Deferred<Set<TSMeta>> getTSMetas(final Deferred<Set<TSMeta>> priorDeferred, final QueryContext queryContext, final String metricName, final Map<String, String> tags) {
		final Deferred<Set<TSMeta>> def = priorDeferred==null ? new Deferred<Set<TSMeta>>() : priorDeferred;
		final String _metricName = (metricName==null || metricName.trim().isEmpty()) ? "*" : metricName.trim();
		final Map<String, String> _tags = (tags==null) ? EMPTY_TAGS : tags;
		this.metaQueryExecutor.execute(new Runnable() {
			@SuppressWarnings("boxing")
			public void run() {				
				final List<Object> binds = new ArrayList<Object>();
//				String sql = null;
				final StringBuilder sqlBuffer = new StringBuilder();
				try {
					generateTSMetaSQL(sqlBuffer, binds, queryContext, _metricName, _tags, "INTERSECT");
					final int modPageSize = queryContext.getNextMaxLimit() + 1;
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
					
					final Set<TSMeta> tsMetas = closeOutTSMetaResult(modPageSize, queryContext, 
							metaReader.readTSMetas(sqlWorker.executeQuery(sqlBuffer.toString(), true, binds.toArray(new Object[0])), true)							
					); 
					def.callback(tsMetas);
					if(queryContext.shouldContinue()) {
						getTSMetas(def, queryContext, _metricName, _tags);
					}
				} catch (Exception ex) {
					log.error("Failed to execute getTSMetas (with tags).\nSQL was [{}]", sqlBuffer, ex);
					def.callback(ex);					
				}
			}
		});
		return def;
		
	}
	

	/**
	 * Generates a TSMeta query
	 * @param sqlBuffer The sql buffer to append generated SQL into
	 * @param binds The query bind values
	 * @param queryOptions The query options
	 * @param metricName The metric name
	 * @param tags The tag pairs
	 */
	protected void generateTSMetaSQL(final StringBuilder sqlBuffer, final List<Object> binds, final QueryContext queryOptions, final String metricName, final Map<String, String> tags, final String mergeOp) {
		final boolean hasMetricName = (metricName==null || metricName.trim().isEmpty());
		if(tags==null || tags.isEmpty()) {	
			if(queryOptions.getNextIndex()==null) {							
				sqlBuffer.append(String.format(hasMetricName ? GET_TSMETAS_NO_TAGS_NAME_SQL : GET_TSMETAS_NO_TAGS_NO_METRIC_NAME_SQL, INITIAL_TSUID_START_SQL)); 
			} else {
				sqlBuffer.append(String.format(hasMetricName ? GET_TSMETAS_NO_TAGS_NAME_SQL : GET_TSMETAS_NO_TAGS_NO_METRIC_NAME_SQL, TSUID_START_SQL));
				binds.add(queryOptions.getNextIndex());
			}
			if(hasMetricName) binds.add(metricName);
			
		} else {
			StringBuilder keySql = new StringBuilder("SELECT * FROM ( ");
			prepareGetTSMetasSQL(metricName, tags, binds, keySql, GET_TSMETAS_SQL, mergeOp);
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
		
	protected void doGetTSMetasSQLTagValues(final boolean firstEntry, final String metricName, Map.Entry<String, String> tag, final List<Object> binds, final StringBuilder sql, final String driverTsMetaSql, String mergeOp) {
		// // expandPredicate(entry.getKey(), TAGK_SQL_BLOCK, binds),
		if(!firstEntry) {
			sql.append("\n ").append(mergeOp).append("  \n");
			
		}
		
		sql.append(String.format(driverTsMetaSql,  
				expandPredicate(metricName, METRIC_SQL_BLOCK, binds),
				expandPredicate(tag.getKey(), TAGK_SQL_BLOCK, binds),
				expandPredicate(tag.getValue(), TAGV_SQL_BLOCK, binds)
		));
	}
	
	protected void prepareGetTSMetasSQL(final String metricName, final Map<String, String> tags, final List<Object> binds, final StringBuilder sql, final String driverTsMetaSql, String mergeOp) {
		Iterator<Map.Entry<String, String>> iter = tags.entrySet().iterator();
		doGetTSMetasSQLTagValues(true, metricName, iter.next(), binds, sql, driverTsMetaSql, mergeOp);
		while(iter.hasNext()) {
			doGetTSMetasSQLTagValues(false, metricName, iter.next(), binds, sql, driverTsMetaSql, mergeOp);
		}
	}
	
	
	

	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.meta.api.MetricsMetaAPI#getTagValues(net.opentsdb.meta.api.QueryContext, java.lang.String, java.util.Map, java.lang.String)
	 */
	@Override
	public Deferred<Set<UIDMeta>> getTagValues(final QueryContext queryContext, final String metricName, final Map<String, String> tags, final String tagKey) {
		return getTagValues(null, queryContext, metricName, tags, tagKey);
	}
	
	/**
	 * @param priorDeferred An optional deferred result from a prior continuous call. If null, this is assumed
	 * to be the first call and a new deferred will be created.
	 * @param queryContext The query context 
	 * @param metricName The optional metric name to match against
	 * @param tags The TSMeta tags to match against
	 * @param tagKey The tag key to get the values of
	 * @return the deferred result
	 */
	protected Deferred<Set<UIDMeta>> getTagValues(final Deferred<Set<UIDMeta>> priorDeferred, final QueryContext queryContext, final String metricName, final Map<String, String> tags, final String tagKey) {
		final Deferred<Set<UIDMeta>> def = priorDeferred==null ? new Deferred<Set<UIDMeta>>() : priorDeferred;		
		final String _metricName = (metricName==null || metricName.trim().isEmpty()) ? "*" : metricName.trim();
		final String _tagKey = (tagKey==null || tagKey.trim().isEmpty()) ? "*" : tagKey.trim();
		final Map<String, String> _tags = (tags==null) ? EMPTY_TAGS : tags;
		this.metaQueryExecutor.execute(new Runnable() {
			@SuppressWarnings("boxing")
			public void run() {				
				final List<Object> binds = new ArrayList<Object>();
				final StringBuilder sqlBuffer = new StringBuilder(String.format(GET_TAG_VALUES_SQL,
						expandPredicate(_metricName, METRIC_SQL_BLOCK, binds),
						expandPredicate(_tagKey, TAGK_SQL_BLOCK, binds)
				));
				if(!_tags.isEmpty()) {
					sqlBuffer.append(" AND EXISTS ( ");
					boolean first = true;
					for(Map.Entry<String, String> pair: _tags.entrySet()) {
						if(!first) sqlBuffer.append(" \nINTERSECT\n ");
						StringBuilder b = new StringBuilder("( ( ");
						b.append(expandPredicate(pair.getKey(), " KA.NAME %s ? ", binds));
						b.append(" ) AND ( ");
						b.append(expandPredicate(pair.getValue(), " VA.NAME %s ? ", binds));
						b.append(" ) ) ");
						sqlBuffer.append(String.format(GET_TAG_FILTER_SQL, b.toString()));
						first = false;
						
					}
					sqlBuffer.append(" ) ");
				}
				sqlBuffer.append(" AND ");
				if(queryContext.getNextIndex()!=null && !queryContext.getNextIndex().toString().trim().isEmpty()) {
					sqlBuffer.append(XUID_START_SQL);
					binds.add(queryContext.getNextIndex().toString().trim());
				} else {
					sqlBuffer.append(INITIAL_XUID_START_SQL);
				}
				sqlBuffer.append(" ORDER BY X.XUID DESC LIMIT ? ");
				final int modPageSize = queryContext.getNextMaxLimit() + 1;
				binds.add(modPageSize);
				try {
					log.info("Executing SQL [{}]", fillInSQL(sqlBuffer.toString(), binds));
					final Set<UIDMeta> uidMetas = closeOutUIDMetaResult(modPageSize, queryContext, 
							metaReader.readUIDMetas(sqlWorker.executeQuery(sqlBuffer.toString(), true, binds.toArray(new Object[0])), UniqueId.UniqueIdType.TAGV)
					);
					def.callback(uidMetas);
					if(queryContext.shouldContinue()) {
						getTagValues(def, queryContext, _metricName, _tags, _tagKey);
					}										
				} catch (Exception ex) {
					log.error("Failed to execute getTagValues (with tags).\nSQL was [{}]", sqlBuffer, ex);
					def.callback(ex);					
				}
			}
		});
		return def;
	}

	
	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.meta.api.MetricsMetaAPI#evaluate(net.opentsdb.meta.api.QueryContext, java.lang.String)
	 */
	@Override
	public Deferred<Set<TSMeta>> evaluate(final QueryContext queryContext, final String expression) {
		if(expression==null || expression.trim().isEmpty()) {
			return Deferred.fromError(new IllegalArgumentException("The passed expression was null or empty"));
		}
		final String expr = expression.trim();
		try {			
			final ObjectName on = JMXHelper.objectName(expr);
			return getTSMetas(null, queryContext, on.getDomain(), on.getKeyPropertyList());
		} catch (Exception ex) {
			return Deferred.fromError(new Exception("Failed to evaluate expression [" + expr + "]", ex));
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.meta.api.MetricsMetaAPI#getAnnotations(net.opentsdb.meta.api.QueryContext, java.lang.String, long[])
	 */
	@Override
	public Deferred<Set<Annotation>> getAnnotations(final QueryContext queryContext, final String expression, final long... startTimeEndTime) {
		if(expression==null || expression.trim().isEmpty()) { 
			return Deferred.fromError(new IllegalArgumentException("The passed expression was null or empty"));
		}
		if(startTimeEndTime.length!=1 && startTimeEndTime.length!=2) { 
			return Deferred.fromError(new IllegalArgumentException("Invalid startTimeEndTime " + Arrays.toString(startTimeEndTime)));
		}
		final ObjectName on;
		try {
			on = new ObjectName(expression.trim());
		} catch (Exception ex) {
			return Deferred.fromError(new IllegalArgumentException("Invalid TSMeta expression", ex));
		}
		final Deferred<Set<Annotation>> def = new Deferred<Set<Annotation>>();		
		this.metaQueryExecutor.execute(new Runnable() {
			public void run() {
				final List<Object> binds = new ArrayList<Object>();
				final boolean hasExpression = (expression!=null && !expression.trim().isEmpty());
				final Object nextKey = queryContext.getNextIndex();
				final int timeRanges;
				StringBuilder sqlBuffer = new StringBuilder("SELECT * FROM TSD_ANNOTATION A WHERE ");
				if(startTimeEndTime==null || startTimeEndTime.length==0) {
					sqlBuffer.append( " 1 = 1 ");
					timeRanges = 0;
				} else {
					if(startTimeEndTime.length==1) {
						sqlBuffer.append( " START_TIME = ? ");
						binds.add(startTimeEndTime[0]);
						timeRanges = 1;
					} else {
						sqlBuffer.append( " START_TIME >= ? AND END_TIME <= ? ");
						binds.add(startTimeEndTime[0]);						
						binds.add(startTimeEndTime[1]);
						timeRanges = 2;
					}
				}
				if(hasExpression) {
					sqlBuffer.append(" AND FQNID IN ( ");
					prepareGetTSMetasSQL(on.getDomain(), on.getKeyPropertyList(), binds, sqlBuffer, GET_ANN_TSMETAS_SQL, "UNION ALL");					
				}				
				sqlBuffer.append(" ) ");
				if(hasExpression) {
					if(nextKey == null) {
						sqlBuffer.append(" AND FQNID >= 0 ");
					} else {
						sqlBuffer.append(" AND FQNID > ? ");
					}
				}
				switch(timeRanges) {
					case 0:
						sqlBuffer.append(" AND START_TIME >= 0 ");
						
				}
				sqlBuffer.append(" ORDER BY START_TIME desc, FQNID desc, END_TIME desc ");
				final int modPageSize = queryContext.getNextMaxLimit() + 1;
				binds.add(modPageSize);
				try {
					log.info("Executing SQL [{}]", fillInSQL(sqlBuffer.toString(), binds));
					final Set<Annotation> annotations = closeOutAnnotationResult(modPageSize, queryContext, 
							metaReader.readAnnotations(sqlWorker.executeQuery(sqlBuffer.toString(), true, binds.toArray(new Object[0])))
					);
					def.callback(annotations);
				} catch (Exception ex) {
					log.error("Failed to execute find.\nSQL was [{}]", sqlBuffer, ex);
					def.callback(ex);					
				}				
				
				
				
//				StringBuilder keySql = new StringBuilder("SELECT * FROM ( ");
//				prepareGetTSMetasSQL(metricName, tags, binds, keySql, GET_TSMETAS_SQL);
//				keySql.append(") X ");
//				if(queryOptions.getNextIndex()==null) {
//					keySql.append(" WHERE ").append(INITIAL_TSUID_START_SQL);
//				} else {
//					keySql.append(" WHERE ").append(TSUID_START_SQL);
//					binds.add(queryOptions.getNextIndex());
//				}
//				keySql.append(" ORDER BY X.TSUID DESC LIMIT ? ");
//				sqlBuffer.append(keySql.toString());
				
				
//				StringBuilder sqlBuffer = new StringBuilder("SELECT * FROM TSD_")
//					.append(type.name()).append(" X WHERE (")
//					.append(expandPredicate(name.trim(), "X.NAME %s ?", binds))
//					.append(") AND ");
//				if(queryContext.getNextIndex()!=null && !queryContext.getNextIndex().toString().trim().isEmpty()) {
//					sqlBuffer.append(XUID_START_SQL);
//					binds.add(queryContext.getNextIndex().toString().trim());
//				} else {
//					sqlBuffer.append(INITIAL_XUID_START_SQL);
//				}
//				sqlBuffer.append(" ORDER BY X.XUID DESC LIMIT ? ");
//				final int modPageSize = queryContext.getNextMaxLimit() + 1;
//				binds.add(modPageSize);
//				try {
//					log.info("Executing SQL [{}]", fillInSQL(sqlBuffer.toString(), binds));
//					final Set<UIDMeta> uidMetas = closeOutUIDMetaResult(modPageSize, queryContext, 
//							metaReader.readUIDMetas(sqlWorker.executeQuery(sqlBuffer.toString(), true, binds.toArray(new Object[0])), type)
//					);
//					def.callback(uidMetas);
//				} catch (Exception ex) {
//					log.error("Failed to execute find.\nSQL was [{}]", sqlBuffer, ex);
//					def.callback(ex);					
//				}				
			}
		});
		return def;			
		
	}

	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.meta.api.MetricsMetaAPI#getGlobalAnnotations(net.opentsdb.meta.api.QueryContext, long[])
	 */
	@Override
	public Deferred<Set<Annotation>> getGlobalAnnotations(QueryContext queryOptions, long... startTimeEndTime) {
		return null;
	}
	
	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.meta.api.MetricsMetaAPI#find(net.opentsdb.meta.api.QueryContext, net.opentsdb.uid.UniqueId.UniqueIdType, java.lang.String)
	 */
	@Override
	public Deferred<Set<UIDMeta>> find(final QueryContext queryContext, final UniqueIdType type, final String name) {
		if(name==null || name.trim().isEmpty()) { 
			return Deferred.fromError(new IllegalArgumentException("The passed name was null or empty"));
		}
		if(type==null) { 
			return Deferred.fromError(new IllegalArgumentException("The passed type was null"));
		}
		
		final Deferred<Set<UIDMeta>> def = new Deferred<Set<UIDMeta>>();		
		this.metaQueryExecutor.execute(new Runnable() {
			public void run() {
				final List<Object> binds = new ArrayList<Object>();
				StringBuilder sqlBuffer = new StringBuilder("SELECT * FROM TSD_")
					.append(type.name()).append(" X WHERE (")
					.append(expandPredicate(name.trim(), "X.NAME %s ?", binds))
					.append(") AND ");
				if(queryContext.getNextIndex()!=null && !queryContext.getNextIndex().toString().trim().isEmpty()) {
					sqlBuffer.append(XUID_START_SQL);
					binds.add(queryContext.getNextIndex().toString().trim());
				} else {
					sqlBuffer.append(INITIAL_XUID_START_SQL);
				}
				sqlBuffer.append(" ORDER BY X.XUID DESC LIMIT ? ");
				final int modPageSize = queryContext.getNextMaxLimit() + 1;
				binds.add(modPageSize);
				try {
					log.info("Executing SQL [{}]", fillInSQL(sqlBuffer.toString(), binds));
					final Set<UIDMeta> uidMetas = closeOutUIDMetaResult(modPageSize, queryContext, 
							metaReader.readUIDMetas(sqlWorker.executeQuery(sqlBuffer.toString(), true, binds.toArray(new Object[0])), type)
					);
					def.callback(uidMetas);
				} catch (Exception ex) {
					log.error("Failed to execute find.\nSQL was [{}]", sqlBuffer, ex);
					def.callback(ex);					
				}				
			}
		});
		return def;			
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
		String nexpandedValue = value;
		final StringTokenizer st = new StringTokenizer(nexpandedValue.replace(" ", ""), "|", false);
		int segmentCount = st.countTokens();
		if(segmentCount<1) {
			throw new RuntimeException("Failed to parse expression [" + value + "]. Segment count was 0");
		}
		if(segmentCount==1) {
			String val = st.nextToken();
			binds.add(val.replace('*', '%'));
			//String pred = expandNumericRange(predicateBase.replace("%s", val.indexOf('*')==-1 ? "=" : "LIKE"));
			String pred = predicateBase.replace("%s", val.indexOf('*')==-1 ? "=" : "LIKE");
			return pred;
		}
		StringBuilder b = new StringBuilder();
		for(int i = 0; i < segmentCount; i++) {			
			if(i!=0) b.append(" OR ");
			String val = st.nextToken();
			binds.add(val.replace('*', '%'));
			b.append(
//					expandNumericRange(
								predicateBase.replace("%s", val.indexOf('*')==-1 ? "=" : "LIKE")
//					)
			);
		}
		return b.toString();				
	}
	
	/** The content prefix for the metric-ui POC resources */
	public static final String CONTENT_PREFIX = "metricapi-ui";
	
	/**
	 * Loads the Static UI content files from the classpath JAR to the configured static root directory
	 * @param the name of the content directory to write the content to
	 */
	private void loadContent() {
		boolean completed = true;
		final String contentDirectory = tsdb.getConfig().getString("tsd.http.staticroot");
		final File gpDir = new File(contentDirectory);
		final long startTime = System.currentTimeMillis();
		final AtomicInteger filesLoaded = new AtomicInteger();
		final AtomicInteger fileFailures = new AtomicInteger();
		final AtomicInteger fileOlder = new AtomicInteger();
		final AtomicLong bytesLoaded = new AtomicLong();
		String codeSourcePath = getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
		File file = new File(codeSourcePath);
		if( codeSourcePath.endsWith(".jar") && file.exists() && file.canRead() ) {
			JarFile jar = null;
			ChannelBuffer contentBuffer = ChannelBuffers.dynamicBuffer(300000);
			try {
				jar = new JarFile(file);
				final Enumeration<JarEntry> entries = jar.entries(); 
				while(entries.hasMoreElements()) {
					JarEntry entry = entries.nextElement();
					final String name = entry.getName();
					if (name.startsWith(CONTENT_PREFIX + "/")) { 
						final int contentSize = (int)entry.getSize();
						final long contentTime = entry.getTime();
						if(entry.isDirectory()) {
							new File(gpDir, name).mkdirs();
							continue;
						}
						File contentFile = new File(gpDir, name.replace(CONTENT_PREFIX + "/", ""));
						if( !contentFile.getParentFile().exists() ) {
							contentFile.getParentFile().mkdirs();
						}
						if( contentFile.exists() ) {
							if( contentFile.lastModified() < contentTime ) {
								log.debug("File in directory was newer [{}]", name);
								fileOlder.incrementAndGet();
								continue;
							}
							contentFile.delete();
						}
						log.debug("Writing content file [{}]", contentFile );
						contentFile.createNewFile();
						if( !contentFile.canWrite() ) {
							log.warn("Content file [{}] not writable", contentFile);
							fileFailures.incrementAndGet();
							continue;
						}
						FileOutputStream fos = null;
						InputStream jis = null;
						try {
							fos = new FileOutputStream(contentFile);
							jis = jar.getInputStream(entry);
							contentBuffer.writeBytes(jis, contentSize);
							contentBuffer.readBytes(fos, contentSize);
							fos.flush();
							jis.close(); jis = null;
							fos.close(); fos = null;
							filesLoaded.incrementAndGet();
							bytesLoaded.addAndGet(contentSize);
							log.debug("Wrote content file [{}] + with size [{}]", contentFile, contentSize );
						} finally {
							if( jis!=null ) try { jis.close(); } catch (Exception ex) {}
							if( fos!=null ) try { fos.close(); } catch (Exception ex) {}
						}
					}  // not content
				} // end of while loop
			} catch (Exception ex) {
				log.error("Failed to export metricapi-ui content from Jar", ex);	
				completed = false;
			} finally {
				if( jar!=null ) try { jar.close(); } catch (Exception x) { /* No Op */}
			}
		}  else {	// end of was-not-a-jar
			try {
				File contentRootDir = new File("./src/main/resources/" + CONTENT_PREFIX);
				if(!contentRootDir.exists() || !contentRootDir.isDirectory()) {
					throw new Exception("Failed to locate content source directory [" + contentRootDir + "]");					
				}
				if(!gpDir.exists() || !gpDir.isDirectory()) {
					throw new Exception("Failed to locate content target directory [" + gpDir + "]");					
				}
				File targetDirectory = new File(gpDir, CONTENT_PREFIX);
				targetDirectory.mkdirs();
				recursiveFileCopy(contentRootDir, targetDirectory, filesLoaded, fileFailures, fileOlder, bytesLoaded);
			} catch (Exception ex) {
				log.error("Failed to export metricapi-ui content from Jar", ex);	
				completed = false;				
			}
		}
		final long elapsed = System.currentTimeMillis()-startTime;
		StringBuilder b = new StringBuilder("\n\n\t===================================================\n\tStatic Root Directory:[").append(contentDirectory).append("]");
		b.append("\n\tTotal Files Written:").append(filesLoaded.get());
		b.append("\n\tTotal Bytes Written:").append(bytesLoaded.get());
		b.append("\n\tFile Write Failures:").append(fileFailures.get());
		b.append("\n\tFile Older Than Content:").append(fileOlder.get());
		b.append("\n\tElapsed (ms):").append(elapsed);
		b.append("\n\t===================================================\n");
		log.info(b.toString());		
	}
	
	private void recursiveFileCopy(File srcDir, File targetDir, 
			final AtomicInteger filesLoaded, 
			final AtomicInteger fileFailures,
			final AtomicInteger fileOlder,
			final AtomicLong bytesLoaded) {
		for(File f: srcDir.listFiles()) {
			if(f.isFile()) {
				File newFile = new File(targetDir + File.separator + f.getName());
				if(newFile.exists()) {
					if(newFile.lastModified() >= f.lastModified()) {
						fileOlder.incrementAndGet();
						continue;
					}
					newFile.delete();
				}
				try {
					if(!newFile.createNewFile()) {
						fileFailures.incrementAndGet();
						continue;
					}
				} catch (Exception ex) {
					fileFailures.incrementAndGet();
					continue;					
				}
				try {
					bytesLoaded.addAndGet(copyFile(f, newFile));
					filesLoaded.incrementAndGet();
				} catch (Exception ex) {
					fileFailures.incrementAndGet();
					continue;					
				}
			} else if(f.isDirectory()) {
				File nextDir = new File(targetDir, f.getName());
				recursiveFileCopy(f, nextDir, filesLoaded, fileFailures, fileOlder, bytesLoaded);
			}
		}
	}

    public static long copyFile(File in, File out)  throws IOException {
    	FileChannel inChannel = new FileInputStream(in).getChannel();
        FileChannel outChannel = new FileOutputStream(out).getChannel();
        try {
        	return inChannel.transferTo(0, inChannel.size(), outChannel);
        }  catch (IOException e) {
        	throw e;
        }
        finally {
        	if (inChannel != null) inChannel.close();
        	if (outChannel != null) outChannel.close();
        }
     }
	
	

//	public static void main(String[] args) { 
//		log("Range Test");
//		String expr = "WebServer[8,9,11-20]";  // |DBServer[8,9,11-20]
//		List<Object> binds = new ArrayList<Object>();
//		log("Expression:" + expr);
//		log("Expanded:" + expandNumericRange(expr));
//		log("Expanded:" + fillInSQL(expandPredicate(expr, TAGK_SQL_BLOCK, binds), binds));
//	}
	
	/** Regex pattern that defines a range of numbers */
	protected static final Pattern intRange = Pattern.compile("\\[(?:(([0-9]+(-[0-9]+)?)(,([0-9]+(-[0-9]+)?))*)\\])");

	
	public static String expandNumericRange(final String valuesStr) {
    	if(valuesStr==null || valuesStr.trim().isEmpty()) return valuesStr;
    	final String token = "{==%s==}";
    	TreeMap<Integer, int[]> ranges = new TreeMap<Integer, int[]>();
    	Matcher m = intRange.matcher(valuesStr.trim().replace(" ", ""));
    	StringBuffer b = new StringBuffer();
    	int matches = 0;
    	while(m.find()) {
    		int[] range = convertToRange(m.group(1).split(","));
    		m.appendReplacement(b, String.format(token, matches));
    		ranges.put(matches, range);
    		matches++;
    	}
    	StringBuilder sb = new StringBuilder();
    	String target = b.toString();   // WebServer{==0==}|DBServer{==1==}
    	
    	
    	
    	for(int i = 0; i < matches; i++) {
    		String rep = String.format(token, i);
    		int[] range = ranges.get(i);
    		for(int x = 0; x < range.length; x++) {
    			sb.append(target.replace(rep, "" + range[x])).append("|");    			
    		}
    	}
    	if(sb.length()>0) {
    		sb.deleteCharAt(sb.length()-1);
    	}
    	return sb.toString();
    	//return sb.deleteCharAt(sb.length()-1).toString();
    	
    	
	}
	
	public static int[] convertToRange(final String[] intExpr) {
		Set<Integer> values = new TreeSet<Integer>();
		for(String s: intExpr) {
			if(s==null || s.trim().isEmpty()) continue;
			int index = s.indexOf('-'); 
			if(index==-1) {
				values.add(Integer.parseInt(s));
			} else {
				int starter = Integer.parseInt(s.substring(0, index));
				int ender = Integer.parseInt(s.substring(index+1));
				if(ender==starter) {
					values.add(ender);
				} else if(starter < ender) {
					for(int i = starter; i <= ender; i++) {
						values.add(i);
					}
				} else {
					throw new RuntimeException("Invalid Negative Range: [" + starter + "-" + ender + "]");
				}
			}
		}	
		int[] range = new int[values.size()];
		int cnt = 0;
		for(int x : values) {
			range[cnt] = x;
			cnt++;
		}
		return range;
	}
	
	
	public static String print(ObjectName on) {
		StringBuilder b = new StringBuilder();
		for(Map.Entry<String, String> entry: on.getKeyPropertyList().entrySet()) {
			b.append(entry.getKey()).append("=").append(entry.getValue()).append(",");
		}
		return b.deleteCharAt(b.length()-1).toString();
	}
	


	
	
	/**
	 * Joins an array of objects
	 * @param delim The delimeter
	 * @param arr The array of objects to join
	 * @return the joined string
	 */
	public static String join(String delim, Object...arr) {
		return Arrays.toString(arr).replace("[", "").replace("]", "").replace(", ", delim);
	}

	
	
	

}

/*

JSON Serialization:
===================
	- Full
	- Names only
	- Tree (TSMetas only)
	- 
	- 				


NON OVERFLOWING TSMETAS:
========================

	SELECT X.*
	FROM TSD_FQN_TAGPAIR T, TSD_TSMETA X WHERE X.FQNID = T.FQNID
	GROUP BY X.FQNID
	HAVING SUM(CASE WHEN T.XUID IN (
	   SELECT DISTINCT P.XUID FROM TSD_TAGPAIR P, TSD_TAGK K, TSD_TAGV V WHERE P.TAGK = K.XUID AND P.TAGV = V.XUID AND ((K.NAME = 'dc') AND (V.NAME = 'dc1'))
	   UNION ALL
	   SELECT DISTINCT P.XUID FROM TSD_TAGPAIR P, TSD_TAGK K, TSD_TAGV V WHERE P.TAGK = K.XUID AND P.TAGV = V.XUID AND ((K.NAME = 'host') AND (V.NAME = 'WebServer1' OR V.NAME = 'WebServer2'))
	   UNION ALL
	   SELECT DISTINCT P.XUID FROM TSD_TAGPAIR P, TSD_TAGK K, TSD_TAGV V WHERE P.TAGK = K.XUID AND P.TAGV = V.XUID AND ((K.NAME = 'type') AND (V.NAME like '%'))	
	) THEN 1 ELSE 0 END) = 3


*/