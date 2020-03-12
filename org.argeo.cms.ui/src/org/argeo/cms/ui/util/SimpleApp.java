package org.argeo.cms.ui.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.security.Privilege;
import javax.jcr.version.VersionManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.api.NodeConstants;
import org.argeo.api.NodeUtils;
import org.argeo.cms.CmsException;
import org.argeo.cms.ui.CmsConstants;
import org.argeo.cms.ui.CmsUiProvider;
import org.argeo.cms.ui.LifeCycleUiProvider;
import org.argeo.jcr.JcrUtils;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.rap.rwt.application.Application;
import org.eclipse.rap.rwt.application.Application.OperationMode;
import org.eclipse.rap.rwt.application.ApplicationConfiguration;
import org.eclipse.rap.rwt.application.EntryPoint;
import org.eclipse.rap.rwt.application.EntryPointFactory;
import org.eclipse.rap.rwt.application.ExceptionHandler;
import org.eclipse.rap.rwt.client.WebClient;
import org.eclipse.rap.rwt.client.service.JavaScriptExecutor;
import org.eclipse.rap.rwt.service.ResourceLoader;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/** A basic generic app based on {@link SimpleErgonomics}. */
public class SimpleApp implements CmsConstants, ApplicationConfiguration {
	private final static Log log = LogFactory.getLog(SimpleApp.class);

	private String contextName = null;

	private Map<String, Map<String, String>> branding = new HashMap<String, Map<String, String>>();
	private Map<String, List<String>> styleSheets = new HashMap<String, List<String>>();

	private List<String> resources = new ArrayList<String>();

	private BundleContext bundleContext;

	private Repository repository;
	private String workspace = null;
	private String jcrBasePath = "/";
	private List<String> roPrincipals = Arrays.asList(NodeConstants.ROLE_ANONYMOUS, NodeConstants.ROLE_USER);
	private List<String> rwPrincipals = Arrays.asList(NodeConstants.ROLE_USER);

	private CmsUiProvider header;
	private Map<String, CmsUiProvider> pages = new LinkedHashMap<String, CmsUiProvider>();

	private Integer headerHeight = 40;

	private ServiceRegistration<ApplicationConfiguration> appReg;

	public void configure(Application application) {
		try {
			BundleResourceLoader bundleRL = new BundleResourceLoader(bundleContext.getBundle());

			application.setOperationMode(OperationMode.SWT_COMPATIBILITY);
			// application.setOperationMode(OperationMode.JEE_COMPATIBILITY);

			application.setExceptionHandler(new CmsExceptionHandler());

			// loading animated gif
			application.addResource(LOADING_IMAGE, createResourceLoader(LOADING_IMAGE));
			// empty image
			application.addResource(NO_IMAGE, createResourceLoader(NO_IMAGE));

			for (String resource : resources) {
				application.addResource(resource, bundleRL);
				if (log.isTraceEnabled())
					log.trace("Resource " + resource);
			}

			Map<String, String> defaultBranding = null;
			if (branding.containsKey("*"))
				defaultBranding = branding.get("*");
			String defaultTheme = defaultBranding.get(WebClient.THEME_ID);

			// entry points
			for (String page : pages.keySet()) {
				Map<String, String> properties = defaultBranding != null ? new HashMap<String, String>(defaultBranding)
						: new HashMap<String, String>();
				if (branding.containsKey(page)) {
					properties.putAll(branding.get(page));
				}
				// favicon
				if (properties.containsKey(WebClient.FAVICON)) {
					String themeId = defaultBranding.get(WebClient.THEME_ID);
					Bundle themeBundle = ThemeUtils.findThemeBundle(bundleContext, themeId);
					String faviconRelPath = properties.get(WebClient.FAVICON);
					application.addResource(faviconRelPath,
							new BundleResourceLoader(themeBundle != null ? themeBundle : bundleContext.getBundle()));
					if (log.isTraceEnabled())
						log.trace("Favicon " + faviconRelPath);

				}

				// page title
				if (!properties.containsKey(WebClient.PAGE_TITLE)) {
					if (page.length() > 0)
						properties.put(WebClient.PAGE_TITLE, Character.toUpperCase(page.charAt(0)) + page.substring(1));
				}

				// default body HTML
				if (!properties.containsKey(WebClient.BODY_HTML))
					properties.put(WebClient.BODY_HTML, DEFAULT_LOADING_BODY);

				//
				// ADD ENTRY POINT
				//
				application.addEntryPoint("/" + page,
						new CmsEntryPointFactory(pages.get(page), repository, workspace, properties), properties);
				log.info("Page /" + page);
			}

			// stylesheets and themes
			Set<Bundle> themeBundles = new HashSet<>();
			for (String themeId : styleSheets.keySet()) {
				Bundle themeBundle = ThemeUtils.findThemeBundle(bundleContext, themeId);
				StyleSheetResourceLoader styleSheetRL = new StyleSheetResourceLoader(
						themeBundle != null ? themeBundle : bundleContext.getBundle());
				if (themeBundle != null)
					themeBundles.add(themeBundle);
				List<String> cssLst = styleSheets.get(themeId);
				if (log.isDebugEnabled())
					log.debug("Theme " + themeId);
				for (String css : cssLst) {
					application.addStyleSheet(themeId, css, styleSheetRL);
					if (log.isDebugEnabled())
						log.debug(" CSS " + css);
				}

			}
			for (Bundle themeBundle : themeBundles) {
				BundleResourceLoader themeBRL = new BundleResourceLoader(themeBundle);
				ThemeUtils.addThemeResources(application, themeBundle, themeBRL, "*.png");
				ThemeUtils.addThemeResources(application, themeBundle, themeBRL, "*.gif");
				ThemeUtils.addThemeResources(application, themeBundle, themeBRL, "*.jpg");
			}
		} catch (RuntimeException e) {
			// Easier access to initialisation errors
			log.error("Unexpected exception when configuring RWT application.", e);
			throw e;
		}
	}

	public void init() throws RepositoryException {
		Session session = null;
		try {
			session = NodeUtils.openDataAdminSession(repository, workspace);
			// session = JcrUtils.loginOrCreateWorkspace(repository, workspace);
			VersionManager vm = session.getWorkspace().getVersionManager();
			JcrUtils.mkdirs(session, jcrBasePath);
			session.save();
			if (!vm.isCheckedOut(jcrBasePath))
				vm.checkout(jcrBasePath);
			for (String principal : rwPrincipals)
				JcrUtils.addPrivilege(session, jcrBasePath, principal, Privilege.JCR_WRITE);
			for (String principal : roPrincipals)
				JcrUtils.addPrivilege(session, jcrBasePath, principal, Privilege.JCR_READ);

			for (String pageName : pages.keySet()) {
				try {
					initPage(session, pages.get(pageName));
					session.save();
				} catch (Exception e) {
					throw new CmsException("Cannot initialize page " + pageName, e);
				}
			}

		} finally {
			JcrUtils.logoutQuietly(session);
		}

		// publish to OSGi
		register();
	}

	protected void initPage(Session adminSession, CmsUiProvider page) throws RepositoryException {
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

	protected void register() {
		Hashtable<String, String> props = new Hashtable<String, String>();
		if (contextName != null)
			props.put("contextName", contextName);
		appReg = bundleContext.registerService(ApplicationConfiguration.class, this, props);
		if (log.isDebugEnabled())
			log.debug("Registered " + (contextName == null ? "/" : contextName));
	}

	protected void unregister() {
		appReg.unregister();
		if (log.isDebugEnabled())
			log.debug("Unregistered " + (contextName == null ? "/" : contextName));
	}

	public void setRepository(Repository repository) {
		this.repository = repository;
	}

	public void setWorkspace(String workspace) {
		this.workspace = workspace;
	}

	public void setHeader(CmsUiProvider header) {
		this.header = header;
	}

	public void setPages(Map<String, CmsUiProvider> pages) {
		this.pages = pages;
	}

	public void setJcrBasePath(String basePath) {
		this.jcrBasePath = basePath;
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

	public void setContextName(String contextName) {
		this.contextName = contextName;
	}

	class CmsExceptionHandler implements ExceptionHandler {

		@Override
		public void handleException(Throwable throwable) {
			// TODO be smarter
			CmsUiUtils.getCmsView().exception(throwable);
		}

	}

	private class CmsEntryPointFactory implements EntryPointFactory {
		private final CmsUiProvider page;
		private final Repository repository;
		private final String workspace;
		private final Map<String, String> properties;

		public CmsEntryPointFactory(CmsUiProvider page, Repository repository, String workspace,
				Map<String, String> properties) {
			this.page = page;
			this.repository = repository;
			this.workspace = workspace;
			this.properties = properties;
		}

		@Override
		public EntryPoint create() {
			SimpleErgonomics entryPoint = new SimpleErgonomics(repository, workspace, jcrBasePath, page, properties) {
				private static final long serialVersionUID = -637940404865527290L;

				@Override
				protected void createAdminArea(Composite parent) {
					Composite adminArea = new Composite(parent, SWT.NONE);
					adminArea.setLayout(new FillLayout());
					Button refresh = new Button(adminArea, SWT.PUSH);
					refresh.setText("Reload App");
					refresh.addSelectionListener(new SelectionAdapter() {
						private static final long serialVersionUID = -7671999525536351366L;

						@Override
						public void widgetSelected(SelectionEvent e) {
							long timeBeforeReload = 1000;
							RWT.getClient().getService(JavaScriptExecutor.class).execute(
									"setTimeout(function() { " + "location.reload();" + "}," + timeBeforeReload + ");");
							reloadApp();
						}
					});
				}
			};
			// entryPoint.setState("");
			entryPoint.setHeader(header);
			entryPoint.setHeaderHeight(headerHeight);
			// CmsSession.current.set(entryPoint);
			return entryPoint;
		}

		private void reloadApp() {
			new Thread("Refresh app") {
				@Override
				public void run() {
					unregister();
					register();
				}
			}.start();
		}
	}

	private static ResourceLoader createResourceLoader(final String resourceName) {
		return new ResourceLoader() {
			public InputStream getResourceAsStream(String resourceName) throws IOException {
				return getClass().getClassLoader().getResourceAsStream(resourceName);
			}
		};
	}

	// private static ResourceLoader createUrlResourceLoader(final URL url) {
	// return new ResourceLoader() {
	// public InputStream getResourceAsStream(String resourceName)
	// throws IOException {
	// return url.openStream();
	// }
	// };
	// }

	/*
	 * TEXTS
	 */
	private static String DEFAULT_LOADING_BODY = "<div"
			+ " style=\"position: absolute; left: 50%; top: 50%; margin: -32px -32px; width: 64px; height:64px\">"
			+ "<img src=\"./rwt-resources/" + LOADING_IMAGE
			+ "\" width=\"32\" height=\"32\" style=\"margin: 16px 16px\"/>" + "</div>";
}
