package org.argeo.cms.jcr.acr;

import java.util.Arrays;
import java.util.Iterator;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.xml.namespace.NamespaceContext;

import org.argeo.jcr.JcrException;

/** A {@link NamespaceContext} based on a JCR {@link Session}. */
public class JcrSessionNamespaceContext implements NamespaceContext {
	private final Session session;

	public JcrSessionNamespaceContext(Session session) {
		this.session = session;
	}

	@Override
	public String getNamespaceURI(String prefix) {
		try {
			return session.getNamespaceURI(prefix);
		} catch (RepositoryException e) {
			throw new JcrException(e);
		}
	}

	@Override
	public String getPrefix(String namespaceURI) {
		try {
			return session.getNamespacePrefix(namespaceURI);
		} catch (RepositoryException e) {
			throw new JcrException(e);
		}
	}

	@Override
	public Iterator<String> getPrefixes(String namespaceURI) {
		try {
			return Arrays.asList(session.getNamespacePrefix(namespaceURI)).iterator();
		} catch (RepositoryException e) {
			throw new JcrException(e);
		}
	}
}
