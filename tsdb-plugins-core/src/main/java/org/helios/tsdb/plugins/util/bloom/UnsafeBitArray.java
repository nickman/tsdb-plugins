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
package org.helios.tsdb.plugins.util.bloom;

import static org.helios.jmx.util.unsafe.UnsafeAdapter.LONG_ARRAY_OFFSET;
import static org.helios.jmx.util.unsafe.UnsafeAdapter.copyMemory;
import static org.helios.jmx.util.unsafe.UnsafeAdapter.getLong;
import static org.helios.jmx.util.unsafe.UnsafeAdapter.putLong;
import static org.helios.jmx.util.unsafe.UnsafeAdapter.setMemory;

import java.math.RoundingMode;

import org.helios.jmx.util.unsafe.DeAllocateMe;
import org.helios.jmx.util.unsafe.UnsafeAdapter;

import com.google.common.math.IntMath;

/**
 * <p>Title: UnsafeBitArray</p>
 * <p>Description: A reimplementation of google guava's BitArray class using off heap allocation.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.util.bloom.UnsafeBitArray</code></p>
 */

public class UnsafeBitArray implements DeAllocateMe {
	/** The assigned allocation address */
	protected final long[][] address = new long[1][1];
	/** The number of set bits */
	protected int bits = 0;
	/** The number of longs managed for the bit array */
	protected final int size;
	
	private long address() {
		return address[0][0];
	}
	
	/** A zero byte */
	public static final byte ZERO = 0;
	
	/**
	 * Creates a new UnsafeBitArray
	 * @param data The data allocation for the bit array
	 */
	public UnsafeBitArray(long[] data) {
		if(data==null || data.length==0) throw new IllegalArgumentException("Data length was zero or null");
		size = data.length;
	    int bitCount = 0;
	    long bytesOfData = 0;
	    for (long value : data) {
	        bitCount += Long.bitCount(value);
	    }
	    bits = bitCount;
	    bytesOfData = bitCount << 3;
	    address[0][0] = UnsafeAdapter.allocateMemory(bytesOfData, this);
	    copyMemory(data, LONG_ARRAY_OFFSET, null, address(), bytesOfData);
	}
	
	private UnsafeBitArray(int bits, int size, long srcAddress) {
		this.bits = bits;
		this.size = size;
		long byteSize = size << 3;
		address[0][0] = UnsafeAdapter.allocateMemory(byteSize, this);
		copyMemory(srcAddress, address[0][0], byteSize);
	}
	
	/**
	 * Creates a new UnsafeBitArray
	 * @param bits The number of bits to represent
	 */
	public UnsafeBitArray(int bits) {
		this.bits = bits;
		size = IntMath.divide(bits, 64, RoundingMode.CEILING);
		long byteSize = size << 3;
		address[0][0] = UnsafeAdapter.allocateMemory(byteSize, this);
		setMemory(address[0][0], byteSize, ZERO);
	}
	
	
	
	/**
	 * Returns this bit array as a long array
	 * @return a long array
	 */
	public long[] toLongArray() {
		long[] arr = new long[size];
		copyMemory(null, address[0][0], arr, LONG_ARRAY_OFFSET, size << 3);
		return arr;
	}
	
	private long data(int index) {
		return getLong(address[0][0] + (index << 3));
	}
	
	private void data(int index, long value) {
		putLong(address[0][0] + (index << 3), value);
	}
	
	
	private int byteSize() {
		return size << 3;
	}
	
    /**
     * Determines if the bit at the specified index is on
     * @param index the bit index
     * @return true if on, false if off
     */
    public boolean get(int index) {
    	return (data(index >> 6) & (1L << index)) != 0;
//    	return getLong(address[0][0] + ((index >> 6) << 3)) != 0;
    }
	
	/**
	 * Conditionally sets the bit at the specified bit index
	 * @param index the bit index
	 * @return true if set, false otherwise
	 */
	public boolean set(int index) {
		if (!get(index)) {
			data(index >> 6, data(index >> 6) | (1L << index));
			bits++;
			return true;
		}
		return false;
	}	
	

    /**
     * Returns the bit size of this array
     * @return the bit size of this array
     */
    public int size() {
      return size << 6;
    }
    
    /**
     * Returns the number of set bits (1s)
     * @return the number of set bits
     */
    public int bitCount() {
      return bits;
    }

    /**
     * Creates a copy of this bit array
     * @return a copy of this bit array
     */
    public UnsafeBitArray copy() {
        return new UnsafeBitArray(bits, byteSize(), address());
    }    
    
    @Override
    public int hashCode() {
        int result = 1;
        for(int i = 0; i < size; i++) {
        	int elementHash = (int)(data(0) ^ (data(0) >>> 32));
        	result = 31 * result + elementHash;
        }
        return result;
    }
    
    @Override 
    public boolean equals(Object o) {
        if (o instanceof UnsafeBitArray) {        	
        	UnsafeBitArray bitArray = (UnsafeBitArray)o;
        	if(bitArray.size != size) return false;
        	for(int i = 0; i < size; i++) {
        		if(data(0) != bitArray.data(0)) return false;
        	}
        	return true;
        }
        return false;
      }    
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.util.unsafe.DeAllocateMe#getAddresses()
	 */
	@Override
	public long[][] getAddresses() {
		return address;
	}

}
