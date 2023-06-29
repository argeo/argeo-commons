package org.argeo.cms.ux.acr;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.argeo.api.acr.Content;
import org.argeo.cms.ux.widgets.AbstractHierarchicalPart;
import org.argeo.cms.ux.widgets.HierarchicalPart;

/** A {@link HierarchicalPart} based on {@link Content}. */
public class ContentHierarchicalPart extends AbstractHierarchicalPart<Content> implements HierarchicalPart<Content> {
	@Override
	public List<Content> getChildren(Content parent) {
		List<Content> res = new ArrayList<>();
		if (isLeaf(parent))
			return res;
		if (parent == null)
			return res;
		for (Iterator<Content> it = parent.iterator(); it.hasNext();) {
			res.add(it.next());
		}

		return res;
	}

	protected boolean isLeaf(Content content) {
		return false;
	}
}
