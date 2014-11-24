package org.argeo.cms;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.security.Privilege;
import javax.jcr.version.VersionManager;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.cms.internal.ImageManagerImpl;
import org.argeo.jcr.JcrUtils;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.rap.rwt.application.EntryPoint;
import org.eclipse.rap.rwt.application.EntryPointFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/** Creates and registers an {@link EntryPoint} */
public class CmsEntryPointFactory implements EntryPointFactory {
	private final static Log log = LogFactory
			.getLog(CmsEntryPointFactory.class);

	private Repository repository;
	private String workspace = null;
	private String basePath = "/";
	private List<String> roPrincipals = Arrays.asList("anonymous", "everyone");
	private List<String> rwPrincipals = Arrays.asList("everyone");

	private CmsLogin cmsLogin;

	private CmsUiProvider header;
	// private CmsUiProvider dynamicPages;
	// private Map<String, CmsUiProvider> staticPages;
	private Map<String, CmsUiProvider> pages = new LinkedHashMap<String, CmsUiProvider>();

	private Integer headerHeight = 40;

	// Managers
	private CmsImageManager imageManager = new ImageManagerImpl();

	@Override
	public EntryPoint create() {
		CmsEntryPoint cmsEntryPoint = new CmsEntryPoint(repository, workspace);
		CmsSession.current.set(cmsEntryPoint);
		return cmsEntryPoint;
	}

	public void init() throws RepositoryException {
		if (workspace == null)
			throw new CmsException(
					"Workspace must be set when calling initialization."
							+ " Please make sure that read-only and read-write roles"
							+ " have been properly configured:"
							+ " the defaults are open.");

		Session session = null;
		try {
			session = JcrUtils.loginOrCreateWorkspace(repository, workspace);
			VersionManager vm = session.getWorkspace().getVersionManager();
			if (!vm.isCheckedOut("/"))
				vm.checkout("/");
			// session = repository.login(workspace);
			JcrUtils.mkdirs(session, basePath);
			for (String principal : rwPrincipals)
				JcrUtils.addPrivilege(session, basePath, principal,
						Privilege.JCR_WRITE);
			for (String principal : roPrincipals)
				JcrUtils.addPrivilege(session, basePath, principal,
						Privilege.JCR_READ);

			for (String pageName : pages.keySet()) {
				try {
					initPage(session, pages.get(pageName));
					session.save();
				} catch (Exception e) {
					throw new CmsException(
							"Cannot initialize page " + pageName, e);
				}
			}

		} finally {
			JcrUtils.logoutQuietly(session);
		}
	}

	protected void initPage(Session adminSession, CmsUiProvider page)
			throws RepositoryException {
		if (page instanceof LifeCycleUiProvider)
			((LifeCycleUiProvider) page).init(adminSession);
	}

	public void destroy() {
		for (String pageName : pages.keySet()) {
			try {
				CmsUiProvider page = pages.get(pageName);
				if (page instanceof LifeCycleUiProvider)
					((LifeCycleUiProvider) page).destroy();
			} catch (Exception e) {
				log.error("Cannot destroy page " + pageName, e);
			}
		}
	}

	public void setRepository(Repository repository) {
		this.repository = repository;
	}

	public void setWorkspace(String workspace) {
		this.workspace = workspace;
	}

	public void setCmsLogin(CmsLogin cmsLogin) {
		this.cmsLogin = cmsLogin;
	}

	public void setHeader(CmsUiProvider header) {
		this.header = header;
	}

	public void setPages(Map<String, CmsUiProvider> pages) {
		this.pages = pages;
	}

	@Deprecated
	public void setDynamicPages(CmsUiProvider dynamicPages) {
		log.warn("'dynamicPages' is deprecated, use 'pages' instead, with \"\" as key");
		pages.put("", dynamicPages);
	}

	@Deprecated
	public void setStaticPages(Map<String, CmsUiProvider> staticPages) {
		log.warn("'staticPages' is deprecated, use 'pages' instead");
		pages.putAll(staticPages);
	}

	public void setBasePath(String basePath) {
		this.basePath = basePath;
	}

	public void setRoPrincipals(List<String> roPrincipals) {
		this.roPrincipals = roPrincipals;
	}

	public void setRwPrincipals(List<String> rwPrincipals) {
		this.rwPrincipals = rwPrincipals;
	}

	public void setHeaderHeight(Integer headerHeight) {
		this.headerHeight = headerHeight;
	}

	private class CmsEntryPoint extends AbstractCmsEntryPoint {
		private Composite headerArea;
		private Composite bodyArea;

		public CmsEntryPoint(Repository repository, String workspace) {
			super(repository, workspace);
		}

		@Override
		protected void createContents(Composite parent) {
			try {
				getShell().getDisplay().setData(CmsSession.KEY, this);

				parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true,
						true));
				parent.setLayout(CmsUtils.noSpaceGridLayout());

				headerArea = new Composite(parent, SWT.NONE);
				headerArea.setLayout(new FillLayout());
				GridData headerData = new GridData(SWT.FILL, SWT.FILL, false,
						false);
				headerData.heightHint = headerHeight;
				headerArea.setLayoutData(headerData);
				refreshHeader();

				bodyArea = new Composite(parent, SWT.NONE);
				bodyArea.setData(RWT.CUSTOM_VARIANT, CmsStyles.CMS_BODY);
				bodyArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true,
						true));
				bodyArea.setBackgroundMode(SWT.INHERIT_DEFAULT);
				bodyArea.setLayout(CmsUtils.noSpaceGridLayout());
			} catch (Exception e) {
				throw new CmsException("Cannot create entrypoint contents", e);
			}
		}

		@Override
		protected void refreshHeader() {
			if (headerArea == null)
				return;
			for (Control child : headerArea.getChildren())
				child.dispose();
			try {
				header.createUi(headerArea, getNode());
			} catch (RepositoryException e) {
				throw new CmsException("Cannot refresh header", e);
			}
			headerArea.layout(true, true);
		}

		@Override
		protected void refreshBody() {
			if (bodyArea == null)
				return;
			// clear
			for (Control child : bodyArea.getChildren())
				child.dispose();
			bodyArea.setLayout(CmsUtils.noSpaceGridLayout());

			// Exception
			Throwable exception = getException();
			if (exception != null) {
				new Label(bodyArea, SWT.NONE).setText("Unreachable state : "
						+ getState());
				if (getNode() != null)
					new Label(bodyArea, SWT.NONE).setText("Context : "
							+ getNode());

				Text errorText = new Text(bodyArea, SWT.MULTI | SWT.H_SCROLL
						| SWT.V_SCROLL);
				errorText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true,
						true));
				StringWriter sw = new StringWriter();
				exception.printStackTrace(new PrintWriter(sw));
				errorText.setText(sw.toString());
				IOUtils.closeQuietly(sw);
				resetException();
				// TODO report
			} else {
				String state = getState();
				String page = getPage();
				try {
					if (state == null)
						throw new CmsException("State cannot be null");
					if (page == null)
						throw new CmsException("Page cannot be null");
					// else if (state.length() == 0)
					// log.debug("empty state");
					else if (pages.containsKey(page))
						pages.get(page).createUi(bodyArea, getNode());
					else {
						// try {
						// RWT.getResponse().sendError(404);
						// } catch (IOException e) {
						// log.error("Cannot send 404 code", e);
						// }
						throw new CmsException("Unsupported state " + state);
					}
				} catch (RepositoryException e) {
					throw new CmsException("Cannot refresh body", e);
				}
			}
			bodyArea.layout(true, true);
		}

		@Override
		protected void logAsAnonymous() {
			cmsLogin.logInAsAnonymous();
		}

		@Override
		protected Node getDefaultNode(Session session)
				throws RepositoryException {
			if (!session.hasPermission(basePath, "read")) {
				if (session.getUserID().equals("anonymous"))
					throw new CmsLoginRequiredException();
				else
					throw new CmsException("Unauthorized");
			}
			return session.getNode(basePath);
		}

		@Override
		public CmsImageManager getImageManager() {
			return imageManager;
		}

	}
}
