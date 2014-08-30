package net.opentsdb.meta.api;
/**
 * <p>Title: Completion</p>
 * <p>Description: Defines a class that determines if a continuous MultiDeferred is complete or not</p>
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><b><code>net.opentsdb.meta.api.Completion</code></b>
 */

public interface Completion {
	/**
	 * Returns true if complete, false otherwise
	 * @return true if complete, false otherwise
	 */
	public boolean isComplete();
}
