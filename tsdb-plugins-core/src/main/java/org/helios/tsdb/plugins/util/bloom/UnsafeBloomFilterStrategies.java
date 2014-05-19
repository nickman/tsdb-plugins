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

import com.google.common.hash.Funnel;
import com.google.common.hash.Hashing;


/**
 * <p>Title: UnsafeBloomFilterStrategies</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.util.bloom.UnsafeBloomFilterStrategies</code></p>
 */

public enum UnsafeBloomFilterStrategies implements Strategy {
	 /**
	   * See "Less Hashing, Same Performance: Building a Better Bloom Filter" by Adam Kirsch and
	   * Michael Mitzenmacher. The paper argues that this trick doesn't significantly deteriorate the
	   * performance of a Bloom filter (yet only needs two 32bit hash functions).
	   */
	  MURMUR128_MITZ_32() {
	    @Override public <T> boolean put(T object, Funnel<? super T> funnel,
	        int numHashFunctions, UnsafeBitArray bits) {
	      // TODO(user): when the murmur's shortcuts are implemented, update this code
	      long hash64 = Hashing.murmur3_128().newHasher().putObject(object, funnel).hash().asLong();
	      int hash1 = (int) hash64;
	      int hash2 = (int) (hash64 >>> 32);
	      boolean bitsChanged = false;
	      for (int i = 1; i <= numHashFunctions; i++) {
	        int nextHash = hash1 + i * hash2;
	        if (nextHash < 0) {
	          nextHash = ~nextHash;
	        }
	        bitsChanged |= bits.set(nextHash % bits.size());
	      }
	      return bitsChanged;
	    }

	    @Override public <T> boolean mightContain(T object, Funnel<? super T> funnel,
	        int numHashFunctions, UnsafeBitArray bits) {
	      long hash64 = Hashing.murmur3_128().newHasher().putObject(object, funnel).hash().asLong();
	      int hash1 = (int) hash64;
	      int hash2 = (int) (hash64 >>> 32);
	      for (int i = 1; i <= numHashFunctions; i++) {
	        int nextHash = hash1 + i * hash2;
	        if (nextHash < 0) {
	          nextHash = ~nextHash;
	        }
	        if (!bits.get(nextHash % bits.size())) {
	          return false;
	        }
	      }
	      return true;
	    }
	  };
	  

}
