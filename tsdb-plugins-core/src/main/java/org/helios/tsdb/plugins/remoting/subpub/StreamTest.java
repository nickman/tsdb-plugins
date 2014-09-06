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
package org.helios.tsdb.plugins.remoting.subpub;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.helios.jmx.util.helpers.SystemClock;

import reactor.core.Environment;
import reactor.core.composable.Deferred;
import reactor.core.composable.Stream;
import reactor.core.composable.spec.Streams;
import reactor.function.Consumer;
import reactor.function.Function;
import reactor.function.Supplier;
import reactor.tuple.Tuple2;

/**
 * <p>Title: StreamTest</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.remoting.subpub.StreamTest</code></p>
 */

public class StreamTest {
	static final Environment ENV = new Environment();
	
	public static void main(String[] args) {
		final AtomicLong eventCounter = new AtomicLong(0L);
		final AtomicLong aggCounter = new AtomicLong(0L);
		try {			
			final Deferred<String, Stream<String>> def = Streams.<String>defer(ENV);
			
			def.compose()
			.reduce(new Function<Tuple2<String,Map<String, AtomicInteger>>, Map<String, AtomicInteger>>() {
				public Map<String,AtomicInteger> apply(Tuple2<String,Map<String,AtomicInteger>> t) {
					eventCounter.incrementAndGet();
					final String s = t.getT1();
					final Map<String,AtomicInteger> map = t.getT2();
					AtomicInteger i = map.get(s);
					if(i==null) {
						synchronized(map) {
							i = map.get(s);
							if(i==null) {
								i = new AtomicInteger(0);
								map.put(s, i);
							}
						}
					}
					i.incrementAndGet();
//					log(map);		// looks something like: {PAUL=1143, DICK=1133, HARRY=1170, ANDY=1209, TOM=1143, GEORGE=1129, JOHN=1111, RINGO=1146, IAN=1128, NEVILLE=1125}
					return map;
				}
			}, new Supplier<Map<String,AtomicInteger>>(){
				@Override
				public Map<String, AtomicInteger> get() {					
					return new ConcurrentHashMap<String,AtomicInteger>();
				}
			}, 10000)
			.window(1000)
			.consume(new Consumer<List<Map<String,AtomicInteger>>>() {
				public void accept(List<Map<String, AtomicInteger>> t) {
					log("Accepted " + t.size() + " maps.");
					for(Map<String,AtomicInteger> map: t) {
						for(AtomicInteger ai: map.values()) {
							aggCounter.addAndGet(ai.get());
						}
					}
					if(t==null || t.isEmpty()) {
						log("Empty");
					} else {
						
					}
					
					t.clear();
				}
			});
			final String[] txt = {"TOM", "DICK", "HARRY", "NEVILLE", "GEORGE", "PAUL", "JOHN", "RINGO", "ANDY", "IAN", "BILLY"};
			final Random r = new Random(System.currentTimeMillis());
			final int samples = txt.length-1;
			final long endTime = System.currentTimeMillis() + 20000;
			long cnt = 0;
			log("====== Starting ======");
			while(System.currentTimeMillis()<endTime) {
				def.accept(txt[Math.abs(r.nextInt(samples))]);
				cnt++;
				if(cnt%1000==0) {
//					SystemClock.sleep(1000);
//					log("Sent [" + cnt + "]");
				}
			}		
			log("Flushing...");
			def.flush();
			SystemClock.sleep(1000);
			log("====== Done ======");
			log("Expected: [" + cnt + "], Actual: [" + eventCounter + "], Aggregate: [" + aggCounter + "]");
		} finally {
			ENV.shutdown();
			log("Env Shutdown");
		}
	}
	
	public static void log(Object msg) {
		System.out.println(msg);
	}

}