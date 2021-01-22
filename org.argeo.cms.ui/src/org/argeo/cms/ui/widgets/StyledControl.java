package org.argeo.cms.ui.widgets;

import javax.jcr.Item;

import org.argeo.cms.ui.CmsConstants;
import org.argeo.cms.ui.util.CmsUiUtils;
import org.argeo.eclipse.ui.specific.EclipseUiSpecificUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/** Editable text part displaying styled text. */
public abstract class StyledControl extends JcrComposite implements CmsConstants {
	private static final long serialVersionUID = -6372283442330912755L;
	private Control control;

	private Composite container;
	private Composite box;

	protected MouseListener mouseListener;
	protected FocusListener focusListener;

	private Boolean editing = Boolean.FALSE;

	private Composite ancestorToLayout;

	public StyledControl(Composite parent, int swtStyle) {
		super(parent, swtStyle);
		setLayout(CmsUiUtils.noSpaceGridLayout());
	}

	public StyledControl(Composite parent, int style, Item item) {
		super(parent, style, item);
	}

	public StyledControl(Composite parent, int style, Item item, boolean cacheImmediately) {
		super(parent, style, item, cacheImmediately);
	}

	protected abstract Control createControl(Composite box, String style);

	protected Composite createBox() {
		Composite box = new Composite(container, SWT.INHERIT_DEFAULT);
		setContainerLayoutData(box);
		box.setLayout(CmsUiUtils.noSpaceGridLayout(3));
		return box;
	}

	protected Composite createContainer() {
		Composite container = new Composite(this, SWT.INHERIT_DEFAULT);
		setContainerLayoutData(container);
		container.setLayout(CmsUiUtils.noSpaceGridLayout());
		return container;
	}

	public Control getControl() {
		return control;
	}

	protected synchronized Boolean isEditing() {
		return editing;
	}

	public synchronized void startEditing() {
		assert !isEditing();
		editing = true;
		// int height = control.getSize().y;
		String style = (String) EclipseUiSpecificUtils.getStyleData(control);
		clear(false);
		refreshControl(style);

		// add the focus listener to the newly created edition control
		if (focusListener != null)
			control.addFocusListener(focusListener);
	}

	public synchronized void stopEditing() {
		assert isEditing();
		editing = false;
		String style = (String) EclipseUiSpecificUtils.getStyleData(control);
		clear(false);
		refreshControl(style);
	}

	protected void refreshControl(String style) {
		control = createControl(box, style);
		setControlLayoutData(control);
		if (ancestorToLayout != null)
			ancestorToLayout.layout(true, true);
		else
			getParent().layout(true, true);
	}

	public void setStyle(String style) {
		Object currentStyle = null;
		if (control != null)
			currentStyle = EclipseUiSpecificUtils.getStyleData(control);
		if (currentStyle != null && currentStyle.equals(style))
			return;

		clear(true);
		refreshControl(style);

		if (style != null) {
			CmsUiUtils.style(box, style + "_box");
			CmsUiUtils.style(container, style + "_container");
		}
	}

	/** To be overridden */
	protected void setControlLayoutData(Control control) {
		control.setLayoutData(CmsUiUtils.fillWidth());
	}

	/** To be overridden */
	protected void setContainerLayoutData(Composite composite) {
		composite.setLayoutData(CmsUiUtils.fillWidth());
	}

	protected void clear(boolean deep) {
		if (deep) {
			for (Control control : getChildren())
				control.dispose();
			container = createContainer();
			box = createBox();
		} else {
			control.dispose();
		}
	}

	public void setMouseListener(MouseListener mouseListener) {
		if (this.mouseListener != null && control != null)
			control.removeMouseListener(this.mouseListener);
		this.mouseListener = mouseListener;
		if (control != null && this.mouseListener != null)
			control.addMouseListener(mouseListener);
	}

	public void setFocusListener(FocusListener focusListener) {
		if (this.focusListener != null && control != null)
			control.removeFocusListener(this.focusListener);
		this.focusListener = focusListener;
		if (control != null && this.focusListener != null)
			control.addFocusListener(focusListener);
	}

	public void setAncestorToLayout(Composite ancestorToLayout) {
		this.ancestorToLayout = ancestorToLayout;
	}

}
