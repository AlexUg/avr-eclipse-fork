/*******************************************************************************
 * 
 * Copyright (c) 2009 Thomas Holland (thomas@innot.de) and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the GNU Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Thomas Holland - initial API and implementation
 *     
 * $Id$
 *     
 *******************************************************************************/

package de.innot.avreclipse.core.targets;

import org.osgi.service.prefs.BackingStoreException;

/**
 * 
 * @author Thomas Holland
 * @since 2.4
 * 
 */
public interface ITargetConfigurationWorkingCopy extends ITargetConfiguration {

	/**
	 * Set a new name for this configuration.
	 * <p>
	 * This is a convenience method equivalent to
	 * 
	 * <pre>
	 * setAttribute(ATTR_NAME, name);
	 * </pre>
	 * 
	 * </p>
	 * 
	 * @param name
	 *            the Name to set
	 */
	public void setName(String name);

	/**
	 * Set a new description for this configuration.
	 * <p>
	 * This is a convenience method equivalent to
	 * 
	 * <pre>
	 * setAttribute(ATTR_DESCRIPTION, name);
	 * </pre>
	 * 
	 * </p>
	 * 
	 * @param name
	 *            the Name to set
	 */
	public void setDescription(String description);

	/**
	 * <p>
	 * This is a convenience method equivalent to
	 * 
	 * <pre>
	 * setAttribute(ATTR_MCU, name);
	 * </pre>
	 * 
	 * </p>
	 * 
	 * @param mcuid
	 *            the MCU to set
	 */
	public void setMCU(String mcuid);

	/**
	 * Change the target MCU clock.
	 * <p>
	 * This is a convenience method equivalent to
	 * 
	 * <pre>
	 * setAttribute(ATTR_FCPU, name);
	 * </pre>
	 * 
	 * </p>
	 * 
	 * @param fcpu
	 *            the FCPU to set
	 */
	public void setFCPU(int fcpu);

	/**
	 * Persist this configuration to the preference storage.
	 * <p>
	 * This will not do anything if the configuration has not been modified.
	 * </p>
	 * 
	 * @throws BackingStoreException
	 *             If this configuration cannot be written to the preference storage area.
	 */
	public void doSave() throws BackingStoreException;

	/**
	 * Set the attribute to the value.
	 * <p>
	 * Neither <code>attribute</code> nor <code>value</code> may be <code>null</code>.
	 * </p>
	 * 
	 * @param attribute
	 * @param value
	 */
	public void setAttribute(String attribute, String value);

	/**
	 * Set the attribute to the boolean value.
	 * <p>
	 * The attribute is actually stored as a String containing "true" or "false".
	 * </p>
	 * 
	 * @param attribute
	 * @param value
	 */
	public void setBooleanAttribute(String attribute, boolean value);

	/**
	 * Set the attribute to an integer value.
	 * <p>
	 * The attribute is actually stored as a String containing the value.
	 * </p>
	 * 
	 * @param attribute
	 * @param value
	 */
	public void setIntegerAttribute(String attribute, int value);

	/**
	 * Reset this Configuration to the default values.
	 * <p>
	 * The ID and the Name of this Configuration are not changed.
	 * </p>
	 */
	public void setDefaults();

	/**
	 * Checks if this working copy has unsaved changes.
	 * 
	 * @return <code>true</code> if this configuration has unsaved changes.
	 */
	public boolean isDirty();
}