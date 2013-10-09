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
package org.helios.tsdb.plugins.util;

import java.net.URL;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

/**
 * <p>Title: VersionHelper</p>
 * <p>Description: Maven build version locator</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.util.VersionHelper</code></p>
 */

public class VersionHelper {

	/** The version template, populated with <b><code>ModuleName</code></b>, <b><code>Version</code></b> and <b><code>BuildDate</code></b>  */
	public static final String VERSION_TEMPLATE = "Helios Shorthand %s, v.%s, build-date:[%s]";

	/**
	 * Returns the module name
	 * @return the module name
	 */
	public static String getModuleName() {
		return getModuleName(VersionHelper.class);
	}

	
	/**
	 * Returns the module name
	 * @param clazz The class to derive the module name from
	 * @return the module name
	 */
	public static String getModuleName(Class<?> clazz) {
		String module = clazz.getPackage().getImplementationTitle();
		if(module==null) module = "Agent";
		return module;
	}
	
	/**
	 * Returns the version
	 * @return the version
	 */
	public static String getVersion() {
		return getVersion(VersionHelper.class);
	}
	
	/**
	 * Returns the version
	 * @param clazz The class to derive the version from
	 * @return the version
	 */
	public static String getVersion(Class<?> clazz) {
		String version = clazz.getPackage().getImplementationVersion();
		if(version==null) version = "Dev Version";
		return version;
	}
	
	/**
	 * Returns the build date
	 * @return the build date
	 */
	public static String getBuildDate() {
		return getBuildDate(VersionHelper.class);
	}
	
	
	/**
	 * Returns the build date
	 * @param clazz The class to derive the build date from
	 * @return the build date
	 */
	public static String getBuildDate(Class<?> clazz) {
		if(clazz==null) clazz = VersionHelper.class;
		String buildDate = null;
		JarInputStream is = null;
		try {
			URL url = clazz.getProtectionDomain().getCodeSource().getLocation();
			if(url==null) throw new Exception();
			is = new JarInputStream(url.openStream());
			Manifest mf = is.getManifest();
			Attributes mainAttributes = mf.getMainAttributes();
			log("Loaded [%s] manifest attributes", mainAttributes.size());
			StringBuilder b = new StringBuilder();
			for(Map.Entry<Object,Object> entry: mainAttributes.entrySet()) {
				b.append("\n\t[").append(entry.getKey()).append("]:[").append(entry.getValue()).append("]");
			}
			log(b.toString());
			buildDate = mf.getMainAttributes().getValue("Build-Date");
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		} finally {
			if(is!=null) try { is.close(); } catch (Exception ex) {/*NoOp*/}
		}
		if(buildDate==null) buildDate = "Unknown";
		return buildDate;
	}
	
	/**
	 * Simple out formatted logger
	 * @param fmt The format of the message
	 * @param args The message arguments
	 */
	public static void log(String fmt, Object...args) {
		System.out.println(String.format(fmt, args));
	}	
	
	/**
	 * Returns the full version banner
	 * @return the full version banner
	 */
	public static String getVersionBanner() {
		return getVersionBanner(VersionHelper.class);
	}
	
	/**
	 * Returns the full version banner
	 * @param clazz The class to derive the version banner from
	 * @return the full version banner
	 */
	public static String getVersionBanner(Class<?> clazz) {		
		return String.format(VERSION_TEMPLATE, getModuleName(clazz), getVersion(clazz), getBuildDate(clazz));
	}
	

}
