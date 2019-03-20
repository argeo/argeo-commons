package org.argeo.fm.jcr;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import freemarker.template.SimpleScalar;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateNodeModel;
import freemarker.template.TemplateSequenceModel;

public class JcrModel implements TemplateNodeModel, TemplateHashModel {
	private final Node node;

	public JcrModel(Node node) {
		this.node = node;
	}

	@Override
	public TemplateSequenceModel getChildNodes() throws TemplateModelException {
		try {
			return new NodeIteratorModel(node.getNodes());
		} catch (RepositoryException e) {
			throw new TemplateModelException("Cannot list children of " + node, e);
		}
	}

	@Override
	public String getNodeName() throws TemplateModelException {
		try {
			return node.getName();
		} catch (RepositoryException e) {
			throw new TemplateModelException("Cannot get name of " + node, e);
		}
	}

	@Override
	public String getNodeNamespace() throws TemplateModelException {
		// TODO find out namespace
		return null;
	}

	@Override
	public String getNodeType() throws TemplateModelException {
		try {
			return node.getPrimaryNodeType().getName();
		} catch (RepositoryException e) {
			throw new TemplateModelException("Cannot get node type of " + node, e);
		}
	}

	@Override
	public TemplateNodeModel getParentNode() throws TemplateModelException {
		try {
			Node parent = node.getParent();
			if (parent == null)
				return null;
			return new JcrModel(parent);
		} catch (RepositoryException e) {
			throw new TemplateModelException("Cannot get parent of " + node, e);
		}
	}

	@Override
	public TemplateModel get(String key) throws TemplateModelException {
		try {
			Property property = node.getProperty(key);
			if (property == null)
				return null;
			return new SimpleScalar(property.getString());
		} catch (RepositoryException e) {
			throw new TemplateModelException("Cannot get property " + key + " of " + node, e);
		}
	}

	@Override
	public boolean isEmpty() throws TemplateModelException {
		// JCR default properties always accessible
		return false;
	}

	// HASH

}
