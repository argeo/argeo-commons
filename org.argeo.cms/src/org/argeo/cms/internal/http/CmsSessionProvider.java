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
import org.argeo.cms.auth.CmsSession;

/**
 * Implements an open session in view patter: a new JCR session is created for
 * each request
 */
class CmsSessionProvider implements SessionProvider, Serializable {
	private static final long serialVersionUID = -1358136599534938466L;

	private final static Log log = LogFactory.getLog(CmsSessionProvider.class);

	private final String alias;

	private LinkedHashMap<Session, CmsSession> cmsSessions = new LinkedHashMap<>();

	public CmsSessionProvider(String alias) {
		this.alias = alias;
	}

	public Session getSession(HttpServletRequest request, Repository rep, String workspace)
			throws javax.jcr.LoginException, ServletException, RepositoryException {

		CmsSession cmsSession = WebCmsSessionImpl.getCmsSession(request);
		Session session = cmsSession.getDataSession(alias, workspace, rep);
		cmsSessions.put(session, cmsSession);
		return session;
	}

	public void releaseSession(Session session) {
		if (cmsSessions.containsKey(session)) {
			CmsSession cmsSession = cmsSessions.get(session);
			cmsSession.releaseDataSession(alias, session);
		} else {
			log.warn("No CMS session for JCR session " + session);
		}
	}
}
