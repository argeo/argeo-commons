package org.argeo.cms.internal.http;

import java.io.Serializable;
import java.security.PrivilegedExceptionAction;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.server.SessionProvider;
import org.argeo.cms.CmsException;
import org.argeo.jcr.JcrUtils;
import org.argeo.node.NodeConstants;

/**
 * Implements an open session in view patter: a new JCR session is created for
 * each request
 */
class OpenInViewSessionProvider implements SessionProvider, Serializable {
	private final static Log log = LogFactory.getLog(OpenInViewSessionProvider.class);

	private static final long serialVersionUID = 2270957712453841368L;
	private final String alias;

	public OpenInViewSessionProvider(String alias) {
		this.alias = alias;
	}

	public Session getSession(HttpServletRequest request, Repository rep, String workspace)
			throws javax.jcr.LoginException, ServletException, RepositoryException {
		return login(request, rep, workspace);
	}

	protected Session login(HttpServletRequest request, Repository repository, String workspace)
			throws RepositoryException {
		if (log.isTraceEnabled())
			log.trace("Repo " + alias + ", login to workspace " + (workspace == null ? "<default>" : workspace)
					+ " in web session " + request.getSession().getId());
		LoginContext lc = (LoginContext) request.getAttribute(NodeConstants.LOGIN_CONTEXT_USER);
		if (lc == null)
			throw new CmsException("No login context available");
		try {
			// LoginContext lc = new
			// LoginContext(NodeConstants.LOGIN_CONTEXT_USER,
			// new HttpRequestCallbackHandler(request));
			// lc.login();
			return Subject.doAs(lc.getSubject(), new PrivilegedExceptionAction<Session>() {
				@Override
				public Session run() throws Exception {
					return repository.login(workspace);
				}
			});
		} catch (Exception e) {
			throw new CmsException("Cannot log in to JCR", e);
		}
		// return repository.login(workspace);
	}

	public void releaseSession(Session session) {
		JcrUtils.logoutQuietly(session);
		if (log.isTraceEnabled())
			log.trace("Logged out remote JCR session " + session);
	}
}
