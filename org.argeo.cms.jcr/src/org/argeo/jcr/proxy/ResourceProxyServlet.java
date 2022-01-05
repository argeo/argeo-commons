package org.argeo.jcr.proxy;

import java.io.IOException;
import java.io.InputStream;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.argeo.jcr.JcrException;
import org.argeo.api.cms.CmsLog;
import org.argeo.jcr.Bin;
import org.argeo.jcr.JcrUtils;

/** Wraps a proxy via HTTP */
public class ResourceProxyServlet extends HttpServlet {
	private static final long serialVersionUID = -8886549549223155801L;

	private final static CmsLog log = CmsLog
			.getLog(ResourceProxyServlet.class);

	private ResourceProxy proxy;

	private String contentTypeCharset = "UTF-8";

	@Override
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		String path = request.getPathInfo();

		if (log.isTraceEnabled()) {
			log.trace("path=" + path);
			log.trace("UserPrincipal = " + request.getUserPrincipal().getName());
			log.trace("SessionID = " + request.getSession(false).getId());
			log.trace("ContextPath = " + request.getContextPath());
			log.trace("ServletPath = " + request.getServletPath());
			log.trace("PathInfo = " + request.getPathInfo());
			log.trace("Method = " + request.getMethod());
			log.trace("User-Agent = " + request.getHeader("User-Agent"));
		}

		Node node = null;
		try {
			node = proxy.proxy(path);
			if (node == null)
				response.sendError(404);
			else
				processResponse(node, response);
		} finally {
			if (node != null)
				try {
					JcrUtils.logoutQuietly(node.getSession());
				} catch (RepositoryException e) {
					// silent
				}
		}

	}

	/** Retrieve the content of the node. */
	protected void processResponse(Node node, HttpServletResponse response) {
//		Binary binary = null;
//		InputStream in = null;
		try(Bin binary = new Bin( node.getNode(Property.JCR_CONTENT)
				.getProperty(Property.JCR_DATA));InputStream in = binary.getStream()) {
			String fileName = node.getName();
			String ext = FilenameUtils.getExtension(fileName);

			// TODO use a more generic / standard approach
			// see http://svn.apache.org/viewvc/tomcat/trunk/conf/web.xml
			String contentType;
			if ("xml".equals(ext))
				contentType = "text/xml;charset=" + contentTypeCharset;
			else if ("jar".equals(ext))
				contentType = "application/java-archive";
			else if ("zip".equals(ext))
				contentType = "application/zip";
			else if ("gz".equals(ext))
				contentType = "application/x-gzip";
			else if ("bz2".equals(ext))
				contentType = "application/x-bzip2";
			else if ("tar".equals(ext))
				contentType = "application/x-tar";
			else if ("rpm".equals(ext))
				contentType = "application/x-redhat-package-manager";
			else
				contentType = "application/octet-stream";
			contentType = contentType + ";name=\"" + fileName + "\"";
			response.setHeader("Content-Disposition", "attachment; filename=\""
					+ fileName + "\"");
			response.setHeader("Expires", "0");
			response.setHeader("Cache-Control", "no-cache, must-revalidate");
			response.setHeader("Pragma", "no-cache");

			response.setContentType(contentType);

			IOUtils.copy(in, response.getOutputStream());
		} catch (RepositoryException e) {
			throw new JcrException("Cannot download " + node, e);
		} catch (IOException e) {
			throw new RuntimeException("Cannot download " + node, e);
		}
	}

	public void setProxy(ResourceProxy resourceProxy) {
		this.proxy = resourceProxy;
	}

}
