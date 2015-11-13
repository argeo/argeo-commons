package org.argeo.cms;

import static javax.jcr.Property.JCR_DESCRIPTION;

import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import javax.security.auth.Subject;
import javax.security.auth.login.CredentialNotFoundException;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.ArgeoException;
import org.argeo.cms.auth.AuthConstants;
import org.argeo.cms.auth.HttpRequestCallbackHandler;
import org.argeo.cms.util.CmsUtils;
import org.argeo.eclipse.ui.specific.UiContext;
import org.argeo.jcr.JcrUtils;
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
public abstract class AbstractCmsEntryPoint extends AbstractEntryPoint
		implements CmsView {
	private final Log log = LogFactory.getLog(AbstractCmsEntryPoint.class);

	private final Subject subject;
	private LoginContext loginContext;

	private final Repository repository;
	private final String workspace;
	private final String defaultPath;
	private final Map<String, String> factoryProperties;

	// Current state
	private Session session;
	private Node node;
	private String state;
	private String page;
	private Throwable exception;

	// Client services
	private final JavaScriptExecutor jsExecutor;
	private final BrowserNavigation browserNavigation;

	public AbstractCmsEntryPoint(Repository repository, String workspace,
			String defaultPath, Map<String, String> factoryProperties) {
		this.repository = repository;
		this.workspace = workspace;
		this.defaultPath = defaultPath;
		this.factoryProperties = new HashMap<String, String>(factoryProperties);
		subject = new Subject();

		// Initial login
		try {
			loginContext = new LoginContext(AuthConstants.LOGIN_CONTEXT_USER,
					subject, new HttpRequestCallbackHandler(
							UiContext.getHttpRequest()));
			loginContext.login();
		} catch (CredentialNotFoundException e) {
			try {
				loginContext = new LoginContext(
						AuthConstants.LOGIN_CONTEXT_ANONYMOUS, subject);
				loginContext.login();
			} catch (LoginException e1) {
				throw new ArgeoException("Cannot log as anonymous", e);
			}
		} catch (LoginException e) {
			throw new ArgeoException("Cannot initialize subject", e);
		}
		authChange(loginContext);

		jsExecutor = RWT.getClient().getService(JavaScriptExecutor.class);
		browserNavigation = RWT.getClient().getService(BrowserNavigation.class);
		if (browserNavigation != null)
			browserNavigation
					.addBrowserNavigationListener(new CmsNavigationListener());
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
		UiContext.setData(CmsView.KEY, this);
		Subject.doAs(subject, new PrivilegedAction<Void>() {
			@Override
			public Void run() {
				try {
					initUi(parent);
				} catch (Exception e) {
					throw new CmsException("Cannot create entrypoint contents",
							e);
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
			if (session.getUserID().equals(AuthConstants.ROLE_ANONYMOUS))
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

	@Override
	public Subject getSubject() {
		return subject;
	}

	@Override
	public void logout() {
		if (loginContext == null)
			throw new CmsException("Login context should not be null");
		try {
			loginContext.logout();
			LoginContext anonymousLc = new LoginContext(
					AuthConstants.LOGIN_CONTEXT_ANONYMOUS, subject);
			anonymousLc.login();
			authChange(anonymousLc);
		} catch (LoginException e) {
			throw new CmsException("Cannot logout", e);
		}
	}

	@Override
	public void authChange(LoginContext loginContext) {
		if (loginContext == null)
			throw new CmsException("Login context cannot be null");
		this.loginContext = loginContext;
		Subject.doAs(subject, new PrivilegedAction<Void>() {

			@Override
			public Void run() {
				try {
					String currentPath = null;
					if (node != null)
						currentPath = node.getPath();
					JcrUtils.logoutQuietly(session);

					session = repository.login(workspace);
					if (currentPath != null)
						try {
							node = session.getNode(currentPath);
						} catch (Exception e) {
							logout();
							session = repository.login(workspace);
							navigateTo("~");
							throw e;
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

	protected void doRefresh() {
		Subject.doAs(subject, new PrivilegedAction<Void>() {
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

		node = null;
		page = null;
		this.state = newState;
		if (newState.equals("~"))
			this.state = "";

		try {
			int firstSlash = state.indexOf('/');
			if (firstSlash == 0) {
				if (session.nodeExists(state))
					node = session.getNode(state);
				else
					throw new CmsException("Data " + state + " does not exist");
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

			// Title
			String title;
			if (node.isNodeType(NodeType.MIX_TITLE)
					&& node.hasProperty(Property.JCR_TITLE))
				title = node.getProperty(Property.JCR_TITLE).getString()
						+ " - " + getBaseTitle();
			else
				title = getBaseTitle();

			publishMetaData(title);

			if (log.isTraceEnabled())
				log.trace("node=" + node + ", state=" + state + " (page="
						+ page + ", title=" + title + ")");

			return title;
		} catch (Exception e) {
			log.error("Cannot set state '" + state + "'", e);
			if (previousState.equals(""))
				previousState = "~";
			navigateTo(previousState);
			throw new CmsException("Unexpected issue when accessing #"
					+ newState, e);
		}
	}

	private void publishMetaData(String title) throws RepositoryException {
		HttpServletRequest request = UiContext.getHttpRequest();
		if (request == null)
			return;
		String url = CmsUtils.getCanonicalUrl(node, request);
		String desc = node.hasProperty(JCR_DESCRIPTION) ? node.getProperty(
				JCR_DESCRIPTION).getString() : null;
		String imgUrl = null;
		for (NodeIterator it = node.getNodes(); it.hasNext();) {
			Node child = it.nextNode();
			if (child.isNodeType(CmsTypes.CMS_IMAGE))
				imgUrl = CmsUtils.getDataUrl(child, request);
		}

		StringBuilder js = new StringBuilder();
		js.append("document.title = '" + title + "';");
//		js.append("var metas = document.getElementsByTagName('meta');");
//		js.append("for (var i=0; i<metas.length; i++) {");
//		js.append("	if (metas[i].getAttribute('property'))");
//		js.append("	 if(metas[i].getAttribute('property')=='og:title')");
//		js.append("	  metas[i].setAttribute('content','" + title + "');");
//		js.append("	 else if(metas[i].getAttribute('property')=='og:url')");
//		js.append("	  metas[i].setAttribute('content','" + url + "');");
//		js.append("	 else if(metas[i].getAttribute('property')=='og:type')");
//		js.append("	  metas[i].setAttribute('content','website');");
//		if (desc != null) {
//			js.append("	 else if(metas[i].getAttribute('property')=='og:decription')");
//			js.append("	  metas[i].setAttribute('content','" + clean(desc)
//					+ "');");
//		}
//		if (imgUrl != null) {
//			js.append("	 else if(metas[i].getAttribute('property')=='og:image')");
//			js.append("	  metas[i].setAttribute('content','" + imgUrl + "');");
//		} else {
//			// TODO reset default image
//		}
//		js.append("	};");
		jsExecutor.execute(js.toString());
	}

	// Simply remove some illegal character
//	private String clean(String stringToClean) {
//		return stringToClean.replaceAll("'", "").replaceAll("\\n", "")
//				.replaceAll("\\t", "");
//	}

	protected Node getNode() {
		return node;
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