package org.argeo.cms.internal.http;

import java.io.Serializable;
import java.util.LinkedHashMap;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.server.SessionProvider;
import org.argeo.cms.internal.auth.CmsSessionImpl;
import org.argeo.jcr.JcrUtils;

/**
 * Implements an open session in view patter: a new JCR session is created for
 * each request
 */
public class CmsSessionProvider implements SessionProvider, Serializable {
	private static final long serialVersionUID = -1358136599534938466L;

	private final static Log log = LogFactory.getLog(CmsSessionProvider.class);

	private final String alias;

	private LinkedHashMap<Session, CmsSessionImpl> cmsSessions = new LinkedHashMap<>();

	public CmsSessionProvider(String alias) {
		this.alias = alias;
	}

	public Session getSession(HttpServletRequest request, Repository rep, String workspace)
			throws javax.jcr.LoginException, ServletException, RepositoryException {

		// a client is scanning parent URLs.
//		if (workspace == null)
//			return null;

		CmsSessionImpl cmsSession = WebCmsSessionImpl.getCmsSession(request);
		if (log.isTraceEnabled()) {
			log.trace("Get JCR session from " + cmsSession);
		}
		if (cmsSession == null)
			throw new IllegalStateException("Cannot find a session for request " + request.getRequestURI());
		Session session = cmsSession.getDataSession(alias, workspace, rep);
		cmsSessions.put(session, cmsSession);
		return session;
	}

	public void releaseSession(Session session) {
//		JcrUtils.logoutQuietly(session);
		if (cmsSessions.containsKey(session)) {
			CmsSessionImpl cmsSession = cmsSessions.get(session);
			cmsSession.releaseDataSession(alias, session);
		} else {
			log.warn("JCR session " + session + " not found in CMS session list. Logging it out...");
			JcrUtils.logoutQuietly(session);
		}
	}
}
