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
package org.helios.tsdb.plugins.util.bloom;

import java.util.regex.Pattern;

import com.google.common.hash.Funnel;
import com.google.common.hash.PrimitiveSink;

/**
 * <p>Title: AbstractRegexBackedBloom</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.util.bloom.AbstractRegexBackedBloom</code></p>
 * @param <T> The type being indexed
 */

public abstract class AbstractRegexBackedBloom<T> implements Funnel<T>, RegexIndexable<T> {
	/** The number of expected insertions to the constructed UnsafeBloomFilter<T> */
	protected final int expectedInsertions;
	/** The desired false positive probability (must be positive and less than 1.0) */
	protected final double fpp;
	/** The unsafe bloom filter */
	protected final UnsafeBloomFilter<T> filter;
	/** The underlying pattern matcher */
	protected final Pattern pattern;
	
	/**  */
	private static final long serialVersionUID = -4385713292301675664L;

	/** The default false positive probability */
	public static final double DEFAULT_FPP = 0.03;
	
	/**
	 * Creates a new AbstractRegexBackedBloom
	 * @param expectedInsertions the number of expected insertions to the constructed UnsafeBloomFilter<T>; must be positive
	 * @param fpp the desired false positive probability (must be positive and less than 1.0)
	 * @param pattern The underlying pattern matcher
	 */
	public AbstractRegexBackedBloom(int expectedInsertions, double fpp, Pattern pattern) {
		this.expectedInsertions = expectedInsertions;
		this.fpp = fpp;
		this.pattern = pattern;
		filter = UnsafeBloomFilter.create(this, this.expectedInsertions, this.fpp);
	}
	
	/**
	 * Creates a new AbstractRegexBackedBloom
	 * @param expectedInsertions the number of expected insertions to the constructed UnsafeBloomFilter<T>; must be positive
	 * @param fpp the desired false positive probability (must be positive and less than 1.0)
	 * @param pattern The underlying pattern matcher
	 */
	public AbstractRegexBackedBloom(int expectedInsertions, double fpp, CharSequence pattern) {
		this(expectedInsertions, fpp, Pattern.compile(pattern.toString()));
	}
	
	
	/**
	 * Creates a new AbstractRegexBackedBloom using the default false positive probability
	 * @param expectedInsertions the number of expected insertions to the constructed UnsafeBloomFilter<T>; must be positive
	 * @param pattern The underlying pattern matcher
	 */
	public AbstractRegexBackedBloom(int expectedInsertions, Pattern pattern) {
		this(expectedInsertions, DEFAULT_FPP, pattern);
	}
	
	/**
	 * Creates a new AbstractRegexBackedBloom using the default false positive probability
	 * @param expectedInsertions the number of expected insertions to the constructed UnsafeBloomFilter<T>; must be positive
	 * @param pattern The underlying pattern matcher
	 */
	public AbstractRegexBackedBloom(int expectedInsertions, CharSequence pattern) {
		this(expectedInsertions, Pattern.compile(pattern.toString()));
	}
	
	
	
	/**
	 * {@inheritDoc}
	 * @see com.google.common.hash.Funnel#funnel(java.lang.Object, com.google.common.hash.PrimitiveSink)
	 */
	@Override
	public abstract void funnel(T from, PrimitiveSink into);

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.util.bloom.RegexIndexable#expression(java.lang.Object)
	 */
	@Override
	public abstract CharSequence expression(T t);	

}
