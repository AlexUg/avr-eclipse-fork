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
/****************************************************************************
 * 
 * AVRPlugin.java
 * 
 * This file is part of AVR Eclipse Plugin.
 *
 ****************************************************************************/

/* $Id$ */

package de.innot.avreclipse;

import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import de.innot.avreclipse.core.arduino.AbstractArduinoHelper;
import de.innot.avreclipse.core.arduino.ArduinoBoards;
import de.innot.avreclipse.core.paths.AVRPath;
import de.innot.avreclipse.core.paths.AbstractSystemPathHelper;
import de.innot.avreclipse.core.targets.ToolManager;

/**
 * The activator class controls the plug-in life cycle
 */
public class AVRPlugin extends Plugin {

	// The plug-in ID
	public static final String	PLUGIN_ID		= "de.innot.avreclipse.core";
	public static final String	DEFAULT_CONSOLE	= "AVR Eclipse Plugin Log";
	
	public static final String	SYSTEMPATHHELPER_ID		= "systemPathHelper";
	public static final String	SYSTEMPATHELEMENT_ID	= "pathHelper";
	public static final String	SYSTEMPATHHELPER_TYPE	= "SystemPathHelper";

	public static final String	ARDUINOHELPER_ID		= "arduinoHelper";
	public static final String	ARDUINOELEMENT_ID		= "arduinoHelper";
	public static final String	ARDUINOHELPER_TYPE		= "ArduinoHelper";

	// The shared instance
	private static AVRPlugin	plugin;
	
	private AbstractSystemPathHelper systemPathHelper;
	
	private AbstractArduinoHelper arduinoHelper;

	/**
	 * The constructor
	 */
	public AVRPlugin() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;

		// Rescan all system paths (unless the "No startup scan" flag has been set
		// AVRPathsPreferences.scanAllPaths();

		ToolManager toolmanager = ToolManager.getDefault();
		String[] extpoints = toolmanager.getExtensionPointIDs();
		for (String ext : extpoints) {
			Platform.getExtensionRegistry().addListener(toolmanager, ext);
		}

	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	@Override
	public void stop(BundleContext context) throws Exception {
		ToolManager toolmanager = ToolManager.getDefault();
		Platform.getExtensionRegistry().removeListener(toolmanager);

		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 * 
	 * @return the shared instance
	 */
	public static AVRPlugin getDefault() {
		return plugin;
	}

	/**
	 * Log the given status and print it to the err Stream if debugging is enabled.
	 * 
	 * @param status
	 */
	public void log(IStatus status) {
		ILog log = getLog();
		if (status.getSeverity() >= Status.WARNING) {
			log.log(status);
		}
		if (isDebugging()) {
			System.err.print(PLUGIN_ID + ": " + status.getMessage());
			if (status.getCode() != 0) {
				System.err.print("(" + status.getCode() + ")");
			}
			System.out.println("");
			if (status.getException() != null) {
				status.getException().printStackTrace();
			}
		}
	}

	/**
	 * Gets the default console for this plugin.
	 * <p>
	 * This console is used for logging internal information, like the output of the external tools
	 * called internally (not by user interaction). This Console is only for background information
	 * and debugging and should never be actively brought to the front.
	 * </p>
	 * 
	 * @return The default <code>MessageConsole</code>
	 */
	public MessageConsole getDefaultConsole() {
		return getConsole(DEFAULT_CONSOLE);
	}

	/**
	 * Gets the console with the given name.
	 * <p>
	 * This is a convenience method to get a console with the given name.
	 * </p>
	 * <p>
	 * If the console already exists, a reference is returned, otherwise a new
	 * <code>MessageConsole</code> with the given name is created, added to the ConsoleManager, and
	 * returned.
	 * </p>
	 * 
	 * @param name
	 *            The name of the Console
	 * @return A <code>MessageConsole</code> with the given name, 
	 * or <code>null</code> if running in headless mode
	 */
	public MessageConsole getConsole(String name) {
		
		// Check if we are running headless (JUnit Test e.g.)
		// If Headless we return null
		Bundle b = Platform.getBundle("org.eclipse.ui"); 
		if ((b==null) || (b.getState() != Bundle.ACTIVE) || (! PlatformUI.isWorkbenchRunning())) {
			return null;
		} 
		
		// Get a list of all known Consoles from the Global Console Manager and
		// see if a Console with the given name already exists.
		IConsoleManager conman = ConsolePlugin.getDefault().getConsoleManager();
		IConsole[] allconsoles = conman.getConsoles();
		for (IConsole console : allconsoles) {
			if (console.getName().equals(name)) {
				return (MessageConsole) console;
			}
		}
		// Console not found - create a new one
		MessageConsole newconsole = new MessageConsole(name, null);
		conman.addConsoles(new IConsole[] { newconsole });
		return newconsole;
	}
	
	public AbstractSystemPathHelper getSystemPathHelper() {
		if (systemPathHelper == null) {
			systemPathHelper = createHelper(SYSTEMPATHHELPER_ID, SYSTEMPATHELEMENT_ID, SYSTEMPATHHELPER_TYPE);
		}
		if (systemPathHelper == null) {
			systemPathHelper = new AbstractSystemPathHelper() {
				
				@Override
				protected IPath findSystemPath(AVRPath avrpath) {
					return new Path("");
				}
			};
		}
		return systemPathHelper;
	}
	
	public AbstractArduinoHelper getArduinoHelper() {
		if (arduinoHelper == null) {
			arduinoHelper = createHelper(ARDUINOHELPER_ID, ARDUINOELEMENT_ID, ARDUINOHELPER_TYPE);
		}
		if (arduinoHelper == null) {
			arduinoHelper = new AbstractArduinoHelper() {
				
				@Override
				public List<String> findArduinoPorts(ArduinoBoards boards, String boardId) {
					return Collections.emptyList();
				}
			};
		}
		return arduinoHelper;
	}
	
	@SuppressWarnings("unchecked")
	private <T> T createHelper(String extensionPointId, String elementId, String type) {
		IExtensionPoint extensionPoint = Platform.getExtensionRegistry().getExtensionPoint(PLUGIN_ID, extensionPointId);
		for (IConfigurationElement elm : extensionPoint.getConfigurationElements()) {
			if (elementId.equals(elm.getName())) {
				try {
					return (T) elm.createExecutableExtension("class");
				} catch (Exception ex) {
					getLog().log(new Status(IStatus.ERROR, PLUGIN_ID, "Error while creating " + type, ex));
				}
			}
		}
		IStatus status = new Status(IStatus.ERROR, PLUGIN_ID, "Error while creating " + type
																+ ". No extensions are registered.");
		getLog().log(status);
		return null;
	}
}
