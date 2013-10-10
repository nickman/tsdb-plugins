/** Helios Development Group LLC, 2013 */
package org.helios.tsdb.plugins.util.unsafe.collections;

import java.nio.LongBuffer;

/**
 * <p>Title: ILongSlidingWindow</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.collections.ILongSlidingWindow</code></p>
 */

public interface ILongSlidingWindow {

	/**
	 * Inserts the each passed value into the first slot position in the array dropping the values in the last slot to make room if required
	 * @param values The values to insert
	 */
	public abstract void insert(long... values);

	public abstract void insert(LongBuffer longBuff);

	/**
	 * Inserts the passed value into the first slot of the array, moving all other other populated slots to the right.
	 * @param value The value to insert
	 * @return The dropped value if one was dropped, otherwise null
	 */
	public abstract Long insert(long value);

	/**
	 * Increments the value at the specified index by the passed amount
	 * @param index The index to update at
	 * @param value The amount to increment by
	 * @return The new value
	 */
	public abstract long inc(int index, long value);

	/**
	 * Increments the value at the specified index by 1
	 * @param index The index to update at
	 * @return The new value
	 */
	public abstract long inc(int index);

	/**
	 * Increments the value of the 0th index by the passed amount
	 * @param value The amount to increment by
	 * @return The new value
	 */
	public abstract long inc(long value);

	/**
	 * Increments the value of the 0th index by 1
	 * @return The new value
	 */
	public abstract long inc();

	/**
	 * Attempts to locate the index of the passed value
	 * @param value The value to search for
	 * @return the index of the located value, which will be negative if not found
	 */
	public abstract int find(long value);

	/**
	 * Sets the value of the 0th index to the passed value
	 * @param value The value to set to 
	 */
	public abstract void set(long value);

	/**
	 * Returns this array as an array of doubles
	 * @return an array of doubles
	 */
	public abstract double[] asDoubleArray();

	/**
	 * Loads this window from a byte array
	 * @param arr the byte array
	 */
	public abstract void load(byte[] arr);

	public abstract void reinitAndLoad(byte[] arr);

	/**
	 * Removes all the values from this array, keeping the capacity fixed.
	 */
	public abstract void clear();

	/**
	 * Indicates if this set is empty
	 * @return true if this set is empty, false otherwise
	 */
	public abstract boolean isEmpty();

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	public abstract String toString();



	/**
	 * Returns the long value at the specified array index
	 * @param index the index of the value to retrieve 
	 * @return the long value at the specified array index
	 */
	public abstract long get(int index);

	/**
	 * Returns the number of entries in the array
	 * @return the number of entries in the array
	 */
	public abstract int size();

	/**
	 * Returns the sum of all the longs in the array within the passed ending index range
	 * @param within The index of the last entry to sum 
	 * @return the sum of all the longs in the array
	 */
	public abstract long sum(int within);

	/**
	 * Returns the sum of all the longs in the array 
	 * @return the sum of all the longs in the array
	 */
	public abstract long sum();
	
	/**
	 * Returns the minumum value in the array
	 * @return the minumum value in the array
	 */
	public abstract long min();

	/**
	 * Returns the maxumum value in the array
	 * @return the maxumum value in the array
	 */
	public abstract long max();

	/**
	 * Returns the average of all the longs in the array within the passed ending index range
	 * @param within The index of the last entry to average 
	 * @return the average of all the longs in the array
	 */
	public abstract long avg(int within);

	/**
	 * Returns the average of all the longs in the array 
	 * @return the average of all the longs in the array
	 */
	public abstract long avg();

}