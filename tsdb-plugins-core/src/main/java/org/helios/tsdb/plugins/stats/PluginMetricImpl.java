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
package org.helios.tsdb.plugins.stats;

/**
 * <p>Title: PluginMetricImpl</p>
 * <p>Description: A concrete manifestation of the {@link PluginMetric} annotation.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>stats.PluginMetricImpl</code></p>
 */

public class PluginMetricImpl {
	private String category = "";
	private String displayName = "";
	private MetricType metricType = MetricType.GAUGE;
	private int persistPeriod = -1;
	private String persistPolicy = "";
	private String unit = "";
	
	/**
	 * Creates a new PluginMetricImpl from a {@link PluginMetric} annotation instance.
	 * @param pm the {@link PluginMetric} annotation instance.
	 */
	public PluginMetricImpl(PluginMetric pm) {
		category = pm.category();
		displayName = pm.displayName();
		metricType = pm.metricType();
		persistPeriod = pm.persistPeriod();
		persistPolicy = pm.persistPolicy();
		unit = pm.unit();
	}

	/**
	 * The category of this metric (ex. throughput, performance, utilization).
	 */
	public void setCategory(String category) {
		this.category = category;
	}

	/**
	 * The category of this metric (ex. throughput, performance, utilization).
	 */
	public String getCategory() {
		return this.category;
	}

	/**
	 * A display name for this metric.
	 */
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	/**
	 * A display name for this metric.
	 */
	public String getDisplayName() {
		return this.displayName;
	}

	/**
	 * A description of how this metric's values change over time.
	 */
	public void setMetricType(MetricType metricType) {
		this.metricType = metricType;
	}

	/**
	 * A description of how this metric's values change over time.
	 */
	public MetricType getMetricType() {
		return this.metricType;
	}

	/**
	 * The persist period for this metric.
	 */
	public void setPersistPeriod(int persistPeriod) {
		this.persistPeriod = persistPeriod;
	}

	/**
	 * The persist period for this metric.
	 */
	public int getPersistPeriod() {
		return this.persistPeriod;
	}

	/**
	 * The persist policy for this metric.
	 */
	public void setPersistPolicy(String persistPolicy) {
		this.persistPolicy = persistPolicy;
	}

	/**
	 * The persist policy for this metric.
	 */
	public String getPersistPolicy() {
		return this.persistPolicy;
	}

	/**
	 * The expected unit of measurement values.
	 */
	public void setUnit(String unit) {
		this.unit = unit;
	}

	/**
	 * The expected unit of measurement values.
	 */
	public String getUnit() {
		return this.unit;
	}


}
