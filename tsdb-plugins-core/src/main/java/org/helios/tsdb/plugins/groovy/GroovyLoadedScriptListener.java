
package org.helios.tsdb.plugins.groovy;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

/**
 * <p>Title: GroovyLoadedScriptListener</p>
 * <p>Description: A listener that is notified when a groovy class matching the specified criteria is loaded</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.groovy.GroovyLoadedScriptListener</code></p>
 */
public interface GroovyLoadedScriptListener {
	/**
	* Returns a set of type annotations this listener is interested in
	* @return a set of type annotations this listener is interested in
	*/
	Set<Class<? extends Annotation>> getScanTypeAnnotations();
	
	/**
	* Callback to the listener when a groovy class matching the specified type annotations is loaded
	* @param annotations The annotations that were matched
	* @param clazz The class that was loaded
	* @param instance The instance of the class if the class was loaded as a result of a script compilation. Otherwise null.
	*/
	public void onScanType(Set<? extends Annotation> annotations, Class<?> clazz, Object instance);
	
	/**
	* Returns a set of method annotations this listener is interested in
	* @return a set of method annotations this listener is interested in
	*/
	Set<Class<? extends Annotation>> getScanMethodAnnotations();
	
	/**
	* Callback to the listener when a groovy class matching the specified method annotations is loaded
	* @param methods A map of sets of annotations that were matched keyed by the method they were found on
	* @param clazz The class that was loaded
	* @param instance The instance of the class if the class was loaded as a result of a script compilation. Otherwise null.
	*/
	public void onScanMethod(Map<Method, Set<Annotation>> methods, Class<?> clazz, Object instance);
	
	
	/**
	* Returns a set of classes that loaded classes might extend or implement that this listener is interested in
	* @return a set of classes that loaded classes might extend or implement that this listener is interested in
	*/
	public Set<Class<?>> getScanClasses();
	
	/**
	* Callback to the listener when a groovy class with the specified inherritance is loaded
	* @param parentClasses The parent types that were matched
	* @param clazz The class that was loaded
	* @param instance The instance of the class if the class was loaded as a result of a script compilation. Otherwise null.
	*/
	public void onScanClasses(Set<Class<?>> parentClasses, Class<?> clazz, Object instance);

}
