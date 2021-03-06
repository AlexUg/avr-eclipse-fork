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

package de.innot.avreclipse.core.targets;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Thomas Holland
 * @since
 * 
 */
public class TargetConfigurationManagerTest implements ITargetConfigConstants {

	private TargetConfigurationManager	manager;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		manager = TargetConfigurationManager.getDefault();
		assertNotNull("No target configuration manager", manager);
	}

	/**
	 * Test method for
	 * {@link de.innot.avreclipse.core.targets.TargetConfigurationManager#createNewConfig()}.
	 * 
	 * @throws CoreException
	 */
	@Test
	public void testCreateNewConfig() throws CoreException, IOException {
		ITargetConfiguration tc = manager.createNewConfig();
		assertNotNull("CreateNewConfig returned null", tc);
		assertNotNull("New config has null id", tc.getId());
		assertTrue("New config has empty id", tc.getId().length() > 0);

		// Check some defaults
		assertNotNull("New config has null name", tc.getName());
		assertTrue("New config has empty name", tc.getName().length() > 0);

		assertNotNull("New config has null mcuid", tc.getMCU());
		assertTrue("New config has empty mcuid", tc.getMCU().length() > 0);

		assertNotNull(tc.getAttribute(ATTR_NAME));
		assertNotNull(tc.getAttribute(ATTR_MCU));
		assertNotNull(tc.getAttribute(ATTR_FCPU));

	}

	/**
	 * Test method for
	 * {@link de.innot.avreclipse.core.targets.TargetConfigurationManager#exists(java.lang.String)}.
	 */
	@Test
	public void testExists() throws IOException {
		ITargetConfiguration tc = manager.createNewConfig();

		assertTrue("Config does not exist", manager.exists(tc.getId()));

		// Check some non existing configs
		assertFalse(manager.exists("foobar"));
		assertFalse(manager.exists(""));
		assertFalse(manager.exists(null));

	}

	/**
	 * Test method for
	 * {@link de.innot.avreclipse.core.targets.TargetConfigurationManager#deleteConfig(java.lang.String)}
	 * .
	 */
	@Test
	public void testDeleteConfig() throws IOException {
		// Create a new config and then delete it again
		ITargetConfiguration tc = manager.createNewConfig();

		manager.deleteConfig(tc.getId());
		assertFalse("Config was not deleted", manager.exists(tc.getId()));
	}

	/**
	 * Test method for
	 * {@link de.innot.avreclipse.core.targets.TargetConfigurationManager#getConfig(java.lang.String)}
	 * .
	 */
	@Test
	public void testGetConfig() throws IOException {
		// Create two new configs and then get them
		ITargetConfiguration tc1 = manager.createNewConfig();
		ITargetConfiguration tc2 = manager.createNewConfig();

		assertSame(tc1, manager.getConfig(tc1.getId()));
		assertSame(tc2, manager.getConfig(tc2.getId()));

		// Check some failure modes
		assertNull(manager.getConfig("foobar"));
		assertNull(manager.getConfig(""));
		assertNull(manager.getConfig(null));
	}

	/**
	 * Test method for
	 * {@link de.innot.avreclipse.core.targets.TargetConfigurationManager#getWorkingCopy(java.lang.String)}
	 * .
	 * 
	 * @throws CoreException
	 */
	@Test
	public void testGetWorkingCopy() throws CoreException, IOException {
		ITargetConfiguration tc = manager.createNewConfig();

		ITargetConfigurationWorkingCopy tcwc = manager.getWorkingCopy(tc.getId());

		assertNotNull("Null Working Copy", tcwc);
		assertEquals("Working copy has different id", tc.getId(), tcwc.getId());

		// Check that all attributes are the same
		Map<String, String> tcattrs = tc.getAttributes();
		Map<String, String> wcattrs = tcwc.getAttributes();
		String[] tcattrsarray = tcattrs.keySet().toArray(new String[tcattrs.size()]);
		String[] wcattrsarray = wcattrs.keySet().toArray(new String[tcattrs.size()]);

		// Sort the arrays to have both in the same order for comparing
		Arrays.sort(tcattrsarray);
		Arrays.sort(wcattrsarray);

		assertArrayEquals("Working copy has different attributes", tcattrsarray, wcattrsarray);
		for (String attr : tcattrs.keySet()) {
			String tcvalue = tcattrs.get(attr);
			String wcvalue = wcattrs.get(attr);
			assertEquals("Attribute " + attr + " of working copy differs from original", tcvalue,
					wcvalue);
		}
	}

	/**
	 * Test method for
	 * {@link de.innot.avreclipse.core.targets.TargetConfigurationManager#getConfigurationIDs()}.
	 */
	@Test
	public void testGetConfigurationIDs() throws IOException {
		// The list may or may not be empty at this point depending on the other tests that have
		// already run.

		// Add two more configs and check their ids in the list
		ITargetConfiguration tc1 = manager.createNewConfig();
		ITargetConfiguration tc2 = manager.createNewConfig();

		List<String> allids = manager.getConfigurationIDs();
		assertNotNull("ID list is null", allids);
		assertTrue("ID list must have 2 or more entries", allids.size() >= 2);

		assertTrue("First config id missing", allids.contains(tc1.getId()));
		assertTrue("Second config id missing", allids.contains(tc2.getId()));

	}

}
