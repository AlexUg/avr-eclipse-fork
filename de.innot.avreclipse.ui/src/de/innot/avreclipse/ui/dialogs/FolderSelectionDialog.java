package de.innot.avreclipse.ui.dialogs;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class FolderSelectionDialog extends TitleAreaDialog {
	
	private IProject project;

	private boolean setManually = false;
	private String folderName;
	private Text folderNameText;
	
	private String folderPath;
	private Text folderPathText;
	
	public FolderSelectionDialog(Shell parentShell, IProject project) {
		super(parentShell);
		this.project = project;
	}

	public String getFolderName() {
		return folderName;
	}

	public String getFolderPath() {
		return folderPath;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite main = (Composite) super.createDialogArea(parent);
		Group group = new Group(main, SWT.NONE);
		group.setLayout(new GridLayout(2, false));
		group.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		Label l = new Label(group, SWT.NONE);
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		l.setText("Folder name in project (link name):");
		folderNameText = new Text(group, SWT.SINGLE);
		folderNameText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
		folderNameText.addModifyListener(new ModifyListener() {
			
			@Override
			public void modifyText(ModifyEvent e) {
				folderName = folderNameText.getText();
				setManually = true; checkValues();
			}
		});
		Button b = new Button(group, SWT.PUSH);
		b.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));
		b.setText("Set default");
		b.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				setManually = false; setDefaultFolderName();
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		l = new Label(group, SWT.NONE);
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		l.setText("External folder path:");
		folderPathText = new Text(group, SWT.SINGLE | SWT.READ_ONLY);
		folderPathText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
		folderPathText.addModifyListener(new ModifyListener() {
			
			@Override
			public void modifyText(ModifyEvent e) {
				folderPath = folderPathText.getText();
			}
		});
		b = new Button(group, SWT.PUSH);
		b.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));
		b.setText("Browse...");
		b.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				setFolderPath();
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				setFolderPath();
			}
		});
		return main;
	}
	
	private void setFolderPath() {
		DirectoryDialog dialog = new DirectoryDialog(getShell());
		String newFolderPath = dialog.open();
		if (newFolderPath != null) {
			folderPathText.setText(newFolderPath);
			setDefaultFolderName();
		}
		checkValues();
	}
	
	private void setDefaultFolderName() {
		if (!setManually
				&& (folderPath != null)) {
			Path path = new Path(folderPath);
			IFolder folder = project.getFolder(path.lastSegment());
			int idx = 0;
			while (folder.exists()) {
				folder = project.getFolder(folder.getName() + idx);
				idx++;
			}
			folderNameText.setText(folder.getName());
		}
	}
	
	private void checkValues() {
		if ((folderName == null)
				|| folderName.isEmpty()) {
			setErrorMessage("Folder name in project isn't set");
		} else if ((folderPath == null)
				|| folderPath.isEmpty()) {
			setErrorMessage("External folder path isn't set");
		} else {
			setErrorMessage(null);
		}
	}

}
