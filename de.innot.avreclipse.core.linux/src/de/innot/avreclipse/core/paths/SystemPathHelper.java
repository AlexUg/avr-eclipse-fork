/*******************************************************************************
 * Copyright (c) 2008, 2011 Thomas Holland (thomas@innot.de) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Thomas Holland - initial API and implementation
 *******************************************************************************/

package de.innot.avreclipse.core.paths;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;

import org.eclipse.cdt.utils.spawner.ProcessFactory;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;

import de.innot.avreclipse.AVRPlugin;
import de.innot.avreclipse.core.paths.AVRPath;
import de.innot.avreclipse.core.paths.AbstractSystemPathHelper;

/**
 * Gets the actual system paths to the AVR-GCC Toolchain and some config files.
 * 
 * As these path can be almost everywhere (or not exist at all), this class tries to get the
 * location with the following methods:
 * <ol>
 * <li><code>which</code> command to look in the current $PATH</li>
 * <li><code>find</code> command to search certain parts of the filesystem. Currently the
 * following paths are checked (in this order)
 * <ul>
 * <li><code>/usr/bin</code></li>
 * <li><code>/usr/lib</code></li>
 * <li><code>disabled /usr/</code></li>
 * <li><code>/opt/</code></li>
 * <li><code>/usr/local/bin</code></li>
 * <li><code>/usr/local/lib</code></li>
 * <li><code>/usr/local/</code></li>
 * <li><code>~/</code></li>
 * <li><code>disabled /home</code></li>
 * <li><code>/etc/</code></li>
 * </ul>
 * </li>
 * </ol>
 * <p>
 * Finding a path can be quite expensive on large systems. Therefore the caller ({@link AbstractSystemPathHelper})
 * should cache any paths found.
 * </p>
 * 
 * @author Thomas Holland
 * @since 2.1
 */
public class SystemPathHelper extends AbstractSystemPathHelper {

	/** Paths to be searched in order. */
	// /etc/ was used to find the avrdude.conf file. While this is currently not
	// required I leave it in just in case we will be looking for some other
	// configuration file in a future version of the plugin.
	// TODO Perhaps we can make this list configurable. Volunteers?
	private final static String[]	fSearchPaths	= {
		"/usr/bin",
		"/usr/lib",
		"/usr/avr", // Arch Linux, Fedora
		"/opt/",
		"/usr/local/bin",
		"/usr/local/lib",
		"/usr/local/",
		System.getProperty("user.home"),
		"/etc/",
	};

	public SystemPathHelper() {
		// prevent instantiation
	}

	/**
	 * Find the system path for the given {@link AVRPath} enum value.
	 * 
	 * @param avrpath
	 * @return a valid path or <code>null</code> if no path could be found.
	 */
	@Override
	public IPath findSystemPath(AVRPath avrpath) {

		IPath path = fEmptyPath;
		String test = avrpath.getTest();
		if (avrpath.isExecutable()) {
			path = which(test);
		}
		if (path.isEmpty()) {
			path = find(test);
		}
		if (!path.isEmpty()) {
			// remove the number of segments of the test from
			// the path. This makes a test like "avr/io.h" work
			path = path.removeLastSegments(new Path(test).segmentCount());
		}
		return path;
	}

	/**
	 * Use the posix 'which' command to find the given file.
	 * 
	 * @param file
	 *            Name of the file
	 * @return <code>IPath</code> to the file. May be an empty path if the file could not be found
	 *         with the 'which' command.
	 */
	private IPath which(String file) {

		IPath path = executeCommand("which", file);
		return path;
	}

	/**
	 * Use the posix 'find' command to find the given file.
	 * <p>
	 * This method will search the paths in the order given by the {@link #fSearchPaths} array of
	 * path names.
	 * </p>
	 * 
	 * @param file
	 *            Name of the file
	 * @return <code>IPath</code> to the file. May be an empty path if the file could not be found
	 *         with the 'find' command.
	 */
	private IPath find(String file) {

		for (String findpath : fSearchPaths) {
			// TODO: use -ipath instead of -path to be case insensitive.
			// -ipath is a GNU extension to the Posix find, so this might not be as
			// compatible across all platforms. For the time we leave
			// -path until someone complains.
			IPath testpath;
			if (file.indexOf(File.separatorChar) >= 0) {
				testpath = executeCommand("find", findpath, "-path", "*/" + file);
			} else {
				testpath = executeCommand("find", findpath, "-name", file);
			}
			if (!testpath.isEmpty()) {
				return testpath;
			}
		}

		// nothing found: return an empty path
		return fEmptyPath;

	}

	/**
	 * Execute the given command and read its output until a line with a valid path is found, which
	 * is returned.
	 * 
	 * @param command
	 * @return A valid <code>IPath</code> or an empty path if the command did not return a valid
	 *         path.
	 */
	public IPath executeCommand(String... command) {

		IPath path = fEmptyPath;

		Process cmdproc = null;
		InputStream is = null;
		InputStreamReader isr = null;
		BufferedReader br = null;

		try {
			cmdproc = ProcessFactory.getFactory().exec(command);
			
			is = cmdproc.getInputStream();
			isr = new InputStreamReader(is);
			br = new BufferedReader(isr);
			
			String line;

			while ((line = br.readLine()) != null) {
				if (line.length() > 1) {
					// non-empty line should have the path + file
					if (path.isValidPath(line)) {
						path = new Path(line);
						break;
					}
				}
			}
			
		} catch (IOException ex) {
			AVRPlugin.getDefault().getLog().log(new Status(IStatus.ERROR, AVRPlugin.PLUGIN_ID, "Error while executing command: " + command, ex));
		} finally {
			try {
				if (br != null)
					br.close();
				if (isr != null)
					isr.close();
				if (is != null)
					is.close();
			} catch (IOException e) {
				// can't do anything about it
			}
			try {
				if (cmdproc != null) {
					cmdproc.waitFor();
				}
			} catch (InterruptedException e) {
			}
		}

		return path;
	}

}
