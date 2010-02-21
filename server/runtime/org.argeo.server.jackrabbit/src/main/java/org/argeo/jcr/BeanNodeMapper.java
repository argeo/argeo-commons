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

import javax.jcr.Item;
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

//import org.springframework.beans.BeanWrapperImpl;

public class BeanNodeMapper {
	private final static Log log = LogFactory.getLog(BeanNodeMapper.class);

	private final static String NODE_VALUE = "value";

	// private String keyNode = "bean:key";
	private String uuidProperty = "uuid";
	private String classProperty = "class";

	private Boolean versioning = false;
	private Boolean strictUuidReference = false;
	private String primaryNodeType = null;

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

	public Node saveOrUpdate(Session session, Object obj) {
		return saveOrUpdate(session, storagePath(obj), obj);
	}

	public Node saveOrUpdate(Session session, String path, Object obj) {
		try {
			BeanWrapper beanWrapper = createBeanWrapper(obj);
			final Node node;
			if (session.itemExists(path)) {
				Item item = session.getItem(path);
				if (!item.isNode())
					throw new ArgeoException("An item exist under " + path
							+ " but it is not a node: " + item);
				node = (Node) item;
			} else {
				String parentPath = JcrUtils.parentPath(path);
				Node parentNode;
				if (session.itemExists(path))
					parentNode = (Node) session.getItem(parentPath);
				else
					parentNode = JcrUtils.mkdirs(session, parentPath, null,
							versioning);
				// create node
				if (primaryNodeType != null)
					node = parentNode.addNode(JcrUtils.lastPathElement(path),
							primaryNodeType);
				else
					node = parentNode.addNode(JcrUtils.lastPathElement(path));
			}

			beanToNode(session, beanWrapper, node);
			return node;
		} catch (ArgeoException e) {
			throw e;
		} catch (Exception e) {
			throw new ArgeoException("Cannot save or update " + obj + " under "
					+ path, e);
		}
	}

	@SuppressWarnings("unchecked")
	public Object nodeToBean(Node node) throws RepositoryException {

		String clssName = node.getProperty(classProperty).getValue()
				.getString();

		if (log.isDebugEnabled())
			log.debug("Map node " + node.getPath() + " to bean " + clssName);

		BeanWrapper beanWrapper;
		try {
			// FIXME: use OSGi compatible class loading
			Class clss = Class.forName(clssName);
			beanWrapper = new BeanWrapperImpl(clss);
		} catch (ClassNotFoundException e1) {
			throw new ArgeoException("Cannot initialize been wrapper for node "
					+ node.getPath(), e1);
		}

		// process properties
		PropertyIterator propIt = node.getProperties();
		props: while (propIt.hasNext()) {
			Property prop = propIt.nextProperty();
			if (!beanWrapper.isWritableProperty(prop.getName()))
				continue props;

			PropertyDescriptor pd = beanWrapper.getPropertyDescriptor(prop
					.getName());
			Class propClass = pd.getPropertyType();

			// list
			if (propClass != null && List.class.isAssignableFrom(propClass)) {
				List<Object> lst = new ArrayList<Object>();
				Class<?> valuesClass = classFromProperty(prop);
				if (valuesClass != null)
					for (Value value : prop.getValues()) {
						lst.add(asObject(value, valuesClass));
					}
				continue props;
			}

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

			log.debug(childNode.getName() + "=" + propClass);

			if (propClass != null && List.class.isAssignableFrom(propClass)) {
				String lstClass = childNode.getProperty(classProperty)
						.getString();
				// FIXME: use OSGi compatible class loading
				List<Object> lst;
				try {
					lst = (List<Object>) Class.forName(lstClass).newInstance();
				} catch (Exception e) {
					lst = new ArrayList<Object>();
				}

				NodeIterator valuesIt = childNode.getNodes();
				while (valuesIt.hasNext()) {
					Node lstValueNode = valuesIt.nextNode();
					Object lstValue = nodeToBean(lstValueNode);
					lst.add(lstValue);
				}

				beanWrapper.setPropertyValue(name, lst);
				continue nodes;
			}

			if (propClass != null && Map.class.isAssignableFrom(propClass)) {
				String mapClass = childNode.getProperty(classProperty)
						.getString();
				// FIXME: use OSGi compatible class loading
				Map<Object, Object> map;
				try {
					map = (Map<Object, Object>) Class.forName(mapClass)
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
	}

	protected void beanToNode(Session session, BeanWrapper beanWrapper,
			Node node) throws RepositoryException {
		if (log.isDebugEnabled())
			log.debug("Map bean to node " + node.getPath());

		properties: for (PropertyDescriptor pd : beanWrapper
				.getPropertyDescriptors()) {
			String name = pd.getName();

			if (!beanWrapper.isReadableProperty(name))
				continue properties;// skip

			Object value = beanWrapper.getPropertyValue(name);
			if (value == null)
				continue properties;// skip

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

			if (value instanceof Class<?>) {
				node.setProperty(name, ((Class<?>) value).getName());
				continue properties;
			}

			Value val = asValue(session, value);
			if (val != null) {
				node.setProperty(name, val);
				continue properties;
			}

			if (value instanceof List<?>) {
				List<?> lst = (List<?>) value;
				addList(session, node, name, lst);
				continue properties;
			}

			if (value instanceof Map<?, ?>) {
				Map<?, ?> map = (Map<?, ?>) value;
				addMap(session, node, name, map);
				continue properties;
			}

			BeanWrapper child = createBeanWrapper(value);
			// TODO: delegate to another mapper

			Node childNode = findChildReference(session, child);
			if (childNode != null) {
				node.setProperty(name, childNode);
				continue properties;
			}

			// default case (recursive)
			childNode = node.addNode(name);
			beanToNode(session, child, childNode);
			// if (childNode.isNodeType(ArgeoJcrConstants.MIX_REFERENCEABLE)) {
			// log.debug("Add reference to  " + childNode.getPath());
			// node.setProperty(name, childNode);
			// }
		}
	}

	protected void addList(Session session, Node node, String name, List<?> lst)
			throws RepositoryException {
		Node listNode = node.addNode(name);
		listNode.setProperty(classProperty, lst.getClass().getName());
		Value[] values = new Value[lst.size()];
		boolean atLeastOneSet = false;
		for (int i = 0; i < lst.size(); i++) {
			Object lstValue = lst.get(i);
			values[i] = asValue(session, lstValue);
			if (values[i] != null) {
				atLeastOneSet = true;
			} else {
				Node childNode = findChildReference(session,
						createBeanWrapper(lstValue));
				if (childNode != null) {
					values[i] = session.getValueFactory()
							.createValue(childNode);
					atLeastOneSet = true;
				}
			}
		}

		// will be either properties or nodes, not both
		if (!atLeastOneSet && lst.size() != 0) {
			for (Object lstValue : lst) {
				Node childNode = listNode.addNode(NODE_VALUE);
				beanToNode(session, createBeanWrapper(lstValue), childNode);
			}
		} else {
			listNode.setProperty(name, values);
		}
	}

	protected void addMap(Session session, Node node, String name, Map<?, ?> map)
			throws RepositoryException {
		// TODO: add map specific type
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

			Value mapVal = asValue(session, mapValue);
			if (mapVal != null)
				mapNode.setProperty(keyStr, mapVal);
			else {
				Node entryNode = mapNode.addNode(keyStr);
				beanToNode(session, createBeanWrapper(mapValue), entryNode);
			}

		}

	}

	protected BeanWrapper createBeanWrapper(Object obj) {
		return new BeanWrapperImpl(obj);
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

	protected String propertyName(String name) {
		return name;
	}

	public void setVersioning(Boolean versioning) {
		this.versioning = versioning;
	}

}
