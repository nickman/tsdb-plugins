/**
* Helios Development Group LLC, 2013. 
 *
 */
package org.helios.tsdb.plugins.util;



import java.beans.BeanInfo;
import java.beans.Introspector;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.RMISocketFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.DynamicMBean;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerFactory;
import javax.management.MBeanServerInvocationHandler;
import javax.management.MalformedObjectNameException;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.QueryExp;
import javax.management.openmbean.CompositeData;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;

/**
 * <p>Title: JMXHelper</p>
 * <p>Description: Static JMX Utility methods</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.jmx.JMXHelper</code></p>
 */
public class JMXHelper {
	/** The property name where the jmx default domain is referenced */
	public static final String JMX_DOMAIN_PROPERTY = "org.helios.jmx.domain";
	/** The default jmx default domain is referenced */
	public static final String JMX_DOMAIN_DEFAULT = System.getProperty(JMX_DOMAIN_PROPERTY, ManagementFactory.getPlatformMBeanServer().getDefaultDomain());
	/** Regex WildCard Support Pattern for ObjectName key values */
	public static final Pattern OBJECT_NAME_KP_WILDCARD = Pattern.compile("[:|,](\\S+?)~=\\[(\\S+?)\\]");
	
	/** An object name filter that maps to all registered MBeans */
	public static final ObjectName ALL_MBEANS_FILTER = objectName("*:*");

	
	/**
	 * Acquires the configured or default Helios target MBeanServer.
	 * @return An MBeanServer.
	 */
	public static MBeanServer getHeliosMBeanServer() {
		MBeanServer server = null;
		String jmxDomain = ConfigurationHelper.getEnvThenSystemProperty(JMX_DOMAIN_PROPERTY, null);
		if(jmxDomain!=null) {
			server = getLocalMBeanServer(jmxDomain, true);
		}
		if(server==null) {
			return ManagementFactory.getPlatformMBeanServer();
		}		
		return server;
	}
	
	/**
	 * Returns an array of matching ObjectNames
	 * @param server The MBeanServer to query
	 * @param pattern The ObjectName pattern
	 * @param query An optional query expression
	 * @return an array of ObjectNames
	 */
	public static ObjectName[] query(MBeanServerConnection server, ObjectName pattern, QueryExp query) {
		try {
			if(server==null) server = getHeliosMBeanServer();
			Set<ObjectName> ons = server.queryNames(pattern, query);
			return ons.toArray(new ObjectName[ons.size()]);
		} catch (Exception e) {
			throw new RuntimeException("Failed to issue MBean query", e);
		}
	}
	
	/**
	 * Returns an array of matching ObjectNames
	 * @param server The MBeanServer to query
	 * @param pattern The ObjectName pattern
	 * @param query An optional query expression
	 * @return an array of ObjectNames
	 */
	public static ObjectName[] query(MBeanServerConnection server, CharSequence pattern, QueryExp query) {
		return query(server, objectName(pattern), query);
	}
	
	/**
	 * Returns an array of matching ObjectNames
	 * @param server The MBeanServer to query
	 * @param pattern The ObjectName pattern
	 * @return an array of ObjectNames
	 */
	public static ObjectName[] query(MBeanServerConnection server, CharSequence pattern) {
		return query(server, objectName(pattern), null);
	}
	
	/**
	 * Returns an array of matching ObjectNames
	 * @param server The MBeanServer to query
	 * @param pattern The ObjectName pattern
	 * @return an array of ObjectNames
	 */
	public static ObjectName[] query(MBeanServerConnection server, ObjectName pattern) {
		return query(server, pattern, null);
	}
	

	
	
	
	/**
	 * Returns an array of matching ObjectNames from the default MBeanServer
	 * @param pattern The ObjectName pattern
	 * @param query An optional query expression
	 * @return an array of ObjectNames
	 */
	public static ObjectName[] query(ObjectName pattern, QueryExp query) {
		return query(getHeliosMBeanServer(), pattern, query);
	}
	
	/**
	 * Returns an array of matching ObjectNames from the default MBeanServer
	 * @param pattern The ObjectName pattern
	 * @param query An optional query expression
	 * @return an array of ObjectNames
	 */
	public static ObjectName[] query(CharSequence pattern, QueryExp query) {
		return query(getHeliosMBeanServer(), objectName(pattern), query);
	}
	
	
	/**
	 * Returns an array of matching ObjectNames from the default MBeanServer
	 * @param pattern The ObjectName pattern
	 * @return an array of ObjectNames
	 */
	public static ObjectName[] query(ObjectName pattern) {
		return query(getHeliosMBeanServer(), pattern, null);
	}
	
	/**
	 * Returns an array of matching ObjectNames from the default MBeanServer
	 * @param pattern The ObjectName pattern
	 * @return an array of ObjectNames
	 */
	public static ObjectName[] query(CharSequence pattern) {
		return query(getHeliosMBeanServer(), objectName(pattern), null);
	}
	
	/**
	 * Determines if the passed Object is or represents a JMX ObjectName
	 * @param obj the object to test
	 * @return true if the passed Object is or represents a JMX ObjectName, false otherwise
	 */
	public static boolean isObjectName(Object obj) {
		if(obj==null) return false;
		if(obj instanceof ObjectName) return true;
		try {
			new ObjectName(obj.toString());
			return true;
		} catch (Exception e) {
			return false;
		}
	}
	
	/**
	 * Determines if the passed ObjectName is registered in the passed MBeanServer
	 * @param conn The MBeanServer reference
	 * @param on The ObjectName to test for
	 * @return true if registered, false otherwise
	 */
	public static  boolean isRegistered(MBeanServerConnection conn, ObjectName on) {
		try {
			return conn.isRegistered(on);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	
	/**
	 * Determines if the passed ObjectName is registered in the passed MBeanServer
	 * @param conn The MBeanServer reference
	 * @param on The ObjectName to test for
	 * @return true if registered, false otherwise
	 */
	public static  boolean isRegistered(MBeanServerConnection conn, CharSequence on) {
		return isRegistered(conn, objectName(on));
	}
	
	
	/**
	 * Determines if the passed ObjectName is registered in the default MBeanServer
	 * @param on The ObjectName to test for
	 * @return true if registered, false otherwise
	 */
	public static  boolean isRegistered(ObjectName on) {
		return isRegistered(getHeliosMBeanServer(), on);
	}
	
	/**
	 * Determines if the passed ObjectName is registered in the default MBeanServer
	 * @param on The ObjectName to test for
	 * @return true if registered, false otherwise
	 */
	public static  boolean isRegistered(CharSequence on) {
		return isRegistered(objectName(on));
	}
	
	
	/**
	 * Returns an MBeanConnection for an in-vm MBeanServer that has the specified default domain.
	 * @param domain The default domain of the requested MBeanServer.
	 * @return The located MBeanServerConnection or null if one cannot be located. 
	 */
	public static MBeanServer getLocalMBeanServer(String domain) {
		return getLocalMBeanServer(domain, true);
	}
	
	/**
	 * Searches for a matching MBeanServer in the passed list of domains and returns the first located.
	 * If one cannot be located a null will be returned. 
	 * @param domains The default domain of the requested MBeanServer.
	 * @return The located MBeanServerConnection or null if one cannot be found.
	 */
	public static MBeanServer getLocalMBeanServer(String...domains) {
		return getLocalMBeanServer(true, domains);
	}
	
	/**
	 * Searches for a matching MBeanServer in the passed list of domains and returns the first located.
	 * If one cannot be located, returnNullIfNotFound will either cause a null to be returned, or a RuntimeException. 
	 * @param returnNullIfNotFound If true, returns a null if a matching MBeanServer cannot be found. Otherwise, throws a RuntimeException.
	 * @param domains The default domain of the requested MBeanServer.
	 * @return The located MBeanServerConnection or null if one cannot be found and returnNullIfNotFound is true.
	 */
	public static MBeanServer getLocalMBeanServer(boolean returnNullIfNotFound, String...domains) {
		MBeanServer server = null;
		StringBuilder buff = new StringBuilder();
		for(String domain: domains) {
			server = getLocalMBeanServer(domain);
			buff.append(domain).append(",");
			if(server!=null) return server;
		}
		if(returnNullIfNotFound) {
			return null;
		}
		throw new RuntimeException("No MBeanServer located for domains [" + buff.toString() + "]");
	}
	
	
	/**
	 * Returns an MBeanConnection for an in-vm MBeanServer that has the specified default domain.
	 * @param domain The default domain of the requested MBeanServer.
	 * @param returnNullIfNotFound If true, returns a null if a matching MBeanServer cannot be found. Otherwise, throws a RuntimeException. 
	 * @return The located MBeanServerConnection or null if one cannot be found and returnNullIfNotFound is true. 
	 */
	public static MBeanServer getLocalMBeanServer(String domain, boolean returnNullIfNotFound) {
		if(domain==null || domain.equals("") || domain.equalsIgnoreCase("DefaultDomain") || domain.equalsIgnoreCase("Default")) {
			return ManagementFactory.getPlatformMBeanServer();
		}
		List<MBeanServer> servers = MBeanServerFactory.findMBeanServer(null);
		for(MBeanServer server: servers) {
			if(server.getDefaultDomain().equals(domain)) return server;
		}
		if(returnNullIfNotFound) {
			return null;
		}
		throw new RuntimeException("No MBeanServer located for domain [" + domain + "]");
	}
	
	/**
	 * Acquires a connected JMX connection
	 * @param jmxUrl The JMXServiceURL of the service to connec to
	 * @return a JMXConnector
	 */
	public static JMXConnector getJMXConnection(CharSequence jmxUrl) {
		return getJMXConnection(jmxUrl, true, null);
	}
	
	
	/**
	 * Acquires a JMX connection
	 * @param jmxUrl The JMXServiceURL of the service to connec to
	 * @param connect If true, the returned connector will be connected
	 * @param environment a set of attributes to determine how the connection is made. Can be null.
	 * @return a JMXConnector
	 */
	public static JMXConnector getJMXConnection(CharSequence jmxUrl, boolean connect, Map<String,?> environment) {
		if(jmxUrl==null) throw new IllegalArgumentException("The passed JMXServiceURL was null", new Throwable());
		try {
			
			JMXConnector connector = JMXConnectorFactory.newJMXConnector(new JMXServiceURL(jmxUrl.toString().trim()), environment);
			if(connect) {
				connector.connect();
			}
			return connector;
		} catch (Exception e) {
			e.printStackTrace(System.err);
			throw new RuntimeException("Failed to acquire JMXConnection to [" + jmxUrl + "]", e);
		}
	}
	
	
	/**
	 * Creates a new JMX object name.
	 * @param on A string type representing the ObjectName string.
	 * @return an ObjectName the created ObjectName
	 */
	public static ObjectName objectName(CharSequence on) {
		try {
			return new ObjectName(on.toString().trim());
		} catch (Exception e) {
			throw new RuntimeException("Failed to create Object Name", e);
		}
	}
	
	/**
	 * Creates a new JMX object name.
	 * @param format The string format template 
	 * @param args The arguments to populate the template with
	 * @return an ObjectName the created ObjectName
	 */
	public static ObjectName objectName(String format, Object...args) {
		try {
			return new ObjectName(String.format(format.trim(), args));
		} catch (MalformedObjectNameException moex) {
			if(moex.getMessage().startsWith("Invalid character")) {
				for(int i = 0; i < args.length; i++) {
					args[i] = ObjectName.quote(args[i].toString());
				}
				return objectName(String.format(format.trim(), args));
			}
			throw new RuntimeException("Failed to create Object Name", moex);
		} catch (Exception e) {
			throw new RuntimeException("Failed to create Object Name", e);
		}		
	}
	

	
	
	/**
	 * Creates a new JMX object name.
	 * @param on An object representing the ObjectName
	 * @return an ObjectName the created ObjectName
	 */
	public static ObjectName objectName(Object on) {
		try {
			return new ObjectName(on.toString().trim());
		} catch (Exception e) {
			throw new RuntimeException("Failed to create Object Name", e);
		}
	}	
	
	/**
	 * Creates a new JMX object name by appending properties on the end of an existing name
	 * @param on An existing ObjectName
	 * @param props Appended properties in the for {@code key=value}
	 * @return an ObjectName the created ObjectName
	 */
	public static ObjectName objectName(ObjectName on, CharSequence...props) {
		StringBuilder b = new StringBuilder(on.toString());
		try {			
			if(props!=null) {
				for(CharSequence prop: props) {
					b.append(",").append(prop);
				}
			}
			return new ObjectName(b.toString());
		} catch (Exception e) {
			throw new RuntimeException("Failed to create Object Name from [" + b + "]", e);			 
		}
	}
	
	
	/**
	 * Creates a new JMX object name.
	 * @param domain A string type representing the ObjectName domain
	 * @param properties A hash table of the Object name's properties
	 * @return an ObjectName the created ObjectName
	 */
	public static ObjectName objectName(CharSequence domain, Hashtable<String, String> properties) {
		try {
			return new ObjectName(domain.toString(), properties);
		} catch (Exception e) {
			throw new RuntimeException("Failed to create Object Name", e);
		}
	}
	

//	/**
//	 * Creates a new JMX object name.
//	 * @param domain The ObjectName domain
//	 * @param nameValuePairs an (even lengthed) array of name value pairs making up the key properties
//	 * @return an ObjectName the created ObjectName
//	 */
//	public static ObjectName objectName(CharSequence domain, CharSequence...nameValuePairs) {
//		if(domain==null || domain.toString().length()<1) throw new IllegalArgumentException("Null or zero length domain name");
//		if(nameValuePairs==null || nameValuePairs.length<1 || nameValuePairs.length%2!=0) {
//			throw new IllegalArgumentException("Invalid number of namevaluepairs [" + (nameValuePairs==null ? 0 : nameValuePairs.length) + "]");
//		}
//		try {
//			Hashtable<String, String> props = new Hashtable<String, String>();
//			for(int i = 0; i < nameValuePairs.length; i++) {
//				if(nameValuePairs[i]==null || nameValuePairs[i].toString().length()<1) {
//					throw new IllegalArgumentException("Null or blank nameValuePair entry at index [" + i + "]");
//				}
//				String key = nameValuePairs[i].toString();
//				i++;
//				if(nameValuePairs[i]==null || nameValuePairs[i].toString().length()<1) {
//					throw new IllegalArgumentException("Null or blank nameValuePair entry at index [" + i + "]");
//				}				
//				String value = nameValuePairs[i].toString();
//				props.put(key, value);
//			}
//			return new ObjectName(domain.toString(), props);
//		} catch (IllegalArgumentException iae) {
//			throw iae;
//		} catch (Exception e) {
//			throw new RuntimeException("Failed to create Object Name", e);
//		}
//	}
	
	/**
	 * Registers an MBean
	 * @param server The MBeanServer to register in
	 * @param objectName The ObjectName of the MBean
	 * @param mbean The MBean object instance to register
	 */
	public static void registerMBean(MBeanServer server, ObjectName objectName, Object mbean) {
		try {
			server.registerMBean(mbean, objectName);
		} catch(Exception e) {
			//throw new RuntimeException("Failed to register MBean [" + objectName + "]", e);
			System.err.println("Failed to register MBean [" + objectName + "]");
		}
	}
	
	/**
	 * Registers an MBean in the helios MBeanServer
	 * @param objectName The ObjectName of the MBean
	 * @param mbean The MBean object instance to register
	 */
	public static void registerMBean(ObjectName objectName, Object mbean) {
		registerMBean(getHeliosMBeanServer(), objectName, mbean);
	}
	
	/**
	 * Unregisters the named MBean from the passed MBeanServer
	 * @param server The MBeanServer to unregister from
	 * @param objectName The ObjectName of the MBean to unregister
	 */
	public static void unregisterMBean(MBeanServer server, ObjectName objectName) {
		try {
			server.unregisterMBean(objectName);
		} catch(Exception e) {
			//throw new RuntimeException("Failed to register MBean [" + objectName + "]", e);
			System.err.println("Failed to unregister MBean [" + objectName + "]");
		}		
	}
	
	/**
	 * Registers the named MBean in the passed MBeanServer
	 * @param server The MBeanServer to register with
	 * @param mbean The object to register
	 * @param objectName The ObjectName of the MBean to register
	 */
	public static void registerMBean(MBeanServer server, Object mbean, ObjectName objectName) {
		try {
			server.registerMBean(mbean, objectName);
		} catch(Exception e) {
			throw new RuntimeException("Failed to register MBean [" + objectName + "]", e);
			//System.err.println("Failed to register MBean [" + objectName + "]:" + e);
		}		
	}
	
	/**
	 * Registers the named MBean in the helios MBeanServer
	 * @param mbean The object to register
	 * @param objectName The ObjectName of the MBean to register
	 */
	public static void registerMBean(Object mbean, ObjectName objectName) {
		registerMBean(getHeliosMBeanServer(), mbean, objectName);
	}
	
	
	
	/**
	 * Unregisters the named MBean from the Helios MBeanServer
	 * @param objectName The ObjectName of the MBean to unregister
	 */
	public static void unregisterMBean(ObjectName objectName) {
		unregisterMBean(getHeliosMBeanServer(), objectName);
	}
	
	
	/**
	 * Retrieves MBeanInfo on the specified object name.
	 * @param server The mbean server
	 * @param on The object name
	 * @return an MBeanInfo
	 */
	public static MBeanInfo mbeanInfo(MBeanServerConnection server, CharSequence on) {
		try {
			return server.getMBeanInfo(objectName(on));
		} catch (Exception e) {
			throw new RuntimeException("Failed to get MBeanInfo", e);
		}		
	}
	
	/**
	 * Sets an MBean attribute.
	 * @param on The object name
	 * @param server The mbean server
	 * @param name The attribute name
	 * @param value The attribute value
	 */
	public static void setAttribute(CharSequence on, MBeanServerConnection server, String name, Object value) {
		try {
			server.setAttribute(objectName(on), new Attribute(name, value));
		} catch (Exception e) {
			throw new RuntimeException("Failed to set Attribute", e);
		}				
	}
	
	
	/**
	 * Sets a list of MBean attributes. Throws no exceptions. Returns a map of successfully set values.
	 * @param on on the object name
	 * @param server the mbean server
	 * @param attributes The attributes to set
	 * @return a map of successfully set values
	 */
	public static Map<String, Object> setAttributesWithRet(CharSequence on, MBeanServerConnection server, Object...attributes) {
		Map<String, Object> returnValues = new HashMap<String, Object>(attributes.length);		
		Collection<NVP> list = NVP.generate(attributes);
		for(NVP nvp: list) {
			try {
				setAttribute(on, server, nvp.getName(), nvp.getValue());
				returnValues.put(nvp.getName(), nvp.getValue());
			} catch (Exception e) {/* No Op */}
		}
		return returnValues;
	}
	
	/**
	 * Returns a String->Object Map of the named attributes from the Mbean.
	 * @param on The object name of the MBean.
	 * @param server The MBeanServerConnection the MBean is registered in. If this is null, uses the helios mbean server
	 * @param attributes An array of attribute names to retrieve. If this is null or empty, retrieves all the names
	 * @return A name value map of the requested attributes.
	 */
	public static Map<String, Object> getAttributes(ObjectName on, MBeanServerConnection server, String...attributes) {
		try {
			if(attributes==null || attributes.length<1) {
				attributes = getAttributeNames(on, server);
			}
			Map<String, Object> attrs = new HashMap<String, Object>(attributes.length);
			AttributeList attributeList = server.getAttributes(on, attributes);
			
			
			for(int i = 0; i < attributeList.size(); i++) {
				Attribute at = (Attribute)attributeList.get(i);
				attrs.put(at.getName(), at.getValue());
			}
			return attrs;
		} catch (Exception e) {
			throw new RuntimeException("Failed to getAttributes on [" + on + "]", e);
		}
	}
	
	/**
	 * Returns a String->Object Map of the named attributes from the Mbean in the helios mbeanserver
	 * @param on The object name of the MBean.
	 * @param attributes An array of attribute names to retrieve. If this is null or empty, retrieves all the names
	 * @return A name value map of the requested attributes.
	 */
	public static Map<String, Object> getAttributes(ObjectName on, String...attributes) {
		return getAttributes(on, getHeliosMBeanServer(), attributes);
	}
	
	
	/**
	 * Returns an array of the names of the attributes for the passed ObjectName reached through the helios mbeanserver
	 * @param objectName The mbean to get the attribute names for
	 * @return an array of strings
	 */
	public static String[] getAttributeNames(ObjectName objectName) {
		return getAttributeNames(objectName, getHeliosMBeanServer());
	}
	
	
	/**
	 * Returns an array of the names of the attributes for the passed ObjectName reached through the passed mbean server connection
	 * @param objectName The mbean to get the attribute names for
	 * @param connection The connection to reach the mbean through. If null, uses the helios mbean server
	 * @return an array of strings
	 */
	public static String[] getAttributeNames(ObjectName objectName, MBeanServerConnection connection) {
		if(objectName==null) throw new IllegalArgumentException("The passed objectname was null", new Throwable());
		if(connection==null) connection = getHeliosMBeanServer();		
		try {
			MBeanAttributeInfo[] infos = connection.getMBeanInfo(objectName).getAttributes();
			String[] names = new String[infos.length];
			for(int i = 0; i < infos.length; i++) {
				names[i] = infos[i].getName();
			}
			return names;
		} catch (Exception ex) {
			return new String[0];
		}
	}
	
	/**
	 * Inspects the array to see if it contains the passed string.
	 * @param name The name to search for
	 * @param array The array to search
	 * @return true if the array contains the passed string.
	 */
	public static boolean isIn(String name, String[] array) {
		if(array==null || name==null) return false;
		return Arrays.binarySearch(array, name)>=0; 
	}
	
	
	/**
	 * Sets a list of MBean attributes. Throws an exception on any failure. Returns a map of successfully set values.
	 * @param on the object name
	 * @param server the mbean server
	 * @param attributes The attributes to set
	 * @return a map of successfully set values.
	 */
	public static Map<String, Object> setAttributes(CharSequence on, MBeanServerConnection server, Object...attributes) {
		Map<String, Object> returnValues = new HashMap<String, Object>(attributes.length);		
		Collection<NVP> list = NVP.generate(attributes);
		for(NVP nvp: list) {
			setAttribute(on, server, nvp.getName(), nvp.getValue());
			returnValues.put(nvp.getName(), nvp.getValue());
		}
		return returnValues;
	}
	
	/**
	 * Gets an attribute value from an mbean.
	 * @param on on the object name
	 * @param server the mbean server
	 * @param name the name of the attribute
	 * @return the value of the attribute
	 */
	public static Object getAttribute(ObjectName on, MBeanServerConnection server, String name) {
		try {
			return server.getAttribute(on,name);
		} catch (Exception e) {
			throw new RuntimeException("Failed to get attribute", e);
		}
	}
	
	/**
	 * Invokes an operation on the mbean.
	 * @param on the object name
	 * @param server the mbean server
	 * @param action The name of the operation to invoke
	 * @param args The argument values to pass to the invocation
	 * @param signature The argument signature
	 * @return the return value of the invocation
	 */
	public static Object invoke(ObjectName on, MBeanServerConnection server, String action, Object[] args, String[] signature) {
		try {
			return server.invoke(on, action, args, signature);
		} catch (Exception e) {
			throw new RuntimeException("Failed to invoke operation", e);
		}
	}

	/**
	 * Invokes an operation on the mbean.
	 * @param on the object name
	 * @param server the mbean server
	 * @param action The name of the operation to invoke
	 * @param args The argument values to pass to the invocation
	 * @param signature The argument signature
	 * @return the return value of the invocation
	 */	
	public static Object invoke(CharSequence on, MBeanServerConnection server, String action, Object[] args, String[] signature) {
		return invoke(objectName(on), server, action, args, signature);
	}
	
	
	/**
	 * Returns a set of ObjectNames matching the passed wildcard object names
	 * @param wildcardEq The ObjectName equals
	 * @param wildcardWc The ObjectName wildcard
	 * @param conn The MBeanServer connection
	 * @return a set of ObjectNames matching the passed wildcard object name
	 */
	public static Set<ObjectName> getMatchingObjectNames(CharSequence wildcardEq, CharSequence wildcardWc, MBeanServerConnection conn) {
		ObjectName wildcardEquals = objectName(wildcardEq);
		ObjectName wildcard = objectName(wildcardWc);
		
		final String wc = new StringBuilder("(").append(wildcardEquals).append("$)").toString();
		Set<ObjectName> names = new HashSet<ObjectName>();
		// A map of regex patterns to match on, keyed by the actual property key
		Map<String, Pattern> wildcardQueryProps = new HashMap<String, Pattern>();
		// the original wildcard object's key properties
		Hashtable<String, String> wildcardProps = objectName(wildcard).getKeyPropertyList();
		// the non wildcarded property keys we will query the mbean server with
		Hashtable<String, String> queryProps = new Hashtable<String, String>();
		queryProps.putAll(wildcardProps);
		// Extract the wildcarded property keys, ie, where the key is KEY<wildcardEquals>
		for(Map.Entry<String, String> prop: wildcard.getKeyPropertyList().entrySet()) {
			if(prop.getKey().endsWith(wc)) {
				String actualKey = prop.getKey().replaceFirst(wc, "");
				wildcardQueryProps.put(actualKey, Pattern.compile(prop.getValue()));
				queryProps.remove(actualKey);
			}
		}
		// Build the lookup query
		StringBuilder b = new StringBuilder(wildcard.getDomain());
		b.append(":");
		// Append the non regex wildcarded properties
		for(Map.Entry<String, String> qp: queryProps.entrySet()) {
			b.append(qp.getKey()).append("=").append(qp.getValue()).append(",");
		}
		// Append the regex wildcarded property keys and "*" as the value
		for(String key: wildcardQueryProps.keySet()) {
			b.append(key).append("=*,");
		}
		// Append a property wild card if the wildcard objectName had:
		//	Property Pattern:true
		//	PropertyList Pattern:true
		//	PropertyValue Pattern:false
		if(wildcard.isPropertyPattern() && wildcard.isPropertyListPattern() && !wildcard.isPropertyValuePattern()) {
			b.append("*");
		}
		if(b.toString().endsWith(",")) {
			b.deleteCharAt(b.length()-1);
		}
		// Create the query object
		try {
			ObjectName queryObjectName = objectName(b);
			for(ObjectName qon: conn.queryNames(queryObjectName, null)) {
				boolean match = true;
				for(Map.Entry<String, Pattern> pattern: wildcardQueryProps.entrySet()) {
					match = pattern.getValue().matcher(qon.getKeyProperty(pattern.getKey())).matches();
					if(!match) break;
				}
				if(match) {
					names.add(qon);
				}
			}
		} catch (Exception e) {			
		}
		
		// Remove all the wildcarded properties from the wildcard objectname's props
		
		//ObjectName query = new ObjectName(wildcard.getDomain());
		
		
		return names;
	}
	
	/*
import java.util.regex.*;
import javax.management.*;

String value = "com.ecs.jms.destinations:service~=[A/B/C],type~=[Queue|Topic],*";
on =  new ObjectName(value);
println on.getKeyPropertyList();
Pattern p = Pattern.compile("[:|,](\\S+?)~=\\[(\\S+?)\\]");
Matcher m = p.matcher(value);
while(m.find()) {
    println "Group:${m.group(1)}";
    println "Group:${m.group(2)}";
}

	 */
	
	
	
//	/**
//	 * Returns an attribute map for the MBean with the passed object name registered in the passed server
//	 * @param server The MBeanServer where the target MBean is registered
//	 * @param objectName The object name of the target MBean. Should not be a pattern. For pattern lookups, use {@link JMXHelper#getMBeanAttributeMap(MBeanServerConnection, ObjectName, String, Collection)}.
//	 * @return A map of attribute values keyed by attribute name
//	 */
//	public static Map<String, Object> getMBeanAttributeMap(MBeanServerConnection server, ObjectName objectName) {
//		if(objectName==null) throw new IllegalArgumentException("The passed ObjectName was null", new Throwable());
//		if(objectName.isPattern()) throw new IllegalArgumentException("The passed ObjectName was a pattern. For pattern lookups, use {@link JMXHelper#getMBeanAttributeMap(MBeanServerConnection, ObjectName, String, Collection)}", new Throwable());
//		if(server==null) server = getHeliosMBeanServer();
//		MBean
//	}
	
	
	
	
	/**
	 * Retrieves maps of attribute values keyed by attribute name, in turn keyed by the ObjectName of the MBean.
	 * @param server An MBeanServerConnection
	 * @param objectName An ObjectName which can be absolute or a wildcard.
	 * @param delimeter The delimeter for composite type compound names
	 * @param attributeNames An array of absolute or compound attribute names.
	 * @return a map of results.
	 * TODO: TabularData
	 * TODO: Collections / Maps / Arrays --> ref by index
	 */
	public static Map<ObjectName, Map<String, Object>> getMBeanAttributeMap(MBeanServerConnection server, ObjectName objectName, String delimeter, String...attributeNames) {
		if(server==null) throw new RuntimeException("MBeanServerConnection was null", new Throwable());
		if(objectName==null) throw new RuntimeException("ObjectName was null", new Throwable());
		if(attributeNames==null || attributeNames.length<1) throw new RuntimeException("Attribute names array was null or zero length", new Throwable());
		String[] rootNames = new String[attributeNames.length];
		Map<String, String> compoundNames = new HashMap<String, String>();
		for(int i = 0; i < attributeNames.length; i++) {
			String rootKey = null;
			if(attributeNames[i].contains(delimeter)) {
				String[] fragments = attributeNames[i].split(Pattern.quote(delimeter));
				rootKey = fragments[0];
				compoundNames.put(rootKey, attributeNames[i]);
			} else {
				rootKey = attributeNames[i];
			}
			rootNames[i] = rootKey;
		}
		Map<ObjectName, Map<String, Object>> map = new HashMap<ObjectName, Map<String, Object>>();		
		try {
			for(ObjectName on: server.queryNames(objectName, null)) {
				AttributeList attrs = null;
				try {
					attrs = server.getAttributes(on, rootNames);
					if(attrs.size()<1) continue;
				} catch (Exception e) {
					continue;
				}
				Map<String, Object> attrMap = new HashMap<String, Object>();
				map.put(on, attrMap);
				for(Attribute attr: attrs.asList()) {
					Object value = attr.getValue();
					if(value==null) continue;
					String name = attr.getName();
					if(value instanceof CompositeData && compoundNames.containsKey(name)) {
						try {
							name = compoundNames.get(name);
							value = extractCompositeData((CompositeData)value, delimeter, name);
						} catch (Exception e) {
							continue;
						}
					}
					attrMap.put(name, value);
				}
			}
		} catch (Exception e) {
			throw new RuntimeException("Failed to acquire attribute names for ObjectName [" + objectName + "] for MBeanServer [" + server + "]", e);
		}
		
		return map;
	}
	
	/**
	 * Retrieves maps of attribute values keyed by attribute name, in turn keyed by the ObjectName of the MBean.
	 * @param server An MBeanServerConnection
	 * @param objectName An ObjectName which can be absolute or a wildcard.
	 * @param delimeter The delimeter for composite type compound names
	 * @param attributeNames An collection of absolute or compound attribute names.
	 * @return a map of results.
	 */
	public static Map<ObjectName, Map<String, Object>> getMBeanAttributeMap(MBeanServerConnection server, ObjectName objectName, String delimeter, Collection<String> attributeNames) {
		if(attributeNames==null || attributeNames.size()<1) throw new RuntimeException("Attribute names collection was null or zero size", new Throwable());
		return getMBeanAttributeMap(server, objectName, delimeter, attributeNames.toArray(new String[attributeNames.size()]));
	}
	
	/**
	 * Returns the MBeanInfo for the passed ObjectName from the specified MBeanServer
	 * @param server The MBeanServer to get the MBeanInfo from
	 * @param objectName The ObjectName of the MBean to get info for
	 * @return the MBeanInfo of the specified ObjectName
	 */
	public static MBeanInfo getMBeanInfo(MBeanServerConnection server, ObjectName objectName) {
		try {
			return server.getMBeanInfo(objectName);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to get MBeanInfo for [" + objectName + "]");
		}
	}
	
	/**
	 * Returns the MBeanInfo for the passed ObjectName from the helios MBeanServer
	 * @param objectName The ObjectName of the MBean to get info for
	 * @return the MBeanInfo of the specified ObjectName
	 */
	public static MBeanInfo getMBeanInfo(ObjectName objectName) {
		return getMBeanInfo(getHeliosMBeanServer(), objectName);
	}
	
	/**
	 * Returns the MBeanInfo for the passed ObjectName from the specified MBeanServer
	 * @param server The MBeanServer to get the MBeanInfo from
	 * @param objectName The ObjectName of the MBean to get info for
	 * @return the MBeanInfo of the specified ObjectName
	 */
	public static MBeanInfo getMBeanInfo(MBeanServerConnection server, CharSequence objectName) {
		return getMBeanInfo(server, objectName(objectName));
	}
	
	/**
	 * Returns the MBeanInfo for the passed ObjectName from the helios MBeanServer
	 * @param objectName The ObjectName of the MBean to get info for
	 * @return the MBeanInfo of the specified ObjectName
	 */
	public static MBeanInfo getMBeanInfo(CharSequence objectName) {
		return getMBeanInfo(getHeliosMBeanServer(), objectName(objectName));
	}

	
	/**
	 * Extracts a composite data field from a CompositeData instance using a compound name.
	 * @param cd The composite data instance
	 * @param delimeter The delimiter used for the compound name
	 * @param name The compound attribute name
	 * @return The extracted object
	 */
	public static Object extractCompositeData(final CompositeData cd, final String delimeter, final String name) {
		String[] fragments = name.split(Pattern.quote(delimeter));
		CompositeData ref = cd;
		Object value = null;
		for(int i = 1; i < fragments.length; i++) {
			value = ref.get(fragments[i]);
			if(value instanceof CompositeData) {
				ref = (CompositeData)value;
			} else {
				break;
			}
		}
		return value;
	}
	
	
	
	/**
	 * An regex pattern to parse A[X/Y/Z...] 
	 */
	public static final Pattern OBJECT_NAME_ATTR_PATTERN = Pattern.compile("(\\S+)\\[(\\S+)\\]"); 
	
	/**
	 * Retrieves the named attribute from the MBean with the passed ObjectName in the passed MBeanServerConnection.
	 * If the retrieval results in an exception or a null, the default value is returned 
	 * @param conn The MBeanServerConnection to the MBeanServer where the target MBean is registered
	 * @param objectName The ObjectName  of the target MBean
	 * @param attributeName The attribute name
	 * @param defaultValue The default value
	 * @return The attribute value or the defaut value
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getAttribute(MBeanServerConnection conn, ObjectName objectName, String attributeName, T defaultValue) {
		try {
			T t = (T)conn.getAttribute(objectName, attributeName);
			return t==null ? defaultValue : t;
		} catch (Exception e) {
			return defaultValue;
		}
	}
	
	/**
	 * Retrieves an attribute from an MBeanServer connection. 
	 * The compound name is in the format <b><code>&lt;ObjectName&gt;[&lt;Fragment<i>1</i>&gt;/&lt;Fragment<i>2</i>&gt;/&lt;Fragment<i>n</i>&gt;]</code></b>. 
	 * The multiple fragment names represent support for nested fields in a composite type.
	 * To retrieve a standard "flat" attribute, simply supply one fragment. 
	 * @param conn The MBeanServer connection
	 * @param compoundName The compound name
	 * @return the attribute value or null.
	 */
	public static Object getAttribute(MBeanServerConnection conn, CharSequence compoundName) {
		try {
			Matcher m = OBJECT_NAME_ATTR_PATTERN.matcher(compoundName);
			if(m.find()) {
				String objName = m.group(1);
				String[] fragments = m.group(2).split("/");
				return getAttribute(conn, objName, fragments);
			}
			return null;
		} catch (Exception e) { return null; }
	}
	
	/**
	 * Retrieves an attribute from an MBeanServer connection. 
	 * @param conn The MBeanServer connection
	 * @param objectName The ObjectName
	 * @param attrs the compound attribute name in the format <b><code>&lt;Fragment<i>1</i>&gt;/&lt;Fragment<i>2</i>&gt;/&lt;Fragment<i>n</i>&gt;</code></b>.
	 * @return the attribute value or null.
	 */
	public static Object getAttribute(MBeanServerConnection conn, String objectName, String...attrs) {
		return getAttribute(conn, objectName(objectName), attrs);
	}
	
	/**
	 * Retrieves an attribute from an MBeanServer connection with a default compound delimiter of <code>/</code> 
	 * @param conn The MBeanServer connection
	 * @param objectName The ObjectName
	 * @param attrs the compound attribute name in the format <b><code>&lt;Fragment<i>1</i>&gt;/&lt;Fragment<i>2</i>&gt;/&lt;Fragment<i>n</i>&gt;</code></b>.
	 * @return the attribute value or null.
	 */
	public static Object getAttribute(MBeanServerConnection conn, ObjectName objectName, String...attrs) {
		return getAttribute(conn, "/", objectName, attrs);
	}
	
	/**
	 * Retrieves an attribute from an MBeanServer connection. 
	 * @param conn The MBeanServer connection
	 * @param delimiter The compund opentype delimiter
	 * @param objectName The ObjectName
	 * @param attrs the compound attribute name in the format <b><code>&lt;Fragment<i>1</i>&gt;<b>DELIMITER</b>&lt;Fragment<i>2</i>&gt;<b>DELIMITER</b>&lt;Fragment<i>n</i>&gt;</code></b>.
	 * @return the attribute value or null.
	 */
	public static Object getAttribute(MBeanServerConnection conn, String delimiter, ObjectName objectName, String...attrs) {
		try {
			if(objectName!=null && attrs!=null && attrs.length > 0) {
				ObjectName on = objectName;
				String key = StringHelper.fastConcatAndDelim(delimiter, attrs);
				Map<ObjectName, Map<String, Object>> map = getMBeanAttributeMap(conn, on, delimiter, key);
				return map.get(on).get(key);
			}
		} catch (Exception e) {
		}
		return null;
	}
	
	/**
	 * Retrieves an attribute from an MBeanServer connection. 
	 * @param conn The MBeanServer connection
	 * @param delimiter The compund opentype delimiter
	 * @param objectName The ObjectName
	 * @param attrs the compound attribute name in the format <b><code>&lt;Fragment<i>1</i>&gt;<b>DELIMITER</b>&lt;Fragment<i>2</i>&gt;<b>DELIMITER</b>&lt;Fragment<i>n</i>&gt;</code></b>.
	 * @return the attribute value or null.
	 */
	public static Object getAttribute(MBeanServerConnection conn, String delimiter, String objectName, String...attrs) {
		return getAttribute(conn, delimiter, objectName(objectName), attrs);
	}
	
	
	/**
	 * Adds a listener to a registered MBean.
	 * @param connection The MBeanServer to register the listener with
	 * @param name The name of the MBean on which the listener should be added.
	 * @param listener The listener object which will handle the notifications emitted by the registered MBean.
	 * @param filter The filter object. If filter is null, no filtering will be performed before handling notifications.
	 * @param handback The context to be sent to the listener when a notification is emitted. 
	 */
	public static void addNotificationListener(MBeanServerConnection connection, ObjectName name, NotificationListener listener, NotificationFilter filter, Object handback) {
		try {
			connection.addNotificationListener(name, listener, filter, handback);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to register JMX Notification Listener", ex);
		}
	}
	
	/**
	 * Adds a listener to a registered MBean in the Helios MBeanServer
	 * @param name The name of the MBean on which the listener should be added.
	 * @param listener The listener object which will handle the notifications emitted by the registered MBean.
	 * @param filter The filter object. If filter is null, no filtering will be performed before handling notifications.
	 * @param handback The context to be sent to the listener when a notification is emitted. 
	 */
	public static void addNotificationListener(ObjectName name, NotificationListener listener, NotificationFilter filter, Object handback) {
		addNotificationListener(getHeliosMBeanServer(), name, listener, filter, handback);
	}
	
	/**
	 * Adds a listener to a registered MBean.
	 * @param connection The MBeanServer to register the listener with
	 * @param name The name of the MBean on which the listener should be added.
	 * @param listener The object name of the listener which will handle the notifications emitted by the registered MBean.
	 * @param filter The filter object. If filter is null, no filtering will be performed before handling notifications.
	 * @param handback The context to be sent to the listener when a notification is emitted. 
	 */
	public static void addNotificationListener(MBeanServerConnection connection, ObjectName name, ObjectName listener, NotificationFilter filter, Object handback) {
		try {
			connection.addNotificationListener(name, listener, filter, handback);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to register JMX Notification Listener", ex);
		}
	}

	/**
	 * Adds a listener to a registered MBean in the Helios MBeanServer
	 * @param name The name of the MBean on which the listener should be added.
	 * @param listener The object name of the listener which will handle the notifications emitted by the registered MBean.
	 * @param filter The filter object. If filter is null, no filtering will be performed before handling notifications.
	 * @param handback The context to be sent to the listener when a notification is emitted. 
	 */
	public static void addNotificationListener(ObjectName name, ObjectName listener, NotificationFilter filter, Object handback) {
		addNotificationListener(getHeliosMBeanServer(), name, listener, filter, handback);
	}
	
	
	/**
	 * Removes a notification listener
	 * @param connection The MBeanServer to remove the listener from
	 * @param name The ObjectName the listener was registered with
	 * @param listener The listener to remove
	 */
	public static void removeNotificationListener(MBeanServerConnection connection, ObjectName name, NotificationListener listener) {
		try {
			 connection.removeNotificationListener(name, listener);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to remove JMX notification listener", ex);
		}
	}
	
	/**
	 * Removes a notification listener from the Helios MBeanServer
	 * @param name The ObjectName the listener was registered with
	 * @param listener The listener to remove
	 */
	public static void removeNotificationListener(ObjectName name, NotificationListener listener) {
		removeNotificationListener(getHeliosMBeanServer(), name, listener);
	}
	

	/**
	 * Wrapped call to <code>java.beans.Introspector</code>.
	 * Impl. may be swapped out.
	 * @param pojo The object to get the bean info for.
	 * @return A BeanInfo instance.
	 */
	public static BeanInfo getBeanInfo(Object pojo) {
		try {
			return Introspector.getBeanInfo(pojo.getClass());
		} catch (Exception e) {
			throw new RuntimeException("Failed to create bean info", e);
		}
	}
	
	/**
	 * Reregisters mbeans from one MBeanServer to another.
	 * @param query An ObjectName mask.
	 * @param source The source MBeanServer
	 * @param target The target MBeanServer
	 * @return The number of MBeans susccessfully re-registered.
	 */
	public static int remapMBeans(ObjectName query, MBeanServer source, MBeanServer target) {
		int remaps = 0;
		Set<ObjectName> mbeans = target.queryNames(query, null);
		for(ObjectName on: mbeans) {
			try {
				Object proxy = MBeanServerInvocationHandler.newProxyInstance(source, on, DynamicMBean.class, true);
				target.registerMBean(proxy, on);
				remaps++;
			} catch (Exception e) {/* No Op */}
		}
		return remaps;
	}
	/**
	 * Creates, registers and starts a JMXConnectorServer
	 * @param bindInterface The interface to bind to
	 * @param serviceURL The JMXService URL
	 * @param server The MBeanServer to expose
	 */
	public static void fireUpJMXServer(final String bindInterface, final int serverSocketBacklog, CharSequence serviceURL, MBeanServer server) {
		try {
			fireUpJMXServer(bindInterface, serverSocketBacklog, new JMXServiceURL(serviceURL.toString()), server);
		} catch (Exception e) {
			throw new RuntimeException("Failed to start JMXServer on [" + serviceURL + "]", e);
		}
	}
	
	
	/**
	 * Creates, registers and starts a JMXConnectorServer
	 * @param bindInterface The interface to bind to
	 * @param serviceURL The JMXService URL
	 * @param server The MBeanServer to expose
	 */
	public static void fireUpJMXServer(final String bindInterface, final int serverSocketBacklog, JMXServiceURL serviceURL, MBeanServer server) {
		try {
			Map<String, Object> env = Collections.singletonMap("jmx.remote.rmi.server.socket.factory", (Object)new RMISocketFactory(){
				public ServerSocket createServerSocket(int port) throws IOException {
					return new ServerSocket(port, serverSocketBacklog, InetAddress.getByName(bindInterface));
				}
				public Socket createSocket(String host, int port) throws IOException {
					return new Socket(host, port);
				}
			});
			JMXConnectorServer jmxServer = JMXConnectorServerFactory.newJMXConnectorServer(serviceURL, env, server);
			server.registerMBean(jmxServer, JMXHelper.objectName("org.helios.netty:service=JMXConnectorServer,url=" + ObjectName.quote(serviceURL.toString())));
			jmxServer.start();
		} catch (Exception e) {
			throw new RuntimeException("Failed to start JMXServer on [" + serviceURL + "]", e);
		}
	}
	
	public static void fireUpRMIRegistry(final String bindInterface,  final int port)  {
		try {
			LocateRegistry.createRegistry(port);
		} catch (Exception e) {
			throw new RuntimeException("Failed to start RMIRegistry on [" + bindInterface + ":" + port + "]", e);
		}
	}
	
	/**
	 * Registers a new classloader MBean (an MLet) on the passed MBeanServer
	 * @param server The MBeanServer on which to register
	 * @param objectName The JMX object name of the new MBean
	 * @param delegateToCLR True if, when a class is not found in either the parent ClassLoader or the URLs, the MLet should delegate to its containing MBeanServer's ClassLoaderRepository.
	 * @param privateClassLoader If true, registers a private MLet, otherwise, registers a public one
	 * @param urls The URLs from which to load classes and resources.
	 * @return the ObjectName of the classloader
	 */
	public static ObjectName publishClassLoader(MBeanServerConnection server, CharSequence objectName, boolean delegateToCLR, boolean privateClassLoader, URL...urls) {
		ObjectName on = objectName(objectName);
		String className = privateClassLoader ? "javax.management.loading.PrivateMLet" : "javax.management.loading.MLet"; 
		try {
			server.createMBean(className, on, new Object[]{urls, delegateToCLR}, new String[]{URL[].class.getName(), "boolean"});
			return on;
		} catch (Exception ex) {
			throw new RuntimeException("Failed to register classloader MBean [" + objectName + "]", ex);
		}
	}
	
	/**
	 * Registers a new classloader MBean (an MLet) on the default MBeanServer
	 * @param objectName The JMX object name of the new MBean
	 * @param delegateToCLR True if, when a class is not found in either the parent ClassLoader or the URLs, the MLet should delegate to its containing MBeanServer's ClassLoaderRepository.
	 * @param privateClassLoader If true, registers a private MLet, otherwise, registers a public one
	 * @param urls The URLs from which to load classes and resources.
	 * @return the ObjectName of the classloader
	 */
	public static ObjectName publishClassLoader(CharSequence objectName, boolean delegateToCLR, boolean privateClassLoader, URL...urls) {
		return publishClassLoader(getHeliosMBeanServer(), objectName, delegateToCLR, privateClassLoader, urls);
	}
	
	/**
	 * Registers a new public classloader MBean (an MLet) on the default MBeanServer
	 * @param objectName The JMX object name of the new MBean
	 * @param delegateToCLR True if, when a class is not found in either the parent ClassLoader or the URLs, the MLet should delegate to its containing MBeanServer's ClassLoaderRepository.
	 * @param urls The URLs from which to load classes and resources.
	 * @return the ObjectName of the classloader
	 */
	public static ObjectName publishClassLoader(CharSequence objectName, boolean delegateToCLR, URL...urls) {
		return publishClassLoader(getHeliosMBeanServer(), objectName, delegateToCLR, false, urls);
	}
	
	/**
	 * Registers a new public and CLR delegating classloader MBean (an MLet) on the default MBeanServer
	 * @param objectName The JMX object name of the new MBean
	 * @param urls The URLs from which to load classes and resources.
	 * @return the ObjectName of the classloader 
	 */
	public static ObjectName publishClassLoader(CharSequence objectName, URL...urls) {
		return publishClassLoader(getHeliosMBeanServer(), objectName, true, false, urls);
	}
	
	
	
	
}


	

class NVP {
	String name = null;
	Object value = null;
	
	public static Collection<NVP> generate(Object...args) {
		List<NVP> list = new ArrayList<NVP>(args.length);
		String name = null;		
		for(int i=0; i<args.length; i++) {
			if(i+1 < args.length) {
				name=args[i].toString();
				i++;
				list.add(new NVP(name, args[i]));
			}
		}
		return list;
	}
	
	
	/**
	 * @param name The NVP name
	 * @param value The NVP value
	 */
	public NVP(String name, Object value) {
		super();
		this.name = name;
		this.value = value;
	}
	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}
	/**
	 * @return the value
	 */
	public Object getValue() {
		return value;
	}
	/**
	 * @param value the value to set
	 */
	public void setValue(Object value) {
		this.value = value;
	}
	
}
