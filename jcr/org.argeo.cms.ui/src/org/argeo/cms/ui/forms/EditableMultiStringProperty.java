package org.argeo.cms.ui.forms;

import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.argeo.cms.swt.CmsSwtUtils;
import org.argeo.cms.swt.SwtEditablePart;
import org.argeo.cms.ui.widgets.StyledControl;
import org.argeo.eclipse.ui.EclipseUiUtils;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/** Display, add or remove values from a list in a CMS context */
public class EditableMultiStringProperty extends StyledControl implements SwtEditablePart {
	private static final long serialVersionUID = -7044614381252178595L;

	private String propertyName;
	private String message;
	// TODO implement the ability to provide a list of possible values
//	private String[] possibleValues;
	private boolean canEdit;
	private SelectionListener removeValueSL;
	private List<String> values;

	// TODO manage within the CSS
	private int rowSpacing = 5;
	private int rowMarging = 0;
	private int oneValueMargingRight = 5;
	private int btnWidth = 16;
	private int btnHeight = 16;
	private int btnHorizontalIndent = 3;

	public EditableMultiStringProperty(Composite parent, int style, Node node, String propertyName, List<String> values,
			String[] possibleValues, String addValueMsg, SelectionListener removeValueSelectionListener)
			throws RepositoryException {
		super(parent, style, node, true);

		this.propertyName = propertyName;
		this.values = values;
//		this.possibleValues = new String[] { "Un", "Deux", "Trois" };
		this.message = addValueMsg;
		this.canEdit = removeValueSelectionListener != null;
		this.removeValueSL = removeValueSelectionListener;
	}

	public List<String> getValues() {
		return values;
	}

	public void setValues(List<String> values) {
		this.values = values;
	}

	// Row layout items do not need explicit layout data
	protected void setControlLayoutData(Control control) {
	}

	/** To be overridden */
	protected void setContainerLayoutData(Composite composite) {
		composite.setLayoutData(CmsSwtUtils.fillWidth());
	}

	@Override
	public Control getControl() {
		return super.getControl();
	}

	@Override
	protected Control createControl(Composite box, String style) {
		Composite row = new Composite(box, SWT.NO_FOCUS);
		row.setLayoutData(EclipseUiUtils.fillAll());

		RowLayout rl = new RowLayout(SWT.HORIZONTAL);
		rl.wrap = true;
		rl.spacing = rowSpacing;
		rl.marginRight = rl.marginLeft = rl.marginBottom = rl.marginTop = rowMarging;
		row.setLayout(rl);

		if (values != null) {
			for (final String value : values) {
				if (canEdit)
					createRemovableValue(row, SWT.SINGLE, value);
				else
					createValueLabel(row, SWT.SINGLE, value);
			}
		}

		if (!canEdit)
			return row;
		else if (isEditing())
			return createText(row, style);
		else
			return createLabel(row, style);
	}

	/**
	 * Override to provide specific layout for the existing values, typically adding
	 * a pound (#) char for tags or anchor info for browsable links. We assume the
	 * parent composite already has a layout and it is the caller responsibility to
	 * apply corresponding layout data
	 */
	protected Label createValueLabel(Composite parent, int style, String value) {
		Label label = new Label(parent, style);
		label.setText("#" + value);
		CmsSwtUtils.markup(label);
		CmsSwtUtils.style(label, FormStyle.propertyText.style());
		return label;
	}

	private Composite createRemovableValue(Composite parent, int style, String value) {
		Composite valCmp = new Composite(parent, SWT.NO_FOCUS);
		GridLayout gl = EclipseUiUtils.noSpaceGridLayout(new GridLayout(2, false));
		gl.marginRight = oneValueMargingRight;
		valCmp.setLayout(gl);

		createValueLabel(valCmp, SWT.WRAP, value);

		Button deleteBtn = new Button(valCmp, SWT.FLAT);
		deleteBtn.setData(FormConstants.LINKED_VALUE, value);
		deleteBtn.addSelectionListener(removeValueSL);
		CmsSwtUtils.style(deleteBtn, FormStyle.delete.style() + FormStyle.BUTTON_SUFFIX);
		GridData gd = new GridData();
		gd.heightHint = btnHeight;
		gd.widthHint = btnWidth;
		gd.horizontalIndent = btnHorizontalIndent;
		deleteBtn.setLayoutData(gd);

		return valCmp;
	}

	protected Text createText(Composite box, String style) {
		final Text text = new Text(box, getStyle());
		// The "add new value" text is not meant to change, so we can set it on
		// creation
		text.setMessage(message);
		CmsSwtUtils.style(text, style);
		text.setFocus();

		text.addTraverseListener(new TraverseListener() {
			private static final long serialVersionUID = 1L;

			public void keyTraversed(TraverseEvent e) {
				if (e.keyCode == SWT.CR) {
					addValue(text);
					e.doit = false;
				}
			}
		});

		// The OK button does not work with the focusOut listener
		// because focus out is called before the OK button is pressed

		// // we must call layout() now so that the row data can compute the
		// height
		// // of the other controls.
		// text.getParent().layout();
		// int height = text.getSize().y;
		//
		// Button okBtn = new Button(box, SWT.BORDER | SWT.PUSH | SWT.BOTTOM);
		// okBtn.setText("OK");
		// RowData rd = new RowData(SWT.DEFAULT, height - 2);
		// okBtn.setLayoutData(rd);
		//
		// okBtn.addSelectionListener(new SelectionAdapter() {
		// private static final long serialVersionUID = 2780819012423622369L;
		//
		// @Override
		// public void widgetSelected(SelectionEvent e) {
		// addValue(text);
		// }
		// });

		return text;
	}

	/** Performs the real addition, overwrite to make further sanity checks */
	protected void addValue(Text text) {
		String value = text.getText();
		String errMsg = null;

		if (EclipseUiUtils.isEmpty(value))
			return;

		if (values.contains(value))
			errMsg = "Dupplicated value: " + value + ", please correct and try again";
		if (errMsg != null)
			MessageDialog.openError(this.getShell(), "Addition not allowed", errMsg);
		else {
			values.add(value);
			Composite newCmp = createRemovableValue(text.getParent(), SWT.SINGLE, value);
			newCmp.moveAbove(text);
			text.setText("");
			newCmp.getParent().layout();
		}
	}

	protected Label createLabel(Composite box, String style) {
		if (canEdit) {
			Label lbl = new Label(box, getStyle());
			lbl.setText(message);
			CmsSwtUtils.style(lbl, style);
			CmsSwtUtils.markup(lbl);
			if (mouseListener != null)
				lbl.addMouseListener(mouseListener);
			return lbl;
		}
		return null;
	}

	protected void clear(boolean deep) {
		Control child = getControl();
		if (deep)
			super.clear(deep);
		else {
			child.getParent().dispose();
		}
	}

	public void setText(String text) {
		Control child = getControl();
		if (child instanceof Label) {
			Label lbl = (Label) child;
			if (canEdit)
				lbl.setText(text);
			else
				lbl.setText("");
		} else if (child instanceof Text) {
			Text txt = (Text) child;
			txt.setText(text);
		}
	}

	public synchronized void startEditing() {
		CmsSwtUtils.style(getControl(), FormStyle.propertyText.style());
//		getControl().setData(STYLE, FormStyle.propertyText.style());
		super.startEditing();
	}

	public synchronized void stopEditing() {
		CmsSwtUtils.style(getControl(), FormStyle.propertyMessage.style());
//		getControl().setData(STYLE, FormStyle.propertyMessage.style());
		super.stopEditing();
	}

	public String getPropertyName() {
		return propertyName;
	}
}