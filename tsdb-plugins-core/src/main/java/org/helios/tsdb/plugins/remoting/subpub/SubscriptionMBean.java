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

import org.helios.jmx.metrics.ewma.DirectEWMAMBean;

/**
 * <p>Title: SubscriptionMBean</p>
 * <p>Description: MBean interface for Subscriptions</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.remoting.subpub.SubscriptionMBean</code></p>
 */

public interface SubscriptionMBean {
	/**
	 * Indicates if the passed message is a member of this subscription
	 * @param message The message to determine the membership of
	 * @return false if the message is definitely NOT a member of this subscription, true if it is a member.
	 */
	public boolean test(String message);
	
	/**
	 * Returns the total number of matched incoming messages
	 * @return the total number of matched incoming messages
	 */
	public long getMatches();
	
	/**
	 * Returns the the total number of dropped incoming messages
	 * @return the the total number of dropped incoming messages
	 */
	public long getDrops();
	
	
	/**
	 * Returns the current number of retained (inserted) patterns
	 * @return the current number of retained (inserted) patterns
	 */
	public int getSize();
	
	/**
	 * Returns the current number of subscribers
	 * @return the current number of subscribers
	 */
	public int getSubscriberCount();
	
	/**
	 * Returns the subscription Id
	 * @return the subscription Id
	 */	
	public long getSubscriptionId();
	
	/**
	 * Returns the probability that the subscription will erroneously return true for a message that has not actually been put in the BloomFilter.
	 * @return the error probability
	 */
	public double getErrorProbability();

	/**
	 * Returns the delta between the expected and actual error probability
	 * @return the delta between the expected and actual error probability
	 */
	public double getRelativeProbability();
	
	/**
	 * Returns a message describing the subscription and state
	 * @return a message describing the subscription and state
	 */
	public String getDescription();
	
	/**
	 * Returns the mean elapsed time for the filter match in ns.
	 * @return the mean elapsed time for the filter match in ns.
	 */
	public double getMeanMatch();
	
	/**
	 * Returns the rolling average elapsed time for the filter match in ns.
	 * @return the rolling average elapsed time for the filter match in ns.
	 */
	public double getAverageMatch();
	
	/**
	 * Returns the initial expected insertion count for the bloom filter
	 * @return the bloom filter initial expected insertion count
	 */
	public int getCapacity();



	
}
