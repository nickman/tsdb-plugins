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
package net.opentsdb.spring;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ThreadPoolExecutor;

import net.opentsdb.core.TSDB;
import net.opentsdb.meta.Annotation;
import net.opentsdb.meta.TSMeta;
import net.opentsdb.meta.UIDMeta;
import net.opentsdb.search.SearchQuery;

import org.helios.tsdb.plugins.async.AsyncDispatcherExecutor;
import org.helios.tsdb.plugins.async.AsyncEventDispatcher;
import org.helios.tsdb.plugins.service.AbstractTSDBPluginService;
import org.helios.tsdb.plugins.util.ConfigurationHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.context.support.GenericXmlApplicationContext;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

import com.stumbleupon.async.Deferred;

/**
 * <p>Title: SpringContainerService</p>
 * <p>Description: A bootstrap for a spring container triggered for loading as an OpenTSDB plugin.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>net.opentsdb.spring.SpringContainerService</code></p>
 */

public class SpringContainerService extends AbstractTSDBPluginService {
	/** Singleton instance */
	private static volatile SpringContainerService instance = null;
	/** Singleton instance ctor lock */
	private static final Object lock = new Object();

	/** Instance logger */
	protected final Logger log = LoggerFactory.getLogger(getClass());
	
	/** The service root context */
	protected final GenericXmlApplicationContext appContext = new GenericXmlApplicationContext();
	/** The bootstrap XML config resource */
	protected final Resource resource;
	/** The asynch application event multicaster */
	protected final SimpleApplicationEventMulticaster eventMulticaster;
	/** The asynch dispatcher's executor */
	protected final ThreadPoolExecutor asyncExecutor;
	
	
	/** The config property name for the resource path of the spring bootstrap xml */
	public static final String SPRING_ROOT_XML = "spr.tsd.config";
	/** The default resource path of the spring bootstrap xml */
	public static final String DEFAULT_SPRING_ROOT_XML = "classpath:spring/appCtx.xml";
	
	// CONFIG_PLUGIN_SUPPORT_PATH
	
	/**
	 * Acquires the SpringContainerService singleton instance
	 * @param tsdb The parent TSDB instance
	 * @param config The extracted configuration properties
	 * @return the SpringContainerService singleton instance
	 */
	public static SpringContainerService getInstance(TSDB tsdb, Properties config) {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new SpringContainerService(tsdb, config);
				}
			}
		}
		return instance;
	}
	
	/**
	 * Creates a new SpringContainerService
	 * @param tsdb the core TSDB instance
	 * @param config The extracted config properties
	 */
	private SpringContainerService(TSDB tsdb, Properties config) {
		super(tsdb, config);
		String springConfig = ConfigurationHelper.getSystemThenEnvProperty(SPRING_ROOT_XML, DEFAULT_SPRING_ROOT_XML, config);
		resource = new DefaultResourceLoader().getResource(springConfig);
		asyncExecutor = new AsyncDispatcherExecutor(config);
		eventMulticaster = new SimpleApplicationEventMulticaster(appContext);
		eventMulticaster.setTaskExecutor(asyncExecutor);

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.service.AbstractTSDBPluginService#doInitialize()
	 */
	@Override
	protected void doInitialize() {
		appContext.load(resource);
		PropertyPlaceholderConfigurer propPlaceholder = new PropertyPlaceholderConfigurer();
		propPlaceholder.setProperties(config);
		appContext.getBeanFactory().registerSingleton("tsdbConfig", propPlaceholder);
		appContext.getBeanFactory().registerSingleton("tsdb", tsdb);
		appContext.getBeanFactory().registerSingleton("asyncExecutor", asyncExecutor);
		appContext.getBeanFactory().registerSingleton("eventMulticaster", eventMulticaster);
		appContext.refresh();
	}
	
	/**
	 * Stops the event dispatcher and all subsidiary services
	 */
	public void doPreShutdown() {
		if(asyncDispatcher!=null) {
			asyncDispatcher.shutdown();
			log.info("Shutdown AsyncDispatcher.");
		}
		if(asyncExecutor!=null) {
			int remainingTasks = asyncExecutor.shutdownNow().size();
			log.info("Shutdown AsyncExecutor. Remaining Tasks:{}", remainingTasks);
		}				
	}

	/**
	 * Called any time a new data point is published
	 * @param metric The name of the metric associated with the data point
	 * @param timestamp Timestamp as a Unix epoch in seconds or milliseconds (depending on the TSD's configuration)
	 * @param value Value for the data point
	 * @param tags The metric tags
	 * @param tsuid Time series UID for the value
	 */
	public void publishDataPoint(String metric, long timestamp, double value, Map<String, String> tags, byte[] tsuid) {
		
		asyncDispatcher.publishDataPoint(metric, timestamp, value, tags, tsuid);
	}

	/**
	 * Called any time a new data point is published
	 * @param metric The name of the metric associated with the data point
	 * @param timestamp Timestamp as a Unix epoch in seconds or milliseconds (depending on the TSD's configuration)
	 * @param value Value for the data point
	 * @param tags The metric tags
	 * @param tsuid Time series UID for the value
	 */
	public void publishDataPoint(String metric, long timestamp, long value, Map<String, String> tags, byte[] tsuid) {
		asyncDispatcher.publishDataPoint(metric, timestamp, value, tags, tsuid);
	}
	

	
	/**
	 * Deletes an annotation
	 * @param annotation The annotation to delete
	 * @see net.opentsdb.search.SearchPlugin#deleteAnnotation(net.opentsdb.meta.Annotation)
	 */
	public void deleteAnnotation(Annotation annotation) {
		if(annotation!=null) {
			asyncDispatcher.deleteAnnotation(annotation);
		}
	}
	

	/**
	 * Indexes an annotation
	 * @param annotation The annotation to index
	 * @see net.opentsdb.search.SearchPlugin#indexAnnotation(net.opentsdb.meta.Annotation)
	 */
	public void indexAnnotation(Annotation annotation) {
		if(annotation!=null) {
			asyncDispatcher.indexAnnotation(annotation);
		}
	}	

	/**
	 * Called when we need to remove a timeseries meta object from the engine Note: Unique Document ID = TSUID
	 * @param tsMeta The TSMeta name to delete
	 * @see net.opentsdb.search.SearchPlugin#deleteTSMeta(java.lang.String)
	 */
	public void deleteTSMeta(String tsMeta) {
		if(tsMeta!=null) {
			asyncDispatcher.deleteTSMeta(tsMeta);
		}
	}
	
	/**
	 * Indexes a timeseries metadata object in the search engine Note: Unique Document ID = TSUID
	 * @param tsMeta The TSMeta to index
	 * @see net.opentsdb.search.SearchPlugin#indexTSMeta(net.opentsdb.meta.TSMeta)
	 */
	public void indexTSMeta(TSMeta tsMeta) {
		if(tsMeta!=null) {
			asyncDispatcher.indexTSMeta(tsMeta);
		}
	}	
	
	/**
	 * Indexes a UID metadata object for a metric, tagk or tagv Note: Unique Document ID = UID and the Type "TYPEUID"
	 * @param uidMeta The UIDMeta to index
	 * @see net.opentsdb.search.SearchPlugin#indexUIDMeta(net.opentsdb.meta.UIDMeta)
	 */
	public void indexUIDMeta(UIDMeta uidMeta) {
		if(uidMeta!=null) {
			asyncDispatcher.indexUIDMeta(uidMeta);
		}
	}	

	/**
	 * Called when we need to remove a UID meta object from the engine Note: Unique Document ID = UID and the Type "TYPEUID"
	 * @param uidMeta The UIDMeta to delete
	 * @see net.opentsdb.search.SearchPlugin#deleteUIDMeta(net.opentsdb.meta.UIDMeta)
	 */
	public void deleteUIDMeta(UIDMeta uidMeta) {
		if(uidMeta!=null) {
			asyncDispatcher.deleteUIDMeta(uidMeta);
		}
	}

	/**
	 * 
	 * @see net.opentsdb.search.SearchPlugin#executeQuery(net.opentsdb.search.SearchQuery)
	 */
	public Deferred<SearchQuery> executeQuery(SearchQuery searchQuery) {
		Deferred<SearchQuery> defSearch = new Deferred<SearchQuery>();
		defSearch.callback(searchQuery);
		return defSearch;
	}
	
	
	/**
	 * CL Test
	 * @param args None
	 */
	public static void main(String[] args) {

	}


}
