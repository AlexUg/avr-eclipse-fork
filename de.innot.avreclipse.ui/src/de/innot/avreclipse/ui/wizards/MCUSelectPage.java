/*******************************************************************************
 * Copyright (c) 2008, 2011 Thomas Holland (thomas@innot.de) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Manuel Stahl - initial API and implementation
 *     Thomas Holland - rewritten and fixed for Eclipse >= 3.4
 *     
 *******************************************************************************/
package de.innot.avreclipse.ui.wizards;

import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.internal.ui.wizards.ICDTCommonProjectWizard;
import org.eclipse.cdt.managedbuilder.ui.wizards.MBSCustomPage;
import org.eclipse.cdt.managedbuilder.ui.wizards.MBSCustomPageData;
import org.eclipse.cdt.managedbuilder.ui.wizards.MBSCustomPageManager;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.PlatformUI;
import org.osgi.service.prefs.BackingStoreException;

import de.innot.avreclipse.AVRPlugin;
import de.innot.avreclipse.core.arduino.ProjectConfigurator;
import de.innot.avreclipse.core.natures.AVRProjectNature;
import de.innot.avreclipse.core.properties.AVRProjectProperties;
import de.innot.avreclipse.core.properties.ProjectPropertyManager;
import de.innot.avreclipse.core.util.AVRMCUidConverter;
import de.innot.avreclipse.ui.controls.MCUSelectControl;
import de.innot.avreclipse.ui.controls.MCUSelectControl.DataEvent;
import de.innot.avreclipse.ui.controls.MCUSelectControl.DataListener;
import de.innot.avreclipse.ui.controls.MCUSelectControl.EventType;

/**
 * New Project Wizard Page to set the default target MCU and its frequency.
 * 
 * <p>
 * This Page takes the possible target MCU types and its default as well as the target MCU frequency
 * default directly from the winAVR toolchain as defined in the <code>plugin.xml</code>.
 * </p>
 * <p>
 * If changed, the new type and MCU frequency are written back to the winAVR toolchain as current
 * value and as default value for this project.
 * </p>
 * 
 * @author Manuel Stahl (thymythos@web.de)
 * @author Thomas Holland (thomas@innot.de)
 * @since 1.0
 */
@SuppressWarnings("restriction")
public class MCUSelectPage extends MBSCustomPage implements Runnable {

	private final static String			PAGE_ID				= "de.innot.avreclipse.mcuselectpage";
	
	private MCUSelectControl			fMCUControl;

	private final AVRProjectProperties	fProperties;

	/**
	 * Constructor for the Wizard Page.
	 * 
	 * <p>
	 * Gets the list of supported MCUs from the compiler and sets the default values.
	 * </p>
	 * 
	 */
	public MCUSelectPage() {
		// If the user does not click on "next", this constructor is
		// the only thing called before the "run" method.
		// Therefore we'll set the defaults here. They are set as
		// page properties, as this seems to be the only way to pass
		// values to the run() method.

		this.pageID = PAGE_ID;

		fProperties = ProjectPropertyManager.getDefaultProperties();

		// Set the default values as page properties
		MBSCustomPageManager.addPageProperty(PAGE_ID, EventType.MCU_ID.name(), fProperties.getMCUId());
		MBSCustomPageManager.addPageProperty(PAGE_ID, EventType.F_CPU.name(), fProperties.getFCPU());
		MBSCustomPageManager.addPageProperty(PAGE_ID, EventType.BOARD.name(), fProperties.getBoardId());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.wizard.IWizardPage#getName()
	 */
	public String getName() {
		return "AVR Cross Target Hardware Selection Page";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		// some general layout work

		fMCUControl = new MCUSelectControl(parent);
		fMCUControl.setLayoutData(new GridData(GridData.FILL_BOTH));
		fMCUControl.updateData(fProperties);
		fMCUControl.addDataListener(new DataListener() {
			
			@Override
			public void onModify(DataEvent event) {
				MBSCustomPageManager.addPageProperty(PAGE_ID, event.type.name(), event.data);
			}
		});

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.dialogs.IDialogPage#dispose()
	 */
	public void dispose() {
		fMCUControl.dispose();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.dialogs.IDialogPage#getControl()
	 */
	public Control getControl() {
		return fMCUControl;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.dialogs.IDialogPage#getDescription()
	 */
	public String getDescription() {
		return "Define the AVR target properties";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.dialogs.IDialogPage#getErrorMessage()
	 */
	public String getErrorMessage() {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.dialogs.IDialogPage#getImage()
	 */
	public Image getImage() {
		return wizard.getDefaultPageImage();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.dialogs.IDialogPage#getMessage()
	 */
	public String getMessage() {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.dialogs.IDialogPage#getTitle()
	 */
	public String getTitle() {
		return "AVR Target Hardware Properties";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.dialogs.IDialogPage#performHelp()
	 */
	public void performHelp() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.dialogs.IDialogPage#setDescription(java.lang.String)
	 */
	public void setDescription(String description) {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.dialogs.IDialogPage#setImageDescriptor(org.eclipse.jface.resource.ImageDescriptor)
	 */
	public void setImageDescriptor(ImageDescriptor image) {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.dialogs.IDialogPage#setTitle(java.lang.String)
	 */
	public void setTitle(String title) {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.dialogs.IDialogPage#setVisible(boolean)
	 */
	public void setVisible(boolean visible) {
		fMCUControl.setVisible(visible);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.cdt.managedbuilder.ui.wizards.MBSCustomPage#isCustomPageComplete()
	 */
	@Override
	protected boolean isCustomPageComplete() {
		// We only change defaults, so this page is always complete
		return true;
	}

	/**
	 * Operation for the MCUSelectPage.
	 * 
	 * This is called when the finish button of the new Project Wizard has been pressed. It will get
	 * the new Project and set the project options as selected by the user (or to the default
	 * values).
	 * 
	 */
	public void run() {

		// At this point the new project has been created and its
		// configuration(s) with their toolchains have been set up.

		// Is there a more elegant way to get to the Project?
		// May be 'NO'.
		// The problems in implementation of method 'org.eclipse.cdt.managedbuilder.ui.wizards.MBSWizardHandler.doCustom(IProject)'.
		// That method ignores parameter 'IProject' (handler of newly created project) when invokes additional operations like this runnable. 
		MBSCustomPageData pagedata = MBSCustomPageManager.getPageData(this.pageID);
		
		// In some case 'pagedata.getWizardPage().getWizard()' returns 'CCProjectWizard2' (which implements 'ICDTCommonProjectWizard')
		ICDTCommonProjectWizard wizz = (ICDTCommonProjectWizard) pagedata.getWizardPage().getWizard();
		IProject project = wizz.getLastProject();

		ProjectPropertyManager projpropsmanager = ProjectPropertyManager.getPropertyManager(project);
		AVRProjectProperties props = projpropsmanager.getProjectProperties();

		// Set the Project properties according to the selected values

		// Get the id of the selected MCU and store it
		String mcuname = (String) MBSCustomPageManager.getPageProperty(PAGE_ID, EventType.MCU_ID.name());
		String mcuid = AVRMCUidConverter.name2id(mcuname);
		props.setMCUId(mcuid);

		// Set the F_CPU and store it
		String fcpu = (String) MBSCustomPageManager.getPageProperty(PAGE_ID, EventType.F_CPU.name());
		props.setFCPU(fcpu);
		
		// Set the BOARD and store it
		String boardId = (String) MBSCustomPageManager.getPageProperty(PAGE_ID, EventType.BOARD.name());
		props.setBoardId(boardId);

		try {
			
			props.save();
			
		} catch (BackingStoreException ex) {
			IStatus status = new Status(IStatus.ERROR, AVRPlugin.PLUGIN_ID,
					"Could not write project properties to the preferences.", ex);

			ErrorDialog.openError(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
					"AVR Project Wizard Error", null, status);
		}

		// Add the AVR Nature to the project
		try {
			AVRProjectNature.addAVRNature(project);
		} catch (CoreException ce) {
			// addAVRNature() should not cause an Exception, but just in case we log it.
			IStatus status = new Status(IStatus.ERROR, AVRPlugin.PLUGIN_ID,
					"Could not add AVR nature to project [" + project.toString() + "]", ce);
			AVRPlugin.getDefault().log(status);
		}
		
		if ((boardId != null)
				&& !boardId.isEmpty()) {
			try {
				
				ICProjectDescription pDesc = CoreModel.getDefault().getProjectDescription(project, true);
				ProjectConfigurator.configureForArduino(pDesc, boardId);
				CoreModel.getDefault().setProjectDescription(project, pDesc);
				
			} catch (CoreException ex) {
	
				ErrorDialog.openError(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
						"Linking Arduino sources failed", null, ex.getStatus());
			}
		}

	}
}
