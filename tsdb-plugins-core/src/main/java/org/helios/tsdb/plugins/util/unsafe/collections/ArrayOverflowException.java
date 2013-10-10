/** Helios Development Group LLC, 2013 */
package org.helios.tsdb.plugins.util.unsafe.collections;

/**
 * <p>Title: ArrayOverflowException</p>
 * <p>Description: Exception thrown when an unsafe array implementation cannot extend beyong the size of {@value Integer#MAX_VALUE}</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.collections.ArrayOverflowException</code></p>
 */
public class ArrayOverflowException extends RuntimeException {

	/**  */
	private static final long serialVersionUID = 5400947594380549746L;
	/**
	 * Creates a new ArrayOverflowException
	 * @param message The exception message
	 * @param cause The exception cause
	 */
	public ArrayOverflowException(String message, Throwable cause) {
		super(message, cause);
	}



}
