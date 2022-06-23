package org.argeo.cms.ui.forms;

import java.util.Observable;
import java.util.Observer;

import javax.jcr.Node;

import org.argeo.api.cms.ux.CmsEditable;
import org.argeo.cms.swt.CmsSwtUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

/** Add life cycle management abilities to an editable form page */
public class FormEditorHeader implements SelectionListener, Observer {
	private static final long serialVersionUID = 7392898696542484282L;

	// private final Node context;
	private final CmsEditable cmsEditable;
	private Button publishBtn;

	// Should we provide here the ability to switch from read only to edition
	// mode?
	// private Button editBtn;
	// private boolean readOnly;

	// TODO add information about the current node status, typically if it is
	// dirty or not

	private Composite parent;
	private Composite display;
	private Object layoutData;

	public FormEditorHeader(Composite parent, int style, Node context,
			CmsEditable cmsEditable) {
		this.cmsEditable = cmsEditable;
		this.parent = parent;
		// readOnly = SWT.READ_ONLY == (style & SWT.READ_ONLY);
		// this.context = context;
		if (this.cmsEditable instanceof Observable)
			((Observable) this.cmsEditable).addObserver(this);
		refresh();
	}

	public void setLayoutData(Object layoutData) {
		this.layoutData = layoutData;
		if (display != null && !display.isDisposed())
			display.setLayoutData(layoutData);
	}

	protected void refresh() {
		if (display != null && !display.isDisposed())
			display.dispose();

		display = new Composite(parent, SWT.NONE);
		display.setLayoutData(layoutData);

		CmsSwtUtils.style(display, FormStyle.header.style());
		display.setBackgroundMode(SWT.INHERIT_FORCE);

		display.setLayout(CmsSwtUtils.noSpaceGridLayout());

		publishBtn = createSimpleBtn(display, getPublishButtonLabel());
		display.moveAbove(null);
		parent.layout();
	}

	private Button createSimpleBtn(Composite parent, String label) {
		Button button = new Button(parent, SWT.FLAT | SWT.PUSH);
		button.setText(label);
		CmsSwtUtils.style(button, FormStyle.header.style());
		button.addSelectionListener(this);
		return button;
	}

	private String getPublishButtonLabel() {
		// Rather check if the current node differs from what has been
		// previously committed
		// For the time being, we always reach here, the underlying CmsEditable
		// is always editing.
		if (cmsEditable.isEditing())
			return " Publish ";
		else
			return " Edit ";
	}

	@Override
	public void widgetSelected(SelectionEvent e) {
		if (e.getSource() == publishBtn) {
			// For the time being, the underlying CmsEditable
			// is always editing when we reach this point
			if (cmsEditable.isEditing()) {
				// we always leave the node in a check outed state
				cmsEditable.stopEditing();
				cmsEditable.startEditing();
			} else {
				cmsEditable.startEditing();
			}
		}
	}

	@Override
	public void widgetDefaultSelected(SelectionEvent e) {
	}

	@Override
	public void update(Observable o, Object arg) {
		if (o == cmsEditable) {
			refresh();
		}
	}
}