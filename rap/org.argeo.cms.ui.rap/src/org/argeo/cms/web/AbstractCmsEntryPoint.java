package org.argeo.cms.web;

import static org.argeo.util.naming.SharedSecret.X_SHARED_SECRET;

import java.io.IOException;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletRequest;

import org.argeo.api.cms.CmsLog;
import org.argeo.api.cms.ux.CmsView;
import org.argeo.api.cms.CmsAuth;
import org.argeo.cms.CmsException;
import org.argeo.cms.auth.CurrentUser;
import org.argeo.cms.auth.RemoteAuthCallback;
import org.argeo.cms.auth.RemoteAuthCallbackHandler;
import org.argeo.cms.servlet.ServletHttpRequest;
import org.argeo.cms.servlet.ServletHttpResponse;
import org.argeo.cms.swt.CmsStyles;
import org.argeo.cms.swt.CmsSwtUtils;
import org.argeo.eclipse.ui.specific.UiContext;
import org.argeo.jcr.JcrUtils;
import org.argeo.util.directory.ldap.AuthPassword;
import org.argeo.util.naming.SharedSecret;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.rap.rwt.application.AbstractEntryPoint;
import org.eclipse.rap.rwt.client.WebClient;
import org.eclipse.rap.rwt.client.service.BrowserNavigation;
import org.eclipse.rap.rwt.client.service.BrowserNavigationEvent;
import org.eclipse.rap.rwt.client.service.BrowserNavigationListener;
import org.eclipse.rap.rwt.client.service.JavaScriptExecutor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/** Manages history and navigation */
@Deprecated
public abstract class AbstractCmsEntryPoint extends AbstractEntryPoint implements CmsView {
	private static final long serialVersionUID = 906558779562569784L;

	private final CmsLog log = CmsLog.getLog(AbstractCmsEntryPoint.class);

	// private final Subject subject;
	private LoginContext loginContext;

	private final Repository repository;
	private final String workspace;
	private final String defaultPath;
	private final Map<String, String> factoryProperties;

	// Current state
	private Session session;
	private Node node;
	private String nodePath;// useful when changing auth
	private String state;
	private Throwable exception;

	// Client services
	private final JavaScriptExecutor jsExecutor;
	private final BrowserNavigation browserNavigation;

	public AbstractCmsEntryPoint(Repository repository, String workspace, String defaultPath,
			Map<String, String> factoryProperties) {
		this.repository = repository;
		this.workspace = workspace;
		this.defaultPath = defaultPath;
		this.factoryProperties = new HashMap<String, String>(factoryProperties);
		// subject = new Subject();

		// Initial login
		LoginContext lc;
		try {
			lc = new LoginContext(CmsAuth.LOGIN_CONTEXT_USER,
					new RemoteAuthCallbackHandler(new ServletHttpRequest(UiContext.getHttpRequest()),
							new ServletHttpResponse(UiContext.getHttpResponse())));
			lc.login();
		} catch (LoginException e) {
			try {
				lc = new LoginContext(CmsAuth.LOGIN_CONTEXT_ANONYMOUS);
				lc.login();
			} catch (LoginException e1) {
				throw new CmsException("Cannot log in as anonymous", e1);
			}
		}
		authChange(lc);

		jsExecutor = RWT.getClient().getService(JavaScriptExecutor.class);
		browserNavigation = RWT.getClient().getService(BrowserNavigation.class);
		if (browserNavigation != null)
			browserNavigation.addBrowserNavigationListener(new CmsNavigationListener());
	}

	@Override
	protected Shell createShell(Display display) {
		Shell shell = super.createShell(display);
		shell.setData(RWT.CUSTOM_VARIANT, CmsStyles.CMS_SHELL);
		display.disposeExec(new Runnable() {

			@Override
			public void run() {
				if (log.isTraceEnabled())
					log.trace("Logging out " + session);
				JcrUtils.logoutQuietly(session);
			}
		});
		return shell;
	}

	@Override
	protected final void createContents(final Composite parent) {
		// UiContext.setData(CmsView.KEY, this);
		CmsSwtUtils.registerCmsView(parent.getShell(), this);
		Subject.doAs(getSubject(), new PrivilegedAction<Void>() {
			@Override
			public Void run() {
				try {
					initUi(parent);
				} catch (Exception e) {
					throw new CmsException("Cannot create entrypoint contents", e);
				}
				return null;
			}
		});
	}

	/** Create UI */
	protected abstract void initUi(Composite parent);

	/** Recreate UI after navigation or auth change */
	protected abstract void refresh();

	/**
	 * The node to return when no node was found (for authenticated users and
	 * anonymous)
	 */
//	private Node getDefaultNode(Session session) throws RepositoryException {
//		if (!session.hasPermission(defaultPath, "read")) {
//			String userId = session.getUserID();
//			if (userId.equals(NodeConstants.ROLE_ANONYMOUS))
//				// TODO throw a special exception
//				throw new CmsException("Login required");
//			else
//				throw new CmsException("Unauthorized");
//		}
//		return session.getNode(defaultPath);
//	}

	protected String getBaseTitle() {
		return factoryProperties.get(WebClient.PAGE_TITLE);
	}

	public void navigateTo(String state) {
		exception = null;
		String title = setState(state);
		doRefresh();
		if (browserNavigation != null)
			browserNavigation.pushState(state, title);
	}

	// @Override
	// public synchronized Subject getSubject() {
	// return subject;
	// }

	// @Override
	// public LoginContext getLoginContext() {
	// return loginContext;
	// }
	protected Subject getSubject() {
		return loginContext.getSubject();
	}

	@Override
	public boolean isAnonymous() {
		return CurrentUser.isAnonymous(getSubject());
	}

	@Override
	public synchronized void logout() {
		if (loginContext == null)
			throw new CmsException("Login context should not be null");
		try {
			CurrentUser.logoutCmsSession(loginContext.getSubject());
			loginContext.logout();
			LoginContext anonymousLc = new LoginContext(CmsAuth.LOGIN_CONTEXT_ANONYMOUS);
			anonymousLc.login();
			authChange(anonymousLc);
		} catch (LoginException e) {
			log.error("Cannot logout", e);
		}
	}

	@Override
	public synchronized void authChange(LoginContext lc) {
		if (lc == null)
			throw new CmsException("Login context cannot be null");
		// logout previous login context
		if (this.loginContext != null)
			try {
				this.loginContext.logout();
			} catch (LoginException e1) {
				log.warn("Could not log out: " + e1);
			}
		this.loginContext = lc;
		Subject.doAs(getSubject(), new PrivilegedAction<Void>() {

			@Override
			public Void run() {
				try {
					JcrUtils.logoutQuietly(session);
					session = repository.login(workspace);
					if (nodePath != null)
						try {
							node = session.getNode(nodePath);
						} catch (PathNotFoundException e) {
							navigateTo("~");
						}

					// refresh UI
					doRefresh();
				} catch (RepositoryException e) {
					throw new CmsException("Cannot perform auth change", e);
				}
				return null;
			}

		});
	}

	@Override
	public void exception(final Throwable e) {
		AbstractCmsEntryPoint.this.exception = e;
		log.error("Unexpected exception in CMS", e);
		doRefresh();
	}

	protected synchronized void doRefresh() {
		Subject.doAs(getSubject(), new PrivilegedAction<Void>() {
			@Override
			public Void run() {
				refresh();
				return null;
			}
		});
	}

	/** Sets the state of the entry point and retrieve the related JCR node. */
	protected synchronized String setState(String newState) {
		String previousState = this.state;

		String newNodePath = null;
		String prefix = null;
		this.state = newState;
		if (newState.equals("~"))
			this.state = "";

		try {
			int firstSlash = state.indexOf('/');
			if (firstSlash == 0) {
				newNodePath = state;
				prefix = "";
			} else if (firstSlash > 0) {
				prefix = state.substring(0, firstSlash);
				newNodePath = state.substring(firstSlash);
			} else {
				newNodePath = defaultPath;
				prefix = state;

			}

			// auth
			int colonIndex = prefix.indexOf('$');
			if (colonIndex > 0) {
				SharedSecret token = new SharedSecret(new AuthPassword(X_SHARED_SECRET + '$' + prefix)) {

					@Override
					public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
						super.handle(callbacks);
						// handle HTTP context
						for (Callback callback : callbacks) {
							if (callback instanceof RemoteAuthCallback) {
								((RemoteAuthCallback) callback)
										.setRequest(new ServletHttpRequest(UiContext.getHttpRequest()));
								((RemoteAuthCallback) callback)
										.setResponse(new ServletHttpResponse(UiContext.getHttpResponse()));
							}
						}
					}
				};
				LoginContext lc = new LoginContext(CmsAuth.LOGIN_CONTEXT_USER, token);
				lc.login();
				authChange(lc);// sets the node as well
				// } else {
				// // TODO check consistency
				// }
			} else {
				Node newNode = null;
				if (session.nodeExists(newNodePath))
					newNode = session.getNode(newNodePath);
				else {
//					throw new CmsException("Data " + newNodePath + " does not exist");
					newNode = null;
				}
				setNode(newNode);
			}
			String title = publishMetaData(getNode());

			if (log.isTraceEnabled())
				log.trace("node=" + newNodePath + ", state=" + state + " (prefix=" + prefix + ")");

			return title;
		} catch (Exception e) {
			log.error("Cannot set state '" + state + "'", e);
			if (state.equals("") || newState.equals("~") || newState.equals(previousState))
				return "Unrecoverable exception : " + e.getClass().getSimpleName();
			if (previousState.equals(""))
				previousState = "~";
			navigateTo(previousState);
			throw new CmsException("Unexpected issue when accessing #" + newState, e);
		}
	}

	private String publishMetaData(Node node) throws RepositoryException {
		// Title
		String title;
		if (node != null && node.isNodeType(NodeType.MIX_TITLE) && node.hasProperty(Property.JCR_TITLE))
			title = node.getProperty(Property.JCR_TITLE).getString() + " - " + getBaseTitle();
		else
			title = getBaseTitle();

		HttpServletRequest request = UiContext.getHttpRequest();
		if (request == null)
			return null;

		StringBuilder js = new StringBuilder();
		if (title == null)
			title = "";
		title = title.replace("'", "\\'");// sanitize
		js.append("document.title = '" + title + "';");
		jsExecutor.execute(js.toString());
		return title;
	}

	// Simply remove some illegal character
	// private String clean(String stringToClean) {
	// return stringToClean.replaceAll("'", "").replaceAll("\\n", "")
	// .replaceAll("\\t", "");
	// }

	protected synchronized Node getNode() {
		return node;
	}

	private synchronized void setNode(Node node) throws RepositoryException {
		this.node = node;
		this.nodePath = node == null ? null : node.getPath();
	}

	protected String getState() {
		return state;
	}

	protected Throwable getException() {
		return exception;
	}

	protected void resetException() {
		exception = null;
	}

	protected Session getSession() {
		return session;
	}

	private class CmsNavigationListener implements BrowserNavigationListener {
		private static final long serialVersionUID = -3591018803430389270L;

		@Override
		public void navigated(BrowserNavigationEvent event) {
			setState(event.getState());
			doRefresh();
		}
	}
}