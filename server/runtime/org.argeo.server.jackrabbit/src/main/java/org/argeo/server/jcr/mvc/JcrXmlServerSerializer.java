package org.argeo.server.jcr.mvc;

import java.io.OutputStream;
import java.util.Set;

import javax.jcr.Node;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.argeo.ArgeoException;
import org.argeo.server.ServerSerializer;
import org.xml.sax.helpers.DefaultHandler;

public class JcrXmlServerSerializer implements ServerSerializer {
	private String contentTypeCharset = "UTF-8";

	public void serialize(Object obj, HttpServletRequest request,
			HttpServletResponse response) {
		if (!(obj instanceof Node))
			throw new ArgeoException("Only " + Node.class + " is supported");

		String noRecurseStr = request.getParameter("noRecurse");
		boolean noRecurse = noRecurseStr != null && noRecurseStr.equals("true");

		Node node = (Node) obj;
		response.setContentType("text/xml;charset=" + contentTypeCharset);
		try {
			node.getSession().exportDocumentView(node.getPath(),
					response.getOutputStream(), true, noRecurse);
		} catch (Exception e) {
			throw new ArgeoException("Cannot serialize " + node, e);
		}

	}

	public void setContentTypeCharset(String contentTypeCharset) {
		this.contentTypeCharset = contentTypeCharset;
	}

}
