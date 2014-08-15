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
package org.helios.tsdb.plugins.test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import net.opentsdb.core.TSDB;
import net.opentsdb.core.UniqueIdRegistry;
import net.opentsdb.meta.Annotation;
import net.opentsdb.meta.TSMeta;
import net.opentsdb.meta.UIDMeta;
import net.opentsdb.search.SearchPlugin;
import net.opentsdb.tsd.RTPublisher;
import net.opentsdb.tsd.RpcPlugin;
import net.opentsdb.uid.UniqueId;
import net.opentsdb.utils.Config;

import org.hbase.async.HBaseClient;
import org.helios.tsdb.plugins.datapoints.DataPoint;
import org.helios.tsdb.plugins.datapoints.LongDataPoint;
import org.helios.tsdb.plugins.handlers.impl.QueuedResultPublishEventHandler;
import org.helios.tsdb.plugins.handlers.impl.QueuedResultSearchEventHandler;
import org.helios.tsdb.plugins.service.TSDBPluginServiceLoader;
import org.helios.tsdb.plugins.shell.Publisher;
import org.helios.tsdb.plugins.shell.RpcService;
import org.helios.tsdb.plugins.shell.Search;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.rules.TestName;

import com.stumbleupon.async.Callback;
import com.stumbleupon.async.Deferred;

/**
 * <p>Title: BaseTest</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.test.BaseTest</code></p>
 */
@Ignore
public class BaseTest {
	/** The currently executing test name */
	@Rule public final TestName name = new TestName();
	/** A random value generator */
	protected static final Random RANDOM = new Random(System.currentTimeMillis());
	
	protected static final PrintStream OUT = System.out;
	protected static final PrintStream ERR = System.err;
	
	/** If true, we tear down the test TSDB after each test. Otherwise, it is torn down after each class */
	protected static boolean tearDownTSDBAfterTest = true;
	
	
	/** A shared testing scheduler */
	protected static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2, new ThreadFactory(){
		final AtomicInteger serial = new AtomicInteger();
		@Override
		public Thread newThread(Runnable r) {
			Thread t = new Thread(r, "BaseTestScheduler#" + serial.incrementAndGet());
			t.setDaemon(true);
			return t;
		}
	});
	
	/** Reflective access to the TSDB's RTPublisher */
	protected static Field sinkDataPointField;
	/** Reflective access to the TSDBEventDispatcher's reset method */
	protected static Method dispatcherResetMethod;
	/** Reflective access to the TSDBEventDispatcher's instance field */
	protected static Field dispatcherInstanceField;
	
	static {
		System.setProperty("tsd.plugins.disableStatsCollect", "true");
		try {
			sinkDataPointField = TSDB.class.getDeclaredField("rt_publisher");
			sinkDataPointField.setAccessible(true);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
		try {
			dispatcherInstanceField = TSDBPluginServiceLoader.class.getDeclaredField("instance");
			dispatcherInstanceField.setAccessible(true);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
		
		try {
			dispatcherResetMethod = TSDBPluginServiceLoader.class.getDeclaredMethod("reset");
			dispatcherResetMethod.setAccessible(true);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}		
	}
	
	/**
	 * Resets the ITSDBPluginService instance
	 */
	public static void resetEventDispatcher() {
		try {
			TSDBPluginServiceLoader instance = (TSDBPluginServiceLoader)dispatcherInstanceField.get(null);
			if(instance!=null) {
				dispatcherResetMethod.invoke(instance);
			}
		} catch (Exception ex) {
			throw new RuntimeException("Failed to reset event dispatcher", ex);
		}
	}
	
	/**
	 * Publishes a data point to the current test TSDB
	 * @param dp The data point to publish
	 */
	public void publishDataPoint(DataPoint dp) {
		try {
			RTPublisher pub = (RTPublisher)sinkDataPointField.get(tsdb);
			if(dp instanceof LongDataPoint) {
				pub.publishDataPoint(dp.metricName, dp.timestamp, dp.longValue(), dp.tags, dp.getKey().getBytes());
			} else {
				pub.publishDataPoint(dp.metricName, dp.timestamp, dp.doubleValue(), dp.tags, dp.getKey().getBytes());
			}
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	
	/**
	 * Returns a random positive long
	 * @return a random positive long
	 */
	protected static long nextPosLong() {
		return Math.abs(RANDOM.nextLong());
	}
	
	/**
	 * Returns a random positive double
	 * @return a random positive double
	 */
	protected static double nextPosDouble() {
		return Math.abs(RANDOM.nextDouble());
	}
	
	
	/**
	 * Returns a random positive int
	 * @return a random positive int
	 */
	protected static int nextPosInt() {
		return Math.abs(RANDOM.nextInt());
	}
	/**
	 * Returns a random positive int within the bound
	 * @param bound the bound on the random number to be returned. Must be positive. 
	 * @return a random positive int
	 */
	protected static int nextPosInt(int bound) {
		return Math.abs(RANDOM.nextInt(bound));
	}
	
	
	
	/**
	 * Prints the test name about to be executed
	 */
	@Before
	public void printTestName() {
		log("\n\t==================================\n\tRunning Test [" + name.getMethodName() + "]\n\t==================================\n");
	}
	
	/** The current test's TSDB */
	protected static TSDB tsdb = null;
	/** The uniqueid for tag keys */
	protected static UniqueId tagKunik = null;
	/** The uniqueid for tag values */
	protected static UniqueId tagVunik = null;
	/** The uniqueid for metric names */
	protected static UniqueId tagMunik = null;

	/**
	 * Creates a new test TSDB
	 * @param configName The config name to configure with
	 * @return the created test TSDB
	 */
	public static TSDB newTSDB(String configName)  {		
		try {
			tsdb = new TSDB(getConfig(configName));
			tsdb.getConfig().overrideConfig("helios.config.name", configName);
			Config config = tsdb.getConfig();
			StringBuilder b = new StringBuilder("\n\t=============================================\n\tTSDB Config\n\t=============================================");
			for(Map.Entry<String, String> entry: config.getMap().entrySet()) {
				b.append("\n\t").append(entry.getKey()).append("\t:[").append(entry.getValue()).append("]");
			}
			b.append("\n\t=============================================\n");
			log(b.toString());
			tsdb.initializePlugins(true);
			tagKunik = UniqueIdRegistry.getInstance().getTagKUniqueId();
			tagVunik = UniqueIdRegistry.getInstance().getTagVUniqueId();
			tagMunik = UniqueIdRegistry.getInstance().getMetricsUniqueId();
			return tsdb;
		} catch (Exception e) {
			throw new RuntimeException("Failed to get test TSDB [" + configName + "]", e);
		}		
	}
	
	/**
	 * Returns the environment classloader for the passed TSDB config
	 * @param configName The config name
	 * @return The classloader that would be created for the passed config
	 */
	public static ClassLoader tsdbClassLoader(String configName) {
		try {
			Config config = getConfig(configName);
			return TSDBPluginServiceLoader.getSupportClassLoader(config);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to load config [" + configName + "]", ex);
			
		}
	}
	
	/**
	 * Forces a stop on the test TSDB
	 */
	public static void stopTSDB() {
		if(tsdb==null) return;
		//TSDBEventDispatcher dispatcher = TSDBEventDispatcher.getInstance(tsdb);
		boolean stopped = false;
		try {
			try {
				long start = System.currentTimeMillis();
				tsdb.shutdown().join(5000);
				long elapsed = System.currentTimeMillis()-start;
				tsdb = null;
				stopped = true;
				log("Stopped Test TSDB Normally in [%s] ms.", elapsed);
			} catch (Exception ex) {
				ex.printStackTrace(System.err);
				try {
					Field clientField = TSDB.class.getDeclaredField("client");
					clientField.setAccessible(true);
					HBaseClient client = (HBaseClient)clientField.get(tsdb);
					client.shutdown();
					tsdb = null;
					stopped = true;
					log("Stopped Test TSDB Via Hack");
				} catch (Exception ex2) {
					throw new RuntimeException("Failed to really stop TSDB", ex2);
				}
			}
		} catch (Exception ex) {
			throw new RuntimeException("Failed to stop TSDB", ex);
		}
//		if(stopped) {
//			Assert.assertEquals("Dispatcher Executor Not Shutdown", true, dispatcher.isAsyncShutdown());
//		}
		resetEventDispatcher();
	}
	
	/**
	 * Nothing yet
	 * @throws java.lang.Exception thrown on any error
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	/**
	 * Deletes the temp plugin directory
	 * @throws java.lang.Exception thrown on any error
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		log("\t\t----->> TearDown After Class:%s", !tearDownTSDBAfterTest);
		if(!tearDownTSDBAfterTest) {
			shutdownTest();
			log("AfterClass Shutdown Complete");
		}
		log("Deleted Temp Plugin Dir:" + new File(TMP_PLUGIN_DIR).delete());
		
	}

	/**
	 * Nothing yet...
	 * @throws java.lang.Exception thrown on any error
	 */
	@Before
	public void setUp() throws Exception {
	}
	

	/**
	 * Cleans up various artifacts after each test
	 * @throws java.lang.Exception thrown on any error
	 */
	@After
	public void tearDown() throws Exception {
		log("\t\t----->> TearDown After Test:%s", tearDownTSDBAfterTest);
		if(tearDownTSDBAfterTest) {			
			shutdownTest();
		}
	}
	
	static void shutdownTest() {
		//=========================================================================================
		//  Delete temp files created during test
		//=========================================================================================
		int files = TO_BE_DELETED.size();
		Iterator<File> iter = TO_BE_DELETED.iterator();
		while(iter.hasNext()) {
			iter.next().delete();			
		}
		TO_BE_DELETED.clear();
		log("Deleted [%s] Tmp Files", files);

		//=========================================================================================
		//  Shutdown the test TSDB and clear the test queues.
		//=========================================================================================
		
//		Method shutdownMethod = TSDBPluginServiceLoader.class.getDeclaredMethod("reset");
//		shutdownMethod.setAccessible(true);
		try {
			stopTSDB();
			log("Stopped TSDB");
		} catch (Exception ex) {
			log("Failed to stop TSDB");
		}
		QueuedResultSearchEventHandler.getInstance().clearQueue();
		QueuedResultPublishEventHandler.getInstance().clearQueue();		
	}

	/**
	 * Out printer
	 * @param fmt the message format
	 * @param args the message values
	 */
	public static void log(String fmt, Object...args) {
		OUT.println(String.format(fmt, args));
	}
	
	/**
	 * Loads the named TSDB config 
	 * @param name The name of the config file
	 * @return The named config
	 * @throws Exception thrown on any error
	 */
	public static Config getConfig(String name) throws Exception {
		if(name==null || name.trim().isEmpty()) throw new Exception("File was null or empty");
		name = String.format("./src/test/resources/configs/%s.cfg", name);
		File f = new File(name);
		if(!f.canRead()) throw new Exception("Cannot read from the file [" + name + "]");
		log("Loading [%s]", f.getAbsolutePath());
		return new Config(f.getAbsolutePath());
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
		} else {
			ERR.println("");
		}
	}
	
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
	
	public static void createServiceJar() {
		createSearchShellJar(); createPublishShellJar(); createRPCShellJar();
	}
	
	
	/**
	 * Creates a search plugin jar for the {@link Search} shell plugin
	 */
	private static void createSearchShellJar() {
		createPluginJar(Search.class);
	}
	
	/**
	 * Creates a search plugin jar for the {@link Publisher} shell plugin
	 */
	private static void createPublishShellJar() {
		createPluginJar(Publisher.class);
	}
	
	/**
	 * Creates an RPC plugin jar for the {@link RpcService} shell plugin
	 */
	private static void createRPCShellJar() {
		createPluginJar(RpcService.class);
	}
	
	
	
	/**
	 * Creates a temp plugin jar in the plugin directory
	 * @param plugin The plugin class to install
	 */
	public static void createPluginJar(Class<?> plugin) {
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
			if(RpcPlugin.class.isAssignableFrom(plugin)) {
				jos.putNextEntry(new ZipEntry("META-INF/services/net.opentsdb.tsd.RpcPlugin"));
				jos.write((plugin.getName() + "\n").getBytes());
				jos.flush();
				jos.closeEntry();				
			}
			
			jos.flush();
			jos.close();
			fos.flush();
			fos.close();
		} catch (Exception e) {
			throw new RuntimeException("Failed to Plugin Jar for [" + plugin.getName() + "]", e);
		} finally {
			if(fos!=null) try { fos.close(); } catch (Exception e) {}
		}		
	}
	
	/**
	 * Generates an array of random strings created from splitting a randomly generated UUID.
	 * @return an array of random strings
	 */
	public static String[] getRandomFragments() {
		return UUID.randomUUID().toString().split("-");
	}
	
	/**
	 * Generates a random string made up from a UUID.
	 * @return a random string
	 */
	public static String getRandomFragment() {
		return UUID.randomUUID().toString();
	}
	
	/**
	 * Starts the periodic generation of data point events using randomly generated values for UIDs.
	 * @param tsdb The TSDB to push the annotations to
	 * @param quantity The total number of data points to push
	 * @param tagCount The number of custom map entries per annotation
	 * @param period The frequency of publication in ms. Frequencies of less than 1 ms. will push out the entire quantity at once.
	 * @return a map of the generated data points.
	 */
	public Map<String, DataPoint> startDataPointStream(final TSDB tsdb, int quantity, int tagCount, final long period) {
		final Map<String, DataPoint> dataPoints = new LinkedHashMap<String, DataPoint>(quantity);
		for(int i = 0; i < quantity; i++) {
			HashMap<String, String> tags = new LinkedHashMap<String, String>(tagCount);
			for(int c = 0; c < tagCount; c++) {
				String[] frags = getRandomFragments();
				tags.put(frags[0], frags[1]);
			}
			DataPoint a = DataPoint.newDataPoint(nextPosDouble(), getRandomFragment(), tags);
			dataPoints.put(a.getKey(), a);
		}
		Runnable r = new Runnable() {
			@Override
			public void run() {
				try {
					for(DataPoint dp: dataPoints.values()) {	
						publishDataPoint(dp);
						//dp.publish(tsdb);
						if(period>0) {
							Thread.currentThread().join(period);
						}
					}
				} catch (Exception ex) {
					ex.printStackTrace(System.err);
					return;
				}
			}
		};
		startStream(r, "DataPointStream");
		return dataPoints;
	}
	

	/**
	 * Starts the periodic generation of annotation indexing events using randomly generated values for UIDs.
	 * @param tsdb The TSDB to push the annotations to
	 * @param quantity The total number of annotations to push
	 * @param customs The number of custom map entries per annotation
	 * @param period The frequency of publication in ms. Frequencies of less than 1 ms. will push out the entire quantity at once.
	 * @return a map of the generated annotations.
	 */
	public Map<String, Annotation> startAnnotationStream(final TSDB tsdb, int quantity, int customs, final long period) {
		final Map<String, Annotation> annotations = new LinkedHashMap<String, Annotation>(quantity);
		for(int i = 0; i < quantity; i++) {
			Annotation a = new Annotation();
			if(customs>0) {
				HashMap<String, String> custs = new LinkedHashMap<String, String>(customs);
				for(int c = 0; c < customs; c++) {
					String[] frags = getRandomFragments();
					custs.put(frags[0], frags[1]);
				}
				a.setCustom(custs);
			}
			a.setDescription(getRandomFragment());
			long start = nextPosLong();
			a.setStartTime(start);
			a.setEndTime(start + nextPosInt(10000));
			a.setTSUID(getRandomFragment());
			annotations.put(a.getTSUID() + "/" + a.getStartTime(), a);
		}
		Runnable r = new Runnable() {
			@Override
			public void run() {
				try {
					for(Annotation an: annotations.values()) {
						tsdb.indexAnnotation(an);
						if(period>0) {
							Thread.currentThread().join(period);
						}
					}
				} catch (Exception ex) {
					ex.printStackTrace(System.err);
					return;
				}
			}
		};
		startStream(r, "AnnotationStream");
		return annotations;
	}
	
	/** A serial number factory for stream threads */
	public static final AtomicLong streamThreadSerial = new AtomicLong();

	/**
	 * Starts a stream thread
	 * @param r The runnable to run
	 * @param threadName the name of the thread
	 */
	public void startStream(Runnable r, String threadName) {
		Thread t = new Thread(r, threadName + "#" + streamThreadSerial.incrementAndGet());
		t.setDaemon(true);
		t.start();
		log("Started Thread [%s]", threadName);
	}
	
	/**
	 * Asserts the equality of the passed indexable objects
	 * @param c the created indexable object
	 * @param r the retrieved indexable object
	 */
	protected void validate(Object c, Object r) {
		Assert.assertNotNull("The created indexable was null", c);
		Assert.assertNotNull("The retrieved indexable was null", r);
		Assert.assertEquals("The retrieved indexable is not the same type as the created", c.getClass(), r.getClass());
		if(c instanceof Annotation) {
			Annotation a = (Annotation)c, a2 = (Annotation)r;
			Assert.assertEquals("The annotation TSUIDs do not match", a.getTSUID(), a2.getTSUID());
			Assert.assertEquals("The annotation Start Times do not match", a.getStartTime(), a2.getStartTime());
			Assert.assertEquals("The annotation Descriptions do not match", a.getDescription(), a2.getDescription());
			Assert.assertEquals("The annotation Notes do not match", a.getNotes(), a2.getNotes());
			Assert.assertEquals("The annotation Customs do not match", a.getCustom(), a2.getCustom());						
		} else if(c instanceof TSMeta) {
			TSMeta t = (TSMeta)c, t2 = (TSMeta)r;
			Assert.assertEquals("The TSMeta TSUIDs do not match", t.getTSUID(), t2.getTSUID());
			Assert.assertEquals("The TSMeta Create Times do not match", t.getCreated(), t2.getCreated());
			Assert.assertEquals("The TSMeta descriptions do not match", t.getDescription(), t2.getDescription());
			Assert.assertEquals("The TSMeta notes do not match", t.getNotes(), t2.getNotes());
			Assert.assertEquals("The TSMeta customs do not match", t.getCustom(), t2.getCustom());
		} else if(c instanceof UIDMeta) {
			UIDMeta u = (UIDMeta)c, u2 = (UIDMeta)r;
			Assert.assertEquals("The UIDMeta UIDs do not match", u.getUID(), u2.getUID());
			Assert.assertEquals("The UIDMeta Create Times do not match", u.getCreated(), u2.getCreated());
			Assert.assertEquals("The UIDMeta descriptions do not match", u.getDescription(), u2.getDescription());
			Assert.assertEquals("The UIDMeta notes do not match", u.getNotes(), u2.getNotes());
			Assert.assertEquals("The UIDMeta customs do not match", u.getCustom(), u2.getCustom());
		} else {
			throw new RuntimeException("Invalid Indexable Type [" + c.getClass().getName() + "]");
		}
	}
	
	public static <T> T executeAsync(Deferred<T> deferred) throws Exception {
		return executeAsync(deferred, 2000);
	}
	
	public static <T> T executeAsync(Deferred<T> deferred, long timeoutMs) throws Exception {
		final CountDownLatch latch = new CountDownLatch(1);
		final AtomicReference<T> result = new AtomicReference<T>(null); 
		final AtomicReference<Throwable> err = new AtomicReference<Throwable>(null);
		deferred.addCallback(new Callback<Void, T>() {
			@Override
			public Void call(T t) throws Exception {
				if(t instanceof Throwable) {
					err.set((Throwable)t);
				} else {
					result.set(t);
				}
				latch.countDown();
				return null;
			}
		});
		deferred.addErrback(new Callback<Void, T>() {
			@Override
			public Void call(T t) throws Exception {
				if(t instanceof Throwable) {
					err.set((Throwable)t);
				} else {
					result.set(t);
				}
				latch.countDown();
				return null;
			}
		});
		if(!latch.await(timeoutMs, TimeUnit.MILLISECONDS)) {
			throw new Exception("Timeout after [" + timeoutMs + "] ms.");
		}
		if(err.get()!=null) {
			throw new Exception("Errback", err.get());
		}
		return result.get();
	}
	
}
