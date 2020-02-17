package org.argeo.cms.integration;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.AccessControlContext;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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
import javax.security.auth.Subject;
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
import org.osgi.service.http.context.ServletContextHelper;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Access a JCR repository via web services. */
public class JcrReadServlet extends HttpServlet {
	private static final long serialVersionUID = 6536175260540484539L;
	private final static Log log = LogFactory.getLog(JcrReadServlet.class);

	protected final static String ACCEPT_HTTP_HEADER = "Accept";
	protected final static String CONTENT_DISPOSITION_HTTP_HEADER = "Content-Disposition";

	protected final static String OCTET_STREAM_CONTENT_TYPE = "application/octet-stream";
	protected final static String XML_CONTENT_TYPE = "application/xml";
	protected final static String JSON_CONTENT_TYPE = "application/json";

	private final static String PARAM_VERBOSE = "verbose";
	private final static String PARAM_DEPTH = "depth";

	protected final static String JCR_NODES = "jcr:nodes";
	// cf. javax.jcr.Property
	protected final static String JCR_PATH = "path";
	protected final static String JCR_NAME = "name";

	protected final static String _JCR = "_jcr";
	protected final static String JCR_PREFIX = "jcr:";
	protected final static String REP_PREFIX = "rep:";

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

			List<String> acceptHeader = readAcceptHeader(req);
			if (!acceptHeader.isEmpty() && node.isNodeType(NodeType.NT_FILE)) {
				resp.setContentType(OCTET_STREAM_CONTENT_TYPE);
				resp.addHeader(CONTENT_DISPOSITION_HTTP_HEADER, "attachment; filename='" + node.getName() + "'");
				IOUtils.copy(JcrUtils.getFileAsStream(node), resp.getOutputStream());
				resp.flushBuffer();
			} else {
				if (!acceptHeader.isEmpty() && acceptHeader.get(0).equals(XML_CONTENT_TYPE)) {
					// TODO Use req.startAsync(); ?
					resp.setContentType(XML_CONTENT_TYPE);
					session.exportSystemView(node.getPath(), resp.getOutputStream(), false, depth <= 1);
					return;
				}
				if (!acceptHeader.isEmpty() && !acceptHeader.contains(JSON_CONTENT_TYPE)) {
					if (log.isTraceEnabled())
						log.warn("Content type " + acceptHeader + " in Accept header is not supported. Supported: "
								+ JSON_CONTENT_TYPE + " (default), " + XML_CONTENT_TYPE);
				}
				resp.setContentType(JSON_CONTENT_TYPE);
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
		AccessControlContext acc = (AccessControlContext) req.getAttribute(ServletContextHelper.REMOTE_USER);
		Subject subject = Subject.getSubject(acc);
		try {
			return Subject.doAs(subject, new PrivilegedExceptionAction<Session>() {

				@Override
				public Session run() throws RepositoryException {
					return repository.login(workspace);
				}

			});
		} catch (PrivilegedActionException e) {
			if (e.getException() instanceof RepositoryException)
				throw (RepositoryException) e.getException();
			else
				throw new RuntimeException(e.getException());
		}
//		return workspace != null ? repository.login(workspace) : repository.login();
	}

	protected String getWorkspace(HttpServletRequest req) {
		String path = req.getPathInfo();
		try {
			path = URLDecoder.decode(path, StandardCharsets.UTF_8.name());
		} catch (UnsupportedEncodingException e) {
			throw new IllegalArgumentException(e);
		}
		String[] pathTokens = path.split("/");
		return pathTokens[1];
	}

	protected String getJcrPath(HttpServletRequest req) {
		String path = req.getPathInfo();
		try {
			path = URLDecoder.decode(path, StandardCharsets.UTF_8.name());
		} catch (UnsupportedEncodingException e) {
			throw new IllegalArgumentException(e);
		}
		String[] pathTokens = path.split("/");
		String domain = pathTokens[1];
		String jcrPath = path.substring(domain.length() + 1);
		return jcrPath;
	}

	protected List<String> readAcceptHeader(HttpServletRequest req) {
		List<String> lst = new ArrayList<>();
		String acceptHeader = req.getHeader(ACCEPT_HTTP_HEADER);
		if (acceptHeader == null)
			return lst;
//		Enumeration<String> acceptHeader = req.getHeaders(ACCEPT_HTTP_HEADER);
//		while (acceptHeader.hasMoreElements()) {
		String[] arr = acceptHeader.split("\\.");
		for (int i = 0; i < arr.length; i++) {
			String str = arr[i].trim();
			if (!"".equals(str))
				lst.add(str);
		}
//		}
		return lst;
	}

	protected void writeNodeProperties(Node node, JsonGenerator jsonGenerator, boolean verbose)
			throws RepositoryException, IOException {
		String jcrPath = node.getPath();
		Map<String, Map<String, Property>> namespaces = new TreeMap<>();

		PropertyIterator pit = node.getProperties();
		properties: while (pit.hasNext()) {
			Property property = pit.nextProperty();

			final String propertyName = property.getName();
			int columnIndex = propertyName.indexOf(':');
			if (columnIndex > 0) {
				// mark prefix with a '_' before the name of the object, according to JSON
				// conventions to indicate a special value
				String prefix = "_" + propertyName.substring(0, columnIndex);
				String unqualifiedName = propertyName.substring(columnIndex + 1);
				if (!namespaces.containsKey(prefix))
					namespaces.put(prefix, new LinkedHashMap<String, Property>());
				Map<String, Property> map = namespaces.get(prefix);
				assert !map.containsKey(unqualifiedName);
				map.put(unqualifiedName, property);
				continue properties;
			}

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
			if (_JCR.equals(prefix)) {
				jsonGenerator.writeStringField(JCR_NAME, node.getName());
				jsonGenerator.writeStringField(JCR_PATH, jcrPath);
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
