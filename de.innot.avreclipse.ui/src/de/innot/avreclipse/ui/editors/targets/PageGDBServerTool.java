/*******************************************************************************
 * 
 * Copyright (c) 2008,2009 Thomas Holland (thomas@innot.de) and others
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

package de.innot.avreclipse.ui.editors.targets;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.SharedHeaderFormEditor;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;

import de.innot.avreclipse.core.targets.ITargetConfigurationWorkingCopy;

/**
 * 
 * @author Thomas Holland
 * @since 2.4
 * 
 */
public class PageGDBServerTool extends BasePage {

	public final static String						ID		= "de.innot.avreclipse.ui.targets.gdbservertool";

	private final static String						TITLE	= "GDB Server";

	/**
	 * The target configuration this editor page works on. The target config is final and con not be
	 * changed after instantiation of the page. This is the 'model' for the managed form.
	 */
	final private ITargetConfigurationWorkingCopy	fTCWC;

	/**
	 * Create a new EditorPage.
	 * <p>
	 * The page has the id from the {@link #ID} identifier and the fixed title string {@link #TITLE}
	 * .
	 * </p>
	 * 
	 * @param editor
	 *            Parent FormEditor
	 */
	public PageGDBServerTool(SharedHeaderFormEditor editor) {
		super(editor, ID, TITLE);

		// Get the TargetConfiguration from the editor input.
		IEditorInput ei = editor.getEditorInput();
		fTCWC = (ITargetConfigurationWorkingCopy) ei
				.getAdapter(ITargetConfigurationWorkingCopy.class);

	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.ui.forms.editor.FormPage#createFormContent(org.eclipse.ui.forms.IManagedForm)
	 */
	@Override
	protected void createFormContent(IManagedForm managedForm) {

		// fill the fixed parts of the form...
		fillBody(managedForm);

		// ... and give the 'model' to the managed form which will cause the dynamic parts of the
		// form to be rendered.
		managedForm.setInput(fTCWC);
	}

	/**
	 * Fill the managed Form.
	 * <p>
	 * This method will the following parts to the form:
	 * <ul>
	 * <li>The general section with name and description.</li>
	 * <li>The target MCU and its clock freq.</li>
	 * <li>The programmer selection and its associated sub-sections.</li>
	 * <li>The Uploader tool section.</li>
	 * <li>The GDBServer tool section.</li>
	 * </ul>
	 * </p>
	 * 
	 * @param managedForm
	 */
	private void fillBody(IManagedForm managedForm) {

		// fill the fixed parts of the form...
		Composite body = managedForm.getForm().getBody();
		body.setLayout(new TableWrapLayout());

		{
			SectionProgrammerTool programmerToolPart = new SectionProgrammerTool();
			programmerToolPart.setMessageManager(getMessageManager());
			managedForm.addPart(programmerToolPart);
			registerPart(programmerToolPart);
			programmerToolPart.getControl().setLayoutData(
					new TableWrapData(TableWrapData.FILL_GRAB));
		}

		// ... and give the 'model' to the managed form which will cause the dynamic parts of the
		// form to be rendered.
		managedForm.setInput(fTCWC);
	}
}
