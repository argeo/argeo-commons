/*
 * Copyright (C) 2007-2012 Argeo GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.jcr.ArgeoJcrException;
import org.argeo.jcr.Bin;
import org.argeo.jcr.JcrUtils;

/** Wraps a proxy via HTTP */
public class ResourceProxyServlet extends HttpServlet {
	private static final long serialVersionUID = -8886549549223155801L;

	private final static Log log = LogFactory
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

//			try {
//				binary = node.getNode(Property.JCR_CONTENT)
//						.getProperty(Property.JCR_DATA).getBinary();
//			} catch (PathNotFoundException e) {
//				log.error("Node " + node + " as no data under content");
//				throw e;
//			}
//			in = binary.getStream();
			IOUtils.copy(in, response.getOutputStream());
		} catch (Exception e) {
			throw new ArgeoJcrException("Cannot download " + node, e);
//		} finally {
//			IOUtils.closeQuietly(in);
//			JcrUtils.closeQuietly(binary);
		}
	}

	public void setProxy(ResourceProxy resourceProxy) {
		this.proxy = resourceProxy;
	}

}
