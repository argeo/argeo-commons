package org.argeo.cms.internal.kernel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import javax.jcr.Repository;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.server.SessionProvider;
import org.apache.jackrabbit.server.remoting.davex.JcrRemotingServlet;
import org.apache.jackrabbit.webdav.simple.SimpleWebdavServlet;
import org.argeo.cms.CmsException;
import org.argeo.cms.internal.http.CmsSessionProvider;
import org.argeo.cms.internal.http.DataHttpContext;
import org.argeo.cms.internal.http.HtmlServlet;
import org.argeo.cms.internal.http.HttpUtils;
import org.argeo.cms.internal.http.LinkServlet;
import org.argeo.cms.internal.http.PrivateHttpContext;
import org.argeo.cms.internal.http.RobotServlet;
import org.argeo.node.NodeConstants;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Intercepts and enriches http access, mainly focusing on security and
 * transactionality.
 */
public class NodeHttp implements KernelConstants {
	private final static Log log = LogFactory.getLog(NodeHttp.class);

	public final static String DEFAULT_SERVICE = "HTTP";

	private final BundleContext bc = FrameworkUtil.getBundle(getClass()).getBundleContext();

	private ServiceTracker<Repository, Repository> repositories;
	private final ServiceTracker<HttpService, HttpService> httpServiceTracker;

	private String httpRealm = "Argeo";
	private String webDavConfig = HttpUtils.WEBDAV_CONFIG;
	private final boolean cleanState;

	public NodeHttp(boolean cleanState) {
		this.cleanState = cleanState;
		httpServiceTracker = new PrepareHttpStc();
		// httpServiceTracker.open();
		KernelUtils.asyncOpen(httpServiceTracker);
	}

	public void destroy() {
		if (repositories != null)
			repositories.close();
	}

	public void registerRepositoryServlets(HttpService httpService, String alias, Repository repository) {
		if (httpService == null)
			throw new CmsException("No HTTP service available");
		try {
			registerWebdavServlet(httpService, alias, repository);
			registerRemotingServlet(httpService, alias, repository);
			if (NodeConstants.HOME.equals(alias))
				registerFilesServlet(httpService, alias, repository);
			if (log.isTraceEnabled())
				log.trace("Registered servlets for repository '" + alias + "'");
		} catch (Exception e) {
			throw new CmsException("Could not register servlets for repository '" + alias + "'", e);
		}
	}

	public static void unregisterRepositoryServlets(HttpService httpService, String alias) {
		if (httpService == null)
			return;
		try {
			httpService.unregister(webdavPath(alias));
			httpService.unregister(remotingPath(alias));
			if (NodeConstants.HOME.equals(alias))
				httpService.unregister(filesPath(alias));
			if (log.isTraceEnabled())
				log.trace("Unregistered servlets for repository '" + alias + "'");
		} catch (Exception e) {
			log.error("Could not unregister servlets for repository '" + alias + "'", e);
		}
	}

	void registerWebdavServlet(HttpService httpService, String alias, Repository repository)
			throws NamespaceException, ServletException {
		// WebdavServlet webdavServlet = new WebdavServlet(repository, new
		// OpenInViewSessionProvider(alias));
		WebdavServlet webdavServlet = new WebdavServlet(repository, new CmsSessionProvider(alias));
		String path = webdavPath(alias);
		Properties ip = new Properties();
		ip.setProperty(WebdavServlet.INIT_PARAM_RESOURCE_CONFIG, webDavConfig);
		ip.setProperty(WebdavServlet.INIT_PARAM_RESOURCE_PATH_PREFIX, path);
		httpService.registerServlet(path, webdavServlet, ip, new DataHttpContext(httpRealm));
	}

	void registerFilesServlet(HttpService httpService, String alias, Repository repository)
			throws NamespaceException, ServletException {
		WebdavServlet filesServlet = new WebdavServlet(repository, new CmsSessionProvider(alias));
		String path = filesPath(alias);
		Properties ip = new Properties();
		ip.setProperty(WebdavServlet.INIT_PARAM_RESOURCE_CONFIG, webDavConfig);
		ip.setProperty(WebdavServlet.INIT_PARAM_RESOURCE_PATH_PREFIX, path);
		httpService.registerServlet(path, filesServlet, ip, new PrivateHttpContext(httpRealm, true));
	}

	void registerRemotingServlet(HttpService httpService, String alias, Repository repository)
			throws NamespaceException, ServletException {
		RemotingServlet remotingServlet = new RemotingServlet(repository, new CmsSessionProvider(alias));
		String path = remotingPath(alias);
		Properties ip = new Properties();
		ip.setProperty(JcrRemotingServlet.INIT_PARAM_RESOURCE_PATH_PREFIX, path);
		ip.setProperty(JcrRemotingServlet.INIT_PARAM_AUTHENTICATE_HEADER, "Negotiate");

		// Looks like a bug in Jackrabbit remoting init
		Path tmpDir;
		try {
			tmpDir = Files.createTempDirectory("remoting_" + alias);
		} catch (IOException e) {
			throw new CmsException("Cannot create temp directory for remoting servlet", e);
		}
		ip.setProperty(RemotingServlet.INIT_PARAM_HOME, tmpDir.toString());
		ip.setProperty(RemotingServlet.INIT_PARAM_TMP_DIRECTORY, "remoting_" + alias);
		ip.setProperty(RemotingServlet.INIT_PARAM_PROTECTED_HANDLERS_CONFIG, HttpUtils.DEFAULT_PROTECTED_HANDLERS);
		ip.setProperty(RemotingServlet.INIT_PARAM_CREATE_ABSOLUTE_URI, "false");
		httpService.registerServlet(path, remotingServlet, ip, new PrivateHttpContext(httpRealm));
	}

	static String webdavPath(String alias) {
		return NodeConstants.PATH_DATA + "/" + alias;
	}

	static String remotingPath(String alias) {
		return NodeConstants.PATH_JCR + "/" + alias;
	}

	static String filesPath(String alias) {
		return NodeConstants.PATH_FILES;
	}

	class RepositoriesStc extends ServiceTracker<Repository, Repository> {
		private final HttpService httpService;

		private final BundleContext bc;

		public RepositoriesStc(BundleContext bc, HttpService httpService) {
			super(bc, Repository.class, null);
			this.httpService = httpService;
			this.bc = bc;
		}

		@Override
		public Repository addingService(ServiceReference<Repository> reference) {
			Repository repository = bc.getService(reference);
			Object jcrRepoAlias = reference.getProperty(NodeConstants.CN);
			if (jcrRepoAlias != null) {
				String alias = jcrRepoAlias.toString();
				registerRepositoryServlets(httpService, alias, repository);
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
				unregisterRepositoryServlets(httpService, alias);
			}
		}
	}

	private class PrepareHttpStc extends ServiceTracker<HttpService, HttpService> {
		public PrepareHttpStc() {
			super(bc, HttpService.class, null);
		}

		@Override
		public HttpService addingService(ServiceReference<HttpService> reference) {
			long begin = System.currentTimeMillis();
			if (log.isTraceEnabled())
				log.trace("HTTP prepare starts...");
			HttpService httpService = addHttpService(reference);
			if (log.isTraceEnabled())
				log.trace("HTTP prepare duration: " + (System.currentTimeMillis() - begin) + "ms");
			return httpService;
		}

		@Override
		public void removedService(ServiceReference<HttpService> reference, HttpService service) {
			repositories.close();
			repositories = null;
		}

		private HttpService addHttpService(ServiceReference<HttpService> sr) {
			HttpService httpService = bc.getService(sr);
			// TODO find constants
			Object httpPort = sr.getProperty("http.port");
			Object httpsPort = sr.getProperty("https.port");

			try {
				httpService.registerServlet("/!", new LinkServlet(), null, null);
				httpService.registerServlet("/robots.txt", new RobotServlet(), null, null);
				httpService.registerServlet("/html", new HtmlServlet(), null, null);
			} catch (Exception e) {
				throw new CmsException("Cannot register filters", e);
			}
			// track repositories
			if (repositories != null)
				throw new CmsException("An http service is already configured");
			repositories = new RepositoriesStc(bc, httpService);
			// repositories.open();
			if (cleanState)
				KernelUtils.asyncOpen(repositories);
			log.info(httpPortsMsg(httpPort, httpsPort));
			// httpAvailable = true;
			// checkReadiness();

			bc.registerService(NodeHttp.class, NodeHttp.this, null);
			return httpService;
		}

		private String httpPortsMsg(Object httpPort, Object httpsPort) {
			return "HTTP " + httpPort + (httpsPort != null ? " - HTTPS " + httpsPort : "");
		}
	}

	private static class WebdavServlet extends SimpleWebdavServlet {
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
			WebdavServlet.super.service(request, response);
			// try {
			// Subject subject = subjectFromRequest(request);
			// // TODO make it stronger, with eTags.
			// // if (CurrentUser.isAnonymous(subject) &&
			// // request.getMethod().equals("GET")) {
			// // response.setHeader("Cache-Control", "no-transform, public,
			// // max-age=300, s-maxage=900");
			// // }
			//
			// Subject.doAs(subject, new PrivilegedExceptionAction<Void>() {
			// @Override
			// public Void run() throws Exception {
			// WebdavServlet.super.service(request, response);
			// return null;
			// }
			// });
			// } catch (PrivilegedActionException e) {
			// throw new CmsException("Cannot process webdav request",
			// e.getException());
			// }
		}

	}

	private static class RemotingServlet extends JcrRemotingServlet {
		private final Log log = LogFactory.getLog(RemotingServlet.class);
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
			if (log.isTraceEnabled())
				HttpUtils.logRequest(log, request);
			RemotingServlet.super.service(request, response);
		}
	}

}
