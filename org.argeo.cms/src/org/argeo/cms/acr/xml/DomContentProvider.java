package org.argeo.cms.acr.xml;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.argeo.api.acr.Content;
import org.argeo.api.acr.ContentNotFoundException;
import org.argeo.api.acr.NamespaceUtils;
import org.argeo.api.acr.spi.ContentProvider;
import org.argeo.api.acr.spi.ProvidedSession;
import org.argeo.cms.acr.CmsContentRepository;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class DomContentProvider implements ContentProvider, NamespaceContext {
	private final Document document;

	// XPath
	// TODO centralise in some executor?
	private final ThreadLocal<XPath> xPath;

	private String mountPath;

	public DomContentProvider(String mountPath, Document document) {
		this.mountPath = mountPath;
		this.document = document;
		this.document.normalizeDocument();
		XPathFactory xPathFactory = XPathFactory.newInstance();
		xPath = new ThreadLocal<>() {

			@Override
			protected XPath initialValue() {
				// TODO set the document as namespace context?
				XPath res = xPathFactory.newXPath();
				res.setNamespaceContext(DomContentProvider.this);
				return res;
			}
		};
	}

//	@Override
//	public Content get() {
//		return new DomContent(this, document.getDocumentElement());
//	}

//	public Element createElement(String name) {
//		return document.createElementNS(null, name);
//
//	}

	@Override
	public Content get(ProvidedSession session, String mountPath, String relativePath) {
		if ("".equals(relativePath))
			return new DomContent(session, this, document.getDocumentElement());
		if (relativePath.startsWith("/"))
			throw new IllegalArgumentException("Relative path cannot start with /");

		String xPathExpression = '/' + relativePath;
		if ("/".equals(mountPath))
			xPathExpression = "/cr:root" + xPathExpression;
		try {
			NodeList nodes = (NodeList) xPath.get().evaluate(xPathExpression, document, XPathConstants.NODESET);
			if (nodes.getLength() > 1)
				throw new IllegalArgumentException(
						"Multiple content found for " + relativePath + " under " + mountPath);
			if (nodes.getLength() == 0)
				throw new ContentNotFoundException("Path " + relativePath + " under " + mountPath + " was not found");
			Element element = (Element) nodes.item(0);
			return new DomContent(session, this, element);
		} catch (XPathExpressionException e) {
			throw new IllegalArgumentException("XPath expression " + xPathExpression + " cannot be evaluated", e);
		}
	}

	public void persist(ProvidedSession session) {
		if (mountPath != null) {
			Content mountPoint = session.getMountPoint(mountPath);
			try (OutputStream out = mountPoint.open(OutputStream.class)) {
				CmsContentRepository contentRepository = (CmsContentRepository) session.getRepository();
				contentRepository.writeDom(document, out);
			} catch (IOException e) {
				throw new IllegalStateException("Cannot persist " + mountPath, e);
			}
		}
	}

	@Override
	public String getMountPath() {
		return mountPath;
	}

	/*
	 * NAMESPACE CONTEXT
	 */
	@Override
	public String getNamespaceURI(String prefix) {
		String namespaceURI = NamespaceUtils.getStandardNamespaceURI(prefix);
		if (namespaceURI != null)
			return namespaceURI;
		return document.lookupNamespaceURI(prefix);
	}

	@Override
	public String getPrefix(String namespaceURI) {
		String prefix = NamespaceUtils.getStandardPrefix(namespaceURI);
		if (prefix != null)
			return prefix;
		return document.lookupPrefix(namespaceURI);
	}

	@Override
	public Iterator<String> getPrefixes(String namespaceURI) {
		List<String> res = new ArrayList<>();
		res.add(getPrefix(namespaceURI));
		return Collections.unmodifiableList(res).iterator();
	}

}
