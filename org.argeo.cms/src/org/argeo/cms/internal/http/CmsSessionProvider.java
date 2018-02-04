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

		CmsSessionImpl cmsSession = WebCmsSessionImpl.getCmsSession(request);
		// if (cmsSession == null)
		// return anonymousSession(request, rep, workspace);
		if (log.isTraceEnabled()) {
			log.debug("Get JCR session from " + cmsSession);
		}
		Session session = cmsSession.getDataSession(alias, workspace, rep);
		cmsSessions.put(session, cmsSession);
		return session;
	}

	// private synchronized Session anonymousSession(HttpServletRequest request,
	// Repository repository, String workspace) {
	// // TODO rather log in here as anonymous?
	// LoginContext lc = (LoginContext)
	// request.getAttribute(NodeConstants.LOGIN_CONTEXT_ANONYMOUS);
	// if (lc == null)
	// throw new CmsException("No login context available");
	// // optimize
	// Session session;
	// try {
	// session = Subject.doAs(lc.getSubject(), new
	// PrivilegedExceptionAction<Session>() {
	// @Override
	// public Session run() throws Exception {
	// return repository.login(workspace);
	// }
	// });
	// } catch (Exception e) {
	// throw new CmsException("Cannot log in to JCR", e);
	// }
	// return session;
	// }

	public synchronized void releaseSession(Session session) {
		if (cmsSessions.containsKey(session)) {
			CmsSessionImpl cmsSession = cmsSessions.get(session);
			cmsSession.releaseDataSession(alias, session);
		} else {
			log.warn("JCR session " + session + " not found in CMS session list. Logging it out...");
			JcrUtils.logoutQuietly(session);
		}
	}
}
