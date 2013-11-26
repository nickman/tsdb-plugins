/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2013, Helios Development Group and individual contributors
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
package org.helios.tsdb.plugins.groovy;

import java.util.Map;
import java.util.Set;

/**
 * <p>Title: GroovyServiceMXBean</p>
 * <p>Description: JMX MXBean interface for {@link GroovyService}.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.groovy.GroovyServiceMXBean</code></p>
 */

public interface GroovyServiceMXBean {

	/**
	 * Indicates if this service is started
	 * @return true if this service is started, false otherwise
	 */
	public abstract boolean isStarted();

	/**
	 * Flushes the compiled script cache
	 */
	public abstract void flushScriptCache();

	/**
	 * Removes the named script from the script cache
	 * @param name The name of the script to remove
	 */
	public abstract void flushScript(String name);

	/**
	 * Returns the groovy version
	 * @return the groovy version
	 */
	public abstract String getGroovyVersion();

	/**
	 * Returns the names of the cached compiled scripts
	 * @return the names of the cached compiled scripts
	 */
	public abstract String[] getScriptNames();

	/**
	 * Indicates if groovy is using reflection
	 * @return true if groovy is using reflection, false if using .... ?
	 */
	public abstract boolean isUseReflection();

	/**
	 * Invokes the named method in the named script and returns the value returned from the invocation
	 * @param name The name of the script to run
	 * @param methodName The name of the method to run
	 * @return the value returned from the script
	 */
	public abstract Object invoke(String name, String methodName);

	/**
	 * Compiles the passed source and assigns it the passed name
	 * @param scriptName The name assigned to the compiled script. If this is null, and no {@link ScriptName} annotation is found, a synthetic name will be assigned.
	 * @param source The source code of the script to compiled
	 * @return The name of the compiled script
	 */
	public abstract String compile(String scriptName, String source);

	/**
	 * Compiles the passed source and assigns it the passed name
	 * @param source The source code of the script to compiled
	 * @return The name of the compiled script
	 */
	public abstract String compile(String source);

	/**
	 * Sets a compiler optimization option
	 * @param name The name of the option
	 * @param value true to enable, false to disable
	 */
	public abstract void setOptimizationOption(String name, boolean value);

	/**
	 * Compiles the source read from the passed URL and assigns it the passed name
	 * @param sourceUrl The source code URL of the script to compiled
	 * @return The name of the compiled script
	 */
	public abstract String compileFromUrl(String sourceUrl);

	/**
	 * Executes the main function of the named script
	 * @param name The name of the script to execute
	 * @return the return value from the script invocation
	 */
	public abstract Object runScript(String name);

	/**
	 * Launches the groovy console, loading an empty console 
	 */
	public abstract void launchConsole();

	/**
	 * Launches the groovy console 
	 * @param fileName The name of the local file to load
	 */
	public abstract void launchConsole(String fileName);

	/**
	 * Returns the compiler warning level.
	 * Recognized values are: NONE:0, LIKELY_ERRORS:1, POSSIBLE_ERRORS:2, PARANOIA:3
	 * @return the compiler warning level
	 * @see org.codehaus.groovy.control.CompilerConfiguration#getWarningLevel()
	 */
	public abstract int getWarningLevel();

	/**
	 * Sets the compiler warning level.
	 * Recognized values are: NONE:0, LIKELY_ERRORS:1, POSSIBLE_ERRORS:2, PARANOIA:3
	 * @param level the compler warning level
	 * @see org.codehaus.groovy.control.CompilerConfiguration#setWarningLevel(int)
	 */
	public abstract void setWarningLevel(int level);

	/**
	 * Returns the compiler source encoding
	 * @return the compiler source encoding
	 * @see org.codehaus.groovy.control.CompilerConfiguration#getSourceEncoding()
	 */
	public abstract String getSourceEncoding();

	/**
	 * Sets the compiler source encoding
	 * @param encoding the compiler source encoding
	 * @see org.codehaus.groovy.control.CompilerConfiguration#setSourceEncoding(java.lang.String)
	 */
	public abstract void setSourceEncoding(String encoding);

	/**
	 * Returns the compiler target directory
	 * @return the compiler target directory
	 * @see org.codehaus.groovy.control.CompilerConfiguration#getTargetDirectory()
	 */
	public abstract String getTargetDirectory();

	/**
	 * Sets the compiler target directory
	 * @param directory the compiler target directory
	 * @see org.codehaus.groovy.control.CompilerConfiguration#setTargetDirectory(java.lang.String)
	 */
	public abstract void setTargetDirectory(String directory);

	/**
	 * Indicates if the compiler is verbose
	 * @return true if the compiler is verbose, false otherwise
	 * @see org.codehaus.groovy.control.CompilerConfiguration#getVerbose()
	 */
	public abstract boolean isVerbose();

	/**
	 * Sets the verbosity of the compiler
	 * @param verbose true to make verbose, false otherwise
	 * @see org.codehaus.groovy.control.CompilerConfiguration#setVerbose(boolean)
	 */
	public abstract void setVerbose(boolean verbose);

	/**
	 * Indicates if the compiler is in debug mode
	 * @return true if the compiler is in debug mode, false otherwise
	 * @see org.codehaus.groovy.control.CompilerConfiguration#getDebug()
	 */
	public abstract boolean isDebug();

	/**
	 * Sets the debug mode of the compiler
	 * @param debug true for debug, false otherwise
	 * @see org.codehaus.groovy.control.CompilerConfiguration#setDebug(boolean)
	 */
	public abstract void setDebug(boolean debug);

	/**
	 * Returns the compiler tolerance, which is the maximum number of non-fatal errors before compilation is aborted 
	 * @return the maximum number of non-fatal errors before compilation is aborted
	 * @see org.codehaus.groovy.control.CompilerConfiguration#getTolerance()
	 */
	public abstract int getTolerance();

	/**
	 * Sets the compiler tolerance, which is the maximum number of non-fatal errors before compilation is aborted
	 * @param tolerance the maximum number of non-fatal errors before compilation is aborted
	 * @see org.codehaus.groovy.control.CompilerConfiguration#setTolerance(int)
	 */
	public abstract void setTolerance(int tolerance);

	/**
	 * Returns the compiler's base script class
	 * @return the compiler's base script class
	 * @see org.codehaus.groovy.control.CompilerConfiguration#getScriptBaseClass()
	 */
	public abstract String getScriptBaseClass();

	/**
	 * Sets the compiler's base script class
	 * @param scriptBaseClass the compiler's base script class
	 * @see org.codehaus.groovy.control.CompilerConfiguration#setScriptBaseClass(java.lang.String)
	 */
	public abstract void setScriptBaseClass(String scriptBaseClass);

	/**
	 * Sets the compiler's minimum recompilation interval in seconds
	 * @param time the compiler's minimum recompilation interval in seconds
	 * @see org.codehaus.groovy.control.CompilerConfiguration#setMinimumRecompilationInterval(int)
	 */
	public abstract void setMinimumRecompilationInterval(int time);

	/**
	 * Returns the compiler's minimum recompilation interval in seconds
	 * @return the compiler's minimum recompilation interval in seconds
	 * @see org.codehaus.groovy.control.CompilerConfiguration#getMinimumRecompilationInterval()
	 */
	public abstract int getMinimumRecompilationInterval();

	/**
	 * Sets the compiler's target bytecode version
	 * @param version the compiler's target bytecode version
	 * @see org.codehaus.groovy.control.CompilerConfiguration#setTargetBytecode(java.lang.String)
	 */
	public abstract void setTargetBytecode(String version);

	/**
	 * Returns the compiler's target bytecode version
	 * @return the compiler's target bytecode version
	 * @see org.codehaus.groovy.control.CompilerConfiguration#getTargetBytecode()
	 */
	public abstract String getTargetBytecode();

	/**
	 * Returns the compiler's optimization options
	 * @return the compiler's optimization options
	 * @see org.codehaus.groovy.control.CompilerConfiguration#getOptimizationOptions()
	 */
	public abstract Map<String, Boolean> getOptimizationOptions();

	/**
	 * Sets the compiler's optimization options
	 * @param options the compiler's optimization options
	 * @see org.codehaus.groovy.control.CompilerConfiguration#setOptimizationOptions(java.util.Map)
	 */
	public abstract void setOptimizationOptions(Map<String, Boolean> options);

	/**
	 * Returns the currently configured compiler imports
	 * @return the currently configured compiler imports
	 */
	public abstract Set<String> getImports();

	/**
	 * Adds the passed imports to the configured compiler imports
	 * @param imps the imports to add
	 */
	public abstract void setImports(Set<String> imps);

}