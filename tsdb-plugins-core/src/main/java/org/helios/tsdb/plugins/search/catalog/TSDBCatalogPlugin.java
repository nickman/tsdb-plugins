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
package org.helios.tsdb.plugins.search.catalog;

import net.opentsdb.core.TSDB;
import net.opentsdb.meta.Annotation;
import net.opentsdb.meta.TSMeta;
import net.opentsdb.meta.UIDMeta;
import net.opentsdb.search.SearchPlugin;
import net.opentsdb.search.SearchQuery;
import net.opentsdb.stats.StatsCollector;

import org.helios.tsdb.plugins.util.helpers.VersionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stumbleupon.async.Deferred;

import net.opentsdb.utils.JSON;

/**
 * <p>Title: TSDBCatalogPlugin</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.search.catalog.TSDBCatalogPlugin</code></p>
 */

public class TSDBCatalogPlugin extends SearchPlugin {
	/** Instance logger */
	Logger log = LoggerFactory.getLogger(getClass());
	
	
	/**
	 * Creates a new TSDBCatalogPlugin
	 */
	public TSDBCatalogPlugin() {
		log.error("\n\t\tCreated {} Instance", getClass().getName());
	}

	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.search.SearchPlugin#collectStats(net.opentsdb.stats.StatsCollector)
	 */
	@Override
	public void collectStats(StatsCollector collector) {
		log.debug("Collecting Stats....");
	}

	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.search.SearchPlugin#deleteTSMeta(java.lang.String)
	 */
	@Override
	public Deferred<Object> deleteTSMeta(String tsuid) {
		log.error("\n\t\tDelete TSUid {}", tsuid);
		return Deferred.fromResult(null);
	}

	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.search.SearchPlugin#indexAnnotation(net.opentsdb.meta.Annotation)
	 */
	@Override
	public Deferred<Object> indexAnnotation(Annotation a) {
		log.error("\n\t\tIndexing Annotation\n\t{}", JSON.serializeToString(a));
		return Deferred.fromResult(null);
	}

	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.search.SearchPlugin#indexUIDMeta(net.opentsdb.meta.UIDMeta)
	 */
	@Override
	public Deferred<Object> indexUIDMeta(UIDMeta uidMeta) {
		log.error("\n\t\tIndexing UIDMeta\n\t{}", JSON.serializeToString(uidMeta));
		return Deferred.fromResult(null);
	}

	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.search.SearchPlugin#initialize(net.opentsdb.core.TSDB)
	 */
	@Override
	public void initialize(TSDB tsdb) {
		log.error("\n\t===========================================\n\tInitializing {}\n\t===========================================\n", getClass().getSimpleName());
	}

	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.search.SearchPlugin#shutdown()
	 */
	@Override
	public Deferred<Object> shutdown() {
		log.error("\n\t===========================================\n\tShutdown {}\n\t===========================================\n", getClass().getSimpleName());
		return Deferred.fromResult(null);
	}

	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.search.SearchPlugin#version()
	 */
	@Override
	public String version() {
		return VersionHelper.getVersion(getClass());
	}

	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.search.SearchPlugin#deleteAnnotation(net.opentsdb.meta.Annotation)
	 */
	@Override
	public Deferred<Object> deleteAnnotation(Annotation a) {
		log.error("\n\t\tDeleting Annotation\n\t{}", JSON.serializeToString(a));
		return Deferred.fromResult(null);
	}

	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.search.SearchPlugin#deleteUIDMeta(net.opentsdb.meta.UIDMeta)
	 */
	@Override
	public Deferred<Object> deleteUIDMeta(UIDMeta uidMeta) {
		log.error("\n\t\tDeleting UIDMeta\n\t{}", JSON.serializeToString(uidMeta));
		return Deferred.fromResult(null);
	}

	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.search.SearchPlugin#executeQuery(net.opentsdb.search.SearchQuery)
	 */
	@Override
	public Deferred<SearchQuery> executeQuery(SearchQuery searchQuery) {
		log.error("\n\t\tExecuting Query\n\t{}", JSON.serializeToString(searchQuery));
		return Deferred.fromResult(null);
	}

	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.search.SearchPlugin#indexTSMeta(net.opentsdb.meta.TSMeta)
	 */
	@Override
	public Deferred<Object> indexTSMeta(TSMeta tsMeta) {
		log.error("\n\t\tIndexing TSMeta\n\t{}", JSON.serializeToString(tsMeta));
		return Deferred.fromResult(null);
	}

}
