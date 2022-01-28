package org.argeo.eclipse.ui.jcr;

import javax.jcr.version.VersionHistory;

import org.argeo.jcr.Jcr;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

/** Content provider based on a {@link VersionHistory}. */
public class VersionHistoryContentProvider implements IStructuredContentProvider {
	private static final long serialVersionUID = -4921107883428887012L;

	@Override
	public void dispose() {
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}

	@Override
	public Object[] getElements(Object inputElement) {
		VersionHistory versionHistory = (VersionHistory) inputElement;
		return Jcr.getLinearVersions(versionHistory).toArray();
	}

}
