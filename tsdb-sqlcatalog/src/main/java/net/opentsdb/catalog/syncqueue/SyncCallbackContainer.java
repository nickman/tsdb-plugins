package net.opentsdb.catalog.syncqueue;

import net.opentsdb.catalog.TSDBTable;


import com.stumbleupon.async.Callback;

/**
 * <p>Title: SyncCallbackContainer</p>
 * <p>Description: A container for passing a context up and down the async callback chain</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>net.opentsdb.catalog.syncqueue.SyncQueueProcessor.SyncCallbackContainer</code></p>
 */
public class SyncCallbackContainer<T, F> implements Callback<Void, T> {
	/** The enum identifying the type of the object */
	protected final TSDBTable table;
	/** The object to be synced */
	protected final T syncObject;
	/** The pk field of the object to be synced */
	protected final Object syncObjectPk;
	/** An exception thrown somewhere in the callback chain */
	protected Exception ex = null;
	/** A counter of failed sync attempts */
	protected int attempts = 0;
	/** The current callback depth */
	protected int currentDepth = 0;
	/** The max callback depth */
	protected int maxDepth = 0;
	
	/** The value passed by the most recent callback */
	protected F lastCallback = null;
	
	/**
	 * Creates a new SyncCallbackContainer
	 * @param table The enum identifying the type of the object
	 * @param syncObject The object to be synced
	 * @param syncObjectPk The pk field of the object to be synced 
	 */
	public SyncCallbackContainer(TSDBTable table, T syncObject, Object syncObjectPk) {
		this.table = table;
		this.syncObject = syncObject;
		this.syncObjectPk = syncObjectPk;
	}
	
	public <N> SyncCallbackContainer<T, N> pivot(Class<N> callbackType) {
		return (SyncCallbackContainer<T, N>) this;
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.stumbleupon.async.Callback#call(java.lang.Object)
	 */
	@Override
	public Void call(T t) throws Exception {
		return null;
	}		
	
	/**
	 * @param callbackValue
	 * @return
	 */
	public SyncCallbackContainer<T, F> callback(F callbackValue) {
		lastCallback = callbackValue;
		return this;
	}
	
	/**
	 * @return
	 */
	public F getLastCallbackValue() {
		return lastCallback;
	}
	
	

	/**
	 * Returns the set exception
	 * @return the callback exception
	 */
	public Exception getEx() {
		return ex;
	}

	/**
	 * Sets the callback exception
	 * @param ex the exception to set
	 * @return this SyncCallbackContainer
	 */
	public SyncCallbackContainer<T, F> setEx(Exception ex) {
		if(this.ex!=null) throw new IllegalStateException("The exception is already set");
		this.ex = ex;
		return this;
	}

	/**
	 * Returns the enum identifying the type of the object 
	 * @return the enum identifying the type of the object
	 */
	public TSDBTable getTable() {
		return table;
	}

	/**
	 * Returns the object to be synced
	 * @return the syncObject
	 */
	public Object getSyncObject() {
		return syncObject;
	}

	/**
	 * Returns the pk of the object to be synced
	 * @return the syncObjectPk
	 */
	public Object getSyncObjectPk() {
		return syncObjectPk;
	}
	
	/**
	 * Returns the  current callback depth
	 * @return the currentDepth
	 */
	public int getCurrentDepth() {
		return currentDepth;
	}



	/**
	 * Returns the max callback depth 
	 * @return the maxDepth
	 */
	public int getMaxDepth() {
		return maxDepth;
	}
	
	/**
	 * Increments the current depth and celings the max depth
	 * @return this SyncCallbackContainer
	 */
	public SyncCallbackContainer<T, F> incrDepth() {
		currentDepth++;
		maxDepth = currentDepth;
		return this;
	}
	
	/**
	 * Decrements the current depth
	 * @return this SyncCallbackContainer
	 */
	public SyncCallbackContainer<T, F> decrDepth() {
		currentDepth--;
		return this;
	}
	

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("SyncCallbackContainer [");
		if (table != null) {
			builder.append("table=");
			builder.append(table);
			builder.append(", ");
		}
		if (syncObject != null) {
			builder.append("syncObject=");
			builder.append(syncObject);
			builder.append(", ");
		}
		if (syncObjectPk != null) {
			builder.append("syncObjectPk=");
			builder.append(syncObjectPk);
			builder.append(", ");
		}
		if (ex != null) {
			builder.append("ex=");
			builder.append(ex);
			builder.append(", ");
		}
		builder.append("attempts=");
		builder.append(attempts);
		builder.append(", currentDepth=");
		builder.append(currentDepth);
		builder.append(", maxDepth=");
		builder.append(maxDepth);
		builder.append("]");
		return builder.toString();
	}




	
}