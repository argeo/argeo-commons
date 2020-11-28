package org.argeo.cms.web;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.cms.ui.CmsApp;
import org.argeo.cms.ui.CmsAppListener;
import org.argeo.cms.ui.CmsTheme;
import org.argeo.cms.ui.CmsView;
import org.argeo.util.LangUtils;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.rap.rwt.application.Application;
import org.eclipse.rap.rwt.application.ApplicationConfiguration;
import org.eclipse.rap.rwt.application.ExceptionHandler;
import org.eclipse.rap.rwt.client.WebClient;
import org.eclipse.swt.widgets.Display;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.EventAdmin;

/** An RWT web app integrating with a {@link CmsApp}. */
public class CmsWebApp implements ApplicationConfiguration, ExceptionHandler, CmsAppListener {
	private final static Log log = LogFactory.getLog(CmsWebApp.class);

	private BundleContext bundleContext;
	private CmsApp cmsApp;
	private EventAdmin eventAdmin;

	private ServiceRegistration<ApplicationConfiguration> rwtAppReg;

	private final static String CONTEXT_NAME = "contextName";
	private String contextName;

	public void init(BundleContext bundleContext, Map<String, String> properties) {
		this.bundleContext = bundleContext;
		contextName = properties.get(CONTEXT_NAME);
		if (cmsApp != null)
			themingUpdated();
	}

	public void destroy(BundleContext bundleContext, Map<String, String> properties) {
		if (cmsApp != null)
			cmsApp.removeCmsAppListener(this);
	}

	@Override
	public void configure(Application application) {
		for (String uiName : cmsApp.getUiNames()) {
			CmsTheme theme = cmsApp.getTheme(uiName);
			if (theme != null)
				WebThemeUtils.apply(application, theme);
		}

		Map<String, String> properties = new HashMap<>();
		addEntryPoints(application, properties);
		application.setExceptionHandler(this);
	}

	@Override
	public void handleException(Throwable throwable) {
		Display display = Display.getCurrent();
		if (display != null && !display.isDisposed()) {
			CmsView cmsView = CmsView.getCmsView(display.getActiveShell());
			cmsView.exception(throwable);
		} else {
			log.error("Unexpected exception outside an UI thread", throwable);
		}

	}

	protected void addEntryPoints(Application application, Map<String, String> commonProperties) {
		for (String uiName : cmsApp.getUiNames()) {
			Map<String, String> properties = new HashMap<>(commonProperties);
			CmsTheme theme = cmsApp.getTheme(uiName);
			if (theme != null) {
				properties.put(WebClient.THEME_ID, theme.getThemeId());
				properties.put(WebClient.HEAD_HTML, theme.getHtmlHeaders());
			} else {
				properties.put(WebClient.THEME_ID, RWT.DEFAULT_THEME_ID);
			}
			String entryPointName = !uiName.equals("") ? "/" + uiName : "/";
			application.addEntryPoint(entryPointName, () -> {
				CmsWebEntryPoint entryPoint = new CmsWebEntryPoint(this, uiName);
				entryPoint.setEventAdmin(eventAdmin);
				return entryPoint;
			}, properties);
			if (log.isDebugEnabled())
				log.info("Added web entry point " + (contextName != null ? "/" + contextName : "") + entryPointName);
		}
		if (log.isDebugEnabled())
			log.debug("Published CMS web app /" + (contextName != null ? contextName : ""));
	}

	CmsApp getCmsApp() {
		return cmsApp;
	}

	BundleContext getBundleContext() {
		return bundleContext;
	}

	public void setCmsApp(CmsApp cmsApp, Map<String, String> properties) {
		this.cmsApp = cmsApp;
		this.cmsApp.addCmsAppListener(this);
	}

	public void unsetCmsApp(CmsApp cmsApp, Map<String, String> properties) {
		if (rwtAppReg != null)
			rwtAppReg.unregister();
		this.cmsApp = null;
	}

	@Override
	public void themingUpdated() {
		Dictionary<String, Object> regProps = LangUtils.dict(CONTEXT_NAME, contextName);
		if (rwtAppReg != null)
			rwtAppReg.unregister();
		if (bundleContext != null) {
			rwtAppReg = bundleContext.registerService(ApplicationConfiguration.class, this, regProps);
			if (log.isDebugEnabled())
				log.debug("Publishing CMS web app /" + (contextName != null ? contextName : "") + " ...");
		}
	}

	public void setEventAdmin(EventAdmin eventAdmin) {
		this.eventAdmin = eventAdmin;
	}

}
