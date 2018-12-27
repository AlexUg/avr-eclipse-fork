package de.innot.avreclipse.ui.controls;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.cdt.core.settings.model.ICResourceDescription;
import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.ui.properties.AbstractCBuildPropertyTab;
import org.eclipse.cdt.ui.newui.AbstractCPropertyTab;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.UIJob;

import de.innot.avreclipse.AVRPlugin;
import de.innot.avreclipse.core.arduino.BoardsPreferences;
import de.innot.avreclipse.core.avrdude.AVRDudeException;
import de.innot.avreclipse.core.avrdude.AVRDudeSchedulingRule;
import de.innot.avreclipse.core.properties.AVRDudeProperties;
import de.innot.avreclipse.core.properties.AVRProjectProperties;
import de.innot.avreclipse.core.toolinfo.AVRDude;
import de.innot.avreclipse.core.toolinfo.GCC;
import de.innot.avreclipse.core.util.AVRMCUidConverter;
import de.innot.avreclipse.ui.dialogs.AVRDudeErrorDialogJob;
import de.innot.avreclipse.ui.propertypages.AVRPropertyPageManager;

public class MCUSelectControl extends Composite {
	
	public static enum EventType {
		MCU_ID,
		F_CPU,
		BOARD;
	}
	
	public static class DataEvent {
		
		public EventType type;
		public String data;
		
		public DataEvent() {
			super();
		}

		public DataEvent(EventType type, String data) {
			super();
			this.type = type;
			this.data = data;
		}
		
	}
	
	public static interface DataListener {
		void onModify(DataEvent event); 
	}

	private static final String		LABEL_MCUTYPE			= "MCU Type";
	private static final String		LABEL_FCPU				= "MCU Clock Frequency";
	private static final String		LABEL_BOARD				= "Arduino Board";
	private static final String		TEXT_LOADBUTTON			= "Load from MCU";
	private static final String		TEXT_LOADBUTTON_BUSY	= "Loading...";
	
	private final static String		TITLE_FUSEBYTEWARNING	= "{0} Conflict";
	private final static String		TEXT_FUSEBYTEWARNING	= "Selected MCU is not compatible with the currently set {0}.\n"
																	+ "Please check the {0} settings on the AVRDude {1}.";
	private final static String[]	TITLEINSERT				= new String[] { "", "Fuse Byte",
			"Lockbits", "Fuse Byte and Lockbits"			};
	private final static String[]	TEXTINSERT				= new String[] { "", "fuse byte",
			"lockbits", "fuse byte and lockbits"			};
	private final static String[]	TABNAMEINSERT			= new String[] { "", "Fuse tab",
			"Lockbits tab", "Fuse and Lockbit tabs"		};
	
	/** List of common MCU frequencies (taken from mfile) */
	private static final String[]	FCPU_VALUES				= { "1000000", "1843200", "2000000",
			"3686400", "4000000", "7372800", "8000000", "11059200", "14745600", "16000000",
			"18432000", "20000000"							};

	private static final Image		IMG_WARN				= PlatformUI
																	.getWorkbench()
																	.getSharedImages()
																	.getImage(
																			ISharedImages.IMG_OBJS_WARN_TSK);
	
	/** The Properties that this page works with */
	private ICResourceDescription 	fResdesc;
	private AVRProjectProperties	fTargetProps;
	private BoardsPreferences		fBoardPreferences = BoardsPreferences.INSTANCE;
	
	private Button 					fMCUButton;
	private ComboControl			fMCUcombo;
	private Button					fLoadButton;
	private Combo					fFCPUcombo;
	private Composite				fMCUWarningComposite;

	private Button					fArdButton;
	private ComboControl			fBoardCombo;
	
	private Set<String>				fMCUids;
	private String[]				fMCUNames;
	
	private boolean 				fIsArduinoAvailable = false;
	private boolean 				fIsLoadSupported = false;
	
	private List<DataListener>	fListeners;
	
	private Map<String, String> fBoardNameToIdMap;

	public MCUSelectControl(Composite parent) {
		this(parent, false);
	}

	public MCUSelectControl(Composite parent, boolean isLoadSupported) {
		super(parent, SWT.NONE);
		fIsLoadSupported = isLoadSupported;
		setFont(parent.getFont());
		setBackground(parent.getBackground());
		setLayout(new GridLayout(3, false));
		createControls();
	}
	
	public AVRProjectProperties getTargetProperties() {
		return fTargetProps;
	}
	
	public void addDataListener(DataListener l) {
		if (fListeners == null) {
			fListeners = new ArrayList<>();
		}
		if (!fListeners.contains(l)) {
			fListeners.add(l);
		}
	}
	
	public void removeDataListener(DataListener l) {
		if (fListeners != null) {
			fListeners.remove(l);
		}
	}
	
	public void updateData(ICResourceDescription resdesc) {
		fResdesc = resdesc;
		updateData(AVRPropertyPageManager.getConfigProperties(resdesc));
	}
	
	public void updateData(AVRProjectProperties	targetProps) {
		fTargetProps = targetProps;
		updateControlsData();
	}
	
	protected void createControls() {
		
		// Get the list of supported MCU id's from the compiler
		// The list is then converted into an array of MCU names
		//
		// If we ever implement per project paths this needs to be moved to the
		// updataData() method to reload the list of supported mcus every time
		// the paths change. The list is added to the combo in addMCUSection().
		if (fMCUids == null) {
			try {
				fMCUids = GCC.getDefault().getMCUList();
			} catch (IOException e) {
				// Could not start avr-gcc. Pop an Error Dialog and continue with an empty list
				IStatus status = new Status(
						IStatus.ERROR,
						AVRPlugin.PLUGIN_ID,
						"Could not execute avr-gcc. Please check the AVR paths in the preferences.",
						e);
				ErrorDialog.openError(getShell(), "AVR-GCC Execution fault", null, status);
				fMCUids = new HashSet<String>();
			}
			String[] allmcuids = fMCUids.toArray(new String[fMCUids.size()]);
			fMCUNames = new String[fMCUids.size()];
			for (int i = 0; i < allmcuids.length; i++) {
				fMCUNames[i] = AVRMCUidConverter.id2name(allmcuids[i]);
			}
			Arrays.sort(fMCUNames);
		}
		
		fMCUButton = new Button(this, SWT.RADIO);
		setupControl(fMCUButton, ((GridLayout) getLayout()).numColumns, GridData.FILL_HORIZONTAL);
		fMCUButton.setText("Define MCU type and freqency");
		fMCUButton.setSelection(true);
		fMCUButton.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (fMCUButton.getSelection()) {
					enableBoardSection(false);
				}
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});

		addMCUSection(this);
		addFCPUSection(this);
		addSeparator(this);
		
		try {
			fBoardPreferences.load();
			fIsArduinoAvailable = !fBoardPreferences.getAvailiableBoardIds().isEmpty();
		} catch (Exception e) {
		}
		
		fArdButton = new Button(this, SWT.RADIO);
		setupControl(fArdButton, ((GridLayout) getLayout()).numColumns, GridData.FILL_HORIZONTAL);
		fArdButton.setText("Define Arduino board type");
		fArdButton.setSelection(false);
		fArdButton.setEnabled(fIsArduinoAvailable);
		fArdButton.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (fArdButton.getSelection()) {
					enableBoardSection(true);
				}
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		
		addBoardsSection(this);

	}
	
	protected void updateControlsData() {
		
		String mcuid = fTargetProps.getMCUId();
		fMCUcombo.setValue(AVRMCUidConverter.id2name(mcuid));

		String fcpu = fTargetProps.getFCPU();
		fFCPUcombo.setText(fcpu);
		
		String boardId = fTargetProps.getBoardId();
		if (boardId != null) {
			String boardName = fBoardPreferences.getBoardName(boardId);
			if (boardName != null) {
				fBoardCombo.setValue(boardName);
			}
	
			if (!boardId.isEmpty()
					&& fIsArduinoAvailable) {
				fMCUButton.setSelection(false);
				fArdButton.setSelection(true);
				enableBoardSection(true);
			}
		}
	}
	
	protected void updateMCUData(String boardId) {
		if ((boardId != null)
				&& !boardId.isEmpty()) {
			String mcuId = fBoardPreferences.getMCUType(boardId);
			fMCUcombo.setValue(AVRMCUidConverter.id2name(mcuId));
			String fcpu = fBoardPreferences.getFCPU(boardId);
			if (!Character.isDigit(fcpu.charAt(fcpu.length() - 1))) {
				fcpu = fcpu.substring(0, fcpu.length() - 1);
			}
			fFCPUcombo.setText(Integer.valueOf(fcpu).toString());
		}
	}
	
	private void enableBoardSection(boolean enable) {
		fMCUcombo.setEnabled(!enable);
		if (fLoadButton != null) {
			fLoadButton.setEnabled(!enable);
		}
		fFCPUcombo.setEnabled(!enable);
		fBoardCombo.setEnabled(enable);
		if (enable) {
			String boardId = fBoardNameToIdMap.get(fBoardCombo.getValue());
			fTargetProps.setBoardId(boardId);
			updateMCUData(boardId);
		} else {
			fTargetProps.setBoardId("");
		}
	}
	
	private void addMCUSection(Composite parent) {

		// MCU Selection Combo
		setupLabel(parent, LABEL_MCUTYPE, 1, SWT.NONE);
		// Label label = new Label(parent, SWT.NONE);
		// label.setText(LABEL_MCUTYPE);
		// label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));

		fMCUcombo = new ComboControl(parent, fMCUNames);
		setupControl(fMCUcombo, 1, GridData.FILL_HORIZONTAL);

		fMCUcombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String mcuname = e.text;
				String mcuid = AVRMCUidConverter.name2id(mcuname);
				fTargetProps.setMCUId(mcuid);
				fireDataEvent(EventType.MCU_ID, mcuid);

				// Check if supported by avrdude and set the errorpane as
				// required
				checkAVRDude(mcuid);

				// Check fuse byte settings and pop a message if the settings
				// are not compatible
				checkFuseBytes(mcuid);
			}
		});

		if (fIsLoadSupported) {
			// Load from Device Button
			fLoadButton = setupButton(parent, TEXT_LOADBUTTON, 1, SWT.NONE);
			fLoadButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
			fLoadButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					loadComboFromDevice();
				}
			});
		} else {
			// Dummy Label for Padding
			Label label = new Label(parent, SWT.NONE);
			label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		}

		// The Warning Composite
		fMCUWarningComposite = new Composite(parent, SWT.NONE);
		setupControl(fMCUWarningComposite, ((GridLayout) getLayout()).numColumns, GridData.FILL_HORIZONTAL);
		GridLayout gl = new GridLayout(2, false);
		gl.marginHeight = 0;
		gl.marginWidth = 0;
		gl.verticalSpacing = 0;
		gl.horizontalSpacing = 0;
		fMCUWarningComposite.setLayout(gl);

		Label warnicon = new Label(fMCUWarningComposite, SWT.LEFT);
		warnicon.setLayoutData(new GridData(GridData.BEGINNING));
		warnicon.setImage(IMG_WARN);

		Label warnmessage = new Label(fMCUWarningComposite, SWT.LEFT);
		warnmessage.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		warnmessage.setText("This MCU is not supported by AVRDude");

		fMCUWarningComposite.setVisible(false);

	}

	private void addFCPUSection(Composite parent) {

		GridData gd = new GridData();
		FontMetrics fm = AbstractCPropertyTab.getFontMetrics(parent);
		gd.widthHint = Dialog.convertWidthInCharsToPixels(fm, 16);

		setupLabel(parent, LABEL_FCPU, 1, SWT.NONE);

		fFCPUcombo = new Combo(parent, SWT.DROP_DOWN);
		fFCPUcombo.setLayoutData(gd);
		fFCPUcombo.setTextLimit(8); // max. 99 MHz
		fFCPUcombo.setToolTipText("Target Hardware Clock Frequency in Hz");
		fFCPUcombo.setVisibleItemCount(FCPU_VALUES.length);
		fFCPUcombo.setItems(FCPU_VALUES);

		fFCPUcombo.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				if (fTargetProps != null) {
					fTargetProps.setFCPU(fFCPUcombo.getText());
					fireDataEvent(EventType.F_CPU, fFCPUcombo.getText());
				}
			}
		});

		// Ensure that only integer values are entered
		fFCPUcombo.addVerifyListener(new VerifyListener() {
			public void verifyText(VerifyEvent event) {
				String text = event.text;
				if (!text.matches("[0-9]*")) {
					event.doit = false;
				}
			}
		});
	}
	
	private void addBoardsSection(Composite parent) {

		setupLabel(parent, LABEL_BOARD, 1, SWT.NONE);
		
		fBoardNameToIdMap = new HashMap<>();
		fBoardPreferences.getAvailiableBoardIds().forEach(id -> {
			fBoardNameToIdMap.put(fBoardPreferences.getBoardName(id), id);
		});

		fBoardCombo = new ComboControl(parent, fBoardNameToIdMap.keySet().toArray(new String[fBoardNameToIdMap.size()]));
		setupControl(fBoardCombo, 1, GridData.FILL_HORIZONTAL);
		fBoardCombo.setToolTipText("Arduino board name");
		fBoardCombo.setEnabled(false);

		fBoardCombo.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				String boardId = fBoardNameToIdMap.get(fBoardCombo.getValue());
				fTargetProps.setBoardId(boardId);
				fireDataEvent(EventType.BOARD, boardId);
				updateMCUData(boardId);
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
	}

	/**
	 * Load the actual MCU from the currently selected Programmer and set the MCU combo accordingly.
	 * <p>
	 * This method will start a new Job to load the values and return immediately.
	 * </p>
	 */
	private void loadComboFromDevice() {

		// Disable the Load Button. It is re-enabled by the load job when it finishes.
		fLoadButton.setEnabled(false);
		fLoadButton.setText(TEXT_LOADBUTTON_BUSY);

		// The Job that does the actual loading.
		Job readJob = new Job("Reading MCU Signature") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {

				AVRDudeProperties avrdudeprops = fTargetProps.getAVRDudeProperties();
				try {
					
					monitor.beginTask("Starting AVRDude", 100);

					final String mcuid = AVRDude.getDefault().getAttachedMCU(
							avrdudeprops.getProgrammer(),
							SubMonitor.convert(monitor, 95));

					fTargetProps.setMCUId(mcuid);

					// and update the user interface
					if (!fLoadButton.isDisposed()) {
						fLoadButton.getDisplay().syncExec(new Runnable() {
							public void run() {
								fireDataEvent(EventType.MCU_ID, mcuid);
								updateControlsData();

								// Check if supported by avrdude and set the errorpane as
								// required
								checkAVRDude(mcuid);

								// Check fuse byte settings and pop a message if the settings
								// are not compatible
								checkFuseBytes(mcuid);
							}
						});
					}
					monitor.worked(5);
				} catch (AVRDudeException ade) {
					// Show an Error message and exit
					if (!fLoadButton.isDisposed()) {
						UIJob messagejob = new AVRDudeErrorDialogJob(fLoadButton.getDisplay(),
																		ade,
																		avrdudeprops.getProgrammerId());
						messagejob.setPriority(Job.INTERACTIVE);
						messagejob.schedule();
						try {
							messagejob.join(); // block until the dialog is closed.
						} catch (InterruptedException e) {
							// Don't care if the dialog is interrupted from outside.
						}
					}
				} catch (SWTException swte) {
					// The display has been disposed, so the user is not
					// interested in the results from this job
					return Status.CANCEL_STATUS;
				} finally {
					monitor.done();
					// Enable the Load from MCU Button
					if (!fLoadButton.isDisposed()) {
						fLoadButton.getDisplay().syncExec(new Runnable() {
							public void run() {
								// Re-Enable the Button
								fLoadButton.setEnabled(true);
								fLoadButton.setText(TEXT_LOADBUTTON);
							}
						});
					}
				}

				return Status.OK_STATUS;
			}
		};

		// now set the Job properties and start it
		readJob.setRule(new AVRDudeSchedulingRule(fTargetProps.getAVRDudeProperties().getProgrammer()));
		readJob.setPriority(Job.SHORT);
		readJob.setUser(true);
		readJob.schedule();
	}

	protected void fireDataEvent(EventType type, String data) {
		if (fListeners != null) {
			DataEvent event = new DataEvent(type, data);
			for (DataListener l : fListeners) {
				l.onModify(event);
			}
		}
	}
	
	/**
	 * Check if the given MCU is supported by avrdude and set visibility of the MCU Warning Message
	 * accordingly.
	 * 
	 * @param mcuid
	 *            The MCU id value to test
	 */
	private void checkAVRDude(String mcuid) {
		if (AVRDude.getDefault().hasMCU(mcuid)) {
			fMCUWarningComposite.setVisible(false);
		} else {
			fMCUWarningComposite.setVisible(true);
		}
	}

	/**
	 * Check if the FuseBytesProperties and Lockbits in the current properties are compatible with
	 * the selected mcu. If not, a warning dialog is shown.
	 */
	private void checkFuseBytes(String mcuid) {
		
		if (fResdesc != null) {
		
			IConfiguration cfg = AbstractCBuildPropertyTab.getCfg(fResdesc.getConfiguration());
			AVRDudeProperties avrdudeprops = fTargetProps.getAVRDudeProperties();
	
			// State:
			// 0x00 = neither fuses nor lockbits are written
			// 0x01 = fuses not compatible
			// 0x02 = lockbits not compatible
			// 0x03 = both not compatible
			// The state is used as an index to the String arrays with the texts.
			int state = 0x00;
	
			// Check fuse bytes
			boolean fusewrite = avrdudeprops.getFuseBytes(cfg).getWrite();
			if (fusewrite) {
				boolean fusecompatible = avrdudeprops.getFuseBytes(cfg).isCompatibleWith(mcuid);
				if (!fusecompatible) {
					state |= 0x01;
				}
			}
	
			// check lockbits
			boolean lockwrite = avrdudeprops.getLockbitBytes(cfg).getWrite();
			if (lockwrite) {
				boolean lockcompatible = avrdudeprops.getLockbitBytes(cfg).isCompatibleWith(mcuid);
				if (!lockcompatible) {
					state |= 0x02;
				}
			}
	
			if (!fusewrite && !lockwrite) {
				// Neither Fuses nor Lockbits are written, so no need for a warning.
				// The fuses tab respective lockbits tab will show a warning once the write flag is
				// changed.
				return;
			}
	
			if (state == 0) {
				// both fuses and lockbits are compatible, so no need for a warning.
				return;
			}
	
			// Now show the warning.
			String title = MessageFormat.format(TITLE_FUSEBYTEWARNING, TITLEINSERT[state]);
			String text = MessageFormat.format(TEXT_FUSEBYTEWARNING, TEXTINSERT[state],
					TABNAMEINSERT[state]);
			MessageDialog.openWarning(fMCUcombo.getShell(), title, text);
		}
	}
	
	/**
	 * Convenience method to add a separator bar to the composite.
	 * <p>
	 * The parent composite must have a <code>GridLayout</code>. The separator bar will span all
	 * columns of the parent grid layout.
	 * </p>
	 * 
	 * @param parent
	 *            <code>Composite</code>
	 */
	protected void addSeparator(Composite parent) {
		Label separator = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
		Layout parentlayout = parent.getLayout();
		if (parentlayout instanceof GridLayout) {
			int columns = ((GridLayout) parentlayout).numColumns;
			GridData gridData = new GridData(SWT.FILL, SWT.NONE, true, false, columns, 1);
			separator.setLayoutData(gridData);
		}
	}
	
	/**********************************************
	 * Utility methods for unified widget creation
	 **********************************************/
	protected Label setupLabel(Composite c, String name, int span, int mode) {
		Label l = new Label(c, SWT.NONE);
		l.setText(name);
		setupControl(l, span, mode);
		return l;
	}

	protected Button setupButton(Composite c, String name, int span, int mode) {
		Button b = new Button(c, SWT.PUSH);
		b.setText(name);
		setupControl(b, span, mode);
		GridData g = (GridData)b.getLayoutData();
		g.minimumWidth = AbstractCPropertyTab.BUTTON_WIDTH;
		g.horizontalAlignment = SWT.RIGHT;
		b.setLayoutData(g);
		return b;
	}

	protected void setupControl(Control c, int span, int mode) {
		// although we use GridLayout usually,
		// exceptions can occur: do nothing.
		if (c != null) {
			if (span != 0) {
				GridData gd = new GridData(mode);
				gd.horizontalSpan = span;
				c.setLayoutData(gd);
			}
			Composite p = c.getParent();
			c.setFont(p.getFont());
		}
	}
}
