package org.argeo.jcr;

import java.beans.PropertyDescriptor;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.ArgeoException;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

public class BeanNodeMapper implements NodeMapper {
	private final static Log log = LogFactory.getLog(BeanNodeMapper.class);

	private final static String NODE_VALUE = "value";

	// private String keyNode = "bean:key";
	private String uuidProperty = "uuid";
	private String classProperty = "class";

	private Boolean versioning = false;
	private Boolean strictUuidReference = false;

	// TODO define a primaryNodeType Strategy
	private String primaryNodeType = null;

	private ClassLoader classLoader = getClass().getClassLoader();

	private NodeMapperProvider nodeMapperProvider;

	/**
	 * exposed method to retrieve a bean from a node
	 */
	public Object load(Node node) {
		try {
			if (nodeMapperProvider != null) {
				NodeMapper nodeMapper = nodeMapperProvider.findNodeMapper(node);
				if (nodeMapper != this) {
					return nodeMapper.load(node);
				}
			}
			return nodeToBean(node);
		} catch (RepositoryException e) {
			throw new ArgeoException("Cannot load object from node " + node, e);
		}
	}

	/** Update an existing node with an object */
	public void update(Node node, Object obj) {
		try {
			if (nodeMapperProvider != null) {

				NodeMapper nodeMapper = nodeMapperProvider.findNodeMapper(node);
				if (nodeMapper != this) {
					nodeMapper.update(node, obj);
				} else
					beanToNode(createBeanWrapper(obj), node);
			} else
				beanToNode(createBeanWrapper(obj), node);
		} catch (RepositoryException e) {
			throw new ArgeoException("Cannot update node " + node + " with "
					+ obj, e);
		}
	}

	/**
	 * if no storage path is given; we use canonical path
	 * 
	 * @see this.storagePath()
	 */
	public Node save(Session session, Object obj) {
		return save(session, storagePath(obj), obj);
	}

	/**
	 * Create a new node to store an object. If the parentNode doesn't exist, it
	 * is created
	 * 
	 * the primaryNodeType may be initialized before
	 */
	public Node save(Session session, String path, Object obj) {
		try {
			final Node node;
			String parentPath = JcrUtils.parentPath(path);
			// find or create parent node
			Node parentNode;
			if (session.itemExists(path))
				parentNode = (Node) session.getItem(parentPath);
			else {
				parentNode = JcrUtils.mkdirs(session, parentPath, null,
						versioning);
			}
			// create node

			if (primaryNodeType != null)
				node = parentNode.addNode(JcrUtils.lastPathElement(path),
						primaryNodeType);
			else
				node = parentNode.addNode(JcrUtils.lastPathElement(path));

			// Check specific cases
			if (nodeMapperProvider != null) {
				NodeMapper nodeMapper = nodeMapperProvider.findNodeMapper(node);
				if (nodeMapper != this) {
					nodeMapper.update(node, obj);
					return node;
				}
			}
			update(node, obj);
			return node;
		} catch (ArgeoException e) {
			throw e;
		} catch (Exception e) {
			throw new ArgeoException("Cannot save or update " + obj + " under "
					+ path, e);
		}
	}

	/**
	 * Parse the FQN of a class to string with '/' delimiters Prefix the
	 * returned string with "/objects/"
	 */
	public String storagePath(Object obj) {
		String clss = obj.getClass().getName();
		StringBuffer buf = new StringBuffer("/objects/");
		StringTokenizer st = new StringTokenizer(clss, ".");
		while (st.hasMoreTokens()) {
			buf.append(st.nextToken()).append('/');
		}
		buf.append(obj.toString());
		return buf.toString();
	}

	@SuppressWarnings("unchecked")
	/** 
	 * Transforms a node into an object of the class defined by classProperty Property
	 */
	protected Object nodeToBean(Node node) throws RepositoryException {
		if (log.isTraceEnabled())
			log.trace("Load     " + node);

		try {
			String clssName = node.getProperty(classProperty).getValue()
					.getString();

			BeanWrapper beanWrapper = createBeanWrapper(loadClass(clssName));

			// process properties
			PropertyIterator propIt = node.getProperties();
			props: while (propIt.hasNext()) {
				Property prop = propIt.nextProperty();
				if (!beanWrapper.isWritableProperty(prop.getName()))
					continue props;

				PropertyDescriptor pd = beanWrapper.getPropertyDescriptor(prop
						.getName());
				Class propClass = pd.getPropertyType();

				if (log.isTraceEnabled())
					log.trace("Load " + prop + ", propClass=" + propClass
							+ ", property descriptor=" + pd);

				// primitive list
				if (propClass != null && List.class.isAssignableFrom(propClass)) {
					List<Object> lst = new ArrayList<Object>();
					Class<?> valuesClass = classFromProperty(prop);
					if (valuesClass != null)
						for (Value value : prop.getValues()) {
							lst.add(asObject(value, valuesClass));
						}
					continue props;
				}

				// Case of other type of property accepted by jcr
				// Long, Double, String, Binary, Date, Boolean, Name
				Object value = asObject(prop.getValue(), pd.getPropertyType());
				if (value != null)
					beanWrapper.setPropertyValue(prop.getName(), value);
			}

			// process children nodes
			NodeIterator nodeIt = node.getNodes();
			nodes: while (nodeIt.hasNext()) {
				Node childNode = nodeIt.nextNode();
				String name = childNode.getName();
				if (!beanWrapper.isWritableProperty(name))
					continue nodes;

				PropertyDescriptor pd = beanWrapper.getPropertyDescriptor(name);
				Class propClass = pd.getPropertyType();

				// objects list
				if (propClass != null && List.class.isAssignableFrom(propClass)) {
					String lstClass = childNode.getProperty(classProperty)
							.getString();
					List<Object> lst;
					try {
						lst = (List<Object>) loadClass(lstClass).newInstance();
					} catch (Exception e) {
						lst = new ArrayList<Object>();
					}

					if (childNode.hasNodes()) {
						// Look for children nodes
						NodeIterator valuesIt = childNode.getNodes();
						while (valuesIt.hasNext()) {
							Node lstValueNode = valuesIt.nextNode();
							Object lstValue = nodeToBean(lstValueNode);
							lst.add(lstValue);
						}
					} else {
						// look for a property with the same name which will
						// provide
						// primitives
						Property childProp = childNode.getProperty(childNode
								.getName());
						Class<?> valuesClass = classFromProperty(childProp);
						if (valuesClass != null)
							if (childProp.getDefinition().isMultiple())
								for (Value value : childProp.getValues()) {
									lst.add(asObject(value, valuesClass));
								}
							else
								lst.add(asObject(childProp.getValue(),
										valuesClass));
					}
					beanWrapper.setPropertyValue(name, lst);
					continue nodes;
				}

				// objects map
				if (propClass != null && Map.class.isAssignableFrom(propClass)) {
					String mapClass = childNode.getProperty(classProperty)
							.getString();
					Map<Object, Object> map;
					try {
						map = (Map<Object, Object>) loadClass(mapClass)
								.newInstance();
					} catch (Exception e) {
						map = new HashMap<Object, Object>();
					}

					// properties
					PropertyIterator keysPropIt = childNode.getProperties();
					keyProps: while (keysPropIt.hasNext()) {
						Property keyProp = keysPropIt.nextProperty();
						// FIXME: use property editor
						String key = keyProp.getName();
						if (classProperty.equals(key))
							continue keyProps;

						Class keyPropClass = classFromProperty(keyProp);
						if (keyPropClass != null) {
							Object mapValue = asObject(keyProp.getValue(),
									keyPropClass);
							map.put(key, mapValue);
						}
					}

					// node
					NodeIterator keysIt = childNode.getNodes();
					while (keysIt.hasNext()) {
						Node mapValueNode = keysIt.nextNode();
						// FIXME: use property editor
						Object key = mapValueNode.getName();

						Object mapValue = nodeToBean(mapValueNode);

						map.put(key, mapValue);
					}
					beanWrapper.setPropertyValue(name, map);
					continue nodes;
				}

				// default
				Object value = nodeToBean(childNode);
				beanWrapper.setPropertyValue(name, value);

			}
			return beanWrapper.getWrappedInstance();
		} catch (Exception e) {
			throw new ArgeoException("Cannot map node " + node, e);
		}
	}

	/**
	 * Transforms an object to the specified jcr Node in order to persist it.
	 * 
	 * @param beanWrapper
	 * @param node
	 * @throws RepositoryException
	 */
	protected void beanToNode(BeanWrapper beanWrapper, Node node)
			throws RepositoryException {
		properties: for (PropertyDescriptor pd : beanWrapper
				.getPropertyDescriptors()) {
			String name = pd.getName();
			if (!beanWrapper.isReadableProperty(name))
				continue properties;// skip

			Object value = beanWrapper.getPropertyValue(name);
			if (value == null) {
				// remove values when updating
				if (node.hasProperty(name))
					node.setProperty(name, (Value) null);
				if (node.hasNode(name))
					node.getNode(name).remove();

				continue properties;
			}

			// if (uuidProperty != null && uuidProperty.equals(name)) {
			// // node.addMixin(ArgeoJcrConstants.MIX_REFERENCEABLE);
			// node.setProperty(ArgeoJcrConstants.JCR_UUID, value.toString());
			// continue properties;
			// }

			if ("class".equals(name)) {
				if (classProperty != null) {
					node.setProperty(classProperty, ((Class<?>) value)
							.getName());
					// TODO: store a class hierarchy?
				}
				continue properties;
			}

			// Some bean reference other classes. We must deal with this case
			if (value instanceof Class<?>) {
				node.setProperty(name, ((Class<?>) value).getName());
				continue properties;
			}

			Value val = asValue(node.getSession(), value);
			if (val != null) {
				node.setProperty(name, val);
				continue properties;
			}

			if (value instanceof List<?>) {
				List<?> lst = (List<?>) value;
				addList(node, name, lst);
				continue properties;
			}

			if (value instanceof Map<?, ?>) {
				Map<?, ?> map = (Map<?, ?>) value;
				addMap(node, name, map);
				continue properties;
			}

			BeanWrapper child = createBeanWrapper(value);
			// TODO: delegate to another mapper

			// TODO: deal with references
			// Node childNode = findChildReference(session, child);
			// if (childNode != null) {
			// node.setProperty(name, childNode);
			// continue properties;
			// }

			// default case (recursive)
			if (node.hasNode(name)) {// update
				// TODO: optimize
				node.getNode(name).remove();
			}
			Node childNode = node.addNode(name);
			beanToNode(child, childNode);
		}
	}

	/**
	 * Process specific case of list
	 * 
	 * @param node
	 * @param name
	 * @param lst
	 * @throws RepositoryException
	 */
	protected void addList(Node node, String name, List<?> lst)
			throws RepositoryException {
		if (node.hasNode(name)) {// update
			// TODO: optimize
			node.getNode(name).remove();
		}

		Node listNode = node.addNode(name);
		listNode.setProperty(classProperty, lst.getClass().getName());
		Value[] values = new Value[lst.size()];
		boolean atLeastOneSet = false;
		for (int i = 0; i < lst.size(); i++) {
			Object lstValue = lst.get(i);
			values[i] = asValue(node.getSession(), lstValue);
			if (values[i] != null) {
				atLeastOneSet = true;
			} else {
				Node childNode = findChildReference(node.getSession(),
						createBeanWrapper(lstValue));
				if (childNode != null) {
					values[i] = node.getSession().getValueFactory()
							.createValue(childNode);
					atLeastOneSet = true;
				}
			}
		}

		// will be either properties or nodes, not both
		if (!atLeastOneSet && lst.size() != 0) {
			for (Object lstValue : lst) {
				Node childNode = listNode.addNode(NODE_VALUE);
				beanToNode(createBeanWrapper(lstValue), childNode);
			}
		} else {
			listNode.setProperty(name, values);
		}
	}

	/**
	 * Process specific case of maps.
	 * 
	 * @param node
	 * @param name
	 * @param map
	 * @throws RepositoryException
	 */
	protected void addMap(Node node, String name, Map<?, ?> map)
			throws RepositoryException {
		if (node.hasNode(name)) {// update
			// TODO: optimize
			node.getNode(name).remove();
		}

		Node mapNode = node.addNode(name);
		mapNode.setProperty(classProperty, map.getClass().getName());
		for (Object key : map.keySet()) {
			Object mapValue = map.get(key);
			// PropertyEditor pe = beanWrapper.findCustomEditor(key.getClass(),
			// null);
			String keyStr;
			// if (pe == null) {
			if (key instanceof CharSequence)
				keyStr = key.toString();
			else
				throw new ArgeoException(
						"Cannot find property editor for class "
								+ key.getClass());
			// } else {
			// pe.setValue(key);
			// keyStr = pe.getAsText();
			// }
			// TODO: check string format

			Value mapVal = asValue(node.getSession(), mapValue);
			if (mapVal != null)
				mapNode.setProperty(keyStr, mapVal);
			else {
				Node entryNode = mapNode.addNode(keyStr);
				beanToNode(createBeanWrapper(mapValue), entryNode);
			}

		}

	}

	protected BeanWrapper createBeanWrapper(Object obj) {
		return new BeanWrapperImpl(obj);
	}

	protected BeanWrapper createBeanWrapper(Class<?> clss) {
		return new BeanWrapperImpl(clss);
	}

	/** Returns null if value cannot be found */
	protected Value asValue(Session session, Object value)
			throws RepositoryException {
		ValueFactory valueFactory = session.getValueFactory();
		if (value instanceof Integer)
			return valueFactory.createValue((Integer) value);
		else if (value instanceof Long)
			return valueFactory.createValue((Long) value);
		else if (value instanceof Float)
			return valueFactory.createValue((Float) value);
		else if (value instanceof Double)
			return valueFactory.createValue((Double) value);
		else if (value instanceof Boolean)
			return valueFactory.createValue((Boolean) value);
		else if (value instanceof Calendar)
			return valueFactory.createValue((Calendar) value);
		else if (value instanceof Date) {
			Calendar cal = new GregorianCalendar();
			cal.setTime((Date) value);
			return valueFactory.createValue(cal);
		} else if (value instanceof CharSequence)
			return valueFactory.createValue(value.toString());
		else if (value instanceof InputStream)
			return valueFactory.createValue((InputStream) value);
		else
			return null;
	}

	protected Class<?> classFromProperty(Property property)
			throws RepositoryException {
		switch (property.getType()) {
		case PropertyType.LONG:
			return Long.class;
		case PropertyType.DOUBLE:
			return Double.class;
		case PropertyType.STRING:
			return String.class;
		case PropertyType.BOOLEAN:
			return Boolean.class;
		case PropertyType.DATE:
			return Calendar.class;
		case PropertyType.NAME:
			return null;
		default:
			throw new ArgeoException("Cannot find class for property "
					+ property + ", type="
					+ PropertyType.nameFromValue(property.getType()));
		}
	}

	protected Object asObject(Value value, Class<?> propClass)
			throws RepositoryException {
		if (propClass.equals(Integer.class))
			return (int) value.getLong();
		else if (propClass.equals(Long.class))
			return value.getLong();
		else if (propClass.equals(Float.class))
			return (float) value.getDouble();
		else if (propClass.equals(Double.class))
			return value.getDouble();
		else if (propClass.equals(Boolean.class))
			return value.getBoolean();
		else if (CharSequence.class.isAssignableFrom(propClass))
			return value.getString();
		else if (InputStream.class.isAssignableFrom(propClass))
			return value.getStream();
		else if (Calendar.class.isAssignableFrom(propClass))
			return value.getDate();
		else if (Date.class.isAssignableFrom(propClass))
			return value.getDate().getTime();
		else
			return null;
	}

	protected Node findChildReference(Session session, BeanWrapper child)
			throws RepositoryException {
		if (child.isReadableProperty(uuidProperty)) {
			String childUuid = child.getPropertyValue(uuidProperty).toString();
			try {
				return session.getNodeByUUID(childUuid);
			} catch (ItemNotFoundException e) {
				if (strictUuidReference)
					throw new ArgeoException("No node found with uuid "
							+ childUuid, e);
			}
		}
		return null;
	}

	protected Class<?> loadClass(String name) {
		// log.debug("Class loader: " + classLoader);
		try {
			return classLoader.loadClass(name);
		} catch (ClassNotFoundException e) {
			throw new ArgeoException("Cannot load class " + name, e);
		}
	}

	protected String propertyName(String name) {
		return name;
	}

	public void setVersioning(Boolean versioning) {
		this.versioning = versioning;
	}

	public void setUuidProperty(String uuidProperty) {
		this.uuidProperty = uuidProperty;
	}

	public void setClassProperty(String classProperty) {
		this.classProperty = classProperty;
	}

	public void setStrictUuidReference(Boolean strictUuidReference) {
		this.strictUuidReference = strictUuidReference;
	}

	public void setPrimaryNodeType(String primaryNodeType) {
		this.primaryNodeType = primaryNodeType;
	}

	public void setClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	public void setNodeMapperProvider(NodeMapperProvider nodeMapperProvider) {
		this.nodeMapperProvider = nodeMapperProvider;
	}

	public String getPrimaryNodeType() {
		return this.primaryNodeType;
	}

	public String getClassProperty() {
		return this.classProperty;
	}
}
