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
 *     
 * $Id$
 *     
 *******************************************************************************/
package de.innot.avreclipse.util;

public class SystemPathsProvider {

	private static IToolPaths fToolPaths = null;
	
	public static IToolPaths getToolPaths() {
		if (fToolPaths != null)
			return fToolPaths;
		
		
		return null;
	}
}
