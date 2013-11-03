package org.helios.tsdb.plugins.util;
/**
 * Helios Development Group LLC, 2013
 */


import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;

/**
 * <p>Title: URLHelper</p>
 * <p>Description: Static URL and URI helper methods.</p> 
 * @author Whitehead 
 * <p><code>com.heliosapm.jmx.instrumentor.URLHelper</code></p>
 */

public class URLHelper {

	/** Text line separator */
	public static final String EOL = System.getProperty("line.separator", "\n");
	
	/** The system property to retrieve the default client connect timeout in ms.  */
	public static final String DEFAULT_CONNECT_TO = "sun.net.client.defaultConnectTimeout";
	/** The system property to retrieve the default client read timeout in ms.  */
	public static final String DEFAULT_READ_TO = "sun.net.client.defaultReadTimeout";
	
	
	/**
	 * Returns the default URL connect timeout in ms,
	 * @return the connect timeout in ms,
	 */
	protected static int defaultConnectTimeout() {
		return Integer.parseInt(System.getProperty(DEFAULT_CONNECT_TO, "0"));
	}
	
	/**
	 * Returns the default URL read timeout in ms,
	 * @return the read timeout in ms,
	 */
	protected static int defaultReadTimeout() {
		return Integer.parseInt(System.getProperty(DEFAULT_READ_TO, "0"));
	}
	
	
	/**
	 * Tests the passed object for nullness. Throws an {@link IllegalArgumentException} if the object is null 
	 * @param t  The object to test
	 * @param message The message to associate with the exception, Ignored if null
	 * @return the object passed in if not null
	 */
	public static <T> T nvl(T t, String message) {
		if(t==null) throw new IllegalArgumentException(message!=null ? message : "Null parameter");
		return t;
	}
	
	/**
	 * Reads the content of a URL as text using the default connect and read timeouts.
	 * @param url The url to get the text from
	 * @return a string representing the text read from the passed URL
	 */
	public static String getTextFromURL(URL url) {
		return getTextFromURL(url, defaultConnectTimeout(), defaultReadTimeout());
	}
	
	/**
	 * Reads the content of a URL as text
	 * @param url The url to get the text from
	 * @param timeout The connect and read timeout in ms.
	 * @return a string representing the text read from the passed URL
	 */
	public static String getTextFromURL(URL url, int timeout) {
		return getTextFromURL(url, timeout, timeout);
	}
	
	/**
	 * Reads the content of a URL as text
	 * @param url The url to get the text from
	 * @param cTimeout The connect timeout in ms.
	 * @param rTimeout The read timeout in ms.
	 * @return a string representing the text read from the passed URL
	 */
	public static String getTextFromURL(URL url, int cTimeout, int rTimeout) {
		StringBuilder b = new StringBuilder();
		InputStreamReader isr = null;
		BufferedReader br = null;
		InputStream is = null;
		URLConnection connection = null;
		try {
			connection = url.openConnection();
			connection.setConnectTimeout(cTimeout);
			connection.setReadTimeout(rTimeout);
			connection.connect();
			is = connection.getInputStream();
			isr = new InputStreamReader(is);
			br = new BufferedReader(isr);
			String line = null;
			while((line=br.readLine())!=null) {
				b.append(line).append(EOL);
			}
			return b.toString();			
		} catch (Exception e) {
			throw new RuntimeException("Failed to read source of [" + url + "]", e);
		} finally {
			if(br!=null) try { br.close(); } catch (Exception e) {/* No Op */}
			if(isr!=null) try { isr.close(); } catch (Exception e) {/* No Op */}
			if(is!=null) try { is.close(); } catch (Exception e) {/* No Op */}
		}
	}
	
	/**
	 * Returns the URL for the passed file
	 * @param file the file to get the URL for
	 * @return a URL for the passed file
	 */
	public static URL toURL(File file) {
		try {
			return nvl(file, "Passed file was null").toURI().toURL();
		} catch (Exception e) {
			throw new RuntimeException("Failed to get URL for file [" + file + "]", e);
		}
	}
	
	/**
	 * Creates a URL from the passed string 
	 * @param urlStr A char sequence containing a URL representation
	 * @return a URL
	 */
	public static URL toURL(CharSequence urlStr) {
		try {
			return new URL(nvl(urlStr, "Passed string was null").toString());
		} catch (Exception e) {
			throw new RuntimeException("Failed to create URL from string [" + urlStr + "]", e);
		}
	}
	
	/**
	 * Creates a URI from the passed string 
	 * @param uriStr A char sequence containing a URI representation
	 * @return a URI
	 */
	public static URI toURI(CharSequence uriStr) {
		try {
			return new URI(nvl(uriStr, "Passed string was null").toString());
		} catch (Exception e) {
			throw new RuntimeException("Failed to create URL from string [" + uriStr + "]", e);
		}
	}
	
	
	
	/**
	 * Reads the content of a URL as a byte array
	 * @param url The url to get the bytes from
	 * @return a byte array representing the text read from the passed URL
	 */
	public static byte[] getBytesFromURL(URL url) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream(); 
		InputStream is = null;
		try {
			is = url.openStream();
			int bytesRead = 0;
			byte[] buffer = new byte[8092]; 
			while((bytesRead=is.read(buffer))!=-1) {
				baos.write(buffer, 0, bytesRead);
			}
			return baos.toByteArray();
		} catch (Exception e) {
			throw new RuntimeException("Failed to read source of [" + url + "]", e);
		} finally {
			if(is!=null) try { is.close(); } catch (Exception e) {/* No Op */}
		}
	}
	
	/**
	 * Returns the last modified time stamp for the passed URL
	 * @param url The URL to get the timestamp for
	 * @return the last modified time stamp for the passed URL
	 */
	public static long getLastModified(URL url) {
		URLConnection conn = null;
		try {
			conn = nvl(url, "Passed URL was null").openConnection();
			return conn.getLastModified();
		} catch (Exception e) {
			throw new RuntimeException("Failed to get LastModified for [" + url + "]", e);
		}
	}
	
	
	/**
	 * Determines if the passed string is a valid URL
	 * @param urlStr The URL string to test
	 * @return true if is valid, false if invalid or null
	 */
	@SuppressWarnings("unused")
	public static boolean isValidURL(CharSequence urlStr) {
		if(urlStr==null) return false;
		try {
			new URL(urlStr.toString());
			return true;
		} catch (Exception e) {
			return false;
		}
	}
	
	/**
	 * Determines if the passed URL resolves
	 * @param url The URL to test
	 * @return true if is resolves, false otherwise
	 */
	public static boolean resolves(URL url) {
		if(url==null) return false;
		InputStream is = null;
		try {
			is = url.openStream();
			return true;
		} catch (Exception e) {
			return false;
		} finally {
			if(is!=null) try { is.close(); } catch (Exception e) {/* No Op */}
		}
	}
	
	/**
	 * Determines if this URL represents a writable resource.
	 * For now, only <b><code>file:</code></b> protocol will return true 
	 * (if the file exists and is writable). 
	 * @param url The URL to test for writability
	 * @return true if this URL represents a writable resource, false otherwise.
	 */
	public static boolean isWritable(CharSequence url) {
		return isWritable(toURL(url));
	}
	
	/**
	 * Determines if this URL represents a writable resource.
	 * For now, only <b><code>file:</code></b> protocol will return true 
	 * (if the file exists and is writable). 
	 * @param url The URL to test for writability
	 * @return true if this URL represents a writable resource, false otherwise.
	 */
	public static boolean isWritable(URL url) {
		if(url==null) return false;
		if("file".equals(url.getProtocol())) {
			File file = new File(url.getFile());
			return file.exists() && file.isFile() && file.canWrite();
		}
		return false;
	}
	
	/**
	 * Writes the passed byte content to the URL origin.
	 * @param url The URL to write to
	 * @param content The content to write
	 * @param append true to append, false to replace
	 */
	public static void writeToURL(URL url, byte[] content, boolean append) {
		if(!isWritable(url)) throw new RuntimeException("The url [" + url + "] is not writable", new Throwable());
		if(content==null) throw new RuntimeException("The passed content was null", new Throwable());
		File file = new File(url.getFile());
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(file, append);
			fos.write(content);
			fos.flush();
			fos.close();
			fos = null;			
		} catch (IOException ioe) {
			throw new RuntimeException("Failed to write to the url [" + url + "].", ioe);
		} finally {
			if(fos!=null) {
				if(fos!=null) { 
					try { fos.flush(); } catch (Exception ex) {/* No Op */}
					try { fos.close(); } catch (Exception ex) {/* No Op */}
				}
			}
		}
	}
	
	/**
	 * Returns the extension of the passed URL's file
	 * @param url The URL to get the extension of
	 * @return the file extension, or null if the file has no extension
	 */
	public static String getExtension(URL url) {
		return getExtension(url, null);
	}
	
	/**
	 * Returns the extension of the passed URL's file
	 * @param url The URL to get the extension of
	 * @param defaultValue The default value to return if there is no extension
	 * @return the file extension, or the default value if the file has no extension
	 */
	public static String getExtension(URL url, String defaultValue) {
		if(url==null) throw new RuntimeException("The passed url was null", new Throwable());
		String file = url.getFile();
		if(file.lastIndexOf(".")==-1) {
			return defaultValue;
		}
		return file.substring(file.lastIndexOf(".")+1);
	}
	
	/**
	 * Returns the extension of the passed URL's file
	 * @param url The URL to get the extension of
	 * @return the file extension, or null if the file has no extension
	 */
	public static String getExtension(CharSequence url) {
		return getExtension(url, null);
	}
	
	/**
	 * Returns the extension of the passed URL's file
	 * @param url The URL to get the extension of
	 * @param defaultValue The default value to return if there is no extension
	 * @return the file extension, or the default value if the file has no extension
	 */
	public static String getExtension(CharSequence url, String defaultValue) {
		if(url==null) throw new RuntimeException("The passed url was null", new Throwable());
		return getExtension(toURL(url), defaultValue);
	}
	
	/**
	 * Returns the extension of the passed file
	 * @param f The file to get the extension of
	 * @return the file extension, or null if the file has no extension
	 */
	public static String getFileExtension(File f) {
		return getFileExtension(f, null);
	}
	
	
	/**
	 * Returns the extension of the passed file
	 * @param f The file to get the extension of
	 * @param defaultValue The default value to return if there is no extension
	 * @return the file extension, or the default value if the file has no extension
	 */
	public static String getFileExtension(File f, String defaultValue) {
		if(f==null) throw new RuntimeException("The passed file was null", new Throwable());
		return getExtension(toURL(f), defaultValue);		
	}
	
	/**
	 * Returns the extension of the passed file name
	 * @param f The file name to get the extension of
	 * @return the file extension, or null if the file has no extension
	 */
	public static String getFileExtension(String f) {
		return getFileExtension(f, null);
	}
	
	/**
	 * Returns the extension of the passed file name
	 * @param f The file name to get the extension of
	 * @param defaultValue The default value to return if there is no extension
	 * @return the file extension, or the default value if the file has no extension
	 */
	public static String getFileExtension(String f, String defaultValue) {
		if(f==null) throw new RuntimeException("The passed file was null", new Throwable());
		return getExtension(toURL(new File(f)), defaultValue);		
	}
	


}
