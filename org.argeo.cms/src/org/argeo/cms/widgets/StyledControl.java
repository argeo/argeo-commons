package org.argeo.cms.widgets;

import javax.jcr.Item;
import javax.jcr.RepositoryException;

import org.argeo.cms.CmsConstants;
import org.argeo.cms.CmsNames;
import org.argeo.cms.CmsUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/** Editable text part displaying styled text. */
public abstract class StyledControl extends JcrComposite implements
		CmsConstants, CmsNames {
	private static final long serialVersionUID = -6372283442330912755L;
	private Control control;

	private Composite container;
	private Composite box;

	protected MouseListener mouseListener;

	private Boolean editing = Boolean.FALSE;

	public StyledControl(Composite parent, int swtStyle) {
		super(parent, swtStyle);
		setLayout(CmsUtils.noSpaceGridLayout());
	}

	public StyledControl(Composite parent, int style, Item item)
			throws RepositoryException {
		super(parent, style, item);
	}

	public StyledControl(Composite parent, int style, Item item,
			boolean cacheImmediately) throws RepositoryException {
		super(parent, style, item, cacheImmediately);
	}

	protected abstract Control createControl(Composite box, String style);

	protected Composite createBox(Composite parent) {
		Composite box = new Composite(parent, SWT.INHERIT_DEFAULT);
		setContainerLayoutData(box);
		box.setLayout(CmsUtils.noSpaceGridLayout());
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
		control.setLayoutData(CmsUtils.fillWidth());
	}

	/** To be overridden */
	protected void setContainerLayoutData(Composite composite) {
		composite.setLayoutData(CmsUtils.fillWidth());
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
}
