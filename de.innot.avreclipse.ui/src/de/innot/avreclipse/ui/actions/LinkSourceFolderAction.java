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
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;

import de.innot.avreclipse.AVRPlugin;
import de.innot.avreclipse.core.arduino.ProjectConfigurator;
import de.innot.avreclipse.ui.dialogs.FolderSelectionDialog;

/**
 * @author Thomas Holland
 * @since 2.2
 * @since 2.3 Added optional delay between avrdude invocations
 * 
 */
public class LinkSourceFolderAction extends AVRProjectAction implements IWorkbenchWindowActionDelegate {

	private final static String	TITLE_LINK_FOLDER		= "AVR link folder";
	
	private final static String	MSG_NOPROJECT			= "No AVR project selected";


	/**
	 * Constructor for this Action.
	 */
	public LinkSourceFolderAction() {
		super();
	}

	/**
	 * @see IActionDelegate#run(IAction)
	 */
	@Override
	public void run(IAction action) {

		// Check that we have a AVR Project
		IProject project = getProject();
		try {
			if (project == null || !project.hasNature("de.innot.avreclipse.core.avrnature")) {
				MessageDialog.openError(getShell(), TITLE_LINK_FOLDER, MSG_NOPROJECT);
				return;
			}
		} catch (CoreException e) {
			// Log the Exception
			IStatus status = new Status(Status.ERROR, AVRPlugin.PLUGIN_ID,
					"Can't access project nature", e);
			AVRPlugin.getDefault().log(status);
		}
		
		FolderSelectionDialog dialog = new FolderSelectionDialog(getShell(), project);
		if (dialog.open() == Dialog.OK) {
			
			String folderPathStr = dialog.getFolderPath();
			String folderNameStr = dialog.getFolderName();
			
			try {
				ICProjectDescription pDesc = CoreModel.getDefault().getProjectDescription(project, false);
				ProjectConfigurator.linkArduinoSourceFolder(pDesc, folderPathStr, folderNameStr);
				CoreModel.getDefault().setProjectDescription(project, pDesc);
			} catch (CoreException ex) {
				ErrorDialog.openError(getShell(), "AVR Project link sources", null, ex.getStatus());
			}
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
	
	@Override
	public void init(IWorkbenchWindow window) {
	}

}
