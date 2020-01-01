package org.argeo.cms.integration;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

public class XslTemplate {
	private Transformer transformer;

	public XslTemplate(InputStream in, String systemId) {
		this(loadTransformer(in, systemId));
	}

	public XslTemplate(Transformer transformer) {
		this.transformer = transformer;
	}

	private static Transformer loadTransformer(InputStream in, String systemId) {
		try {
			TransformerFactory tFactory = TransformerFactory.newInstance();
			StreamSource stylesource = new StreamSource(in, systemId);
			return tFactory.newTransformer(stylesource);
		} catch (TransformerConfigurationException | TransformerFactoryConfigurationError e) {
			throw new IllegalArgumentException("Cannot initialise stylesheet with systemId " + systemId, e);
		}
	}

	public synchronized void apply(Node node, OutputStream out) {
		// TODO use a pool of Transformer instead of synchronized
		try (ByteArrayOutputStream xml = new ByteArrayOutputStream()) {
			node.getSession().exportDocumentView(node.getPath(), xml, true, false);
			try (ByteArrayInputStream xmlIn = new ByteArrayInputStream(xml.toByteArray())) {
				Source source = new StreamSource(xmlIn);
				Result results = new StreamResult(out);
				transformer.transform(source, results);
			}
		} catch (IOException | RepositoryException | TransformerException e) {
			throw new RuntimeException("Cannot process XSL template on " + node, e);
		}
	}
}
