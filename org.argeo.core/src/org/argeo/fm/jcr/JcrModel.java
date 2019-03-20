package org.argeo.fm.jcr;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import freemarker.template.SimpleScalar;
import freemarker.template.TemplateCollectionModel;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateHashModelEx2;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateModelIterator;
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

	// HASH

	@Override
	public TemplateModel get(String key) throws TemplateModelException {
		try {
			if ("jcr:path".equals(key))
				return new SimpleScalar(node.getPath());
			if ("jcr:name".equals(key))
				return new SimpleScalar(node.getName());
			if ("jcr:properties".equals(key))
				return new PropertiesModel();
			if ("jcr:parent".equals(key))
				return node.getParent() != null ? new JcrModel(node.getParent()) : null;

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
		return false;
	}

	public Node getNode() {
		return node;
	}

	protected TemplateModel propertyValues(Property property) throws RepositoryException {
		if (!property.isMultiple())
			return new SimpleScalar(property.getString());
		Value[] values = property.getValues();
		StringBuilder sb = new StringBuilder();
		for (Value value : values) {
			sb.append(value.getString()).append('\n');
		}
		return new SimpleScalar(sb.toString());
	}

	class PropertiesModel implements TemplateHashModelEx2 {
		@Override
		public TemplateModel get(String key) throws TemplateModelException {
			return JcrModel.this.get(key);
		}

		@Override
		public boolean isEmpty() throws TemplateModelException {
			return false;
		}

		@Override
		public TemplateCollectionModel keys() throws TemplateModelException {
			try {
				PropertyIterator pit = node.getProperties();
				return new TemplateCollectionModel() {

					@Override
					public TemplateModelIterator iterator() throws TemplateModelException {
						return new TemplateModelIterator() {

							@Override
							public TemplateModel next() throws TemplateModelException {
								try {
									return new SimpleScalar(pit.nextProperty().getName());
								} catch (RepositoryException e) {
									throw new TemplateModelException("Cannot list properties of " + node, e);
								}
							}

							@Override
							public boolean hasNext() throws TemplateModelException {
								return pit.hasNext();
							}
						};
					}
				};
			} catch (RepositoryException e) {
				throw new TemplateModelException("Cannot list properties of " + node, e);
			}
		}

		@Override
		public int size() throws TemplateModelException {
			try {
				PropertyIterator pit = node.getProperties();
				return (int) pit.getSize();
			} catch (RepositoryException e) {
				throw new TemplateModelException("Cannot list properties of " + node, e);
			}
		}

		@Override
		public TemplateCollectionModel values() throws TemplateModelException {
			try {
				PropertyIterator pit = node.getProperties();
				return new TemplateCollectionModel() {

					@Override
					public TemplateModelIterator iterator() throws TemplateModelException {
						return new TemplateModelIterator() {

							@Override
							public TemplateModel next() throws TemplateModelException {
								try {
									return propertyValues(pit.nextProperty());
								} catch (RepositoryException e) {
									throw new TemplateModelException("Cannot list properties of " + node, e);
								}
							}

							@Override
							public boolean hasNext() throws TemplateModelException {
								return pit.hasNext();
							}
						};
					}
				};
			} catch (RepositoryException e) {
				throw new TemplateModelException("Cannot list properties of " + node, e);
			}
		}

		@Override
		public KeyValuePairIterator keyValuePairIterator() throws TemplateModelException {
			try {
				PropertyIterator pit = node.getProperties();
				return new KeyValuePairIterator() {

					@Override
					public boolean hasNext() throws TemplateModelException {
						return pit.hasNext();
					}

					@Override
					public KeyValuePair next() throws TemplateModelException {
						Property property = pit.nextProperty();
						return new KeyValuePair() {

							@Override
							public TemplateModel getValue() throws TemplateModelException {
								try {
									return propertyValues(property);
								} catch (RepositoryException e) {
									throw new TemplateModelException("Cannot list properties of " + node, e);
								}
							}

							@Override
							public TemplateModel getKey() throws TemplateModelException {
								try {
									return new SimpleScalar(property.getName());
								} catch (RepositoryException e) {
									throw new TemplateModelException("Cannot list properties of " + node, e);
								}
							}
						};
					}
				};
			} catch (RepositoryException e) {
				throw new TemplateModelException("Cannot list properties of " + node, e);
			}
		}

	}
}
