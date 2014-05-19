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
 * <p>Title: RegexBackedBloom</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.util.bloom.RegexBackedBloom</code></p>
 * @param <T> The underlying type being indexed
 */

public class RegexBackedBloom<T> extends AbstractRegexBackedBloom<T> {



	/**
	 * Creates a new RegexBackedBloom
	 * @param expectedInsertions
	 * @param pattern
	 */
	public RegexBackedBloom(int expectedInsertions, CharSequence pattern) {
		super(expectedInsertions, pattern);
		// TODO Auto-generated constructor stub
	}

	/**
	 * Creates a new RegexBackedBloom
	 * @param expectedInsertions
	 * @param fpp
	 * @param pattern
	 */
	public RegexBackedBloom(int expectedInsertions, double fpp,
			CharSequence pattern) {
		super(expectedInsertions, fpp, pattern);
		// TODO Auto-generated constructor stub
	}

	/**
	 * Creates a new RegexBackedBloom
	 * @param expectedInsertions
	 * @param fpp
	 * @param pattern
	 */
	public RegexBackedBloom(int expectedInsertions, double fpp, Pattern pattern) {
		super(expectedInsertions, fpp, pattern);
		// TODO Auto-generated constructor stub
	}

	/**
	 * Creates a new RegexBackedBloom
	 * @param expectedInsertions
	 * @param pattern
	 */
	public RegexBackedBloom(int expectedInsertions, Pattern pattern) {
		super(expectedInsertions, pattern);
		// TODO Auto-generated constructor stub
	}

	/**
	 * {@inheritDoc}
	 * @see com.google.common.hash.Funnel#funnel(java.lang.Object, com.google.common.hash.PrimitiveSink)
	 */
	@Override
	public void funnel(T from, PrimitiveSink into) {
		// TODO Auto-generated method stub
		
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.util.bloom.RegexIndexable#expression(java.lang.Object)
	 */
	@Override
	public CharSequence expression(T t) {
		// TODO Auto-generated method stub
		return null;
	}

}
