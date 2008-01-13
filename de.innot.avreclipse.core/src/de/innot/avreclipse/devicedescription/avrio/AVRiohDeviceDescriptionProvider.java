/*******************************************************************************
 * 
 * Copyright (c) 2007 Thomas Holland (thomas@innot.de) and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the GNU Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Thomas Holland - initial API and implementation
 *     Manuel Stahl - original idea to parse the <avr/io.h> file and the patterns
 *     
 * $Id: AVRiohDeviceDescriptionProvider.java 14 2007-11-27 12:02:05Z thomas $
 *     
 *******************************************************************************/
package de.innot.avreclipse.devicedescription.avrio;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;

import de.innot.avreclipse.AVRPluginActivator;
import de.innot.avreclipse.devicedescription.ICategory;
import de.innot.avreclipse.devicedescription.IDeviceDescription;
import de.innot.avreclipse.devicedescription.IDeviceDescriptionProvider;
import de.innot.avreclipse.devicedescription.IEntry;
import de.innot.avreclipse.devicedescription.IProviderChangeListener;
import de.innot.avreclipse.ui.preferences.PreferenceConstants;

/**
 * Provides DeviceDescription Objects based on parsing the <avr/io.h> file.
 * <p>
 * As the information in the include/avr folder is static, the class should be
 * accessed with the static method {@link #getDefault()}.
 * </p>
 * <p>
 * <b>Note:</b> The Registers defined in <avr/io.h>, namely the SREG and
 * SP(L|H) are not included, as parsing io.h would require an understanding of
 * <code>#ifdef</code>, which the simple parser in this class has not.
 * </p>
 * 
 * @author Thomas Holland
 * @author Manuel Stahl
 */
/**
 * @author U043192
 * 
 */
/**
 * @author U043192
 * 
 */
public class AVRiohDeviceDescriptionProvider implements IDeviceDescriptionProvider, IPropertyChangeListener {

	final static String DDP_NAME = "<avr/io.h>";

	private static AVRiohDeviceDescriptionProvider instance = null;

	private Map<String, String> devices = null;
	private Map<String, DeviceDescription> fCache = null;

	private String fInternalErrorMsg = null;

	private List<IProviderChangeListener> fChangeListeners = new ArrayList<IProviderChangeListener>(
	        0);

	/**
	 * Get an instance of this DeviceModelProvider.
	 */
	public static AVRiohDeviceDescriptionProvider getDefault() {
		if (instance == null)
			instance = new AVRiohDeviceDescriptionProvider();
		return instance;
	}

	/**
	 * private default constructor, so the class can only be accessed via the
	 * singleton getDefault() method.
	 * 
	 * The Constructor will register a Preference change listener to be informed
	 * about any changes to the <avr/io.h> path preference value.
	 */
	private AVRiohDeviceDescriptionProvider() {
		// Add ourself as a listener to Preference change events
		IPreferenceStore store = AVRPluginActivator.getDefault().getPreferenceStore();
		store.addPropertyChangeListener(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.innot.avreclipse.devicedescription.IDeviceDescriptionProvider#getName()
	 */
	public String getName() {
		return DDP_NAME;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.innot.avreclipse.devicedescription.IDeviceDescriptionProvider#getDeviceList()
	 */
	public List<String> getDeviceList() {
		if (devices == null) {
			try {
				loadDevices();
			} catch (IOException ioe) {
				return null;
			}
		}
		List<String> devs = new ArrayList<String>(devices.keySet());
		Collections.sort(devs);
		return devs;

	}

	public IDeviceDescription getDevice(String name) {
		if (name == null)
			return null;

		if (devices == null) {
			try {
				loadDevices();
			} catch (IOException ioe) {
				// return null on errors
				return null;
			}
		}

		// check if the name actually exists (and has a headerfile to load its
		// properties from)
		String headerfile = devices.get(name);
		if (headerfile == null)
			return null;

		// Test if we already have this device in the cache
		if (fCache == null) {
			fCache = new HashMap<String, DeviceDescription>();
		}
		DeviceDescription currdev = fCache.get(name);
		if (currdev == null) {
			// No: create a new DeviceDescription
			currdev = new DeviceDescription(name);

			try {
				readDeviceHeader(currdev, headerfile);
			} catch (IOException e) {
				return null;
			}
			// Add the DeviceDescription to the cache
			fCache.put(name, currdev);
		}

		return currdev;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.innot.avreclipse.devicedescription.IDeviceDescriptionProvider#getBasePath()
	 */
	public IPath getBasePath() {
		return new Path(getAVRIncludePath());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.innot.avreclipse.devicedescription.IDeviceDescriptionProvider#getErrorMessage()
	 */
	public String getErrorMessage() {
		return fInternalErrorMsg + " Check the preferences for a correct path setting";
	}

	/**
	 * Initialize the list of devices by opening the <avr/io.h> file and parsing
	 * it for all defined MCUs.
	 * 
	 * throws IOException if there was an error opening or reading the file.
	 */
	private void loadDevices() throws IOException {

		BufferedReader in = null;
		try {
			in = new BufferedReader(new FileReader(getAVRiohFile()));
		} catch (FileNotFoundException fnfe) {
			fInternalErrorMsg = "Cannot find <avr/io.h> (looked here: " + getAVRiohFile() + ").";
			throw fnfe;
		}

		devices = new HashMap<String, String>();
		String curDev = null;

		String line;
		Pattern defPat = Pattern.compile("^#(el)??if defined \\(__AVR_(.*)__\\)");
		Pattern incPat = Pattern.compile("^#  include <(.*)>");
		Matcher m;
		try {
			while ((line = in.readLine()) != null) {
				m = defPat.matcher(line);
				if (m.matches()) {
					// save the name
					curDev = m.group(2);
				}
				m = incPat.matcher(line);
				if (m.matches() && curDev != null) {

					devices.put(curDev, m.group(1));
				}
			}
		} catch (IOException ioe) {
			fInternalErrorMsg = "Cannot read <avr/io.h> (" + getAVRiohFile() + ").";
			throw ioe;
		}
		fInternalErrorMsg = null;
	}

	private void readDeviceHeader(DeviceDescription device, String headerfile) throws IOException {
		String line;

		device.addHeaderFile(headerfile);

		BufferedReader in;
		
		File hfile = new File(getAVRIncludePath() + "/" + headerfile);

		try {
			// Open a reader on the given header file and fail
			// if it can't be opened
			in = new BufferedReader(new FileReader(hfile));
		} catch (FileNotFoundException fnfe) {
			fInternalErrorMsg = "Cannot open source header file \"" + hfile.getAbsolutePath() + "\".";
			throw fnfe;
		}

		Pattern incPat = Pattern.compile("^#include <(.+)>.*");
		Pattern descPat = Pattern.compile("/\\* (?:RegDef\\:  )?(.+) \\*/.*");

		Pattern ivecPatNew = Pattern.compile("^#define ([A-Z0-9_]+_vect)\\s+_VECTOR\\((\\d+)\\).*");
		Pattern ivecPatOld = Pattern.compile("^#define (SIG_[A-Z0-9_]+)\\s+_VECTOR\\((\\d+)\\).*");
		Pattern portPat = Pattern
		        .compile("^#define ((?:PORT|PIN|DDR)[A-Z])\\s+_SFR_IO(\\d+)\\s*\\((0[xX].*)\\).*");
		Pattern regPat = Pattern
		        .compile("^#define ([A-Z0-9]+)\\s+_SFR_(IO|MEM)(\\d+)\\s*\\((0[xX].*)\\).*");

		Matcher m;

		List<ICategory> categories = device.getCategories();
		ICategory regCategory = categories.get(0);
		ICategory portCategory = categories.get(1);
		ICategory ivecCategory = categories.get(2);

		// Stores the last comment in the source code
		String activeDesc = "";

		try {
			while ((line = in.readLine()) != null) {

				// Test if current line contains an #include directive
				m = incPat.matcher(line);
				if (m.matches()) {
					// Yes: Recurse into this file if its not <avr/sfr_defs.h>
					// or
					// outside the <avr/*> directory
					// (<avr/sfr_defs.h> because this file has some comments
					// that
					// are picked up as
					// register definitions)
					String incfilename = m.group(1);
					if (!("avr/sfr_defs.h").equals(incfilename)) {
						if (incfilename.startsWith("avr")) {
							readDeviceHeader(device, m.group(1));
						}
					}
					continue;
				}

				// Test if current line contains a descriptive comment
				m = descPat.matcher(line);
				if (m.matches()) {
					// Yes: remember it and add it as a description to all
					// following
					// items
					activeDesc = m.group(1);
					continue;
				}

				if (line.trim().equals("")) {
					// but don't carry activeDesc over empty lines
					activeDesc = "";
					continue;
				}

				// Test if current line defines a Interrupt vector (new style)
				m = ivecPatNew.matcher(line);
				if (m.matches()) {
					// Yes: Add it to the interrupt vectors list
					InterruptVector ivec = null;
					String name = m.group(1);
					String vector = m.group(2);
					// test if an ivec with the old style name has already been
					// created
					List<IEntry> children = ivecCategory.getChildren();
					if (children != null) {
						for (IEntry child : children) {
							if (child.getColumnData(IVecsCategory.IDX_VECTOR).equals(vector)) {
								ivec = (InterruptVector) child;
								break;
							}
						}
					}
					if (ivec == null) {
						ivec = new InterruptVector(ivecCategory);
					}
					ivec.setName(name);
					ivec.setDescription(activeDesc);
					ivec.setVector(vector);
					continue;
				}

				// Test if current line defines a Interrupt vector (old style)
				m = ivecPatOld.matcher(line);
				if (m.matches()) {
					// Yes: Add it to the interrupt vectors list (but only as
					// SIGname)
					InterruptVector ivec = null;
					String signame = m.group(1);
					String vector = m.group(2);
					// test if an ivec with the new style name has already been
					// created (same vector number)
					List<IEntry> children = ivecCategory.getChildren();
					if (children != null) {
						for (IEntry child : children) {
							if (child.getColumnData(IVecsCategory.IDX_VECTOR).equals(vector)) {
								ivec = (InterruptVector) child;
								break;
							}
						}
					}
					if (ivec == null) {
						ivec = new InterruptVector(ivecCategory);
					}
					ivec.setSIGName(signame);
					ivec.setDescription(activeDesc);
					ivec.setVector(vector);
					continue;
				}

				// Test if current line defines a Port Register
				m = portPat.matcher(line);
				if (m.matches()) {
					// Yes: Add it to the Ports list
					Port port = new Port(portCategory);
					port.setName(m.group(1));
					port.setDescription(activeDesc);
					port.setBits(m.group(2));
					port.setAddr(m.group(3));
					continue;
				}

				// Test if current line defines a Register
				m = regPat.matcher(line);
				if (m.matches()) {
					// Yes: Add it to the Register list
					Register register = new Register(regCategory);
					register.setName(m.group(1));
					register.setDescription(activeDesc);
					register.setAddrType(m.group(2));
					register.setBits(m.group(3));
					register.setAddr(m.group(4));
				}
			}
		} catch (IOException ioe) {
			fInternalErrorMsg = "Cannot read source header file \"" + hfile.getAbsolutePath() + "\".";
			throw ioe;
		}
		fInternalErrorMsg = null;

	}

	/**
	 * Retrieves the path to the avr/io.h file from the plugin preferences.
	 * 
	 * @return String containing the path
	 */
	protected String getAVRiohFile() {
		IPreferenceStore store = AVRPluginActivator.getDefault().getPreferenceStore();
		String avr_io_h = store.getString(PreferenceConstants.PREF_DEVICEVIEW_AVR_IO_H);
		return avr_io_h;
	}

	/**
	 * Retrieves the path to the include/avr directory from the Plugin
	 * preferences.
	 * 
	 * @return String containing the path
	 */
	private String getAVRIncludePath() {

		// get the path to the io.h file and remove the last two segments (
		// "io.h" and "avr")
		// to get the path of the include directory.
		String avr_io_h = getAVRiohFile();
		if ((avr_io_h != null) && (avr_io_h.length() > 0)) {
			IPath avr_io_h_path = new Path(avr_io_h);
			avr_io_h_path = avr_io_h_path.removeLastSegments(2);
			return avr_io_h_path.toOSString();
		}
		return null;

	}

	public void propertyChange(PropertyChangeEvent event) {
		// check if the path to the <avr/io.h> file has changed
		if (PreferenceConstants.PREF_DEVICEVIEW_AVR_IO_H.equals(event.getProperty())) {
			// Yes: reset the devicelist and fire an event to all of our
			// own listeners
			devices = null;
			fireProviderChangeEvent();
		}
	}

	private void fireProviderChangeEvent() {
		for (IProviderChangeListener pcl : fChangeListeners) {
			if (pcl != null) {
				pcl.providerChange();
			}
		}
	}

	public void addProviderChangeListener(IProviderChangeListener pcl) {
		if (pcl != null && !(fChangeListeners.contains(pcl))) {
			fChangeListeners.add(pcl);
		}
	}

	public void removeProviderChangeListener(IProviderChangeListener pcl) {
		if (fChangeListeners.contains(pcl)) {
			fChangeListeners.remove(pcl);
		}
	}
}