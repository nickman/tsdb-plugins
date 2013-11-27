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

import net.opentsdb.core.TSDB;
import net.opentsdb.meta.Annotation;
import net.opentsdb.meta.TSMeta;
import net.opentsdb.meta.UIDMeta;
import net.opentsdb.search.SearchPlugin;
import net.opentsdb.search.SearchQuery;
import net.opentsdb.stats.StatsCollector;

import org.helios.tsdb.plugins.Constants;
import org.helios.tsdb.plugins.event.PluginType;
import org.helios.tsdb.plugins.service.ITSDBPluginService;
import org.helios.tsdb.plugins.service.TSDBPluginServiceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stumbleupon.async.Deferred;

/**
 * <p>Title: Search</p>
 * <p>Description: A passthrough {@link SearchPlugin} that delegates to the @link TSDBEventDispatcher} asynch multiplexer.</p> 
 * <p>Activated with the tsdb config property <b><code>tsd.search.plugins</code></b> and <b><code>tsd.search.enable</code></b> must be true</p>
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.shell.Search</code></p>
 */

public class Search extends SearchPlugin implements Plugin {
	/** Instance logger */
	protected final Logger log = LoggerFactory.getLogger(getClass());
	/** The callback supplied TSDB instance */
	protected TSDB tsdb = null;
	/** The event pluginService delegate */
	protected ITSDBPluginService pluginService;
	

	/**
	 * Creates a new Search
	 */
	public Search() {
		log.debug("Created {} instance", getClass().getName());
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.shell.Plugin#getPluginType()
	 */
	public PluginType getPluginType() {
		return PluginType.SEARCH;
	}


	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.search.SearchPlugin#collectStats(net.opentsdb.stats.StatsCollector)
	 */
	@Override
	public void collectStats(StatsCollector statsCollector) {
		if(pluginService!=null) pluginService.collectStats(PluginType.PUBLISH, statsCollector);

	}

	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.search.SearchPlugin#deleteAnnotation(net.opentsdb.meta.Annotation)
	 */
	@Override
	public Deferred<Object> deleteAnnotation(Annotation annotation) {
		if(pluginService!=null) pluginService.deleteAnnotation(annotation);
		return Constants.NULL_DEFERED;
	}

	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.search.SearchPlugin#deleteTSMeta(java.lang.String)
	 */
	@Override
	public Deferred<Object> deleteTSMeta(String tsMeta) {
		if(pluginService!=null) pluginService.deleteTSMeta(tsMeta);
		return Constants.NULL_DEFERED;
	}

	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.search.SearchPlugin#deleteUIDMeta(net.opentsdb.meta.UIDMeta)
	 */
	@Override
	public Deferred<Object> deleteUIDMeta(UIDMeta uidMeta) {
		if(pluginService!=null) pluginService.deleteUIDMeta(uidMeta);
		return Constants.NULL_DEFERED;
	}

	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.search.SearchPlugin#executeQuery(net.opentsdb.search.SearchQuery)
	 */
	@Override
	public Deferred<SearchQuery> executeQuery(SearchQuery searchQuery) {
		Deferred<SearchQuery> toComplete = new Deferred<SearchQuery>(); 
		if(pluginService!=null) {
			pluginService.executeQuery(searchQuery, toComplete);
			return toComplete;
		}
		return Deferred.fromResult(searchQuery);
	}

	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.search.SearchPlugin#indexAnnotation(net.opentsdb.meta.Annotation)
	 */
	@Override
	public Deferred<Object> indexAnnotation(Annotation annotation) {
		if(pluginService!=null) pluginService.indexAnnotation(annotation);
		return Constants.NULL_DEFERED;
	}

	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.search.SearchPlugin#indexTSMeta(net.opentsdb.meta.TSMeta)
	 */
	@Override
	public Deferred<Object> indexTSMeta(TSMeta tsMeta) {				
		if(pluginService!=null) pluginService.indexTSMeta(tsMeta);
		return Constants.NULL_DEFERED;
	}

	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.search.SearchPlugin#indexUIDMeta(net.opentsdb.meta.UIDMeta)
	 */
	@Override
	public Deferred<Object> indexUIDMeta(UIDMeta uidMeta) {		
		if(pluginService!=null) pluginService.indexUIDMeta(uidMeta);
		return Constants.NULL_DEFERED;
	}

	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.search.SearchPlugin#initialize(net.opentsdb.core.TSDB)
	 */
	@Override
	public void initialize(TSDB tsdb) {
		log.debug("Initializing instance");
		pluginService = TSDBPluginServiceLoader.getInstance(tsdb, this);
	}

	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.search.SearchPlugin#shutdown()
	 */
	@Override
	public Deferred<Object> shutdown() {
		if(pluginService==null) return Deferred.fromResult(null);
		try {
			return pluginService.shutdown(new Deferred<Object>());
		} finally {
			pluginService = null;
		}		
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
