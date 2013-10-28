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
package test.net.opentsdb.search;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.nio.channels.FileChannel;

import org.apache.log4j.Logger;
import org.elasticsearch.bootstrap.ElasticSearch;
import org.helios.tsdb.plugins.shell.Search;
import org.helios.tsdb.plugins.test.BaseTest;
import org.helios.tsdb.plugins.util.URLHelper;
import org.junit.Ignore;

/**
 * <p>Title: ESBaseTest</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>test.net.opentsdb.search.ESBaseTest</code></p>
 */
@Ignore
public class ESBaseTest extends BaseTest {

	
	/** The embedded test ES home directory */
	public static File ES_HOME = null;
	/** The embedded test ES config directory */
	public static File ES_HOME_CONFIG = null;
	/** The embedded test ES log directory */
	public static File ES_HOME_LOG = null;
	
	/** Indicates if the ES instance is started */
	public static boolean ES_STARTED = false;
	/** The ES instance bootstrap thread */
	public static Thread ES_BOOT_THREAD = null;
	
	/** The sysprop defining the home dir for ES */
	public static final String ES_PATH_HOME_PROP = "es.path.home";
	/** The URL for the ES Status */
	public static final String ES_STATUS_URL = "http://localhost:8200/_status";
	
	/**
	 * Creates a search plugin jar for the {@link Search} shell plugin
	 */
	public static void createESSearchShellJar() {
		createPluginJar(Search.class);
	}

	
	public static void startEs() {
		try {
			File tmp = File.createTempFile("tsdb-es", "");
			tmp.delete();
			ES_HOME = new File(tmp.getAbsolutePath());
			ES_HOME.mkdir();
			ES_HOME_CONFIG = new File(ES_HOME.getAbsolutePath() + File.separator + "config");
			ES_HOME_CONFIG.mkdir();
			ES_HOME_LOG = new File(ES_HOME.getAbsolutePath() + File.separator + "logs");
			ES_HOME_LOG.mkdir();			
			log("ES HOME:[%s]", ES_HOME);
			copyFile("./src/test/java/es-config/home/config/elasticsearch.yml", ES_HOME_CONFIG.getAbsolutePath() + File.separator + "elasticsearch.yml");
			copyFile("./src/test/java/es-config/home/config/logging.yml", ES_HOME_CONFIG.getAbsolutePath() + File.separator + "logging.yml");
			System.setProperty(ES_PATH_HOME_PROP, ES_HOME.getAbsolutePath());
			log("Starting ES....");
			ES_BOOT_THREAD = new Thread("ES_BOOT_THREAD") {
				public void run() {
					ElasticSearch.main(new String[]{});
				}
			};
			ES_BOOT_THREAD.setDaemon(true);
			ES_BOOT_THREAD.start();
			URL statusURL = new URL(ES_STATUS_URL);
			for(int i = 0; i < 100; i++) {
				loge("Testing Up State (#%s)", i);
				try {
					String status = URLHelper.getTextFromURL(statusURL);
					if(status.contains("{\"ok\":true")) {
						System.setOut(new PrintStream(new FileOutputStream("/dev/stdout")));
						System.setErr(new PrintStream(new FileOutputStream("/dev/stderr")));
						loge("Status OK");
						ES_STARTED = true;
						break;
					}
				} catch (Exception ex) {	
					Thread.currentThread().join(100);
				}
			}
			if(!ES_STARTED) {
				throw new Exception("ES Failed to start");
			}
			log("ES Started");
			Thread.currentThread().join(10000);			
			ES_BOOT_THREAD.join();
			loge("ES Stopped");
			
		} catch (Exception ex) {
			throw new RuntimeException("Failed to boot ES test instance", ex);
		} finally {
			stopEs();
			cleanup();
		}
	}
	
	public static void stopEs() {
		ElasticSearch.close(new String[]{});		
	}
	
	public static void cleanup() {
		cleanDir(ES_HOME);
		ES_HOME.delete();
		log("Cleaned:  Files:[%s], Directories:[%s]", deletedFiles, deletedDirs);
	}
	
	private static int deletedFiles = 0;
	private static int deletedDirs = 0;
	
	public static void cleanDir(File dir) {
		log("Cleaning [%s]", dir);
		for(File f: dir.listFiles()) {
			if(f.isDirectory()) {
				cleanDir(f);
				f.delete();
				deletedDirs++;
			} else {
				f.delete();
				deletedFiles++;
			}
		}
	}
	
	public static void main(String[] args) {		
		startEs();
		//ES_HOME = new File("/tmp/tsdb-es4107370279287884684");
		//cleanup();
	}
	
	public static void copyFile(String source, String dest) {
		FileOutputStream fos = null; FileChannel fcOut = null;
		FileInputStream fis = null; FileChannel fcIn = null;
		try {
			new File(dest).delete();
			fis = new FileInputStream(source);
			fcIn = fis.getChannel();
			fos = new FileOutputStream(dest);
			fcOut = fos.getChannel();
			fcOut.transferFrom(fcIn, 0, fcIn.size());
			fcOut.force(true);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to copy [" + source + "] to [" + dest + "]", ex);
		} finally {
			try { fos.close(); } catch (Exception x) {}
			try { fcOut.close(); } catch (Exception x) {}
			try { fis.close(); } catch (Exception x) {}
			try { fcIn.close(); } catch (Exception x) {}
		}
	}
//	protected static final Logger LOG = Logger.getLogger("STDOUT");
//	protected static final Logger LOGE = Logger.getLogger("STDERR");
	
	/**
	 * Out printer
	 * @param fmt the message format
	 * @param args the message values
	 */
	public static void log(String fmt, Object...args) {
		OUT.println(String.format(fmt, args));
		//LOG.info(String.format(fmt, args));
	}
	
	/**
	 * Err printer
	 * @param fmt the message format
	 * @param args the message values
	 */
	public static void loge(String fmt, Object...args) {
		ERR.print(String.format(fmt, args));
		if(args!=null && args.length>0 && args[0] instanceof Throwable) {
			ERR.println("  Stack trace follows:");
			((Throwable)args[0]).printStackTrace(ERR);
//			LOG.error(String.format(fmt, args), (Throwable)args[0]);
		} else {
			ERR.println("");
//			LOG.error(String.format(fmt, args));
		}
		
	}	
}
