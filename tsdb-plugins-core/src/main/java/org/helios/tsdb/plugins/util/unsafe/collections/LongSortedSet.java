/** Helios Development Group LLC, 2013 */
package org.helios.tsdb.plugins.util.unsafe.collections;

/**
 * <p>Title: LongSortedSet</p>
 * <p>Description: A managed off-heap array of unique longs, maintained in sorted order.</p>
 * <p><b><font color='red'>!!  NOTE !!&nbsp;&nbsp;</font>:&nbsp;&nbsp;</b>This class is THREAD UNSAFE. Only use with one thread at a time, or used one
 * of the concurrent/synchronized versions</p>  
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.collections.LongSortedSet</code></p>
 */
public class LongSortedSet {

	/** The underlying UnsafeLongArray */
	protected final UnsafeLongArray array;
	
	/**
	 * Deallocates the allocated memory for this array
	 */
	public void destroy() {
		array.destroy();
	}
	
	/**
	 * Creates a new LongSortedSet
	 * @param array the cloned array
	 */
	protected LongSortedSet(UnsafeLongArray array) {
		this.array = array;
	}
	
	/**
	 * Creates a new LongSortedSet with the default initial capacity of {@link UnsafeArray#DEFAULT_CAPACITY}.
	 */
	public LongSortedSet() {
		this(UnsafeArray.DEFAULT_CAPACITY);
	}
	
	/**
	 * Creates a new LongSortedSet with the specified initial capacity
	 * @param initialCapacity The initial number of allocated slots
	 */
	public LongSortedSet(int initialCapacity) {
		array = UnsafeArrayBuilder.newBuilder().sorted(true).initialCapacity(initialCapacity).buildLongArray();
	}
	
	/**
	 * Creates a new LongSortedSet initialized with the passed values
	 * @param values The long array to initialize with
	 */
	public LongSortedSet(long[] values) {
		array = UnsafeArrayBuilder.newBuilder().sorted(true).buildLongArray(values);
	}
	
	/**
	 * Creates a new LongSortedSet initialized with the passed values
	 * @param values the values to copy into this new sorted set
	 */
	public LongSortedSet(LongSortedSet values) {
		array = UnsafeArrayBuilder.newBuilder().sorted(true).buildLongArray(values);
	}
	
	/**
	 * Inserts the each passed value into the correct slot position in the array if the value is not present in the array already 
	 * @param values The values to insert
	 * @return true if any of the values were successfully added
	 */
	public boolean add(long...values) {
		return array.insertIfNotExists(values)>0;
	}
	
	/**
	 * Removes all the values from this array, shrinking the capacity if necessary.
	 */
	public void clear() {
		array.clear();
	}
	
	/**
	 * Removes all the passed values from the array
	 * @param values The values to remove from the array
	 * @return true if one or more of the values was removed
	 */
	public boolean remove(long...values) {
		return array.remove(values)>0;
	}
	
	/**
	 * Returns the long value at the specified array index
	 * @param index the index of the value to retrieve 
	 * @return the long value at the specified array index
	 */
	public long get(int index) {
		return array.get(index);
	}
	
	/**
	 * Returns the number of entries in the array
	 * @return the number of entries in the array
	 */
	public int size() {
		return array.size();
	}
	
    /**
     * Returns this array as an array of doubles
     * @return an array of doubles
     */
	public double[] asDoubleArray() {
		return array.asDoubleArray();
	}	
	
    /**
     * Returns this array as an array of longs
     * @return an array of longs
     */
	public long[] asLongArray() {
		return array.getArray();
	}
	
	/**
	 * Returns true if the passed long value is in the array
	 * @param value the long value to test for
	 * @return true if the passed long value is in the array, false otherwise
	 */
	public boolean contains(long value) {
		return array.binarySearch(value)>=0;
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return array.toString();
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#clone()
	 */
	@Override
	public LongSortedSet clone() {
		return new LongSortedSet(array.clone());
	}
	
	/**
	 * Indicates if this set is empty
	 * @return true if this set is empty, false otherwise
	 */
	public boolean isEmpty() {
		return array.size()==0;
	}
}
