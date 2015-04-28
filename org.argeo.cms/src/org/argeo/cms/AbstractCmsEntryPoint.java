package org.argeo.cms;

import java.util.Locale;
import java.util.ResourceBundle;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.ArgeoException;
import org.argeo.cms.auth.ArgeoLoginContext;
import org.argeo.cms.i18n.Msg;
import org.argeo.jcr.JcrUtils;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.rap.rwt.application.AbstractEntryPoint;
import org.eclipse.rap.rwt.client.service.BrowserNavigation;
import org.eclipse.rap.rwt.client.service.BrowserNavigationEvent;
import org.eclipse.rap.rwt.client.service.BrowserNavigationListener;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/** Manages history and navigation */
abstract class AbstractCmsEntryPoint extends AbstractEntryPoint implements
		CmsSession {
	private final Log log = LogFactory.getLog(AbstractCmsEntryPoint.class);

	private Subject subject = new Subject();

	private Repository repository;
	private String workspace;
	private Session session;

	// current state
	private Node node;
	private String state;
	private String page;
	private Throwable exception;

	private BrowserNavigation history;

	public AbstractCmsEntryPoint(Repository repository, String workspace) {
		this.repository = repository;
		this.workspace = workspace;

		// Initial login
		Subject subject = new Subject();
		try {
			new ArgeoLoginContext(KernelHeader.LOGIN_CONTEXT_USER, subject)
					.login();
		} catch (LoginException e) {
			if (log.isTraceEnabled())
				log.trace("Cannot authenticate user", e);
			try {
				new ArgeoLoginContext(KernelHeader.LOGIN_CONTEXT_ANONYMOUS,
						subject).login();
			} catch (LoginException eAnonymous) {
				throw new ArgeoException("Cannot initialize subject",
						eAnonymous);
			}
		}
		authChange();

		history = RWT.getClient().getService(BrowserNavigation.class);
		if (history != null)
			history.addBrowserNavigationListener(new CmsNavigationListener());

		// RWT.setLocale(Locale.FRANCE);
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

	/** Recreate header UI */
	protected abstract void refreshHeader();

	/** Recreate body UI */
	protected abstract void refreshBody();

	/**
	 * The node to return when no node was found (for authenticated users and
	 * anonymous)
	 */
	protected abstract Node getDefaultNode(Session session)
			throws RepositoryException;

	/**
	 * Reasonable default since it is a nt:hierarchyNode and is thus compatible
	 * with the obvious default folder type, nt:folder, conceptual equivalent of
	 * an empty text file in an operating system. To be overridden.
	 */
	protected String getDefaultNewNodeType() {
		return CmsTypes.CMS_TEXT;
	}

	/** Default new folder type (used in mkdirs) is nt:folder. To be overridden. */
	protected String getDefaultNewFolderType() {
		return NodeType.NT_FOLDER;
	}

	public void navigateTo(String state) {
		exception = null;
		setState(state);
		refreshBody();
		if (history != null)
			history.pushState(state, state);
	}

	@Override
	public Subject getSubject() {
		return subject;
	}

	@Override
	public void authChange() {
		try {
			String currentPath = null;
			if (node != null)
				currentPath = node.getPath();
			JcrUtils.logoutQuietly(session);

			session = repository.login(workspace);
			if (currentPath != null)
				node = session.getNode(currentPath);

			// refresh UI
			refreshHeader();
			refreshBody();
		} catch (RepositoryException e) {
			throw new CmsException("Cannot perform auth change", e);
		}

	}

	@Override
	public void exception(Throwable e) {
		this.exception = e;
		log.error("Unexpected exception in CMS", e);
		refreshBody();
	}

	@Override
	public Object local(Msg msg) {
		String key = msg.getId();
		int lastDot = key.lastIndexOf('.');
		String className = key.substring(0, lastDot);
		String fieldName = key.substring(lastDot + 1);
		Locale locale = RWT.getLocale();
		ResourceBundle rb = ResourceBundle.getBundle(className, locale,
				msg.getClassLoader());
		return rb.getString(fieldName);
	}

	/** Sets the state of the entry point and retrieve the related JCR node. */
	protected synchronized void setState(String newState) {
		String previousState = this.state;

		node = null;
		page = null;
		this.state = newState;

		try {
			int firstSlash = state.indexOf('/');
			if (firstSlash == 0) {
				if (!session.nodeExists(state))
					node = addNode(session, state, null);
				else
					node = session.getNode(state);
				page = "";
			} else if (firstSlash > 0) {
				String prefix = state.substring(0, firstSlash);
				String path = state.substring(firstSlash);
				if (session.getWorkspace().getNodeTypeManager()
						.hasNodeType(prefix)) {
					String nodeType = prefix;
					if (!session.nodeExists(path))
						node = addNode(session, path, nodeType);
					else {
						node = session.getNode(path);
						if (!node.isNodeType(nodeType))
							throw new CmsException("Node " + path
									+ " not of type " + nodeType);
					}
				} else if ("delete".equals(prefix)) {
					if (session.itemExists(path)) {
						Node nodeToDelete = session.getNode(path);
						// TODO "Are you sure?"
						nodeToDelete.remove();
						session.save();
						log.debug("Deleted " + path);
						navigateTo(previousState);
					} else
						throw new CmsException("Data " + path
								+ " does not exist");
				} else {
					if (session.itemExists(path))
						node = session.getNode(path);
					else
						throw new CmsException("Data " + path
								+ " does not exist");
				}
				page = prefix;
			} else {
				node = getDefaultNode(session);
				page = state;
			}

			if (log.isTraceEnabled())
				log.trace("node=" + node + ", state=" + state + " (page="
						+ page);

		} catch (RepositoryException e) {
			throw new CmsException("Cannot retrieve node", e);
		}
	}

	protected Node addNode(Session session, String path, String nodeType)
			throws RepositoryException {
		return JcrUtils.mkdirs(session, path, nodeType != null ? nodeType
				: getDefaultNewNodeType(), getDefaultNewFolderType(), false);
		// not saved, so that the UI can discard it later on
	}

	protected Node getNode() {
		return node;
	}

	@Override
	public String getState() {
		return state;
	}

	// String getPage() {
	// return page;
	// }

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
			refreshBody();
		}
	}

}
