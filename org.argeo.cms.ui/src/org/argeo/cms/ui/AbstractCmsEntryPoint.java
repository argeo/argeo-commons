package org.argeo.cms.ui;

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
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.cms.CmsException;
import org.argeo.cms.auth.HttpRequestCallbackHandler;
import org.argeo.eclipse.ui.specific.UiContext;
import org.argeo.jcr.JcrUtils;
import org.argeo.node.NodeConstants;
import org.argeo.node.security.NodeAuthenticated;
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
public abstract class AbstractCmsEntryPoint extends AbstractEntryPoint implements CmsView {
	private static final long serialVersionUID = 906558779562569784L;

	private final Log log = LogFactory.getLog(AbstractCmsEntryPoint.class);

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
	private String page;
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
		try {
			loginContext = new LoginContext(NodeConstants.LOGIN_CONTEXT_USER,
					new HttpRequestCallbackHandler(UiContext.getHttpRequest(), UiContext.getHttpResponse()));
			loginContext.login();
		} catch (LoginException e) {
			try {
				loginContext = new LoginContext(NodeConstants.LOGIN_CONTEXT_USER);
				loginContext.login();
			} catch (LoginException e1) {
				throw new CmsException("Cannot log in as anonymous", e1);
			}
		}
		authChange(loginContext);

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
		UiContext.setData(NodeAuthenticated.KEY, this);
		Subject.doAs(loginContext.getSubject(), new PrivilegedAction<Void>() {
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
	protected Node getDefaultNode(Session session) throws RepositoryException {
		if (!session.hasPermission(defaultPath, "read")) {
			String userId = session.getUserID();
			if (userId.equals(NodeConstants.ROLE_ANONYMOUS))
				// TODO throw a special exception
				throw new CmsException("Login required");
			else
				throw new CmsException("Unauthorized");
		}
		return session.getNode(defaultPath);
	}

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

	@Override
	public LoginContext getLoginContext() {
		return loginContext;
	}

	@Override
	public synchronized void logout() {
		if (loginContext == null)
			throw new CmsException("Login context should not be null");
		try {
			loginContext.logout();
			LoginContext anonymousLc = new LoginContext(NodeConstants.LOGIN_CONTEXT_USER);
			anonymousLc.login();
			authChange(anonymousLc);
		} catch (LoginException e) {
			log.error("Cannot logout", e);
		}
	}

	@Override
	public synchronized void authChange(LoginContext loginContext) {
		if (loginContext == null)
			throw new CmsException("Login context cannot be null");
		this.loginContext = loginContext;
		Subject.doAs(loginContext.getSubject(), new PrivilegedAction<Void>() {

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
		Subject.doAs(loginContext.getSubject(), new PrivilegedAction<Void>() {
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

		Node node = null;
		page = null;
		this.state = newState;
		if (newState.equals("~"))
			this.state = "";

		try {
			int firstSlash = state.indexOf('/');
			if (firstSlash == 0) {
				node = session.getNode(state);
				page = "";
			} else if (firstSlash > 0) {
				String prefix = state.substring(0, firstSlash);
				String path = state.substring(firstSlash);
				if (session.nodeExists(path))
					node = session.getNode(path);
				else
					throw new CmsException("Data " + path + " does not exist");
				page = prefix;
			} else {
				node = getDefaultNode(session);
				page = state;
			}
			setNode(node);
			String title = publishMetaData(node);

			if (log.isTraceEnabled())
				log.trace("node=" + node + ", state=" + state + " (page=" + page + ")");

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
		if (node.isNodeType(NodeType.MIX_TITLE) && node.hasProperty(Property.JCR_TITLE))
			title = node.getProperty(Property.JCR_TITLE).getString() + " - " + getBaseTitle();
		else
			title = getBaseTitle();

		HttpServletRequest request = UiContext.getHttpRequest();
		if (request == null)
			return null;

		StringBuilder js = new StringBuilder();
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
			refresh();
		}
	}
}