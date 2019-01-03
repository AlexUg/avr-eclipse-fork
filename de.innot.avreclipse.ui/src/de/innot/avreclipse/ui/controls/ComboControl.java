package de.innot.avreclipse.ui.controls;



import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;


public class ComboControl extends Composite {
	
	ControlListener shellControlListener = new ControlListener() {
		
		@Override
		public void controlResized(ControlEvent e) {
		}
		
		@Override
		public void controlMoved(ControlEvent e) {
			updateListBounds();
		}
	};
	
	ControlListener textControlListener = new ControlListener() {
		
		@Override
		public void controlResized(ControlEvent e) {
			updateListBounds();
		}
		
		@Override
		public void controlMoved(ControlEvent e) {
		}
	};
	
	ModifyListener textModifyListener = new ModifyListener() {
		
		@Override
		public void modifyText(ModifyEvent e) {
			String textStr = text.getText().toLowerCase();
			String name = availableValues.get(textStr);
			if (name != null) {
				selectedValue = name;
			} else {
				selectedValue = "";
			}
			fireValueSelected(selectedValue);
			if (list.getItemCount() < 2) {
				updatePopupVisible(false);
			} else {
				updatePopupVisible(true);
			}
			valuePrefix = new StringBuffer(textStr);
		}
	};
	
	VerifyListener textVerifyLisener = new VerifyListener() {
		
		@Override
		public void verifyText(VerifyEvent e) {
			String oldPrefix = valuePrefix.toString();
			if (e.text.isEmpty()) {
				if (e.start < e.end) {
					valuePrefix.replace(e.start, e.end, "");
				}
			} else {
				if (e.start < e.end) {
					valuePrefix.replace(e.start, e.end, e.text);
				} else {
					valuePrefix.insert(e.start, e.text);
				}
			}
			String[] filter = getFilteredRegion(valuePrefix.toString());
			boolean result = filter != null;
			if (result) {
				updateListContents(filter);
			} else {
				valuePrefix = new StringBuffer(oldPrefix);
			}
			e.doit = result;
		}
		
	};
	
	FocusListener textFocusListener = new FocusListener() {
		
		@Override
		public void focusLost(FocusEvent e) {
			updatePopupVisible(false);
		}
		
		@Override
		public void focusGained(FocusEvent e) {
			if (!valueSelectEvent) {
				updatePopupVisible(true);
			}
			valueSelectEvent = false;
		}
	};
	
	KeyListener textKeyListener = new KeyListener() {
		
		@Override
		public void keyReleased(KeyEvent e) {
		}
		
		@Override
		public void keyPressed(KeyEvent e) {
			switch (e.keyCode) {
			case SWT.ARROW_UP:
				if (!popup.isDisposed()
						&& popup.isVisible()) {
					int idx = list.getSelectionIndex();
					idx--;
					if (idx < 0) {
						idx = 0;
					}
					list.select(idx);
				}
				break;
			case SWT.ARROW_DOWN:
				if (!popup.isDisposed()
						&& popup.isVisible()) {
					int idx = list.getSelectionIndex();
					idx++;
					if (idx >= list.getItemCount()) {
						idx = list.getItemCount() - 1;
					}
					list.select(idx);
				}
				break;
			}
		}
	};
	
	TraverseListener textTraverseListener = new TraverseListener() {
		
		@Override
		public void keyTraversed(TraverseEvent e) {
			if (currentPopupVisible) {
				switch (e.detail) {
				case SWT.TRAVERSE_RETURN:
					onValueSelected();
				case SWT.TRAVERSE_ESCAPE:
					if (selectedValue != null) {
						text.setText(selectedValue);
					}
					updatePopupVisible(false);
				case SWT.TRAVERSE_ARROW_NEXT:
				case SWT.TRAVERSE_ARROW_PREVIOUS:
					e.doit = false;
				default:
					break;
				}
			}
		}
	};
	
	private Text text;
	private Shell popup;
	private List list;
	
	boolean currentPopupVisible = false;
	boolean valueSelectEvent = false;
	
	
	private String selectedValue;
	private StringBuffer valuePrefix = new StringBuffer();
	private TreeMap<String, String> availableValues;
	
	private ArrayList<SelectionListener> listeners = new ArrayList<>();
	

	public ComboControl(Composite parent, String[] availableValues) {
		super(parent, SWT.NONE);
		this.availableValues = new TreeMap<>();
		for (String mcuName : availableValues) {
			this.availableValues.put(mcuName.toLowerCase(), mcuName);
		}
		GridLayout layout = new GridLayout(1, false);
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		setLayout(layout);
		createText();
		createPopup();
		parent.getShell().addControlListener(shellControlListener);
	}

	public String getValue() {
		return selectedValue;
	}
	
	public void setValue(String mcuName) {
		this.selectedValue = mcuName;
		if (text != null) {
			text.setText(mcuName);
		}
	}
	
	public void addSelectionListener(SelectionListener listener) {
		if (!listeners.contains(listener)) {
			listeners.add(listener);
		}
	}
	
	public void removeSelectionListener(SelectionListener listener) {
		listeners.remove(listener);
	}

	@Override
	public void dispose() {
		if (popup.isDisposed()) {
			popup.dispose();
		}
		super.dispose();
	}
	
	@Override
	protected void finalize() throws Throwable {
		if (popup.isDisposed()) {
			popup.dispose();
		}
		super.finalize();
	}

	private void createText() {
		text = new Text(this, SWT.SINGLE | SWT.BORDER | SWT.BORDER_DASH);
		text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		if (selectedValue != null) {
			text.setText(selectedValue);
		}
		text.addModifyListener(textModifyListener);
		text.addVerifyListener(textVerifyLisener);
		text.addFocusListener(textFocusListener);
		text.addKeyListener(textKeyListener);
		text.addTraverseListener(textTraverseListener);
		text.addControlListener(textControlListener);
	}
	
	private void createPopup() {
		popup = new Shell(getShell(), SWT.NO_TRIM | SWT.ON_TOP);
		GridLayout layout = new GridLayout(1, false);
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		popup.setLayout(layout);
		list = new List(popup, SWT.SINGLE | SWT.V_SCROLL | SWT.H_SCROLL);
		list.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		list.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				onValueSelected();
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		updateListContents(null);
	}
	
	private void updateListBounds() {
		Rectangle popuRect = new Rectangle(0, 0, 0, 0);
		Rectangle textRect = text.getBounds();
		Point absTextPosition = this.toDisplay(textRect.x, textRect.y);
		popuRect.x = absTextPosition.x;
		popuRect.y = absTextPosition.y + textRect.height + text.getBorderWidth();
		popuRect.width = textRect.width;
		popuRect.height = 100;
		popup.setBounds(popuRect);
		list.setSize(new Point(popuRect.width - 4, popuRect.height - 4));
	}
	
	private void updateListContents(String[] filter) {
		if (filter == null) {
			list.setItems(availableValues.values().toArray(new String[availableValues.size()]));
		} else {
			list.setItems(filter);
		}
		if (list.getItemCount() > 0) {
			list.setSelection(0);
		}
	}
	
	private String[] getFilteredRegion(String valuePart) {
		if ((valuePart != null)
				&& !valuePart.isEmpty()) {
			valuePart = valuePart.toLowerCase();
			ArrayList<String> result = new ArrayList<>();
			for (Entry<String, String> condidate : availableValues.entrySet()) {
				if (condidate.getKey().contains(valuePart)) {
					result.add(condidate.getValue());
				}
			}
			return result.isEmpty() ? null : result.toArray(new String[result.size()]);
		} else {
			return availableValues.values().toArray(new String[availableValues.size()]);
		}
	}
	
	protected void onValueSelected() {
		valueSelectEvent = true;
		int idx = list.getSelectionIndex();
		if (idx >= 0) {
			String selectedValue = list.getItem(idx);
			text.setText(selectedValue);
			updatePopupVisible(false);
		}
	}
	
	protected void updatePopupVisible(boolean popupVisible) {
		if (!popup.isDisposed()
				&& text.isFocusControl()
				&& (popupVisible != currentPopupVisible)) {
			currentPopupVisible = popupVisible;
			if (currentPopupVisible) {
				updateListBounds();
			}
			popup.setVisible(currentPopupVisible);
		}
	}
	
	protected void fireValueSelected(String value) {
		Event e = new Event();
		e.widget = this;
		SelectionEvent event = new SelectionEvent(e);
		event.display = getDisplay();
		event.text = value;
		for (SelectionListener listener : listeners) {
			listener.widgetSelected(event);
		}
	}

}
