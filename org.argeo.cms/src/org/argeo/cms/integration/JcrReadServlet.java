package org.argeo.cms.integration;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.api.JackrabbitNode;
import org.apache.jackrabbit.api.JackrabbitValue;
import org.argeo.jcr.JcrUtils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Access a JCR repository via web services. */
public class JcrReadServlet extends HttpServlet {
	private static final long serialVersionUID = 6536175260540484539L;
	private final static Log log = LogFactory.getLog(JcrReadServlet.class);

	private final static String PARAM_VERBOSE = "verbose";
	private final static String PARAM_DEPTH = "depth";

	public final static String JCR_NODES = "jcr:nodes";
	// cf. javax.jcr.Property
	public final static String JCR_PATH = "path";
	public final static String JCR_NAME = "name";
//	public final static String JCR_ID = "id";

	final static String JCR_ = "jcr_";
	final static String JCR_PREFIX = "jcr:";
	final static String REP_PREFIX = "rep:";

	private Repository repository;
	private Integer maxDepth = 8;

	private ObjectMapper objectMapper = new ObjectMapper();

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		if (log.isTraceEnabled())
			log.trace("Data service: " + req.getPathInfo());

		String dataWorkspace = getWorkspace(req);
		String jcrPath = getJcrPath(req);

		boolean verbose = req.getParameter(PARAM_VERBOSE) != null && !req.getParameter(PARAM_VERBOSE).equals("false");
		int depth = 1;
		if (req.getParameter(PARAM_DEPTH) != null) {
			depth = Integer.parseInt(req.getParameter(PARAM_DEPTH));
			if (depth > maxDepth)
				throw new RuntimeException("Depth " + depth + " is higher than maximum " + maxDepth);
		}

		Session session = null;
		try {
			// authentication
			session = openJcrSession(req, resp, getRepository(), dataWorkspace);
			if (!session.itemExists(jcrPath))
				throw new RuntimeException("JCR node " + jcrPath + " does not exist");
			Node node = session.getNode(jcrPath);
			if (node.isNodeType(NodeType.NT_FILE)) {
				resp.setContentType("application/octet-stream");
				resp.addHeader("Content-Disposition", "attachment; filename='" + node.getName() + "'");
				IOUtils.copy(JcrUtils.getFileAsStream(node), resp.getOutputStream());
				resp.flushBuffer();
			} else {
				resp.setContentType("application/json");
				JsonGenerator jsonGenerator = getObjectMapper().getFactory().createGenerator(resp.getWriter());
				jsonGenerator.writeStartObject();
				writeNodeChildren(node, jsonGenerator, depth, verbose);
				writeNodeProperties(node, jsonGenerator, verbose);
				jsonGenerator.writeEndObject();
				jsonGenerator.flush();
			}
		} catch (Exception e) {
			new CmsExceptionsChain(e).writeAsJson(getObjectMapper(), resp);
		} finally {
			JcrUtils.logoutQuietly(session);
		}
	}

	protected Session openJcrSession(HttpServletRequest req, HttpServletResponse resp, Repository repository,
			String workspace) throws RepositoryException {
		return workspace != null ? repository.login(workspace) : repository.login();
	}

	/**
	 * To be overridden.
	 * 
	 * @return the workspace to use, or <code>null</code> if default should be used.
	 */
	protected String getWorkspace(HttpServletRequest req) {
		return null;
	}

	protected String getJcrPath(HttpServletRequest req) {
		return req.getPathInfo();
	}

	protected void writeNodeProperties(Node node, JsonGenerator jsonGenerator, boolean verbose)
			throws RepositoryException, IOException {
		String jcrPath = node.getPath();
//		jsonGenerator.writeStringField(JCR_NAME, node.getName());
//		jsonGenerator.writeStringField(JCR_PATH, jcrPath);
//		// meta data
//		if (verbose) {
//			jsonGenerator.writeStringField(JCR_ID, node.getIdentifier());
//		}

		Map<String, Map<String, Property>> namespaces = new TreeMap<>();

		PropertyIterator pit = node.getProperties();
		properties: while (pit.hasNext()) {
			Property property = pit.nextProperty();

			final String propertyName = property.getName();
			int columnIndex = propertyName.indexOf(':');
			if (columnIndex > 0) {
				String prefix = propertyName.substring(0, columnIndex) + "_";
				String unqualifiedName = propertyName.substring(columnIndex + 1);
				if (!namespaces.containsKey(prefix))
					namespaces.put(prefix, new LinkedHashMap<String, Property>());
				Map<String, Property> map = namespaces.get(prefix);
				assert !map.containsKey(unqualifiedName);
				map.put(unqualifiedName, property);
				continue properties;
			}

//			String fieldName = property.getName();
//			switch (fieldName) {
//			case "jcr:title":
//			case "jcr:description":
//			case "jcr:created":
//			case "jcr:createdBy":
//			case "jcr:lastModified":
//			case "jcr:lastModifiedBy":
//				fieldName = fieldName.substring(JCR_PREFIX.length());
//			}
//
//			if (!verbose) {
////				if (property.getName().equals("jcr:primaryType") || property.getName().equals("jcr:mixinTypes")
////						|| property.getName().equals("jcr:created") || property.getName().equals("jcr:createdBy")
////						|| property.getName().equals("jcr:lastModified")
////						|| property.getName().equals("jcr:lastModifiedBy")) {
////					continue properties;// skip
////				}
//				if (fieldName.startsWith(JCR_PREFIX))
//					continue properties;
//			}

			if (property.getType() == PropertyType.BINARY) {
				if (!(node instanceof JackrabbitNode)) {
					continue properties;// skip
				}
			}

			writeProperty(propertyName, property, jsonGenerator);
		}

		for (String prefix : namespaces.keySet()) {
			Map<String, Property> map = namespaces.get(prefix);
			jsonGenerator.writeFieldName(prefix);
			jsonGenerator.writeStartObject();
			if (JCR_.equals(prefix)) {
				jsonGenerator.writeStringField(JCR_NAME, node.getName());
				jsonGenerator.writeStringField(JCR_PATH, jcrPath);
//				jsonGenerator.writeStringField(JCR_ID, node.getIdentifier());
			}
			properties: for (String unqualifiedName : map.keySet()) {
				Property property = map.get(unqualifiedName);
				if (property.getType() == PropertyType.BINARY) {
					if (!(node instanceof JackrabbitNode)) {
						continue properties;// skip
					}
				}
				writeProperty(unqualifiedName, property, jsonGenerator);
			}
			jsonGenerator.writeEndObject();
		}
	}

	protected void writeProperty(String fieldName, Property property, JsonGenerator jsonGenerator)
			throws RepositoryException, IOException {
		if (!property.isMultiple()) {
			jsonGenerator.writeFieldName(fieldName);
			writePropertyValue(property.getType(), property.getValue(), jsonGenerator);
		} else {
			jsonGenerator.writeFieldName(fieldName);
			jsonGenerator.writeStartArray();
			Value[] values = property.getValues();
			for (Value value : values) {
				writePropertyValue(property.getType(), value, jsonGenerator);
			}
			jsonGenerator.writeEndArray();
		}
	}

	protected void writePropertyValue(int type, Value value, JsonGenerator jsonGenerator)
			throws RepositoryException, IOException {
		if (type == PropertyType.DOUBLE)
			jsonGenerator.writeNumber(value.getDouble());
		else if (type == PropertyType.LONG)
			jsonGenerator.writeNumber(value.getLong());
		else if (type == PropertyType.BINARY) {
			if (value instanceof JackrabbitValue) {
				String contentIdentity = ((JackrabbitValue) value).getContentIdentity();
				jsonGenerator.writeString("SHA256:" + contentIdentity);
			} else {
				// TODO write Base64 ?
				jsonGenerator.writeNull();
			}
		} else
			jsonGenerator.writeString(value.getString());
	}

	protected void writeNodeChildren(Node node, JsonGenerator jsonGenerator, int depth, boolean verbose)
			throws RepositoryException, IOException {
		if (!node.hasNodes())
			return;
		if (depth <= 0)
			return;
		NodeIterator nit;

		nit = node.getNodes();
		children: while (nit.hasNext()) {
			Node child = nit.nextNode();
			if (!verbose && child.getName().startsWith(REP_PREFIX)) {
				continue children;// skip Jackrabbit auth metadata
			}

			jsonGenerator.writeFieldName(child.getName());
			jsonGenerator.writeStartObject();
			writeNodeChildren(child, jsonGenerator, depth - 1, verbose);
			writeNodeProperties(child, jsonGenerator, verbose);
			jsonGenerator.writeEndObject();
		}

		// old
//		nit = node.getNodes();
//		jsonGenerator.writeFieldName(JcrServlet.JCR_NODES);
//		jsonGenerator.writeStartArray();
//		children: while (nit.hasNext()) {
//			Node child = nit.nextNode();
//
//			if (child.getName().startsWith(REP_PREFIX)) {
//				continue children;// skip Jackrabbit auth metadata
//			}
//
//			jsonGenerator.writeStartObject();
//			writeNodeProperties(child, jsonGenerator, verbose);
//			writeNodeChildren(child, jsonGenerator, depth - 1, verbose);
//			jsonGenerator.writeEndObject();
//		}
//		jsonGenerator.writeEndArray();
	}

	public void setRepository(Repository repository) {
		this.repository = repository;
	}

	public void setMaxDepth(Integer maxDepth) {
		this.maxDepth = maxDepth;
	}

	protected Repository getRepository() {
		return repository;
	}

	protected ObjectMapper getObjectMapper() {
		return objectMapper;
	}

}
