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
import org.helios.tsdb.plugins.service.AbstractTSDBPluginService;
import org.helios.tsdb.plugins.service.PluginContext;
import org.helios.tsdb.plugins.util.ConfigurationHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.beans.factory.support.GenericBeanDefinition;
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
	protected GenericXmlApplicationContext appContext = new GenericXmlApplicationContext();
	/** The bootstrap XML config resource */
	protected Resource resource;
	/** The asynch application event multicaster */
	protected SimpleApplicationEventMulticaster eventMulticaster;
	/** The asynch dispatcher's executor */
	protected ThreadPoolExecutor asyncExecutor;
	
	
	/** The config property name for the resource path of the spring bootstrap xml */
	public static final String SPRING_ROOT_XML = "spr.tsd.config";
	/** The default resource path of the spring bootstrap xml */
	public static final String DEFAULT_SPRING_ROOT_XML = "classpath:spring/appCtx.xml";
	/** The bean name of the TSDB extracted config properties bean */
	public static final String TSDB_CONFIG_BEAN_NAME = "tsdbConfig";
	/** The bean name of the TSDB instance bean */
	public static final String TSDB_BEAN_NAME = "tsdb";
	/** The bean name of the plugin service async executor */
	public static final String TSDB_ASYNC_EXEC_BEAN_NAME = "tsdbAsyncExecutor";
	
	private static void reset() {
		instance = null;
	}
	
	/**
	 * Acquires the SpringContainerService singleton instance
	 * @param pc The plugin context
	 * @return the SpringContainerService singleton instance
	 */
	public static SpringContainerService getInstance(PluginContext pc) {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new SpringContainerService(pc);
				}
			}
		}
		return instance;
	}
	
	/**
	 * Returns the singleton instance, throwing an exception if it has not been initialized yet
	 * @return the singleton instance
	 */
	public static SpringContainerService getInstance() {
		if(instance==null) {
			throw new RuntimeException("Service not configured");
		}
		return instance;
	}
	
	/**
	 * Creates a new SpringContainerService
	 * @param pc The plugin context
	 */
	private SpringContainerService(PluginContext pc) {		
		super(pc);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.service.AbstractTSDBPluginService#doInitialize()
	 */
	@Override
	protected void doInitialize() {
		String springConfig = ConfigurationHelper.getSystemThenEnvProperty(SPRING_ROOT_XML, DEFAULT_SPRING_ROOT_XML, config);
		resource = new DefaultResourceLoader().getResource(springConfig);
		asyncExecutor = new AsyncDispatcherExecutor(config);
		eventMulticaster = new SimpleApplicationEventMulticaster(appContext);
		eventMulticaster.setTaskExecutor(asyncExecutor);		
		appContext.load(resource);
		final PropertyPlaceholderConfigurer propPlaceholder = new PropertyPlaceholderConfigurer();
		propPlaceholder.setProperties(config);

		appContext.registerBeanDefinition("tsdbConfigPlaceHolder", beanDefinition(propPlaceholder, true));
		appContext.registerBeanDefinition(TSDB_CONFIG_BEAN_NAME, beanDefinition(config, true));
		appContext.registerBeanDefinition(TSDB_BEAN_NAME, beanDefinition(tsdb, true));
		appContext.registerBeanDefinition(TSDB_ASYNC_EXEC_BEAN_NAME, beanDefinition(asyncExecutor, true));
		appContext.registerBeanDefinition(GenericXmlApplicationContext.APPLICATION_EVENT_MULTICASTER_BEAN_NAME, beanDefinition(eventMulticaster, true));
		appContext.refresh();
		StringBuilder b = new StringBuilder("\nPublished Beans:\n================");
		for(String name: appContext.getBeanDefinitionNames()) {
			b.append("\n\t").append(name).append("   [").append(appContext.getBean(name).getClass().getName()).append("]");
		}
		log.info(b.toString());
	}
	
	
	/**
	 * Creates an on the fly bean definition for the passed object instance. This is so we can inject some of the TSDB plumbing
	 * which already exists, into the spring context as live objects before it is refreshed so that beans defined in the loaded
	 * resources can declare dependencies on this plumbing.
	 * @param beanInstance The object to define a definition for
	 * @param singleton true if the object will be a singleton. Uh.... there is no false.
	 * @return the bean definition
	 */
	public <T> BeanDefinition beanDefinition(final T beanInstance, final boolean singleton) {
		GenericBeanDefinition beanDef = new GenericBeanDefinition();
		beanDef.setBeanClass(InstanceFactoryBean.class);
		ConstructorArgumentValues ctorValues = new ConstructorArgumentValues();
		ctorValues.addGenericArgumentValue(beanInstance);
		ctorValues.addGenericArgumentValue(singleton);
		beanDef.setConstructorArgumentValues(ctorValues);		
		return beanDef;
	}
	
	/**
	 * <p>Title: InstanceFactoryBean</p>
	 * <p>Description: A factory bean for a predefined object</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>net.opentsdb.spring.SpringContainerService.InstanceFactoryBean</code></p>
	 * @param <T> The type of the bean instance
	 */
	public static class InstanceFactoryBean<T> implements FactoryBean<T> {
		/** The bean instance to be returned from the factory */
		final T beanInstance;
		/** Indicates if the bean will be a singleton */
		final boolean singleton;

		/**
		 * Creates a new InstanceFactoryBean
		 * @param beanInstance The bean instance to be returned from the factory
		 * @param singleton Indicates if the bean will be a singleton
		 */
		public InstanceFactoryBean(T beanInstance, boolean singleton) {			
			this.beanInstance = beanInstance;
			this.singleton = singleton;
		}

		@Override
		public T getObject() throws Exception {
			return beanInstance;
		}

		@Override
		public Class<T> getObjectType() {
			return (Class<T>)beanInstance.getClass();
		}
		@Override
		public boolean isSingleton() {
			return singleton;
		}
		
	}
	
	
	/**
	 * Stops the event dispatcher and all subsidiary services
	 */
	public void doPreShutdown() {

		if(appContext!=null) {
			appContext.stop();
			appContext.close();
			appContext = null;
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
		if(appContext != null) appContext.publishEvent(ApplicationTSDBPublishEvent.publishDataPoint(metric, timestamp, value, tags, tsuid));
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
		if(appContext != null) appContext.publishEvent(ApplicationTSDBPublishEvent.publishDataPoint(metric, timestamp, value, tags, tsuid));
	}
	

	
	/**
	 * Deletes an annotation
	 * @param annotation The annotation to delete
	 * @see net.opentsdb.search.SearchPlugin#deleteAnnotation(net.opentsdb.meta.Annotation)
	 */
	public void deleteAnnotation(Annotation annotation) {
		if(annotation!=null && appContext != null) {
			appContext.publishEvent(ApplicationTSDBSearchEvent.deleteAnnotation(annotation));
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.service.AbstractTSDBPluginService#initialize()
	 */
	@Override
	public void initialize() {	
		super.initialize();
	}

	/**
	 * Indexes an annotation
	 * @param annotation The annotation to index
	 * @see net.opentsdb.search.SearchPlugin#indexAnnotation(net.opentsdb.meta.Annotation)
	 */
	public void indexAnnotation(Annotation annotation) {
		if(annotation!=null && appContext != null) {
			appContext.publishEvent(ApplicationTSDBSearchEvent.indexAnnotation(annotation));
		}
	}	

	/**
	 * Called when we need to remove a timeseries meta object from the engine Note: Unique Document ID = TSUID
	 * @param tsMeta The TSMeta name to delete
	 * @see net.opentsdb.search.SearchPlugin#deleteTSMeta(java.lang.String)
	 */
	public void deleteTSMeta(String tsMeta) {
		if(tsMeta!=null && appContext != null) {
			appContext.publishEvent(ApplicationTSDBSearchEvent.deleteTSMeta(tsMeta));
		}
	}
	
	/**
	 * Indexes a timeseries metadata object in the search engine Note: Unique Document ID = TSUID
	 * @param tsMeta The TSMeta to index
	 * @see net.opentsdb.search.SearchPlugin#indexTSMeta(net.opentsdb.meta.TSMeta)
	 */
	public void indexTSMeta(TSMeta tsMeta) {
		if(tsMeta!=null && appContext != null) {
			appContext.publishEvent(ApplicationTSDBSearchEvent.indexTSMeta(tsMeta));
		}
	}	
	
	/**
	 * Indexes a UID metadata object for a metric, tagk or tagv Note: Unique Document ID = UID and the Type "TYPEUID"
	 * @param uidMeta The UIDMeta to index
	 * @see net.opentsdb.search.SearchPlugin#indexUIDMeta(net.opentsdb.meta.UIDMeta)
	 */
	public void indexUIDMeta(UIDMeta uidMeta) {
		if(uidMeta!=null && appContext != null) {
			appContext.publishEvent(ApplicationTSDBSearchEvent.indexUIDMeta(uidMeta));
		}
	}	

	/**
	 * Called when we need to remove a UID meta object from the engine Note: Unique Document ID = UID and the Type "TYPEUID"
	 * @param uidMeta The UIDMeta to delete
	 * @see net.opentsdb.search.SearchPlugin#deleteUIDMeta(net.opentsdb.meta.UIDMeta)
	 */
	public void deleteUIDMeta(UIDMeta uidMeta) {
		if(uidMeta!=null && appContext != null) {
			appContext.publishEvent(ApplicationTSDBSearchEvent.deleteUIDMeta(uidMeta));
		}
	}

	/**
	 * 
	 * @see net.opentsdb.search.SearchPlugin#executeQuery(net.opentsdb.search.SearchQuery)
	 */
	public void executeQuery(SearchQuery searchQuery, Deferred<SearchQuery> toComplete) {
		if(appContext != null) appContext.publishEvent(ApplicationTSDBSearchEvent.executeQueryEvent(searchQuery, toComplete));
	}
	
	
	/**
	 * CL Test
	 * @param args None
	 */
	public static void main(String[] args) {

	}

	/**
	 * Returns 
	 * @return the appContext
	 */
	public GenericXmlApplicationContext getAppContext() {
		return appContext;
	}


}
