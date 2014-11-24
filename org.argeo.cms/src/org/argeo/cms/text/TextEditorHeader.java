package org.argeo.cms.text;

import java.util.Observable;
import java.util.Observer;

import org.argeo.cms.CmsEditable;
import org.argeo.cms.CmsUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

/** Adds editing capabilities to a page editing text */
public class TextEditorHeader implements SelectionListener, Observer {
	private static final long serialVersionUID = 4186756396045701253L;

	private final CmsEditable cmsEditable;
	private Button publish;

	private Composite parent;
	private Composite display;
	private Object layoutData;

	public TextEditorHeader(CmsEditable cmsEditable, Composite parent, int style) {
		this.cmsEditable = cmsEditable;
		this.parent = parent;
		if (this.cmsEditable instanceof Observable)
			((Observable) this.cmsEditable).addObserver(this);
		refresh();
	}

	protected void refresh() {
		if (display != null && !display.isDisposed())
			display.dispose();
		display = null;
		publish = null;
		if (cmsEditable.isEditing()) {
			display = new Composite(parent, SWT.NONE);
			// display.setBackgroundMode(SWT.INHERIT_NONE);
			display.setLayoutData(layoutData);
			display.setLayout(CmsUtils.noSpaceGridLayout());
			CmsUtils.style(display, TextStyles.TEXT_EDITOR_HEADER);
			publish = new Button(display, SWT.FLAT | SWT.PUSH);
			publish.setText(getPublishButtonLabel());
			CmsUtils.style(publish, TextStyles.TEXT_EDITOR_HEADER);
			publish.addSelectionListener(this);
			display.moveAbove(null);
		}
		parent.layout();
	}

	private String getPublishButtonLabel() {
		if (cmsEditable.isEditing())
			return "Publish";
		else
			return "Edit";
	}

	@Override
	public void widgetSelected(SelectionEvent e) {
		if (e.getSource() == publish) {
			if (cmsEditable.isEditing()) {
				cmsEditable.stopEditing();
			} else {
				cmsEditable.startEditing();
			}
			// publish.setText(getPublishButtonLabel());
		}
	}

	@Override
	public void widgetDefaultSelected(SelectionEvent e) {
	}

	@Override
	public void update(Observable o, Object arg) {
		if (o == cmsEditable) {
			// publish.setText(getPublishButtonLabel());
			refresh();
		}
	}

	public void setLayoutData(Object layoutData) {
		this.layoutData = layoutData;
		if (display != null && !display.isDisposed())
			display.setLayoutData(layoutData);
	}

}