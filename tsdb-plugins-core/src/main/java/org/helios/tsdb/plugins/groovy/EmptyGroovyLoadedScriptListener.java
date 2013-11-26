
package org.helios.tsdb.plugins.groovy;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * <p>Title: EmptyGroovyLoadedScriptListener</p>
 * <p>Description: An empty {@link GroovyLoadedScriptListener} for extending</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.groovy.EmptyGroovyLoadedScriptListener</code></p>
 */
public class EmptyGroovyLoadedScriptListener implements GroovyLoadedScriptListener {

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
	 * @see org.helios.tsdb.plugins.groovy.GroovyLoadedScriptListener#onScanType(java.util.Set, java.lang.Class, java.lang.Object)
	 */
	@Override
	public void onScanType(Set<? extends Annotation> annotations, Class<?> clazz, Object instance) {
		
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.groovy.GroovyLoadedScriptListener#getScanMethodAnnotations()
	 */
	@Override
	public Set<Class<? extends Annotation>> getScanMethodAnnotations() {
		return Collections.emptySet();		
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.groovy.GroovyLoadedScriptListener#onScanMethod(java.util.Map, java.lang.Class, java.lang.Object)
	 */
	@Override
	public void onScanMethod(Map<Method, Set<Annotation>> methods, Class<?> clazz, Object instance) {
		
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
	 * @see org.helios.tsdb.plugins.groovy.GroovyLoadedScriptListener#onScanClasses(java.util.Set, java.lang.Class, java.lang.Object)
	 */
	@Override
	public void onScanClasses(Set<Class<?>> parentClasses, Class<?> clazz, Object instance) {
		
	}



}
