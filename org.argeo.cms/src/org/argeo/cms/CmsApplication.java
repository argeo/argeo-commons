package org.argeo.cms;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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
import org.argeo.cms.auth.LoginRequiredException;
import org.argeo.cms.internal.ImageManagerImpl;
import org.argeo.cms.util.BundleResourceLoader;
import org.argeo.cms.util.CmsUtils;
import org.argeo.jcr.JcrUtils;
import org.eclipse.gemini.blueprint.context.BundleContextAware;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.rap.rwt.application.Application;
import org.eclipse.rap.rwt.application.Application.OperationMode;
import org.eclipse.rap.rwt.application.ApplicationConfiguration;
import org.eclipse.rap.rwt.application.EntryPoint;
import org.eclipse.rap.rwt.application.EntryPointFactory;
import org.eclipse.rap.rwt.application.ExceptionHandler;
import org.eclipse.rap.rwt.client.WebClient;
import org.eclipse.rap.rwt.service.ResourceLoader;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.osgi.framework.BundleContext;

/** Configures an Argeo CMS RWT application. */
public class CmsApplication implements CmsConstants, ApplicationConfiguration,
		BundleContextAware {
	private final static Log log = LogFactory.getLog(CmsApplication.class);

	private Map<String, Map<String, String>> branding = new HashMap<String, Map<String, String>>();
	private Map<String, List<String>> styleSheets = new HashMap<String, List<String>>();

	private List<String> resources = new ArrayList<String>();

	private BundleContext bundleContext;

	private Repository repository;
	private String workspace = null;
	private String basePath = "/";
	private List<String> roPrincipals = Arrays.asList("anonymous", "everyone");
	private List<String> rwPrincipals = Arrays.asList("everyone");

	private CmsUiProvider header;
	private Map<String, CmsUiProvider> pages = new LinkedHashMap<String, CmsUiProvider>();

	private Integer headerHeight = 40;

	// Managers
	private CmsImageManager imageManager = new ImageManagerImpl();

	public void configure(Application application) {
		try {
			application.setOperationMode(OperationMode.SWT_COMPATIBILITY);
			application.setExceptionHandler(new CmsExceptionHandler());

			// TODO load all pics under icons
			// loading animated gif
			application.addResource(LOADING_IMAGE,
					createResourceLoader(LOADING_IMAGE));
			// empty image
			application.addResource(NO_IMAGE, createResourceLoader(NO_IMAGE));

			for (String resource : resources) {
				application.addResource(resource, new BundleResourceLoader(
						bundleContext));
				if (log.isDebugEnabled())
					log.debug("Registered resource " + resource);
			}

			Map<String, String> defaultBranding = null;
			if (branding.containsKey("*"))
				defaultBranding = branding.get("*");

			// entry points
			for (String page : pages.keySet()) {
				Map<String, String> properties = defaultBranding != null ? new HashMap<String, String>(
						defaultBranding) : new HashMap<String, String>();
				if (branding.containsKey(page)) {
					properties.putAll(branding.get(page));
				}
				// favicon
				if (properties.containsKey(WebClient.FAVICON)) {
					String faviconRelPath = properties.get(WebClient.FAVICON);
					application.addResource(faviconRelPath,
							new BundleResourceLoader(bundleContext));
					if (log.isTraceEnabled())
						log.trace("Registered favicon " + faviconRelPath);

				}

				// page title
				if (!properties.containsKey(WebClient.PAGE_TITLE))
					properties.put(
							WebClient.PAGE_TITLE,
							Character.toUpperCase(page.charAt(0))
									+ page.substring(1));

				// default body HTML
				if (!properties.containsKey(WebClient.BODY_HTML))
					properties.put(WebClient.BODY_HTML, DEFAULT_LOADING_BODY);

				application.addEntryPoint("/" + page, new CmsEntryPointFactory(
						page), properties);
				log.info("Registered entry point /" + page);
			}

			// stylesheets
			for (String themeId : styleSheets.keySet()) {
				List<String> cssLst = styleSheets.get(themeId);
				for (String css : cssLst) {
					application.addStyleSheet(themeId, css,
							new BundleResourceLoader(bundleContext));
				}

			}
		} catch (RuntimeException e) {
			// Easier access to initialisation errors
			log.error("Unexpected exception when configuring RWT application.",
					e);
			throw e;
		}
	}

	public void init() throws RepositoryException {
		Session session = null;
		try {
			session = JcrUtils.loginOrCreateWorkspace(repository, workspace);
			VersionManager vm = session.getWorkspace().getVersionManager();
			if (!vm.isCheckedOut("/"))
				vm.checkout("/");
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

	public void setCmsLogin(@SuppressWarnings("deprecation") CmsLogin cmsLogin) {
		// this.cmsLogin = cmsLogin;
		log.warn("cmsLogin"
				+ " is deprecated and will be removed soon. Adapt your configuration ASAP.");
	}

	public void setHeader(CmsUiProvider header) {
		this.header = header;
	}

	public void setPages(Map<String, CmsUiProvider> pages) {
		this.pages = pages;
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

	public void setBranding(Map<String, Map<String, String>> branding) {
		this.branding = branding;
	}

	public void setStyleSheets(Map<String, List<String>> styleSheets) {
		this.styleSheets = styleSheets;
	}

	public void setBundleContext(BundleContext bundleContext) {
		this.bundleContext = bundleContext;
	}

	public void setResources(List<String> resources) {
		this.resources = resources;
	}

	class CmsExceptionHandler implements ExceptionHandler {

		@Override
		public void handleException(Throwable throwable) {
			CmsSession.current.get().exception(throwable);
		}

	}

	private class CmsEntryPointFactory implements EntryPointFactory {
		private final String page;

		public CmsEntryPointFactory(String page) {
			this.page = page;
		}

		@Override
		public EntryPoint create() {
			CmsEntryPoint entryPoint = new CmsEntryPoint(repository, workspace,
					pages.get(page));
			entryPoint.setState("");
			CmsSession.current.set(entryPoint);
			return entryPoint;
		}

	}

	private class CmsEntryPoint extends AbstractCmsEntryPoint {
		private Composite headerArea;
		private Composite bodyArea;
		private final CmsUiProvider uiProvider;

		public CmsEntryPoint(Repository repository, String workspace,
				CmsUiProvider uiProvider) {
			super(repository, workspace);
			this.uiProvider = uiProvider;
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
				// Should not be set here: it then prevent all children
				// composite to define a background color via CSS
				// bodyArea.setBackgroundMode(SWT.INHERIT_DEFAULT);
				bodyArea.setLayout(CmsUtils.noSpaceGridLayout());
				refreshBody();
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
				try {
					if (state == null)
						throw new CmsException("State cannot be null");
					uiProvider.createUi(bodyArea, getNode());
				} catch (RepositoryException e) {
					throw new CmsException("Cannot refresh body", e);
				}
			}
			bodyArea.layout(true, true);
		}

		@Override
		protected Node getDefaultNode(Session session)
				throws RepositoryException {
			if (!session.hasPermission(basePath, "read")) {
				if (session.getUserID().equals("anonymous"))
					throw new LoginRequiredException();
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

	private static ResourceLoader createResourceLoader(final String resourceName) {
		return new ResourceLoader() {
			public InputStream getResourceAsStream(String resourceName)
					throws IOException {
				return getClass().getClassLoader().getResourceAsStream(
						resourceName);
			}
		};
	}

	/*
	 * TEXTS
	 */
	private static String DEFAULT_LOADING_BODY = "<div"
			+ " style=\"position: absolute; left: 50%; top: 50%; margin: -32px -32px; width: 64px; height:64px\">"
			+ "<img src=\"./rwt-resources/icons/loading.gif\" width=\"32\" height=\"32\" style=\"margin: 16px 16px\"/>"
			+ "</div>";
}
