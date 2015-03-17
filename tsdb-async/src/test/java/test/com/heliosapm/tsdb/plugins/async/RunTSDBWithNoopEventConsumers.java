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
package test.com.heliosapm.tsdb.plugins.async;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.management.ManagementFactory;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;

import net.opentsdb.search.SearchPlugin;
import net.opentsdb.tsd.RTPublisher;

import org.reactivestreams.Publisher;

import com.heliosapm.tsdb.plugins.async.DelegatingRTPublisher;
import com.heliosapm.tsdb.plugins.async.DelegatingSearchPlugin;

/**
 * <p>Title: RunTSDBWithNoopEventConsumers</p>
 * <p>Description: Runs an OpenTSDB configured with a No Op RTPublisher and SearchPlugin.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>test.com.heliosapm.tsdb.plugins.async.RunTSDBWithNoopEventConsumers</code></p>
 */

public class RunTSDBWithNoopEventConsumers {
	
	/** A set of files to be deleted after each test */
	protected static final Set<File> TO_BE_DELETED = new CopyOnWriteArraySet<File>();
	
	/** Temp directory for fake plugin jars */
	public static final String TMP_PLUGIN_DIR = "./tmp-plugins";
	
	static {
		File f = new File(TMP_PLUGIN_DIR);
		if(f.exists() && !f.isDirectory()) {
			throw new RuntimeException("Temp plugin directory cannot be created [" + f.getAbsolutePath() + "]");
		}
		if(!f.exists()) {
			if(!f.mkdir()) {
				throw new RuntimeException("Failed to create Temp plugin directory [" + f.getAbsolutePath() + "]");
			}
		}
	}
	

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		log("RunTSDBWithNoopEventConsumers Test");
		System.setProperty("tools-level", "DEBUG");
		File searchPluginJar = createPluginJar(DelegatingSearchPlugin.class);
		File publisherPluginJar = createPluginJar(DelegatingRTPublisher.class);
		addConfigProp("tsdb.asyncprocessor.consumers", "test.com.heliosapm.tsdb.plugins.async.EventCountingTSDBEventConsumer");
		final String[] tsdbArgs = new String[]{
			"tsd",
			"--auto-metric",
			"--realtime-ts",
			"--realtime-uid",
			"--tsuid-incr",
			"--tsuid-tracking",
			"--plugin-path ", TMP_PLUGIN_DIR,
			"--rtplublisher",
			"--rtplublisher-plugin ", DelegatingRTPublisher.class.getName(),
			"--search",
			"--search-plugin ",  DelegatingSearchPlugin.class.getName(),
			"--fixdups",
			"--ignore-existing-pid",
			configFile==null ? "" : "--config",
			configFile==null ? "" : configFile.getAbsolutePath()
		};
		// tsdb.asyncprocessor.consumers
		startJMXMP();
		log("Starting OpenTSDB.....");
		net.opentsdb.tools.Main.main(tsdbArgs);
	}
	
	private static volatile File configFile = null;
	
	public static void addConfigProp(final String key, final String value) {
		if(configFile==null) {
			try {
				configFile = File.createTempFile("tsdb-", ".conf");
				configFile.deleteOnExit();
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		}
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(configFile, true);
			fos.write((key + "=" + value + "\n").getBytes());
			fos.flush();
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		} finally {
			if(fos!=null) try { fos.close(); } catch (Exception x) {/* No Op */}
		}
	}
	
	public static void startJMXMP() {
		try {
			final JMXServiceURL surl = new JMXServiceURL("service:jmx:jmxmp://0.0.0.0:8456");
			final JMXConnectorServer server = JMXConnectorServerFactory.newJMXConnectorServer(surl, null, ManagementFactory.getPlatformMBeanServer());
			Thread t = new Thread() {
				public void run() {
					try {
						server.start();
						log("JMXMP Server Started on [%s]", surl);
					} catch (Exception ex) {
						ex.printStackTrace(System.err);
					}
				}
			};
			t.setDaemon(true);
			t.start();
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		}
	}
	
	/**
	 * Formatted std-out logger
	 * @param fmt The message formatter
	 * @param args The message tokens
	 */
	public static void log(final Object fmt, final Object...args) {
		System.out.println(String.format(fmt.toString(), args));
	}
	
	
	
	
	
	/**
	 * Creates a search plugin jar for the {@link Search} shell plugin
	 */
	private static void createSearchShellJar() {
		createPluginJar(DelegatingSearchPlugin.class);
	}
	
	/**
	 * Creates a search plugin jar for the {@link Publisher} shell plugin
	 */
	private static void createPublishShellJar() {
		createPluginJar(DelegatingRTPublisher.class);
	}
	
//	/**
//	 * Creates an RPC plugin jar for the {@link RpcService} shell plugin
//	 */
//	private static void createRPCShellJar() {
//		createPluginJar(RpcService.class);
//	}
	
	
	
	/**
	 * Creates a temp plugin jar in the plugin directory
	 * @param plugin The plugin class to install
	 */
	public static File createPluginJar(Class<?> plugin) {
		FileOutputStream fos = null;
		JarOutputStream jos = null;
		try {
			new File(TMP_PLUGIN_DIR).mkdirs();
			File jarFile = new File(TMP_PLUGIN_DIR + "/" + plugin.getSimpleName() + ".jar");
			log("Temp JAR File:" + jarFile.getAbsolutePath());
			TO_BE_DELETED.add(jarFile);
			//jarFile.deleteOnExit();		
			StringBuilder manifest = new StringBuilder();
			manifest.append("Manifest-Version: 1.0\n");
			ByteArrayInputStream bais = new ByteArrayInputStream(manifest.toString().getBytes());
			Manifest mf = new Manifest(bais);
			fos = new FileOutputStream(jarFile, false);
			jos = new JarOutputStream(fos, mf);
			if(SearchPlugin.class.isAssignableFrom(plugin)) {
				jos.putNextEntry(new ZipEntry("META-INF/services/net.opentsdb.search.SearchPlugin"));
				jos.write((plugin.getName() + "\n").getBytes());
				jos.flush();
				jos.closeEntry();				
			}
			if(RTPublisher.class.isAssignableFrom(plugin)) {
				jos.putNextEntry(new ZipEntry("META-INF/services/net.opentsdb.tsd.RTPublisher"));
				jos.write((plugin.getName() + "\n").getBytes());
				jos.flush();
				jos.closeEntry();				
			}
			jos.flush();
			jos.close();
			fos.flush();
			fos.close();
			return jarFile;
		} catch (Exception e) {
			throw new RuntimeException("Failed to Plugin Jar for [" + plugin.getName() + "]", e);
		} finally {
			if(fos!=null) try { fos.close(); } catch (Exception e) {}
		}		
	}
	
	

}
