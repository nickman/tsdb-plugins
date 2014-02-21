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

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Title: FileHelper</p>
 * <p>Description: Some static helper functions for file system ops</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>test.net.opentsdb.search.util.FileHelper</code></p>
 */

public class FileHelper {
	/** An empty file array */
	public static final File[] EMPTY_FILE_ARR = new File[0];
	/** An empty file set */
	public static final Set<File> EMPTY_FILE_SET = Collections.emptySet();
	/** A regex path splitter pattern */
	public static final Pattern PATH_SPLITTER = Pattern.compile(File.pathSeparator);

	/** Static class logger */
	private static final Logger LOG = LoggerFactory.getLogger(FileHelper.class);
	
	/** The user home directory */
	public static final File USER_HOME = new File(System.getProperty("user.home"));
	/** The user directory */
	public static final File USER_DIR = new File(System.getProperty("user.dir"));
	/** The Java Home */
	public static final File JAVA_HOME = new File(System.getProperty("java.home"));
	
	/** A set of the file system roots */
	public static final Set<File> ROOTS = Collections.unmodifiableSet(new HashSet<File>(Arrays.asList(File.listRoots())));
	
	/** A set of the directories in the boot classpath */
	public static final Set<File> JAVA_BOOT_DIRS = Collections.unmodifiableSet(filesFromPath(System.getProperty("sun.boot.class.path"), false));
	/** A set of the directories in the java ext */
	public static final Set<File> JAVA_EXT_DIRS = Collections.unmodifiableSet(filesFromPath(System.getProperty("java.ext.dirs"), false));
	/** A set of the directories in the java lib */
	public static final Set<File> JAVA_LIB_DIRS = Collections.unmodifiableSet(filesFromPath(System.getProperty("java.library.path"), false));
	/** A set of the directories in the java endorsed dirs */
	public static final Set<File> JAVA_ENDORSED_DIRS = Collections.unmodifiableSet(filesFromPath(System.getProperty("java.endorsed.dirs"), false));
	/** A set of the directories in the java boot lib */
	public static final Set<File> JAVA_BOOT_LIB_DIRS = Collections.unmodifiableSet(filesFromPath(System.getProperty("sun.boot.library.path"), false));
	/** A set of the directories in the path */
	public static final Set<File> PATH_DIRS = Collections.unmodifiableSet(filesFromPath(System.getenv("PATH"), false));
	
	
	
//	public static void main(String[] args) {
//		try { Class.forName(FileHelper.class.getName(), true, FileHelper.class.getClassLoader()); } catch (Exception ex) {}
//		LOG.info("All Protected Files Size: {}", ALL_PROTECTED_FILES.size());
//		LOG.info("Gnuplot in path ?: {}", isFileInPath("gnuplot.exe" , System.getenv("PATH")));
//	LOG.info("PATH DIRS: {}", PATH_DIRS);
//	}

	/** A set of the files and directories in the java classpath */
	public static final Set<File> JAVA_CLASSPATH_FILES = Collections.unmodifiableSet(filesFromPath(System.getProperty("java.class.path")));
	/** A set of all the protected files and directories */
	@SuppressWarnings("unchecked")
	public static final Set<File> ALL_PROTECTED_FILES = aggregate(ROOTS, JAVA_BOOT_DIRS, JAVA_EXT_DIRS, JAVA_LIB_DIRS, JAVA_ENDORSED_DIRS, JAVA_BOOT_LIB_DIRS, JAVA_CLASSPATH_FILES);
	
	
	/**
	 * Splits the passed path and returns a set of the validated files therein.
	 * @param paths The path statement
	 * @param filesOnly If true, returns only files, If false, returns only directories, ignored if null
	 * @return A set of files extracted from the passed path
	 */
	public static Set<File> filesFromPath(String paths, Boolean filesOnly) {
		Set<File> files = new HashSet<File>();
		if(paths==null || paths.trim().isEmpty()) return EMPTY_FILE_SET;
		for(String path: PATH_SPLITTER.split(paths)) {
			File f = new File(path.trim());
			if(f.exists()) {
				if(filesOnly!=null) {
					if(filesOnly) {
						if(f.isFile()) files.add(f);
					} else {
						if(f.isDirectory()) files.add(f);
					}
				} else {
					files.add(f);
				}
			}
		}		
		return files;
	}
	
	/**
	 * Aggregates all the passed file sets and returns a single set containing all
	 * @param sets The file sets to aggregate
	 * @return the aggregated file set
	 */
	public static Set<File> aggregate(Set<File>...sets) {
		int x = 0;
		for(Set<File> set: sets) {
			if(set==null) continue;
			x += set.size();
		}
		Set<File> files = new HashSet<File>(x);
		for(Set<File> set: sets) {
			if(set==null) continue;
			files.addAll(set);
		}
		return files;
	}

	/**
	 * Splits the passed path and returns a set of the validated files (and directories) therein.
	 * @param paths The path statement
	 * @return A set of files extracted from the passed path
	 */
	public static Set<File> filesFromPath(String paths) {
		return filesFromPath(paths, null);
	}
	/**
	 * Purges the passed file. If it is a regular file, it is simply deleted. 
	 * If it is a directory, it is recursively deleted.
	 * @param root The file to purge
	 */
	public static void deltree(File root) {
		if(root==null || !root.exists()) return;
		if(root.isDirectory()) {
			for(File subRoot: root.listFiles()) {
				if(subRoot.isFile()) {
					subRoot.delete();
					LOG.info("Deleted file [{}]", subRoot.getAbsolutePath());
				} else {
					deltree(subRoot);					
				}
			}
			root.delete();
			LOG.info("Deleted directory [{}]", root.getAbsolutePath());
		} else {
			root.delete();
			LOG.info("Deleted file [{}]", root.getAbsolutePath());
		}
	}
	
	/**
	 * If the passed file is an existing directory, it will be emptied
	 * @param dir The directory to empty
	 */
	public static void emptyDir(File dir) {
		if(dir==null || !dir.exists() || !dir.isDirectory()) return;
		if(ALL_PROTECTED_FILES.contains(dir)) throw new RuntimeException("The directory [" + dir + "] is protected");
		for(File subRoot: dir.listFiles()) {
			if(subRoot.isDirectory()) {
				deltree(subRoot);
			} else {
				subRoot.delete();
				LOG.info("Deleted file [{}]", subRoot.getAbsolutePath());
			}
		}
	}

	/**
	 * Searches all the directories in the passed path to find the passed file name
	 * @param fileName The file name to search for
	 * @param paths A string of path separator delimited paths
	 * @return true if the file was found, false otherwise
	 */
	public static boolean isFileInPath(String fileName, String paths) {
		for(File dir: filesFromPath(paths, false)) {
			if(new File(dir, fileName).exists()) return true;
		}
		return false;
	}
	
	private FileHelper() {
	}
	
	
}
