/**
 * 
 */
package org.helios.tsdb.plugins.remoting.json;

import org.jboss.netty.buffer.ChannelBuffer;

/**
 * <p>Title: ChannelBufferizable</p>
 * <p>Description: Marks a class as knowing how to convert itself to a {@link ChannelBuffer}.</p>
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><b><code>org.helios.tsdb.plugins.remoting.json.ChannelBufferizable</code></b>
 */

public interface ChannelBufferizable {
	/**
	 * Marshalls this object to a ChannelBuffer 
	 * @return a ChannelBuffer with this marshalled object
	 */
	public ChannelBuffer toChannelBuffer();
	
	/**
	 * Writes this object into the passed channel buffer
	 * @param buffer The channel buffer to write to
	 */
	public void write(ChannelBuffer buffer);
}
