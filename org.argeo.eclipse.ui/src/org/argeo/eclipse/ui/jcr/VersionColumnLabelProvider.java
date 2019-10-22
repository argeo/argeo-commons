package org.argeo.eclipse.ui.jcr;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.version.Version;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.swt.graphics.Image;

/** Simplifies writing JCR-based column label provider. */
public class VersionColumnLabelProvider extends ColumnLabelProvider {
	private static final long serialVersionUID = -6117690082313161159L;

	protected String getVersionText(Version version) throws RepositoryException {
		return super.getText(version);
	}

	protected String getVersionToolTipText(Version version) throws RepositoryException {
		return super.getToolTipText(version);
	}

	protected Image getVersionImage(Version version) throws RepositoryException {
		return super.getImage(version);
	}

	protected String getUserName(Version version) throws RepositoryException {
		Node node = version.getFrozenNode();
		if(node.hasProperty(Property.JCR_LAST_MODIFIED_BY))
			return node.getProperty(Property.JCR_LAST_MODIFIED_BY).getString();
		if(node.hasProperty(Property.JCR_CREATED_BY))
			return node.getProperty(Property.JCR_CREATED_BY).getString();
		return null;
	}
	
//	protected String getActivityTitle(Version version) throws RepositoryException {
//		Node activity = getActivity(version);
//		if (activity == null)
//			return null;
//		if (activity.hasProperty("jcr:activityTitle"))
//			return activity.getProperty("jcr:activityTitle").getString();
//		else
//			return activity.getName();
//	}
//
//	protected Node getActivity(Version version) throws RepositoryException {
//		if (version.hasProperty(Property.JCR_ACTIVITY)) {
//			return version.getProperty(Property.JCR_ACTIVITY).getNode();
//		} else
//			return null;
//	}

	@Override
	public String getText(Object element) {
		try {
			return getVersionText((Version) element);
		} catch (RepositoryException e) {
			throw new RuntimeException("Runtime repository exception when accessing " + element, e);
		}
	}

	@Override
	public Image getImage(Object element) {
		try {
			return getVersionImage((Version) element);
		} catch (RepositoryException e) {
			throw new RuntimeException("Runtime repository exception when accessing " + element, e);
		}
	}

	@Override
	public String getToolTipText(Object element) {
		try {
			return getVersionToolTipText((Version) element);
		} catch (RepositoryException e) {
			throw new RuntimeException("Runtime repository exception when accessing " + element, e);
		}
	}

}
