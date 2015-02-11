package org.argeo.cms.text;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.argeo.cms.CmsException;
import org.argeo.cms.CmsNames;
import org.argeo.cms.CmsTypes;

/** Based on HTML with a few Wiki-like shortcuts. */
public class IdentityTextInterpreter implements TextInterpreter, CmsNames {

	@Override
	public void write(Item item, String content) {
		try {
			if (item instanceof Node) {
				Node node = (Node) item;
				if (node.isNodeType(CmsTypes.CMS_STYLED)) {
					String raw = convertToStorage(node, content);
					node.setProperty(CMS_CONTENT, raw);
				} else {
					throw new CmsException("Don't know how to interpret "
							+ node);
				}
			} else {// property
				Property property = (Property) item;
				property.setValue(content);
			}
			item.getSession().save();
		} catch (RepositoryException e) {
			throw new CmsException("Cannot set content on " + item, e);
		}
	}

	@Override
	public String read(Item item) {
		try {
			String raw = raw(item);
			return convertFromStorage(item, raw);
		} catch (RepositoryException e) {
			throw new CmsException("Cannot get " + item + " for edit", e);
		}
	}

	@Override
	public String raw(Item item) {
		try {
			if (item instanceof Node) {
				Node node = (Node) item;
				if (node.isNodeType(CmsTypes.CMS_STYLED)) {
					// WORKAROUND FOR BROKEN PARARAPHS
					if (!node.hasProperty(CMS_CONTENT)) {
						node.setProperty(CMS_CONTENT, "");
						node.getSession().save();
					}
					
					return node.getProperty(CMS_CONTENT).getString();
				} else {
					throw new CmsException("Don't know how to interpret "
							+ node);
				}
			} else {// property
				Property property = (Property) item;
				return property.getString();
			}
		} catch (RepositoryException e) {
			throw new CmsException("Cannot get " + item + " content", e);
		}
	}

	protected String convertToStorage(Item item, String content)
			throws RepositoryException {
		return content;

	}

	protected String convertFromStorage(Item item, String content)
			throws RepositoryException {
		return content;
	}
}
