/*
 * Copyright (C) 2007-2012 Mathieu Baudier
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
package org.argeo.jcr.mvc;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.argeo.ArgeoException;
import org.argeo.server.ServerSerializer;
import org.springframework.xml.dom.DomContentHandler;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/** @deprecated */
public class JcrXmlServerSerializer implements ServerSerializer {
	private String contentTypeCharset = "UTF-8";

	private final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory
			.newInstance();
	private final TransformerFactory transformerFactory = TransformerFactory
			.newInstance();

	public void serialize(Object obj, HttpServletRequest request,
			HttpServletResponse response) {
		if (!(obj instanceof Node))
			throw new ArgeoException("Only " + Node.class + " is supported");

		String noRecurseStr = request.getParameter("noRecurse");
		boolean noRecurse = noRecurseStr != null && noRecurseStr.equals("true");

		String depthStr = request.getParameter("depth");
		String downloadStr = request.getParameter("download");

		Node node = (Node) obj;

		try {
			String contentType = "text/xml;charset=" + contentTypeCharset;
			// download case
			if (downloadStr != null && downloadStr.equals("true")) {
				String fileName = node.getName().replace(':', '_') + ".xml";
				contentType = contentType + ";name=\"" + fileName + "\"";
				response.setHeader("Content-Disposition",
						"attachment; filename=\"" + fileName + "\"");
				response.setHeader("Expires", "0");
				response
						.setHeader("Cache-Control", "no-cache, must-revalidate");
				response.setHeader("Pragma", "no-cache");
			}

			response.setContentType(contentType);
			if (depthStr == null) {
				node.getSession().exportDocumentView(node.getPath(),
						response.getOutputStream(), true, noRecurse);
			} else {
				int depth = Integer.parseInt(depthStr);
				Document document = documentBuilderFactory.newDocumentBuilder()
						.newDocument();
				serializeLevelToDom(node, document, 0, depth);
				Transformer transformer = transformerFactory.newTransformer();
				transformer.transform(new DOMSource(document),
						new StreamResult(response.getOutputStream()));
			}
		} catch (Exception e) {
			throw new ArgeoException("Cannot serialize " + node, e);
		}
	}

	protected void serializeLevelToDom(Node currentJcrNode,
			org.w3c.dom.Node currentDomNode, int currentDepth, int targetDepth)
			throws RepositoryException, SAXException {
		DomContentHandler domContentHandler = new DomContentHandler(
				currentDomNode);
		currentJcrNode.getSession().exportDocumentView(
				currentJcrNode.getPath(), domContentHandler, true, true);

		if (currentDepth == targetDepth)
			return;

		// TODO: filter
		NodeIterator nit = currentJcrNode.getNodes();
		while (nit.hasNext()) {
			Node nextJcrNode = nit.nextNode();
			org.w3c.dom.Node nextDomNode;
			if (currentDomNode instanceof Document)
				nextDomNode = ((Document) currentDomNode).getDocumentElement();
			else {
				String name = currentJcrNode.getName();
				NodeList nodeList = ((Element) currentDomNode)
						.getElementsByTagName(name);
				if (nodeList.getLength() < 1)
					throw new ArgeoException("No elment named " + name
							+ " under " + currentDomNode);
				// we know it is the last one added
				nextDomNode = nodeList.item(nodeList.getLength() - 1);
			}
			// recursive call
			serializeLevelToDom(nextJcrNode, nextDomNode, currentDepth + 1,
					targetDepth);
		}
	}

	public void setContentTypeCharset(String contentTypeCharset) {
		this.contentTypeCharset = contentTypeCharset;
	}

}
