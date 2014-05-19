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


/**
 * <p>Title: Strategy</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.util.bloom.Strategy</code></p>
 */

interface Strategy extends java.io.Serializable {

    /**
     * Sets {@code numHashFunctions} bits of the given bit array, by hashing a user element.
     *
     * <p>Returns whether any bits changed as a result of this operation.
     */
    <T> boolean put(T object, Funnel<? super T> funnel, int numHashFunctions, UnsafeBitArray bits);

    /**
     * Queries {@code numHashFunctions} bits of the given bit array, by hashing a user element;
     * returns {@code true} if and only if all selected bits are set.
     */
    <T> boolean mightContain(
        T object, Funnel<? super T> funnel, int numHashFunctions, UnsafeBitArray bits);

    /**
     * Identifier used to encode this strategy, when marshalled as part of a BloomFilter.
     * Only values in the [-128, 127] range are valid for the compact serial form.
     * Non-negative values are reserved for enums defined in BloomFilterStrategies;
     * negative values are reserved for any custom, stateful strategy we may define
     * (e.g. any kind of strategy that would depend on user input).
     */
    int ordinal();
  }
