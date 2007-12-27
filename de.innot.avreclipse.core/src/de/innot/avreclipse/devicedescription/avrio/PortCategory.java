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
 * $Id: PortCategory.java 18 2007-11-27 20:27:41Z thomas $
 *     
 *******************************************************************************/
package de.innot.avreclipse.devicedescription.avrio;

import de.innot.avreclipse.devicedescription.ICategory;

/**
 * Implements a ICategory for Port Elements.
 * 
 * @author Thomas Holland
 * 
 * @see RegisterCategory
 * @see IVecsCategory
 */
public class PortCategory extends BaseEntry implements ICategory {

	// The indices for Register Entry column fields
	final static int IDX_NAME = 0;
	final static int IDX_DESCRIPTION = 1;
	final static int IDX_ADDRTYPE = 2;
	final static int IDX_ADDR = 3;
	final static int IDX_BITS = 4;

	// The labels for Register Entry column data fields
	final static String STR_NAME = "Name";
	final static String STR_DESCRIPTION = "Description";
	final static String STR_ADDRTYPE = "";
	final static String STR_ADDR = "Addr";
	final static String STR_BITS = "Bits";

	final static String[] fLabels = { STR_NAME, STR_DESCRIPTION, STR_ADDRTYPE, STR_ADDR, STR_BITS};
	final static int[] fDefaultWidths = { 10, 35, 4, 8, 6 };

	public final static String CATEGORY_NAME = "Ports";

	/**
	 * Instantiate a new PortCategory. The name is fixed to
	 * {@value #CATEGORY_NAME}
	 */
	public PortCategory() {
		super.setName(CATEGORY_NAME);
	}

	@Override
	public String getName() {
		return CATEGORY_NAME;
	}

	public int getColumnCount() {
		return fLabels.length;
	}

	public String[] getColumnLabels() {
		return fLabels;
	}

	public int[] getColumnDefaultWidths() {
		return fDefaultWidths;
	}


}
