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

import de.innot.avreclipse.core.targets.ITargetConfiguration.ValidationResult;

/**
 * @author Thomas Holland
 * @since
 * 
 */
public interface IAttributeProvider {

	public String[] getAttributes();

	public String getDefaultValue(String attribute);

	public ValidationResult validate(String attribute);

}
