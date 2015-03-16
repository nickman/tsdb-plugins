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
package com.heliosapm.tsdb.plugins.async;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import reactor.core.dispatch.wait.ParkWaitStrategy;
import reactor.jarjar.com.lmax.disruptor.BlockingWaitStrategy;
import reactor.jarjar.com.lmax.disruptor.BusySpinWaitStrategy;
import reactor.jarjar.com.lmax.disruptor.LiteBlockingWaitStrategy;
import reactor.jarjar.com.lmax.disruptor.PhasedBackoffWaitStrategy;
import reactor.jarjar.com.lmax.disruptor.SleepingWaitStrategy;
import reactor.jarjar.com.lmax.disruptor.TimeoutBlockingWaitStrategy;
import reactor.jarjar.com.lmax.disruptor.WaitStrategy;
import reactor.jarjar.com.lmax.disruptor.YieldingWaitStrategy;

/**
 * <p>Title: WaitStrategyFactories</p>
 * <p>Description: Enumerated wait strategy factories</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.tsdb.plugins.async.WaitStrategyFactories</code></p>
 */

public enum WaitStrategyFactories implements WaitStrategyFactory {
	BLOCK(new BlockingWaitStrategyFactory()),
	BUSY(new BusySpinWaitStrategyFactory()),
	LITEBLOCK(new LiteBlockingWaitStrategyFactory()),
	PARK(new ParkWaitStrategyFactory()),
	PHASED(new PhasedBackoffWaitStrategyFactory()),
	SLEEP(new SleepingWaitStrategyFactory()),
	TIMEOUT(new TimeoutBlockingWaitStrategyFactory()),
	YIELDING(new YieldingWaitStrategyFactory());
	
	private WaitStrategyFactories(final WaitStrategyFactory factory) {
		this.factory = factory;
	}
	
	private final WaitStrategyFactory factory;
	
	@Override
	public WaitStrategy create(final String... args) {		
		return factory.create(args);
	}
	
	public static Integer toIntOrNull(final int index, final String...args) {
		try { return Integer.parseInt(args[index].trim()); } catch (Exception x) {/* No Op */}
		return null;
	}

	
	public static Long toLongOrNull(final int index, final String...args) {
		try { return Long.parseLong(args[index].trim()); } catch (Exception x) {/* No Op */}
		return null;
	}
	
	public static long toLong(final int index, final String...args) {
		try { return Long.parseLong(args[index].trim()); } catch (Exception x) {/* No Op */}
		throw new IllegalArgumentException("Invalid LongValue in Args[" + index + "]:" + Arrays.toString(args));
	}
	
	
	public static WaitStrategyFactories toWSF(final int index, final String...args) {
		try { return WaitStrategyFactories.valueOf(args[index].trim().toUpperCase()); } catch (Exception x) {/* No Op */}
		throw new IllegalArgumentException("Invalid WaitStrategyFactories in Args[" + index + "]:" + Arrays.toString(args));
	}
	
	public static TimeUnit toTimeUnit(final int index, final String...args) {
		try { return TimeUnit.valueOf(args[index].trim().toUpperCase()); } catch (Exception x) {/* No Op */}
		throw new IllegalArgumentException("Invalid TimeUnit in Args[" + index + "]:" + Arrays.toString(args));
	}
	
	public static class BlockingWaitStrategyFactory implements WaitStrategyFactory {
		public WaitStrategy create(final String...args) {
			return new BlockingWaitStrategy();
		}
	}

	public static class YieldingWaitStrategyFactory implements WaitStrategyFactory {
		public WaitStrategy create(final String...args) {
			return new YieldingWaitStrategy();
		}
	}	
	
	public static class BusySpinWaitStrategyFactory implements WaitStrategyFactory {
		public WaitStrategy create(final String...args) {
			return new BusySpinWaitStrategy();
		}
	}

	public static class LiteBlockingWaitStrategyFactory implements WaitStrategyFactory {
		public WaitStrategy create(final String...args) {
			return new LiteBlockingWaitStrategy();
		}
	}
	
	public static class ParkWaitStrategyFactory implements WaitStrategyFactory {
		public WaitStrategy create(final String...args) {
			final Long parkFor = toLong(0, args);
			if(parkFor!=null && parkFor < 1) throw new IllegalArgumentException("Invalid parkFor time for ParkWaitStrategy [" + parkFor + "]");
			return parkFor==null ? new ParkWaitStrategy() : new ParkWaitStrategy(parkFor); 
		}
	}

	public static class PhasedBackoffWaitStrategyFactory implements WaitStrategyFactory {
		public WaitStrategy create(final String...args) {
			if(args==null || args.length < 4) throw new IllegalArgumentException("Insufficient arguments to create a PhasedBackoffWaitStrategy:" + Arrays.toString(args));
			final long spinTimeout = toLong(0, args);
			final long yieldTimeout = toLong(1, args);
			final TimeUnit unit  = toTimeUnit(2, args);
			final WaitStrategyFactories wsf = toWSF(3, args);
            final int wsfArgsLength = args.length-4;
            final String[] wsfArgs = new String[wsfArgsLength];
            if(wsfArgsLength>0) {
            	System.arraycopy(args, 4, wsfArgs, 0, wsfArgsLength);
            }
            final WaitStrategy fallbackStrategy = wsf.create(wsfArgs);
            return new PhasedBackoffWaitStrategy(spinTimeout, yieldTimeout, unit, fallbackStrategy);
		}
	}

	public static class SleepingWaitStrategyFactory implements WaitStrategyFactory {
		public WaitStrategy create(final String...args) {
			final Integer retries = toIntOrNull(0, args);
			if(retries != null && retries < 1) throw new IllegalArgumentException("Invalid retry count time for SleepingWaitStrategy [" + retries + "]");
			return retries==null ? new SleepingWaitStrategy() : new SleepingWaitStrategy(retries); 
		}
	}

	
	public static class TimeoutBlockingWaitStrategyFactory implements WaitStrategyFactory {
		public WaitStrategy create(final String...args) {
			final long timeout = toLong(0, args);
			final TimeUnit unit  = toTimeUnit(1, args);
			if(timeout < 1) throw new IllegalArgumentException("Invalid timeout time for TimeoutBlockingWaitStrategy [" + timeout + "]");
			return new TimeoutBlockingWaitStrategy(timeout, unit); 
		}
	}
	
}
