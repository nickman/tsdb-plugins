/** Helios Development Group LLC, 2013 */
package org.helios.tsdb.plugins.util.unsafe.collections;

import java.nio.LongBuffer;
import java.util.Arrays;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

/**
 * <p>Title: ConcurrentLongSlidingWindow</p>
 * <p>Description: A fixed size sliding window unsafe long array, guarded by a {@link ReentrantReadWriteLock}</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.util.collections.ConcurrentLongSlidingWindow</code></p>
 */

public class ConcurrentLongSlidingWindow extends LongSlidingWindow implements ILongSlidingWindow {
	/** The reentrant read/write lock */
	private final ReentrantReadWriteLock readWriteLock;
	/** The concurrent read lock */
	private final ReadLock readLock;
	/** The exclusive write lock */
	private final WriteLock writeLock;
	

	/**
	 * Creates a new ConcurrentLongSlidingWindow
	 * @param size the fixed size of the sliding window
	 */
	public ConcurrentLongSlidingWindow(int size) {
		super(size);
		readWriteLock = new ReentrantReadWriteLock(false);
		readLock = readWriteLock.readLock();
		writeLock = readWriteLock.writeLock();		
	}

	/**
	 * Creates a new ConcurrentLongSlidingWindow
	 * @param size the fixed size of the sliding window
	 * @param values the initial values of the sliding window
	 */
	public ConcurrentLongSlidingWindow(int size, long[] values) {
		super(size, values);
		readWriteLock = new ReentrantReadWriteLock(false);
		readLock = readWriteLock.readLock();
		writeLock = readWriteLock.writeLock();		
	}
	
	private ConcurrentLongSlidingWindow(UnsafeLongArray array) {
		super(array);
		readWriteLock = new ReentrantReadWriteLock(false);
		readLock = readWriteLock.readLock();
		writeLock = readWriteLock.writeLock();				
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.util.collections.ILongSlidingWindow#insert(long)
	 */
	@Override
	public void insert(long...values) {
		writeLock.lock();
		try {
			super.insert(values);
		} finally {
			writeLock.unlock();
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.util.collections.ILongSlidingWindow#insert(java.nio.LongBuffer)
	 */
	@Override
	public void insert(LongBuffer longBuff) {
		writeLock.lock();
		try {
				long[] larr = new long[longBuff.limit()];
				longBuff.get(larr);
				super.insert(larr);
		} finally {
			writeLock.unlock();
		}
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.util.collections.ILongSlidingWindow#insert(long)
	 */
	@Override
	public Long insert(long value) {
		writeLock.lock();
		try {
			return super.insert(value);
		} finally {
			writeLock.unlock();
		}
	}	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.util.collections.ILongSlidingWindow#inc(int, long)
	 */
	@Override
	public long inc(int index, long value) {
		writeLock.lock();
		try {
			if(size()<index+1) throw new ArrayOverflowException("Attempted to increment at index [" + index + "] but size is [" + size() + "]", new Throwable());
			return array.set(index, array.get(index)+value).get(index);
		} finally {
			writeLock.unlock();
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.util.collections.ILongSlidingWindow#inc(int)
	 */
	@Override
	public long inc(int index) {
		return inc(index, 1L);
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.util.collections.ILongSlidingWindow#inc(long)
	 */
	@Override
	public long inc(long value) {
		return inc(0, value);
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.util.collections.ILongSlidingWindow#inc()
	 */
	@Override
	public long inc() {
		return inc(0, 1L);
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.util.collections.ILongSlidingWindow#find(long)
	 */
	@Override
	public int find(long value) {
		readLock.lock();
		try {
			return array.binarySearch(value);
		} finally {
			readLock.unlock();
		}
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.util.collections.ILongSlidingWindow#set(long)
	 */
	@Override
	public void set(long value) {
		writeLock.lock();
		try {
			array.set(0, value);
		} finally {
			writeLock.unlock();
		}
	}
	
    /**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.util.collections.ILongSlidingWindow#asDoubleArray()
	 */
	@Override
	public double[] asDoubleArray() {
		readLock.lock();
		try {
			return array.asDoubleArray();
		} finally {
			readLock.unlock();
		}
	}	
	
	/**
	 * Returns this sliding window as a long array
	 * @return a long array
	 */
	public long[] asLongArray() {
		readLock.lock();
		try {
			return array.getArray();
		} finally {
			readLock.unlock();
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.util.collections.ILongSlidingWindow#load(byte[])
	 */
	@Override
	public void load(byte[] arr) {
		writeLock.lock();
		try {
			array.load(arr);
		} finally {
			writeLock.unlock();
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.util.collections.ILongSlidingWindow#reinitAndLoad(byte[])
	 */
	@Override
	public void reinitAndLoad(byte[] arr) {
		writeLock.lock();
		try {
			array.initAndLoad(arr);
		} finally {
			writeLock.unlock();
		}
		
		
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.util.collections.ILongSlidingWindow#clear()
	 */
	@Override
	public void clear() {
		writeLock.lock();
		try {
			super.clear();
		} finally {
			writeLock.unlock();
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.util.collections.ILongSlidingWindow#isEmpty()
	 */
	@Override
	public boolean isEmpty() {
		readLock.lock();
		try {
			return super.isEmpty();
		} finally {
			readLock.unlock();
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.util.collections.ILongSlidingWindow#toString()
	 */
	@Override
	public String toString() {
		readLock.lock();
		try {
			return super.toString();
		} finally {
			readLock.unlock();
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.util.collections.ILongSlidingWindow#clone()
	 */
	@Override
	public LongSlidingWindow clone() {
		readLock.lock();
		try {
			return new LongSlidingWindow(array.clone());
		} finally {
			readLock.unlock();
		}
	}
	
	/**
	 * Returns the most recent value in the array or -1L if the size is 0.
	 * @return the most recent in the array or -1L if the size is 0.
	 */
	public long getNewest() {
		return getFirst();
	}
	
	/**
	 * Returns the oldest value in the array or -1L if the size is 0.
	 * @return the oldest in the array or -1L if the size is 0.
	 */
	public long getOldest() {
		return getLast();
	}
	

	
	/**
	 * Returns the first (chronologically the most recent) value in the array or -1L if the size is 0.
	 * @return the first value in the array or -1L if the size is 0.
	 */
	public long getFirst() {
		readLock.lock();
		try {
			if(size()<1) return -1L;
			return array.get(0);
		} finally {
			readLock.unlock();
		}
	}
	
	/**
	 * Returns the last value (chronologically the oldest) in the array or -1L if the size is 0.
	 * @return the last value in the array or -1L if the size is 0.
	 */
	public long getLast() {
		readLock.lock();
		try {
			if(size()<1) return -1L;
			return array.get(size()-1);
		} finally {
			readLock.unlock();
		}
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.util.collections.ILongSlidingWindow#get(int)
	 */
	@Override
	public long get(int index) {
		readLock.lock();
		try {
			return array.get(index);
		} finally {
			readLock.unlock();
		}
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.util.collections.ILongSlidingWindow#size()
	 */
	@Override
	public int size() {
		readLock.lock();
		try {
			return array.size();
		} finally {
			readLock.unlock();
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.util.unsafe.collections.LongSlidingWindow#min()
	 */
	@Override
	public long min() {
		if(array.size==0) throw new RuntimeException("Cannot get min for empty array");
		long[] arr = null;
		readLock.lock();
		try {
			arr = asLongArray();
		} finally {
			readLock.unlock();
		}
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
		long[] arr = null;
		readLock.lock();
		try {
			arr = asLongArray();
		} finally {
			readLock.unlock();
		}
		Arrays.sort(arr);
		return arr[arr.length-1];		
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.util.collections.ILongSlidingWindow#sum(int)
	 */
	@Override
	public long sum(int within) {
		readLock.lock();
		try {
			return super.sum(within);
		} finally {
			readLock.unlock();
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.util.collections.ILongSlidingWindow#sum()
	 */
	@Override
	public long sum() {
		readLock.lock();
		try {
			return super.sum();
		} finally {
			readLock.unlock();
		}

	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.util.collections.ILongSlidingWindow#avg(int)
	 */
	@Override
	public long avg(int within) {
		readLock.lock();
		try {
			return super.avg(within);
		} finally {
			readLock.unlock();
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.util.collections.ILongSlidingWindow#avg()
	 */
	@Override
	public long avg() {
		readLock.lock();
		try {
			return super.avg();
		} finally {
			readLock.unlock();
		}

	}
	
	

}
