package org.argeo.cms.jcr.acr;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.xml.namespace.NamespaceContext;

import org.argeo.api.acr.Content;
import org.argeo.api.acr.spi.ContentProvider;
import org.argeo.api.acr.spi.ProvidedContent;
import org.argeo.api.acr.spi.ProvidedSession;
import org.argeo.cms.acr.ContentUtils;
import org.argeo.cms.jcr.CmsJcrUtils;
import org.argeo.jcr.JcrException;
import org.argeo.jcr.JcrUtils;

/** A JCR workspace accessed as an {@link ContentProvider}. */
public class JcrContentProvider implements ContentProvider, NamespaceContext {
	public final static String ACR_MOUNT_PATH_PROPERTY = "acr.mount.path";

	private Repository jcrRepository;
	private Session adminSession;

	private String mountPath;

	private Map<ProvidedSession, JcrSessionAdapter> sessionAdapters = Collections.synchronizedMap(new HashMap<>());

	public void start(Map<String, String> properties) {
		mountPath = properties.get(ACR_MOUNT_PATH_PROPERTY);
		if ("/".equals(mountPath))
			throw new IllegalArgumentException("JCR content provider cannot be root /");
		Objects.requireNonNull(mountPath);
		adminSession = CmsJcrUtils.openDataAdminSession(jcrRepository, null);
	}

	public void stop() {
		if (adminSession.isLive())
			JcrUtils.logoutQuietly(adminSession);
	}

	public void setJcrRepository(Repository jcrRepository) {
		this.jcrRepository = jcrRepository;
	}

	@Override
	public ProvidedContent get(ProvidedSession contentSession, String relativePath) {
		String jcrWorkspace = ContentUtils.getParentPath(mountPath)[1];
		String jcrPath = "/" + relativePath;
		return new JcrContent(contentSession, this, jcrWorkspace, jcrPath);
	}

	@Override
	public boolean exists(ProvidedSession contentSession, String relativePath) {
		String jcrWorkspace = ContentUtils.getParentPath(mountPath)[1];
		String jcrPath = "/" + relativePath;
		return new JcrContent(contentSession, this, jcrWorkspace, jcrPath).exists();
	}

	public Session getJcrSession(ProvidedSession contentSession, String jcrWorkspace) {
		JcrSessionAdapter sessionAdapter = sessionAdapters.get(contentSession);
		if (sessionAdapter == null) {
			final JcrSessionAdapter newSessionAdapter = new JcrSessionAdapter(jcrRepository, contentSession,
					contentSession.getSubject());
			sessionAdapters.put(contentSession, newSessionAdapter);
			contentSession.onClose().thenAccept((s) -> newSessionAdapter.close());
			sessionAdapter = newSessionAdapter;
		}

		Session jcrSession = sessionAdapter.getSession(jcrWorkspace);
		return jcrSession;
	}

	public Session getJcrSession(Content content, String jcrWorkspace) {
		return getJcrSession(((ProvidedContent) content).getSession(), jcrWorkspace);
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
