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
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.management.Notification;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;

import net.opentsdb.search.index.IndexOperations;
import net.opentsdb.search.index.PercolateEvent;

import org.elasticsearch.bootstrap.ElasticSearch;
import org.helios.tsdb.plugins.shell.Search;
import org.helios.tsdb.plugins.test.BaseTest;
import org.helios.tsdb.plugins.util.JMXHelper;
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
	
    /**
     * <p>Title: TimeOutCountDownLatch</p>
     * <p>Description: A killable count down latch. Mostly acts like a regular CountDownLatch, but once it is killed,
     * any threads successfully passing await will throw a runtime exception.</p> 
     * <p>Company: Helios Development Group LLC</p>
     * @author Whitehead (nwhitehead AT heliosdev DOT org)
     * <p><code>test.net.opentsdb.search.ESBaseTest.TimeOutCountDownLatch</code></p>
     */
    public class TimeOutCountDownLatch extends CountDownLatch {
    	/** Indicates if the latch was killed */
    	private final AtomicBoolean killed = new AtomicBoolean(false);
    	
		/**
		 * Creates a new TimeOutCountDownLatch
		 * @param count The drop count
		 */
		public TimeOutCountDownLatch(int count) {
			super(count);
		}
    	
		/**
		 * Kills the latch, meaning anyone passing the await will get an exception
		 * @return true if the kill switch was invoked, false if it was already invoked
		 */
		public boolean kill() {
			boolean _killed = killed.compareAndSet(false, true);
			if(_killed) {
				while(getCount()>0) countDown();
			}
			return _killed;
		}
		
		/**
		 * {@inheritDoc}
		 * @see java.util.concurrent.CountDownLatch#await()
		 */
		@Override
		public void await() throws InterruptedException {	
			super.await();
			if(killed.get()) throw new RuntimeException("CountDownLatch was killed");
		}
		
		/**
		 * {@inheritDoc}
		 * @see java.util.concurrent.CountDownLatch#await(long, java.util.concurrent.TimeUnit)
		 */
		@Override
		public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
			boolean passed = super.await(timeout, unit);
			if(passed && killed.get()) throw new RuntimeException("CountDownLatch was killed after timeout [" + timeout + ":" + unit.name() + "]");
			return passed;
		}
    }
	
//	protected static final Logger LOG = Logger.getLogger("STDOUT");
//	protected static final Logger LOGE = Logger.getLogger("STDERR");
	
    /**
     * Waits for the specified number of percolate events and then returns them as a collection
     * @param matcher The ObjectName to match the events
     * @param count The number of events to wait for
     * @param timeout The timeout
     * @param unit The timeout unit
     * @return the collection of matching events
     */
    public Collection<PercolateEvent> waitOnDocEvent(ObjectName matcher, int count, long timeout, TimeUnit unit) {
    	final Set<PercolateEvent> events = new HashSet<PercolateEvent>(count);
    	final TimeOutCountDownLatch latch = new TimeOutCountDownLatch(count);
    	final WaitEventListener listener = new WaitEventListener(matcher, latch, events);
    	JMXHelper.addNotificationListener(IndexOperations.OBJECT_NAME, listener, listener, null);
    	final ScheduledFuture<?> handle = scheduler.schedule(new Runnable() {
    		@Override
    		public void run() {
    			latch.kill();
    			JMXHelper.removeNotificationListener(IndexOperations.OBJECT_NAME, listener);
    		}
    	}, timeout, unit);
    	try {
	    	if(latch.await(timeout, unit)) {
	    		return events;
	    	}
	    	throw new RuntimeException("Thread Timed Out Waiting On PercolateEvents Matching [" + matcher + "]");
    	} catch (InterruptedException iex) {
    		/* Won't happen ... ? */
    		throw new RuntimeException("Thread Interrupted While Waiting On PercolateEvents Matching [" + matcher + "]", iex);
    	} finally {
    		handle.cancel(true);
    		try { JMXHelper.removeNotificationListener(IndexOperations.OBJECT_NAME, listener); } catch (Exception ex) {}    		
    	}
    }
    
    
    /**
     * <p>Title: WaitEventListener</p>
     * <p>Description: </p> 
     * <p>Company: Helios Development Group LLC</p>
     * @author Whitehead (nwhitehead AT heliosdev DOT org)
     * <p><code>test.net.opentsdb.search.WaitEventListener</code></p>
     */
    public class WaitEventListener implements NotificationListener, NotificationFilter {
    	/**  */
		private static final long serialVersionUID = 7281541728123940486L;
		/** The ObjectName to match against in the filter */
    	private final ObjectName matcher;
    	/** The latch to drop when the event arrives */
    	private final CountDownLatch latch;
    	/** The collection to put events into if they match */
    	private final Collection<PercolateEvent> events;
		/**
		 * Creates a new WaitEventListener
		 * @param matcher The ObjectName to match against in the filter
		 * @param latch The latch to drop when the event arrives 
		 * @param events The collection to put events into if they match 
		 */
		public WaitEventListener(ObjectName matcher, CountDownLatch latch, Collection<PercolateEvent> events) {
			this.matcher = matcher;
			this.latch = latch;
			this.events = events;
		}

		/**
		 * {@inheritDoc}
		 * @see javax.management.NotificationFilter#isNotificationEnabled(javax.management.Notification)
		 */
		@Override
		public boolean isNotificationEnabled(Notification notification) {
			Object userData = notification.getUserData();
			if(userData!=null && userData instanceof PercolateEvent) {				
				return ((PercolateEvent)userData).matches(matcher);
			}
			return false;
		}

		/**
		 * {@inheritDoc}
		 * @see javax.management.NotificationListener#handleNotification(javax.management.Notification, java.lang.Object)
		 */
		@Override
		public void handleNotification(Notification notification, Object handback) {
			try { 
				events.add((PercolateEvent)notification.getUserData());
				latch.countDown(); 				
			} catch (Exception ex) {}			
		}
    	
    }
	
	
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
