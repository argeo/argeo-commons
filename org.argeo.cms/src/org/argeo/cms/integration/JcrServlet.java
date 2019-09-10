package org.argeo.cms.integration;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.api.JackrabbitNode;
import org.apache.jackrabbit.api.JackrabbitValue;
import org.argeo.jcr.JcrUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;

/** Canonical access to a JCR repository via web services. */
public class JcrServlet extends HttpServlet {
	private static final long serialVersionUID = 6536175260540484539L;

	private final static String PARAM_VERBOSE = "verbose";
	private final static String PARAM_DEPTH = "depth";
	private final static String PARAM_PRETTY = "pretty";

	private final static Log log = LogFactory.getLog(JcrServlet.class);

	private Repository repository;
	private Integer maxDepth = 8;

	private Gson gson = new GsonBuilder().setPrettyPrinting().create();

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String path = req.getPathInfo();
		if (log.isTraceEnabled())
			log.trace("Data service: " + path);
		path = URLDecoder.decode(path, StandardCharsets.UTF_8.name());
		String[] pathTokens = path.split("/");
		// TODO make it more robust

		String domain = pathTokens[1];
		String dataWorkspace = "vje_" + domain;
		String jcrPath = path.substring(domain.length() + 1);

		boolean verbose = req.getParameter(PARAM_VERBOSE) != null && !req.getParameter(PARAM_VERBOSE).equals("false");
		int depth = 1;
		if (req.getParameter(PARAM_DEPTH) != null) {
			depth = Integer.parseInt(req.getParameter(PARAM_DEPTH));
			if (depth > maxDepth)
				throw new RuntimeException("Depth " + depth + " is higher than maximum " + maxDepth);
		}
		String urlBase = null;
		if (req.getParameter("html") != null)
			urlBase = req.getServletPath() + '/' + domain;
		boolean pretty = req.getParameter(PARAM_PRETTY) != null;

		resp.setContentType("application/json");
		Session session = null;
		try {
			// authentication
			session = openJcrSession(req, resp, repository, dataWorkspace);
			if (!session.itemExists(jcrPath))
				throw new RuntimeException("JCR node " + jcrPath + " does not exist");
			Node node = session.getNode(jcrPath);

			JsonWriter jsonWriter;
			if (pretty)
				jsonWriter = new GsonBuilder().setPrettyPrinting().create().newJsonWriter(resp.getWriter());
			else
				jsonWriter = gson.newJsonWriter(resp.getWriter());
			jsonWriter.beginObject();
			writeNodeProperties(node, jsonWriter, verbose, urlBase);
			writeNodeChildren(node, jsonWriter, depth, verbose, urlBase);
			jsonWriter.endObject();
			jsonWriter.flush();
		} catch (RepositoryException e) {
			resp.setStatus(500);
			throw new RuntimeException("Cannot process JCR node " + jcrPath, e);
		} finally {
			JcrUtils.logoutQuietly(session);
		}
	}

	protected Session openJcrSession(HttpServletRequest req, HttpServletResponse resp, Repository repository,
			String workspace) throws RepositoryException {
		return repository.login();
	}

	protected void writeNodeProperties(Node node, JsonWriter jsonWriter, boolean verbose, String urlBase)
			throws RepositoryException, IOException {
		String jcrPath = node.getPath();
		jsonWriter.name("jcr:name");
		jsonWriter.value(node.getName());
		jsonWriter.name("jcr:path");
		jsonWriter.value(jcrPath);

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
				jsonWriter.name(property.getName());
				writePropertyValue(property.getType(), property.getValue(), jsonWriter);
			} else {
				jsonWriter.name(property.getName());
				jsonWriter.beginArray();
				Value[] values = property.getValues();
				for (Value value : values) {
					writePropertyValue(property.getType(), value, jsonWriter);
				}
				jsonWriter.endArray();
			}
		}

		// meta data
		if (verbose) {
			jsonWriter.name("jcr:identifier");
			jsonWriter.value(node.getIdentifier());
		}
		if (urlBase != null) {// TODO make it browsable
			jsonWriter.name("url");
			String url = urlBase + jcrPath;
			jsonWriter.value("<a href='" + url + "?html=true'>" + url + "</a>");
		}
	}

	protected void writePropertyValue(int type, Value value, JsonWriter jsonWriter)
			throws RepositoryException, IOException {
		if (type == PropertyType.DOUBLE)
			jsonWriter.value(value.getDouble());
		else if (type == PropertyType.LONG)
			jsonWriter.value(value.getLong());
		else if (type == PropertyType.BINARY) {
			if (value instanceof JackrabbitValue) {
				String contentIdentity = ((JackrabbitValue) value).getContentIdentity();
				jsonWriter.value("SHA256:" + contentIdentity);
			}
		} else
			jsonWriter.value(value.getString());
	}

	protected void writeNodeChildren(Node node, JsonWriter jsonWriter, int depth, boolean verbose, String urlBase)
			throws RepositoryException, IOException {
		if (!node.hasNodes())
			return;
		if (depth <= 0)
			return;
		NodeIterator nit = node.getNodes();
		jsonWriter.name("jcr:nodes");
		jsonWriter.beginArray();
		while (nit.hasNext()) {
			Node child = nit.nextNode();
			jsonWriter.beginObject();
			writeNodeProperties(child, jsonWriter, verbose, urlBase);
			writeNodeChildren(child, jsonWriter, depth - 1, verbose, urlBase);
			jsonWriter.endObject();
		}
		jsonWriter.endArray();
	}

	public void setRepository(Repository repository) {
		this.repository = repository;
	}

	public void setMaxDepth(Integer maxDepth) {
		this.maxDepth = maxDepth;
	}

}
