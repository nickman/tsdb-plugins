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
package org.helios.tsdb.plugins.shell;

import org.helios.tsdb.plugins.Constants;
import org.helios.tsdb.plugins.event.PluginType;
import org.helios.tsdb.plugins.event.TSDBEventDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stumbleupon.async.Deferred;

import net.opentsdb.core.TSDB;
import net.opentsdb.meta.Annotation;
import net.opentsdb.meta.TSMeta;
import net.opentsdb.meta.UIDMeta;
import net.opentsdb.search.SearchPlugin;
import net.opentsdb.search.SearchQuery;
import net.opentsdb.stats.StatsCollector;

/**
 * <p>Title: Search</p>
 * <p>Description: A passthrough {@link SearchPlugin} that delegates to the @link TSDBEventDispatcher} asynch multiplexer.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.shell.Search</code></p>
 */

public class Search extends SearchPlugin {
	/** Instance logger */
	protected final Logger log = LoggerFactory.getLogger(getClass());
	/** The callback supplied TSDB instance */
	protected TSDB tsdb = null;
	/** The event publisher delegate */
	protected TSDBEventDispatcher publisher;
	

	/**
	 * Creates a new Search
	 */
	public Search() {
		log.debug("Created {} instance", getClass().getName());
	}

	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.search.SearchPlugin#collectStats(net.opentsdb.stats.StatsCollector)
	 */
	@Override
	public void collectStats(StatsCollector statsCollector) {
		publisher.collectStats(PluginType.PUBLISH, statsCollector);

	}

	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.search.SearchPlugin#deleteAnnotation(net.opentsdb.meta.Annotation)
	 */
	@Override
	public Deferred<Object> deleteAnnotation(Annotation annotation) {
		publisher.deleteAnnotation(annotation);
		return Constants.NULL_DEFERED;
	}

	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.search.SearchPlugin#deleteTSMeta(java.lang.String)
	 */
	@Override
	public Deferred<Object> deleteTSMeta(String tsMeta) {
		publisher.deleteTSMeta(tsMeta);
		return Constants.NULL_DEFERED;
	}

	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.search.SearchPlugin#deleteUIDMeta(net.opentsdb.meta.UIDMeta)
	 */
	@Override
	public Deferred<Object> deleteUIDMeta(UIDMeta uidMeta) {
		publisher.deleteUIDMeta(uidMeta);
		return Constants.NULL_DEFERED;
	}

	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.search.SearchPlugin#executeQuery(net.opentsdb.search.SearchQuery)
	 */
	@Override
	public Deferred<SearchQuery> executeQuery(SearchQuery searchQuery) {
		return publisher.executeQuery(searchQuery);
	}

	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.search.SearchPlugin#indexAnnotation(net.opentsdb.meta.Annotation)
	 */
	@Override
	public Deferred<Object> indexAnnotation(Annotation annotation) {
		publisher.indexAnnotation(annotation);
		return Constants.NULL_DEFERED;
	}

	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.search.SearchPlugin#indexTSMeta(net.opentsdb.meta.TSMeta)
	 */
	@Override
	public Deferred<Object> indexTSMeta(TSMeta tsMeta) {
		publisher.indexTSMeta(tsMeta);
		return Constants.NULL_DEFERED;
	}

	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.search.SearchPlugin#indexUIDMeta(net.opentsdb.meta.UIDMeta)
	 */
	@Override
	public Deferred<Object> indexUIDMeta(UIDMeta uidMeta) {
		publisher.indexUIDMeta(uidMeta);
		return Constants.NULL_DEFERED;
	}

	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.search.SearchPlugin#initialize(net.opentsdb.core.TSDB)
	 */
	@Override
	public void initialize(TSDB tsdb) {
		log.debug("Initializing instance");
		publisher = TSDBEventDispatcher.getInstance(tsdb);
	}

	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.search.SearchPlugin#shutdown()
	 */
	@Override
	public Deferred<Object> shutdown() {
		if(publisher!=null) publisher.shutdown();
		return Constants.NULL_DEFERED;
	}

	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.search.SearchPlugin#version()
	 */
	@Override
	public String version() {
		return Constants.PLUGIN_VERSION;
	}

}
