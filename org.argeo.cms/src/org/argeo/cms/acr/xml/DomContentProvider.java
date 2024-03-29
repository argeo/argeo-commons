package org.argeo.cms.acr.xml;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.NamespaceContext;
import javax.xml.transform.TransformerFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.argeo.api.acr.Content;
import org.argeo.api.acr.ContentNotFoundException;
import org.argeo.api.acr.CrName;
import org.argeo.api.acr.NamespaceUtils;
import org.argeo.api.acr.spi.ContentProvider;
import org.argeo.api.acr.spi.ProvidedContent;
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

	private TransformerFactory transformerFactory;

	private String mountPath;

	public DomContentProvider(String mountPath, Document document) {
		this.mountPath = mountPath;
		this.document = document;
		this.document.normalizeDocument();

		transformerFactory = TransformerFactory.newInstance();

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

	@Override
	public ProvidedContent get(ProvidedSession session, String relativePath) {
		if ("".equals(relativePath))
			return new DomContent(session, this, document.getDocumentElement());

		NodeList nodes = findContent(relativePath);
		if (nodes.getLength() > 1)
			throw new IllegalArgumentException("Multiple content found for " + relativePath + " under " + mountPath);
		if (nodes.getLength() == 0)
			throw new ContentNotFoundException(session, mountPath + "/" + relativePath,
					"Path " + relativePath + " under " + mountPath + " was not found");
		Element element = (Element) nodes.item(0);
		return new DomContent(session, this, element);
	}

	protected NodeList findContent(String relativePath) {
		if (relativePath.startsWith("/"))
			throw new IllegalArgumentException("Relative path cannot start with /");
		String xPathExpression;
		if (Content.ROOT_PATH.equals(mountPath)) {// repository root
			xPathExpression = "/" + CrName.root.get() + '/' + relativePath;
		} else {
			String documentNodeName = document.getDocumentElement().getNodeName();
			xPathExpression = '/' + documentNodeName + '/' + relativePath;
		}
		try {
			NodeList nodes = (NodeList) xPath.get().evaluate(xPathExpression, document, XPathConstants.NODESET);
			return nodes;
		} catch (XPathExpressionException e) {
			throw new IllegalArgumentException("XPath expression " + xPathExpression + " cannot be evaluated", e);
		}

	}

	@Override
	public boolean exists(ProvidedSession session, String relativePath) {
		if ("".equals(relativePath))
			return true;
		NodeList nodes = findContent(relativePath);
		return nodes.getLength() != 0;
	}

	@Override
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

	public void registerPrefix(String prefix, String namespace) {
		DomUtils.addNamespace(document.getDocumentElement(), prefix, namespace);
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

	TransformerFactory getTransformerFactory() {
		return transformerFactory;
	}

}
