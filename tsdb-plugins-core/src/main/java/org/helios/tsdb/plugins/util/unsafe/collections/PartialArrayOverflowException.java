/** Helios Development Group LLC, 2013 */
package org.helios.tsdb.plugins.util.unsafe.collections;

/**
 * <p>Title: PartialArrayOverflowException</p>
 * <p>Description: Exception thrown when an addition of items to an array partially completes.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.collections.PartialArrayOverflowException</code></p>
 */

public class PartialArrayOverflowException extends ArrayOverflowException {
	/**  */
	private static final long serialVersionUID = 5930277580751072763L;
	/** The number of items successfully inserted */
	private final int succeeded;
	/**
	 * Creates a new PartialArrayOverflowException
	 * @param succeeded The number of items successfully inserted
	 * @param message The exception message
	 * @param cause The exception cause
	 */
	public PartialArrayOverflowException(int succeeded, String message, Throwable cause) {
		super(message, cause);
		this.succeeded = succeeded;
	}
	
	/**
	 * Returns the number of items successfully inserted
	 * @return the number of items successfully inserted
	 */
	public int getSucceeded() {
		return succeeded;
	}

}
