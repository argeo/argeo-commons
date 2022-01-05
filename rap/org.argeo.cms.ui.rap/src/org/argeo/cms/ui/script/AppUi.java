package org.argeo.cms.ui.script;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.script.Invocable;
import javax.script.ScriptException;

import org.argeo.api.cms.CmsConstants;
import org.argeo.cms.swt.CmsSwtUtils;
import org.argeo.cms.swt.Selected;
import org.argeo.cms.ui.CmsUiProvider;
import org.argeo.cms.ui.util.CmsPane;
import org.argeo.cms.web.SimpleErgonomics;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.rap.rwt.application.Application;
import org.eclipse.rap.rwt.application.EntryPoint;
import org.eclipse.rap.rwt.application.EntryPointFactory;
import org.eclipse.rap.rwt.client.WebClient;
import org.eclipse.rap.rwt.client.service.JavaScriptExecutor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.osgi.framework.BundleContext;

public class AppUi implements CmsUiProvider, Branding {
	private final CmsScriptApp app;

	private CmsUiProvider ui;
	private String createUi;
	private Object impl;
	private String script;
	// private Branding branding = new Branding();

	private EntryPointFactory factory;

	// Branding
	private String themeId;
	private String additionalHeaders;
	private String bodyHtml;
	private String pageTitle;
	private String pageOverflow;
	private String favicon;

	public AppUi(CmsScriptApp app) {
		this.app = app;
	}

	public AppUi(CmsScriptApp app, String scriptPath) {
		this.app = app;
		this.ui = new ScriptUi((BundleContext) app.getScriptEngine().get(CmsScriptRwtApplication.BC),
				app.getScriptEngine(), scriptPath);
	}

	public AppUi(CmsScriptApp app, CmsUiProvider uiProvider) {
		this.app = app;
		this.ui = uiProvider;
	}

	public AppUi(CmsScriptApp app, EntryPointFactory factory) {
		this.app = app;
		this.factory = factory;
	}

	public void apply(Repository repository, Application application, Branding appBranding, String path) {
		Map<String, String> factoryProperties = new HashMap<>();
		if (appBranding != null)
			appBranding.applyBranding(factoryProperties);
		applyBranding(factoryProperties);
		if (factory != null) {
			application.addEntryPoint("/" + path, factory, factoryProperties);
		} else {
			EntryPointFactory entryPointFactory = new EntryPointFactory() {
				@Override
				public EntryPoint create() {
					SimpleErgonomics ergonomics = new SimpleErgonomics(repository, CmsConstants.SYS_WORKSPACE,
							"/home/root/argeo:keyring", AppUi.this, factoryProperties);
//					CmsUiProvider header = app.getHeader();
//					if (header != null)
//						ergonomics.setHeader(header);
					app.applySides(ergonomics);
					Integer headerHeight = app.getHeaderHeight();
					if (headerHeight != null)
						ergonomics.setHeaderHeight(headerHeight);
					return ergonomics;
				}
			};
			application.addEntryPoint("/" + path, entryPointFactory, factoryProperties);
		}
	}

	public void setUi(CmsUiProvider uiProvider) {
		this.ui = uiProvider;
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

	// public Branding getBranding() {
	// return branding;
	// }

	@Override
	public Control createUi(Composite parent, Node context) throws RepositoryException {
		CmsPane cmsPane = new CmsPane(parent, SWT.NONE);

		if (false) {
			// QA
			CmsSwtUtils.style(cmsPane.getQaArea(), "qa");
			Button reload = new Button(cmsPane.getQaArea(), SWT.FLAT);
			CmsSwtUtils.style(reload, "qa");
			reload.setText("Reload");
			reload.addSelectionListener(new Selected() {
				private static final long serialVersionUID = 1L;

				@Override
				public void widgetSelected(SelectionEvent e) {
					new Thread() {
						@Override
						public void run() {
							app.reload();
						}
					}.start();
					RWT.getClient().getService(JavaScriptExecutor.class)
							.execute("setTimeout('location.reload()',1000)");
				}
			});

			// Support
			CmsSwtUtils.style(cmsPane.getSupportArea(), "support");
			Label msg = new Label(cmsPane.getSupportArea(), SWT.NONE);
			CmsSwtUtils.style(msg, "support");
			msg.setText("UNSUPPORTED DEVELOPMENT VERSION");
		}

		if (ui != null) {
			ui.createUi(cmsPane.getMainArea(), context);
		}
		if (createUi != null) {
			Invocable invocable = (Invocable) app.getScriptEngine();
			try {
				invocable.invokeFunction(createUi, cmsPane.getMainArea(), context);

			} catch (NoSuchMethodException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ScriptException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if (impl != null) {
			Invocable invocable = (Invocable) app.getScriptEngine();
			try {
				invocable.invokeMethod(impl, "createUi", cmsPane.getMainArea(), context);

			} catch (NoSuchMethodException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ScriptException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		// Invocable invocable = (Invocable) app.getScriptEngine();
		// try {
		// invocable.invokeMethod(AppUi.this, "initUi", parent, context);
		//
		// } catch (NoSuchMethodException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// } catch (ScriptException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }

		return null;
	}

	public void setCreateUi(String createUi) {
		this.createUi = createUi;
	}

	public void setImpl(Object impl) {
		this.impl = impl;
	}

	public Object getImpl() {
		return impl;
	}

	public String getScript() {
		return script;
	}

	public void setScript(String script) {
		this.script = script;
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

}
