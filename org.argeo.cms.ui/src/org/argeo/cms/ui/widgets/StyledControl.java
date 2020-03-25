package org.argeo.cms.ui.widgets;

import javax.jcr.Item;
import javax.jcr.RepositoryException;

import org.argeo.cms.ui.CmsConstants;
import org.argeo.cms.ui.util.CmsUiUtils;
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

	public StyledControl(Composite parent, int swtStyle) {
		super(parent, swtStyle);
		setLayout(CmsUiUtils.noSpaceGridLayout());
	}

	public StyledControl(Composite parent, int style, Item item) throws RepositoryException {
		super(parent, style, item);
	}

	public StyledControl(Composite parent, int style, Item item, boolean cacheImmediately) throws RepositoryException {
		super(parent, style, item, cacheImmediately);
	}

	protected abstract Control createControl(Composite box, String style);

	protected Composite createBox(Composite parent) {
		Composite box = new Composite(parent, SWT.INHERIT_DEFAULT);
		setContainerLayoutData(box);
		box.setLayout(CmsUiUtils.noSpaceGridLayout());
		// new Label(box, SWT.NONE).setText("BOX");
		return box;
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
		String style = (String) control.getData(STYLE);
		clear(false);
		control = createControl(box, style);
		setControlLayoutData(control);

		// add the focus listener to the newly created edition control
		if (focusListener != null)
			control.addFocusListener(focusListener);
	}

	public synchronized void stopEditing() {
		assert isEditing();
		editing = false;
		String style = (String) control.getData(STYLE);
		clear(false);
		control = createControl(box, style);
		setControlLayoutData(control);
	}

	public void setStyle(String style) {
		Object currentStyle = null;
		if (control != null)
			currentStyle = control.getData(STYLE);
		if (currentStyle != null && currentStyle.equals(style))
			return;

		// Integer preferredHeight = control != null ? control.getSize().y :
		// null;
		clear(true);
		control = createControl(box, style);
		setControlLayoutData(control);

		control.getParent().setData(STYLE, style + "_box");
		control.getParent().getParent().setData(STYLE, style + "_container");
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
			container = createBox(this);
			box = createBox(container);
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
}