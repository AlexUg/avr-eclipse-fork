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

public enum AVRPath {
	// The compiler
	AVRGCC(true, true, "AVR-GCC",
			"Directory containing 'avr-gcc' and the other tools of the AVR-GCC toolchain",
			"avr-gcc"),

	// Make
	MAKE(true, true, "GNU make", "Directory containing 'make' executable", "make"),

	// The avr header files
	AVRINCLUDE(true, false, "AVR Header Files", "Directory containing 'avr/io.h' include file", "avr/io.h"),

	// AVRDude executable
	AVRDUDE(false, true, "AVRDude", "Directory containing 'avrdude' executable", "avrdude"),
	
	ARDUINO(false, false, "Arduino", "Directory containing 'boards.txt' configuration file", "boards.txt"),

	// AVRDude config is not used - We get the path from AVRDude itself
	// AVRDUDECONFIG(false, "AVRDude.conf", "Directory containing 'avrdude.conf' configuration
	// file", "avrdude.conf"),

	// Atmel part description files
	PDFPATH(false, false, "Atmel Part Description Files",
			"(currently unused) Directory containing the Atmel Part Description Files",
			"ATmega16.xml");

	private boolean	fRequired;
	private boolean	fExecutable;
	private String	fName;
	private String	fDescription;
	private String	fTest;
	
	private AVRPathManager fPathManager;

	/**
	 * Default Enum constructor. Sets the internal fields according to the selected enum value.
	 */
	AVRPath(boolean required, boolean executable, String name, String description, String test) {
		fRequired = required;
		fExecutable = executable;
		fName = name;
		fDescription = description;
		fTest = test;
		fPathManager = new AVRPathManager(this);
	}

	public String getDescription() {
		return fDescription;
	}

	public String getName() {
		return fName;
	}

	public boolean isOptional() {
		return !fRequired;
	}

	public boolean isExecutable() {
		return fExecutable;
	}

	public String getTest() {
		return fTest;
	}

	@Override
	public String toString() {
		return fName;
	}

	public AVRPathManager getPathManager() {
		return fPathManager;
	}
}
