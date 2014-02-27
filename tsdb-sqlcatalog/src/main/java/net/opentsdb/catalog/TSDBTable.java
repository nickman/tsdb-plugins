/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2014, Helios Development Group and individual contributors
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

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.List;

import net.opentsdb.catalog.h2.H2Support;
import net.opentsdb.core.TSDB;
import net.opentsdb.meta.Annotation;
import net.opentsdb.meta.TSMeta;
import net.opentsdb.meta.UIDMeta;
import net.opentsdb.uid.UniqueId.UniqueIdType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stumbleupon.async.Callback;
import com.stumbleupon.async.Deferred;

/**
 * <p>Title: TSDBTable</p>
 * <p>Description: A functional enumeration of the major tables for the OpenTSDB SQL Catalog.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>net.opentsdb.catalog.TSDBTable</code></p>
 */

public enum TSDBTable {
	/** The tag key meta table  */
	TSD_TAGK(UIDMeta.class, null),
	/** The tag value meta table  */
	TSD_TAGV(UIDMeta.class, null),
	/** The metric meta table */
	TSD_METRIC(UIDMeta.class, null),
	/** The time series meta table */
	TSD_TSMETA(TSMeta.class, new TSTableInfo()),	
	/** The annotation meta table  */
	TSD_ANNOTATION(Annotation.class, new AnnotationTableInfo());
	
	private <T> TSDBTable(Class<T> type, TableInfo<T> ti) {
		this.type = type;
		this.ti = ti!=null ? ti : new UIDTableInfo(this);
	}
	
	/** The table-info instance for this enum */
	public final TableInfo<?> ti;
	
	/** The type of the object stored in this table */
	public final Class<?> type;
	
	/**
	 * Returns the corresponding TSDBTable for the passed object
	 * @param object The object to get the TSDBTable for
	 * @return the TSDBTable
	 */
	public static TSDBTable getTableFor(Object object) {
		if(object==null) throw new IllegalArgumentException("The passed object was null");
		Class<?> clazz = object.getClass();
		if(TSMeta.class.equals(clazz)) {
			return TSD_TSMETA;
		} else if(Annotation.class.equals(clazz)) {
			return TSD_ANNOTATION;
		} else if(UIDMeta.class.equals(clazz)) {
			return TSDBTable.valueOf("TSD_" + ((UIDMeta)object).getType().name());
		} else {
			throw new IllegalArgumentException("The passed object was an invalid type [" + clazz.getName() + "]");
		}
	}
	
	/**
	 * <p>Title: TableInfo</p>
	 * <p>Description: Table specific support methods</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>net.opentsdb.catalog.TSDBTable.TableInfo</code></p>
	 * @param <T> The object type managed by this TableInfo
	 */
	public interface TableInfo<T> {
		/**
		 * Returns the SQL to select an object from its table by the PK
		 * @return a sql string
		 */
		public String getByPKSql();
		/**
		 * Converts the passed object to the correct form for binding into a prepared statement
		 * @param pk The opaque pk value of an object from a TSDBTable
		 * @return the bindable form of the PK
		 */
		public Object getBindablePK(Object pk);
		
		/**
		 * Retrieves the object[s] represented in the passed result set
		 * @param rset The ResultSet representing selected objects
		 * @param dbInterface The DB interface to get the objects from
		 * @return a list of Objects composed from the result set
		 */
		public List<T> getObjects(ResultSet rset, CatalogDBInterface dbInterface);
		
		/**
		 * Syncs the passed object to the TSDB
		 * @param obj The object to sync
		 * @param tsdb The TSDB to sync to
		 * @return The callback deferral
		 */
		public Deferred<Boolean> sync(Object obj, TSDB tsdb);
		
		/**
		 * Returns the DB PK for the passed object
		 * @param conn A connection to query the DB with
		 * @param obj The object to get a DB pk for
		 * @return the pk
		 */
		public Object getPk(Connection conn, Object obj);
		
		/**
		 * Returns the managed object instance identified by the passed pk
		 * @param conn The DB connection to use
		 * @param sqlWorker The database SQLWorker
		 * @param pk The managed object identifier
		 * @param dbInterface The DB interface to resolve the result set into a T
		 * @return The retrieved object
		 */
		public T get(Connection conn, SQLWorker sqlWorker, Object pk, CatalogDBInterface dbInterface);
	}
	
	/**
	 * <p>Title: TSTableInfo</p>
	 * <p>Description: The TableInfo impl for the time series table</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>net.opentsdb.catalog.TSDBTable.TSTableInfo</code></p>
	 */
	public static class TSTableInfo implements TableInfo<TSMeta> {
		/** The select SQL */
		private final String sql;
		/** Instance logger */
		private final Logger log = LoggerFactory.getLogger(getClass());
		/**
		 * Creates a new TSTableInfo
		 */
		public TSTableInfo() {
			sql = "SELECT * FROM TSD_TSMETA WHERE FQNID = ?";
		}

		/**
		 * {@inheritDoc}
		 * @see net.opentsdb.catalog.TSDBTable.TableInfo#getByPKSql()
		 */
		@Override
		public String getByPKSql() {
			return sql; 
		}

		/**
		 * {@inheritDoc}
		 * @see net.opentsdb.catalog.TSDBTable.TableInfo#getBindablePK(java.lang.Object)
		 */
		@Override
		public Object getBindablePK(Object pk) {
			return Long.parseLong(pk.toString());
		}
		
		/**
		 * {@inheritDoc}
		 * @see net.opentsdb.catalog.TSDBTable.TableInfo#getObjects(java.sql.ResultSet, net.opentsdb.catalog.CatalogDBInterface)
		 */
		public List<TSMeta> getObjects(ResultSet rset, CatalogDBInterface dbInterface) {
			return dbInterface.readTSMetas(rset);
		}
		
		/**
		 * {@inheritDoc}
		 * @see net.opentsdb.catalog.TSDBTable.TableInfo#sync(java.lang.Object, net.opentsdb.core.TSDB)
		 */
		public Deferred<Boolean> sync(final Object ts, final TSDB tsdb) {
			final TSMeta tsMeta = (TSMeta)ts;
			final Deferred<Boolean> completion = new Deferred<Boolean>();
			try {
				tsMeta.syncToStorage(tsdb, false).addBothDeferring(new Callback<Deferred<Void>, Boolean>(){
					public Deferred<Void> call(Boolean syncSuccess) throws Exception {
						log.info("Callback on [{}] with arg [{}]", tsMeta, syncSuccess);
						completion.callback(syncSuccess);
						return Deferred.fromResult(null);
					}
				});				
			} catch (Throwable ex) {
				ex.printStackTrace(System.err);
				completion.callback(ex);
			}
			return completion;
		}
		
		/**
		 * {@inheritDoc}
		 * @see net.opentsdb.catalog.TSDBTable.TableInfo#getPk(java.sql.Connection, java.lang.Object)
		 */
		public Long getPk(Connection conn, Object tsMeta) {
			return H2Support.fqnId(conn, ((TSMeta)tsMeta).getTSUID());
		}

		/**
		 * {@inheritDoc}
		 * @see net.opentsdb.catalog.TSDBTable.TableInfo#get(java.sql.Connection, net.opentsdb.catalog.SQLWorker, java.lang.Object, net.opentsdb.catalog.CatalogDBInterface)
		 */
		@Override
		public TSMeta get(Connection conn, SQLWorker sqlWorker, Object pk, CatalogDBInterface dbInterface) {
			return getObjects(sqlWorker.executeQuery(conn, getByPKSql(), true, pk), dbInterface).iterator().next();
		}
	}
	
	
	/**
	 * <p>Title: AnnotationTableInfo</p>
	 * <p>Description: The TableInfo impl for the Annotation table</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>net.opentsdb.catalog.TSDBTable.AnnotationTableInfo</code></p>
	 */
	public static class AnnotationTableInfo implements TableInfo<Annotation> {
		/** The select SQL */
		private final String sql;
		/** Instance logger */
		private final Logger log = LoggerFactory.getLogger(getClass());

		/**
		 * Creates a new AnnotationTableInfo
		 */
		public AnnotationTableInfo() {
			sql = "SELECT * FROM TSD_ANNOTATION WHERE ANNID = ?";
		}

		/**
		 * {@inheritDoc}
		 * @see net.opentsdb.catalog.TSDBTable.TableInfo#getByPKSql()
		 */
		@Override
		public String getByPKSql() {
			return sql; 
		}

		/**
		 * {@inheritDoc}
		 * @see net.opentsdb.catalog.TSDBTable.TableInfo#getBindablePK(java.lang.Object)
		 */
		@Override
		public Object getBindablePK(Object pk) {
			return Long.parseLong(pk.toString());
		}
		
		/**
		 * {@inheritDoc}
		 * @see net.opentsdb.catalog.TSDBTable.TableInfo#getObjects(java.sql.ResultSet, net.opentsdb.catalog.CatalogDBInterface)
		 */
		public List<Annotation> getObjects(ResultSet rset, CatalogDBInterface dbInterface) {
			return dbInterface.readAnnotations(rset);
		}
		
		/**
		 * {@inheritDoc}
		 * @see net.opentsdb.catalog.TSDBTable.TableInfo#sync(java.lang.Object, net.opentsdb.core.TSDB)
		 */
		public Deferred<Boolean> sync(Object ann, TSDB tsdb) {
			Annotation a = (Annotation)ann;
			return a.syncToStorage(tsdb, false);
		}
		
		/**
		 * {@inheritDoc}
		 * @see net.opentsdb.catalog.TSDBTable.TableInfo#getPk(java.sql.Connection, java.lang.Object)
		 */
		public Object getPk(Connection conn, Object annotation) {
			Annotation ann = (Annotation)annotation;
			return H2Support.annotationId(conn, ann.getStartTime(), ann.getTSUID());
		}

		/**
		 * {@inheritDoc}
		 * @see net.opentsdb.catalog.TSDBTable.TableInfo#get(java.sql.Connection, net.opentsdb.catalog.SQLWorker, java.lang.Object, net.opentsdb.catalog.CatalogDBInterface)
		 */
		@Override
		public Annotation get(Connection conn, SQLWorker sqlWorker, Object pk, CatalogDBInterface dbInterface) {
			return getObjects(sqlWorker.executeQuery(conn, getByPKSql(), true, pk), dbInterface).iterator().next();
		}
		
		
	}

	
	/**
	 * <p>Title: UIDTableInfo</p>
	 * <p>Description: The TableInfo impl for UID tables (METRIC, TAGK or TAGV)</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>net.opentsdb.catalog.TSDBTable.UIDTableInfo</code></p>
	 */
	public static class UIDTableInfo implements TableInfo<UIDMeta> {
		/** The select SQL */
		private final String sql;
		/** The UniqueIdType of the UIDMetas */
		private final UniqueIdType type;
		/** Instance logger */
		private final Logger log = LoggerFactory.getLogger(getClass());
		
		
		/**
		 * Creates a new UIDTableInfo
		 * @param table The actual table (METRIC, TAGK or TAGV)
		 */
		public UIDTableInfo(TSDBTable table) {
			sql = String.format("SELECT '%s' as TAG_TYPE, * FROM %s WHERE XUID = ?", table.name().replace("TSD_", ""), table.name());
			type = UniqueIdType.valueOf(table.name().replace("TSD_", ""));
		}

		/**
		 * {@inheritDoc}
		 * @see net.opentsdb.catalog.TSDBTable.TableInfo#getByPKSql()
		 */
		@Override
		public String getByPKSql() {
			return sql; 
		}

		/**
		 * {@inheritDoc}
		 * @see net.opentsdb.catalog.TSDBTable.TableInfo#getBindablePK(java.lang.Object)
		 */
		@Override
		public Object getBindablePK(Object pk) {
			return pk.toString();
		}
		
		/**
		 * {@inheritDoc}
		 * @see net.opentsdb.catalog.TSDBTable.TableInfo#getObjects(java.sql.ResultSet, net.opentsdb.catalog.CatalogDBInterface)
		 */
		public List<UIDMeta> getObjects(ResultSet rset, CatalogDBInterface dbInterface) {
			return dbInterface.readUIDMetas(rset, type);
		}
		
		/**
		 * {@inheritDoc}
		 * @see net.opentsdb.catalog.TSDBTable.TableInfo#sync(java.lang.Object, net.opentsdb.core.TSDB)
		 */
		public Deferred<Boolean> sync(final Object uid, final TSDB tsdb) {
			final UIDMeta uidMeta = (UIDMeta)uid;
			final Deferred<Boolean> completion = new Deferred<Boolean>();
			try {
				uidMeta.syncToStorage(tsdb, false).addBothDeferring(new Callback<Deferred<Void>, Boolean>(){
					public Deferred<Void> call(Boolean syncSuccess) throws Exception {
						log.info("Errback on [{}] with arg [{}]", uidMeta, syncSuccess);
						completion.callback(syncSuccess);
						return Deferred.fromResult(null);
					}
				});											
			} catch (Throwable ex) {
				ex.printStackTrace(System.err);
				return completion;
			}
			return completion;
		}

		/**
		 * {@inheritDoc}
		 * @see net.opentsdb.catalog.TSDBTable.TableInfo#getPk(java.sql.Connection, java.lang.Object)
		 */
		public String getPk(Connection conn, Object uidMeta) {
			return ((UIDMeta)uidMeta).getUID();
		}

		/**
		 * {@inheritDoc}
		 * @see net.opentsdb.catalog.TSDBTable.TableInfo#get(java.sql.Connection, net.opentsdb.catalog.SQLWorker, java.lang.Object, net.opentsdb.catalog.CatalogDBInterface)
		 */
		@Override
		public UIDMeta get(Connection conn, SQLWorker sqlWorker, Object pk, CatalogDBInterface dbInterface) {
			return getObjects(sqlWorker.executeQuery(conn, getByPKSql(), true, pk), dbInterface).iterator().next();
		}
		
		
		
		
	}
}
