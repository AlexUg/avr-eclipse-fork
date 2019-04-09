package de.innot.avreclipse.ui.actions;

import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionDelegate;

public abstract class AVRProjectAction extends ActionDelegate {
	
	private IProject			fProject;

	public AVRProjectAction() {
		super();
	}

	public IProject getProject() {
		IProject result = tryGetActiveProject();
		if (result != null) {
			return result;
		}
		return fProject;
	}

	/**
	 * @see IActionDelegate#selectionChanged(IAction, ISelection)
	 */
	@Override
	public void selectionChanged(IAction action, ISelection selection) {
	
		// The user has selected a different Workbench object.
		// If it is an IProject we keep it.
	
		Object item;
	
		if (selection instanceof IStructuredSelection) {
			item = ((IStructuredSelection) selection).getFirstElement();
		} else {
			return;
		}
		if (item == null) {
			return;
		}
		IProject project = null;
	
		// See if the given is an IProject (directly or via IAdaptable)
		if (item instanceof IProject) {
			project = (IProject) item;
		} else if (item instanceof IResource) {
			project = ((IResource) item).getProject();
		} else if (item instanceof IAdaptable) {
			IAdaptable adaptable = (IAdaptable) item;
			project = (IProject) adaptable.getAdapter(IProject.class);
			if (project == null) {
				// Try ICProject -> IProject
				ICProject cproject = (ICProject) adaptable.getAdapter(ICProject.class);
				if (cproject == null) {
					// Try ICElement -> ICProject -> IProject
					ICElement celement = (ICElement) adaptable.getAdapter(ICElement.class);
					if (celement != null) {
						cproject = celement.getCProject();
					}
				}
				if (cproject != null) {
					project = cproject.getProject();
				}
			}
		}
	
		fProject = project;
	}
	
	private IProject tryGetActiveProject() {
		IProject result = null;
		IWorkbenchPart part = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActivePart();
		ISelection selection = part.getSite().getSelectionProvider().getSelection();
		if (selection instanceof IStructuredSelection) {
			Object selObj = ((IStructuredSelection) selection).getFirstElement();
			if (selObj instanceof IAdaptable) {
				IResource resource = ((IAdaptable) selObj).getAdapter(IResource.class);
				if (resource != null) {
					result = resource.getProject();
				}
			}
		}
		if (result == null) {
			part = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
			if (part != null) {
				IEditorInput editorInput = ((IEditorPart) part).getEditorInput();
				if (editorInput != null) {
					IResource resource = editorInput.getAdapter(IResource.class);
					if (resource != null) {
						result = resource.getProject();
					}
				}
			}
		}
		return result;
	}

}