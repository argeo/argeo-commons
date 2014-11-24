package org.argeo.cms.text;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;

import org.argeo.cms.CmsEditable;
import org.argeo.cms.CmsLink;
import org.argeo.cms.CmsNames;
import org.argeo.cms.CmsTypes;
import org.argeo.cms.CmsUiProvider;
import org.argeo.cms.CmsUtils;
import org.argeo.cms.viewers.JcrVersionCmsEditable;
import org.argeo.cms.widgets.ScrolledPage;
import org.argeo.jcr.JcrUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/** Display the text of the context, and provide an editor if the user can edit. */
public class WikiPage implements CmsUiProvider, CmsNames {
	@Override
	public Control createUi(Composite parent, Node context)
			throws RepositoryException {
		CmsEditable cmsEditable = new JcrVersionCmsEditable(context);
		if (cmsEditable.canEdit())
			new TextEditorHeader(cmsEditable, parent, SWT.NONE)
					.setLayoutData(CmsUtils.fillWidth());

		ScrolledPage page = new ScrolledPage(parent, SWT.NONE);
		page.setLayout(CmsUtils.noSpaceGridLayout());
		GridData textGd = CmsUtils.fillAll();
		page.setLayoutData(textGd);

		if (context.isNodeType(CmsTypes.CMS_TEXT)) {
			new StandardTextEditor(page, SWT.NONE, context, cmsEditable);
		} else if (context.isNodeType(NodeType.NT_FOLDER)
				|| context.getPath().equals("/")) {
			parent.setBackgroundMode(SWT.INHERIT_NONE);
			Node indexNode = JcrUtils.getOrAdd(context, CMS_INDEX,
					CmsTypes.CMS_TEXT);
			new StandardTextEditor(page, SWT.NONE, indexNode, cmsEditable);
			textGd.heightHint = 400;

			for (NodeIterator ni = context.getNodes(); ni.hasNext();) {
				Node textNode = ni.nextNode();
				if (textNode.isNodeType(NodeType.NT_FOLDER))
					new CmsLink(textNode.getName() + "/", textNode.getPath())
							.createUi(parent, textNode);
			}
			for (NodeIterator ni = context.getNodes(); ni.hasNext();) {
				Node textNode = ni.nextNode();
				if (textNode.isNodeType(CmsTypes.CMS_TEXT)
						&& !textNode.getName().equals(CMS_INDEX))
					new CmsLink(textNode.getName(), textNode.getPath())
							.createUi(parent, textNode);
			}
		}
		return page;
	}
}
