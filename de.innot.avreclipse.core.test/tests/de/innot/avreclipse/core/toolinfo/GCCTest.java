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
/**
 * 
 */
package de.innot.avreclipse.core.toolinfo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.junit.Before;
import org.junit.Test;

/**
 * @author U043192
 * 
 */
public class GCCTest {


	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
	}

	/**
	 * Test method for {@link de.innot.avreclipse.core.toolinfo.GCC#getDefault()}.
	 */
	@Test
	public void testGetDefault() {
		GCC tool = GCC.getDefault();
		assertNotNull(tool);
		// this next test will fail if other than avr-tool toolchain is used
		assertEquals("avr-gcc", tool.getCommandName());
	}

	/**
	 * Test method for {@link de.innot.avreclipse.core.toolinfo.GCC#getToolPath()}.
	 */
	@Test
	public void testGetToolPath() {
		GCC tool = GCC.getDefault();
		IPath gccpath = tool.getToolPath();
		assertNotNull("No ToolPath returned", gccpath);
		File gccfile = gccpath.toFile();
		if (isWindows()) {
			// append .exe
			String windowsname = gccfile.getPath() + ".exe";
			gccfile = new File(windowsname);
		}
		assertTrue("Toolpath does not point to an executable file", gccfile.canRead());
	}

	/**
	 * Test method for {@link de.innot.avreclipse.core.toolinfo.GCC#getToolInfo(java.lang.String)}.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testGetMCUList() throws IOException {
		GCC tool = GCC.getDefault();
		Set<String> mcus = tool.getMCUList();
		assertNotNull(mcus);
		assertTrue(mcus.size() > 5); // at least a few micros should be in the list
		assertTrue(mcus.contains("atmega16"));
		assertFalse(mcus.contains("avr1"));
		assertFalse(mcus.contains(""));
		assertFalse(mcus.contains(null));
	}

	/**
	 * @return true if running on windows
	 */
	private static boolean isWindows() {
		return (Platform.getOS().equals(Platform.OS_WIN32));
	}

}
