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
package de.innot.avreclipse.ui.actions;

import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;

import de.innot.avreclipse.AVRPlugin;
import de.innot.avreclipse.core.arduino.ProjectConfigurator;

/**
 * @author Thomas Holland
 * @since 2.2
 * @since 2.3 Added optional delay between avrdude invocations
 * 
 */
public class FixIncludesAction extends AVRProjectAction implements IWorkbenchWindowActionDelegate {

	private final static String	TITLE_FIX_INCLUDES		= "AVR fix includes";
	
	private final static String	MSG_NOPROJECT			= "No AVR project selected";
	


	/**
	 * Constructor for this Action.
	 */
	public FixIncludesAction() {
		super();
	}

	/**
	 * @see IActionDelegate#run(IAction)
	 */
	@Override
	public void run(IAction action) {

		// Check that we have a AVR Project
		try {
			if (getProject() == null || !getProject().hasNature("de.innot.avreclipse.core.avrnature")) {
				MessageDialog.openError(getShell(), TITLE_FIX_INCLUDES, MSG_NOPROJECT);
				return;
			}
		} catch (CoreException e) {
			// Log the Exception
			IStatus status = new Status(Status.ERROR, AVRPlugin.PLUGIN_ID, "Can't access project nature", e);
			AVRPlugin.getDefault().log(status);
		}
		
		try {
			ICProjectDescription pDesc = CoreModel.getDefault().getProjectDescription(getProject(), false);
			ProjectConfigurator.fixIncludes(pDesc);
			CoreModel.getDefault().setProjectDescription(getProject(), pDesc);
		} catch (CoreException ex) {
			ErrorDialog.openError(getShell(), "AVR Project link sources", null, ex.getStatus());
		}
	}

	/**
	 * Get the current Shell.
	 * 
	 * @return <code>Shell</code> of the active Workbench window.
	 */
	private Shell getShell() {
		return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
	}

}
