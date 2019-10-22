package org.argeo.eclipse.ui.jcr;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;

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
		try {
			VersionHistory versionHistory = (VersionHistory) inputElement;
			List<Version> lst = new ArrayList<>();
			VersionIterator vit = versionHistory.getAllLinearVersions();
			while (vit.hasNext())
				lst.add(vit.nextVersion());
			Collections.reverse(lst);
			return lst.toArray();
		} catch (RepositoryException e) {
			throw new IllegalStateException("Cannot list versions", e);
		}
	}

}
