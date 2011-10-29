package org.argeo.jackrabbit.remote;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.server.SessionProvider;
import org.argeo.ArgeoException;
import org.argeo.jcr.ArgeoNames;
import org.argeo.jcr.JcrUtils;

/**
 * To be injected, typically of scope="session". Implements an open session in
 * view patter: a new JCR session is created for each request
 */
public class SimpleSessionProvider implements SessionProvider, Serializable {
	private static final long serialVersionUID = 2270957712453841368L;

	private final static Log log = LogFactory
			.getLog(SimpleSessionProvider.class);

	private transient Map<String, Session> sessions;

	private Boolean openSessionInView = true;

	public Session getSession(HttpServletRequest request, Repository rep,
			String workspace) throws LoginException, ServletException,
			RepositoryException {

		if (openSessionInView) {
			JackrabbitSession session = (JackrabbitSession) rep
					.login(workspace);
			writeRemoteRoles(session);
			return session;
		} else {
			// since sessions is transient it can't be restored from the session
			if (sessions == null)
				sessions = Collections
						.synchronizedMap(new HashMap<String, Session>());

			if (!sessions.containsKey(workspace)) {
				try {
					JackrabbitSession session = (JackrabbitSession) rep.login(
							null, workspace);
					writeRemoteRoles(session);
					if (log.isTraceEnabled())
						log.trace("User " + session.getUserID()
								+ " logged into " + request.getServletPath());
					sessions.put(workspace, session);
					return session;
				} catch (Exception e) {
					throw new ArgeoException("Cannot open session", e);
				}
			} else {
				Session session = sessions.get(workspace);
				if (!session.isLive()) {
					sessions.remove(workspace);
					session = rep.login(null, workspace);
					sessions.put(workspace, session);
				}
				return session;
			}
		}
	}

	protected void writeRemoteRoles(JackrabbitSession session)
			throws RepositoryException {
		// retrieve roles
		String userId = session.getUserID();
		UserManager userManager = session.getUserManager();
		User user = (User) userManager.getAuthorizable(userId);
		if (user == null) {
			// anonymous
			return;
		}
		List<String> userGroupIds = new ArrayList<String>();
		if (user != null)
			for (Iterator<Group> it = user.memberOf(); it.hasNext();)
				userGroupIds.add(it.next().getID());

		// write roles if needed
		Node userProfile = JcrUtils.getUserProfile(session);
		boolean writeRoles = false;
		if (userProfile.hasProperty(ArgeoNames.ARGEO_REMOTE_ROLES)) {
			Value[] roles = userProfile.getProperty(
					ArgeoNames.ARGEO_REMOTE_ROLES).getValues();
			if (roles.length != userGroupIds.size())
				writeRoles = true;
			else
				for (int i = 0; i < roles.length; i++)
					if (!roles[i].getString().equals(userGroupIds.get(i)))
						writeRoles = true;
		} else
			writeRoles = true;

		if (writeRoles) {
			session.getWorkspace().getVersionManager()
					.checkout(userProfile.getPath());
			String[] roleIds = userGroupIds.toArray(new String[userGroupIds
					.size()]);
			userProfile.setProperty(ArgeoNames.ARGEO_REMOTE_ROLES, roleIds);
			JcrUtils.updateLastModified(userProfile);
			session.save();
			session.getWorkspace().getVersionManager()
					.checkin(userProfile.getPath());
		}

	}

	public void releaseSession(Session session) {
		if (log.isTraceEnabled())
			log.trace("Releasing JCR session " + session);
		if (openSessionInView) {
			if (session.isLive()) {
				session.logout();
				if (log.isTraceEnabled())
					log.trace("Logged out remote JCR session " + session);
			}
		}
	}

	public void init() {
	}

	public void dispose() {
		if (sessions != null)
			for (String workspace : sessions.keySet()) {
				Session session = sessions.get(workspace);
				if (session.isLive()) {
					session.logout();
					if (log.isDebugEnabled())
						log.debug("Logged out remote JCR session " + session);
				}
			}
	}
}
