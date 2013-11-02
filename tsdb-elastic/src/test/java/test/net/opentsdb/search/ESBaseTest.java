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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.Notification;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;

import net.opentsdb.meta.Annotation;
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
	/** The embedded test ES data directory */
	public static File ES_HOME_DATA = null;
	
	/** Indicates if the ES instance is started */
	public static boolean ES_STARTED = false;
	/** The ES instance bootstrap thread */
	public static Thread ES_BOOT_THREAD = null;
	
	/** The sysprop defining the home dir for ES */
	public static final String ES_PATH_HOME_PROP = "es.path.home";
	/** The URL for the ES Status */
	public static final String ES_STATUS_URL = "http://localhost:9200/_status";
	
	//==============================================================================================
	/*
	This is the JSON returned when HTTP GETing an ES host:port.
	The idea is that we check the sysprop tsdb.plugins.testing.search.esurl
	If it is defined, we verify.
	If it is not defined, or verification fails, we try localhost:9200
	If that fails too, we boot up a temp instance.
	*/
	//==============================================================================================
	
//	{
//		  "ok" : true,
//		  "status" : 200,
//		  "name" : "Ancient One",
//		  "version" : {
//		    "number" : "0.90.5",
//		    "build_hash" : "c8714e8e0620b62638f660f6144831792b9dedee",
//		    "build_timestamp" : "2013-09-17T12:50:20Z",
//		    "build_snapshot" : false,
//		    "lucene_version" : "4.4"
//		  },
//		  "tagline" : "You Know, for Search"
//		}	
	
	
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
			ES_HOME_DATA = new File(ES_HOME.getAbsolutePath() + File.separator + "data");
			ES_HOME_DATA.mkdir();			
			
			System.setProperty("path.logs", ES_HOME_LOG.getAbsolutePath());
			System.setProperty("cluster.name", "opentsdb");
			System.setProperty("es.logger.level", "DEBUG");
			
			
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
					Thread.currentThread().join(1000);
				}
			}
			if(!ES_STARTED) {
				throw new Exception("ES Failed to start");
			}
			log("ES Started  ----  Home:" + ES_HOME);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to boot ES test instance", ex);
		}
	}
	
	public static void stopEs() {
		ElasticSearch.close(new String[]{});
		cleanup();
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
    
    /**
     * Returns the document ID for the passed annotation
     * @param annotationType The annotation type name
     * @param annotation the annotation to get the ID for
     * @return the ID of the annotation
     */
    public String getAnnotationId(String annotationType, Annotation annotation) {
    	return String.format("%s%s%s", annotationType, annotation.getStartTime(), (annotation.getTSUID()==null ? "" : annotation.getTSUID()));
    }
	
//	protected static final Logger LOG = Logger.getLogger("STDOUT");
//	protected static final Logger LOGE = Logger.getLogger("STDERR");
	
    /**
     * <p>Title: DocEventWaiter</p>
     * <p>Description: </p> 
     * <p>Company: Helios Development Group LLC</p>
     * @author Whitehead (nwhitehead AT heliosdev DOT org)
     * <p><code>test.net.opentsdb.search.ESBaseTest.DocEventWaiter</code></p>
     */
    public class DocEventWaiter {
    	/** The waiter latch */
    	final TimeOutCountDownLatch latch;
    	/** The collected events */
    	final Set<PercolateEvent> events;
    	/** The kill scheduled task handle */
    	final ScheduledFuture<?> handle;
    	/** The registered listener */
    	final WaitEventListener listener;
    	/** The timeout period to wait for the events */
    	final long timeout;
    	/** The timeout unit */
    	final TimeUnit unit;
    	/** The matcher ObjectName */
    	final ObjectName matcher;
    	
    	/**
    	 * Creates a new DocEventWaiter
	     * @param matcher The ObjectName to match the events
	     * @param count The number of events to wait for
	     * @param timeout The timeout
	     * @param unit The timeout unit
    	 */
    	DocEventWaiter(ObjectName matcher, int count, long timeout, TimeUnit unit) {
    		this.timeout = timeout;
    		this.unit = unit;
    		this.matcher = matcher;
        	events = new HashSet<PercolateEvent>(count);
        	latch = new TimeOutCountDownLatch(count);
        	listener = new WaitEventListener(matcher, latch, events);
        	JMXHelper.addNotificationListener(IndexOperations.OBJECT_NAME, listener, listener, null);
        	handle = scheduler.schedule(new Runnable() {
        		@Override
        		public void run() {
        			latch.kill();
        			JMXHelper.removeNotificationListener(IndexOperations.OBJECT_NAME, listener);
        		}
        	}, timeout, unit);    		
    	}
    	
    	/**
    	 * Cleans up the waiter 
    	 */
    	public void cleanup() {
    		try { JMXHelper.removeNotificationListener(IndexOperations.OBJECT_NAME, listener); } catch (Exception ex) {}
    	}
    	
    	/**
    	 * Waits for the timeout period for the matching events to be delivered
    	 * @return a collection of received PercolateEvents.
    	 */
    	Collection<PercolateEvent> waitForEvent() {
        	try {
    	    	if(latch.await(timeout, unit)) {
    	    		return events;
    	    	}
    	    	RuntimeException ex = new RuntimeException("Thread Timed Out Waiting On PercolateEvents Matching [" + matcher + "]");
    	    	loge("WaitForEvent Failed", ex);
    	    	throw ex;
        	} catch (InterruptedException iex) {
        		/* Won't happen ... ? */
        		throw new RuntimeException("Thread Interrupted While Waiting On PercolateEvents Matching [" + matcher + "]", iex);
        	} finally {
        		handle.cancel(true);
        		try { JMXHelper.removeNotificationListener(IndexOperations.OBJECT_NAME, listener); } catch (Exception ex) {}    		
        	}    		
    	}
    }
    
    
    
    /** Serial number factory for wait listeners */
    private static final AtomicInteger LISTENER_SERIAL = new AtomicInteger();
    /** The number of filter mismatches */
    protected static final AtomicLong FILTER_FAILS = new AtomicLong();
    
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
    	/** The listener serial number */
    	public final int serial = LISTENER_SERIAL.incrementAndGet();
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
				PercolateEvent pe = (PercolateEvent)userData;
				if(!matcher.apply(pe.getObjectName())) {
					FILTER_FAILS.incrementAndGet();
					log("Failed to match Percolate Events\n\t[%s]\nvs\n\t[%s]", matcher, pe.getObjectName());
					return false;
				}
				return true;				
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

		/**
		 * {@inheritDoc}
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + serial;
			return result;
		}

		/**
		 * {@inheritDoc}
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			WaitEventListener other = (WaitEventListener) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (serial != other.serial)
				return false;
			return true;
		}

		private ESBaseTest getOuterType() {
			return ESBaseTest.this;
		}

		/**
		 * {@inheritDoc}
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return String.format("WaitEventListener [matcher=%s, serial=%s]",
					matcher, serial);
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
