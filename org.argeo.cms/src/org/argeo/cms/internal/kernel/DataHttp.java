package org.argeo.cms.internal.kernel;

import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.login.CredentialNotFoundException;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.server.SessionProvider;
import org.apache.jackrabbit.server.remoting.davex.JcrRemotingServlet;
import org.apache.jackrabbit.webdav.simple.SimpleWebdavServlet;
import org.argeo.cms.CmsException;
import org.argeo.cms.auth.HttpRequestCallback;
import org.argeo.cms.auth.HttpRequestCallbackHandler;
import org.argeo.jcr.JcrUtils;
import org.argeo.node.NodeConstants;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.osgi.service.useradmin.Authorization;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * Intercepts and enriches http access, mainly focusing on security and
 * transactionality.
 */
class DataHttp implements KernelConstants {
	private final static Log log = LogFactory.getLog(DataHttp.class);

	// private final static String ATTR_AUTH = "auth";
	private final static String HEADER_AUTHORIZATION = "Authorization";
	private final static String HEADER_WWW_AUTHENTICATE = "WWW-Authenticate";

	private final static String DEFAULT_PROTECTED_HANDLERS = "/org/argeo/cms/internal/kernel/protectedHandlers.xml";

	private final BundleContext bc;
	private final HttpService httpService;
	private final ServiceTracker<Repository, Repository> repositories;

	// FIXME Make it more unique
	private String httpAuthRealm = "Argeo";

	// WebDav / JCR remoting
	private OpenInViewSessionProvider sessionProvider;

	DataHttp(HttpService httpService) {
		this.bc = FrameworkUtil.getBundle(getClass()).getBundleContext();
		sessionProvider = new OpenInViewSessionProvider();
		this.httpService = httpService;
		repositories = new ServiceTracker<>(bc, Repository.class, new RepositoriesStc());
		repositories.open();
	}

	public void destroy() {
		repositories.close();
	}

	void registerRepositoryServlets(String alias, Repository repository) {
		try {
			registerWebdavServlet(alias, repository, true);
			registerWebdavServlet(alias, repository, false);
			registerRemotingServlet(alias, repository, true);
			registerRemotingServlet(alias, repository, false);
			if (log.isDebugEnabled())
				log.debug("Registered servlets for repository '" + alias + "'");
		} catch (Exception e) {
			throw new CmsException("Could not register servlets for repository '" + alias + "'", e);
		}
	}

	void unregisterRepositoryServlets(String alias) {
		try {
			httpService.unregister(webdavPath(alias, true));
			httpService.unregister(webdavPath(alias, false));
			httpService.unregister(remotingPath(alias, true));
			httpService.unregister(remotingPath(alias, false));
			if (log.isDebugEnabled())
				log.debug("Unregistered servlets for repository '" + alias + "'");
		} catch (Exception e) {
			log.error("Could not unregister servlets for repository '" + alias + "'", e);
		}
	}

	void registerWebdavServlet(String alias, Repository repository, boolean anonymous)
			throws NamespaceException, ServletException {
		WebdavServlet webdavServlet = new WebdavServlet(repository, sessionProvider);
		String path = webdavPath(alias, anonymous);
		Properties ip = new Properties();
		ip.setProperty(WebdavServlet.INIT_PARAM_RESOURCE_CONFIG, WEBDAV_CONFIG);
		ip.setProperty(WebdavServlet.INIT_PARAM_RESOURCE_PATH_PREFIX, path);
		httpService.registerServlet(path, webdavServlet, ip, new DataHttpContext(anonymous));
	}

	void registerRemotingServlet(String alias, Repository repository, boolean anonymous)
			throws NamespaceException, ServletException {
		RemotingServlet remotingServlet = new RemotingServlet(repository, sessionProvider);
		String path = remotingPath(alias, anonymous);
		Properties ip = new Properties();
		ip.setProperty(JcrRemotingServlet.INIT_PARAM_RESOURCE_PATH_PREFIX, path);

		// Looks like a bug in Jackrabbit remoting init
		ip.setProperty(RemotingServlet.INIT_PARAM_HOME, KernelUtils.getOsgiInstanceDir() + "/tmp/jackrabbit");
		ip.setProperty(RemotingServlet.INIT_PARAM_TMP_DIRECTORY, "remoting");
		ip.setProperty(RemotingServlet.INIT_PARAM_PROTECTED_HANDLERS_CONFIG, DEFAULT_PROTECTED_HANDLERS);
		httpService.registerServlet(path, remotingServlet, ip, new DataHttpContext(anonymous));
	}

	private String webdavPath(String alias, boolean anonymous) {
		String pathPrefix = anonymous ? WEBDAV_PUBLIC : WEBDAV_PRIVATE;
		return pathPrefix + "/" + alias;
	}

	private String remotingPath(String alias, boolean anonymous) {
		String pathPrefix = anonymous ? REMOTING_PUBLIC : REMOTING_PRIVATE;
		return pathPrefix + "/" + alias;
	}

	private Subject subjectFromRequest(HttpServletRequest request) {
		Authorization authorization = (Authorization) request.getAttribute(HttpContext.AUTHORIZATION);
		if (authorization == null)
			throw new CmsException("Not authenticated");
		try {
			LoginContext lc = new LoginContext(NodeConstants.LOGIN_CONTEXT_USER,
					new HttpRequestCallbackHandler(request));
			lc.login();
			return lc.getSubject();
		} catch (LoginException e) {
			throw new CmsException("Cannot login", e);
		}
	}

	private class RepositoriesStc implements ServiceTrackerCustomizer<Repository, Repository> {

		@Override
		public Repository addingService(ServiceReference<Repository> reference) {
			Repository repository = bc.getService(reference);
			Object jcrRepoAlias = reference.getProperty(NodeConstants.CN);
			if (jcrRepoAlias != null) {
				String alias = jcrRepoAlias.toString();
				registerRepositoryServlets(alias, repository);
			}
			return repository;
		}

		@Override
		public void modifiedService(ServiceReference<Repository> reference, Repository service) {
		}

		@Override
		public void removedService(ServiceReference<Repository> reference, Repository service) {
			Object jcrRepoAlias = reference.getProperty(NodeConstants.CN);
			if (jcrRepoAlias != null) {
				String alias = jcrRepoAlias.toString();
				unregisterRepositoryServlets(alias);
			}
		}
	}

	private class DataHttpContext implements HttpContext {
		private final boolean anonymous;

		DataHttpContext(boolean anonymous) {
			this.anonymous = anonymous;
		}

		@Override
		public boolean handleSecurity(final HttpServletRequest request, HttpServletResponse response)
				throws IOException {

			if (anonymous) {
				Subject subject = KernelUtils.anonymousLogin();
				Authorization authorization = subject.getPrivateCredentials(Authorization.class).iterator().next();
				request.setAttribute(REMOTE_USER, NodeConstants.ROLE_ANONYMOUS);
				request.setAttribute(AUTHORIZATION, authorization);
				return true;
			}

			if (log.isTraceEnabled())
				KernelUtils.logRequestHeaders(log, request);
			try {
				new LoginContext(NodeConstants.LOGIN_CONTEXT_USER, new HttpRequestCallbackHandler(request)).login();
				return true;
			} catch (CredentialNotFoundException e) {
				CallbackHandler token = basicAuth(request);
				if (token != null) {
					try {
						LoginContext lc = new LoginContext(NodeConstants.LOGIN_CONTEXT_USER, token);
						lc.login();
						// Note: this is impossible to reliably clear the
						// authorization header when access from a browser.
						return true;
					} catch (LoginException e1) {
						throw new CmsException("Could not login", e1);
					}
				} else {
					String path = request.getServletPath();
					if (path.startsWith(REMOTING_PRIVATE))
						requestBasicAuth(request, response);
					return false;
				}
			} catch (LoginException e) {
				throw new CmsException("Could not login", e);
			}
		}

		@Override
		public URL getResource(String name) {
			return KernelUtils.getBundleContext(DataHttp.class).getBundle().getResource(name);
		}

		@Override
		public String getMimeType(String name) {
			return null;
		}

		private void requestBasicAuth(HttpServletRequest request, HttpServletResponse response) {
			response.setStatus(401);
			response.setHeader(HEADER_WWW_AUTHENTICATE, "basic realm=\"" + httpAuthRealm + "\"");
			// request.getSession().setAttribute(ATTR_AUTH, Boolean.TRUE);
		}

		private CallbackHandler basicAuth(final HttpServletRequest httpRequest) {
			String authHeader = httpRequest.getHeader(HEADER_AUTHORIZATION);
			if (authHeader != null) {
				StringTokenizer st = new StringTokenizer(authHeader);
				if (st.hasMoreTokens()) {
					String basic = st.nextToken();
					if (basic.equalsIgnoreCase("Basic")) {
						try {
							// TODO manipulate char[]
							String credentials = new String(Base64.decodeBase64(st.nextToken()), "UTF-8");
							// log.debug("Credentials: " + credentials);
							int p = credentials.indexOf(":");
							if (p != -1) {
								final String login = credentials.substring(0, p).trim();
								final char[] password = credentials.substring(p + 1).trim().toCharArray();
								return new CallbackHandler() {
									public void handle(Callback[] callbacks) {
										for (Callback cb : callbacks) {
											if (cb instanceof NameCallback)
												((NameCallback) cb).setName(login);
											else if (cb instanceof PasswordCallback)
												((PasswordCallback) cb).setPassword(password);
											else if (cb instanceof HttpRequestCallback)
												((HttpRequestCallback) cb).setRequest(httpRequest);
										}
									}
								};
							} else {
								throw new CmsException("Invalid authentication token");
							}
						} catch (Exception e) {
							throw new CmsException("Couldn't retrieve authentication", e);
						}
					}
				}
			}
			return null;
		}

	}

	/**
	 * Implements an open session in view patter: a new JCR session is created
	 * for each request
	 */
	private class OpenInViewSessionProvider implements SessionProvider, Serializable {
		private static final long serialVersionUID = 2270957712453841368L;

		public Session getSession(HttpServletRequest request, Repository rep, String workspace)
				throws javax.jcr.LoginException, ServletException, RepositoryException {
			return login(request, rep, workspace);
		}

		protected Session login(HttpServletRequest request, Repository repository, String workspace)
				throws RepositoryException {
			if (log.isTraceEnabled())
				log.trace("Login to workspace " + (workspace == null ? "<default>" : workspace) + " in web session "
						+ request.getSession().getId());
			return repository.login(workspace);
		}

		public void releaseSession(Session session) {
			JcrUtils.logoutQuietly(session);
			if (log.isTraceEnabled())
				log.trace("Logged out remote JCR session " + session);
		}
	}

	private class WebdavServlet extends SimpleWebdavServlet {
		private static final long serialVersionUID = -4687354117811443881L;
		private final Repository repository;

		public WebdavServlet(Repository repository, SessionProvider sessionProvider) {
			this.repository = repository;
			setSessionProvider(sessionProvider);
		}

		public Repository getRepository() {
			return repository;
		}

		@Override
		protected void service(final HttpServletRequest request, final HttpServletResponse response)
				throws ServletException, IOException {
			try {
				Subject subject = subjectFromRequest(request);
				// TODO make it stronger, with eTags.
				// if (CurrentUser.isAnonymous(subject) &&
				// request.getMethod().equals("GET")) {
				// response.setHeader("Cache-Control", "no-transform, public,
				// max-age=300, s-maxage=900");
				// }

				Subject.doAs(subject, new PrivilegedExceptionAction<Void>() {
					@Override
					public Void run() throws Exception {
						WebdavServlet.super.service(request, response);
						return null;
					}
				});
			} catch (PrivilegedActionException e) {
				throw new CmsException("Cannot process webdav request", e.getException());
			}
		}
	}

	private class RemotingServlet extends JcrRemotingServlet {
		private static final long serialVersionUID = 4605238259548058883L;
		private final Repository repository;
		private final SessionProvider sessionProvider;

		public RemotingServlet(Repository repository, SessionProvider sessionProvider) {
			this.repository = repository;
			this.sessionProvider = sessionProvider;
		}

		@Override
		protected Repository getRepository() {
			return repository;
		}

		@Override
		protected SessionProvider getSessionProvider() {
			return sessionProvider;
		}

		@Override
		protected void service(final HttpServletRequest request, final HttpServletResponse response)
				throws ServletException, IOException {
			try {
				Subject subject = subjectFromRequest(request);
				Subject.doAs(subject, new PrivilegedExceptionAction<Void>() {
					@Override
					public Void run() throws Exception {
						RemotingServlet.super.service(request, response);
						return null;
					}
				});
			} catch (PrivilegedActionException e) {
				throw new CmsException("Cannot process JCR remoting request", e.getException());
			}
		}
	}
}
