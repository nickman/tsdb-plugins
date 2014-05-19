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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.helios.jmx.util.helpers.SystemClock;
import org.helios.jmx.util.helpers.SystemClock.ElapsedTime;
import org.helios.jmx.util.unsafe.UnsafeAdapter;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnel;
import com.google.common.hash.PrimitiveSink;

/**
 * <p>Title: TestBloomFilter</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.util.bloom.TestBloomFilter</code></p>
 */

public class TestBloomFilter {

	/**
	 * Creates a new TestBloomFilter
	 */
	public TestBloomFilter() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		System.setProperty("unsafe.allocations.track", "true");
		System.setProperty("unsafe.allocations.align", "true");
		log("BloomFilter Test");
		int unique = 10000000;
		int totalMultiplier = 3;
		int intoFilterDiv = 5;
		double prob = 0.0000001;
		int warmupLoops = 3;
//		double prob = 0.0001;
		Funnel<UUID> funnel = new Funnel<UUID>() {
			private static final long serialVersionUID = -2177915412707545905L;
			public void funnel(UUID from, PrimitiveSink into) {
				into.putLong(from.getLeastSignificantBits()).putLong(from.getMostSignificantBits());
			}
		};

		List<UUID> uuids = new ArrayList<UUID>(totalMultiplier * unique);
		Set<UUID> containedUuids = new HashSet<UUID>(unique/intoFilterDiv);
		
		
		
		for(int i = 0; i < unique; i++) {
			UUID uuid = UUID.randomUUID();
			if(i%intoFilterDiv==0) {
				containedUuids.add(uuid);
			}			
			for(int x = 0; x < totalMultiplier; x++) {
				uuids.add(uuid);
			}
		}
		log("Generated UUID samples");
		Collections.shuffle(uuids, new Random(System.currentTimeMillis()));
		log("Shuffled UUID samples");
		log(UnsafeAdapter.printUnsafeMemoryStats());
		log("Warmup Starting");
		for(int i = 0; i < warmupLoops; i++) {
			System.gc();
//			runUnsafe(false, uuids, containedUuids, UnsafeBloomFilter.create(funnel, unique/intoFilterDiv, prob));
			runUnsafe(false, uuids, containedUuids, UnsafeBloomFilter.create(funnel, unique/intoFilterDiv));
			if(i==warmupLoops-1) log(UnsafeAdapter.printUnsafeMemoryStats());
//			runStandard(false, uuids, containedUuids, BloomFilter.create(funnel, unique/intoFilterDiv, prob));
			runStandard(false, uuids, containedUuids, BloomFilter.create(funnel, unique/intoFilterDiv));
		}
		
//		System.gc();
		log("Warmup Complete");
		runUnsafe(true, uuids, containedUuids, UnsafeBloomFilter.create(funnel, unique/intoFilterDiv, prob));
		runStandard(true, uuids, containedUuids, BloomFilter.create(funnel, unique/intoFilterDiv, prob));
		log("Done");
//		log(UnsafeAdapter.printUnsafeMemoryStats());
	}
	
	
	private static void runUnsafe(boolean printResults, List<UUID> uuids, Set<UUID> containedUuids, UnsafeBloomFilter<UUID> filter) {
		for(UUID uuid: containedUuids) {
			filter.put(uuid);
		}
		int noMatch = 0;
		int maybeMatch = 0;
		ElapsedTime et = SystemClock.startClock();
		for(UUID u: uuids) {
			if(filter.mightContain(u)) {
				maybeMatch++;
			} else {
				noMatch++;
			}
		}
		
		if(printResults) {
			et = et.stopClock();
			log("UNSAFE: Nos: %s, Maybes: %s, Expected: %s, [%s]", noMatch, maybeMatch, containedUuids.size(), et.printAvg("rate", uuids.size()));			
		}
	}

	
	private static void runStandard(boolean printResults, List<UUID> uuids, Set<UUID> containedUuids, BloomFilter<UUID> filter) {
		for(UUID uuid: containedUuids) {
			filter.put(uuid);
		}		
		int noMatch = 0;
		int maybeMatch = 0;
		ElapsedTime et = SystemClock.startClock();
		for(UUID u: uuids) {
			if(filter.mightContain(u)) {
				maybeMatch++;
			} else {
				noMatch++;
			}
		}
		
		if(printResults) {
			et = et.stopClock();
			log("STANDARD: Nos: %s, Maybes: %s, Expected: %s, [%s]", noMatch, maybeMatch, containedUuids.size(), et.printAvg("rate", uuids.size()));			
		}		
	}

	
	public static void log(Object fmt, Object...args) {
		System.out.println(String.format(fmt.toString(), args));
	}

}
