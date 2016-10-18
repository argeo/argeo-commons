package org.argeo.cms.ui.workbench.internal.useradmin.providers;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.osgi.service.useradmin.User;

/** Default drag listener to modify group and users via the UI */
public class UserDragListener implements DragSourceListener {
	private static final long serialVersionUID = -2074337775033781454L;
	private final Viewer viewer;

	public UserDragListener(Viewer viewer) {
		this.viewer = viewer;
	}

	public void dragStart(DragSourceEvent event) {
		// TODO implement finer checks
		IStructuredSelection selection = (IStructuredSelection) viewer
				.getSelection();
		if (selection.isEmpty() || selection.size() > 1)
			event.doit = false;
		else
			event.doit = true;
	}

	public void dragSetData(DragSourceEvent event) {
		// TODO Support multiple selection
		Object obj = ((IStructuredSelection) viewer.getSelection())
				.getFirstElement();
		if (obj != null) {
			User user = (User) obj;
			event.data = user.getName();
		}
	}

	public void dragFinished(DragSourceEvent event) {
	}
}
