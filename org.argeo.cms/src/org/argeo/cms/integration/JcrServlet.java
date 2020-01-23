package org.argeo.cms.integration;

import java.io.IOException;

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

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.api.JackrabbitNode;
import org.apache.jackrabbit.api.JackrabbitValue;
import org.argeo.jcr.JcrUtils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Access a JCR repository via web services. */
public class JcrServlet extends HttpServlet {
	private static final long serialVersionUID = 6536175260540484539L;
	private final static Log log = LogFactory.getLog(JcrServlet.class);

	private final static String PARAM_VERBOSE = "verbose";
	private final static String PARAM_DEPTH = "depth";

	public final static String JCR_NODES = "jcr:nodes";
	public final static String JCR_PATH = "jcr:path";
	public final static String JCR_NAME = "jcr:name";


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
			session = openJcrSession(req, resp, repository, dataWorkspace);
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
				JsonGenerator jsonGenerator = objectMapper.getFactory().createGenerator(resp.getWriter());
				jsonGenerator.writeStartObject();
				writeNodeProperties(node, jsonGenerator, verbose);
				writeNodeChildren(node, jsonGenerator, depth, verbose);
				jsonGenerator.writeEndObject();
				jsonGenerator.flush();
			}
		} catch (Exception e) {
			new CmsExceptionsChain(e).writeAsJson(objectMapper, resp);
		} finally {
			JcrUtils.logoutQuietly(session);
		}
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		if (log.isTraceEnabled())
			log.trace("Data service: " + req.getPathInfo());

		String dataWorkspace = getWorkspace(req);
		String jcrPath = getJcrPath(req);

		Session session = null;
		try {
			// authentication
			session = openJcrSession(req, resp, repository, dataWorkspace);
			if (!session.itemExists(jcrPath)) {
				String parentPath = FilenameUtils.getFullPathNoEndSeparator(jcrPath);
				String fileName = FilenameUtils.getName(jcrPath);
				Node folderNode = JcrUtils.mkfolders(session, parentPath);
				byte[] bytes = IOUtils.toByteArray(req.getInputStream());
				JcrUtils.copyBytesAsFile(folderNode, fileName, bytes);
			} else {
				Node node = session.getNode(jcrPath);
				if (!node.isNodeType(NodeType.NT_FILE))
					throw new IllegalArgumentException("Node " + jcrPath + " exists but is not a file");
				byte[] bytes = IOUtils.toByteArray(req.getInputStream());
				JcrUtils.copyBytesAsFile(node.getParent(), node.getName(), bytes);
			}
		} catch (Exception e) {
			new CmsExceptionsChain(e).writeAsJson(objectMapper, resp);
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
		jsonGenerator.writeStringField(JcrServlet.JCR_NAME, node.getName());
		jsonGenerator.writeStringField(JcrServlet.JCR_PATH, jcrPath);

		PropertyIterator pit = node.getProperties();
		properties: while (pit.hasNext()) {
			Property property = pit.nextProperty();

			if (!verbose) {
				if (property.getName().equals("jcr:primaryType") || property.getName().equals("jcr:mixinTypes")
						|| property.getName().equals("jcr:created") || property.getName().equals("jcr:createdBy")
						|| property.getName().equals("jcr:lastModified")
						|| property.getName().equals("jcr:lastModifiedBy")) {
					continue properties;// skip
				}
			}

			if (property.getType() == PropertyType.BINARY) {
				if (!(node instanceof JackrabbitNode)) {
					continue properties;// skip
				}
			}

			if (!property.isMultiple()) {
				jsonGenerator.writeFieldName(property.getName());
				writePropertyValue(property.getType(), property.getValue(), jsonGenerator);
			} else {
				jsonGenerator.writeFieldName(property.getName());
				jsonGenerator.writeStartArray();
				Value[] values = property.getValues();
				for (Value value : values) {
					writePropertyValue(property.getType(), value, jsonGenerator);
				}
				jsonGenerator.writeEndArray();
			}
		}

		// meta data
		if (verbose) {
			jsonGenerator.writeStringField("jcr:identifier", node.getIdentifier());
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
		NodeIterator nit = node.getNodes();
		jsonGenerator.writeFieldName(JcrServlet.JCR_NODES);
		jsonGenerator.writeStartArray();
		children: while (nit.hasNext()) {
			Node child = nit.nextNode();

			if (child.getName().startsWith("rep:")) {
				continue children;// skip Jackrabbit auth metadata
			}

			jsonGenerator.writeStartObject();
			writeNodeProperties(child, jsonGenerator, verbose);
			writeNodeChildren(child, jsonGenerator, depth - 1, verbose);
			jsonGenerator.writeEndObject();
		}
		jsonGenerator.writeEndArray();
	}

	public void setRepository(Repository repository) {
		this.repository = repository;
	}

	public void setMaxDepth(Integer maxDepth) {
		this.maxDepth = maxDepth;
	}

}
