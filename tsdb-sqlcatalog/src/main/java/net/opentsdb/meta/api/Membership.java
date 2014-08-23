/**
 * 
 */
package net.opentsdb.meta.api;

import javax.management.ObjectName;

import net.opentsdb.core.DataPoints;
import net.opentsdb.meta.Annotation;
import net.opentsdb.meta.TSMeta;
import net.opentsdb.meta.UIDMeta;

/**
 * <p>Title: Membership</p>
 * <p>Description: Static helper methods to help with establishing the membership of objects to a Metric Meta API expression</p>
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><b><code>net.opentsdb.meta.api.Membership</code></b>
 */

public class Membership {

	public static boolean isMemberOf(String expression, UIDMeta uidMeta) {
		
		return true;
	}
	
	public static boolean isMemberOf(String expression, TSMeta tsdMeta) {
		return true;
	}
	
	public static boolean isMemberOf(String expression, Annotation annotation) {
		return true;
	}
	
	public static boolean isMemberOf(String expression, DataPoints datapoints) {
		return true;
	}
	
	/**
	 * Validates and converts the expression to an ObjectName
	 * @param expression The metric membership expression to validate
	 * @return an ObjectName representing the metric membership  
	 */
	public static ObjectName validate(String expression) {
		if(expression==null || expression.trim().isEmpty()) throw new IllegalArgumentException("Expression was null or empty");
		try {
			return new ObjectName(expression.trim());
		} catch (Exception ex) {
			throw new IllegalArgumentException("Invalid metric expression [" + expression + "]", ex);
		}
	}
	
	
	

	
	private Membership() {}

}
