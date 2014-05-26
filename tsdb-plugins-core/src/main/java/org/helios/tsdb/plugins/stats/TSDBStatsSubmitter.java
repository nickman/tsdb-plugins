/**
 * 
 */
package org.helios.tsdb.plugins.stats;

import java.util.Properties;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import net.opentsdb.core.TSDB;
import net.opentsdb.stats.StatsCollector;

import org.helios.jmx.util.helpers.StringHelper;
import org.helios.tsdb.plugins.rpc.AbstractRPCService;
import org.helios.tsdb.plugins.service.TSDBPluginServiceLoader;

/**
 * <p>Title: TSDBStatsSubmitter</p>
 * <p>Description: An RPC plugin for collecting stats from the TSDB instance and resubmitting them as actual metrics</p>
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><b><code>org.helios.tsdb.plugins.stats.TSDBStatsSubmitter</code></b>
 */

public class TSDBStatsSubmitter extends AbstractRPCService implements Runnable {
	/** The stats collection scheduler */
	protected ScheduledExecutorService scheduler = null;
	/** The schedule handle */
	protected ScheduledFuture<?> handle = null;
	
	protected TSDBStatsCollector statsCollector = new TSDBStatsCollector();
	
	/**
	 * Creates a new TSDBStatsSubmitter
	 * @param tsdb The TSDB instance
	 * @param config The configuration
	 */
	public TSDBStatsSubmitter(TSDB tsdb, Properties config) {		
		super(tsdb, config);
	}
	
	protected void startImpl() {
		scheduler = TSDBPluginServiceLoader.getLoaderInstance().getPluginContext().getResource("scheduler", ScheduledExecutorService.class);
		handle = scheduler.scheduleWithFixedDelay(this, 5000, 15000, TimeUnit.MILLISECONDS);
		log.info(StringHelper.banner("Started TSDBStatsSubmitter"));
	}
	
	protected void stopImpl() {
		if(handle!=null) {
			handle.cancel(true);
		}
	}

	@Override
	public void run() {
		tsdb.collectStats(statsCollector);
	}
	
	class TSDBStatsCollector extends StatsCollector {
		public TSDBStatsCollector() {
			super("");
		}		
		public void emit(String datapoint) {
			
		}
	}
}
