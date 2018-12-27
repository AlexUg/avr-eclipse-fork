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
package de.innot.avreclipse.ui.propertypages;

import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.ui.newui.CDTPropertyManager;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;

import de.innot.avreclipse.core.arduino.ProjectConfigurator;
import de.innot.avreclipse.core.properties.AVRProjectProperties;
import de.innot.avreclipse.ui.controls.MCUSelectControl;

/**
 * This tab handles setting of all target hardware related properties.
 * 
 * @author Thomas Holland
 * @since 2.2
 * 
 */
public class TabTargetHardware extends AbstractAVRPropertyTab {

	private MCUSelectControl		fMCUControl;

	private String					fOldMCUid;
	private String					fOldFCPU;
	private String					fOldBoard;

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.cdt.ui.newui.AbstractCPropertyTab#createControls(org.eclipse.swt.widgets.Composite
	 * )
	 */
	@Override
	public void createControls(Composite parent) {
		super.createControls(parent);
		usercomp.setLayout(new GridLayout(1, false));
		fMCUControl = new MCUSelectControl(usercomp, true);
		fMCUControl.setLayoutData(new GridData(GridData.FILL_BOTH));
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * de.innot.avreclipse.ui.propertypages.AbstractAVRPropertyTab#performApply(de.innot.avreclipse
	 * .core.preferences.AVRConfigurationProperties,
	 * de.innot.avreclipse.core.preferences.AVRConfigurationProperties)
	 */
	@Override
	protected void performApply(AVRProjectProperties dst) {

		AVRProjectProperties targetProp = fMCUControl.getTargetProperties();
		
		if (targetProp == null) {
			// Do nothing if the Target properties do not exist.
			return;
		}
		String newMCUid = targetProp.getMCUId();
		String newFCPU = targetProp.getFCPU();
		String newBoard = targetProp.getBoardId();

		dst.setMCUId(newMCUid);
		dst.setFCPU(newFCPU);
		dst.setBoardId(newBoard);

		// Check if a rebuild is required
		boolean rebuild = setRebuildRequired();
		if (rebuild) {
			setDiscoveryRequired();
		}

		fOldMCUid = newMCUid;
		fOldFCPU = newFCPU;
		fOldBoard = newBoard;

	}

	/*
	 * (non-Javadoc)
	 * @see
	 * de.innot.avreclipse.ui.propertypages.AbstractAVRPropertyTab#performDefaults(de.innot.avreclipse
	 * .core.preferences.AVRProjectProperties)
	 */
	@Override
	protected void performCopy(AVRProjectProperties defaults) {
		AVRProjectProperties targetProp = fMCUControl.getTargetProperties();
		targetProp.setMCUId(defaults.getMCUId());
		targetProp.setFCPU(defaults.getFCPU());
		targetProp.setBoardId(defaults.getBoardId());
		fMCUControl.updateData(targetProp);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.cdt.ui.newui.AbstractCPropertyTab#performOK()
	 */
	@Override
	protected void performOK() {
		// We override this to set the rebuild state as required
		boolean rebuild = setRebuildRequired();
		if (rebuild) {
			// Now we need to invalidate all discovered Symbols, because they still contain infos
			// about the previous MCU.
			setDiscoveryRequired();
		}
		super.performOK();
		
		String boardId = fMCUControl.getTargetProperties().getBoardId();
		if ((boardId != null)
			&& !boardId.isEmpty()) {
		try {
			
			ICProjectDescription pDesc = CDTPropertyManager.getProjectDescription(getCfg().getOwner().getProject());
			ProjectConfigurator.configureForArduino(pDesc, boardId);
			
		} catch (CoreException ex) {

			ErrorDialog.openError(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
					"Linking Arduino sources failed", null, ex.getStatus());
		}
	}
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * de.innot.avreclipse.ui.propertypages.AbstractAVRPropertyTab#updateData(de.innot.avreclipse
	 * .core.preferences.AVRConfigurationProperties)
	 */
	@Override
	protected void updateData(AVRProjectProperties cfg) {
		fMCUControl.updateData(cfg);
	}

	/**
	 * Checks if the current target values are different from the original ones and set the rebuild
	 * flag for the configuration / project if yes.
	 */
	private boolean setRebuildRequired() {
		AVRProjectProperties targetProp = fMCUControl.getTargetProperties();
		if (fOldMCUid == null || fOldFCPU == null
				|| !(targetProp.getMCUId().equals(fOldMCUid))
				|| !(targetProp.getFCPU().equals(fOldFCPU))
				|| !(targetProp.getBoardId().equals(fOldBoard))) {
			setRebuildState(true);
			return true;
		}
		return false;
	}

}
