package org.argeo.cms.ux.acr;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.argeo.api.acr.Content;
import org.argeo.api.acr.NamespaceUtils;
import org.argeo.cms.ux.widgets.AbstractDataPart;
import org.argeo.cms.ux.widgets.HierarchicalPart;

public class ContentHierarchicalPart extends AbstractDataPart<Content, Content> implements HierarchicalPart<Content> {
	@Override
	public List<Content> getChildren(Content content) {
		List<Content> res = new ArrayList<>();
		if (isLeaf(content))
			return res;
		if (content == null)
			return res;
		for (Iterator<Content> it = content.iterator(); it.hasNext();) {
			res.add(it.next());
		}

		return res;
	}

	protected boolean isLeaf(Content content) {
		return false;
	}

	@Override
	public String getText(Content model) {
		try {
			return NamespaceUtils.toPrefixedName(model.getName());
		} catch (IllegalStateException e) {
			return model.getName().toString();
		}
	}

}
