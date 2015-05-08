package org.argeo.cms.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.security.Privilege;
import javax.jcr.version.VersionManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.cms.CmsConstants;
import org.argeo.cms.CmsException;
import org.argeo.cms.CmsSession;
import org.argeo.cms.CmsUiProvider;
import org.argeo.cms.LifeCycleUiProvider;
import org.argeo.jcr.JcrUtils;
import org.eclipse.gemini.blueprint.context.BundleContextAware;
import org.eclipse.rap.rwt.application.Application;
import org.eclipse.rap.rwt.application.Application.OperationMode;
import org.eclipse.rap.rwt.application.ApplicationConfiguration;
import org.eclipse.rap.rwt.application.EntryPoint;
import org.eclipse.rap.rwt.application.EntryPointFactory;
import org.eclipse.rap.rwt.application.ExceptionHandler;
import org.eclipse.rap.rwt.client.WebClient;
import org.eclipse.rap.rwt.service.ResourceLoader;
import org.osgi.framework.BundleContext;

/** A basic generic app based on {@link SimpleErgonomics}. */
public class SimpleApp implements CmsConstants, ApplicationConfiguration,
		BundleContextAware {
	private final static Log log = LogFactory.getLog(SimpleApp.class);

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

	public void configure(Application application) {
		try {
			application.setOperationMode(OperationMode.SWT_COMPATIBILITY);
			application.setExceptionHandler(new CmsExceptionHandler());

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

				//
				// ADD ENTRY POINT
				//
				application.addEntryPoint("/" + page, new CmsEntryPointFactory(
						pages.get(page), repository, workspace, properties),
						properties);
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
		private final CmsUiProvider page;
		private final Repository repository;
		private final String workspace;
		private final Map<String, String> properties;

		public CmsEntryPointFactory(CmsUiProvider page, Repository repository,
				String workspace, Map<String, String> properties) {
			this.page = page;
			this.repository = repository;
			this.workspace = workspace;
			this.properties = properties;
		}

		@Override
		public EntryPoint create() {
			SimpleErgonomics entryPoint = new SimpleErgonomics(repository,
					workspace, basePath, page, properties);
			// entryPoint.setState("");
			entryPoint.setHeader(header);
			entryPoint.setHeaderHeight(headerHeight);
			CmsSession.current.set(entryPoint);
			return entryPoint;
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
			+ "\" width=\"32\" height=\"32\" style=\"margin: 16px 16px\"/>"
			+ "</div>";
}
