package org.argeo.cms.ui.rcp;

import java.net.InetSocketAddress;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.argeo.api.cms.CmsApp;
import org.argeo.api.cms.CmsAuth;
import org.argeo.api.cms.CmsDeployment;
import org.argeo.api.cms.CmsLog;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Display;

import com.sun.net.httpserver.HttpsServer;

/** Creates the SWT {@link Display} in a dedicated thread. */
public class CmsRcpDisplayFactory {
	private final static CmsLog log = CmsLog.getLog(CmsRcpDisplayFactory.class);

	/** There is only one display in RCP mode */
	private Display display;

	private CmsUiThread uiThread;

	private boolean shutdown = false;

	private CmsDeployment cmsDeployment;

	private LoginContext singleUserLoginContext;

	public void init() {
		// full control over UI thread
		uiThread = new CmsUiThread();
		uiThread.start();
		while (display == null)
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// silent
			}
	}

	public void destroy() {
		// close UI
		shutdown = true;

		if (display != null)
			display.wake();
		try {
			uiThread.join();
		} catch (InterruptedException e) {
			// silent
		} finally {
			uiThread = null;
		}

		// log out
		try {
			singleUserLoginContext.logout();
		} catch (LoginException e) {
			log.error("Cannot log out RCP single user", e);
		}
	}

	class CmsUiThread extends Thread {

		public CmsUiThread() {
			super("CMS RCP UI");
		}

		@Override
		public void run() {

			// create display loop
			try {
				display = Display.getDefault();
				boolean displayOwner = display.getThread() == this;
				if (displayOwner) {
					display.setRuntimeExceptionHandler((e) -> e.printStackTrace());
					display.setErrorHandler((e) -> e.printStackTrace());

					while (!shutdown) {
						if (!display.readAndDispatch())
							display.sleep();
					}
					display.dispose();
					display = null;
				}

			} catch (UnsatisfiedLinkError e) {
				log.error(
						"Cannot load SWT, either because the SWT DLLs are no in the java.library.path,"
								+ " or because the OSGi framework has been refreshed." + " Restart the application.",
						e);
			}
		}
	}

	protected Display getDisplay() {
		return display;
	}

	public void openCmsApp(CmsApp cmsApp, String uiName, DisposeListener disposeListener) {
		cmsDeployment.getHttpServer().thenAccept((httpServer) -> {
			getDisplay().syncExec(() -> {

				// login
				if (singleUserLoginContext == null)
					try {
						singleUserLoginContext = new LoginContext(CmsAuth.SINGLE_USER.getLoginContextName());
						singleUserLoginContext.login();
					} catch (LoginException e) {
						throw new IllegalStateException("Could not log in.", e);
					}

				RcpCmsView rcpCmsView = new RcpCmsView(cmsApp, uiName);
				if (httpServer != null) {
					InetSocketAddress addr = httpServer.getAddress();
					String scheme = "http";
					if (httpServer instanceof HttpsServer httpsServer) {
						if (httpsServer.getHttpsConfigurator() != null)
							scheme = "https";
					}
					String httpServerBase = scheme + "://" + addr.getHostString() + ":" + addr.getPort();
					rcpCmsView.setHttpServerBase(httpServerBase);
				}
				// FIXME find window title definition
				rcpCmsView.initView(singleUserLoginContext, "Argeo CMS");
				if (disposeListener != null)
					rcpCmsView.getShell().addDisposeListener(disposeListener);
			});
		});
	}

	public void setCmsDeployment(CmsDeployment cmsDeployment) {
		this.cmsDeployment = cmsDeployment;
	}

}
