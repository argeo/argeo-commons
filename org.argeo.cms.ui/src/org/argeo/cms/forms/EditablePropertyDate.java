package org.argeo.cms.forms;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.argeo.cms.util.CmsUtils;
import org.argeo.cms.viewers.EditablePart;
import org.argeo.cms.widgets.StyledControl;
import org.argeo.eclipse.ui.EclipseUiUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/** CMS form part to display and edit a date */
public class EditablePropertyDate extends StyledControl implements EditablePart {
	private static final long serialVersionUID = 2500215515778162468L;

	// Context
	private String propertyName;
	private String message;
	private DateFormat dateFormat;

	// UI Objects
	private Text dateTxt;
	private Button openCalBtn;

	// TODO manage within the CSS
	private int fieldBtnSpacing = 5;

	/**
	 * 
	 * @param parent
	 * @param style
	 * @param node
	 * @param propertyName
	 * @param message
	 * @param dateFormat
	 *            provide a {@link DateFormat} as contract to be able to
	 *            read/write dates as strings
	 * @throws RepositoryException
	 */
	public EditablePropertyDate(Composite parent, int style, Node node,
			String propertyName, String message, DateFormat dateFormat)
			throws RepositoryException {
		super(parent, style, node, false);

		this.propertyName = propertyName;
		this.message = message;
		this.dateFormat = dateFormat;

		if (node.hasProperty(propertyName)) {
			this.setStyle(FormStyle.propertyText.style());
			this.setText(dateFormat.format(node.getProperty(propertyName)
					.getDate().getTime()));
		} else {
			this.setStyle(FormStyle.propertyMessage.style());
			this.setText(message);
		}
	}

	public void setText(String text) {
		Control child = getControl();
		if (child instanceof Label) {
			Label lbl = (Label) child;
			if (EclipseUiUtils.isEmpty(text))
				lbl.setText(message);
			else
				lbl.setText(text);
		} else if (child instanceof Text) {
			Text txt = (Text) child;
			if (EclipseUiUtils.isEmpty(text)) {
				txt.setText("");
			} else
				txt.setText(text);
		}
	}

	public synchronized void startEditing() {
		// if (dateTxt != null && !dateTxt.isDisposed())
		getControl().setData(STYLE, FormStyle.propertyText.style());
		super.startEditing();
	}

	public synchronized void stopEditing() {
		if (EclipseUiUtils.isEmpty(dateTxt.getText()))
			getControl().setData(STYLE, FormStyle.propertyMessage.style());
		else
			getControl().setData(STYLE, FormStyle.propertyText.style());
		super.stopEditing();
	}

	public String getPropertyName() {
		return propertyName;
	}

	@Override
	protected Control createControl(Composite box, String style) {
		if (isEditing()) {
			return createCustomEditableControl(box, style);
		} else
			return createLabel(box, style);
	}

	protected Label createLabel(Composite box, String style) {
		Label lbl = new Label(box, getStyle() | SWT.WRAP);
		lbl.setLayoutData(CmsUtils.fillWidth());
		CmsUtils.style(lbl, style);
		CmsUtils.markup(lbl);
		if (mouseListener != null)
			lbl.addMouseListener(mouseListener);
		return lbl;
	}

	private Control createCustomEditableControl(Composite box, String style) {
		box.setLayoutData(CmsUtils.fillWidth());
		Composite dateComposite = new Composite(box, SWT.NONE);
		GridLayout gl = EclipseUiUtils.noSpaceGridLayout(new GridLayout(2,
				false));
		gl.horizontalSpacing = fieldBtnSpacing;
		dateComposite.setLayout(gl);
		dateTxt = new Text(dateComposite, SWT.BORDER);
		CmsUtils.style(dateTxt, style);
		dateTxt.setLayoutData(new GridData(120, SWT.DEFAULT));
		dateTxt.setToolTipText("Enter a date with form \""
				+ FormUtils.DEFAULT_SHORT_DATE_FORMAT
				+ "\" or use the calendar");
		openCalBtn = new Button(dateComposite, SWT.FLAT);
		CmsUtils.style(openCalBtn, FormStyle.calendar.style()
				+ FormStyle.BUTTON_SUFFIX);
		GridData gd = new GridData(SWT.CENTER, SWT.CENTER, false, false);
		gd.heightHint = 17;
		openCalBtn.setLayoutData(gd);
		// openCalBtn.setImage(PeopleRapImages.CALENDAR_BTN);

		openCalBtn.addSelectionListener(new SelectionAdapter() {
			private static final long serialVersionUID = 1L;

			public void widgetSelected(SelectionEvent event) {
				CalendarPopup popup = new CalendarPopup(dateTxt);
				popup.open();
			}
		});

		// dateTxt.addFocusListener(new FocusListener() {
		// private static final long serialVersionUID = 1L;
		//
		// @Override
		// public void focusLost(FocusEvent event) {
		// String newVal = dateTxt.getText();
		// // Enable reset of the field
		// if (FormUtils.notNull(newVal))
		// calendar = null;
		// else {
		// try {
		// Calendar newCal = parseDate(newVal);
		// // DateText.this.setText(newCal);
		// calendar = newCal;
		// } catch (ParseException pe) {
		// // Silent. Manage error popup?
		// if (calendar != null)
		// EditablePropertyDate.this.setText(calendar);
		// }
		// }
		// }
		//
		// @Override
		// public void focusGained(FocusEvent event) {
		// }
		// });
		return dateTxt;
	}

	protected void clear(boolean deep) {
		Control child = getControl();
		if (deep || child instanceof Label)
			super.clear(deep);
		else {
			child.getParent().dispose();
		}
	}

	/** Enable setting a custom tooltip on the underlying text */
	@Deprecated
	public void setToolTipText(String toolTipText) {
		dateTxt.setToolTipText(toolTipText);
	}

	@Deprecated
	/** Enable setting a custom message on the underlying text */
	public void setMessage(String message) {
		dateTxt.setMessage(message);
	}

	@Deprecated
	public void setText(Calendar cal) {
		String newValueStr = "";
		if (cal != null)
			newValueStr = dateFormat.format(cal.getTime());
		if (!newValueStr.equals(dateTxt.getText()))
			dateTxt.setText(newValueStr);
	}

	// UTILITIES TO MANAGE THE CALENDAR POPUP
	// TODO manage the popup shell in a cleaner way
	private class CalendarPopup extends Shell {
		private static final long serialVersionUID = 1L;
		private DateTime dateTimeCtl;

		public CalendarPopup(Control source) {
			super(source.getDisplay(), SWT.NO_TRIM | SWT.BORDER | SWT.ON_TOP);
			populate();
			// Add border and shadow style
			CmsUtils.markup(CalendarPopup.this);
			CmsUtils.style(CalendarPopup.this, FormStyle.popupCalendar.style());
			pack();
			layout();
			setLocation(source.toDisplay((source.getLocation().x - 2),
					(source.getSize().y) + 3));

			addShellListener(new ShellAdapter() {
				private static final long serialVersionUID = 5178980294808435833L;

				@Override
				public void shellDeactivated(ShellEvent e) {
					close();
					dispose();
				}
			});
			open();
		}

		private void setProperty() {
			// Direct set does not seems to work. investigate
			// cal.set(dateTimeCtl.getYear(), dateTimeCtl.getMonth(),
			// dateTimeCtl.getDay(), 12, 0);
			Calendar cal = new GregorianCalendar();
			cal.set(Calendar.YEAR, dateTimeCtl.getYear());
			cal.set(Calendar.MONTH, dateTimeCtl.getMonth());
			cal.set(Calendar.DAY_OF_MONTH, dateTimeCtl.getDay());
			String dateStr = dateFormat.format(cal.getTime());
			dateTxt.setText(dateStr);
		}

		protected void populate() {
			setLayout(EclipseUiUtils.noSpaceGridLayout());

			dateTimeCtl = new DateTime(this, SWT.CALENDAR);
			dateTimeCtl.setLayoutData(EclipseUiUtils.fillAll());

			Calendar calendar = FormUtils.parseDate(dateFormat,
					dateTxt.getText());

			if (calendar != null)
				dateTimeCtl.setDate(calendar.get(Calendar.YEAR),
						calendar.get(Calendar.MONTH),
						calendar.get(Calendar.DAY_OF_MONTH));

			dateTimeCtl.addSelectionListener(new SelectionAdapter() {
				private static final long serialVersionUID = -8414377364434281112L;

				@Override
				public void widgetSelected(SelectionEvent e) {
					setProperty();
				}
			});

			dateTimeCtl.addMouseListener(new MouseListener() {
				private static final long serialVersionUID = 1L;

				@Override
				public void mouseUp(MouseEvent e) {
				}

				@Override
				public void mouseDown(MouseEvent e) {
				}

				@Override
				public void mouseDoubleClick(MouseEvent e) {
					setProperty();
					close();
					dispose();
				}
			});
		}
	}
}