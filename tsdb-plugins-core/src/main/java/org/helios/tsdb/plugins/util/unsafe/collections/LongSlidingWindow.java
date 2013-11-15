/** Helios Development Group LLC, 2013 */
package org.helios.tsdb.plugins.util.unsafe.collections;

import java.nio.LongBuffer;
import java.util.Arrays;
import java.util.Random;

/**
 * <p>Title: LongSlidingWindow</p>
 * <p>Description: A fixed size sorted "list" of longs that when full, drops the oldest entry to make room for the newest.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.collections.LongSlidingWindow</code></p>
 */

public class LongSlidingWindow  implements ILongSlidingWindow {
	/** The underlying UnsafeLongArray */
	protected final UnsafeLongArray array;
	
	/**
     * Deallocates this UnsafeLongArrray
     */
    public void destroy() {
    	array.destroy();
    }
	
	/**
	 * Creates a new and empty LongSlidingWindow
	 * @param size The size of the sliding window
	 */
	public LongSlidingWindow(int size) {
		array = UnsafeArrayBuilder.newBuilder().sorted(false).fixed(true).initialCapacity(size).maxCapacity(size).buildLongArray();
	}
	
	/**
	 * Creates a new LongSlidingWindow cloned from the passed array
	 * @param array The array to base this sliding window from
	 */
	protected LongSlidingWindow(UnsafeLongArray array) {
		this.array = array;
	}
	
	@Override
	public void reinitAndLoad(byte[] arr) {
		array.initAndLoad(arr);
	}
	
    /**
     * Returns this sliding window as a (copied) byte array
     * @return this sliding window as a (copied) byte array
     */
    public byte[] getBytes() {
    	return array.getBytes();
    }	
    
    public boolean isFull() {
    	return array.size()==array.maxCapacity;
    }
	
    public String debug() {
    	return new StringBuilder("LongSlidingWindow:")
    		.append("\n\tSize:").append(array.size)
    		.append("\n\tMax Size:").append(array.maxCapacity)
    		.append("\n\tSorted:").append(array.sorted)
    		
    	.toString();
    }
	
	/**
	 * Creates a new LongSlidingWindow with the provided initial values
	 * @param size The size of the sliding window
	 * @param values The initial values to load
	 */
	public LongSlidingWindow(int size, long[] values) {
		array = UnsafeArrayBuilder.newBuilder().sorted(false).fixed(true).initialCapacity(size).maxCapacity(size).buildLongArray();
		for(long v: values) {
			array.rollRight(0, v);
		}
	}
	
	/**
	 * Inserts the each passed value into the first slot position in the array dropping the values in the last slot to make room if required
	 * @param values The values to insert
	 */
	@Override
	public void insert(long...values) {
		for(long v: values) {
			array.rollRight(0, v);
		}
	}
	
	/**
	 * Inserts the contents of a LongBuffer into the sliding window
	 * @param longBuff a long buffer
	 */
	@Override
	public void insert(LongBuffer longBuff) {
		if(longBuff.hasArray()) {
			long[] larr = new long[longBuff.limit()];
			longBuff.get(larr);
			insert(larr);
		} else {
			longBuff.position(0);
			while(longBuff.remaining()>0) {
				insert(longBuff.get());
			}
			
		}
	}
	
	
	/**
	 * Inserts the passed value into the first slot of the array, moving all other other populated slots to the right.
	 * @param value The value to insert
	 * @return The dropped value if one was dropped, otherwise null
	 */
	@Override
	public Long insert(long value) {
		return array.rollRightCap(0, value);		
	}
	
	
	/**
	 * Removes all the values from this array, keeping the capacity fixed.
	 */
	@Override
	public void clear() {
		array.clear();
	}
	
	/**
	 * Indicates if this set is empty
	 * @return true if this set is empty, false otherwise
	 */
	@Override
	public boolean isEmpty() {
		return array.size()==0;
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
	public LongSlidingWindow clone() {
		return new LongSlidingWindow(array.clone());
	}
	
	/**
	 * Returns the long value at the specified array index
	 * @param index the index of the value to retrieve 
	 * @return the long value at the specified array index
	 */
	@Override
	public long get(int index) {
		return array.get(index);
	}
	
	/**
	 * Returns the first (chronologically the most recent) value in the array or -1L if the size is 0.
	 * @return the first value in the array or -1L if the size is 0.
	 */
	public long getFirst() {
		if(size()<1) return -1L;
		return array.get(0);
	}
	
	/**
	 * Returns the last value (chronologically the oldest) in the array or -1L if the size is 0.
	 * @return the last value in the array or -1L if the size is 0.
	 */
	public long getLast() {
		if(size()<1) return -1L;
		return array.get(size()-1);
	}
	
	
	/**
	 * Returns this sliding window as a double array
	 * @return a double array
	 */
	@Override
	public double[] asDoubleArray() {
		return array.asDoubleArray();
	}
	
	/**
	 * Returns this sliding window as a long array
	 * @return a long array
	 */
	public long[] asLongArray() {
		return array.getArray();
	}
	
	
	/**
	 * Returns the number of entries in the array
	 * @return the number of entries in the array
	 */
	@Override
	public int size() {
		return array.size();
	}
	
	/**
	 * Returns the sum of all the longs in the array within the passed ending index range
	 * @param within The index of the last entry to sum 
	 * @return the sum of all the longs in the array
	 */
	@Override
	public long sum(int within) {
		long total = 0;
		int end = within<array.size ? within : array.size;
		for(int i = 0; i < end; i++) {
			total += array.get(i);
		}
		return total;
	}
	
	/**
	 * Returns the sum of all the longs in the array 
	 * @return the sum of all the longs in the array
	 */
	@Override
	public long sum() {
		return sum(array.size);
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.util.unsafe.collections.ILongSlidingWindow#min()
	 */
	@Override
	public long min() {
		if(array.size==0) throw new RuntimeException("Cannot get min for empty array");
		long[] arr = asLongArray();
		Arrays.sort(arr);
		return arr[0];
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.util.unsafe.collections.ILongSlidingWindow#max()
	 */
	@Override
	public long max() {
		if(array.size==0) throw new RuntimeException("Cannot get max for empty array");
		long[] arr = asLongArray();
		Arrays.sort(arr);
		return arr[arr.length-1];		
	}
	
	/**
	 * Returns the average of all the longs in the array within the passed ending index range
	 * @param within The index of the last entry to average 
	 * @return the average of all the longs in the array
	 */
	@Override
	public long avg(int within) {
		double total = 0;
		double cnt = 0;
		int end = within<array.size ? within : array.size;
		for(int i = 0; i < end; i++) {
			total += array.get(i);
			cnt++;
		}
		if(total==0 || cnt==0) return 0;
		double d = total/cnt;
		return (long)d;
	}
	
	/** A half as a double */
	public static final double ONE_HALF = 0.5d;
	
	/**
	 * Returns the pth percentile value from this array using the nearest rank formula.
	 * @param p The percentile to get
	 * @return the pth percentile value
	 */
	public long percentile(int p) {
		if (p < 1 || p > 100) {
			throw new IllegalArgumentException("invalid percentile: " + p);
		}
		double _p = p;
		double _pp = _p/100d;
		LongSlidingWindow sorted = new LongSlidingWindow(new LongSortedSet(clone().array).array);
		int ix = (int)((_pp*sorted.size())+ONE_HALF);
		return sorted.get(ix);
	}
	
	public static void main(String[] args) {
		log("Percentile Test");
		LongSlidingWindow lsw = new LongSlidingWindow(1000);
		Random r = new Random(System.currentTimeMillis());
		for(int i = 0; i < 1000; i++) {
			lsw.insert(Math.abs(r.nextInt()));
		}
		log("Min:" + lsw.min());
		log("Max:" + lsw.max());
		log("30th Percentile:" + lsw.percentile(30));
		log("90th Percentile:" + lsw.percentile(90));
		log("99th Percentile:" + lsw.percentile(99));
		
	}
	
	/**
	 * Returns the average of all the longs in the array 
	 * @return the average of all the longs in the array
	 */
	@Override
	public long avg() {
		return avg(array.size);
	}
	
//	public static void main(String[] args) {
//		log("Long Sliding Window Test");
//		LongSlidingWindow sw = new LongSlidingWindow(5, new long[]{1,2,3,4,5});
//		for(int i = 0; i < 5; i++) {
//			Long val = sw.insert(i);
//			log("Removed:" + val);
//		}
//	}
	
	public static void log(Object msg) {
		System.out.println(msg);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.util.collections.ILongSlidingWindow#inc(int, long)
	 */
	@Override
	public long inc(int index, long value) {
		// TODO Auto-generated method stub
		return 0;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.util.collections.ILongSlidingWindow#inc(int)
	 */
	@Override
	public long inc(int index) {
		// TODO Auto-generated method stub
		return 0;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.util.collections.ILongSlidingWindow#inc(long)
	 */
	@Override
	public long inc(long value) {
		// TODO Auto-generated method stub
		return 0;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.util.collections.ILongSlidingWindow#inc()
	 */
	@Override
	public long inc() {
		// TODO Auto-generated method stub
		return 0;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.util.collections.ILongSlidingWindow#find(long)
	 */
	@Override
	public int find(long value) {
		// TODO Auto-generated method stub
		return 0;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.util.collections.ILongSlidingWindow#set(long)
	 */
	@Override
	public void set(long value) {
		// TODO Auto-generated method stub
		
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.util.collections.ILongSlidingWindow#load(byte[])
	 */
	@Override
	public void load(byte[] arr) {
		// TODO Auto-generated method stub
		
	}

}
