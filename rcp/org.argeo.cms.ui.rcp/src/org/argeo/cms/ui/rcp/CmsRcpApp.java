package org.argeo.cms.ui.rcp;

import java.io.IOException;
import java.io.InputStream;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.api.NodeConstants;
import org.argeo.api.cms.CmsApp;
import org.argeo.api.cms.CmsImageManager;
import org.argeo.api.cms.CmsSession;
import org.argeo.api.cms.CmsTheme;
import org.argeo.api.cms.CmsUi;
import org.argeo.api.cms.CmsView;
import org.argeo.api.cms.UxContext;
import org.argeo.cms.osgi.CmsOsgiUtils;
import org.argeo.cms.swt.CmsSwtUtils;
import org.eclipse.e4.ui.css.core.engine.CSSEngine;
import org.eclipse.e4.ui.css.core.engine.CSSErrorHandler;
import org.eclipse.e4.ui.css.swt.engine.CSSSWTEngineImpl;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

/** Runs a {@link CmsApp} as an SWT desktop application. */
@SuppressWarnings("restriction")
public class CmsRcpApp implements CmsView {
	private final static Log log = LogFactory.getLog(CmsRcpApp.class);

	private BundleContext bundleContext = FrameworkUtil.getBundle(CmsRcpApp.class).getBundleContext();

	private Display display;
	private Shell shell;
	private CmsApp cmsApp;
	private CmsUiThread uiThread;

	// CMS View
	private String uid;
	private LoginContext loginContext;

	private EventAdmin eventAdmin;

	private CSSEngine cssEngine;

	private CmsUi ui;
	// TODO make it configurable
	private String uiName = "desktop";

	public CmsRcpApp() {
		uid = UUID.randomUUID().toString();
	}

	public void init(Map<String, String> properties) {
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			// silent
		}
		uiThread = new CmsUiThread();
		uiThread.start();

	}

	public void destroy(Map<String, String> properties) {
		if (!shell.isDisposed())
			shell.dispose();
		try {
			uiThread.join();
		} catch (InterruptedException e) {
			// silent
		} finally {
			uiThread = null;
		}
	}

	class CmsUiThread extends Thread {

		public CmsUiThread() {
			super("CMS UI");
		}

		@Override
		public void run() {
			display = new Display();
			shell = new Shell(display);
			shell.setText("Argeo CMS");
			Composite parent = shell;
			parent.setLayout(new GridLayout());
			CmsSwtUtils.registerCmsView(shell, CmsRcpApp.this);

//			Subject subject = new Subject();
//			CmsLoginShell loginShell = new CmsLoginShell(CmsRcpApp.this);
//			loginShell.setSubject(subject);
			try {
				// try pre-auth
//				loginContext = new LoginContext(NodeConstants.LOGIN_CONTEXT_USER, subject, loginShell);
				loginContext = new LoginContext(NodeConstants.LOGIN_CONTEXT_SINGLE_USER);
				loginContext.login();
			} catch (LoginException e) {
				throw new IllegalStateException("Could not log in.", e);
//				loginShell.createUi();
//				loginShell.open();
//
//				while (!loginShell.getShell().isDisposed()) {
//					if (!display.readAndDispatch())
//						display.sleep();
//				}
			}
			if (log.isDebugEnabled())
				log.debug("Logged in to desktop: " + loginContext.getSubject());

			Subject.doAs(loginContext.getSubject(), (PrivilegedAction<Void>) () -> {

				// TODO factorise with web app
				parent.setData(CmsApp.UI_NAME_PROPERTY, uiName);
				ui = cmsApp.initUi(parent);
				if (ui instanceof Composite)
					((Composite) ui).setLayoutData(CmsSwtUtils.fillAll());
				//ui.setLayoutData(CmsUiUtils.fillAll());
				// we need ui to be set before refresh so that CmsView can store UI context data
				// in it.
				cmsApp.refreshUi(ui, null);

				// Styling
				CmsTheme theme = CmsSwtUtils.getCmsTheme(parent);
				if (theme != null) {
					cssEngine = new CSSSWTEngineImpl(display);
					for (String path : theme.getSwtCssPaths()) {
						try (InputStream in = theme.loadPath(path)) {
							cssEngine.parseStyleSheet(in);
						} catch (IOException e) {
							throw new IllegalStateException("Cannot load stylesheet " + path, e);
						}
					}
					cssEngine.setErrorHandler(new CSSErrorHandler() {
						public void error(Exception e) {
							log.error("SWT styling error: ", e);
						}
					});
					applyStyles(shell);
				}
				shell.layout(true, true);

				shell.open();
				while (!shell.isDisposed()) {
					if (!display.readAndDispatch())
						display.sleep();
				}
				display.dispose();
				return null;
			});
		}

	}

	/*
	 * CMS VIEW
	 */

	@Override
	public String getUid() {
		return uid;
	}

	@Override
	public UxContext getUxContext() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void navigateTo(String state) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void authChange(LoginContext loginContext) {
	}

	@Override
	public void logout() {
		if (loginContext != null)
			try {
				loginContext.logout();
			} catch (LoginException e) {
				log.error("Cannot log out", e);
			}
	}

	@Override
	public void exception(Throwable e) {
		log.error("Unexpected exception in CMS RCP", e);
	}

	@Override
	public CmsImageManager getImageManager() {
		throw new UnsupportedOperationException();
	}

	@Override
	public CmsSession getCmsSession() {
		CmsSession cmsSession = CmsOsgiUtils.getCmsSession(bundleContext, getSubject());
		return cmsSession;
	}

	@Override
	public Object getData(String key) {
		if (ui != null) {
			return ui.getData(key);
		} else {
			throw new IllegalStateException("UI is not initialized");
		}
	}

	@Override
	public void setData(String key, Object value) {
		if (ui != null) {
			ui.setData(key, value);
		} else {
			throw new IllegalStateException("UI is not initialized");
		}
	}

	@Override
	public boolean isAnonymous() {
		return false;
	}

	@Override
	public void applyStyles(Object node) {
		if (cssEngine != null)
			cssEngine.applyStyles(node, true);
	}

	@Override
	public void sendEvent(String topic, Map<String, Object> properties) {
		if (properties == null)
			properties = new HashMap<>();
		if (properties.containsKey(CMS_VIEW_UID_PROPERTY) && !properties.get(CMS_VIEW_UID_PROPERTY).equals(uid))
			throw new IllegalArgumentException("Property " + CMS_VIEW_UID_PROPERTY + " is set to another CMS view uid ("
					+ properties.get(CMS_VIEW_UID_PROPERTY) + ") then " + uid);
		properties.put(CMS_VIEW_UID_PROPERTY, uid);
		eventAdmin.sendEvent(new Event(topic, properties));
	}

	public <T> T doAs(PrivilegedAction<T> action) {
		return Subject.doAs(getSubject(), action);
	}

	protected Subject getSubject() {
		return loginContext.getSubject();
	}

	/*
	 * DEPENDENCY INJECTION
	 */
	public void setCmsApp(CmsApp cmsApp) {
		this.cmsApp = cmsApp;
	}

	public void setEventAdmin(EventAdmin eventAdmin) {
		this.eventAdmin = eventAdmin;
	}

}
