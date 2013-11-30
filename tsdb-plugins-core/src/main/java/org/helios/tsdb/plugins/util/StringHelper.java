/**
* Helios Development Group LLC, 2013. 
 *
 */
package org.helios.tsdb.plugins.util;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * <p>Title: StringHelper</p>
 * <p>Description: String helper utility class</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p>{@link #getConstructorDescriptor(Constructor)}, {@link #getMethodDescriptor(Method)} and {@link #getDescriptor(StringBuffer, Class)}
 * are copied from <b>ObjectWeb ASM</b> and were authored by Eric Bruneton and Chris Nokleberg.</p>
 * <p><code>org.helios.apmrouter.jmx.StringHelper</code></p>
 */
public class StringHelper {
	
	/** The ThreadMXBean */
	protected static final ThreadMXBean tmx = ManagementFactory.getThreadMXBean();
	
	
	/**
	 * Returns the descriptor for the passed member
	 * @param m The class member
	 * @return the member descriptor
	 */
	public static String getMemberDescriptor(final Member m) {
		if(m instanceof Method) {
			return getMethodDescriptor((Method)m);
		} else if(m instanceof Constructor) {
			return getConstructorDescriptor((Constructor)m);
		} else {
			return m.toString();
		}
	}
	
	
	   /**
     * Returns the descriptor corresponding to the given method.
     * @param m a {@link Method Method} object.
     * @return the descriptor of the given method.
     * (All credit to ObjectWeb ASM)
     * @author Eric Bruneton  
     * @author Chris Nokleberg
     */
    public static String getMethodDescriptor(final Method m) {
        Class<?>[] parameters = m.getParameterTypes();
        StringBuffer buf = new StringBuffer();
        buf.append('(');
        for (int i = 0; i < parameters.length; ++i) {
            getDescriptor(buf, parameters[i]);
        }
        buf.append(')');
        getDescriptor(buf, m.getReturnType());
        return buf.toString();
    }
    
    /**
     * Returns the descriptor corresponding to the given constructor.
     * @param c a {@link Constructor Constructor} object.
     * @return the descriptor of the given constructor.
     * (All credit to ObjectWeb ASM)
     * @author Eric Bruneton  
     * @author Chris Nokleberg
     */
    public static String getConstructorDescriptor(final Constructor<?> c) {
        Class<?>[] parameters = c.getParameterTypes();
        StringBuffer buf = new StringBuffer();
        buf.append('(');
        for (int i = 0; i < parameters.length; ++i) {
            getDescriptor(buf, parameters[i]);
        }
        return buf.append(")V").toString();
    }
    

    /**
     * Appends the descriptor of the given class to the given string buffer.
     * @param buf the string buffer to which the descriptor must be appended.
     * @param c the class whose descriptor must be computed.
     * (All credit to ObjectWeb ASM)
     * @author Eric Bruneton  
     * @author Chris Nokleberg
     */
    private static void getDescriptor(final StringBuffer buf, final Class<?> c) {
        Class<?> d = c;
        while (true) {
            if (d.isPrimitive()) {
                char car;
                if (d == Integer.TYPE) {
                    car = 'I';
                } else if (d == Void.TYPE) {
                    car = 'V';
                } else if (d == Boolean.TYPE) {
                    car = 'Z';
                } else if (d == Byte.TYPE) {
                    car = 'B';
                } else if (d == Character.TYPE) {
                    car = 'C';
                } else if (d == Short.TYPE) {
                    car = 'S';
                } else if (d == Double.TYPE) {
                    car = 'D';
                } else if (d == Float.TYPE) {
                    car = 'F';
                } else /* if (d == Long.TYPE) */{
                    car = 'J';
                }
                buf.append(car);
                return;
            } else if (d.isArray()) {
                buf.append('[');
                d = d.getComponentType();
            } else {
                buf.append('L');
                String name = d.getName();
                int len = name.length();
                for (int i = 0; i < len; ++i) {
                    char car = name.charAt(i);
                    buf.append(car == '.' ? '/' : car);
                }
                buf.append(';');
                return;
            }
        }
    }

	
	/**
	 * Caps the first letter in the passed string
	 * @param cs The string value to initcap
	 * @return the initcapped string
	 */
	public static String initCap(CharSequence cs) {
		char[] chars = cs.toString().trim().toCharArray();
		chars[0] = new String(new char[]{chars[0]}).toUpperCase().charAt(0);
		return new String(chars);
	}

	/**
	 * Returns a formatted string representing the thread identified by the passed id
	 * @param id The id of the thread
	 * @return the formatted message
	 */
	public static String formatThreadName(long id) {
		if(id<1) return "[Nobody]";
		ThreadInfo ti = tmx.getThreadInfo(id);		
		if(ti==null)  return String.format("No Such Thread [%s]", id);
		return String.format("[%s/%s]", ti.getThreadName(), ti.getThreadId());
	}
	
	
	/**
	 * Returns a formatted string presenting the passed elapsed time in the native nanos, microseconds, milliseconds and seconds.
	 * @param title The arbitrary name for the timing
	 * @param nanos The elapsed time in nanos
	 * @return the formatted message
	 */
	public static String reportTimes(String title, long nanos) {
		StringBuilder b = new StringBuilder(title).append(":  ");
		b.append(nanos).append( " ns.  ");
		b.append(TimeUnit.MICROSECONDS.convert(nanos, TimeUnit.NANOSECONDS)).append( " \u00b5s.  ");
		b.append(TimeUnit.MILLISECONDS.convert(nanos, TimeUnit.NANOSECONDS)).append( " ms.  ");
		b.append(TimeUnit.SECONDS.convert(nanos, TimeUnit.NANOSECONDS)).append( " s.");
		return b.toString();
	}
	
	/**
	 * Returns a formatted string presenting the average elapsed time 
	 * based on the passed time stamp and count of incidents in the native nanos, microseconds, milliseconds and seconds.
	 * @param title The arbitrary name for the timing
	 * @param nanos The elapsed time in nanos
	 * @param count The number of incidents, used for calculating an average
	 * @return the formatted message
	 */
	public static String reportAvgs(String title, long nanos, long count) {
		if(nanos==0 || count==0) return reportTimes(title, 0);
		return reportTimes(title, (nanos/count));
	}
	
	/**
	 * Returns a formatted string presenting the total and average elapsed time 
	 * based on the passed time stamp and count of incidents in the native nanos, microseconds, milliseconds and seconds.
	 * @param title The arbitrary name for the timing
	 * @param nanos The elapsed time in nanos
	 * @param count The number of incidents, used for calculating an average
	 * @return the formatted message
	 */
	public static String reportSummary(String title, long nanos, long count) {
		return reportTimes(title, nanos) + 
				"\n" +
				reportAvgs(title + "  AVGS", nanos, count);
	}


	/**
	 * Acquires and truncates the current thread's StringBuilder.
	 * @return A truncated string builder for use by the current thread.
	 */
	public static StringBuilder getStringBuilder() {
		return new StringBuilder();
	}	
	
	/**
	 * Escapes quote characters in the passed string
	 * @param s The string to esacape
	 * @return the escaped string
	 */
	public static String escapeQuotes(CharSequence s) {
		return s.toString().replace("\"", "\\\"");
	}
	
	/**
	 * Escapes json characters in the passed string
	 * @param s The string to esacape
	 * @return the escaped string
	 */
	public static String jsonEscape(CharSequence s) {
		return s.toString().replace("\"", "\\\"").replace("[", "\\[").replace("]", "\\]").replace("{", "\\{").replace("}", "\\}");
	}
	
	
	/**
	 * Acquires and truncates the current thread's StringBuilder.
	 * @param size the inited size of the stringbuilder
	 * @return A truncated string builder for use by the current thread.
	 */
	public static StringBuilder getStringBuilder(int size) {
		return new StringBuilder(size);
	}
	
	/**
	 * Concatenates all the passed strings
	 * @param args The strings to concatentate
	 * @return the concatentated string
	 */
	public static String fastConcat(CharSequence...args) {
		StringBuilder buff = getStringBuilder();
		for(CharSequence s: args) {
			if(s==null) continue;
			buff.append(s);
		}
		return buff.toString();
	}
	
	/**
	 * Accepts an array of strings and returns the array flattened into a single string, optionally delimeted.
	 * @param skipBlanks If true, blank or null items in the passed array will be skipped.
	 * @param delimeter The delimeter to insert between each item.
	 * @param args The string array to flatten
	 * @return the flattened string
	 */
	public static String fastConcatAndDelim(boolean skipBlanks, String delimeter, CharSequence...args) {
		StringBuilder buff = getStringBuilder();
		if(args!=null && args.length > 0) {			
			for(CharSequence s: args) {				
				if(!skipBlanks || (s!=null && s.length()>0)) {
					buff.append(s).append(delimeter);
				}
			}
			if(buff.length()>0) {
				buff.deleteCharAt(buff.length()-1);
			}
		}
		return buff.toString();
	}
	
	/**
	 * Accepts an array of strings and returns the array flattened into a single string, optionally delimeted.
	 * Blank or zero length items in the array will be skipped.
	 * @param delimeter The delimeter to insert between each item.
	 * @param args The string array to flatten
	 * @return the flattened string
	 */
	public static String fastConcatAndDelim(String delimeter, CharSequence...args) {
		return fastConcatAndDelim(true, delimeter, args);
	}
	
	/**
	 * Accepts an array of strings and returns the array flattened into a single string, optionally delimeted.
	 * @param skip Skip this many
	 * @param delimeter The delimeter
	 * @param args The strings to concat
	 * @return the resulting string
	 */
	public static String fastConcatAndDelim(int skip, String delimeter, CharSequence...args) {
		StringBuilder buff = getStringBuilder();
		int cnt = args.length - skip;
		int i = 0;
		for(; i < cnt; i++) {
			if(args[i] != null && args[i].length() > 0) {
				buff.append(args[i]).append(delimeter);
			}
		}
		StringBuilder b = buff.reverse();
		while(b.subSequence(0, delimeter.length()).equals(delimeter)) {
			b.delete(0, delimeter.length());
		}
		return b.reverse().toString();
	}
	
	/**
	 * Formats the stack trace of the passed throwable and generates a formatted string.
	 * @param t The throwable
	 * @return A string representing the stack trace.
	 */
	public static String formatStackTrace(Throwable t) {
		if(t==null) return "";
		StackTraceElement[] stacks = t.getStackTrace();
		StringBuilder b = new StringBuilder(stacks.length * 50);
		for(StackTraceElement ste: stacks) {
			b.append("\n\t").append(ste.toString());
		}
		return b.toString();
	}
	
	/**
	 * Formats the stack trace of the passed thread and generates a formatted string.
	 * @param t The thread
	 * @return A string representing the stack trace of the passed thread
	 */
	public static String formatStackTrace(Thread t) {
		if(t==null) return "";
		StackTraceElement[] stacks = t.getStackTrace();
		StringBuilder b = new StringBuilder(stacks.length * 50);
		for(StackTraceElement ste: stacks) {
			b.append("\n\t").append(ste.toString());
		}
		return b.toString();
	}	

}
