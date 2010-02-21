package org.argeo.jcr;

import java.beans.PropertyDescriptor;
import java.beans.PropertyEditor;
import java.io.InputStream;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.ArgeoException;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

public class BeanNodeMapper {
	private final static Log log = LogFactory.getLog(BeanNodeMapper.class);

	private Boolean versioning = false;
	private String uuidProperty = "uuid";
	private String classProperty = "class";
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
			BeanWrapper beanWrapper = new BeanWrapperImpl(obj);
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

	protected void beanToNode(Session session, BeanWrapper beanWrapper,
			Node node) throws RepositoryException {
		if (log.isDebugEnabled())
			log.debug("Map bean to node " + node.getPath());

		properties: for (PropertyDescriptor pd : beanWrapper
				.getPropertyDescriptors()) {
			String name = pd.getName();

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

			Value val = asValue(session, value);
			if (val != null) {
				node.setProperty(name, val);
				continue properties;
			}

			if (value instanceof List<?>) {
				List<?> lst = (List<?>) value;
				Value[] values = new Value[lst.size()];
				boolean atLeastOneSet = false;
				for (int i = 0; i < lst.size(); i++) {
					Object lstValue = lst.get(i);
					values[i] = asValue(session, lstValue);
					if (values[i] != null) {
						atLeastOneSet = true;
					} else {
						Node childNode = findChildReference(session,
								new BeanWrapperImpl(lstValue));
						if (childNode != null) {
							values[i] = session.getValueFactory().createValue(
									childNode);
							atLeastOneSet = true;
						}
					}
				}

				if (!atLeastOneSet && lst.size() != 0)
					throw new ArgeoException(
							"This type of list is not supported "
									+ lst.getClass());

				node.setProperty(name, values);
				continue properties;
			}

			if (value instanceof Map<?, ?>) {
				Map<?, ?> map = (Map<?, ?>) value;
				// TODO: add map specific type
				Node mapNode = node.addNode(name);
				for (Object key : map.keySet()) {
					Object mapValue = map.get(key);
					PropertyEditor pe = beanWrapper.findCustomEditor(key
							.getClass(), null);
					String keyStr = pe.getAsText();
					// TODO: check string format
					Node entryNode = mapNode.addNode(keyStr);
					beanToNode(session, new BeanWrapperImpl(mapValue),
							entryNode);
				}

				continue properties;
			}

			BeanWrapper child = new BeanWrapperImpl(value);
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
		else if (value instanceof CharSequence)
			return valueFactory.createValue(value.toString());
		else if (value instanceof InputStream)
			return valueFactory.createValue((InputStream) value);
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
