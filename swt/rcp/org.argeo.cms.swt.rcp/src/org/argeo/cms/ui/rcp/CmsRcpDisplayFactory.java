package org.argeo.cms.ui.rcp;

import java.nio.file.Path;

import org.argeo.api.cms.CmsApp;
import org.argeo.util.OS;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Display;

/** Creates the SWT {@link Display} in a dedicated thread. */
public class CmsRcpDisplayFactory {
	/** File name in a run directory */
	private final static String ARGEO_RCP_URL = "argeo.rcp.url";

	/** There is only one display in RCP mode */
	private static Display display;

	private CmsUiThread uiThread;

	private boolean shutdown = false;

	public void init() {
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
		shutdown = true;
		display.wake();
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
			display = Display.getDefault();
			display.setRuntimeExceptionHandler((e) -> e.printStackTrace());
			display.setErrorHandler((e) -> e.printStackTrace());

//			for (String contextName : cmsApps.keySet()) {
//				openCmsApp(contextName);
//			}

			while (!shutdown) {
				if (!display.readAndDispatch())
					display.sleep();
			}
			display.dispose();
			display = null;
		}
	}

	public static Display getDisplay() {
		return display;
	}

	public static void openCmsApp(CmsApp cmsApp, String uiName, DisposeListener disposeListener) {
		CmsRcpDisplayFactory.getDisplay().syncExec(() -> {
			CmsRcpApp cmsRcpApp = new CmsRcpApp(uiName);
			cmsRcpApp.setCmsApp(cmsApp, null);
			cmsRcpApp.initRcpApp();
			if (disposeListener != null)
				cmsRcpApp.getShell().addDisposeListener(disposeListener);
		});
	}

	public static Path getUrlRunFile() {
		return OS.getRunDir().resolve(CmsRcpDisplayFactory.ARGEO_RCP_URL);
	}
}
