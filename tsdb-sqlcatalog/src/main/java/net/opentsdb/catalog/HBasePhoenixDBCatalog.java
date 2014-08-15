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
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.LoaderClassPath;

import javax.sql.DataSource;

import net.opentsdb.catalog.sequence.HBasePhoenixLocalSequenceCache;
import net.opentsdb.catalog.sequence.ISequenceCache;
import net.opentsdb.meta.Annotation;
import net.opentsdb.meta.TSMeta;
import net.opentsdb.meta.UIDMeta;
import net.opentsdb.search.SearchQuery;

import org.helios.tsdb.plugins.service.PluginContextImpl;

/**
 * <p>Title: HBasePhoenixDBCatalog</p>
 * <p>Description: DB catalog for storing catalog in HBase using the phoenix JDBC driver</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>net.opentsdb.catalog.HBasePhoenixDBCatalog</code></p>
 */

public class HBasePhoenixDBCatalog extends AbstractDBCatalog {


	/**
	 * Creates a new HBasePhoenixDBCatalog
	 */
	public HBasePhoenixDBCatalog() {
		INSERT_TAGPAIR_SQL = "UPSERT INTO TSD_TAGPAIR (XUID, TAGK, TAGV, NAME) VALUES (?,?,?,?)";
		TSUID_INSERT_SQL = "UPSERT INTO TSD_TSMETA " +
				"(FQNID, VERSION, METRIC_UID, FQN, TSUID, CREATED, LAST_UPDATE, MAX_VALUE, MIN_VALUE, " + 
				"DATA_TYPE, DESCRIPTION, DISPLAY_NAME, NOTES, UNITS, RETENTION, CUSTOM) " + 
				"VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
		TSUID_UPDATE_SQL = TSUID_INSERT_SQL;	
		TSD_FQN_TAGPAIR_SQL = "UPSERT INTO TSD_FQN_TAGPAIR (FQN_TP_ID, FQNID, XUID, PORDER, NODE) VALUES (?,?,?,?,?)";
		TSD_INSERT_ANNOTATION = "UPSERT INTO TSD_ANNOTATION (ANNID,VERSION,START_TIME,LAST_UPDATE,DESCRIPTION,NOTES,FQNID,END_TIME,CUSTOM) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
		TSD_UPDATE_ANNOTATION = TSD_INSERT_ANNOTATION;
		UID_INDEX_SQL_TEMPLATE = "UPSERT INTO %s (XUID,VERSION, NAME,CREATED,LAST_UPDATE,DESCRIPTION,DISPLAY_NAME,NOTES,CUSTOM) VALUES(?,?,?,?,?,?,?,?,?)";
		
	}
	
	protected void addLastSyncEntry(Connection conn, String tableName, int tableOrdinal, long time) {
		sqlWorker.execute(conn, "UPSERT INTO TSD_LASTSYNC (TABLE_NAME, ORDERING, LAST_SYNC) VALUES (?,?,?)", tableName, tableOrdinal, time);
	}	
	
	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.CatalogDBInterface#getUIDMetaTagKIndexSQL()
	 */
	@Override
	public String getUIDMetaTagKIndexSQL() {
		return String.format(UID_INDEX_SQL_TEMPLATE, "TSD_TAGK");
	}	
	
	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.CatalogDBInterface#getUIDMetaTagVIndexSQL()
	 */
	@Override
	public String getUIDMetaTagVIndexSQL() {
		return String.format(UID_INDEX_SQL_TEMPLATE, "TSD_TAGV");
	}

	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.CatalogDBInterface#getUIDMetaMetricIndexSQL()
	 */
	@Override
	public String getUIDMetaMetricIndexSQL() {
		return String.format(UID_INDEX_SQL_TEMPLATE, "TSD_METRIC");
	}

	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.CatalogDBInterface#getUIDMetaMetricUpdateSQL()
	 */
	@Override
	public String getUIDMetaMetricUpdateSQL() { 
		return String.format(UID_UPDATE_SQL_TEMPLATE, "TSD_METRIC");
	}
	
	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.CatalogDBInterface#getUIDMetaTagVUpdateSQL()
	 */
	@Override
	public String getUIDMetaTagVUpdateSQL() {
		return String.format(UID_UPDATE_SQL_TEMPLATE, "TSD_TAGV");
	}
	
	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.CatalogDBInterface#getUIDMetaTagKUpdateSQL()
	 */
	@Override
	public String getUIDMetaTagKUpdateSQL() {
		return String.format(UID_UPDATE_SQL_TEMPLATE, "TSD_TAGK");
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.AbstractDBCatalog#createLocalSequenceCache(int, java.lang.String, javax.sql.DataSource)
	 */
	public ISequenceCache createLocalSequenceCache(int increment, String sequenceName, DataSource dataSource) {
		return new HBasePhoenixLocalSequenceCache(increment, sequenceName, dataSource);
	}
	
	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.CatalogDBInterface#isNaNToNull()
	 */
	@Override
	public boolean isNaNToNull() {
		return true;
	}	

	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.CatalogDBInterface#setConnectionProperty(java.sql.Connection, java.lang.String, java.lang.String)
	 */
	@Override
	public void setConnectionProperty(Connection conn, String key, String value) {

	}

	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.CatalogDBInterface#getConnectionProperty(java.sql.Connection, java.lang.String, java.lang.String)
	 */
	@Override
	public String getConnectionProperty(Connection conn, String key, String defaultValue) {
		return null;
	}

	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.CatalogDBInterface#executeSearch(java.sql.Connection, net.opentsdb.search.SearchQuery)
	 */
	@Override
	public List<?> executeSearch(Connection conn, SearchQuery query) {
		return Collections.emptyList();
	}

	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.CatalogDBInterface#recordSyncQueueFailure(net.opentsdb.meta.api.UIDMeta, net.opentsdb.catalog.TSDBTable)
	 */
	@Override
	public void recordSyncQueueFailure(UIDMeta uidMeta, TSDBTable tsdbTable) {
		// TODO Auto-generated method stub

	}

	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.CatalogDBInterface#recordSyncQueueFailure(net.opentsdb.meta.api.TSMeta)
	 */
	@Override
	public void recordSyncQueueFailure(TSMeta tsMeta) {
		// TODO Auto-generated method stub

	}

	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.CatalogDBInterface#recordSyncQueueFailure(net.opentsdb.meta.api.Annotation)
	 */
	@Override
	public void recordSyncQueueFailure(Annotation ann) {
		// TODO Auto-generated method stub

	}
	
	public static final String hackClassParent = "com.google.protobuf.ByteString";
	public static final String hackClass = "com.google.protobuf.LiteralByteString";
	public static final Set<String> HACKED_CLASSES = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(hackClassParent, hackClass)));
	public static final String hackClassPath = "/home/nwhitehead/services/hbase/hbase-0.96.2-hadoop1/lib/hbase-protocol-0.96.2-hadoop1.jar";
	
	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.AbstractDBCatalog#doInitialize()
	 */
	@Override
	protected void preWorker() {
		ClassLoader hackedClassLoader = new HackDelegatingCL(HACKED_CLASSES, hackClassPath, pluginContext.getSupportClassLoader());
		((PluginContextImpl)pluginContext).setSupportClassLoader(hackedClassLoader);
//		final ClassLoader cl = Thread.currentThread().getContextClassLoader();
//		URLClassLoader supportClassLoader = (URLClassLoader)pluginContext.getSupportClassLoader();
//		final String hackClass = "com.google.protobuf.LiteralByteString";
//		final String hackClassParent = "com.google.protobuf.ByteString";
//		ClassPool cp = new ClassPool();
//		try {
//			for(URL url: supportClassLoader.getURLs()) {				
//				cp.appendClassPath(new LoaderClassPath(new URLClassLoader(supportClassLoader.getURLs(), new NonDelegatingCL())));
//			}
//			CtClass clz = cp.get(hackClass);
//			CtClass parentClz = cp.get(hackClassParent);
//			clz.setModifiers(clz.getModifiers()|Modifier.PUBLIC);
//			log.info("Public: [{}]", Modifier.isPublic(clz.getModifiers()));
//			byte[] byteCode = clz.toBytecode();
//			URL clazzUrl = clz.getURL();
//			CodeSource codeSource = new CodeSource(clazzUrl, ( Certificate[])null);
//			ProtectionDomain protectionDomain = new ProtectionDomain(codeSource, null, supportClassLoader, null);
//			UnsafeAdapter.defineClass(hackClass, byteCode, 0, byteCode.length, supportClassLoader, protectionDomain);
//			CtConstructor ctor = parentClz.getDeclaredConstructor(new CtClass[0]);
//			ctor.setModifiers(clz.getModifiers()|Modifier.PUBLIC);
//			byte[] parentByteCode = parentClz.toBytecode();
//			UnsafeAdapter.defineClass(hackClassParent, parentByteCode, 0, parentByteCode.length, supportClassLoader, protectionDomain);
//			UnsafeAdapter.defineClass(hackClass, byteCode, 0, byteCode.length, supportClassLoader, protectionDomain);
//			
//		} catch (Throwable ex) {
//			log.error("Failed to install HBaseZeroCopyByteString workaround hack", ex);
//		} finally {
//			Thread.currentThread().setContextClassLoader(cl);
//		}
	}
	
	private Class<?> loadHackedClass(String name, String hackedClassPath) {
		try {
			ClassPool cp = new ClassPool();
			cp.appendClassPath(new LoaderClassPath(new URLClassLoader(new URL[] {new File(hackedClassPath).toURI().toURL()}, new NonDelegatingCL())));
			CtClass clz = cp.get(name);
			return clz.toClass();
		} catch (Throwable ex) {
			throw new RuntimeException(ex);
		}
	}

	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.catalog.AbstractDBCatalog#doShutdown()
	 */
	@Override
	protected void doShutdown() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void doInitialize() {
		// TODO Auto-generated method stub
		
	}
	
	private class HackDelegatingCL extends ClassLoader {
		final Set<String> hackedClasses;
		final String hackedClassPath;
		final ClassLoader hackedClassLoader;
		final ClassLoader originalClassLoader;
		
		
		
		/**
		 * Creates a new HackDelegatingCL
		 * @param hackedClasses A set of the hacked class names of classes that should be loaded from the hacked class path
		 * @param hackedClassPath the hacked class path
		 * @param originalClassLoader The original class path classloader
		 */
		public HackDelegatingCL(Set<String> hackedClasses, String hackedClassPath, ClassLoader originalClassLoader) {
			super();
			this.hackedClasses = hackedClasses;
			this.hackedClassPath = hackedClassPath;
			this.originalClassLoader = originalClassLoader;
			try {
				hackedClassLoader = new URLClassLoader(new URL[] {new File(hackedClassPath).toURI().toURL()}, new NonDelegatingCL());
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		}
		@Override
		protected Class<?> findClass(String name) throws ClassNotFoundException {			
			if(hackedClasses.contains(name)) {
				return loadHackedClass(name, hackedClassPath);
			} else {
				return originalClassLoader.loadClass(name);
			}
		}
		@Override
		public Class<?> loadClass(String name) throws ClassNotFoundException {
			if(hackedClasses.contains(name)) {
				return loadHackedClass(name, hackedClassPath);
			} else {
				return originalClassLoader.loadClass(name);
			}
		}
	}
	
	private class NonDelegatingCL extends ClassLoader {
		@Override
		protected Class<?> findClass(String name) throws ClassNotFoundException {
			throw new ClassNotFoundException(name);
		}
		@Override
		public Class<?> loadClass(String name) throws ClassNotFoundException {
			throw new ClassNotFoundException(name);
		}
	}
	

}
