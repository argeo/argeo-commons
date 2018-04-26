package org.argeo.cms.text;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;

import org.argeo.cms.CmsNames;
import org.argeo.cms.ui.CmsEditable;
import org.argeo.cms.ui.CmsUiProvider;
import org.argeo.cms.util.CmsLink;
import org.argeo.cms.util.CmsUtils;
import org.argeo.cms.viewers.JcrVersionCmsEditable;
import org.argeo.cms.widgets.ScrolledPage;
import org.argeo.jcr.JcrUtils;
import org.argeo.jcr.docbook.DocBookTypes;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * Display the text of the context, and provide an editor if the user can edit.
 */
public class DocumentPage implements CmsUiProvider {
	@Override
	public Control createUi(Composite parent, Node context) throws RepositoryException {
		CmsEditable cmsEditable = new JcrVersionCmsEditable(context);
		if (cmsEditable.canEdit())
			new TextEditorHeader(cmsEditable, parent, SWT.NONE).setLayoutData(CmsUtils.fillWidth());

		ScrolledPage page = new ScrolledPage(parent, SWT.NONE);
		page.setLayout(CmsUtils.noSpaceGridLayout());
		GridData textGd = CmsUtils.fillAll();
		page.setLayoutData(textGd);

		if (context.isNodeType(DocBookTypes.ARTICLE)) {
			new DocumentTextEditor(page, SWT.NONE, context, cmsEditable);
		} else {
			parent.setBackgroundMode(SWT.INHERIT_NONE);
			if (context.getSession().hasPermission(context.getPath(), Session.ACTION_ADD_NODE)) {
				Node indexNode = JcrUtils.getOrAdd(context, CmsNames.CMS_INDEX, DocBookTypes.ARTICLE);
				new DocumentTextEditor(page, SWT.NONE, indexNode, cmsEditable);
				textGd.heightHint = 400;

				for (NodeIterator ni = context.getNodes(); ni.hasNext();) {
					Node textNode = ni.nextNode();
					if (textNode.isNodeType(NodeType.NT_FOLDER))
						new CmsLink(textNode.getName() + "/", textNode.getPath()).createUi(parent, textNode);
				}
				for (NodeIterator ni = context.getNodes(); ni.hasNext();) {
					Node textNode = ni.nextNode();
					if (textNode.isNodeType(DocBookTypes.ARTICLE) && !textNode.getName().equals(CmsNames.CMS_INDEX))
						new CmsLink(textNode.getName(), textNode.getPath()).createUi(parent, textNode);
				}
			}
		}
		return page;
	}
}
