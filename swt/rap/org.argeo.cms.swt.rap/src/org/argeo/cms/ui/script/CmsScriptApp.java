package org.argeo.cms.ui.script;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.script.ScriptEngine;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.argeo.api.cms.CmsLog;
import org.argeo.api.cms.ux.CmsTheme;
import org.argeo.cms.swt.CmsException;
import org.argeo.cms.ui.CmsUiConstants;
import org.argeo.cms.ui.CmsUiProvider;
import org.argeo.cms.ui.util.CmsUiUtils;
import org.argeo.cms.web.BundleResourceLoader;
import org.argeo.cms.web.SimpleErgonomics;
import org.argeo.cms.web.WebThemeUtils;
import org.eclipse.rap.rwt.application.Application;
import org.eclipse.rap.rwt.application.Application.OperationMode;
import org.eclipse.rap.rwt.application.ApplicationConfiguration;
import org.eclipse.rap.rwt.application.ExceptionHandler;
import org.eclipse.rap.rwt.client.WebClient;
import org.eclipse.rap.rwt.service.ResourceLoader;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;

public class CmsScriptApp implements Branding {
	public final static String CONTEXT_NAME = "contextName";

	ServiceRegistration<ApplicationConfiguration> appConfigReg;

	private ScriptEngine scriptEngine;

	private final static CmsLog log = CmsLog.getLog(CmsScriptApp.class);

	private String webPath;
	private String repo = "(cn=node)";

	// private Branding branding = new Branding();
	private CmsTheme theme;

	private List<String> resources = new ArrayList<>();

	private Map<String, AppUi> ui = new HashMap<>();

	private CmsUiProvider header;
	private Integer headerHeight = null;
	private CmsUiProvider lead;
	private CmsUiProvider end;
	private CmsUiProvider footer;

	// Branding
	private String themeId;
	private String additionalHeaders;
	private String bodyHtml;
	private String pageTitle;
	private String pageOverflow;
	private String favicon;

	public CmsScriptApp(ScriptEngine scriptEngine) {
		super();
		this.scriptEngine = scriptEngine;
	}

	public void apply(BundleContext bundleContext, Repository repository, Application application) {
		BundleResourceLoader bundleRL = new BundleResourceLoader(bundleContext.getBundle());

		application.setOperationMode(OperationMode.SWT_COMPATIBILITY);
		// application.setOperationMode(OperationMode.JEE_COMPATIBILITY);

		application.setExceptionHandler(new CmsExceptionHandler());

		// loading animated gif
		application.addResource(CmsUiConstants.LOADING_IMAGE, createResourceLoader(CmsUiConstants.LOADING_IMAGE));
		// empty image
		application.addResource(CmsUiConstants.NO_IMAGE, createResourceLoader(CmsUiConstants.NO_IMAGE));

		for (String resource : resources) {
			application.addResource(resource, bundleRL);
			if (log.isTraceEnabled())
				log.trace("Resource " + resource);
		}

		if (theme != null) {
			WebThemeUtils.apply(application, theme);
			String themeHeaders = theme.getHtmlHeaders();
			if (themeHeaders != null) {
				if (additionalHeaders == null)
					additionalHeaders = themeHeaders;
				else
					additionalHeaders = themeHeaders + "\n" + additionalHeaders;
			}
			themeId = theme.getThemeId();
		}

		// client JavaScript
		Bundle appBundle = bundleRL.getBundle();
		BundleContext bc = appBundle.getBundleContext();
		HttpService httpService = bc.getService(bc.getServiceReference(HttpService.class));
		HttpContext httpContext = new BundleHttpContext(bc);
		Enumeration<URL> themeResources = appBundle.findEntries("/js/", "*", true);
		if (themeResources != null)
			bundleResources: while (themeResources.hasMoreElements()) {
				try {
					String name = themeResources.nextElement().getPath();
					if (name.endsWith("/"))
						continue bundleResources;
					String alias = "/" + getWebPath() + name;

					httpService.registerResources(alias, name, httpContext);
					if (log.isDebugEnabled())
						log.debug("Mapped " + name + " to alias " + alias);

				} catch (NamespaceException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		// App UIs
		for (String appUiName : ui.keySet()) {
			AppUi appUi = ui.get(appUiName);
			appUi.apply(repository, application, this, appUiName);

		}

	}

	public void applySides(SimpleErgonomics simpleErgonomics) {
		simpleErgonomics.setHeader(header);
		simpleErgonomics.setLead(lead);
		simpleErgonomics.setEnd(end);
		simpleErgonomics.setFooter(footer);
	}

	public void register(BundleContext bundleContext, ApplicationConfiguration appConfig) {
		Hashtable<String, String> props = new Hashtable<>();
		props.put(CONTEXT_NAME, webPath);
		appConfigReg = bundleContext.registerService(ApplicationConfiguration.class, appConfig, props);
	}

	public void reload() {
		BundleContext bundleContext = appConfigReg.getReference().getBundle().getBundleContext();
		ApplicationConfiguration appConfig = bundleContext.getService(appConfigReg.getReference());
		appConfigReg.unregister();
		register(bundleContext, appConfig);

		// BundleContext bundleContext = (BundleContext)
		// getScriptEngine().get("bundleContext");
		// try {
		// Bundle bundle = bundleContext.getBundle();
		// bundle.stop();
		// bundle.start();
		// } catch (BundleException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
	}

	private static ResourceLoader createResourceLoader(final String resourceName) {
		return new ResourceLoader() {
			public InputStream getResourceAsStream(String resourceName) throws IOException {
				return getClass().getClassLoader().getResourceAsStream(resourceName);
			}
		};
	}

	public List<String> getResources() {
		return resources;
	}

	public AppUi newUi(String name) {
		if (ui.containsKey(name))
			throw new IllegalArgumentException("There is already an UI named " + name);
		AppUi appUi = new AppUi(this);
		// appUi.setApp(this);
		ui.put(name, appUi);
		return appUi;
	}

	public void addUi(String name, AppUi appUi) {
		if (ui.containsKey(name))
			throw new IllegalArgumentException("There is already an UI named " + name);
		// appUi.setApp(this);
		ui.put(name, appUi);
	}

	public void applyBranding(Map<String, String> properties) {
		if (themeId != null)
			properties.put(WebClient.THEME_ID, themeId);
		if (additionalHeaders != null)
			properties.put(WebClient.HEAD_HTML, additionalHeaders);
		if (bodyHtml != null)
			properties.put(WebClient.BODY_HTML, bodyHtml);
		if (pageTitle != null)
			properties.put(WebClient.PAGE_TITLE, pageTitle);
		if (pageOverflow != null)
			properties.put(WebClient.PAGE_OVERFLOW, pageOverflow);
		if (favicon != null)
			properties.put(WebClient.FAVICON, favicon);
	}

	class CmsExceptionHandler implements ExceptionHandler {

		@Override
		public void handleException(Throwable throwable) {
			// TODO be smarter
			CmsUiUtils.getCmsView().exception(throwable);
		}

	}

	// public Branding getBranding() {
	// return branding;
	// }

	ScriptEngine getScriptEngine() {
		return scriptEngine;
	}

	public static String toJson(Node node) {
		try {
			StringBuilder sb = new StringBuilder();
			sb.append('{');
			PropertyIterator pit = node.getProperties();
			int count = 0;
			while (pit.hasNext()) {
				Property p = pit.nextProperty();
				int type = p.getType();
				if (type == PropertyType.REFERENCE || type == PropertyType.WEAKREFERENCE || type == PropertyType.PATH) {
					Node ref = p.getNode();
					if (count != 0)
						sb.append(',');
					// TODO limit depth?
					sb.append(toJson(ref));
					count++;
				} else if (!p.isMultiple()) {
					if (count != 0)
						sb.append(',');
					sb.append('\"').append(p.getName()).append("\":\"").append(p.getString()).append('\"');
					count++;
				}
			}
			sb.append('}');
			return sb.toString();
		} catch (RepositoryException e) {
			throw new CmsException("Cannot convert " + node + " to JSON", e);
		}
	}

	public void fromJson(Node node, String json) {
		// TODO
	}

	public CmsTheme getTheme() {
		return theme;
	}

	public void setTheme(CmsTheme theme) {
		this.theme = theme;
	}

	public String getWebPath() {
		return webPath;
	}

	public void setWebPath(String context) {
		this.webPath = context;
	}

	public String getRepo() {
		return repo;
	}

	public void setRepo(String repo) {
		this.repo = repo;
	}

	public Map<String, AppUi> getUi() {
		return ui;
	}

	public void setUi(Map<String, AppUi> ui) {
		this.ui = ui;
	}

	// Branding
	public String getThemeId() {
		return themeId;
	}

	public void setThemeId(String themeId) {
		this.themeId = themeId;
	}

	public String getAdditionalHeaders() {
		return additionalHeaders;
	}

	public void setAdditionalHeaders(String additionalHeaders) {
		this.additionalHeaders = additionalHeaders;
	}

	public String getBodyHtml() {
		return bodyHtml;
	}

	public void setBodyHtml(String bodyHtml) {
		this.bodyHtml = bodyHtml;
	}

	public String getPageTitle() {
		return pageTitle;
	}

	public void setPageTitle(String pageTitle) {
		this.pageTitle = pageTitle;
	}

	public String getPageOverflow() {
		return pageOverflow;
	}

	public void setPageOverflow(String pageOverflow) {
		this.pageOverflow = pageOverflow;
	}

	public String getFavicon() {
		return favicon;
	}

	public void setFavicon(String favicon) {
		this.favicon = favicon;
	}

	public CmsUiProvider getHeader() {
		return header;
	}

	public void setHeader(CmsUiProvider header) {
		this.header = header;
	}

	public Integer getHeaderHeight() {
		return headerHeight;
	}

	public void setHeaderHeight(Integer headerHeight) {
		this.headerHeight = headerHeight;
	}

	public CmsUiProvider getLead() {
		return lead;
	}

	public void setLead(CmsUiProvider lead) {
		this.lead = lead;
	}

	public CmsUiProvider getEnd() {
		return end;
	}

	public void setEnd(CmsUiProvider end) {
		this.end = end;
	}

	public CmsUiProvider getFooter() {
		return footer;
	}

	public void setFooter(CmsUiProvider footer) {
		this.footer = footer;
	}

	static class BundleHttpContext implements HttpContext {
		private BundleContext bundleContext;

		public BundleHttpContext(BundleContext bundleContext) {
			super();
			this.bundleContext = bundleContext;
		}

		@Override
		public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) throws IOException {
			// TODO Auto-generated method stub
			return true;
		}

		@Override
		public URL getResource(String name) {

			return bundleContext.getBundle().getEntry(name);
		}

		@Override
		public String getMimeType(String name) {
			return null;
		}

	}

}
