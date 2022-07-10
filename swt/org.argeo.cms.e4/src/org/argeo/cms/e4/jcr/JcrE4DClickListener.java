package org.argeo.cms.e4.jcr;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.argeo.cms.swt.CmsException;
import org.argeo.cms.ui.jcr.JcrDClickListener;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.modeling.EPartService.PartState;
import org.eclipse.jface.viewers.TreeViewer;

public class JcrE4DClickListener extends JcrDClickListener {
	EPartService partService;

	public JcrE4DClickListener(TreeViewer nodeViewer, EPartService partService) {
		super(nodeViewer);
		this.partService = partService;
	}

	@Override
	protected void openNode(Node node) {
		MPart part = partService.createPart(JcrNodeEditor.DESCRIPTOR_ID);
		try {
			part.setLabel(node.getName());
			part.getPersistedState().put("nodeWorkspace", node.getSession().getWorkspace().getName());
			part.getPersistedState().put("nodePath", node.getPath());
		} catch (RepositoryException e) {
			throw new CmsException("Cannot open " + node, e);
		}

		// the provided part is be shown
		partService.showPart(part, PartState.ACTIVATE);
	}

}
