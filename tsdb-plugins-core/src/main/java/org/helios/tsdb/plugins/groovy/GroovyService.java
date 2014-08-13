
package org.helios.tsdb.plugins.groovy;

import groovy.lang.Binding;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyShell;
import groovy.lang.GroovySystem;
import groovy.lang.MissingPropertyException;
import groovy.lang.Script;

import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.ObjectName;
import javax.script.Bindings;

import net.opentsdb.core.TSDB;

import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.helios.tsdb.plugins.service.PluginContext;
import org.helios.tsdb.plugins.util.JMXHelper;
import org.helios.tsdb.plugins.util.SystemClock;
import org.helios.tsdb.plugins.util.URLHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Title: GroovyService</p>
 * <p>Description: container wide Groovy script compiler, interpreter and executor.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.groovy.GroovyService</code></p>
 */
public class GroovyService implements GroovyLoadedScriptListener, GroovyServiceMXBean {
	/** A map of compiled scripts keyed by an arbitrary reference name */
	protected final Map<String, Script> compiledScripts = new ConcurrentHashMap<String, Script>();
	
	/** The instance logger */
	protected final Logger log = LoggerFactory.getLogger(getClass());
	
	/** The plugin context */
	protected final PluginContext pluginContext;
	/** The TSDB instance */
	protected final TSDB tsdb;
	/** The TSDB provided and extracted configuration */
	protected final Properties config;
	/** Thread pool for asynch tasks */
	protected ThreadPoolExecutor threadPool;
	/** Scheduler for scheduled tasks */	
	protected ScheduledThreadPoolExecutor scheduler;
	
	/** The compiler configuration for script compilations */
	protected final CompilerConfiguration compilerConfiguration = new CompilerConfiguration();
	/** A groovy classloader for compiling scripts */
	protected final GroovyClassLoader groovyClassLoader; 
	/** The shared bindings */
	protected final Map<String, Object> beans = new HashMap<String, Object>();
	
	/** A set of registered class listeners */
	protected final Set<GroovyLoadedScriptListener> listeners = new CopyOnWriteArraySet<GroovyLoadedScriptListener>();
	
	/** The compiler configuration's JMX ObjectName */
	protected final ObjectName compilerConfigurationObjectName;

	/** This service's JMX ObjectName */
	protected final ObjectName objectName;
	
	/** The started indicator */
	protected final AtomicBoolean started = new AtomicBoolean(false);
	
	/** The plugin class loader */
	protected final ClassLoader pluginClassLoader;
	
	
	
	/** A set of implicit imports for the compiler configuration */
	protected final Set<String> imports = new CopyOnWriteArraySet<String>();
	/** The initial and default imports customizer for the compiler configuration */
	protected final ImportCustomizer importCustomizer = new ImportCustomizer(); 
	
	/**
	 * Creates a new GroovyService
	 * @param pluginContext The shared plugin context
	 */
	public GroovyService(PluginContext pluginContext) {
		this.pluginContext = pluginContext;
		this.pluginClassLoader = pluginContext.getSupportClassLoader();
		this.tsdb = pluginContext.getTsdb();
		this.config = pluginContext.getExtracted();
		pluginContext.setResource("groovy-service", this);
		objectName = JMXHelper.objectName(getClass().getPackage().getName() + ":service=" + getClass().getSimpleName());
		compilerConfigurationObjectName = JMXHelper.objectName(objectName.toString() + ",type=CompilerConfiguration");
		imports.add("import org.helios.apmrouter.groovy.annotations.*");
		compilerConfiguration.setOptimizationOptions(Collections.singletonMap("indy", true));
		groovyClassLoader =  new GroovyClassLoader(getClass().getClassLoader(), compilerConfiguration);
		registerLoadListener(this);
		if(JMXHelper.getHeliosMBeanServer().isRegistered(objectName)) {
			try { JMXHelper.getHeliosMBeanServer().unregisterMBean(objectName); } catch (Exception ex) {/* No Op */}
		}
		try { 
			JMXHelper.getHeliosMBeanServer().registerMBean(this, objectName); 
			log.info("\n\t============================================\n\tRegistered [" + objectName + "]\n\t============================================\n");
		} catch (Exception ex) {
			log.warn("Failed to register GroovyService Management Interface", ex);
		}
	}
	
	

	/**
	 * Called when the application context refreshes 
	 */
	protected void doStart() {
		if(isStarted()) return;
		applyImports(importCustomizer, imports.toArray(new String[imports.size()]));
		started.set(true);
	}
	
	/**
	 * Applies the configured imports to the compiler configuration
	 * @param impCustomizer The import customizer to add the imports to
	 * @param imps  The imports to add
	 */
	protected void applyImports(ImportCustomizer impCustomizer, String...imps) {		
		for(String imp: imps) {
			String _imp = imp.trim().replaceAll("\\s+", " ");
			if(!_imp.startsWith("import")) {
				log.warn("Unrecognized import [" + imp + "]");
				continue;
			}
			if(_imp.startsWith("import static ")) {
				if(_imp.endsWith(".*")) {
					impCustomizer.addStaticStars(_imp.replace("import static ", "").replace(".*", ""));
				} else {
					String cleaned = _imp.replace("import static ", "").replace(".*", "");
					int index = cleaned.lastIndexOf('.');
					if(index==-1) {
						log.warn("Failed to parse non-star static import [" + imp + "]");
						continue;
					}
					impCustomizer.addStaticImport(cleaned.substring(0, index), cleaned.substring(index+1));
				}
			} else {
				if(_imp.endsWith(".*")) {
					impCustomizer.addStarImports(_imp.replace("import ", "").replace(".*", ""));
				} else {
					impCustomizer.addImports(_imp.replace("import ", ""));
				}
			}
		}
		compilerConfiguration.addCompilationCustomizers(impCustomizer);
	}
	
	/**
	 * Registers the passed load listener
	 * @param listener the load listener
	 */
	public void registerLoadListener(GroovyLoadedScriptListener listener) {
		if(listener!=null) {
			listeners.add(listener);
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.groovy.GroovyServiceMXBean#isStarted()
	 */
	@Override
	public boolean isStarted() {
		return started.get();
	}
	
	/**
	 * Unregisters the passed load listener
	 * @param listener the load listener
	 */
	public void unregisterLoadListener(GroovyLoadedScriptListener listener) {
		if(listener!=null) {
			listeners.remove(listener);
		}
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.groovy.GroovyServiceMXBean#flushScriptCache()
	 */
	@Override
	public void flushScriptCache() {
		compiledScripts.clear();
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.groovy.GroovyServiceMXBean#flushScript(java.lang.String)
	 */
	@Override
	public void flushScript(String name) {
		if(name==null || name.trim().isEmpty()) throw new IllegalArgumentException("The passed name was null or empty", new Throwable());
		compiledScripts.remove(name);
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.groovy.GroovyServiceMXBean#getGroovyVersion()
	 */
	@Override
	public String getGroovyVersion() {
		return GroovySystem.getVersion();
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.groovy.GroovyServiceMXBean#getScriptNames()
	 */
	@Override
	public String[] getScriptNames() {
		return compiledScripts.keySet().toArray(new String[compiledScripts.size()]);
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.groovy.GroovyServiceMXBean#isUseReflection()
	 */
	@Override
	public boolean isUseReflection() {
		return GroovySystem.isUseReflection();
	}
	
	/*
	 * compile(String name, String source)
	 * compile(String name, URL source) // needs check for source update
	 * compile(String name, File source) // needs check for source update
	 * all options:
	 * 	name
	 * 	source
	 *  properties (compiler options)
	 *  url[] (additional classpaths)
	 *  classloader
	 *  
	 * 
	 * invoke(String name, OutputStream os, Object...args)  // run, with args in bindings
	 * invoke(String name, Object...args)  // run, with args in bindings, ditch output
	 * invokeMethod(String name, String methodName, Object...args)
	 * invokeMethod(String name, OutputStream os, String methodName, Object...args)
	 * 
	 * compileAndInvoke(...)
	 * 
	 */
	
	/**
	 * Invokes the named method in the named script and returns the value returned from the invocation
	 * @param name The name of the script to run
	 * @param methodName The name of the method to run
	 * @param os The output stream the script will write to when it calls <p><code>out</code></p>.
	 * @param es The output stream the script will write to when it calls <p><code>err</code></p>.
	 * @param args The arguments passed to the script as <p><code>args</code></p>.
	 * @return the value returned from the script
	 */
	public Object invoke(String name, String methodName, OutputStream os, OutputStream es, Object...args) {
		if(name==null || name.trim().isEmpty() )  throw new IllegalArgumentException("The passed script name was null or empty", new Throwable());
		if(methodName==null || methodName.trim().isEmpty() )  throw new IllegalArgumentException("The passed method name was null or empty", new Throwable());
		Script script = compiledScripts.get(name);
		if(script==null)  throw new IllegalArgumentException("No script found for passed script name [" + name + "]", new Throwable());
		if(os!=null) {
			script.setProperty("out", new PrintStream(os, true));
		}
		if(es!=null) {
			script.setProperty("err", new PrintStream(es, true));
		}		
		return script.invokeMethod(methodName, args);
	}
	
	/**
	 * Invokes the named method in the named script and returns the value returned from the invocation
	 * @param name The name of the script to run
	 * @param methodName The name of the method to run
	 * @param args The arguments passed to the script as <p><code>args</code></p>.
	 * @return the value returned from the script
	 */
	public Object invoke(String name, String methodName, Object...args) {
		if(name==null || name.trim().isEmpty() )  throw new IllegalArgumentException("The passed script name was null or empty", new Throwable());
		if(methodName==null || methodName.trim().isEmpty() )  throw new IllegalArgumentException("The passed method name was null or empty", new Throwable());
		Script script = compiledScripts.get(name);
		if(script==null)  throw new IllegalArgumentException("No script found for passed script name [" + name + "]", new Throwable());
		return invoke(name, methodName, args);
	}
	

	public Object invoke(String name, String methodName) {
		return invoke(name, methodName, EMPTY_OBJ_ARR);
	}
	
	/**
	 * Runs the named compiled script 
	 * @param scriptName The name of the script to run
	 * @param args The arguments passed to the script invocation bound as a property named <b><code>args</code></b>.
	 * @return The return value from the script invocation
	 */
	public Object invokeScript(String scriptName, Object...args) {
		if(scriptName==null || scriptName.trim().isEmpty()) throw new IllegalArgumentException("ScriptName was null or empty");
		Script script = this.compiledScripts.get(scriptName);
		if(script==null) throw new RuntimeException("No Script found for ScriptName [" + scriptName + "]");
		Binding binds = getBindings();
		binds.setProperty("args", args);
		script.setBinding(binds);
		return script.run();
	}
	
	
	
	/**
	 * Runs the named script and returns the value returned from the invocation
	 * @param name The name of the script to run
	 * @param os The output stream the script will write to when it calls <p><code>out</code></p>.
	 * @param es The output stream the script will write to when it calls <p><code>err</code></p>.
	 * @param args The arguments passed to the script as <p><code>args</code></p>.
	 * @return the value returned from the script
	 */
	public Object run(String name, OutputStream os, OutputStream es, Object...args) {
		if(name==null || name.trim().isEmpty() )  throw new IllegalArgumentException("The passed script name was null or empty", new Throwable());
		Script script = compiledScripts.get(name);
		if(script==null)  throw new IllegalArgumentException("No script found for passed script name [" + name + "]", new Throwable());
		if(args==null || args.length==0) {
			script.setProperty("args", EMPTY_OBJ_ARR);
		} else {
			script.setProperty("args", args);
		}
		if(os!=null) {
			script.setProperty("out", new PrintStream(os, true));
		}
		if(es!=null) {
			script.setProperty("err", new PrintStream(es, true));
		}		
		return script.run();
	}
	
	/** A synthetic script name serial generator */
	protected static final AtomicLong nameSerial = new AtomicLong();
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.groovy.GroovyServiceMXBean#compile(java.lang.String, java.lang.String)
	 */
	@Override
	public String compile(String scriptName, String source) {
		if(scriptName!=null && scriptName.trim().isEmpty()) scriptName=null;
		//else scriptName = scriptName.trim();
		if(source==null || source.length()==0) throw new IllegalArgumentException("The passed source was null or empty", new Throwable());	
		//source = source.replace("\\n", "\n");
		Script script = null;
		String name = scriptName!=null ? scriptName.trim() : "groovy#" + nameSerial.incrementAndGet();
//		GroovyClassLoader gcl = new GroovyClassLoader(Thread.currentThread().getContextClassLoader(), compilerConfiguration);
		try {
			script = new GroovyShell(compilerConfiguration).parse(source, name);
//			Class<?> clazz = gcl.parseClass(source);
			ScriptName sn = script.getClass().getAnnotation(ScriptName.class);
			if(sn!=null && !sn.value().trim().isEmpty()) {
				name = sn.value().trim();
			}
			log.info("Compiled script named [" + name + "]. Class is: [" + script.getClass().getName() + "]");
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
			throw new RuntimeException(ex);
		} finally {
//			try { gcl.close(); } catch (Exception ex) {}
		}
		Binding bindings = getBindings();
		script.setBinding(bindings);
		script.setProperty("bindings", bindings);
		compiledScripts.put(name, script);
		
		Class<?> clazz = script.getClass();//groovyClassLoader.parseClass(source);
		scanLoadedClass(clazz, script);
		return name;
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.groovy.GroovyServiceMXBean#compile(java.lang.String)
	 */
	@Override
	public String compile(String source) {
		return compile(null, source);
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.groovy.GroovyServiceMXBean#setOptimizationOption(java.lang.String, boolean)
	 */
	@Override
	public void setOptimizationOption(String name, boolean value) {
		compilerConfiguration.setOptimizationOptions(Collections.singletonMap(name, value));
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.groovy.GroovyServiceMXBean#compileFromUrl(java.lang.String)
	 */
	@Override
	public String compileFromUrl(String sourceUrl) {
		URL url = URLHelper.toURL(sourceUrl);
		String source = URLHelper.getTextFromURL(url);
		return compile(source);
	}
	
	
	/**
	 * Scans the passed class and looks for matches with registered groovy loaded class listeners, firing their callbacks when matches are found
	 * @param clazz The class to scan
	 * @param instance The instance of the scanned class
	 */
	protected void scanLoadedClass(Class<?> clazz, Object instance) {
		if(listeners.isEmpty()) return;
		Map<Class<? extends Annotation>, Annotation> typeAnns = new HashMap<Class<? extends Annotation>, Annotation>();
		Map<Class<? extends Annotation>, Map<Method, Set<Annotation>>> methodAnns = new HashMap<Class<? extends Annotation>, Map<Method, Set<Annotation>>>();
		for(Annotation annot: clazz.getAnnotations()) {
			typeAnns.put(annot.annotationType(), annot);
		}
		for(Method m: clazz.getMethods()) {
			for(Annotation annot: m.getAnnotations()) {
				addMethodAnnotation(methodAnns, m, annot);
			}				
		}
		for(Method m: clazz.getDeclaredMethods()) {
			for(Annotation annot: m.getAnnotations()) {
				addMethodAnnotation(methodAnns, m, annot);
			}				
		}
		for(GroovyLoadedScriptListener listener: listeners) {
			Set<Annotation> matchedAnnotations = new HashSet<Annotation>();
			for(Class<? extends Annotation> cl: listener.getScanTypeAnnotations()) {
				Annotation matchedAnnotation = typeAnns.get(cl);
				if(matchedAnnotation!=null) {
					matchedAnnotations.add(matchedAnnotation);
				}
			}			
			if(!matchedAnnotations.isEmpty()) {
				listener.onScanType(matchedAnnotations, clazz, instance);				
			}
			Map<Method, Set<Annotation>> matchedMethodAnnotations = new HashMap<Method, Set<Annotation>>();
			Set<Class<? extends Annotation>> listenerMethodAnnotationTypes = listener.getScanMethodAnnotations();
			for(Method method: clazz.getMethods()) {
				for(Annotation methodAnnotation: method.getAnnotations()) {
					if(listenerMethodAnnotationTypes.contains(methodAnnotation.annotationType())) {
						Set<Annotation> annotationSet = matchedMethodAnnotations.get(method);
						if(annotationSet==null) {
							annotationSet = new HashSet<Annotation>();
							matchedMethodAnnotations.put(method, annotationSet);
						}
						annotationSet.add(methodAnnotation);
					}
				}
			}
			for(Method method: clazz.getDeclaredMethods()) {
				for(Annotation methodAnnotation: method.getAnnotations()) {
					if(listenerMethodAnnotationTypes.contains(methodAnnotation.annotationType())) {
						Set<Annotation> annotationSet = matchedMethodAnnotations.get(method);
						if(annotationSet==null) {
							annotationSet = new HashSet<Annotation>();
							matchedMethodAnnotations.put(method, annotationSet);
						}
						annotationSet.add(methodAnnotation);
					}
				}
			}
			if(!matchedMethodAnnotations.isEmpty()) {
				listener.onScanMethod(matchedMethodAnnotations, clazz, instance);
			}
			Set<Class<?>> matchedParentClasses = new HashSet<Class<?>>();
			for(Class<?> parentClass: listener.getScanClasses()) {
				if(parentClass.isAssignableFrom(clazz)) {
					matchedParentClasses.add(parentClass);
				}				
			}
			if(!matchedParentClasses.isEmpty()) {
				listener.onScanClasses(matchedParentClasses, clazz, instance);
			}
		}
	}
	
	/**
	 * Adds the passed method and it's associated annotation to the passed annotation tree
	 * @param annotationTree The method annotation tree for the class being scanned
	 * @param method The scanned method
	 * @param annotation The annotation associated with the passed method
	 */
	protected void addMethodAnnotation(final Map<Class<? extends Annotation>, Map<Method, Set<Annotation>>> annotationTree, final Method method, final Annotation annotation) {
		Class<? extends Annotation> annClass = annotation.annotationType();
		Map<Method, Set<Annotation>> methodSets = annotationTree.get(annClass);
		if(methodSets==null) {
			methodSets = new HashMap<Method, Set<Annotation>>();
			methodSets.put(method, new HashSet<Annotation>());
			annotationTree.put(annClass, methodSets);
		}
		methodSets.get(method).add(annotation);
	}
	
	
	/**
	 * Compiles the passed source and assignes it the passed name
	 * @param name The name assigned to the compiled script
	 * @param source The source code of the script to compiled
	 */
	public void compileBuffer(String name, CharSequence source) {
		if(source==null || source.length()==0) throw new IllegalArgumentException("The passed source was null or empty", new Throwable());
		compile(name, source.toString());
	}
	
	
	/** Empty object array constant */
	protected static final Object[] EMPTY_OBJ_ARR = {}; 
	
	/**
	 * Executes the main function of the named script
	 * @param name The name of the script to execute
	 * @param args The optional arguments to pass to the script
	 * @return the return value from the script invocation
	 */
	public Object run(String name, Object...args) {
		return run(name, null, null, args);
	}
	
	public Object runScript(String name) {
		return run(name, EMPTY_OBJ_ARR);
	}
	
	
	
	/**
	 * Returns a bindings instance
	 * @return a bindings instance
	 */
	protected Binding getBindings() {
		if(beans.isEmpty()) {
			synchronized(beans) {
				if(beans.isEmpty()) {
					beans.put("jmxserver", JMXHelper.getHeliosMBeanServer());
					beans.put("jmxhelper", JMXHelper.class);
					beans.put("tsdb", tsdb);
					beans.put("pluginContext", pluginContext);
					beans.put("sysclock", SystemClock.class);
					
//					for(String beanName: applicationContext.getBeanDefinitionNames()) {
//						Object bean = applicationContext.getBean(beanName);
//						if(bean==null) continue;
//						beans.put(beanName, bean);
//					}
//					beans.put("RootCtx", applicationContext);					
				}
			}
		}
		//return new ThreadSafeNoNullsBinding(beans);
		return new Binding(beans);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.groovy.GroovyServiceMXBean#launchConsole()
	 */
	@Override
	public void launchConsole() {
		launchConsole(null);
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.groovy.GroovyServiceMXBean#launchConsole(java.lang.String)
	 */
	@Override
	public void launchConsole(String fileName) {
		final ClassLoader cl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(pluginClassLoader);
			Class<?> clazz = Class.forName("groovy.ui.Console", true, pluginClassLoader);
			Constructor<?> ctor = clazz.getDeclaredConstructor(ClassLoader.class, Binding.class);
			Object console = ctor.newInstance(pluginClassLoader, getBindings());
			console.getClass()
				.getDeclaredMethod("run")
					.invoke(console);
			if(fileName!=null) {
				fileName = fileName.trim();
				File f = new File(fileName);
				if(f.canRead()) {
					clazz.getDeclaredMethod("loadScriptFile", File.class).invoke(console, f);
				}
			}
		} catch (Exception e) {
			log.error("Failed to launch console", e);
			if(e.getCause()!=null) {
				log.error("Failed to launch console cause", e.getCause());
			}
			throw new RuntimeException("Failed to launch console", e);
		} finally {
			Thread.currentThread().setContextClassLoader(cl);
		}
	}
	


	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.groovy.GroovyServiceMXBean#getWarningLevel()
	 */
	@Override
	public int getWarningLevel() {
		return compilerConfiguration.getWarningLevel();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.groovy.GroovyServiceMXBean#setWarningLevel(int)
	 */
	@Override
	public void setWarningLevel(int level) {
		compilerConfiguration.setWarningLevel(level);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.groovy.GroovyServiceMXBean#getSourceEncoding()
	 */
	@Override
	public String getSourceEncoding() {
		return compilerConfiguration.getSourceEncoding();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.groovy.GroovyServiceMXBean#setSourceEncoding(java.lang.String)
	 */
	@Override
	public void setSourceEncoding(String encoding) {
		compilerConfiguration.setSourceEncoding(encoding);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.groovy.GroovyServiceMXBean#getTargetDirectory()
	 */
	@Override
	public String getTargetDirectory() {		
		File f = compilerConfiguration.getTargetDirectory();
		return f==null ? null : f.getAbsolutePath();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.groovy.GroovyServiceMXBean#setTargetDirectory(java.lang.String)
	 */
	@Override
	public void setTargetDirectory(String directory) {
		compilerConfiguration.setTargetDirectory(directory);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.groovy.GroovyServiceMXBean#isVerbose()
	 */
	@Override
	public boolean isVerbose() {
		return compilerConfiguration.getVerbose();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.groovy.GroovyServiceMXBean#setVerbose(boolean)
	 */
	@Override
	public void setVerbose(boolean verbose) {
		compilerConfiguration.setVerbose(verbose);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.groovy.GroovyServiceMXBean#isDebug()
	 */
	@Override
	public boolean isDebug() {
		return compilerConfiguration.getDebug();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.groovy.GroovyServiceMXBean#setDebug(boolean)
	 */
	@Override
	public void setDebug(boolean debug) {
		compilerConfiguration.setDebug(debug);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.groovy.GroovyServiceMXBean#getTolerance()
	 */
	@Override
	public int getTolerance() {
		return compilerConfiguration.getTolerance();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.groovy.GroovyServiceMXBean#setTolerance(int)
	 */
	@Override
	public void setTolerance(int tolerance) {
		compilerConfiguration.setTolerance(tolerance);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.groovy.GroovyServiceMXBean#getScriptBaseClass()
	 */
	@Override
	public String getScriptBaseClass() {
		return compilerConfiguration.getScriptBaseClass();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.groovy.GroovyServiceMXBean#setScriptBaseClass(java.lang.String)
	 */
	@Override
	public void setScriptBaseClass(String scriptBaseClass) {
		compilerConfiguration.setScriptBaseClass(scriptBaseClass);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.groovy.GroovyServiceMXBean#setMinimumRecompilationInterval(int)
	 */
	@Override
	public void setMinimumRecompilationInterval(int time) {
		compilerConfiguration.setMinimumRecompilationInterval(time);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.groovy.GroovyServiceMXBean#getMinimumRecompilationInterval()
	 */
	@Override
	public int getMinimumRecompilationInterval() {
		return compilerConfiguration.getMinimumRecompilationInterval();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.groovy.GroovyServiceMXBean#setTargetBytecode(java.lang.String)
	 */
	@Override
	public void setTargetBytecode(String version) {
		compilerConfiguration.setTargetBytecode(version);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.groovy.GroovyServiceMXBean#getTargetBytecode()
	 */
	@Override
	public String getTargetBytecode() {
		return compilerConfiguration.getTargetBytecode();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.groovy.GroovyServiceMXBean#getOptimizationOptions()
	 */
	@Override
	public Map<String, Boolean> getOptimizationOptions() {
		return compilerConfiguration.getOptimizationOptions();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.groovy.GroovyServiceMXBean#setOptimizationOptions(java.util.Map)
	 */
	@Override
	public void setOptimizationOptions(Map<String, Boolean> options) {
		compilerConfiguration.setOptimizationOptions(options);
	}


	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.groovy.GroovyLoadedScriptListener#getScanTypeAnnotations()
	 */
	@Override
	public Set<Class<? extends Annotation>> getScanTypeAnnotations() {
		return Collections.emptySet();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.groovy.GroovyLoadedScriptListener#getScanMethodAnnotations()
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Set<Class<? extends Annotation>> getScanMethodAnnotations() {
		return new HashSet<Class<? extends Annotation>>(Arrays.asList(Start.class));
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.groovy.GroovyLoadedScriptListener#getScanClasses()
	 */
	@Override
	public Set<Class<?>> getScanClasses() {
		return Collections.emptySet();
	}


	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.groovy.GroovyLoadedScriptListener#onScanType(java.util.Set, java.lang.Class, java.lang.Object)
	 */
	@Override
	public void onScanType(Set<? extends Annotation> annotations, Class<?> clazz, Object instance) {
		log.info("\n\t===================================\n\tType Annotation Match:" + clazz.getName() + "\n\t===================================\n");
	}


	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.groovy.GroovyLoadedScriptListener#onScanMethod(java.util.Map, java.lang.Class, java.lang.Object)
	 */
	@Override
	public void onScanMethod(Map<Method, Set<Annotation>> methods, Class<?> clazz, Object instance) {
		StringBuilder b = new StringBuilder("\n\t===================================\n\tMethod Annotation Match:" + clazz.getName() + "\n\t===================================\n");
		for(Map.Entry<Method, Set<Annotation>> match: methods.entrySet()) {
			b.append("\n\tMethod:").append(match.getKey().getName());
			for(Annotation ann: match.getValue()) {
				b.append("\n\t\tAnn:" + ann.annotationType().getSimpleName());
			}
		}
		log.info(b.toString());
	}


	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.groovy.GroovyLoadedScriptListener#onScanClasses(java.util.Set, java.lang.Class, java.lang.Object)
	 */
	@Override
	public void onScanClasses(Set<Class<?>> annotations, Class<?> clazz, Object instance) {
		log.info("\n\t===================================\n\tInherritance Match:" + clazz.getName() + "\n\t===================================\n");
	}


	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.groovy.GroovyServiceMXBean#getImports()
	 */
	@Override
	public Set<String> getImports() {
		return imports;
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.groovy.GroovyServiceMXBean#setImports(java.util.Set)
	 */
	@Override
	public void setImports(Set<String> imps) {
		if(imps!=null) {
			imps.removeAll(imports);
			if(imps.isEmpty()) return;
			if(this.isStarted()) {								
				applyImports(new ImportCustomizer(), imps.toArray(new String[imps.size()]));
				imports.addAll(imps);
			} else {
				imports.addAll(imps);
			}
		}
	}
	
	
	/**
	 * <p>Title: ThreadSafeNoNullsBinding</p>
	 * <p>Description: A binding extension that prevents nul value property and variable sets</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.apmrouter.groovy.GroovyService.ThreadSafeNoNullsBinding</code></p>
	 */
	protected class ThreadSafeNoNullsBinding extends Binding {
		/** The values as thread locals */
		protected final Map<String, InheritableThreadLocal<Object>> values = new HashMap<String, InheritableThreadLocal<Object>>();
		
		
		/**
		 * Creates a new ThreadSafeNoNullsBinding
		 */
		public ThreadSafeNoNullsBinding() {
			super();
		}

		/**
		 * Creates a new ThreadSafeNoNullsBinding
		 * @param variables The variables to add to the binding
		 */
		public ThreadSafeNoNullsBinding(Map<String, Object> variables) {
			super();
			if(variables!=null) {
				for(Map.Entry<String, Object> entry: variables.entrySet()) {
					InheritableThreadLocal<Object> t = new InheritableThreadLocal<Object>();
					t.set(entry.getValue());
					values.put(entry.getKey(), t);
				}
			}
		}

		/**
		 * Creates a new ThreadSafeNoNullsBinding
		 * @param args args to the binding
		 */
		public ThreadSafeNoNullsBinding(String[] args) {
			super();
			InheritableThreadLocal<Object> t = new InheritableThreadLocal<Object>();
			t.set(args);
			values.put("args", t);			
		}
		
		/**
		 * {@inheritDoc}
		 * @see groovy.lang.Binding#setProperty(java.lang.String, java.lang.Object)
		 */
		@Override
		public void setProperty(String key, Object value) {
			if(value==null) {
				log.error(String.format("Someone attempted to set a null value property into the groovy bindings [%s]:[%s]", key, value));
			} else {
				InheritableThreadLocal<Object> t = new InheritableThreadLocal<Object>();
				t.set(value);
				values.put(key, t);							
			}
		}
		
		/**
		 * {@inheritDoc}
		 * @see groovy.lang.Binding#setVariable(java.lang.String, java.lang.Object)
		 */
		@Override
		public void setVariable(String name, Object value) { 
			if(value==null) {
				log.error(String.format("Someone attempted to put a null value variable into the groovy bindings [%s]:[%s]", name, value));
			} else {
				InheritableThreadLocal<Object> t = new InheritableThreadLocal<Object>();
				t.set(value);
				values.put(name, t);							
			}
		}
		
		/**
		 * {@inheritDoc}
		 * @see groovy.lang.Binding#getVariable(java.lang.String)
		 */
		@Override
		public Object getVariable(String name) {
			InheritableThreadLocal<Object> t = values.get(name);
			if(t==null) throw new MissingPropertyException(name, this.getClass());
			return t.get();
		}
		
		/**
		 * {@inheritDoc}
		 * @see groovy.lang.Binding#getProperty(java.lang.String)
		 */
		@Override
		public Object getProperty(String key) {
			InheritableThreadLocal<Object> t = values.get(key);
			if(t==null) return null;
			return t.get();			
		}
		
		/**
		 * {@inheritDoc}
		 * @see groovy.lang.Binding#hasVariable(java.lang.String)
		 */
		@Override
		public boolean hasVariable(String name) {
			return values.containsKey(name);
		}
		
		/**
		 * {@inheritDoc}
		 * @see groovy.lang.Binding#getVariables()
		 */
		@Override
		public Map<String, Object> getVariables() {
			Map<String, Object> map = new HashMap<String, Object>(values.size());
			for(Map.Entry<String, InheritableThreadLocal<Object>> entry: values.entrySet()) {
				if(entry.getValue().get()!=null) {
					map.put(entry.getKey(), entry.getValue().get());
				}
			}
			return map;
		}
	}




}
