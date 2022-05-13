package org.argeo.cms.jcr.acr;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.xml.namespace.NamespaceContext;

import org.argeo.api.acr.Content;
import org.argeo.api.acr.ContentUtils;
import org.argeo.api.acr.spi.ContentProvider;
import org.argeo.api.acr.spi.ProvidedSession;
import org.argeo.cms.jcr.CmsJcrUtils;
import org.argeo.jcr.JcrException;
import org.argeo.jcr.JcrUtils;

/** A JCR workspace accessed as an {@link ContentProvider}. */
public class JcrContentProvider implements ContentProvider, NamespaceContext {
	private Repository jcrRepository;
	private Session adminSession;

	private Map<ProvidedSession, JcrSessionAdapter> sessionAdapters = Collections.synchronizedMap(new HashMap<>());

	public void start() {
		adminSession = CmsJcrUtils.openDataAdminSession(jcrRepository, null);
	}

	public void stop() {
		JcrUtils.logoutQuietly(adminSession);
	}

	public void setJcrRepository(Repository jcrRepository) {
		this.jcrRepository = jcrRepository;
	}

	@Override
	public Content get(ProvidedSession contentSession, String mountPath, String relativePath) {
		String workspace = ContentUtils.getParentPath(mountPath)[1];
		JcrSessionAdapter sessionAdapter = sessionAdapters.get(contentSession);
		if (sessionAdapter == null) {
			final JcrSessionAdapter newSessionAdapter = new JcrSessionAdapter(jcrRepository,
					contentSession.getSubject());
			sessionAdapters.put(contentSession, newSessionAdapter);
			contentSession.onClose().thenAccept((s) -> newSessionAdapter.close());
			sessionAdapter = newSessionAdapter;
		}

		Session jcrSession = sessionAdapter.getSession(workspace);
		String jcrPath = "/" + relativePath;
		try {
			Node node = jcrSession.getNode(jcrPath);
			return new JcrContent(contentSession, this, node);
		} catch (RepositoryException e) {
			throw new JcrException("Cannot get JCR content '" + jcrPath + ", mounted from '" + mountPath
					+ "' with JCR session " + jcrSession, e);
		}
	}

	/*
	 * NAMESPACE CONTEXT
	 */
	@Override
	public String getNamespaceURI(String prefix) {
		try {
			return adminSession.getNamespaceURI(prefix);
		} catch (RepositoryException e) {
			throw new JcrException(e);
		}
	}

	@Override
	public String getPrefix(String namespaceURI) {
		try {
			return adminSession.getNamespacePrefix(namespaceURI);
		} catch (RepositoryException e) {
			throw new JcrException(e);
		}
	}

	@Override
	public Iterator<String> getPrefixes(String namespaceURI) {
		try {
			return Arrays.asList(adminSession.getNamespacePrefix(namespaceURI)).iterator();
		} catch (RepositoryException e) {
			throw new JcrException(e);
		}
	}

}
