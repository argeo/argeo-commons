package org.argeo.cms.viewers;

import java.util.Observable;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import javax.jcr.version.VersionManager;

import org.argeo.cms.CmsEditable;
import org.argeo.cms.CmsEditionEvent;
import org.argeo.cms.CmsException;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

/** Provides the CmsEditable semantic based on JCR versioning. */
public class JcrVersionCmsEditable extends Observable implements CmsEditable {
	private final String nodePath;// cache
	private final VersionManager versionManager;
	private final Boolean canEdit;

	public JcrVersionCmsEditable(Node node) throws RepositoryException {
		this.nodePath = node.getPath();
		if (node.getSession().hasPermission(node.getPath(),
				Session.ACTION_ADD_NODE)) {
			canEdit = true;
			if (!node.isNodeType(NodeType.MIX_VERSIONABLE)) {
				node.addMixin(NodeType.MIX_VERSIONABLE);
				node.getSession().save();
			}
			versionManager = node.getSession().getWorkspace()
					.getVersionManager();
		} else {
			canEdit = false;
			versionManager = null;
		}

		// bind keys
		if (canEdit) {
			Display display = Display.getCurrent();
			display.setData(RWT.ACTIVE_KEYS, new String[] { "CTRL+RETURN",
					"CTRL+E" });
			display.addFilter(SWT.KeyDown, new Listener() {
				private static final long serialVersionUID = -4378653870463187318L;

				public void handleEvent(Event e) {
					boolean ctrlPressed = (e.stateMask & SWT.CTRL) != 0;
					if (ctrlPressed && e.keyCode == '\r')
						stopEditing();
					else if (ctrlPressed && e.keyCode == 'E')
						stopEditing();
				}
			});
		}
	}

	@Override
	public Boolean canEdit() {
		return canEdit;
	}

	public Boolean isEditing() {
		try {
			if (!canEdit())
				return false;
			return versionManager.isCheckedOut(nodePath);
		} catch (RepositoryException e) {
			throw new CmsException("Cannot check whether " + nodePath
					+ " is editing", e);
		}
	}

	@Override
	public void startEditing() {
		try {
			versionManager.checkout(nodePath);
			setChanged();
		} catch (RepositoryException e1) {
			throw new CmsException("Cannot publish " + nodePath);
		}
		notifyObservers(new CmsEditionEvent(nodePath,
				CmsEditionEvent.START_EDITING));
	}

	@Override
	public void stopEditing() {
		try {
			versionManager.checkin(nodePath);
			setChanged();
		} catch (RepositoryException e1) {
			throw new CmsException("Cannot publish " + nodePath, e1);
		}
		notifyObservers(new CmsEditionEvent(nodePath,
				CmsEditionEvent.STOP_EDITING));
	}
}
