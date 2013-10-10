/**
 * Helios Development Group LLC, 2013
 */
package org.helios.tsdb.plugins.util;

import java.util.Arrays;

/**
 * <p>Title: ArrayUtils</p>
 * <p>Description: Some generic array utilities</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead 
 * <p><code>org.helios.tsdb.plugins.util.ArrayUtils</code></p>
 */

public class ArrayUtils {

	private static final long[][] EMPTY_LONG_ARR = new long[0][0];
	
	private static long[][] validateAndInit(long[][] source) {
		if(source==null) throw new IllegalArgumentException("Null array passed");
		int size = source.length;
		if(size==0) return EMPTY_LONG_ARR;
		int secondary = source[0].length;
		for(int i = 1; i < source.length; i++) {
			if(source[i].length != secondary) throw new IllegalArgumentException("Pivot not supported on uneven primitive arrays");
		}		
		return new long[secondary][source.length];
	}
	
	/**
	 * Pivots an array. e.g. an array like this:<pre>
		[33, 2, 3, 4, 5]
		[10, 9, 8, 7, 6]
	 * </pre> turns into an array like this:<pre>
		[33, 10]
		[2, 9]
		[3, 8]
		[4, 7]
		[5, 6]
	 * </pre>
	 * @param source The array to pivot
	 * @return The pivoted array
	 */
	public static long[][] pivot(long[][] source) {
		long[][] target = validateAndInit(source);
		for(int x = 0; x < target.length; x++) {			
			for(int y = 0; y < source.length; y++) {
				target[x][y] = source[y][x];
			}
		}
		return target;
	}
	
	
	
	/**
	 * Prints a formatted matrix from the passed array
	 * @param arr The array to print
	 * @return a matrix string
	 */
	public static String formatArray(long[][] arr) {
		if(arr==null) return "";
		if(arr.length==0) return "";
		StringBuilder b = new StringBuilder();
		for(int x = 0; x < arr.length; x++) {
			b.append(Arrays.toString(arr[x])).append("\n");
		}
		return b.toString();
		
	}
	

}
