package org.argeo.cms.users;

import static org.eclipse.swt.SWT.NONE;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.argeo.cms.CmsEditable;
import org.argeo.cms.util.CmsUtils;
import org.argeo.cms.viewers.AbstractPageViewer;
import org.argeo.cms.viewers.EditablePart;
import org.argeo.cms.viewers.Section;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public class UserViewer extends AbstractPageViewer {
	private static final long serialVersionUID = -717973369525981931L;
	private UserPart userPart;

	public UserViewer(Composite parent, int style, Node userNode,
			CmsEditable cmsEditable) throws RepositoryException {
		this(new UserPart(parent, SWT.NO_BACKGROUND, userNode), style,
				userNode, cmsEditable);
	}

	private UserViewer(UserPart userPart, int style, Node userNode,
			CmsEditable cmsEditable) throws RepositoryException {
		super(new Section(userPart, NONE, userNode), style, cmsEditable);
		this.userPart = userPart;
		userPart.createControl(userPart, "cms_test");
		userPart.setStyle("cms_test");
		refresh();
		userPart.setLayoutData(CmsUtils.fillWidth());
		userPart.setMouseListener(getMouseListener());

		// Add other parts
		// userRolesPart.createControl(userPart,"cms_test");
		// userPart.setStyle("cms_test");
		// refresh();
		// userPart.setLayoutData(CmsUtils.fillWidth());
		// userPart.setMouseListener(getMouseListener());

	}

	// private JcrComposite createParents(Composite parent, Node userNode)
	// throws RepositoryException {
	// this.parent = ;
	// return this.parent;
	// }

	@Override
	public Control getControl() {
		return userPart;
	}

	// MOUSE LISTENER
	@Override
	protected MouseListener createMouseListener() {
		return new ML();
	}

	private class ML extends MouseAdapter {
		private static final long serialVersionUID = 8526890859876770905L;

		@Override
		public void mouseDoubleClick(MouseEvent e) {
			if (e.button == 1) {
				Control source = (Control) e.getSource();
				if (getCmsEditable().canEdit()) {
					getCmsEditable().startEditing();
					EditablePart composite = findDataParent(source);
					Point point = new Point(e.x, e.y);
					edit(composite, source.toDisplay(point));
				}
			}
		}
	}

	protected void updateContent(EditablePart part) throws RepositoryException {
		if (part instanceof UserPart)
			((UserPart) part).refresh();
	}

	protected void refresh(Control control) throws RepositoryException {
		if (control instanceof UserPart)
			((UserPart) control).refresh();
	}

}
